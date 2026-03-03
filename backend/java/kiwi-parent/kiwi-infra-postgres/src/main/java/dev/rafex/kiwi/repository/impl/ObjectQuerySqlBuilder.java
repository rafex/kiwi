/*
 * Copyright 2026 Raúl Eduardo González Argote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.rafex.kiwi.repository.impl;

import dev.rafex.kiwi.query.QuerySpec;
import dev.rafex.kiwi.query.RsqlNode;
import dev.rafex.kiwi.query.RsqlOperator;
import dev.rafex.kiwi.query.Sort;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class ObjectQuerySqlBuilder {

	record BuiltQuery(String sql, List<SqlParam> params) {
	}

	record SqlParam(Object value, Integer sqlType) {
	}

	private static final Map<String, String> SORT_MAPPER = new HashMap<>();
	private static final Map<String, String> FIELD_MAPPER = new HashMap<>();

	static {
		SORT_MAPPER.put("name", "o.name");
		SORT_MAPPER.put("status", "o.status");
		SORT_MAPPER.put("createdAt", "o.created_at");
		SORT_MAPPER.put("updatedAt", "o.updated_at");

		FIELD_MAPPER.put("name", "o.name");
		FIELD_MAPPER.put("status", "o.status::text");
		FIELD_MAPPER.put("type", "o.type");
		FIELD_MAPPER.put("objectId", "o.object_id");
		FIELD_MAPPER.put("locationId", "l.location_id");
	}

	BuiltQuery build(final QuerySpec spec) {
		final var sql = new StringBuilder();
		sql.append("SELECT o.object_id, o.name, 0::real AS rank ")
				.append("FROM objects o ")
				.append("LEFT JOIN locations l ON l.id = o.current_location_fk");

		final var params = new ArrayList<SqlParam>();
		if (spec.filter() != null) {
			final var where = toSql(spec.filter(), params);
			sql.append(" WHERE ").append(where);
		}

		appendOrderBy(spec.sorts(), sql);
		sql.append(" LIMIT ? OFFSET ?");
		params.add(new SqlParam(spec.limit(), null));
		params.add(new SqlParam(spec.offset(), null));

		return new BuiltQuery(sql.toString(), params);
	}

	void bind(final Connection connection, final java.sql.PreparedStatement ps, final List<SqlParam> params)
			throws java.sql.SQLException {
		for (int i = 0; i < params.size(); i++) {
			final var p = params.get(i);
			final int idx = i + 1;
			if (p.sqlType() != null && p.value() == null) {
				ps.setNull(idx, p.sqlType());
				continue;
			}
			if (p.value() instanceof UUID uuid) {
				ps.setObject(idx, uuid);
			} else if (p.value() instanceof String[] arr) {
				ps.setArray(idx, connection.createArrayOf("text", arr));
			} else {
				ps.setObject(idx, p.value());
			}
		}
	}

	private static void appendOrderBy(final List<Sort> sorts, final StringBuilder sql) {
		if (sorts == null || sorts.isEmpty()) {
			sql.append(" ORDER BY o.created_at DESC");
			return;
		}
		final var chunks = new ArrayList<String>();
		for (final var sort : sorts) {
			final var field = SORT_MAPPER.get(sort.field());
			if (field == null) {
				throw new IllegalArgumentException("unknown sort field: " + sort.field());
			}
			final var direction = sort.direction() == Sort.Direction.DESC ? "DESC" : "ASC";
			chunks.add(field + " " + direction);
		}
		sql.append(" ORDER BY ").append(String.join(", ", chunks));
	}

	private static String toSql(final RsqlNode node, final List<SqlParam> params) {
		if (node instanceof RsqlNode.And and) {
			return joinChildren(and.nodes(), " AND ", params);
		}
		if (node instanceof RsqlNode.Or or) {
			return joinChildren(or.nodes(), " OR ", params);
		}
		if (node instanceof RsqlNode.Comp comp) {
			return toSqlComp(comp, params);
		}
		throw new IllegalArgumentException("unsupported rsql node");
	}

	private static String joinChildren(final List<RsqlNode> nodes, final String op, final List<SqlParam> params) {
		if (nodes == null || nodes.isEmpty()) {
			throw new IllegalArgumentException("empty rsql node list");
		}
		final var chunks = new ArrayList<String>();
		for (final var child : nodes) {
			chunks.add("(" + toSql(child, params) + ")");
		}
		return String.join(op, chunks);
	}

	private static String toSqlComp(final RsqlNode.Comp comp, final List<SqlParam> params) {
		final var selector = comp.selector();
		if ("tags".equals(selector)) {
			return toTagsSql(comp, params);
		}
		if ("enabled".equals(selector)) {
			return toEnabledSql(comp, params);
		}

		final var field = FIELD_MAPPER.get(selector);
		if (field == null) {
			throw new IllegalArgumentException("unknown selector: " + selector);
		}
		return switch (comp.operator()) {
			case EQ -> {
				params.add(new SqlParam(castArg(selector, comp.args().get(0)), null));
				yield field + " = ?";
			}
			case NEQ -> {
				params.add(new SqlParam(castArg(selector, comp.args().get(0)), null));
				yield field + " <> ?";
			}
			case LIKE -> {
				params.add(new SqlParam(comp.args().get(0), null));
				yield field + " ILIKE ?";
			}
			case IN -> {
				final var placeholders = addArgs(selector, comp.args(), params);
				yield field + " IN (" + placeholders + ")";
			}
			case OUT -> {
				final var placeholders = addArgs(selector, comp.args(), params);
				yield field + " NOT IN (" + placeholders + ")";
			}
		};
	}

	private static String toTagsSql(final RsqlNode.Comp comp, final List<SqlParam> params) {
		if (comp.operator() != RsqlOperator.IN && comp.operator() != RsqlOperator.OUT && comp.operator() != RsqlOperator.EQ
				&& comp.operator() != RsqlOperator.NEQ) {
			throw new IllegalArgumentException("operator not supported for tags: " + comp.operator());
		}

		if (comp.operator() == RsqlOperator.IN) {
			params.add(new SqlParam(comp.args().toArray(new String[0]), null));
			return "o.tags && ?";
		}
		if (comp.operator() == RsqlOperator.OUT) {
			params.add(new SqlParam(comp.args().toArray(new String[0]), null));
			return "NOT (o.tags && ?)";
		}
		params.add(new SqlParam(comp.args().get(0), null));
		if (comp.operator() == RsqlOperator.EQ) {
			return "? = ANY(o.tags)";
		}
		return "NOT (? = ANY(o.tags))";
	}

	private static String toEnabledSql(final RsqlNode.Comp comp, final List<SqlParam> params) {
		if (comp.operator() != RsqlOperator.EQ && comp.operator() != RsqlOperator.NEQ) {
			throw new IllegalArgumentException("enabled only supports == and !=");
		}
		final var v = comp.args().get(0).toLowerCase(Locale.ROOT);
		if (!"true".equals(v) && !"false".equals(v)) {
			throw new IllegalArgumentException("enabled must be true or false");
		}
		final boolean enabled = Boolean.parseBoolean(v);
		final boolean wantActive = comp.operator() == RsqlOperator.EQ ? enabled : !enabled;
		params.add(new SqlParam("active", Types.VARCHAR));
		return wantActive ? "o.status::text = ?" : "o.status::text <> ?";
	}

	private static String addArgs(final String selector, final List<String> args, final List<SqlParam> params) {
		if (args == null || args.isEmpty()) {
			throw new IllegalArgumentException("operator requires arguments");
		}
		final var placeholders = new ArrayList<String>();
		for (final var arg : args) {
			params.add(new SqlParam(castArg(selector, arg), null));
			placeholders.add("?");
		}
		return String.join(",", placeholders);
	}

	private static Object castArg(final String selector, final String arg) {
		if ("objectId".equals(selector) || "locationId".equals(selector)) {
			try {
				return UUID.fromString(arg);
			} catch (final IllegalArgumentException e) {
				throw new IllegalArgumentException("invalid UUID for selector " + selector);
			}
		}
		return arg;
	}

}
