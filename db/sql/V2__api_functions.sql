CREATE OR REPLACE FUNCTION update_object_search_vector(p_object_id UUID)
RETURNS VOID AS $$
BEGIN
  UPDATE objects
  SET search_vector =
        setweight(to_tsvector('spanish', coalesce(name, '')), 'A') ||
        setweight(to_tsvector('spanish', coalesce(description, '')), 'B'),
      updated_at = now()
  WHERE object_id = p_object_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_create_object(
  p_object_id UUID,
  p_name TEXT,
  p_description TEXT,
  p_type TEXT,
  p_tags TEXT[],
  p_metadata JSONB,
  p_location_id UUID
)
RETURNS VOID AS $$
DECLARE
  v_location_fk BIGINT;
  v_object_fk BIGINT;
BEGIN
  IF p_location_id IS NOT NULL THEN
    SELECT id
    INTO v_location_fk
    FROM locations
    WHERE location_id = p_location_id;

    IF v_location_fk IS NULL THEN
      RAISE EXCEPTION 'location_id not found: %', p_location_id;
    END IF;
  END IF;

  INSERT INTO objects (
    object_id, name, description, type,
    tags, metadata, current_location_fk
  )
  VALUES (
    p_object_id, p_name, p_description, p_type,
    p_tags, p_metadata, v_location_fk
  )
  RETURNING id INTO v_object_fk;

  PERFORM update_object_search_vector(p_object_id);

  INSERT INTO object_events (
    object_fk, event_type, to_location_fk
  )
  VALUES (
    v_object_fk, 'CREATED', v_location_fk
  );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_move_object(
  p_object_id UUID,
  p_new_location_id UUID
)
RETURNS VOID AS $$
DECLARE
  v_object_fk BIGINT;
  v_old_location_fk BIGINT;
  v_new_location_fk BIGINT;
BEGIN
  SELECT id, current_location_fk
  INTO v_object_fk, v_old_location_fk
  FROM objects
  WHERE object_id = p_object_id;

  IF v_object_fk IS NULL THEN
    RAISE EXCEPTION 'object_id not found: %', p_object_id;
  END IF;

  IF p_new_location_id IS NOT NULL THEN
    SELECT id
    INTO v_new_location_fk
    FROM locations
    WHERE location_id = p_new_location_id;

    IF v_new_location_fk IS NULL THEN
      RAISE EXCEPTION 'new location_id not found: %', p_new_location_id;
    END IF;
  END IF;

  UPDATE objects
  SET current_location_fk = v_new_location_fk,
      updated_at = now()
  WHERE id = v_object_fk;

  INSERT INTO object_events (
    object_fk,
    event_type,
    from_location_fk,
    to_location_fk
  )
  VALUES (
    v_object_fk,
    'MOVED',
    v_old_location_fk,
    v_new_location_fk
  );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_update_tags(
  p_object_id UUID,
  p_tags TEXT[]
)
RETURNS VOID AS $$
DECLARE
  v_object_fk BIGINT;
BEGIN
  SELECT id
  INTO v_object_fk
  FROM objects
  WHERE object_id = p_object_id;

  IF v_object_fk IS NULL THEN
    RAISE EXCEPTION 'object_id not found: %', p_object_id;
  END IF;

  UPDATE objects
  SET tags = p_tags,
      updated_at = now()
  WHERE id = v_object_fk;

  INSERT INTO object_events (
    object_fk, event_type, payload
  )
  VALUES (
    v_object_fk,
    'TAGGED',
    jsonb_build_object('tags', p_tags)
  );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_update_text(
  p_object_id UUID,
  p_name TEXT,
  p_description TEXT
)
RETURNS VOID AS $$
DECLARE
  v_object_fk BIGINT;
BEGIN
  SELECT id
  INTO v_object_fk
  FROM objects
  WHERE object_id = p_object_id;

  IF v_object_fk IS NULL THEN
    RAISE EXCEPTION 'object_id not found: %', p_object_id;
  END IF;

  UPDATE objects
  SET name = p_name,
      description = p_description,
      updated_at = now()
  WHERE id = v_object_fk;

  PERFORM update_object_search_vector(p_object_id);

  INSERT INTO object_events (
    object_fk, event_type
  )
  VALUES (
    v_object_fk,
    'UPDATED'
  );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_update_metadata(
  p_object_id UUID,
  p_metadata JSONB
)
RETURNS VOID AS $$
DECLARE
  v_object_fk BIGINT;
