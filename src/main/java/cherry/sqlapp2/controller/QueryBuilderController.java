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
import cherry.sqlapp2.service.QueryBuilderService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query-builder")
public class QueryBuilderController {

    @Autowired
    private QueryBuilderService queryBuilderService;

    /**
     * Build SQL query from structure
     */
    @PostMapping("/build")
    public ApiResponse<QueryBuilderResponse> buildQuery(
            @Valid @RequestBody QueryBuilderRequest request,
            Authentication authentication) {
        
        QueryBuilderResponse response = queryBuilderService.buildQuery(request);
        return ApiResponse.success(response);
    }

}