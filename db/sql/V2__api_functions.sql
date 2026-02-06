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
BEGIN
  INSERT INTO objects (
    object_id, name, description, type,
    tags, metadata, current_location_id
  )
  VALUES (
    p_object_id, p_name, p_description, p_type,
    p_tags, p_metadata, p_location_id
  );

  PERFORM update_object_search_vector(p_object_id);

  INSERT INTO object_events (
    event_id, object_id, event_type, to_location_id
  )
  VALUES (
    gen_random_uuid(), p_object_id, 'CREATED', p_location_id
  );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_move_object(
  p_object_id UUID,
  p_new_location_id UUID
)
RETURNS VOID AS $$
DECLARE
  v_old_location UUID;
BEGIN
  SELECT current_location_id
  INTO v_old_location
  FROM objects
  WHERE object_id = p_object_id;

  UPDATE objects
  SET current_location_id = p_new_location_id,
      updated_at = now()
  WHERE object_id = p_object_id;

  INSERT INTO object_events (
    event_id,
    object_id,
    event_type,
    from_location_id,
    to_location_id
  )
  VALUES (
    gen_random_uuid(),
    p_object_id,
    'MOVED',
    v_old_location,
    p_new_location_id
  );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_update_tags(
  p_object_id UUID,
  p_tags TEXT[]
)
RETURNS VOID AS $$
BEGIN
  UPDATE objects
  SET tags = p_tags,
      updated_at = now()
  WHERE object_id = p_object_id;

  INSERT INTO object_events (
    event_id, object_id, event_type, payload
  )
  VALUES (
    gen_random_uuid(),
    p_object_id,
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
BEGIN
  UPDATE objects
  SET name = p_name,
      description = p_description,
      updated_at = now()
  WHERE object_id = p_object_id;

  PERFORM update_object_search_vector(p_object_id);

  INSERT INTO object_events (
    event_id, object_id, event_type
  )
  VALUES (
    gen_random_uuid(),
    p_object_id,
    'UPDATED'
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
    AND (p_location_id IS NULL OR o.current_location_id = p_location_id)
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
    object_id,
    name,
    similarity(name, p_text) AS score
  FROM objects
  WHERE name % p_text
  ORDER BY score DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

