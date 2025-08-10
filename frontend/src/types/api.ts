/*
 * Copyright 2024 sqlapp2 project
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

/**
 * Backend API Response Types
 * バックエンドAPI レスポンス型定義
 * 
 * Javaのrecordクラスに対応するTypeScript型定義
 */

// HealthController
export interface HealthResponse {
  status: string
  timestamp: string
  application: string
}

// DatabaseConnectionController
export interface ConnectionCountResponse {
  activeConnections: number
}

export interface DatabaseTypeResponse {
  name: string
  displayName: string
  defaultPort: number
}

export interface ConnectionStatusResponse {
  connectionId: number
  available: boolean
  error?: string
  checkedAt: string
}

// QueryController
export interface UserStatisticsResponse {
  savedQueryCount: number
  executionCount: number
  averageExecutionTime?: number
  failedQueryCount: number
}

// SqlExecutionController
export interface QueryValidationResponse {
  valid: boolean
  message?: string
  error?: string
  errorType?: string
  validatedAt: string
  sql: string
}

export interface QueryExecutionValidationResponse {
  valid: boolean
  message: string
  validatedAt: string
}

export interface QueryExecutionErrorResponse {
  success: boolean
  error: string
  errorType: string
  executedAt: string
  sql?: string
  errorCode?: number
  sqlState?: string
}

// 既存のレスポンス型（互換性維持）
export interface SqlExecutionResult {
  success: boolean
  data?: {
    columns: string[]
    rows: any[][]
    rowCount: number
    executionTime: number
  }
  error?: string
  executedAt?: string
  queryHistoryId?: number
  savedQueryId?: number
}