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

package cherry.sqlapp2.exception;

import cherry.sqlapp2.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers
 * 全RESTコントローラー共通例外ハンドラー
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle validation errors for request body
     * リクエストボディのバリデーションエラー処理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        logger.debug("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(ApiResponse.error(errors));
    }

    /**
     * Handle validation errors for form data
     * フォームデータのバリデーションエラー処理
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        logger.debug("Bind validation error: {}", errors);
        return ResponseEntity.badRequest().body(ApiResponse.error(errors));
    }

    /**
     * Handle constraint validation errors
     * 制約バリデーションエラー処理
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        logger.debug("Constraint violation error: {}", errors);
        return ResponseEntity.badRequest().body(ApiResponse.error(errors));
    }

    /**
     * Handle authentication errors
     * 認証エラー処理
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        logger.debug("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(List.of("Invalid username or password")));
    }

    /**
     * Handle general authentication errors
     * 一般的な認証エラー処理
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        logger.debug("Authentication error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(List.of("Authentication failed")));
    }

    /**
     * Handle custom validation errors
     * カスタムバリデーションエラー処理
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
        logger.debug("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(List.of(ex.getMessage())));
    }

    /**
     * Handle resource not found errors
     * リソース未発見エラー処理
     */
    @ExceptionHandler({ResourceNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(Exception ex) {
        logger.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(List.of(ex.getMessage())));
    }

    /**
     * Handle database connection errors
     * データベース接続エラー処理
     */
    @ExceptionHandler({DatabaseConnectionException.class, SQLException.class})
    public ResponseEntity<ApiResponse<Void>> handleDatabaseException(Exception ex) {
        logger.error("Database error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(List.of("Database operation failed")));
    }

    /**
     * Handle invalid query errors
     * 無効なクエリエラー処理
     */
    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidQueryException(InvalidQueryException ex) {
        logger.debug("Invalid query: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(List.of(ex.getMessage())));
    }

    /**
     * Handle business logic errors
     * ビジネスロジックエラー処理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.debug("Business logic error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(List.of(ex.getMessage())));
    }

    /**
     * Handle other runtime errors
     * その他のランタイムエラー処理
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        logger.debug("Runtime error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(List.of(ex.getMessage() != null ? ex.getMessage() : "Request processing failed")));
    }

    /**
     * Handle unexpected errors
     * 予期しないエラー処理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(List.of("Internal server error")));
    }
}