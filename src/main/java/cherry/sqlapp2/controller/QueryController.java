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
import cherry.sqlapp2.dto.QueryHistory;
import cherry.sqlapp2.dto.SavedQueryRequest;
import cherry.sqlapp2.dto.SavedQueryResponse;
import cherry.sqlapp2.dto.UserStatisticsResponse;
import cherry.sqlapp2.entity.DatabaseConnection;
import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.repository.DatabaseConnectionRepository;
import cherry.sqlapp2.repository.UserRepository;
import cherry.sqlapp2.service.QueryManagementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/queries")
public class QueryController {

    @Autowired
    private QueryManagementService queryManagementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseConnectionRepository connectionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== Saved Queries Endpoints ====================

    @PostMapping("/saved")
    public ApiResponse<SavedQueryResponse> saveQuery(
            @Valid @RequestBody SavedQueryRequest request,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        DatabaseConnection defaultConnection = null;

        if (request.getDefaultConnectionId() != null) {
            defaultConnection = connectionRepository.findByUserAndId(user, request.getDefaultConnectionId())
                .orElseThrow(() -> new RuntimeException("Database connection not found"));
        }

        SavedQuery savedQuery = queryManagementService.saveQuery(
            request.getName(),
            request.getSqlContent(),
            request.getDescription(),
            request.getParameterDefinitions(),
            request.getSharingScope(),
            user,
            defaultConnection
        );

        SavedQueryResponse response = createSavedQueryResponse(savedQuery);
        return ApiResponse.success(response);
    }

    @PutMapping("/saved/{queryId}")
    public ApiResponse<SavedQueryResponse> updateSavedQuery(
            @PathVariable Long queryId,
            @Valid @RequestBody SavedQueryRequest request,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        DatabaseConnection defaultConnection = null;

        if (request.getDefaultConnectionId() != null) {
            defaultConnection = connectionRepository.findByUserAndId(user, request.getDefaultConnectionId())
                .orElseThrow(() -> new RuntimeException("Database connection not found"));
        }

        SavedQuery savedQuery = queryManagementService.updateSavedQuery(
            queryId,
            request.getName(),
            request.getSqlContent(),
            request.getDescription(),
            request.getParameterDefinitions(),
            request.getSharingScope(),
            defaultConnection,
            user
        );

        SavedQueryResponse response = createSavedQueryResponse(savedQuery);
        return ApiResponse.success(response);
    }

    @GetMapping("/saved")
    public ApiResponse<List<SavedQueryResponse>> getUserSavedQueries(
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        List<SavedQuery> savedQueries = queryManagementService.getUserQueries(user);
        
        List<SavedQueryResponse> response = savedQueries.stream()
            .map(this::createSavedQueryResponse)
            .collect(Collectors.toList());
            
        return ApiResponse.success(response);
    }

    @Deprecated
    @GetMapping("/saved/paged")
    public ApiResponse<Page<SavedQueryResponse>> getUserSavedQueriesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<SavedQuery> savedQueries = queryManagementService.getUserQueries(user, pageable);
        