BEGIN
  SELECT id
  INTO v_object_fk
  FROM objects
  WHERE object_id = p_object_id;

  IF v_object_fk IS NULL THEN
    RAISE EXCEPTION 'object_id not found: %', p_object_id;
  END IF;

  UPDATE objects
  SET metadata = p_metadata,
      updated_at = now()
  WHERE id = v_object_fk;

  INSERT INTO object_events (
    object_fk, event_type, payload
  )
  VALUES (
    v_object_fk,
    'METADATA_UPDATED',
    jsonb_build_object('metadata', p_metadata)
  );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_search_objects(
  p_query TEXT,
  p_tags TEXT[] DEFAULT NULL,
  p_location_id UUID DEFAULT NULL,
  p_limit INT DEFAULT 20
)
RETURNS TABLE (
  object_id UUID,
  name TEXT,
  rank REAL
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    o.object_id,
    o.name,
    ts_rank(o.search_vector, q) AS rank
  FROM objects o,
       websearch_to_tsquery('spanish', p_query) q
  WHERE o.search_vector @@ q
    AND (p_tags IS NULL OR o.tags @> p_tags)
    AND (
      p_location_id IS NULL
      OR o.current_location_fk = (
        SELECT l.id FROM locations l WHERE l.location_id = p_location_id
      )
    )
  ORDER BY rank DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_fuzzy_search(
  p_text TEXT,
  p_limit INT DEFAULT 10
)
RETURNS TABLE (
  object_id UUID,
  name TEXT,
  score REAL
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    o.object_id,
    o.name,
    similarity(o.name, p_text) AS score
  FROM objects o
  WHERE o.name % p_text
  ORDER BY score DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_get_object(
  p_object_id UUID
)
RETURNS TABLE (
  object_id UUID,
  name TEXT,
  description TEXT,
  type TEXT,
  status object_status,
  current_location_id UUID,
  tags TEXT[],
  metadata JSONB,
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    o.object_id,
    o.name,
    o.description,
    o.type,
    o.status,
    l.location_id AS current_location_id,
    o.tags,
    o.metadata,
    o.created_at,
    o.updated_at
  FROM objects o
  LEFT JOIN locations l ON l.id = o.current_location_fk
  WHERE o.object_id = p_object_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_create_location(
  p_location_id UUID,
  p_name TEXT,
  p_parent_location_id UUID DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
  v_parent_fk BIGINT;
BEGIN
  IF p_parent_location_id IS NOT NULL THEN
    SELECT id
    INTO v_parent_fk
    FROM locations
    WHERE location_id = p_parent_location_id;

    IF v_parent_fk IS NULL THEN
      RAISE EXCEPTION 'parent_location_id not found: %', p_parent_location_id;
    END IF;
  END IF;

  INSERT INTO locations (location_id, name, parent_id)
  VALUES (p_location_id, p_name, v_parent_fk);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_assign_role_to_user(
  p_user_id UUID,
  p_role_id UUID
)
RETURNS VOID AS $$
BEGIN
  INSERT INTO user_roles(user_fk, role_fk)
  SELECT u.id, r.id
  FROM users u
  JOIN roles r ON r.role_id = p_role_id
  WHERE u.user_id = p_user_id
  ON CONFLICT (user_fk, role_fk) DO NOTHING;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_find_role_names_by_user_id(
  p_user_id UUID
)
RETURNS TABLE (
  role_name TEXT
) AS $$
BEGIN
  RETURN QUERY
  SELECT r.name
  FROM users u
  JOIN user_roles ur ON ur.user_fk = u.id
  JOIN roles r ON ur.role_fk = r.id
  WHERE u.user_id = p_user_id
    AND r.status = 'active'
  ORDER BY r.name;
END;
$$ LANGUAGE plpgsql;