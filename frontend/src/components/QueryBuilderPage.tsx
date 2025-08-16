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

import React, {useCallback, useEffect, useState} from 'react'
import type {Location} from 'react-router-dom'
import {useLocation, useNavigate} from 'react-router-dom'
import {useTranslation} from 'react-i18next'
import {useAuth} from '../context/AuthContext'
import type {DatabaseConnection, TableInfo as ApiTableInfo} from '../types/api'
import Layout from './Layout'

interface SchemaInfo {
  tables: LocalTableInfo[]
}

interface LocalTableInfo {
  name: string
  columns: { name: string }[]
}

interface SelectColumn {
  tableName: string
  columnName: string
  alias?: string
  aggregateFunction?: string
}

interface FromTable {
  tableName: string
  alias?: string
}

interface JoinCondition {
  leftTable: string
  leftColumn: string
  operator: string
  rightTable: string
  rightColumn: string
}

interface JoinClause {
  joinType: string
  tableName: string
  alias?: string
  conditions: JoinCondition[]
}

interface WhereCondition {
  tableName?: string
  columnName: string
  aggregateFunction?: string
  operator: string
  value?: string
  values?: string[]
  minValue?: string
  maxValue?: string
  logicalOperator?: string
  negated: boolean
}

interface GroupByColumn {
  tableName?: string
  columnName: string
}

interface OrderByColumn {
  tableName?: string
  columnName: string
  aggregateFunction?: string
  direction: string
}

interface QueryStructure {
  selectColumns: SelectColumn[]
  distinct: boolean
  fromTables: FromTable[]
  joins: JoinClause[]
  whereConditions: WhereCondition[]
  groupByColumns: GroupByColumn[]
  havingConditions: WhereCondition[]
  orderByColumns: OrderByColumn[]
  limit?: number
  offset?: number
}

interface QueryBuilderResponse {
  generatedSql: string | null
  valid: boolean
  validationErrors: string[] | null
  warnings: string[] | null
  detectedParameters: Record<string, string> | null
  parsedStructure: QueryStructure | null
  buildTimeMs: number
}