        Page<SavedQueryResponse> response = savedQueries.map(this::createSavedQueryResponse);
        return ApiResponse.success(response);
    }

    @Deprecated
    @GetMapping("/saved/{queryId}")
    public ApiResponse<SavedQueryResponse> getSavedQuery(
            @PathVariable Long queryId,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        SavedQuery savedQuery = queryManagementService.getAccessibleQuery(queryId, user)
            .orElseThrow(() -> new RuntimeException("Saved query not found or not accessible"));
            
        SavedQueryResponse response = createSavedQueryResponse(savedQuery);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/saved/{queryId}")
    public ApiResponse<Void> deleteSavedQuery(
            @PathVariable Long queryId,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        queryManagementService.deleteSavedQuery(queryId, user);
        return ApiResponse.success(null);
    }

    @GetMapping("/public")
    public ApiResponse<List<SavedQueryResponse>> getPublicQueries() {
        List<SavedQuery> publicQueries = queryManagementService.getPublicQueries();
        
        List<SavedQueryResponse> response = publicQueries.stream()
            .map(this::createSavedQueryResponse)
            .collect(Collectors.toList());
            
        return ApiResponse.success(response);
    }

    @Deprecated
    @GetMapping("/public/paged")
    public ApiResponse<Page<SavedQueryResponse>> getPublicQueriesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SavedQuery> publicQueries = queryManagementService.getPublicQueries(pageable);
        
        Page<SavedQueryResponse> response = publicQueries.map(this::createSavedQueryResponse);
        return ApiResponse.success(response);
    }

    @Deprecated
    @GetMapping("/search")
    public ApiResponse<Page<SavedQueryResponse>> searchQueries(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<SavedQuery> searchResults = queryManagementService.searchAccessibleQueries(user, searchTerm, pageable);
        
        Page<SavedQueryResponse> response = searchResults.map(this::createSavedQueryResponse);
        return ApiResponse.success(response);
    }

    @Deprecated
    @PostMapping("/saved/{queryId}/execute")
    public ApiResponse<Void> recordQueryExecution(
            @PathVariable Long queryId,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        queryManagementService.updateQueryExecutionStats(queryId, user);
        return ApiResponse.success(null);
    }

    // ==================== Query History Endpoints ====================

    @GetMapping("/history")
    public ApiResponse<Page<QueryHistory>> getQueryHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        var history = queryManagementService.getUserQueryHistory(user, pageable);
        
        var response = history.map(this::createQueryHistory);
        return ApiResponse.success(response);
    }

    @Deprecated
    @GetMapping("/history/recent")
    public ApiResponse<List<QueryHistory>> getRecentQueryHistory(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        var recentHistory = queryManagementService.getRecentQueryHistory(user, limit);
        
        var response = recentHistory.stream()
            .map(this::createQueryHistory)
            .collect(Collectors.toList());
            
        return ApiResponse.success(response);
    }

    @GetMapping("/history/{historyId}")
    public ApiResponse<QueryHistory> getQueryHistoryById(
            @PathVariable Long historyId,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        var queryHistory = queryManagementService.getQueryHistoryById(historyId, user)
            .orElseThrow(() -> new RuntimeException("Query history not found or not accessible"));
            
        QueryHistory response = createQueryHistory(queryHistory);
        return ApiResponse.success(response);
    }

    @GetMapping("/history/search")
    public ApiResponse<Page<QueryHistory>> searchQueryHistory(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        var searchResults = queryManagementService.searchQueryHistory(user, searchTerm, pageable);
        
        var response = searchResults.map(this::createQueryHistory);
        return ApiResponse.success(response);
    }

    @GetMapping("/history/successful")
    public ApiResponse<Page<QueryHistory>> getSuccessfulQueries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        var successfulQueries = queryManagementService.getSuccessfulQueries(user, pageable);
        
        var response = successfulQueries.map(this::createQueryHistory);
        return ApiResponse.success(response);
    }

    @GetMapping("/history/failed")
    public ApiResponse<Page<QueryHistory>> getFailedQueries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        var failedQueries = queryManagementService.getFailedQueries(user, pageable);
        
        var response = failedQueries.map(this::createQueryHistory);
        return ApiResponse.success(response);
    }

    // ==================== Statistics Endpoints ====================

    @GetMapping("/stats")
    public ApiResponse<UserStatisticsResponse> getUserStatistics(Authentication authentication) {
        User user = getCurrentUser(authentication);
        
        UserStatisticsResponse response = new UserStatisticsResponse(
                queryManagementService.getUserQueryCount(user),
                queryManagementService.getUserExecutionCount(user),
                queryManagementService.getUserAverageExecutionTime(user),
                queryManagementService.getUserFailedQueryCount(user)
        );
        
        return ApiResponse.success(response);
    }

    // ==================== Helper Methods ====================

    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private SavedQueryResponse createSavedQueryResponse(SavedQuery savedQuery) {
        SavedQueryResponse response = new SavedQueryResponse(savedQuery);
        
        // Set parameter definitions
        Map<String, String> parameterDefinitions = queryManagementService
            .getParameterDefinitions(savedQuery.getId(), savedQuery.getUser());
        response.setParameterDefinitions(parameterDefinitions);
        
        return response;
    }

    private QueryHistory createQueryHistory(cherry.sqlapp2.entity.QueryHistory queryHistory) {
        QueryHistory response = new QueryHistory(queryHistory);
        
        // Set parameter values if available
        if (queryHistory.getParameterValues() != null && !queryHistory.getParameterValues().trim().isEmpty()) {
            try {
                Map<String, Object> parameterValues = objectMapper.readValue(
                    queryHistory.getParameterValues(), 
                    new TypeReference<Map<String, Object>>() {}
                );
                response.setParameterValues(parameterValues);
            } catch (JsonProcessingException e) {
                // If deserialization fails, leave parameter values null
            }
        }
        
        return response;
    }
}