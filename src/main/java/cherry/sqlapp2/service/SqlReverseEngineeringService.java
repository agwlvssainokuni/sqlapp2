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
 * SQLクエリのリバースエンジニアリング機能を提供するサービスクラス。
 * 既存のSQLクエリを解析してQueryStructureオブジェクトに変換し、
 * ビジュアルクエリビルダーで編集可能な形式に変換します。
 * 
 * 現在の制限事項:
 * - 基本的なSELECT、FROM、WHERE句のみサポート
 * - JOIN、サブクエリ、UNIONなどの複雑なSQL機能は一部制限あり
 * - 将来の機能拡張のための基盤として機能
 */
@Service
public class SqlReverseEngineeringService {

    private static final Logger logger = LoggerFactory.getLogger(SqlReverseEngineeringService.class);

    /**
     * SQLクエリを解析してQueryStructureに変換します。
     * 
     * @param sql 解析対象のSQLクエリ
     * @return 解析結果（成功時はクエリ構造、失敗時はエラー情報）
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

        // Parse GROUP BY
        GroupByElement groupByElement = plainSelect.getGroupBy();
        builder.groupByColumns(parseGroupBy(groupByElement));

        // Parse HAVING
        Expression havingExpression = plainSelect.getHaving();
        builder.havingConditions(parseWhereConditions(havingExpression));

        // Parse ORDER BY
        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
        builder.orderByColumns(parseOrderBy(orderByElements));

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
            // Try to parse complex expressions with AND/OR
            parseComplexWhereExpression(whereExpression, conditions, null);
        }
        
        return conditions;
    }
    
    /**
     * Parse complex WHERE expressions with AND/OR support.
     * Handles BETWEEN clauses correctly by protecting them from being split.
     */
    private void parseComplexWhereExpression(Expression expression, List<WhereCondition> conditions, String logicalOperator) {
        if (expression == null) {
            return;
        }
        
        String expressionString = expression.toString();
        
        // Handle OR expressions first (since they have lower precedence)
        if (expressionString.toUpperCase().contains(" OR ")) {
            List<String> orParts = splitRespectingBetween(expressionString, "OR");
            for (int i = 0; i < orParts.size(); i++) {
                String part = orParts.get(i).trim();
                // Recursively parse each OR part to handle nested AND conditions
                parseComplexWhereExpressionPart(part, conditions, i > 0 ? "OR" : logicalOperator);
            }
        }
        // Handle AND expressions
        else if (expressionString.toUpperCase().contains(" AND ")) {
            List<String> andParts = splitRespectingBetween(expressionString, "AND");
            for (int i = 0; i < andParts.size(); i++) {
                String part = andParts.get(i).trim();
                WhereCondition condition = parseSimpleWhereCondition(part);
                if (condition != null) {
                    if (i > 0) {
                        condition.setLogicalOperator("AND");
                    } else if (logicalOperator != null) {
                        condition.setLogicalOperator(logicalOperator);
                    }
                    conditions.add(condition);
                }
            }
        }
        // Single condition
        else {
            WhereCondition condition = parseSimpleWhereCondition(expressionString);
            if (condition != null) {
                if (logicalOperator != null) {
                    condition.setLogicalOperator(logicalOperator);
                }
                conditions.add(condition);
            }
        }
    }
    
    /**
     * Helper method to parse a part of WHERE expression that might contain AND.
     */
    private void parseComplexWhereExpressionPart(String part, List<WhereCondition> conditions, String logicalOperator) {
        if (part.toUpperCase().contains(" AND ")) {
            List<String> andParts = splitRespectingBetween(part, "AND");
            for (int i = 0; i < andParts.size(); i++) {
                String andPart = andParts.get(i).trim();
                WhereCondition condition = parseSimpleWhereCondition(andPart);
                if (condition != null) {
                    if (i > 0) {
                        condition.setLogicalOperator("AND");
                    } else if (logicalOperator != null) {
                        condition.setLogicalOperator(logicalOperator);
                    }
                    conditions.add(condition);
                }
            }
        } else {
            WhereCondition condition = parseSimpleWhereCondition(part);
            if (condition != null) {
                if (logicalOperator != null) {
                    condition.setLogicalOperator(logicalOperator);
                }
                conditions.add(condition);
            }
        }
    }
    
    /**
     * Split a string by AND/OR while respecting BETWEEN clauses.
     * BETWEEN clauses contain AND keywords that should not be used for splitting.
     */
    private List<String> splitRespectingBetween(String expression, String operator) {
        List<String> parts = new ArrayList<>();
        String upperExpression = expression.toUpperCase();
        String upperOperator = " " + operator.toUpperCase() + " ";
        
        int start = 0;
        int pos = 0;
        
        while (pos < expression.length()) {
            int operatorPos = upperExpression.indexOf(upperOperator, pos);
            if (operatorPos == -1) {
                // No more operators found
                parts.add(expression.substring(start));
                break;
            }
            
            // Check if this operator is inside a BETWEEN clause
            String beforeOperator = upperExpression.substring(start, operatorPos);
            
            // Count BETWEEN keywords that don't have matching AND
            int betweenCount = 0;
            int betweenPos = 0;
            while ((betweenPos = beforeOperator.indexOf(" BETWEEN ", betweenPos)) != -1) {
                betweenCount++;
                betweenPos += 9; // length of " BETWEEN "
            }
            
            // Count AND keywords (only for AND operator splitting)
            int andCount = 0;
            if ("AND".equals(operator)) {
                int andPos = 0;
                while ((andPos = beforeOperator.indexOf(" AND ", andPos)) != -1) {
                    andCount++;
                    andPos += 5; // length of " AND "
                }
            }
            
            // If we're splitting by AND and there are unmatched BETWEEN clauses, skip this AND
            if ("AND".equals(operator) && betweenCount > andCount) {
                pos = operatorPos + upperOperator.length();
                continue;
            }
            
            // This is a valid split point
            parts.add(expression.substring(start, operatorPos));
            start = operatorPos + upperOperator.length();
            pos = start;
        }
        
        return parts;
    }
    
