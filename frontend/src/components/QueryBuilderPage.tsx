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

import React, {useState, useEffect} from 'react'
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
  operator: string
  value?: string
  values?: string[]
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

  // Load database connections
  useEffect(() => {
    loadConnections()
  }, [])

  // Load schema when connection changes
  useEffect(() => {
    if (selectedConnectionId) {
      loadSchema(selectedConnectionId)
    }
  }, [selectedConnectionId])

  const loadConnections = async () => {
    try {
      const response = await apiRequest('/api/connections')
      setConnections(response.data as DatabaseConnection[])
    } catch (error) {
      console.error('Failed to load connections:', error)
    }
  }

  const loadSchema = async (connectionId: number) => {
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
  }

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
    setQueryStructure(prev => ({
      ...prev,
      fromTables: prev.fromTables.map((table, i) =>
        i === index ? {...table, [field]: value || undefined} : table
      )
    }))
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

        {/* SELECT Clause */}
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
                  {schemaInfo?.tables.map(table => (
                    <option key={table.name} value={table.name}>
                      {table.name}
                    </option>
                  ))}
                </select>

                <select
                  value={column.columnName}
                  onChange={(e) => updateSelectColumn(index, 'columnName', e.target.value)}
                >
                  <option value="">{t('queryBuilder.selectColumn')}</option>
                  <option value="*">{t('queryBuilder.allColumns')}</option>
                  {column.tableName && schemaInfo?.tables
                    .find(table => table.name === column.tableName)?.columns
                    ?.map(col => (
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

        {/* FROM Clause */}
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

        {/* WHERE Clause */}
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
                  {schemaInfo?.tables.map(table => (
                    <option key={table.name} value={table.name}>
                      {table.name}
                    </option>
                  ))}
                </select>

                <select
                  value={condition.columnName}
                  onChange={(e) => updateWhereCondition(index, 'columnName', e.target.value)}
                >
                  <option value="">{t('queryBuilder.selectColumn')}</option>
                  {condition.tableName && schemaInfo?.tables
                    .find(table => table.name === condition.tableName)?.columns
                    ?.map(col => (
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

                {!['IS NULL', 'IS NOT NULL'].includes(condition.operator) && (
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

        {/* ORDER BY Clause */}
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
                  {schemaInfo?.tables.map(table => (
                    <option key={table.name} value={table.name}>
                      {table.name}
                    </option>
                  ))}
                </select>

                <select
                  value={column.columnName}
                  onChange={(e) => updateOrderByColumn(index, 'columnName', e.target.value)}
                >
                  <option value="">{t('queryBuilder.selectColumn')}</option>
                  {column.tableName && schemaInfo?.tables
                    .find(table => table.name === column.tableName)?.columns
                    ?.map(col => (
                      <option key={col.name} value={col.name}>
                        {col.name}
                      </option>
                    ))}
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

        {/* LIMIT */}
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

        {/* Build Button */}
        <div className="section">
          <button
            onClick={buildQuery}
            disabled={isBuilding}
            className="build-btn"
          >
            {isBuilding ? t('queryBuilder.building') : t('queryBuilder.generateSQL')}
          </button>
        </div>

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
