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
import cherry.sqlapp2.dto.HealthcheckResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * アプリケーションのヘルスチェック機能を提供するコントローラクラス。
 * システムの稼働状態や正常性を監視するためのエンドポイントを提供します。
 * 負荷分散器やモニタリングツールによる死活監視に使用されます。
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health Check", description = "Application health monitoring endpoints")
public class HealthController {

    /**
     * アプリケーションのヘルスチェックを実行します。
     * システムの稼働状態と現在時刻を返します。
     *
     * @return アプリケーションの健康状態を含むAPIレスポンス
     */
    @Operation(
            summary = "Health check",
            description = "Check application health status"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Application is healthy",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/health")
    public ApiResponse<HealthcheckResult> health() {
        HealthcheckResult response = new HealthcheckResult(
                "UP",
                LocalDateTime.now(),
                "SqlApp2"
        );
        return ApiResponse.success(response);
    }
}
