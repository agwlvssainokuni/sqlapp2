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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * データベーススキーマ情報の取得機能を提供するコントローラクラス。
 * データベースのテーブル、カラム、インデックス、外部キーなどの
 * メタデータ情報を取得するエンドポイントを提供します。
 */
@RestController
@RequestMapping("/api/schema")
@Tag(name = "Database Schema", description = "Database schema information and metadata operations")
@SecurityRequirement(name = "bearerAuth")
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
     * データベース情報を取得します。
     * 指定されたデータベース接続のカタログやスキーマ情報を取得します。
     * 
     * @param connectionId データベース接続ID
     * @param authentication 認証情報
     * @return データベース情報を含むAPIレスポンス
     */
    @Operation(
            summary = "Get database information",
            description = "Retrieve database metadata including catalogs and schemas for a specific connection"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Database information retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Database connection error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/connections/{connectionId}")
    public ApiResponse<DatabaseInfo> getDatabaseInfo(
            @Parameter(description = "Database connection ID", required = true) @PathVariable Long connectionId,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        DatabaseInfo databaseInfo = schemaService.getDatabaseInfo(currentUser, connectionId);
        return ApiResponse.success(databaseInfo);
    }

    @Operation(
            summary = "Get database tables",
            description = "Retrieve list of tables for a specific catalog/schema in the database"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tables retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Database connection error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/connections/{connectionId}/tables")
    public ApiResponse<List<TableInfo>> getTables(
            @Parameter(description = "Database connection ID", required = true) @PathVariable Long connectionId,
            @Parameter(description = "Database catalog name") @RequestParam(required = false) String catalog,
            @Parameter(description = "Database schema name") @RequestParam(required = false) String schema,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        List<TableInfo> tables = schemaService.getTables(currentUser, connectionId, catalog, schema);
        return ApiResponse.success(tables);
    }

    @Operation(
            summary = "Get table details",
            description = "Retrieve detailed table information including columns, primary keys, foreign keys, and indexes"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Table details retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Table not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Database connection error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/connections/{connectionId}/tables/{tableName}")
    public ApiResponse<TableDetails> getTableDetails(
            @Parameter(description = "Database connection ID", required = true) @PathVariable Long connectionId,
            @Parameter(description = "Table name", required = true) @PathVariable String tableName,
            @Parameter(description = "Database catalog name") @RequestParam(required = false) String catalog,
            @Parameter(description = "Database schema name") @RequestParam(required = false) String schema,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        TableDetails tableDetails = schemaService.getTableDetails(currentUser, connectionId, catalog, schema, tableName);
        return ApiResponse.success(tableDetails);
    }

    @Operation(
            summary = "Get table columns",
            description = "Retrieve column information for a specific table including data types and constraints"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Table columns retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Table not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Database connection error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/connections/{connectionId}/tables/{tableName}/columns")
    public ApiResponse<List<ColumnInfo>> getTableColumns(
            @Parameter(description = "Database connection ID", required = true) @PathVariable Long connectionId,
            @Parameter(description = "Table name", required = true) @PathVariable String tableName,
            @Parameter(description = "Database catalog name") @RequestParam(required = false) String catalog,
            @Parameter(description = "Database schema name") @RequestParam(required = false) String schema,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        List<ColumnInfo> columns = schemaService.getTableColumns(currentUser, connectionId, catalog, schema, tableName);
        return ApiResponse.success(columns);
    }

}