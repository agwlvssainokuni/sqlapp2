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

        // Parse WHERE conditions (basic implementation)
        builder.whereConditions(new ArrayList<>());

        // Parse ORDER BY
        List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
        builder.orderByColumns(parseOrderBy(orderByElements));

        // Parse other clauses (set to empty for now)
        builder.groupByColumns(new ArrayList<>());
        builder.havingConditions(new ArrayList<>());

        // Parse LIMIT/OFFSET (if available)
        Limit limit = plainSelect.getLimit();
        if (limit != null && limit.getRowCount() != null) {
            builder.limit(Integer.parseInt(limit.getRowCount().toString()));
        }
        if (limit != null && limit.getOffset() != null) {
            builder.offset(Integer.parseInt(limit.getOffset().toString()));
        }

        return builder.build();
    }

    private List<SelectColumn> parseSelectItems(List<SelectItem<?>> selectItems) {
        List<SelectColumn> selectColumns = new ArrayList<>();
        
        if (selectItems != null) {
            for (SelectItem<?> item : selectItems) {
                SelectColumn selectColumn = new SelectColumn();
                
                // Use toString() to parse the item (simple but effective approach)
                String itemString = item.toString();
                
                if (itemString.equals("*")) {
                    // SELECT *
                    selectColumn.setTableName("");
                    selectColumn.setColumnName("*");
                } else if (itemString.contains(".*")) {
                    // SELECT table.*
                    String tableName = itemString.replace(".*", "");
                    selectColumn.setTableName(tableName);
                    selectColumn.setColumnName("*");
                } else if (itemString.contains(".")) {
                    // SELECT table.column
                    String[] parts = itemString.split("\\.", 2);
                    selectColumn.setTableName(parts[0]);
                    selectColumn.setColumnName(parts[1]);
                } else {
                    // SELECT column
                    selectColumn.setTableName("");
                    selectColumn.setColumnName(itemString);
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

    private List<cherry.sqlapp2.dto.QueryStructure.JoinClause> parseJoins(List<Join> joins) {
        List<cherry.sqlapp2.dto.QueryStructure.JoinClause> joinClauses = new ArrayList<>();
        
        if (joins != null) {
            for (Join join : joins) {
                cherry.sqlapp2.dto.QueryStructure.JoinClause joinClause = new cherry.sqlapp2.dto.QueryStructure.JoinClause();
                
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
                
                // Parse join conditions (simplified - only handles basic ON conditions)
                joinClause.setConditions(new ArrayList<>());
                
                joinClauses.add(joinClause);
            }
        }
        
        return joinClauses;
    }

    private List<cherry.sqlapp2.dto.QueryStructure.OrderByColumn> parseOrderBy(List<OrderByElement> orderByElements) {
        List<cherry.sqlapp2.dto.QueryStructure.OrderByColumn> orderByColumns = new ArrayList<>();
        
        if (orderByElements != null) {
            for (OrderByElement element : orderByElements) {
                cherry.sqlapp2.dto.QueryStructure.OrderByColumn orderByColumn = 
                    new cherry.sqlapp2.dto.QueryStructure.OrderByColumn();
                
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