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

import cherry.sqlapp2.dto.*;
import cherry.sqlapp2.dto.QueryStructure.*;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.expression.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for reverse engineering SQL queries into QueryStructure objects.
 * This is a simplified implementation that provides basic SQL to QueryBuilder conversion.
 * 
 * Current limitations:
 * - Only supports basic SELECT, FROM, WHERE clauses
 * - Complex SQL features like JOINs, subqueries, UNION are not fully supported
 * - Serves as a foundation for future enhancements
 */
@Service
public class SqlReverseEngineeringService {

    private static final Logger logger = LoggerFactory.getLogger(SqlReverseEngineeringService.class);

    /**
     * Parse SQL query and convert to QueryStructure.
     * 
     * @param sql SQL query to parse
     * @return SqlParseResult containing either parsed structure or error information
     */
    public SqlParseResult parseSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return SqlParseResult.error("SQL query is empty");
        }

        try {
            Statement statement = CCJSqlParserUtil.parse(sql.trim());
            
            if (!(statement instanceof Select)) {
                return SqlParseResult.error("Only SELECT statements are supported for reverse engineering");
            }

            Select selectStatement = (Select) statement;
            QueryStructure queryStructure = parseSelectStatement(selectStatement);
            return SqlParseResult.success(queryStructure);

        } catch (JSQLParserException e) {
            logger.warn("Failed to parse SQL: {}", e.getMessage());
            // Return fallback structure for unparseable SQL
            QueryStructure fallbackStructure = createBasicQueryStructure(sql);
            return SqlParseResult.success(fallbackStructure);
        } catch (Exception e) {
            logger.error("Unexpected error parsing SQL", e);
            return SqlParseResult.error("Failed to parse SQL: " + e.getMessage());
        }
    }

    /**
     * Create a basic QueryStructure that represents the original SQL.
     * This is a temporary implementation until full parsing is available.
     */
    private QueryStructure createBasicQueryStructure(String originalSql) {
        QueryStructure.Builder builder = QueryStructure.builder();

        // Create a basic structure with SELECT *
        SelectColumn selectColumn = new SelectColumn();
        selectColumn.setTableName("");
        selectColumn.setColumnName("*");
        List<SelectColumn> selectColumns = new ArrayList<>();
        selectColumns.add(selectColumn);
        builder.selectColumns(selectColumns);

        // Create empty FROM table (user will need to add manually)
        FromTable fromTable = new FromTable();
        fromTable.setTableName(""); // Will be filled by user
        List<FromTable> fromTables = new ArrayList<>();
        fromTables.add(fromTable);
        builder.fromTables(fromTables);

        // Set other fields to empty
        builder.distinct(false);
        builder.joins(new ArrayList<>());
        builder.whereConditions(new ArrayList<>());
        builder.groupByColumns(new ArrayList<>());
        builder.havingConditions(new ArrayList<>());
        builder.orderByColumns(new ArrayList<>());

        return builder.build();
    }

    /**
     * Parse SELECT statement and extract QueryStructure information.
     */
    private QueryStructure parseSelectStatement(Select selectStatement) {
        QueryStructure.Builder builder = QueryStructure.builder();
        
        PlainSelect plainSelect = selectStatement.getPlainSelect();
        if (plainSelect == null) {
            return createBasicQueryStructure("");
        }

        // Parse SELECT columns
        List<SelectColumn> selectColumns = parseSelectItems(plainSelect.getSelectItems());
        builder.selectColumns(selectColumns);

        // Parse DISTINCT
        Distinct distinct = plainSelect.getDistinct();
        builder.distinct(distinct != null);

        // Parse FROM tables
        List<FromTable> fromTables = parseFromItem(plainSelect.getFromItem());
        builder.fromTables(fromTables);

        // Parse JOINs (basic implementation)
        List<Join> joins = plainSelect.getJoins();
        builder.joins(parseJoins(joins));

        // Parse WHERE conditions
        Expression whereExpression = plainSelect.getWhere();
        builder.whereConditions(parseWhereConditions(whereExpression));

        // Parse ORDER BY
        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
        builder.orderByColumns(parseOrderBy(orderByElements));

        // Parse other clauses (set to empty for now)
        builder.groupByColumns(new ArrayList<>());
        builder.havingConditions(new ArrayList<>());

        // Parse LIMIT/OFFSET (if available)
        Limit limit = plainSelect.getLimit();
        if (limit != null) {
            if (limit.getRowCount() != null) {
                builder.limit(Integer.parseInt(limit.getRowCount().toString()));
            }
            if (limit.getOffset() != null) {
                builder.offset(Integer.parseInt(limit.getOffset().toString()));
            }
        }
        
        // Also check for OFFSET in a different way if not found above
        if (plainSelect.getOffset() != null) {
            builder.offset(Integer.parseInt(plainSelect.getOffset().getOffset().toString()));
        }

        return builder.build();
    }

    private List<SelectColumn> parseSelectItems(List<SelectItem<?>> selectItems) {
        List<SelectColumn> selectColumns = new ArrayList<>();
        
        if (selectItems != null) {
            for (SelectItem<?> item : selectItems) {
                SelectColumn selectColumn = new SelectColumn();
                
                // Use toString() to parse the item (improved approach with alias handling)
                String itemString = item.toString();
                
                // Handle alias first (AS keyword)
                String expression = itemString;
                String alias = null;
                
                if (itemString.toUpperCase().contains(" AS ")) {
                    String[] aliasParts = itemString.split("(?i)\\s+AS\\s+", 2);
                    if (aliasParts.length == 2) {
                        expression = aliasParts[0].trim();
                        alias = aliasParts[1].trim();
                    }
                }
                
                // Parse the expression part (without alias)
                if (expression.equals("*")) {
                    // SELECT *
                    selectColumn.setTableName("");
                    selectColumn.setColumnName("*");
                } else if (expression.contains(".*")) {
                    // SELECT table.*
                    String tableName = expression.replace(".*", "");
                    selectColumn.setTableName(tableName);
                    selectColumn.setColumnName("*");
                } else if (expression.contains(".")) {
                    // SELECT table.column
                    String[] parts = expression.split("\\.", 2);
                    selectColumn.setTableName(parts[0]);
                    selectColumn.setColumnName(parts[1]);
                } else {
                    // SELECT column
                    selectColumn.setTableName("");
                    selectColumn.setColumnName(expression);
                }
                
                // Set alias if present
                if (alias != null) {
                    selectColumn.setAlias(alias);
                }
                
                selectColumns.add(selectColumn);
            }
        }
        
        return selectColumns;
    }

    private List<FromTable> parseFromItem(FromItem fromItem) {
        List<FromTable> fromTables = new ArrayList<>();
        
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            FromTable fromTable = new FromTable();
            fromTable.setTableName(table.getName());
            
            if (table.getAlias() != null) {
                fromTable.setAlias(table.getAlias().getName());
            }
            
            fromTables.add(fromTable);
        }
        
        return fromTables;
    }

    private List<JoinClause> parseJoins(List<Join> joins) {
        List<JoinClause> joinClauses = new ArrayList<>();
        
        if (joins != null) {
            for (Join join : joins) {
                JoinClause joinClause = new JoinClause();
                
                // Determine join type
                if (join.isInner()) {
                    joinClause.setJoinType("INNER");
                } else if (join.isLeft()) {
                    joinClause.setJoinType("LEFT");
                } else if (join.isRight()) {
                    joinClause.setJoinType("RIGHT");
                } else if (join.isFull()) {
                    joinClause.setJoinType("FULL OUTER");
                } else {
                    joinClause.setJoinType("INNER"); // Default
                }
                
                // Parse joined table
                if (join.getFromItem() instanceof Table) {
                    Table table = (Table) join.getFromItem();
                    joinClause.setTableName(table.getName());
                    if (table.getAlias() != null) {
                        joinClause.setAlias(table.getAlias().getName());
                    }
                }
                
                // Parse join conditions (ON clause)
                List<JoinCondition> joinConditions = parseJoinConditions(join);
                joinClause.setConditions(joinConditions);
                
                joinClauses.add(joinClause);
            }
        }
        
        return joinClauses;
    }

    /**
     * Parse JOIN ON conditions from JSqlParser Join object.
     */
    private List<JoinCondition> parseJoinConditions(Join join) {
        List<JoinCondition> conditions = new ArrayList<>();
        
        // Try to get ON conditions using different methods for JSqlParser 5.3
        String onExpressionString = null;
        
        // Method 1: Try getOnExpressions() (newer API)
        if (join.getOnExpressions() != null && !join.getOnExpressions().isEmpty()) {
            onExpressionString = join.getOnExpressions().iterator().next().toString();
        }
        // Method 2: Fall back to toString parsing if no ON expression found
        else {
            String joinString = join.toString();
            if (joinString.toUpperCase().contains(" ON ")) {
                String[] parts = joinString.split("(?i)\\s+ON\\s+", 2);
                if (parts.length == 2) {
                    onExpressionString = parts[1].trim();
                }
            }
        }
        
        if (onExpressionString != null) {
            // Parse multiple conditions separated by AND
            String[] andParts = onExpressionString.split("(?i)\\s+AND\\s+");
            for (String conditionPart : andParts) {
                JoinCondition condition = parseSimpleJoinCondition(conditionPart.trim());
                if (condition != null) {
                    conditions.add(condition);
                }
            }
        }
        
        return conditions;
    }

    /**
     * Parse a simple JOIN condition string like "v.field_id = m.id".
     */
    private JoinCondition parseSimpleJoinCondition(String conditionString) {
        if (conditionString == null || conditionString.trim().isEmpty()) {
            return null;
        }
        
        // Handle common operators
        String[] operators = {"=", "<>", "!=", "<", ">", "<=", ">="};
        
        for (String operator : operators) {
            if (conditionString.contains(" " + operator + " ")) {
                String[] parts = conditionString.split("\\s+" + operator.replace("=", "\\=").replace("<", "\\<").replace(">", "\\>") + "\\s+", 2);
                if (parts.length == 2) {
                    String leftSide = parts[0].trim();
                    String rightSide = parts[1].trim();
                    
                    // Parse left side (e.g., "v.field_id")
                    String leftTable = "";
                    String leftColumn = leftSide;
                    if (leftSide.contains(".")) {
                        String[] leftParts = leftSide.split("\\.", 2);
                        leftTable = leftParts[0];
                        leftColumn = leftParts[1];
                    }
                    
                    // Parse right side (e.g., "m.id")
                    String rightTable = "";
                    String rightColumn = rightSide;
                    if (rightSide.contains(".")) {
                        String[] rightParts = rightSide.split("\\.", 2);
                        rightTable = rightParts[0];
                        rightColumn = rightParts[1];
                    }
                    
                    // Create JOIN condition
                    JoinCondition condition = new JoinCondition();
                    condition.setLeftTable(leftTable);
                    condition.setLeftColumn(leftColumn);
                    condition.setOperator(operator);
                    condition.setRightTable(rightTable);
                    condition.setRightColumn(rightColumn);
                    
                    return condition;
                }
            }
        }
        
        return null;
    }

    /**
     * Parse WHERE conditions from JSqlParser Expression.
     */
    private List<WhereCondition> parseWhereConditions(Expression whereExpression) {
        List<WhereCondition> conditions = new ArrayList<>();
        
        if (whereExpression != null) {
            // For now, use string parsing as a fallback
            String whereString = whereExpression.toString();
            WhereCondition condition = parseSimpleWhereCondition(whereString);
            if (condition != null) {
                conditions.add(condition);
            }
        }
        
        return conditions;
    }
    
    /**
     * Parse a simple WHERE condition string like "m.id = 'job_id'".
     */
    private WhereCondition parseSimpleWhereCondition(String conditionString) {
        if (conditionString == null || conditionString.trim().isEmpty()) {
            return null;
        }
        
        // Handle common operators
        String[] operators = {"=", "<>", "!=", "<=", ">=", "<", ">", "LIKE", "IN"};
        
        for (String operator : operators) {
            if (conditionString.contains(" " + operator + " ")) {
                String regex;
                if (operator.equals("=")) {
                    regex = "\\s+=\\s+";
                } else if (operator.equals("<>") || operator.equals("!=")) {
                    regex = "\\s+" + operator.replace("<", "\\<").replace(">", "\\>").replace("!", "\\!") + "\\s+";
                } else if (operator.equals("<=") || operator.equals(">=")) {
                    regex = "\\s+" + operator.replace("<", "\\<").replace(">", "\\>") + "\\s+";
                } else if (operator.equals("<") || operator.equals(">")) {
                    regex = "\\s+" + operator.replace("<", "\\<").replace(">", "\\>") + "\\s+";
                } else {
                    regex = "\\s+" + operator + "\\s+";
                }
                
                String[] parts = conditionString.split(regex, 2);
                if (parts.length == 2) {
                    String leftSide = parts[0].trim();
                    String rightSide = parts[1].trim();
                    
                    // Parse left side (e.g., "m.id")
                    String tableName = "";
                    String columnName = leftSide;
                    if (leftSide.contains(".")) {
                        String[] leftParts = leftSide.split("\\.", 2);
                        tableName = leftParts[0];
                        columnName = leftParts[1];
                    }
                    
                    // Parse right side (remove quotes if present)
                    String value = rightSide;
                    if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    // Create WHERE condition
                    WhereCondition condition = new WhereCondition();
                    condition.setTableName(tableName);
                    condition.setColumnName(columnName);
                    condition.setOperator(operator);
                    condition.setValue(value);
                    
                    return condition;
                }
            }
        }
        
        return null;
    }

    private List<OrderByColumn> parseOrderBy(List<OrderByElement> orderByElements) {
        List<OrderByColumn> orderByColumns = new ArrayList<>();
        
        if (orderByElements != null) {
            for (OrderByElement element : orderByElements) {
                OrderByColumn orderByColumn = new OrderByColumn();
                
                String expression = element.getExpression().toString();
                if (expression.contains(".")) {
                    String[] parts = expression.split("\\.", 2);
                    orderByColumn.setTableName(parts[0]);
                    orderByColumn.setColumnName(parts[1]);
                } else {
                    orderByColumn.setTableName("");
                    orderByColumn.setColumnName(expression);
                }
                
                orderByColumn.setDirection(element.isAsc() ? "ASC" : "DESC");
                orderByColumns.add(orderByColumn);
            }
        }
        
        return orderByColumns;
    }
}