    /**
     * Parse a simple WHERE condition string like "m.id = 'job_id'".
     */
    private WhereCondition parseSimpleWhereCondition(String conditionString) {
        if (conditionString == null || conditionString.trim().isEmpty()) {
            return null;
        }
        
        // Handle NULL operators first (they have no right-hand value)
        if (conditionString.toUpperCase().contains(" IS NULL")) {
            String leftSide = conditionString.replaceAll("(?i)\\s+IS\\s+NULL", "").trim();
            return createWhereCondition(leftSide, "IS NULL", null);
        }
        
        if (conditionString.toUpperCase().contains(" IS NOT NULL")) {
            String leftSide = conditionString.replaceAll("(?i)\\s+IS\\s+NOT\\s+NULL", "").trim();
            return createWhereCondition(leftSide, "IS NOT NULL", null);
        }
        
        // Handle BETWEEN operator first (special case with two values)
        if (conditionString.toUpperCase().contains(" BETWEEN ")) {
            String[] betweenParts = conditionString.split("(?i)\\s+BETWEEN\\s+", 2);
            if (betweenParts.length == 2) {
                String leftSide = betweenParts[0].trim();
                String rightSide = betweenParts[1].trim();
                
                // Parse the "value1 AND value2" part, being careful about AND keyword
                if (rightSide.toUpperCase().contains(" AND ")) {
                    // Find the last " AND " which should be the BETWEEN AND
                    int lastAndIndex = rightSide.toUpperCase().lastIndexOf(" AND ");
                    if (lastAndIndex > 0) {
                        String minValue = rightSide.substring(0, lastAndIndex).trim();
                        String maxValue = rightSide.substring(lastAndIndex + 5).trim(); // +5 for " AND "
                        
                        // Remove quotes if present
                        if (minValue.startsWith("'") && minValue.endsWith("'")) {
                            minValue = minValue.substring(1, minValue.length() - 1);
                        }
                        if (maxValue.startsWith("'") && maxValue.endsWith("'")) {
                            maxValue = maxValue.substring(1, maxValue.length() - 1);
                        }
                        
                        return createBetweenWhereCondition(leftSide, minValue, maxValue);
                    }
                }
            }
        }
        
        // Handle common operators with values
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
                    
                    // Parse right side (remove quotes if present)
                    String value = rightSide;
                    if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    return createWhereCondition(leftSide, operator, value);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Helper method to create WhereCondition from left side, operator, and value.
     */
    private WhereCondition createWhereCondition(String leftSide, String operator, String value) {
        // Parse left side (e.g., "m.id")
        String tableName = "";
        String columnName = leftSide;
        if (leftSide.contains(".")) {
            String[] leftParts = leftSide.split("\\.", 2);
            tableName = leftParts[0];
            columnName = leftParts[1];
        }
        
        // Create WHERE condition
        WhereCondition condition = new WhereCondition();
        condition.setTableName(tableName);
        condition.setColumnName(columnName);
        condition.setOperator(operator);
        condition.setValue(value);
        
        return condition;
    }
    
    /**
     * Helper method to create WhereCondition for BETWEEN operator with min and max values.
     */
    private WhereCondition createBetweenWhereCondition(String leftSide, String minValue, String maxValue) {
        // Parse left side (e.g., "m.id")
        String tableName = "";
        String columnName = leftSide;
        if (leftSide.contains(".")) {
            String[] leftParts = leftSide.split("\\.", 2);
            tableName = leftParts[0];
            columnName = leftParts[1];
        }
        
        // Create WHERE condition for BETWEEN
        WhereCondition condition = new WhereCondition();
        condition.setTableName(tableName);
        condition.setColumnName(columnName);
        condition.setOperator("BETWEEN");
        condition.setMinValue(minValue);
        condition.setMaxValue(maxValue);
        
        return condition;
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

    /**
     * Parse GROUP BY clause from JSqlParser GroupByElement.
     */
    private List<GroupByColumn> parseGroupBy(GroupByElement groupByElement) {
        List<GroupByColumn> groupByColumns = new ArrayList<>();
        
        if (groupByElement != null && groupByElement.getGroupByExpressionList() != null) {
            for (Object expressionObj : groupByElement.getGroupByExpressionList()) {
                if (expressionObj instanceof Expression) {
                    Expression expression = (Expression) expressionObj;
                    GroupByColumn groupByColumn = new GroupByColumn();
                    
                    String expressionString = expression.toString();
                    if (expressionString.contains(".")) {
                        String[] parts = expressionString.split("\\.", 2);
                        groupByColumn.setTableName(parts[0]);
                        groupByColumn.setColumnName(parts[1]);
                    } else {
                        groupByColumn.setTableName("");
                        groupByColumn.setColumnName(expressionString);
                    }
                    
                    groupByColumns.add(groupByColumn);
                }
            }
        }
        
        return groupByColumns;
    }
}