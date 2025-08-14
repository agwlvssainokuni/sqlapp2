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

package cherry.sqlapp2.dto;

import java.util.ArrayList;
import java.util.List;

public class QueryStructure {
    
    private List<SelectColumn> selectColumns = new ArrayList<>();
    private boolean distinct = false;
    private List<FromTable> fromTables = new ArrayList<>();
    private List<JoinClause> joins = new ArrayList<>();
    private List<WhereCondition> whereConditions = new ArrayList<>();
    private List<GroupByColumn> groupByColumns = new ArrayList<>();
    private List<WhereCondition> havingConditions = new ArrayList<>();
    private List<OrderByColumn> orderByColumns = new ArrayList<>();
    private Integer limit;
    private Integer offset;

    public static class SelectColumn {
        private String tableName;
        private String columnName;
        private String alias;
        private String aggregateFunction; // COUNT, SUM, AVG, MAX, MIN, etc.
        private boolean distinct;

        // Constructors
        public SelectColumn() {}

        public SelectColumn(String tableName, String columnName) {
            this.tableName = tableName;
            this.columnName = columnName;
        }

        public SelectColumn(String tableName, String columnName, String aggregateFunction) {
            this.tableName = tableName;
            this.columnName = columnName;
            this.aggregateFunction = aggregateFunction;
        }

        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }
        
        public String getAggregateFunction() { return aggregateFunction; }
        public void setAggregateFunction(String aggregateFunction) { this.aggregateFunction = aggregateFunction; }
        
