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

import cherry.sqlapp2.dto.*;
import cherry.sqlapp2.entity.DatabaseConnection;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.repository.DatabaseConnectionRepository;
import cherry.sqlapp2.repository.UserRepository;
import cherry.sqlapp2.service.QueryManagementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/queries")
@Tag(name = "Query Management", description = "SQL query execution, saving, and management operations")
@SecurityRequirement(name = "bearerAuth")
public class QueryController {

    private final QueryManagementService queryManagementService;

    private final UserRepository userRepository;

    private final DatabaseConnectionRepository connectionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public QueryController(
            QueryManagementService queryManagementService,
            UserRepository userRepository,
            DatabaseConnectionRepository connectionRepository
    ) {
        this.queryManagementService = queryManagementService;
        this.userRepository = userRepository;
        this.connectionRepository = connectionRepository;
    }

    // ==================== Saved Queries Endpoints ====================

    @PostMapping("/saved")
    public ResponseEntity<ApiResponse<SavedQuery>> saveQuery(
            @Valid @RequestBody SavedQueryRequest request,
            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);

        DatabaseConnection defaultConnection = Optional.ofNullable(request.getDefaultConnectionId())
                .flatMap(connectionId ->
                        connectionRepository.findByUserAndId(user, connectionId)
                ).orElse(null);

        cherry.sqlapp2.entity.SavedQuery savedQuery = queryManagementService.saveQuery(
                request.getName(),
                request.getSqlContent(),
                request.getDescription(),
                request.getParameterDefinitions(),
                request.getSharingScope(),
                user,
                defaultConnection
        );

        SavedQuery response = createSavedQueryDto(savedQuery);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(response)
        );
    }

    @GetMapping("/saved/{queryId}")
    public ApiResponse<SavedQuery> getSavedQuery(
            @PathVariable Long queryId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        cherry.sqlapp2.entity.SavedQuery savedQuery = queryManagementService.getAccessibleQuery(
                queryId,
                user
        ).get();

        SavedQuery response = createSavedQueryDto(savedQuery);
        return ApiResponse.success(response);
    }

    @PutMapping("/saved/{queryId}")
    public ApiResponse<SavedQuery> updateSavedQuery(
            @PathVariable Long queryId,
            @Valid @RequestBody SavedQueryRequest request,
            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);
        DatabaseConnection defaultConnection = null;

        if (request.getDefaultConnectionId() != null) {
            defaultConnection = connectionRepository.findByUserAndId(user, request.getDefaultConnectionId())
                    .orElseThrow(() -> new RuntimeException("Database connection not found"));
        }

        cherry.sqlapp2.entity.SavedQuery savedQuery = queryManagementService.updateSavedQuery(
                queryId,
                request.getName(),
                request.getSqlContent(),
                request.getDescription(),
                request.getParameterDefinitions(),
                request.getSharingScope(),
                defaultConnection,
                user
        );

        SavedQuery response = createSavedQueryDto(savedQuery);
        return ApiResponse.success(response);
    }

    @GetMapping("/saved")
    public ApiResponse<List<SavedQuery>> getUserSavedQueries(
            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);
        List<cherry.sqlapp2.entity.SavedQuery> savedQueries = queryManagementService.getUserQueries(user);

        List<SavedQuery> response = savedQueries.stream()
                .map(this::createSavedQueryDto)
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    @DeleteMapping("/saved/{queryId}")
    public ApiResponse<Void> deleteSavedQuery(
            @PathVariable Long queryId,
            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);
        queryManagementService.deleteSavedQuery(queryId, user);
        return ApiResponse.success(null);
    }

    @GetMapping("/public")
    public ApiResponse<List<SavedQuery>> getPublicQueries() {
        List<cherry.sqlapp2.entity.SavedQuery> publicQueries = queryManagementService.getPublicQueries();

        List<SavedQuery> response = publicQueries.stream()
                .map(this::createSavedQueryDto)
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }


    // ==================== Query History Endpoints ====================

    @GetMapping("/history")
    public ApiResponse<Page<QueryHistory>> getQueryHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        var history = queryManagementService.getUserQueryHistory(user, pageable);

        var response = history.map(this::createQueryHistoryDto);
        return ApiResponse.success(response);
    }

    @GetMapping("/history/{historyId}")
    public ApiResponse<QueryHistory> getQueryHistoryById(
            @PathVariable Long historyId,
            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);
        var queryHistory = queryManagementService.getQueryHistoryById(historyId, user)
                .orElseThrow(() -> new RuntimeException("Query history not found or not accessible"));

        QueryHistory response = createQueryHistoryDto(queryHistory);
        return ApiResponse.success(response);
    }

    @GetMapping("/history/search")
    public ApiResponse<Page<QueryHistory>> searchQueryHistory(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        var searchResults = queryManagementService.searchQueryHistory(user, searchTerm, pageable);

        var response = searchResults.map(this::createQueryHistoryDto);
        return ApiResponse.success(response);
    }

    @GetMapping("/history/successful")
    public ApiResponse<Page<QueryHistory>> getSuccessfulQueries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        var successfulQueries = queryManagementService.getSuccessfulQueries(user, pageable);

        var response = successfulQueries.map(this::createQueryHistoryDto);
        return ApiResponse.success(response);
    }

    @GetMapping("/history/failed")
    public ApiResponse<Page<QueryHistory>> getFailedQueries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        var failedQueries = queryManagementService.getFailedQueries(user, pageable);

        var response = failedQueries.map(this::createQueryHistoryDto);
        return ApiResponse.success(response);
    }

    // ==================== Statistics Endpoints ====================

    @GetMapping("/stats")
    public ApiResponse<UserStatistics> getUserStatistics(
            Authentication authentication
    ) {
        User user = getCurrentUser(authentication);

        UserStatistics response = new UserStatistics(
                queryManagementService.getUserQueryCount(user),
                queryManagementService.getUserExecutionCount(user),
                queryManagementService.getUserAverageExecutionTime(user),
                queryManagementService.getUserFailedQueryCount(user)
        );

        return ApiResponse.success(response);
    }

    // ==================== Helper Methods ====================

    private User getCurrentUser(Authentication authentication) {
        return Optional.of(authentication)
                .map(Authentication::getName)
                .flatMap(userRepository::findByUsername)
                .get();
    }

    private SavedQuery createSavedQueryDto(cherry.sqlapp2.entity.SavedQuery savedQuery) {
        SavedQuery response = new SavedQuery(savedQuery);

        // Set parameter definitions
        Map<String, String> parameterDefinitions = queryManagementService
                .getParameterDefinitions(savedQuery.getId(), savedQuery.getUser());
        response.setParameterDefinitions(parameterDefinitions);

        return response;
    }

    private QueryHistory createQueryHistoryDto(cherry.sqlapp2.entity.QueryHistory queryHistory) {
        QueryHistory response = new QueryHistory(queryHistory);

        // Set parameter values if available
        if (queryHistory.getParameterValues() != null && !queryHistory.getParameterValues().trim().isEmpty()) {
            try {
                Map<String, Object> parameterValues = objectMapper.readValue(
                        queryHistory.getParameterValues(),
                        new TypeReference<Map<String, Object>>() {
                        }
                );
                response.setParameterValues(parameterValues);
            } catch (JsonProcessingException e) {
                // If deserialization fails, leave parameter values null
            }
        }

        return response;
    }
}
