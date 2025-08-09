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

import cherry.sqlapp2.dto.QueryHistoryResponse;
import cherry.sqlapp2.dto.SavedQueryRequest;
import cherry.sqlapp2.dto.SavedQueryResponse;
import cherry.sqlapp2.entity.DatabaseConnection;
import cherry.sqlapp2.entity.QueryHistory;
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
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SavedQueryResponse> saveQuery(
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
        return ResponseEntity.ok(response);
    }

    @PutMapping("/saved/{queryId}")
    public ResponseEntity<SavedQueryResponse> updateSavedQuery(
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
        return ResponseEntity.ok(response);
    }

    @GetMapping("/saved")
    public ResponseEntity<List<SavedQueryResponse>> getUserSavedQueries(
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        List<SavedQuery> savedQueries = queryManagementService.getUserQueries(user);
        
        List<SavedQueryResponse> response = savedQueries.stream()
            .map(this::createSavedQueryResponse)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(response);
    }

    @GetMapping("/saved/paged")
    public ResponseEntity<Page<SavedQueryResponse>> getUserSavedQueriesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<SavedQuery> savedQueries = queryManagementService.getUserQueries(user, pageable);
        
        Page<SavedQueryResponse> response = savedQueries.map(this::createSavedQueryResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/saved/{queryId}")
    public ResponseEntity<SavedQueryResponse> getSavedQuery(
            @PathVariable Long queryId,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        SavedQuery savedQuery = queryManagementService.getAccessibleQuery(queryId, user)
            .orElseThrow(() -> new RuntimeException("Saved query not found or not accessible"));
            
        SavedQueryResponse response = createSavedQueryResponse(savedQuery);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/saved/{queryId}")
    public ResponseEntity<Void> deleteSavedQuery(
            @PathVariable Long queryId,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        queryManagementService.deleteSavedQuery(queryId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public")
    public ResponseEntity<List<SavedQueryResponse>> getPublicQueries() {
        List<SavedQuery> publicQueries = queryManagementService.getPublicQueries();
        
        List<SavedQueryResponse> response = publicQueries.stream()
            .map(this::createSavedQueryResponse)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/paged")
    public ResponseEntity<Page<SavedQueryResponse>> getPublicQueriesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SavedQuery> publicQueries = queryManagementService.getPublicQueries(pageable);
        
        Page<SavedQueryResponse> response = publicQueries.map(this::createSavedQueryResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SavedQueryResponse>> searchQueries(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<SavedQuery> searchResults = queryManagementService.searchAccessibleQueries(user, searchTerm, pageable);
        
        Page<SavedQueryResponse> response = searchResults.map(this::createSavedQueryResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/saved/{queryId}/execute")
    public ResponseEntity<Void> recordQueryExecution(
            @PathVariable Long queryId,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        queryManagementService.updateQueryExecutionStats(queryId, user);
        return ResponseEntity.ok().build();
    }

    // ==================== Query History Endpoints ====================

    @GetMapping("/history")
    public ResponseEntity<Page<QueryHistoryResponse>> getQueryHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<QueryHistory> history = queryManagementService.getUserQueryHistory(user, pageable);
        
        Page<QueryHistoryResponse> response = history.map(this::createQueryHistoryResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/recent")
    public ResponseEntity<List<QueryHistoryResponse>> getRecentQueryHistory(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        List<QueryHistory> recentHistory = queryManagementService.getRecentQueryHistory(user, limit);
        
        List<QueryHistoryResponse> response = recentHistory.stream()
            .map(this::createQueryHistoryResponse)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/search")
    public ResponseEntity<Page<QueryHistoryResponse>> searchQueryHistory(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<QueryHistory> searchResults = queryManagementService.searchQueryHistory(user, searchTerm, pageable);
        
        Page<QueryHistoryResponse> response = searchResults.map(this::createQueryHistoryResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/successful")
    public ResponseEntity<Page<QueryHistoryResponse>> getSuccessfulQueries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<QueryHistory> successfulQueries = queryManagementService.getSuccessfulQueries(user, pageable);
        
        Page<QueryHistoryResponse> response = successfulQueries.map(this::createQueryHistoryResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/failed")
    public ResponseEntity<Page<QueryHistoryResponse>> getFailedQueries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<QueryHistory> failedQueries = queryManagementService.getFailedQueries(user, pageable);
        
        Page<QueryHistoryResponse> response = failedQueries.map(this::createQueryHistoryResponse);
        return ResponseEntity.ok(response);
    }

    // ==================== Statistics Endpoints ====================

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStatistics(Authentication authentication) {
        User user = getCurrentUser(authentication);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("savedQueryCount", queryManagementService.getUserQueryCount(user));
        stats.put("executionCount", queryManagementService.getUserExecutionCount(user));
        stats.put("averageExecutionTime", queryManagementService.getUserAverageExecutionTime(user));
        stats.put("failedQueryCount", queryManagementService.getUserFailedQueryCount(user));
        
        return ResponseEntity.ok(stats);
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

    private QueryHistoryResponse createQueryHistoryResponse(QueryHistory queryHistory) {
        QueryHistoryResponse response = new QueryHistoryResponse(queryHistory);
        
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