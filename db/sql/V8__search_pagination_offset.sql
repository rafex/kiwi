CREATE OR REPLACE FUNCTION api_search_objects(
  p_query TEXT,
  p_tags TEXT[] DEFAULT NULL,
  p_location_id UUID DEFAULT NULL,
  p_limit INT DEFAULT 20,
  p_offset INT DEFAULT 0
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
  LIMIT GREATEST(p_limit, 1)
  OFFSET GREATEST(p_offset, 0);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_fuzzy_search(
  p_text TEXT,
  p_limit INT DEFAULT 10,
  p_offset INT DEFAULT 0
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
  LIMIT GREATEST(p_limit, 1)
  OFFSET GREATEST(p_offset, 0);
END;
$$ LANGUAGE plpgsql;
