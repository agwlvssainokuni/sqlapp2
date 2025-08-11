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

export interface ApiResponse<T> {
  ok: boolean
  data?: T
  error?: string[]
}

// HealthController
export interface HealthcheckResult {
  status: string
  timestamp: string
  application: string
}

// DatabaseConnectionController
export interface NewConnection {
  connectionName: string
  databaseType: 'MYSQL' | 'POSTGRESQL' | 'MARIADB'
  host: string
  port: number
  databaseName: string
  username: string
  password: string
  additionalParams?: string
}

export interface ConnectionTestResult {
  success: boolean
  message: string
  responseTimeMs?: number
}

// QueryController
export interface UserStatisticsResponse {
  savedQueryCount: number
  executionCount: number
  averageExecutionTime?: number
  failedQueryCount: number
}

// SqlExecutionController
export interface SqlValidationResult {
  valid: boolean
  message?: string
  error?: string
  errorType?: string
  validatedAt: string
  sql: string
}

// SqlExecutionController - 統一されたSQL実行結果レスポンス
export interface SqlExecutionResult {
  success: boolean
  message: string
  executedAt: string
  sql?: string
  data?: SqlResultData
  error?: string
  errorType?: string
  errorCode?: number
  sqlState?: string
  queryHistoryId?: number
  savedQueryId?: number
}

export interface SqlResultData {
  columns: string[]
  rows: any[][]
  rowCount: number
  executionTime: number
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

// QueryController
export interface QueryHistory {
  id: number
  sqlContent: string
  parameterValues?: Record<string, any>
  executionTimeMs: number
  resultCount?: number
  isSuccessful: boolean
  errorMessage?: string
  connectionName: string
  databaseType: string
  connectionId: number
  savedQueryId?: number
  savedQueryName?: string
  executedAt: string
}

// DatabaseConnection型定義（統一型）
export interface DatabaseConnection {
  id: number
  connectionName: string
  databaseType: 'MYSQL' | 'POSTGRESQL' | 'MARIADB'
  host: string
  port: number
  databaseName: string
  username: string
  additionalParams?: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

// SchemaController
export interface DatabaseInfo {
  databaseProductName: string
  databaseProductVersion: string
  driverName: string
  driverVersion: string
  catalogs: string[]
  schemas: Array<{ name: string; catalog: string }>
}

export interface TableInfo {
  catalog: string
  schema: string
  name: string
  type: string
  remarks: string
}

export interface ColumnInfo {
  name: string
  dataType: number
  typeName: string
  columnSize: number
  decimalDigits: number
  nullable: boolean
  defaultValue: string
  ordinalPosition: number
  remarks: string
}

export interface PrimaryKeyInfo {
  columnName: string
  keySeq: number
  pkName: string
}

export interface ForeignKeyInfo {
  pkTableCatalog: string
  pkTableSchema: string
  pkTableName: string
  pkColumnName: string
  fkTableCatalog: string
  fkTableSchema: string
  fkTableName: string
  fkColumnName: string
  keySeq: number
  fkName: string
}

export interface IndexInfo {
  indexName: string
  unique: boolean
  columnName: string
  ordinalPosition: number
  ascOrDesc: string
}

export interface TableDetails {
  tableName: string
  catalog: string
  schema: string
  columns: ColumnInfo[]
  primaryKeys: PrimaryKeyInfo[]
  foreignKeys: ForeignKeyInfo[]
  indexes: IndexInfo[]
}


// AuthController
export interface LoginResult {
  access_token: string
  token_type: string
  expires_in: number
  user: LoginUser
}

export interface LoginUser {
  id: number
  username: string
  email: string
  createdAt: string
}
