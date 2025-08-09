/*
 * Copyright 2025 SqlApp2
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

import cherry.sqlapp2.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class SchemaService {

    private final DynamicDataSourceService dynamicDataSourceService;

    @Autowired
    public SchemaService(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    /**
     * Get database schema information including tables and views
     */
    public Map<String, Object> getSchemaInfo(User user, Long connectionId) throws SQLException {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            Map<String, Object> schemaInfo = new LinkedHashMap<>();
            
            // Database basic info
            schemaInfo.put("databaseProductName", metaData.getDatabaseProductName());
            schemaInfo.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            schemaInfo.put("driverName", metaData.getDriverName());
            schemaInfo.put("driverVersion", metaData.getDriverVersion());
            
            // Get catalogs and schemas
            schemaInfo.put("catalogs", getCatalogs(metaData));
            schemaInfo.put("schemas", getSchemas(metaData));
            
            return schemaInfo;
        }
    }

    /**
     * Get table list for a specific database/schema
     */
    public List<Map<String, Object>> getTables(User user, Long connectionId, String catalog, String schema) throws SQLException {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<Map<String, Object>> tables = new ArrayList<>();
            
            String[] tableTypes = {"TABLE", "VIEW"};
            try (ResultSet rs = metaData.getTables(catalog, schema, null, tableTypes)) {
                while (rs.next()) {
                    Map<String, Object> table = new LinkedHashMap<>();
                    table.put("catalog", rs.getString("TABLE_CAT"));
                    table.put("schema", rs.getString("TABLE_SCHEM"));
                    table.put("name", rs.getString("TABLE_NAME"));
                    table.put("type", rs.getString("TABLE_TYPE"));
                    table.put("remarks", rs.getString("REMARKS"));
                    tables.add(table);
                }
            }
            
            return tables;
        }
    }

    /**
     * Get column information for a specific table
     */
    public List<Map<String, Object>> getTableColumns(User user, Long connectionId, String catalog, String schema, String tableName) throws SQLException {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<Map<String, Object>> columns = new ArrayList<>();
            
            try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
                while (rs.next()) {
                    Map<String, Object> column = new LinkedHashMap<>();
                    column.put("name", rs.getString("COLUMN_NAME"));
                    column.put("dataType", rs.getInt("DATA_TYPE"));
                    column.put("typeName", rs.getString("TYPE_NAME"));
                    column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                    column.put("decimalDigits", rs.getInt("DECIMAL_DIGITS"));
                    column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    column.put("defaultValue", rs.getString("COLUMN_DEF"));
                    column.put("ordinalPosition", rs.getInt("ORDINAL_POSITION"));
                    column.put("remarks", rs.getString("REMARKS"));
                    columns.add(column);
                }
            }
            
            return columns;
        }
    }

    /**
     * Get primary key information for a table
     */
    public List<Map<String, Object>> getPrimaryKeys(User user, Long connectionId, String catalog, String schema, String tableName) throws SQLException {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<Map<String, Object>> primaryKeys = new ArrayList<>();
            
            try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
                while (rs.next()) {
                    Map<String, Object> pk = new LinkedHashMap<>();
                    pk.put("columnName", rs.getString("COLUMN_NAME"));
                    pk.put("keySeq", rs.getShort("KEY_SEQ"));
                    pk.put("pkName", rs.getString("PK_NAME"));
                    primaryKeys.add(pk);
                }
            }
            
            return primaryKeys;
        }
    }

    /**
     * Get foreign key information for a table
     */
    public List<Map<String, Object>> getForeignKeys(User user, Long connectionId, String catalog, String schema, String tableName) throws SQLException {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<Map<String, Object>> foreignKeys = new ArrayList<>();
            
            try (ResultSet rs = metaData.getImportedKeys(catalog, schema, tableName)) {
                while (rs.next()) {
                    Map<String, Object> fk = new LinkedHashMap<>();
                    fk.put("pkTableCatalog", rs.getString("PKTABLE_CAT"));
                    fk.put("pkTableSchema", rs.getString("PKTABLE_SCHEM"));
                    fk.put("pkTableName", rs.getString("PKTABLE_NAME"));
                    fk.put("pkColumnName", rs.getString("PKCOLUMN_NAME"));
                    fk.put("fkTableCatalog", rs.getString("FKTABLE_CAT"));
                    fk.put("fkTableSchema", rs.getString("FKTABLE_SCHEM"));
                    fk.put("fkTableName", rs.getString("FKTABLE_NAME"));
                    fk.put("fkColumnName", rs.getString("FKCOLUMN_NAME"));
                    fk.put("keySeq", rs.getShort("KEY_SEQ"));
                    fk.put("fkName", rs.getString("FK_NAME"));
                    foreignKeys.add(fk);
                }
            }
            
            return foreignKeys;
        }
    }

    /**
     * Get index information for a table
     */
    public List<Map<String, Object>> getIndexes(User user, Long connectionId, String catalog, String schema, String tableName) throws SQLException {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<Map<String, Object>> indexes = new ArrayList<>();
            
            try (ResultSet rs = metaData.getIndexInfo(catalog, schema, tableName, false, false)) {
                while (rs.next()) {
                    // Skip table statistics
                    if (rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
                        continue;
                    }
                    
                    Map<String, Object> index = new LinkedHashMap<>();
                    index.put("indexName", rs.getString("INDEX_NAME"));
                    index.put("unique", !rs.getBoolean("NON_UNIQUE"));
                    index.put("columnName", rs.getString("COLUMN_NAME"));
                    index.put("ordinalPosition", rs.getShort("ORDINAL_POSITION"));
                    index.put("ascOrDesc", rs.getString("ASC_OR_DESC"));
                    indexes.add(index);
                }
            }
            
            return indexes;
        }
    }

    /**
     * Get complete table information including columns, primary keys, foreign keys, and indexes
     */
    public Map<String, Object> getTableDetails(User user, Long connectionId, String catalog, String schema, String tableName) throws SQLException {
        Map<String, Object> tableDetails = new LinkedHashMap<>();
        
        tableDetails.put("tableName", tableName);
        tableDetails.put("catalog", catalog);
        tableDetails.put("schema", schema);
        tableDetails.put("columns", getTableColumns(user, connectionId, catalog, schema, tableName));
        tableDetails.put("primaryKeys", getPrimaryKeys(user, connectionId, catalog, schema, tableName));
        tableDetails.put("foreignKeys", getForeignKeys(user, connectionId, catalog, schema, tableName));
        tableDetails.put("indexes", getIndexes(user, connectionId, catalog, schema, tableName));
        
        return tableDetails;
    }

    private List<String> getCatalogs(DatabaseMetaData metaData) throws SQLException {
        List<String> catalogs = new ArrayList<>();
        try (ResultSet rs = metaData.getCatalogs()) {
            while (rs.next()) {
                catalogs.add(rs.getString("TABLE_CAT"));
            }
        }
        return catalogs;
    }

    private List<Map<String, Object>> getSchemas(DatabaseMetaData metaData) throws SQLException {
        List<Map<String, Object>> schemas = new ArrayList<>();
        try (ResultSet rs = metaData.getSchemas()) {
            while (rs.next()) {
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("name", rs.getString("TABLE_SCHEM"));
                schema.put("catalog", rs.getString("TABLE_CATALOG"));
                schemas.add(schema);
            }
        }
        return schemas;
    }
}