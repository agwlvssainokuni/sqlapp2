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

import cherry.sqlapp2.dto.*;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.exception.DatabaseConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * データベーススキーマ情報の取得機能を提供するサービスクラス。
 * データベースのメタデータ（テーブル、カラム、インデックス、外部キーなど）を
 * 取得し、クエリビルダーやスキーマブラウザで使用する情報を提供します。
 */
@Service
public class SchemaService {

    private final DynamicDataSourceService dynamicDataSourceService;

    @Autowired
    public SchemaService(
            DynamicDataSourceService dynamicDataSourceService
    ) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    /**
     * データベースのスキーマ情報（テーブルとビューを含む）を取得します。
     * 
     * @param user ユーザ情報
     * @param connectionId データベース接続ID
     * @return データベース情報オブジェクト
     */
    public DatabaseInfo getDatabaseInfo(User user, Long connectionId) {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();

            return new DatabaseInfo(
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion(),
                    metaData.getDriverName(),
                    metaData.getDriverVersion(),
                    getCatalogs(metaData),
                    getSchemaDetails(metaData)
            );
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Database operation failed", e);
        }
    }

    /**
     * Get table list for a specific database/schema
     */
    public List<TableInfo> getTables(User user, Long connectionId, String catalog, String schema) {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<TableInfo> tables = new ArrayList<>();

            String[] tableTypes = {"TABLE", "VIEW"};
            try (ResultSet rs = metaData.getTables(catalog, schema, null, tableTypes)) {
                while (rs.next()) {
                    tables.add(new TableInfo(
                            rs.getString("TABLE_CAT"),
                            rs.getString("TABLE_SCHEM"),
                            rs.getString("TABLE_NAME"),
                            rs.getString("TABLE_TYPE"),
                            rs.getString("REMARKS")
                    ));
                }
            }

            return tables;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Database operation failed", e);
        }
    }

    /**
     * Get column information for a specific table
     */
    public List<ColumnInfo> getTableColumns(User user, Long connectionId, String catalog, String schema, String tableName) {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<ColumnInfo> columns = new ArrayList<>();

            try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
                while (rs.next()) {
                    columns.add(new ColumnInfo(
                            rs.getString("COLUMN_NAME"),
                            rs.getInt("DATA_TYPE"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("COLUMN_SIZE"),
                            rs.getInt("DECIMAL_DIGITS"),
                            rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable,
                            rs.getString("COLUMN_DEF"),
                            rs.getInt("ORDINAL_POSITION"),
                            rs.getString("REMARKS")
                    ));
                }
            }

            return columns;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Database operation failed", e);
        }
    }

    /**
     * Get primary key information for a table
     */
    public List<PrimaryKeyInfo> getPrimaryKeys(User user, Long connectionId, String catalog, String schema, String tableName) {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<PrimaryKeyInfo> primaryKeys = new ArrayList<>();

            try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
                while (rs.next()) {
                    primaryKeys.add(new PrimaryKeyInfo(
                            rs.getString("COLUMN_NAME"),
                            rs.getShort("KEY_SEQ"),
                            rs.getString("PK_NAME")
                    ));
                }
            }

            return primaryKeys;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Database operation failed", e);
        }
    }

    /**
     * Get foreign key information for a table
     */
    public List<ForeignKeyInfo> getForeignKeys(User user, Long connectionId, String catalog, String schema, String tableName) {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<ForeignKeyInfo> foreignKeys = new ArrayList<>();

            try (ResultSet rs = metaData.getImportedKeys(catalog, schema, tableName)) {
                while (rs.next()) {
                    foreignKeys.add(new ForeignKeyInfo(
                            rs.getString("PKTABLE_CAT"),
                            rs.getString("PKTABLE_SCHEM"),
                            rs.getString("PKTABLE_NAME"),
                            rs.getString("PKCOLUMN_NAME"),
                            rs.getString("FKTABLE_CAT"),
                            rs.getString("FKTABLE_SCHEM"),
                            rs.getString("FKTABLE_NAME"),
                            rs.getString("FKCOLUMN_NAME"),
                            rs.getShort("KEY_SEQ"),
                            rs.getString("FK_NAME")
                    ));
                }
            }

            return foreignKeys;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Database operation failed", e);
        }
    }

    /**
     * Get index information for a table
     */
    public List<IndexInfo> getIndexes(User user, Long connectionId, String catalog, String schema, String tableName) {
        try (Connection connection = dynamicDataSourceService.getConnection(user, connectionId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<IndexInfo> indexes = new ArrayList<>();

            try (ResultSet rs = metaData.getIndexInfo(catalog, schema, tableName, false, false)) {
                while (rs.next()) {
                    // Skip table statistics
                    if (rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
                        continue;
                    }

                    indexes.add(new IndexInfo(
                            rs.getString("INDEX_NAME"),
                            !rs.getBoolean("NON_UNIQUE"),
                            rs.getString("COLUMN_NAME"),
                            rs.getShort("ORDINAL_POSITION"),
                            rs.getString("ASC_OR_DESC")
                    ));
                }
            }

            return indexes;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Database operation failed", e);
        }
    }

    /**
     * Get complete table information including columns, primary keys, foreign keys, and indexes
     */
    public TableDetails getTableDetails(User user, Long connectionId, String catalog, String schema, String tableName) {
        return new TableDetails(
                tableName,
                catalog,
                schema,
                getTableColumns(user, connectionId, catalog, schema, tableName),
                getPrimaryKeys(user, connectionId, catalog, schema, tableName),
                getForeignKeys(user, connectionId, catalog, schema, tableName),
                getIndexes(user, connectionId, catalog, schema, tableName)
        );
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

    private List<DatabaseInfo.SchemaDetail> getSchemaDetails(DatabaseMetaData metaData) throws SQLException {
        List<DatabaseInfo.SchemaDetail> schemas = new ArrayList<>();
        try (ResultSet rs = metaData.getSchemas()) {
            while (rs.next()) {
                schemas.add(new DatabaseInfo.SchemaDetail(
                        rs.getString("TABLE_SCHEM"),
                        rs.getString("TABLE_CATALOG")
                ));
            }
        }
        return schemas;
    }
}