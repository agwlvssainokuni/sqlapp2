/*
 * Copyright 2025 agwlvssainokuni
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

package cherry.sqlapp2.service;

import cherry.sqlapp2.dto.QueryStructure;
import cherry.sqlapp2.dto.QueryBuilderRequest;
import cherry.sqlapp2.dto.QueryBuilderResponse;
import cherry.sqlapp2.util.SqlParameterExtractor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * ビジュアルクエリビルダーのSQL生成を担当するサービスクラス。
 * クエリ構造からSQLクエリを生成し、バリデーション、パラメータ抽出、
 * エラーハンドリングを提供します。
 */
@Service
public class QueryBuilderService {

    /**
     * クエリ構造からSQLクエリを生成します。
     * 
     * @param request クエリビルダーリクエスト（クエリ構造を含む）
     * @return SQL生成結果（成功時はSQL文とパラメータ、失敗時はエラー詳細）
     */
    public QueryBuilderResponse buildQuery(QueryBuilderRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            QueryStructure structure = request.getQueryStructure();
            
            // Validate the query structure
            List<String> validationErrors = validateQueryStructure(structure);
            if (!validationErrors.isEmpty()) {
                return QueryBuilderResponse.error(validationErrors);
            }

            // Build the SQL query
            StringBuilder sql = new StringBuilder();
            
            // Build SELECT clause
            buildSelectClause(sql, structure, structure.getSelectColumns());
            
            // Build FROM clause
            buildFromClause(sql, structure.getFromTables());
            
            // Build JOIN clauses
            buildJoinClauses(sql, structure.getJoins());
            
            // Build WHERE clause
            buildWhereClause(sql, structure.getWhereConditions());
            
            // Build GROUP BY clause
            buildGroupByClause(sql, structure.getGroupByColumns());
            
            // Build HAVING clause
            buildHavingClause(sql, structure.getHavingConditions());
            
            // Build ORDER BY clause
            buildOrderByClause(sql, structure.getOrderByColumns());
            
            // Build LIMIT clause
            buildLimitClause(sql, structure.getLimit(), structure.getOffset());

            String generatedSql = sql.toString();
            
            if (request.isFormatSql()) {
                generatedSql = formatSql(generatedSql);
            }

            // Detect parameters
            Map<String, String> detectedParameters = detectParameters(generatedSql);
            
            // Create response
            QueryBuilderResponse response = QueryBuilderResponse.success(generatedSql);
            response.setDetectedParameters(detectedParameters);
            response.setBuildTimeMs(System.currentTimeMillis() - startTime);
            
            return response;
            
        } catch (Exception e) {
            List<String> errors = new ArrayList<>();
            errors.add("Failed to build query: " + e.getMessage());
            return QueryBuilderResponse.error(errors);
        }
    }

    private List<String> validateQueryStructure(QueryStructure structure) {
        List<String> errors = new ArrayList<>();
        
        // Check if we have at least one SELECT column
        if (structure.getSelectColumns().isEmpty()) {
            errors.add("At least one SELECT column is required");
        }
        
        // Check if we have at least one FROM table
        if (structure.getFromTables().isEmpty()) {
            errors.add("At least one FROM table is required");
        }
        
        // Validate SELECT columns
        for (QueryStructure.SelectColumn column : structure.getSelectColumns()) {
            if (column.getColumnName() == null || column.getColumnName().trim().isEmpty()) {
                errors.add("SELECT column name cannot be empty");
            }
        }
        
        // Validate FROM tables
        for (QueryStructure.FromTable table : structure.getFromTables()) {
            if (table.getTableName() == null || table.getTableName().trim().isEmpty()) {
                errors.add("FROM table name cannot be empty");
            }
        }
        
        return errors;
    }

    private void buildSelectClause(StringBuilder sql, QueryStructure structure, List<QueryStructure.SelectColumn> selectColumns) {
        sql.append("SELECT ");
        
        // Check QueryStructure global distinct flag or individual column distinct flags
        if (structure.isDistinct() || selectColumns.stream().anyMatch(QueryStructure.SelectColumn::isDistinct)) {
            sql.append("DISTINCT ");
        }
        
        String selectClause = selectColumns.stream()
            .map(this::formatSelectColumn)
            .collect(Collectors.joining(", "));
        
        sql.append(selectClause);
    }

    private String formatSelectColumn(QueryStructure.SelectColumn column) {
        StringBuilder formatted = new StringBuilder();
        
        if (column.getAggregateFunction() != null && !column.getAggregateFunction().trim().isEmpty()) {
            formatted.append(column.getAggregateFunction().toUpperCase()).append("(");
        }
        
        if (column.getTableName() != null && !column.getTableName().trim().isEmpty()) {
            formatted.append(column.getTableName()).append(".");
        }
        
        formatted.append(column.getColumnName());
        
        if (column.getAggregateFunction() != null && !column.getAggregateFunction().trim().isEmpty()) {
            formatted.append(")");
        }
        
        if (column.getAlias() != null && !column.getAlias().trim().isEmpty()) {
            formatted.append(" AS ").append(column.getAlias());
        }
        
        return formatted.toString();
    }

    private void buildFromClause(StringBuilder sql, List<QueryStructure.FromTable> fromTables) {
        sql.append(" FROM ");
        
        String fromClause = fromTables.stream()
            .map(this::formatFromTable)
            .collect(Collectors.joining(", "));
        
        sql.append(fromClause);
    }

    private String formatFromTable(QueryStructure.FromTable table) {
        StringBuilder formatted = new StringBuilder();
        formatted.append(table.getTableName());
        
        if (table.getAlias() != null && !table.getAlias().trim().isEmpty()) {
            formatted.append(" AS ").append(table.getAlias());
        }
        
        return formatted.toString();
    }

    private void buildJoinClauses(StringBuilder sql, List<QueryStructure.JoinClause> joins) {
        for (QueryStructure.JoinClause join : joins) {
            sql.append(" ").append(join.getJoinType().toUpperCase()).append(" JOIN ");
            sql.append(join.getTableName());
            
            if (join.getAlias() != null && !join.getAlias().trim().isEmpty()) {
                sql.append(" AS ").append(join.getAlias());
            }
            
            if (!join.getConditions().isEmpty()) {
                sql.append(" ON ");
                String joinConditions = join.getConditions().stream()
                    .map(this::formatJoinCondition)
                    .collect(Collectors.joining(" AND "));
                sql.append(joinConditions);
            }
        }
    }

    private String formatJoinCondition(QueryStructure.JoinCondition condition) {
        StringBuilder formatted = new StringBuilder();
        
        if (condition.getLeftTable() != null && !condition.getLeftTable().trim().isEmpty()) {
            formatted.append(condition.getLeftTable()).append(".");
        }
        formatted.append(condition.getLeftColumn());
        formatted.append(" ").append(condition.getOperator()).append(" ");
        
        if (condition.getRightTable() != null && !condition.getRightTable().trim().isEmpty()) {
            formatted.append(condition.getRightTable()).append(".");
        }
        formatted.append(condition.getRightColumn());
        
        return formatted.toString();
    }

    private void buildWhereClause(StringBuilder sql, List<QueryStructure.WhereCondition> whereConditions) {
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            buildConditions(sql, whereConditions);
        }
    }

    private void buildHavingClause(StringBuilder sql, List<QueryStructure.WhereCondition> havingConditions) {
        if (!havingConditions.isEmpty()) {
            sql.append(" HAVING ");
            buildConditions(sql, havingConditions);
        }
    }

    private void buildConditions(StringBuilder sql, List<QueryStructure.WhereCondition> conditions) {
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                QueryStructure.WhereCondition condition = conditions.get(i);
                String logicalOp = condition.getLogicalOperator();
                if (logicalOp != null && !logicalOp.trim().isEmpty()) {
                    sql.append(" ").append(logicalOp.toUpperCase()).append(" ");
                } else {
                    sql.append(" AND ");
                }
            }
            
            sql.append(formatWhereCondition(conditions.get(i)));
        }
    }

    private String formatWhereCondition(QueryStructure.WhereCondition condition) {
        StringBuilder formatted = new StringBuilder();
        
        if (condition.isNegated()) {
            formatted.append("NOT ");
        }
        
        if (condition.getTableName() != null && !condition.getTableName().trim().isEmpty()) {
            formatted.append(condition.getTableName()).append(".");
        }
        formatted.append(condition.getColumnName());
        
        String operator = condition.getOperator().toUpperCase();
        formatted.append(" ").append(operator).append(" ");
        
        if ("IN".equals(operator) && condition.getValues() != null && !condition.getValues().isEmpty()) {
            String inValues = condition.getValues().stream()
                .map(v -> "'" + v + "'")
                .collect(Collectors.joining(", "));
            formatted.append("(").append(inValues).append(")");
        } else if ("BETWEEN".equals(operator)) {
            // First try to use minValue and maxValue (new approach)
            if (condition.getMinValue() != null && condition.getMaxValue() != null) {
                String minVal = condition.getMinValue().startsWith(":") ? condition.getMinValue() : "'" + condition.getMinValue() + "'";
                String maxVal = condition.getMaxValue().startsWith(":") ? condition.getMaxValue() : "'" + condition.getMaxValue() + "'";
                formatted.append(minVal).append(" AND ").append(maxVal);
            }
            // Fall back to values array for backward compatibility
            else if (condition.getValues() != null && condition.getValues().size() >= 2) {
                formatted.append("'").append(condition.getValues().get(0)).append("' AND '")
                        .append(condition.getValues().get(1)).append("'");
            }
        } else if ("IS NULL".equals(operator) || "IS NOT NULL".equals(operator)) {
            // No value needed for NULL checks
        } else if (condition.getValue() != null) {
            if (condition.getValue().startsWith(":")) {
                // Parameter placeholder
                formatted.append(condition.getValue());
            } else {
                formatted.append("'").append(condition.getValue()).append("'");
            }
        }
        
        return formatted.toString();
    }

    private void buildGroupByClause(StringBuilder sql, List<QueryStructure.GroupByColumn> groupByColumns) {
        if (!groupByColumns.isEmpty()) {
            sql.append(" GROUP BY ");
            String groupByClause = groupByColumns.stream()
                .map(this::formatGroupByColumn)
                .collect(Collectors.joining(", "));
            sql.append(groupByClause);
        }
    }

    private String formatGroupByColumn(QueryStructure.GroupByColumn column) {
        StringBuilder formatted = new StringBuilder();
        
        if (column.getTableName() != null && !column.getTableName().trim().isEmpty()) {
            formatted.append(column.getTableName()).append(".");
        }
        formatted.append(column.getColumnName());
        
        return formatted.toString();
    }

    private void buildOrderByClause(StringBuilder sql, List<QueryStructure.OrderByColumn> orderByColumns) {
        if (!orderByColumns.isEmpty()) {
            sql.append(" ORDER BY ");
            String orderByClause = orderByColumns.stream()
                .map(this::formatOrderByColumn)
                .collect(Collectors.joining(", "));
            sql.append(orderByClause);
        }
    }

    private String formatOrderByColumn(QueryStructure.OrderByColumn column) {
        StringBuilder formatted = new StringBuilder();
        
        if (column.getTableName() != null && !column.getTableName().trim().isEmpty()) {
            formatted.append(column.getTableName()).append(".");
        }
        formatted.append(column.getColumnName());
        
        if (column.getDirection() != null && !column.getDirection().trim().isEmpty()) {
            formatted.append(" ").append(column.getDirection().toUpperCase());
        }
        
        return formatted.toString();
    }

    private void buildLimitClause(StringBuilder sql, Integer limit, Integer offset) {
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
            
            if (offset != null && offset > 0) {
                sql.append(" OFFSET ").append(offset);
            }
        }
    }

    private String formatSql(String sql) {
        // Simple SQL formatting - add line breaks for major clauses
        return sql.replaceAll("\\s+(FROM|WHERE|JOIN|GROUP BY|HAVING|ORDER BY|LIMIT)\\s+", "\n$1 ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Map<String, String> detectParameters(String sql) {
        Map<String, String> parameters = new HashMap<>();
        List<String> paramNames = SqlParameterExtractor.extractParameters(sql);
        
        for (String paramName : paramNames) {
            parameters.put(paramName, "string"); // Default type, could be enhanced
        }
        
        return parameters;
    }
}