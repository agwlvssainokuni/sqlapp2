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

import { describe, it, expect } from 'vitest'

// Test helper functions for QueryBuilder alias resolution
interface FromTable {
  tableName: string
  alias?: string
}

interface JoinClause {
  tableName: string
  alias?: string
  joinType: string
}

interface QueryStructure {
  fromTables: FromTable[]
  joins: JoinClause[]
}

const getAvailableTableReferences = (queryStructure: QueryStructure): { value: string; label: string }[] => {
  const references: { value: string; label: string }[] = []
  
  // Add FROM table references (prioritize aliases when available)
  queryStructure.fromTables.forEach(table => {
    if (table.tableName) {
      if (table.alias && table.alias.trim()) {
        // Use alias when available
        references.push({ value: table.alias.trim(), label: `${table.alias} (${table.tableName})` })
      } else {
        // Use table name when no alias
        references.push({ value: table.tableName, label: table.tableName })
      }
    }
  })
  
  // Add JOIN table references
  queryStructure.joins.forEach(join => {
    if (join.tableName) {
      if (join.alias && join.alias.trim()) {
        references.push({ value: join.alias.trim(), label: `${join.alias} (${join.tableName})` })
      } else {
        references.push({ value: join.tableName, label: join.tableName })
      }
    }
  })
  
  return references
}

describe('QueryBuilder Alias Resolution', () => {
  describe('getAvailableTableReferences', () => {
    it('should return table names when no aliases are provided', () => {
      const queryStructure: QueryStructure = {
        fromTables: [
          { tableName: 'users' },
          { tableName: 'profiles' }
        ],
        joins: []
      }

      const result = getAvailableTableReferences(queryStructure)

      expect(result).toEqual([
        { value: 'users', label: 'users' },
        { value: 'profiles', label: 'profiles' }
      ])
    })

    it('should prioritize aliases over table names when aliases are provided', () => {
      const queryStructure: QueryStructure = {
        fromTables: [
          { tableName: 'users', alias: 'u' },
          { tableName: 'profiles', alias: 'p' }
        ],
        joins: []
      }

      const result = getAvailableTableReferences(queryStructure)

      expect(result).toEqual([
        { value: 'u', label: 'u (users)' },
        { value: 'p', label: 'p (profiles)' }
      ])
    })

    it('should handle mixed scenarios with some aliases and some without', () => {
      const queryStructure: QueryStructure = {
        fromTables: [
          { tableName: 'users', alias: 'u' },
          { tableName: 'profiles' } // No alias
        ],
        joins: []
      }

      const result = getAvailableTableReferences(queryStructure)

      expect(result).toEqual([
        { value: 'u', label: 'u (users)' },
        { value: 'profiles', label: 'profiles' }
      ])
    })

    it('should handle empty/whitespace aliases correctly', () => {
      const queryStructure: QueryStructure = {
        fromTables: [
          { tableName: 'users', alias: '   ' }, // Whitespace alias should be ignored
          { tableName: 'profiles', alias: '' }   // Empty alias should be ignored
        ],
        joins: []
      }

      const result = getAvailableTableReferences(queryStructure)

      expect(result).toEqual([
        { value: 'users', label: 'users' },
        { value: 'profiles', label: 'profiles' }
      ])
    })

    it('should include JOIN tables with aliases', () => {
      const queryStructure: QueryStructure = {
        fromTables: [
          { tableName: 'users', alias: 'u' }
        ],
        joins: [
          { tableName: 'profiles', alias: 'p', joinType: 'INNER' },
          { tableName: 'addresses', joinType: 'LEFT' } // No alias
        ]
      }

      const result = getAvailableTableReferences(queryStructure)

      expect(result).toEqual([
        { value: 'u', label: 'u (users)' },
        { value: 'p', label: 'p (profiles)' },
        { value: 'addresses', label: 'addresses' }
      ])
    })

    it('should handle empty queryStructure gracefully', () => {
      const queryStructure: QueryStructure = {
        fromTables: [],
        joins: []
      }

      const result = getAvailableTableReferences(queryStructure)

      expect(result).toEqual([])
    })

    it('should demonstrate critical SQL standards fix', () => {
      // This test demonstrates the fix for the SQL standards compliance issue
      const queryStructure: QueryStructure = {
        fromTables: [
          { tableName: 'users', alias: 'u' } // Table "users" with alias "u"
        ],
        joins: []
      }

      const result = getAvailableTableReferences(queryStructure)

      // Before fix: Would return 'users' causing invalid SQL like "SELECT users.name FROM users AS u"
      // After fix: Returns 'u' enabling valid SQL like "SELECT u.name FROM users AS u"
      expect(result).toEqual([
        { value: 'u', label: 'u (users)' }
      ])
      
      // The returned value 'u' should be used in SELECT clauses instead of 'users'
      expect(result[0].value).toBe('u') // Alias, not table name
      expect(result[0].value).not.toBe('users') // Should NOT be table name
    })
  })
})