        public boolean isDistinct() { return distinct; }
        public void setDistinct(boolean distinct) { this.distinct = distinct; }
    }

    public static class FromTable {
        private String tableName;
        private String alias;

        public FromTable() {}

        public FromTable(String tableName) {
            this.tableName = tableName;
        }

        public FromTable(String tableName, String alias) {
            this.tableName = tableName;
            this.alias = alias;
        }

        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }
    }

    public static class JoinClause {
        private String joinType; // INNER, LEFT, RIGHT, FULL
        private String tableName;
        private String alias;
        private List<JoinCondition> conditions = new ArrayList<>();

        public static class JoinCondition {
            private String leftTable;
            private String leftColumn;
            private String operator; // =, <>, <, >, <=, >=
            private String rightTable;
            private String rightColumn;

            public JoinCondition() {}

            public JoinCondition(String leftTable, String leftColumn, String operator, 
                                String rightTable, String rightColumn) {
                this.leftTable = leftTable;
                this.leftColumn = leftColumn;
                this.operator = operator;
                this.rightTable = rightTable;
                this.rightColumn = rightColumn;
            }

            // Getters and Setters
            public String getLeftTable() { return leftTable; }
            public void setLeftTable(String leftTable) { this.leftTable = leftTable; }
            
            public String getLeftColumn() { return leftColumn; }
            public void setLeftColumn(String leftColumn) { this.leftColumn = leftColumn; }
            
            public String getOperator() { return operator; }
            public void setOperator(String operator) { this.operator = operator; }
            
            public String getRightTable() { return rightTable; }
            public void setRightTable(String rightTable) { this.rightTable = rightTable; }
            
            public String getRightColumn() { return rightColumn; }
            public void setRightColumn(String rightColumn) { this.rightColumn = rightColumn; }
        }

        // Constructors
        public JoinClause() {}

        public JoinClause(String joinType, String tableName) {
            this.joinType = joinType;
            this.tableName = tableName;
        }

        // Getters and Setters
        public String getJoinType() { return joinType; }
        public void setJoinType(String joinType) { this.joinType = joinType; }
        
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }
        
        public List<JoinCondition> getConditions() { return conditions; }
        public void setConditions(List<JoinCondition> conditions) { this.conditions = conditions; }
    }

    public static class WhereCondition {
        private String tableName;
        private String columnName;
        private String operator; // =, <>, <, >, <=, >=, LIKE, IN, BETWEEN, IS NULL, IS NOT NULL
        private String value;
        private List<String> values; // For IN operator
        private String logicalOperator; // AND, OR
        private boolean negated; // NOT

        public WhereCondition() {}

        public WhereCondition(String tableName, String columnName, String operator, String value) {
            this.tableName = tableName;
            this.columnName = columnName;
            this.operator = operator;
            this.value = value;
        }

        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public List<String> getValues() { return values; }
        public void setValues(List<String> values) { this.values = values; }
        
        public String getLogicalOperator() { return logicalOperator; }
        public void setLogicalOperator(String logicalOperator) { this.logicalOperator = logicalOperator; }
        
        public boolean isNegated() { return negated; }
        public void setNegated(boolean negated) { this.negated = negated; }
    }

    public static class GroupByColumn {
        private String tableName;
        private String columnName;

        public GroupByColumn() {}

        public GroupByColumn(String tableName, String columnName) {
            this.tableName = tableName;
            this.columnName = columnName;
        }

        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
    }

    public static class OrderByColumn {
        private String tableName;
        private String columnName;
        private String direction; // ASC, DESC

        public OrderByColumn() {}

        public OrderByColumn(String tableName, String columnName, String direction) {
            this.tableName = tableName;
            this.columnName = columnName;
            this.direction = direction;
        }

        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
    }

    // Main class getters and setters
    public List<SelectColumn> getSelectColumns() { return selectColumns; }
    public void setSelectColumns(List<SelectColumn> selectColumns) { this.selectColumns = selectColumns; }
    
    public List<FromTable> getFromTables() { return fromTables; }
    public void setFromTables(List<FromTable> fromTables) { this.fromTables = fromTables; }
    
    public List<JoinClause> getJoins() { return joins; }
    public void setJoins(List<JoinClause> joins) { this.joins = joins; }
    
    public List<WhereCondition> getWhereConditions() { return whereConditions; }
    public void setWhereConditions(List<WhereCondition> whereConditions) { this.whereConditions = whereConditions; }
    
    public List<GroupByColumn> getGroupByColumns() { return groupByColumns; }
    public void setGroupByColumns(List<GroupByColumn> groupByColumns) { this.groupByColumns = groupByColumns; }
    
    public List<WhereCondition> getHavingConditions() { return havingConditions; }
    public void setHavingConditions(List<WhereCondition> havingConditions) { this.havingConditions = havingConditions; }
    
    public List<OrderByColumn> getOrderByColumns() { return orderByColumns; }
    public void setOrderByColumns(List<OrderByColumn> orderByColumns) { this.orderByColumns = orderByColumns; }
    
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    
    public Integer getOffset() { return offset; }
    public void setOffset(Integer offset) { this.offset = offset; }
    
    public boolean isDistinct() { return distinct; }
    public void setDistinct(boolean distinct) { this.distinct = distinct; }

    // Builder pattern for convenience
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private QueryStructure queryStructure = new QueryStructure();

        public Builder selectColumns(List<SelectColumn> selectColumns) {
            queryStructure.selectColumns = selectColumns;
            return this;
        }

        public Builder distinct(boolean distinct) {
            queryStructure.distinct = distinct;
            return this;
        }

        public Builder fromTables(List<FromTable> fromTables) {
            queryStructure.fromTables = fromTables;
            return this;
        }

        public Builder joins(List<JoinClause> joins) {
            queryStructure.joins = joins;
            return this;
        }

        public Builder whereConditions(List<WhereCondition> whereConditions) {
            queryStructure.whereConditions = whereConditions;
            return this;
        }

        public Builder groupByColumns(List<GroupByColumn> groupByColumns) {
            queryStructure.groupByColumns = groupByColumns;
            return this;
        }

        public Builder havingConditions(List<WhereCondition> havingConditions) {
            queryStructure.havingConditions = havingConditions;
            return this;
        }

        public Builder orderByColumns(List<OrderByColumn> orderByColumns) {
            queryStructure.orderByColumns = orderByColumns;
            return this;
        }

        public Builder limit(Integer limit) {
            queryStructure.limit = limit;
            return this;
        }

        public Builder offset(Integer offset) {
            queryStructure.offset = offset;
            return this;
        }

        public QueryStructure build() {
            return queryStructure;
        }
    }
}