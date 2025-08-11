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
package cherry.sqlapp2.controller;

import cherry.sqlapp2.dto.*;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.service.SchemaService;
import cherry.sqlapp2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final SchemaService schemaService;
    private final UserService userService;

    @Autowired
    public SchemaController(
            SchemaService schemaService,
            UserService userService
    ) {
        this.schemaService = schemaService;
        this.userService = userService;
    }

    private User getCurrentUser(Authentication authentication) {
        return Optional.of(authentication)
                .map(Authentication::getName)
                .flatMap(userService::findByUsername)
                .get();
    }

    /**
     * Get database schema information
     */
    @GetMapping("/connections/{connectionId}")
    public ApiResponse<DatabaseInfo> getDatabaseInfo(
            @PathVariable Long connectionId,
            Authentication authentication
    ) throws SQLException {
        User currentUser = getCurrentUser(authentication);
        DatabaseInfo databaseInfo = schemaService.getDatabaseInfo(currentUser, connectionId);
        return ApiResponse.success(databaseInfo);
    }

    /**
     * Get tables for a specific catalog/schema
     */
    @GetMapping("/connections/{connectionId}/tables")
    public ApiResponse<List<TableInfo>> getTables(
            @PathVariable Long connectionId,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema,
            Authentication authentication
    ) throws SQLException {
        User currentUser = getCurrentUser(authentication);
        List<TableInfo> tables = schemaService.getTables(currentUser, connectionId, catalog, schema);
        return ApiResponse.success(tables);
    }

    /**
     * Get table details including columns, primary keys, foreign keys, and indexes
     */
    @GetMapping("/connections/{connectionId}/tables/{tableName}")
    public ApiResponse<TableDetails> getTableDetails(
            @PathVariable Long connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema,
            Authentication authentication
    ) throws SQLException {
        User currentUser = getCurrentUser(authentication);
        TableDetails tableDetails = schemaService.getTableDetails(currentUser, connectionId, catalog, schema, tableName);
        return ApiResponse.success(tableDetails);
    }

    /**
     * Get columns for a specific table
     */
    @GetMapping("/connections/{connectionId}/tables/{tableName}/columns")
    public ApiResponse<List<ColumnInfo>> getTableColumns(
            @PathVariable Long connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema,
            Authentication authentication
    ) throws SQLException {
        User currentUser = getCurrentUser(authentication);
        List<ColumnInfo> columns = schemaService.getTableColumns(currentUser, connectionId, catalog, schema, tableName);
        return ApiResponse.success(columns);
    }

    /**
     * Get primary keys for a specific table
     */
    @Deprecated
    @GetMapping("/connections/{connectionId}/tables/{tableName}/primary-keys")
    public ApiResponse<List<PrimaryKeyInfo>> getPrimaryKeys(
            @PathVariable Long connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema,
            Authentication authentication
    ) throws SQLException {
        User currentUser = getCurrentUser(authentication);
        List<PrimaryKeyInfo> primaryKeys = schemaService.getPrimaryKeys(currentUser, connectionId, catalog, schema, tableName);
        return ApiResponse.success(primaryKeys);
    }

    /**
     * Get foreign keys for a specific table
     */
    @Deprecated
    @GetMapping("/connections/{connectionId}/tables/{tableName}/foreign-keys")
    public ApiResponse<List<ForeignKeyInfo>> getForeignKeys(
            @PathVariable Long connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema,
            Authentication authentication
    ) throws SQLException {
        User currentUser = getCurrentUser(authentication);
        List<ForeignKeyInfo> foreignKeys = schemaService.getForeignKeys(currentUser, connectionId, catalog, schema, tableName);
        return ApiResponse.success(foreignKeys);
    }

    /**
     * Get indexes for a specific table
     */
    @Deprecated
    @GetMapping("/connections/{connectionId}/tables/{tableName}/indexes")
    public ApiResponse<List<IndexInfo>> getIndexes(
            @PathVariable Long connectionId,
            @PathVariable String tableName,
            @RequestParam(required = false) String catalog,
            @RequestParam(required = false) String schema,
            Authentication authentication
    ) throws SQLException {
        User currentUser = getCurrentUser(authentication);
        List<IndexInfo> indexes = schemaService.getIndexes(currentUser, connectionId, catalog, schema, tableName);
        return ApiResponse.success(indexes);
    }
}