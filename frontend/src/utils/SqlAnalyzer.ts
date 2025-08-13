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

/**
 * SQL Analysis utilities for frontend pagination support
 * Simplified version of backend SqlAnalyzer for basic ORDER BY detection
 */

export const PagingCompatibility = {
  COMPATIBLE: 'COMPATIBLE',
  NOT_SELECT: 'NOT_SELECT',
  HAS_LIMIT_OFFSET: 'HAS_LIMIT_OFFSET',
  NO_ORDER_BY: 'NO_ORDER_BY'
} as const

export type PagingCompatibility = typeof PagingCompatibility[keyof typeof PagingCompatibility]

/**
 * Check if SQL query is a SELECT statement
 */
export function isSelectQuery(sql: string): boolean {
  const trimmed = sql.trim().toLowerCase()
  return trimmed.startsWith('select') || 
         trimmed.startsWith('with') ||
         trimmed.startsWith('show') ||
         trimmed.startsWith('describe') ||
         trimmed.startsWith('desc') ||
         trimmed.startsWith('explain')
}

/**
 * Check if SQL contains ORDER BY clause
 * Simple implementation - not as sophisticated as backend version
 */
export function hasOrderByClause(sql: string): boolean {
  const normalized = sql.toLowerCase().replace(/\s+/g, ' ')
  return /\border\s+by\b/.test(normalized)
}

/**
 * Check if SQL contains LIMIT clause
 */
export function hasLimitClause(sql: string): boolean {
  const normalized = sql.toLowerCase().replace(/\s+/g, ' ')
  return /\blimit\s+\d+/.test(normalized)
}

/**
 * Check if SQL contains OFFSET clause
 */
export function hasOffsetClause(sql: string): boolean {
  const normalized = sql.toLowerCase().replace(/\s+/g, ' ')
  return /\boffset\s+\d+/.test(normalized)
}

/**
 * Check if SQL has pagination conflicts (LIMIT/OFFSET)
 */
export function hasPagingConflict(sql: string): boolean {
  return hasLimitClause(sql) || hasOffsetClause(sql)
}

/**
 * Get pagination compatibility for SQL query
 */
export function getPagingCompatibility(sql: string): PagingCompatibility {
  if (!isSelectQuery(sql)) {
    return PagingCompatibility.NOT_SELECT
  }
  
  if (hasPagingConflict(sql)) {
    return PagingCompatibility.HAS_LIMIT_OFFSET
  }
  
  if (!hasOrderByClause(sql)) {
    return PagingCompatibility.NO_ORDER_BY
  }
  
  return PagingCompatibility.COMPATIBLE
}