const QueryBuilderPage: React.FC = () => {
  const {t} = useTranslation()
  const {apiRequest} = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [connections, setConnections] = useState<DatabaseConnection[]>([])
  const [selectedConnectionId, setSelectedConnectionId] = useState<number | null>(null)
  const [schemaInfo, setSchemaInfo] = useState<SchemaInfo | null>(null)

  // Query structure state
  const [queryStructure, setQueryStructure] = useState<QueryStructure>({
    selectColumns: [{tableName: '', columnName: '*'}],
    distinct: false,
    fromTables: [],
    joins: [],
    whereConditions: [],
    groupByColumns: [],
    havingConditions: [],
    orderByColumns: [],
  })

  const [generatedSql, setGeneratedSql] = useState('')
  const [validationErrors, setValidationErrors] = useState<string[]>([])
  const [isBuilding, setIsBuilding] = useState(false)
  const [aliasWarnings, setAliasWarnings] = useState<string[]>([])
  const [activeTab, setActiveTab] = useState<string>('select')

  // Helper function to get available table references (table names + aliases)
  const getAvailableTableReferences = (): { value: string; label: string }[] => {
    const references: { value: string; label: string }[] = []

    // Add FROM table references (prioritize aliases when available)
    queryStructure.fromTables.forEach(table => {
      if (table.tableName) {
        if (table.alias && table.alias.trim()) {
          // Use alias when available
          references.push({value: table.alias.trim(), label: `${table.alias} (${table.tableName})`})
        } else {
          // Use table name when no alias
          references.push({value: table.tableName, label: table.tableName})
        }
      }
    })

    // Add JOIN table references (when JOINs are implemented)
    queryStructure.joins.forEach(join => {
      if (join.tableName) {
        if (join.alias && join.alias.trim()) {
          references.push({value: join.alias.trim(), label: `${join.alias} (${join.tableName})`})
        } else {
          references.push({value: join.tableName, label: join.tableName})
        }
      }
    })

    return references
  }

  // Helper function to check for alias conflicts
  const checkAliasConflicts = useCallback(() => {
    const warnings: string[] = []
    const usedAliases = new Set<string>()
    const usedTableNames = new Set<string>()

    // Check FROM table aliases
    queryStructure.fromTables.forEach((table, index) => {
      if (table.alias) {
        const alias = table.alias.trim()
        if (alias) {
          if (usedAliases.has(alias) || usedTableNames.has(alias)) {
            warnings.push(t('queryBuilder.duplicateAlias', {alias, location: `FROM[${index + 1}]`}))
          }
          usedAliases.add(alias)
        }
      }
      if (table.tableName) {
        usedTableNames.add(table.tableName)
      }
    })

    // Check JOIN table aliases
    queryStructure.joins.forEach((join, index) => {
      if (join.alias) {
        const alias = join.alias.trim()
        if (alias) {
          if (usedAliases.has(alias) || usedTableNames.has(alias)) {
            warnings.push(t('queryBuilder.duplicateAlias', {alias, location: `JOIN[${index + 1}]`}))
          }
          usedAliases.add(alias)
        }
      }
      if (join.tableName && !join.alias) {
        if (usedTableNames.has(join.tableName) || usedAliases.has(join.tableName)) {
          warnings.push(t('queryBuilder.duplicateTable', {table: join.tableName, location: `JOIN[${index + 1}]`}))
        }
        usedTableNames.add(join.tableName)
      }
    })

    setAliasWarnings(warnings)
  }, [queryStructure.fromTables, queryStructure.joins, t])

  // Check alias conflicts when structure changes
  useEffect(() => {
    checkAliasConflicts()
  }, [checkAliasConflicts])

  // Helper function to get columns for a specific table reference
  const getColumnsForTableReference = (tableReference: string): { name: string }[] => {
    // Find the actual table name for this reference
    let actualTableName = tableReference

    // Check if reference is an alias
    const fromTableWithAlias = queryStructure.fromTables.find(table =>
      table.alias && table.alias.trim() === tableReference
    )
    if (fromTableWithAlias) {
      actualTableName = fromTableWithAlias.tableName
    }

    // Check JOIN tables (when implemented)
    const joinTableWithAlias = queryStructure.joins.find(join =>
      join.alias && join.alias.trim() === tableReference
    )
    if (joinTableWithAlias) {
      actualTableName = joinTableWithAlias.tableName
    }

    // Return columns for the actual table
    const table = schemaInfo?.tables.find(t => t.name === actualTableName)
    return table?.columns || []
  }

  const loadConnections = useCallback(async () => {
    try {
      const response = await apiRequest('/api/connections')
      setConnections(response.data as DatabaseConnection[])
    } catch (error) {
      console.error('Failed to load connections:', error)
    }
  }, [apiRequest])

  const loadSchema = useCallback(async (connectionId: number) => {
    try {
      // Load tables first
      const tablesResponse = await apiRequest(`/api/schema/connections/${connectionId}/tables`)
      const tables = tablesResponse.data as ApiTableInfo[]

      // Load columns for each table
      const tablesWithColumns = await Promise.all(
        tables.map(async (table: ApiTableInfo) => {
          try {
            const columnsResponse = await apiRequest(`/api/schema/connections/${connectionId}/tables/${table.name}/columns`)
            const columns = columnsResponse.data as { name: string }[]
            return {...table, columns}
          } catch (error) {
            console.error(`Failed to load columns for table ${table.name}:`, error)
            return {...table, columns: []}
          }
        })
      )

      setSchemaInfo({tables: tablesWithColumns as LocalTableInfo[]})
    } catch (error) {
      console.error('Failed to load schema:', error)
    }
  }, [apiRequest])

  // Load database connections
  useEffect(() => {
    loadConnections()
  }, [loadConnections])

  const parseAndLoadSQL = useCallback(async (sql: string, connectionId: number) => {
    try {
      console.log('parseAndLoadSQL: Starting with SQL:', sql, 'connectionId:', connectionId) // Debug log
      setIsBuilding(true)
      setValidationErrors([])

      // Set connection first
      setSelectedConnectionId(connectionId)

      // Parse SQL using the new API
      console.log('parseAndLoadSQL: Making API request to /api/query-builder/parse') // Debug log
      const response = await apiRequest('/api/query-builder/parse', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({sql})
      })

      console.log('parseAndLoadSQL: API response:', response) // Debug log
      const result = response.data as { success: boolean; queryStructure?: QueryStructure; errorMessage?: string }

      if (result.success && result.queryStructure) {
        // Successfully parsed - load the structure
        console.log('parseAndLoadSQL: Successfully parsed, loading structure:', result.queryStructure) // Debug log
        setQueryStructure(result.queryStructure)
        setGeneratedSql(sql)
        setValidationErrors([])
      } else {
        // Parsing failed - show error and keep original SQL for reference
        console.log('parseAndLoadSQL: Parsing failed:', result.errorMessage) // Debug log
        setValidationErrors([
          t('queryBuilder.parseError') + ': ' + (result.errorMessage || 'Unknown error'),
          t('queryBuilder.manualEdit')
        ])
        setGeneratedSql(sql)
      }
    } catch (error) {
      console.error('parseAndLoadSQL: Error:', error)
      setValidationErrors([
        t('queryBuilder.parseError') + ': ' + (error instanceof Error ? error.message : 'Unknown error'),
        t('queryBuilder.manualEdit')
      ])
      setGeneratedSql(sql)
    } finally {
      setIsBuilding(false)
    }
  }, [t, apiRequest])

  // Load schema when connection changes
  useEffect(() => {
    if (selectedConnectionId) {
      loadSchema(selectedConnectionId)
    }
  }, [selectedConnectionId, loadSchema])

  // Handle SQL parsing from other pages (React Router state)
  useEffect(() => {
    const state = (location as Location<{
      sql?: string;
      connectionId?: number;
      mode?: string;
      savedQueryId?: number;
      savedQueryName?: string
    }>).state
    console.log('QueryBuilderPage: location.state =', state) // Debug log
    console.log('QueryBuilderPage: location.pathname =', location.pathname) // Debug log

    if (state?.mode === 'edit' && state?.sql && state?.connectionId) {
      console.log('QueryBuilderPage: Starting SQL parsing for:', state.sql) // Debug log
      parseAndLoadSQL(state.sql, state.connectionId)

      // Don't clear state immediately to ensure it's processed
      setTimeout(() => {
        if (location.state) {
          window.history.replaceState({}, '', location.pathname)
        }
      }, 100)
    }
  }, [location, parseAndLoadSQL]) // Listen to the entire location object changes

  const buildQuery = async () => {
    if (!selectedConnectionId) {
      setValidationErrors([t('queryBuilder.pleaseSelectConnection')])
      return
    }

    setIsBuilding(true)
    try {
      const request = {
        queryStructure,
        formatSql: true,
        validateSyntax: true
      }

      const response = await apiRequest('/api/query-builder/build', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(request)
      })

      const result = response.data as QueryBuilderResponse

      if (result.valid) {
        setGeneratedSql(result.generatedSql ?? '')
        setValidationErrors([])
      } else {
        setValidationErrors(result.validationErrors || ['Unknown validation error'])
      }
    } catch (error: unknown) {
      console.error('Failed to build query:', error)
      setValidationErrors([error instanceof Error ? error.message : 'Failed to build query'])
    } finally {
      setIsBuilding(false)
    }
  }

  const executeGeneratedQuery = () => {
    if (!generatedSql || !selectedConnectionId) {
      setValidationErrors([
        !selectedConnectionId
          ? t('queryBuilder.pleaseSelectConnection')
          : t('queryBuilder.pleaseGenerateFirst')
      ])
      return
    }

    navigate('/sql', {
      state: {
        sql: generatedSql,
        connectionId: selectedConnectionId
      }
    })
  }

  const saveGeneratedQuery = () => {
    if (!generatedSql || !selectedConnectionId) {
      setValidationErrors([
        !selectedConnectionId
          ? t('queryBuilder.pleaseSelectConnection')
          : t('queryBuilder.pleaseGenerateFirst')
      ])
      return
    }

    navigate('/queries', {
      state: {
        sql: generatedSql,
        connectionId: selectedConnectionId,
        mode: 'create'
      }
    })
  }

  const addSelectColumn = () => {
    setQueryStructure(prev => ({
      ...prev,
      selectColumns: [...prev.selectColumns, {tableName: '', columnName: ''}]
    }))
  }

  const updateSelectColumn = (index: number, field: keyof SelectColumn, value: string) => {
    setQueryStructure(prev => ({
      ...prev,
      selectColumns: prev.selectColumns.map((col, i) =>
        i === index ? {...col, [field]: value || undefined} : col
      )
    }))
  }

  const removeSelectColumn = (index: number) => {
    setQueryStructure(prev => ({
      ...prev,
      selectColumns: prev.selectColumns.filter((_, i) => i !== index)
    }))
  }

  const addFromTable = () => {
    setQueryStructure(prev => ({
      ...prev,
      fromTables: [...prev.fromTables, {tableName: ''}]
    }))
  }

  const updateFromTable = (index: number, field: keyof FromTable, value: string) => {
    setQueryStructure(prev => {
      const updatedTables = prev.fromTables.map((table, i) =>
        i === index ? {...table, [field]: value || undefined} : table
      )

      // Auto-update table references when FROM table aliases change
      let updatedSelectColumns = prev.selectColumns
      let updatedWhereConditions = prev.whereConditions
      let updatedOrderByColumns = prev.orderByColumns
      let updatedJoinConditions = prev.joins

      if (field === 'alias' || field === 'tableName') {
        const oldTable = prev.fromTables[index]
        const newTable = updatedTables[index]

        // If alias changed or table name changed, update all references
        if (oldTable && newTable) {
          const oldReference = oldTable.alias || oldTable.tableName
          const newReference = newTable.alias || newTable.tableName

          if (oldReference && newReference && oldReference !== newReference) {
            // Update SELECT columns
            updatedSelectColumns = prev.selectColumns.map(col =>
              col.tableName === oldReference ? {...col, tableName: newReference} : col
            )

            // Update WHERE conditions
            updatedWhereConditions = prev.whereConditions.map(condition =>
              condition.tableName === oldReference ? {...condition, tableName: newReference} : condition
            )

            // Update ORDER BY columns
            updatedOrderByColumns = prev.orderByColumns.map(col =>
              col.tableName === oldReference ? {...col, tableName: newReference} : col
            )

            // Update JOIN conditions
            updatedJoinConditions = prev.joins.map(join => ({
              ...join,
              conditions: join.conditions.map(condition => ({
                ...condition,
                leftTable: condition.leftTable === oldReference ? newReference : condition.leftTable,
                rightTable: condition.rightTable === oldReference ? newReference : condition.rightTable
              }))
            }))
          }
        }
      }

      return {
        ...prev,
        fromTables: updatedTables,
        selectColumns: updatedSelectColumns,
        whereConditions: updatedWhereConditions,
        orderByColumns: updatedOrderByColumns,
        joins: updatedJoinConditions
      }
    })
  }

  const removeFromTable = (index: number) => {
    setQueryStructure(prev => ({
      ...prev,
      fromTables: prev.fromTables.filter((_, i) => i !== index)
    }))
  }

  const addWhereCondition = () => {
    setQueryStructure(prev => ({
      ...prev,
      whereConditions: [...prev.whereConditions, {
        columnName: '',
        operator: '=',
        value: '',
        negated: false
      }]
    }))
  }

  const updateWhereCondition = (index: number, field: keyof WhereCondition, value: string | boolean) => {
    setQueryStructure(prev => ({
      ...prev,
      whereConditions: prev.whereConditions.map((condition, i) =>
        i === index ? {...condition, [field]: value} : condition
      )
    }))
  }

  const removeWhereCondition = (index: number) => {
    setQueryStructure(prev => ({
      ...prev,
      whereConditions: prev.whereConditions.filter((_, i) => i !== index)
    }))
  }

  // JOIN management functions
  const addJoin = () => {
    setQueryStructure(prev => ({
      ...prev,
      joins: [...prev.joins, {
        joinType: 'INNER',
        tableName: '',
        alias: '',
        conditions: []
      }]
    }))
  }

  const updateJoin = (index: number, field: keyof JoinClause, value: string) => {
    setQueryStructure(prev => {
      const updatedJoins = prev.joins.map((join, i) =>
        i === index ? {...join, [field]: value || undefined} : join
      )

      // Auto-update table references when JOIN table aliases change
      let updatedSelectColumns = prev.selectColumns
      let updatedWhereConditions = prev.whereConditions
      let updatedOrderByColumns = prev.orderByColumns
      let updatedJoinConditions = updatedJoins

      if (field === 'alias' || field === 'tableName') {
        const oldJoin = prev.joins[index]
        const newJoin = updatedJoins[index]

        // If alias changed or table name changed, update all references
        if (oldJoin && newJoin) {
          const oldReference = oldJoin.alias || oldJoin.tableName
          const newReference = newJoin.alias || newJoin.tableName

          if (oldReference && newReference && oldReference !== newReference) {
            // Update SELECT columns
            updatedSelectColumns = prev.selectColumns.map(col =>
              col.tableName === oldReference ? {...col, tableName: newReference} : col
            )

            // Update WHERE conditions
            updatedWhereConditions = prev.whereConditions.map(condition =>
              condition.tableName === oldReference ? {...condition, tableName: newReference} : condition
            )

            // Update ORDER BY columns
            updatedOrderByColumns = prev.orderByColumns.map(col =>
              col.tableName === oldReference ? {...col, tableName: newReference} : col
            )

            // Update other JOIN conditions (both left and right table references)
            updatedJoinConditions = updatedJoins.map(join => ({
              ...join,
              conditions: join.conditions.map(condition => ({
                ...condition,
                leftTable: condition.leftTable === oldReference ? newReference : condition.leftTable,
                rightTable: condition.rightTable === oldReference ? newReference : condition.rightTable
              }))
            }))
          }
        }
      }

      return {
        ...prev,
        joins: updatedJoinConditions,
        selectColumns: updatedSelectColumns,
        whereConditions: updatedWhereConditions,
        orderByColumns: updatedOrderByColumns
      }
    })
  }

  const removeJoin = (index: number) => {
    setQueryStructure(prev => ({
      ...prev,
      joins: prev.joins.filter((_, i) => i !== index)
    }))
  }

  const addJoinCondition = (joinIndex: number) => {
    setQueryStructure(prev => ({
      ...prev,
      joins: prev.joins.map((join, i) =>
        i === joinIndex ? {
          ...join,
          conditions: [...join.conditions, {
            leftTable: '',
            leftColumn: '',
            operator: '=',
            rightTable: '',
            rightColumn: ''
          }]
        } : join
      )
    }))
  }

  const updateJoinCondition = (joinIndex: number, conditionIndex: number, field: keyof JoinCondition, value: string) => {
    setQueryStructure(prev => ({
      ...prev,
      joins: prev.joins.map((join, i) =>
        i === joinIndex ? {
          ...join,
          conditions: join.conditions.map((condition, j) =>
            j === conditionIndex ? {...condition, [field]: value} : condition
          )
        } : join
      )
    }))
  }

  const removeJoinCondition = (joinIndex: number, conditionIndex: number) => {
    setQueryStructure(prev => ({
      ...prev,
      joins: prev.joins.map((join, i) =>
        i === joinIndex ? {
          ...join,
          conditions: join.conditions.filter((_, j) => j !== conditionIndex)
        } : join
      )
    }))
  }

  const addOrderByColumn = () => {
    setQueryStructure(prev => ({
      ...prev,
      orderByColumns: [...prev.orderByColumns, {columnName: '', direction: 'ASC'}]
    }))
  }

  const updateOrderByColumn = (index: number, field: keyof OrderByColumn, value: string) => {
    setQueryStructure(prev => ({
      ...prev,
      orderByColumns: prev.orderByColumns.map((col, i) =>
        i === index ? {...col, [field]: value || undefined} : col
      )
    }))
  }

  const removeOrderByColumn = (index: number) => {
    setQueryStructure(prev => ({
      ...prev,
      orderByColumns: prev.orderByColumns.filter((_, i) => i !== index)
    }))
  }

  // GROUP BY functions
  const addGroupByColumn = () => {
    setQueryStructure(prev => ({
      ...prev,
      groupByColumns: [...prev.groupByColumns, {columnName: ''}]
    }))
  }

  const updateGroupByColumn = (index: number, field: keyof GroupByColumn, value: string) => {
    setQueryStructure(prev => ({
      ...prev,
      groupByColumns: prev.groupByColumns.map((col, i) =>
        i === index ? {...col, [field]: value || undefined} : col
      )
    }))
  }

  const removeGroupByColumn = (index: number) => {
    setQueryStructure(prev => ({
      ...prev,
      groupByColumns: prev.groupByColumns.filter((_, i) => i !== index)
    }))
  }

  // HAVING functions
  const addHavingCondition = () => {
    setQueryStructure(prev => ({
      ...prev,
      havingConditions: [...prev.havingConditions, {
        columnName: '',
        operator: '=',
        value: '',
        negated: false
      }]
    }))
  }

  const updateHavingCondition = (index: number, field: keyof WhereCondition, value: any) => {
    setQueryStructure(prev => ({
      ...prev,
      havingConditions: prev.havingConditions.map((condition, i) =>
        i === index ? {...condition, [field]: value} : condition
      )
    }))
  }

  const removeHavingCondition = (index: number) => {
    setQueryStructure(prev => ({
      ...prev,
      havingConditions: prev.havingConditions.filter((_, i) => i !== index)
    }))
  }

  return (
    <Layout title={t('queryBuilder.title')}>
      <div className="query-builder">

        {/* Connection Selection */}
        <div className="section">
          <h2>{t('queryBuilder.databaseConnection')}</h2>
          <select
            value={selectedConnectionId || ''}
            onChange={(e) => setSelectedConnectionId(Number(e.target.value) || null)}
            className="connection-select"
          >
            <option value="">{t('queryBuilder.selectConnection')}</option>
            {connections.map(conn => (
              <option key={conn.id} value={conn.id}>
                {conn.connectionName} ({conn.databaseType})
              </option>
            ))}
          </select>
        </div>

        {/* Tab Navigation */}
        <div className="tab-navigation">
          <button
            className={`tab-btn ${activeTab === 'select' ? 'active' : ''}`}
            onClick={() => setActiveTab('select')}
          >
            {t('queryBuilder.selectClause')} ({queryStructure.selectColumns.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'from' ? 'active' : ''}`}
            onClick={() => setActiveTab('from')}
          >
            {t('queryBuilder.fromClause')} ({queryStructure.fromTables.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'join' ? 'active' : ''}`}
            onClick={() => setActiveTab('join')}
          >
            {t('queryBuilder.joinClause')} ({queryStructure.joins.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'where' ? 'active' : ''}`}
            onClick={() => setActiveTab('where')}
          >
            {t('queryBuilder.whereClause')} ({queryStructure.whereConditions.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'groupby' ? 'active' : ''}`}
            onClick={() => setActiveTab('groupby')}
          >
            {t('queryBuilder.groupByClause')} ({queryStructure.groupByColumns.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'having' ? 'active' : ''}`}
            onClick={() => setActiveTab('having')}
          >
            {t('queryBuilder.havingClause')} ({queryStructure.havingConditions.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'orderby' ? 'active' : ''}`}
            onClick={() => setActiveTab('orderby')}
          >
            {t('queryBuilder.orderByClause')} ({queryStructure.orderByColumns.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'limit' ? 'active' : ''}`}
            onClick={() => setActiveTab('limit')}
          >
            {t('queryBuilder.limitClause')}
          </button>
        </div>

        {/* Tab Content */}
        <div className="tab-content">

          {/* SELECT Tab */}
          {activeTab === 'select' && (
            <div className="section">
              <h2>{t('queryBuilder.selectClause')}</h2>

              {/* DISTINCT checkbox for entire SELECT */}
              <div className="clause-item">
                <label>
                  <input
                    type="checkbox"
                    checked={queryStructure.distinct}
                    onChange={(e) => setQueryStructure(prev => ({
                      ...prev,
                      distinct: e.target.checked
                    }))}
                  />
                  {t('queryBuilder.distinct')}
                </label>
              </div>
              {queryStructure.selectColumns.map((column, index) => (
                <div key={index} className="clause-item">
                  <div className="clause-item-content">
                    <select
                      value={column.tableName}
                      onChange={(e) => updateSelectColumn(index, 'tableName', e.target.value)}
                    >
                      <option value="">{t('queryBuilder.selectTable')}</option>
                      {getAvailableTableReferences().map(ref => (
                        <option key={ref.value} value={ref.value}>
                          {ref.label}
                        </option>
                      ))}
                    </select>

                    <select
                      value={column.columnName}
                      onChange={(e) => updateSelectColumn(index, 'columnName', e.target.value)}
                    >
                      <option value="">{t('queryBuilder.selectColumn')}</option>
                      <option value="*">{t('queryBuilder.allColumns')}</option>
                      {column.tableName && getColumnsForTableReference(column.tableName)
                        .map(col => (
                          <option key={col.name} value={col.name}>
                            {col.name}
                          </option>
                        ))}
                    </select>

                    <select
                      value={column.aggregateFunction || ''}
                      onChange={(e) => updateSelectColumn(index, 'aggregateFunction', e.target.value || '')}
                    >
                      <option value="">{t('queryBuilder.noFunction')}</option>
                      <option value="COUNT">COUNT</option>
                      <option value="SUM">SUM</option>
                      <option value="AVG">AVG</option>
                      <option value="MAX">MAX</option>
                      <option value="MIN">MIN</option>
                    </select>

                    <input
                      type="text"
                      placeholder={t('queryBuilder.aliasOptional')}
                      value={column.alias || ''}
                      onChange={(e) => updateSelectColumn(index, 'alias', e.target.value || '')}
                    />
                  </div>

                  <div className="clause-item-actions">
                    <button
                      onClick={() => removeSelectColumn(index)}
                      className="remove-btn"
                      title={t('queryBuilder.remove')}
                    >
                      ×
                    </button>
                  </div>
                </div>
              ))}
              <button onClick={addSelectColumn} className="add-btn">{t('queryBuilder.addColumn')}</button>
            </div>
          )}

          {/* FROM Tab */}
          {activeTab === 'from' && (
            <div className="section">
              <h2>{t('queryBuilder.fromClause')}</h2>
              {queryStructure.fromTables.map((table, index) => (
                <div key={index} className="clause-item">
                  <div className="clause-item-content">
                    <select
                      value={table.tableName}
                      onChange={(e) => updateFromTable(index, 'tableName', e.target.value)}
                    >
                      <option value="">{t('queryBuilder.selectTable')}</option>
                      {schemaInfo?.tables.map(t => (
                        <option key={t.name} value={t.name}>
                          {t.name}
                        </option>
                      ))}
                    </select>

                    <input
                      type="text"
                      placeholder={t('queryBuilder.aliasOptional')}
                      value={table.alias || ''}
                      onChange={(e) => updateFromTable(index, 'alias', e.target.value || '')}
                    />

                  </div>

                  <div className="clause-item-actions">
                    <button
                      onClick={() => removeFromTable(index)}
                      className="remove-btn"
                      title={t('queryBuilder.remove')}
                    >
                      ×
                    </button>
                  </div>
                </div>
              ))}
              <button onClick={addFromTable} className="add-btn">{t('queryBuilder.addTable')}</button>
            </div>
          )}

          {/* JOIN Tab */}
          {activeTab === 'join' && (
            <div className="section">
              <h2>{t('queryBuilder.joinClause')}</h2>
              {queryStructure.joins.map((join, index) => (
                <div key={index} className="join-clause">
                  {/* JOIN Table Definition */}
                  <div className="clause-item join-table-definition">
                    <div className="join-table-row">
                      {/* Join Type Selection */}
                      <select
                        value={join.joinType}
                        onChange={(e) => updateJoin(index, 'joinType', e.target.value)}
                        className="join-type-select"
                      >
                        <option value="INNER">INNER JOIN</option>
                        <option value="LEFT">LEFT JOIN</option>
                        <option value="RIGHT">RIGHT JOIN</option>
                        <option value="FULL OUTER">FULL OUTER JOIN</option>
                      </select>

                      {/* Join Table Selection */}
                      <select
                        value={join.tableName}
                        onChange={(e) => updateJoin(index, 'tableName', e.target.value)}
                        className="join-table-select-main"
                      >
                        <option value="">{t('queryBuilder.selectTable')}</option>
                        {schemaInfo?.tables.map(t => (
                          <option key={t.name} value={t.name}>
                            {t.name}
                          </option>
                        ))}
                      </select>

                      {/* Join Table Alias */}
                      <input
                        type="text"
                        placeholder={t('queryBuilder.aliasOptional')}
                        value={join.alias || ''}
                        onChange={(e) => updateJoin(index, 'alias', e.target.value || '')}
                        className="join-alias-input"
                      />

                      {/* Remove JOIN button */}
                      <button
                        onClick={() => removeJoin(index)}
                        className="remove-btn join-remove"
                        title={t('queryBuilder.remove')}
                      >
                        ×
                      </button>
                    </div>
                  </div>

                  {/* Join Conditions - Moved below */}
                  <div className="join-conditions">
                    <h4>{t('queryBuilder.joinConditions')}</h4>
                    {join.conditions.map((condition, condIndex) => (
                      <div key={condIndex} className="join-condition">
                        <div className="condition-expression">
                          <div className="condition-left">
                            <select
                              value={condition.leftTable}
                              onChange={(e) => updateJoinCondition(index, condIndex, 'leftTable', e.target.value)}
                              className="table-select"
                            >
                              <option value="">{t('queryBuilder.selectTable')}</option>
                              {getAvailableTableReferences().map(ref => (
                                <option key={ref.value} value={ref.value}>
                                  {ref.label}
                                </option>
                              ))}
                            </select>
                            <span className="dot">.</span>
                            <select
                              value={condition.leftColumn}
                              onChange={(e) => updateJoinCondition(index, condIndex, 'leftColumn', e.target.value)}
                              className="column-select"
                            >
                              <option value="">{t('queryBuilder.selectColumn')}</option>
                              {condition.leftTable && getColumnsForTableReference(condition.leftTable)
                                .map(col => (
                                  <option key={col.name} value={col.name}>
                                    {col.name}
                                  </option>
                                ))}
                            </select>
                          </div>

                          <div className="condition-operator">
                            <select
                              value={condition.operator}
                              onChange={(e) => updateJoinCondition(index, condIndex, 'operator', e.target.value)}
                              className="operator-select"
                            >
                              <option value="=">=</option>
                              <option value="<>">≠</option>
                              <option value="<">{'<'}</option>
                              <option value=">">{'>'}</option>
                              <option value="<=">≤</option>
                              <option value=">=">≥</option>
                            </select>
                          </div>

                          <div className="condition-right">
                            <select
                              value={condition.rightTable}
                              onChange={(e) => updateJoinCondition(index, condIndex, 'rightTable', e.target.value)}
                              className="table-select"
                            >
                              <option value="">{t('queryBuilder.selectTable')}</option>
                              {getAvailableTableReferences().map(ref => (
                                <option key={ref.value} value={ref.value}>
                                  {ref.label}
                                </option>
                              ))}
                            </select>
                            <span className="dot">.</span>
                            <select
                              value={condition.rightColumn}
                              onChange={(e) => updateJoinCondition(index, condIndex, 'rightColumn', e.target.value)}
                              className="column-select"
                            >
                              <option value="">{t('queryBuilder.selectColumn')}</option>
                              {condition.rightTable && getColumnsForTableReference(condition.rightTable)
                                .map(col => (
                                  <option key={col.name} value={col.name}>
                                    {col.name}
                                  </option>
                                ))}
                            </select>
                          </div>

                          <button
                            onClick={() => removeJoinCondition(index, condIndex)}
                            className="remove-btn condition-remove"
                            title={t('queryBuilder.remove')}
                          >
                            ×
                          </button>
                        </div>
                      </div>
                    ))}
                    <button onClick={() => addJoinCondition(index)} className="add-btn-small">
                      {t('queryBuilder.addCondition')}
                    </button>
                  </div>
                </div>
              ))}
              <button onClick={addJoin} className="add-btn">{t('queryBuilder.addJoin')}</button>
            </div>
          )}

          {/* WHERE Tab */}
          {activeTab === 'where' && (
            <div className="section">
              <h2>{t('queryBuilder.whereClause')}</h2>
              {queryStructure.whereConditions.map((condition, index) => (
                <div key={index} className="clause-item">
                  <div className="clause-item-content">
                    <select
                      value={condition.tableName || ''}
                      onChange={(e) => updateWhereCondition(index, 'tableName', e.target.value || '')}
                    >
                      <option value="">{t('queryBuilder.selectTable')}</option>
                      {getAvailableTableReferences().map(ref => (
                        <option key={ref.value} value={ref.value}>
                          {ref.label}
                        </option>
                      ))}
                    </select>

                    <select
                      value={condition.columnName}
                      onChange={(e) => updateWhereCondition(index, 'columnName', e.target.value)}
                    >
                      <option value="">{t('queryBuilder.selectColumn')}</option>
                      {condition.tableName && getColumnsForTableReference(condition.tableName)
                        .map(col => (
                          <option key={col.name} value={col.name}>
                            {col.name}
                          </option>
                        ))}
                    </select>

                    <select
                      value={condition.operator}
                      onChange={(e) => updateWhereCondition(index, 'operator', e.target.value)}
                    >
                      <option value="=">=</option>
                      <option value="<>">≠</option>
                      <option value="<">{'<'}</option>
                      <option value=">">{'>'}</option>
                      <option value="<=">≤</option>
                      <option value=">=">≥</option>
                      <option value="LIKE">LIKE</option>
                      <option value="IN">IN</option>
                      <option value="BETWEEN">BETWEEN</option>
                      <option value="IS NULL">IS NULL</option>
                      <option value="IS NOT NULL">IS NOT NULL</option>
                    </select>

                    {condition.operator === 'BETWEEN' ? (
                      <>
                        <input
                          type="text"
                          placeholder={t('queryBuilder.minValue')}
                          value={condition.minValue || ''}
                          onChange={(e) => updateWhereCondition(index, 'minValue', e.target.value)}
                        />
                        <input
                          type="text"
                          placeholder={t('queryBuilder.maxValue')}
                          value={condition.maxValue || ''}
                          onChange={(e) => updateWhereCondition(index, 'maxValue', e.target.value)}
                        />
                      </>
                    ) : !['IS NULL', 'IS NOT NULL'].includes(condition.operator) && (
                      <input
                        type="text"
                        placeholder={t('queryBuilder.value')}
                        value={condition.value || ''}
                        onChange={(e) => updateWhereCondition(index, 'value', e.target.value)}
                      />
                    )}

                    {index > 0 && (
                      <select
                        value={condition.logicalOperator || 'AND'}
                        onChange={(e) => updateWhereCondition(index, 'logicalOperator', e.target.value)}
                      >
                        <option value="AND">AND</option>
                        <option value="OR">OR</option>
                      </select>
                    )}

                    <label>
                      <input
                        type="checkbox"
                        checked={condition.negated}
                        onChange={(e) => updateWhereCondition(index, 'negated', e.target.checked)}
                      />
                      {t('queryBuilder.not')}
                    </label>

                  </div>

                  <div className="clause-item-actions">
                    <button
                      onClick={() => removeWhereCondition(index)}
                      className="remove-btn"
                      title={t('queryBuilder.remove')}
                    >
                      ×
                    </button>
                  </div>
                </div>
              ))}
              <button onClick={addWhereCondition} className="add-btn">{t('queryBuilder.addCondition')}</button>
            </div>
          )}

          {/* ORDER BY Tab */}
          {activeTab === 'orderby' && (
            <div className="section">
              <h2>{t('queryBuilder.orderByClause')}</h2>
              {queryStructure.orderByColumns.map((column, index) => (
                <div key={index} className="clause-item">
                  <div className="clause-item-content">
                    <select
                      value={column.tableName || ''}
                      onChange={(e) => updateOrderByColumn(index, 'tableName', e.target.value || '')}
                    >
                      <option value="">{t('queryBuilder.selectTable')}</option>
                      {getAvailableTableReferences().map(ref => (
                        <option key={ref.value} value={ref.value}>
                          {ref.label}
                        </option>
                      ))}
                    </select>

                    <select
                      value={column.columnName}
                      onChange={(e) => updateOrderByColumn(index, 'columnName', e.target.value)}
                    >
                      <option value="">{t('queryBuilder.selectColumn')}</option>
                      {column.tableName && getColumnsForTableReference(column.tableName)
                        .map(col => (
                          <option key={col.name} value={col.name}>
                            {col.name}
                          </option>
                        ))}
                    </select>

                    <select
                      value={column.aggregateFunction || ''}
                      onChange={(e) => updateOrderByColumn(index, 'aggregateFunction', e.target.value || '')}
                    >
                      <option value="">{t('queryBuilder.noFunction')}</option>
                      <option value="COUNT">COUNT</option>
                      <option value="SUM">SUM</option>
                      <option value="AVG">AVG</option>
                      <option value="MAX">MAX</option>
                      <option value="MIN">MIN</option>
                    </select>

                    <select
                      value={column.direction}
                      onChange={(e) => updateOrderByColumn(index, 'direction', e.target.value)}
                    >
                      <option value="ASC">ASC</option>
                      <option value="DESC">DESC</option>
                    </select>

                  </div>

                  <div className="clause-item-actions">
                    <button
                      onClick={() => removeOrderByColumn(index)}
                      className="remove-btn"
                      title={t('queryBuilder.remove')}
                    >
                      ×
                    </button>
                  </div>
                </div>
              ))}
              <button onClick={addOrderByColumn} className="add-btn">{t('queryBuilder.addOrderBy')}</button>
            </div>
          )}

          {/* GROUP BY Tab */}
          {activeTab === 'groupby' && (
            <div className="section">
              <h2>{t('queryBuilder.groupByClause')}</h2>
              {queryStructure.groupByColumns.map((column, index) => (
                <div key={index} className="clause-item">
                  <div className="clause-item-content">
                    <select
                      value={column.tableName || ''}
                      onChange={(e) => updateGroupByColumn(index, 'tableName', e.target.value || '')}
                    >
                      <option value="">{t('queryBuilder.selectTable')}</option>
                      {getAvailableTableReferences().map(ref => (
                        <option key={ref.value} value={ref.value}>{ref.label}</option>
                      ))}
                    </select>

                    <select
                      value={column.columnName}
                      onChange={(e) => updateGroupByColumn(index, 'columnName', e.target.value)}
                    >
                      <option value="">{t('queryBuilder.selectColumn')}</option>
                      {getColumnsForTableReference(column.tableName || '').map(col => (
                        <option key={col.name} value={col.name}>{col.name}</option>
                      ))}
                    </select>
                  </div>

                  <div className="clause-item-actions">
                    <button
                      onClick={() => removeGroupByColumn(index)}
                      className="remove-btn"
                      title={t('queryBuilder.remove')}
                    >
                      ×
                    </button>
                  </div>
                </div>
              ))}
              <button onClick={addGroupByColumn} className="add-btn">{t('queryBuilder.addGroupBy')}</button>
            </div>
          )}

          {/* HAVING Tab */}
          {activeTab === 'having' && (
            <div className="section">
              <h2>{t('queryBuilder.havingClause')}</h2>
              {queryStructure.havingConditions.map((condition, index) => (
                <div key={index} className="clause-item">
                  <div className="clause-item-content">
                    {index > 0 && (
                      <select
                        value={condition.logicalOperator || 'AND'}
                        onChange={(e) => updateHavingCondition(index, 'logicalOperator', e.target.value)}
                        className="logical-operator"
                      >
                        <option value="AND">AND</option>
                        <option value="OR">OR</option>
                      </select>
                    )}

                    <label>
                      <input
                        type="checkbox"
                        checked={condition.negated}
                        onChange={(e) => updateHavingCondition(index, 'negated', e.target.checked)}
                      />
                      {t('queryBuilder.not')}
                    </label>

                    <select
                      value={condition.tableName || ''}
                      onChange={(e) => updateHavingCondition(index, 'tableName', e.target.value || '')}
                    >
                      <option value="">{t('queryBuilder.selectTable')}</option>
                      {getAvailableTableReferences().map(ref => (
                        <option key={ref.value} value={ref.value}>{ref.label}</option>
                      ))}
                    </select>

                    <select
                      value={condition.columnName}
                      onChange={(e) => updateHavingCondition(index, 'columnName', e.target.value)}
                    >
                      <option value="">{t('queryBuilder.selectColumn')}</option>
                      {getColumnsForTableReference(condition.tableName || '').map(col => (
                        <option key={col.name} value={col.name}>{col.name}</option>
                      ))}
                    </select>

                    <select
                      value={condition.aggregateFunction || ''}
                      onChange={(e) => updateHavingCondition(index, 'aggregateFunction', e.target.value || '')}
                    >
                      <option value="">{t('queryBuilder.noFunction')}</option>
                      <option value="COUNT">COUNT</option>
                      <option value="SUM">SUM</option>
                      <option value="AVG">AVG</option>
                      <option value="MAX">MAX</option>
                      <option value="MIN">MIN</option>
                    </select>

                    <select
                      value={condition.operator}
                      onChange={(e) => updateHavingCondition(index, 'operator', e.target.value)}
                    >
                      <option value="=">=</option>
                      <option value="<>">≠</option>
                      <option value="<">&lt;</option>
                      <option value=">">&gt;</option>
                      <option value="<=">&le;</option>
                      <option value=">=">&ge;</option>
                      <option value="LIKE">LIKE</option>
                      <option value="IN">IN</option>
                      <option value="BETWEEN">BETWEEN</option>
                      <option value="IS NULL">IS NULL</option>
                      <option value="IS NOT NULL">IS NOT NULL</option>
                    </select>

                    {condition.operator === 'BETWEEN' ? (
                      <>
                        <input
                          type="text"
                          placeholder={t('queryBuilder.minValue')}
                          value={condition.minValue || ''}
                          onChange={(e) => updateHavingCondition(index, 'minValue', e.target.value)}
                        />
                        <input
                          type="text"
                          placeholder={t('queryBuilder.maxValue')}
                          value={condition.maxValue || ''}
                          onChange={(e) => updateHavingCondition(index, 'maxValue', e.target.value)}
                        />
                      </>
                    ) : condition.operator === 'IN' ? (
                      <input
                        type="text"
                        placeholder="value1,value2,value3"
                        value={condition.values?.join(',') || ''}
                        onChange={(e) => updateHavingCondition(index, 'values', e.target.value.split(',').map(v => v.trim()).filter(v => v))}
                      />
                    ) : !['IS NULL', 'IS NOT NULL'].includes(condition.operator) && (
                      <input
                        type="text"
                        placeholder={t('queryBuilder.value')}
                        value={condition.value || ''}
                        onChange={(e) => updateHavingCondition(index, 'value', e.target.value)}
                      />
                    )}
                  </div>

                  <div className="clause-item-actions">
                    <button
                      onClick={() => removeHavingCondition(index)}
                      className="remove-btn"
                      title={t('queryBuilder.remove')}
                    >
                      ×
                    </button>
                  </div>
                </div>
              ))}
              <button onClick={addHavingCondition} className="add-btn">{t('queryBuilder.addCondition')}</button>
            </div>
          )}

          {/* LIMIT Tab */}
          {activeTab === 'limit' && (
            <div className="section">
              <h2>{t('queryBuilder.limitClause')}</h2>
              <input
                type="number"
                placeholder={t('queryBuilder.limitOptional')}
                value={queryStructure.limit || ''}
                onChange={(e) => setQueryStructure(prev => ({
                  ...prev,
                  limit: e.target.value ? parseInt(e.target.value) : undefined
                }))}
              />
              <input
                type="number"
                placeholder={t('queryBuilder.offsetOptional')}
                value={queryStructure.offset || ''}
                onChange={(e) => setQueryStructure(prev => ({
                  ...prev,
                  offset: e.target.value ? parseInt(e.target.value) : undefined
                }))}
              />
            </div>
          )}

        </div>

        {/* Build Buttons */}
        <div className="section">
          <div className="button-group">
            <button
              onClick={buildQuery}
              disabled={isBuilding}
              className="build-btn"
            >
              {isBuilding ? t('queryBuilder.building') : t('queryBuilder.generateSQL')}
            </button>

            <button
              onClick={executeGeneratedQuery}
              disabled={!generatedSql || !selectedConnectionId}
              className="execute-btn"
            >
              {t('queryBuilder.executeQuery')}
            </button>

            <button
              onClick={saveGeneratedQuery}
              disabled={!generatedSql || !selectedConnectionId}
              className="save-btn"
            >
              {t('queryBuilder.saveQuery')}
            </button>
          </div>
        </div>

        {/* Alias Warnings */}
        {aliasWarnings.length > 0 && (
          <div className="alias-warnings" style={{
            backgroundColor: '#fff3cd',
            border: '1px solid #ffeaa7',
            padding: '10px',
            margin: '10px 0',
            borderRadius: '4px'
          }}>
            <h3 style={{color: '#856404'}}>{t('queryBuilder.aliasWarnings')}:</h3>
            <ul style={{color: '#856404', margin: '5px 0'}}>
              {aliasWarnings.map((warning, index) => (
                <li key={index}>{warning}</li>
              ))}
            </ul>
          </div>
        )}

        {/* Validation Errors */}
        {validationErrors.length > 0 && (
          <div className="validation-errors">
            <h3>{t('queryBuilder.validationErrors')}:</h3>
            <ul>
              {validationErrors.map((error, index) => (
                <li key={index}>{error}</li>
              ))}
            </ul>
          </div>
        )}

        {/* Generated SQL */}
        {generatedSql && (
          <div className="generated-sql">
            <h2>{t('queryBuilder.generatedSQL')}</h2>
            <pre>{generatedSql}</pre>
          </div>
        )}
      </div>
    </Layout>
  )
}

export default QueryBuilderPage
