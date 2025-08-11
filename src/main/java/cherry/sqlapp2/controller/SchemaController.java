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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final SchemaService schemaService;
    private final UserService userService;

    @Autowired
    public SchemaController(SchemaService schemaService, UserService userService) {
        this.schemaService = schemaService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    /**
     * Get database schema information
     */
    @GetMapping("/connections/{connectionId}")
    public ResponseEntity<?> getSchemaInfo(@PathVariable Long connectionId) {
        try {
            User currentUser = getCurrentUser();
            SchemaInfoResponse schemaInfo = schemaService.getSchemaInfo(currentUser, connectionId);
            return ResponseEntity.ok(schemaInfo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Schema info retrieval failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Schema info retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Get tables for a specific catalog/schema
     */
    @GetMapping("/connections/{connectionId}/tables")
    public ResponseEntity<?> getTables(@PathVariable Long connectionId,
                                     @RequestParam(required = false) String catalog,
                                     @RequestParam(required = false) String schema) {
        try {
            User currentUser = getCurrentUser();
            List<TableInfoResponse> tables = schemaService.getTables(currentUser, connectionId, catalog, schema);
            return ResponseEntity.ok(tables);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Table list retrieval failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Table list retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Get table details including columns, primary keys, foreign keys, and indexes
     */
    @GetMapping("/connections/{connectionId}/tables/{tableName}")
    public ResponseEntity<?> getTableDetails(@PathVariable Long connectionId,
                                           @PathVariable String tableName,
                                           @RequestParam(required = false) String catalog,
                                           @RequestParam(required = false) String schema) {
        try {
            User currentUser = getCurrentUser();
            TableDetailsResponse tableDetails = schemaService.getTableDetails(currentUser, connectionId, catalog, schema, tableName);
            return ResponseEntity.ok(tableDetails);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Table details retrieval failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Table details retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Get columns for a specific table
     */
    @GetMapping("/connections/{connectionId}/tables/{tableName}/columns")
    public ResponseEntity<?> getTableColumns(@PathVariable Long connectionId,
                                           @PathVariable String tableName,
                                           @RequestParam(required = false) String catalog,
                                           @RequestParam(required = false) String schema) {
        try {
            User currentUser = getCurrentUser();
            List<ColumnInfoResponse> columns = schemaService.getTableColumns(currentUser, connectionId, catalog, schema, tableName);
            return ResponseEntity.ok(columns);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Column list retrieval failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Column list retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Get primary keys for a specific table
     */
    @GetMapping("/connections/{connectionId}/tables/{tableName}/primary-keys")
    public ResponseEntity<?> getPrimaryKeys(@PathVariable Long connectionId,
                                          @PathVariable String tableName,
                                          @RequestParam(required = false) String catalog,
                                          @RequestParam(required = false) String schema) {
        try {
            User currentUser = getCurrentUser();
            List<PrimaryKeyInfoResponse> primaryKeys = schemaService.getPrimaryKeys(currentUser, connectionId, catalog, schema, tableName);
            return ResponseEntity.ok(primaryKeys);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Primary key retrieval failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Primary key retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Get foreign keys for a specific table
     */
    @GetMapping("/connections/{connectionId}/tables/{tableName}/foreign-keys")
    public ResponseEntity<?> getForeignKeys(@PathVariable Long connectionId,
                                          @PathVariable String tableName,
                                          @RequestParam(required = false) String catalog,
                                          @RequestParam(required = false) String schema) {
        try {
            User currentUser = getCurrentUser();
            List<ForeignKeyInfoResponse> foreignKeys = schemaService.getForeignKeys(currentUser, connectionId, catalog, schema, tableName);
            return ResponseEntity.ok(foreignKeys);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Foreign key retrieval failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Foreign key retrieval failed: " + e.getMessage());
        }
    }

    /**
     * Get indexes for a specific table
     */
    @GetMapping("/connections/{connectionId}/tables/{tableName}/indexes")
    public ResponseEntity<?> getIndexes(@PathVariable Long connectionId,
                                      @PathVariable String tableName,
                                      @RequestParam(required = false) String catalog,
                                      @RequestParam(required = false) String schema) {
        try {
            User currentUser = getCurrentUser();
            List<IndexInfoResponse> indexes = schemaService.getIndexes(currentUser, connectionId, catalog, schema, tableName);
            return ResponseEntity.ok(indexes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Index retrieval failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Index retrieval failed: " + e.getMessage());
        }
    }
}