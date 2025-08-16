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

package cherry.sqlapp2.controller;

import cherry.sqlapp2.dto.ApiResponse;
import cherry.sqlapp2.dto.QueryBuilderRequest;
import cherry.sqlapp2.dto.QueryBuilderResponse;
import cherry.sqlapp2.dto.SqlParseResult;
import cherry.sqlapp2.service.QueryBuilderService;
import cherry.sqlapp2.service.SqlReverseEngineeringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ビジュアルクエリビルダーの機能を提供するコントローラクラス。
 * SQLクエリの生成とSQLからクエリ構造への逆変換機能を提供します。
 * ユーザはGUIを通じてSQLクエリを構築し、既存のSQLを解析してクエリ構造として読み込むことができます。
 */
@RestController
@RequestMapping("/api/query-builder")
@Tag(name = "Query Builder", description = "Visual SQL query building operations")
@SecurityRequirement(name = "bearerAuth")
public class QueryBuilderController {

    private final QueryBuilderService queryBuilderService;
    private final SqlReverseEngineeringService sqlReverseEngineeringService;

    @Autowired
    public QueryBuilderController(
            QueryBuilderService queryBuilderService,
            SqlReverseEngineeringService sqlReverseEngineeringService
    ) {
        this.queryBuilderService = queryBuilderService;
        this.sqlReverseEngineeringService = sqlReverseEngineeringService;
    }

    /**
     * クエリ構造からSQLクエリを生成します。
     * SELECT、FROM、WHERE句などの構造化されたクエリ情報から実行可能なSQLを作成します。
     *
     * @param request クエリ構造を含むリクエスト
     * @return 生成されたSQLクエリを含むAPIレスポンス
     */
    @Operation(
            summary = "Build SQL query from structure",
            description = "Generate SQL query from structured query components (SELECT, FROM, WHERE, etc.)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Query structure with SELECT, FROM, WHERE, and other SQL clauses",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "selectClause": "id, name, email",
                                                "fromClause": "users",
                                                "whereClause": "active = true AND created_date > :startDate",
                                                "orderByClause": "created_date DESC",
                                                "limitClause": "50"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SQL query built successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid query structure or syntax error"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @PostMapping("/build")
    public ApiResponse<QueryBuilderResponse> buildQuery(
            @Valid @RequestBody QueryBuilderRequest request
    ) {

        QueryBuilderResponse response = queryBuilderService.buildQuery(request);
        return ApiResponse.success(response);
    }

    /**
     * SQLクエリを解析してクエリ構造に変換します。
     * 既存のSELECT文を解析し、ビジュアルクエリビルダーで編集可能な構造に変換します。
     *
     * @param request 解析対象のSQLクエリを含むリクエスト
     * @return 解析結果（成功時はクエリ構造、失敗時はエラー詳細）を含むAPIレスポンス
     */
    @Operation(
            summary = "Parse SQL query into structure",
            description = "Reverse engineer SQL SELECT statement into visual query builder structure",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "SQL query to parse",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "sql": "SELECT u.id, u.name, p.title FROM users u LEFT JOIN posts p ON u.id = p.user_id WHERE u.active = true ORDER BY u.created_date DESC LIMIT 10"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SQL parsed successfully or parsing failed with error details"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request format"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @PostMapping("/parse")
    public ApiResponse<SqlParseResult> parseSQL(
            @RequestBody SqlParseRequest request
    ) {
        SqlParseResult result = sqlReverseEngineeringService.parseSQL(request.sql());
        return ApiResponse.success(result);
    }

    /**
     * Request DTO for SQL parsing endpoint.
     */
    public record SqlParseRequest(String sql) {
    }

}
