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

import React, { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'

interface DatabaseConnection {
  id: number
  connectionName: string
  databaseType: string
  host: string
  port: number
  databaseName: string
}

interface SchemaInfo {
  tables: TableInfo[]
}

interface TableInfo {
  tableName: string
  columns: ColumnInfo[]
}

interface ColumnInfo {
  columnName: string
  dataType: string
  isNullable: boolean
  isPrimaryKey: boolean
  isForeignKey: boolean
}

interface SelectColumn {
  tableName: string
  columnName: string
  alias?: string
  aggregateFunction?: string
  distinct: boolean
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
  generatedSql: string
  isValid: boolean
  validationErrors?: string[]
  warnings?: string[]
  detectedParameters?: Record<string, string>
  buildTimeMs: number
}

const QueryBuilder: React.FC = () => {
  const { t } = useTranslation()
  const { apiRequest } = useAuth()
  const [connections, setConnections] = useState<DatabaseConnection[]>([])
  const [selectedConnectionId, setSelectedConnectionId] = useState<number | null>(null)
  const [schemaInfo, setSchemaInfo] = useState<SchemaInfo | null>(null)
  
  // Query structure state
  const [queryStructure, setQueryStructure] = useState<QueryStructure>({
    selectColumns: [{ tableName: '', columnName: '*', distinct: false }],
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
      setConnections(response)
    } catch (error) {
      console.error('Failed to load connections:', error)
    }
  }

  const loadSchema = async (connectionId: number) => {
    try {
      const response = await apiRequest(`/api/schema/${connectionId}`)
      setSchemaInfo(response)
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

      const response: QueryBuilderResponse = await apiRequest('/api/query-builder/build', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      })

      if (response.isValid) {
        setGeneratedSql(response.generatedSql)
        setValidationErrors([])
      } else {
        setValidationErrors(response.validationErrors || ['Unknown validation error'])
      }
    } catch (error: any) {
      console.error('Failed to build query:', error)
      setValidationErrors([error.message || 'Failed to build query'])
    } finally {
      setIsBuilding(false)
    }
  }

  const addSelectColumn = () => {
    setQueryStructure(prev => ({
      ...prev,
      selectColumns: [...prev.selectColumns, { tableName: '', columnName: '', distinct: false }]
    }))
  }

  const updateSelectColumn = (index: number, field: keyof SelectColumn, value: any) => {
    setQueryStructure(prev => ({
      ...prev,
      selectColumns: prev.selectColumns.map((col, i) => 
        i === index ? { ...col, [field]: value || undefined } : col
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
      fromTables: [...prev.fromTables, { tableName: '' }]
    }))
  }

  const updateFromTable = (index: number, field: keyof FromTable, value: string) => {
    setQueryStructure(prev => ({
      ...prev,
      fromTables: prev.fromTables.map((table, i) => 
        i === index ? { ...table, [field]: value || undefined } : table
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

  const updateWhereCondition = (index: number, field: keyof WhereCondition, value: any) => {
    setQueryStructure(prev => ({
      ...prev,
      whereConditions: prev.whereConditions.map((condition, i) => 
        i === index ? { ...condition, [field]: value } : condition
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
      orderByColumns: [...prev.orderByColumns, { columnName: '', direction: 'ASC' }]
    }))
  }

  const updateOrderByColumn = (index: number, field: keyof OrderByColumn, value: string) => {
    setQueryStructure(prev => ({
      ...prev,
      orderByColumns: prev.orderByColumns.map((col, i) => 
        i === index ? { ...col, [field]: value || undefined } : col
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
    <div className="query-builder">
      <h1>{t('queryBuilder.title')}</h1>
      
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
        {queryStructure.selectColumns.map((column, index) => (
          <div key={index} className="clause-item">
            <select 
              value={column.tableName} 
              onChange={(e) => updateSelectColumn(index, 'tableName', e.target.value)}
            >
              <option value="">{t('queryBuilder.selectTable')}</option>
              {schemaInfo?.tables.map(table => (
                <option key={table.tableName} value={table.tableName}>
                  {table.tableName}
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
                .find(table => table.tableName === column.tableName)?.columns
                .map(col => (
                  <option key={col.columnName} value={col.columnName}>
                    {col.columnName} ({col.dataType})
                  </option>
                ))}
            </select>

            <select 
              value={column.aggregateFunction || ''} 
              onChange={(e) => updateSelectColumn(index, 'aggregateFunction', e.target.value || undefined)}
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

            <label>
              <input 
                type="checkbox" 
                checked={column.distinct} 
                onChange={(e) => updateSelectColumn(index, 'distinct', e.target.checked)}
              />
              {t('queryBuilder.distinct')}
            </label>

            <button onClick={() => removeSelectColumn(index)} className="remove-btn">{t('queryBuilder.remove')}</button>
          </div>
        ))}
        <button onClick={addSelectColumn} className="add-btn">{t('queryBuilder.addColumn')}</button>
      </div>

      {/* FROM Clause */}
      <div className="section">
        <h2>{t('queryBuilder.fromClause')}</h2>
        {queryStructure.fromTables.map((table, index) => (
          <div key={index} className="clause-item">
            <select 
              value={table.tableName} 
              onChange={(e) => updateFromTable(index, 'tableName', e.target.value)}
            >
              <option value="">{t('queryBuilder.selectTable')}</option>
              {schemaInfo?.tables.map(t => (
                <option key={t.tableName} value={t.tableName}>
                  {t.tableName}
                </option>
              ))}
            </select>
            
            <input 
              type="text" 
              placeholder={t('queryBuilder.aliasOptional')} 
              value={table.alias || ''} 
              onChange={(e) => updateFromTable(index, 'alias', e.target.value || '')}
            />

            <button onClick={() => removeFromTable(index)} className="remove-btn">{t('queryBuilder.remove')}</button>
          </div>
        ))}
        <button onClick={addFromTable} className="add-btn">{t('queryBuilder.addTable')}</button>
      </div>

      {/* WHERE Clause */}
      <div className="section">
        <h2>{t('queryBuilder.whereClause')}</h2>
        {queryStructure.whereConditions.map((condition, index) => (
          <div key={index} className="clause-item">
            <select 
              value={condition.tableName || ''} 
              onChange={(e) => updateWhereCondition(index, 'tableName', e.target.value || undefined)}
            >
              <option value="">{t('queryBuilder.selectTable')}</option>
              {schemaInfo?.tables.map(table => (
                <option key={table.tableName} value={table.tableName}>
                  {table.tableName}
                </option>
              ))}
            </select>

            <select 
              value={condition.columnName} 
              onChange={(e) => updateWhereCondition(index, 'columnName', e.target.value)}
            >
              <option value="">{t('queryBuilder.selectColumn')}</option>
              {condition.tableName && schemaInfo?.tables
                .find(table => table.tableName === condition.tableName)?.columns
                .map(col => (
                  <option key={col.columnName} value={col.columnName}>
                    {col.columnName} ({col.dataType})
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

            <button onClick={() => removeWhereCondition(index)} className="remove-btn">{t('queryBuilder.remove')}</button>
          </div>
        ))}
        <button onClick={addWhereCondition} className="add-btn">{t('queryBuilder.addCondition')}</button>
      </div>

      {/* ORDER BY Clause */}
      <div className="section">
        <h2>{t('queryBuilder.orderByClause')}</h2>
        {queryStructure.orderByColumns.map((column, index) => (
          <div key={index} className="clause-item">
            <select 
              value={column.tableName || ''} 
              onChange={(e) => updateOrderByColumn(index, 'tableName', e.target.value || '')}
            >
              <option value="">{t('queryBuilder.selectTable')}</option>
              {schemaInfo?.tables.map(table => (
                <option key={table.tableName} value={table.tableName}>
                  {table.tableName}
                </option>
              ))}
            </select>

            <select 
              value={column.columnName} 
              onChange={(e) => updateOrderByColumn(index, 'columnName', e.target.value)}
            >
              <option value="">{t('queryBuilder.selectColumn')}</option>
              {column.tableName && schemaInfo?.tables
                .find(table => table.tableName === column.tableName)?.columns
                .map(col => (
                  <option key={col.columnName} value={col.columnName}>
                    {col.columnName} ({col.dataType})
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

            <button onClick={() => removeOrderByColumn(index)} className="remove-btn">{t('queryBuilder.remove')}</button>
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

      <style>{`
        .query-builder {
          max-width: 1200px;
          margin: 0 auto;
          padding: 20px;
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }

        .section {
          margin-bottom: 30px;
          border: 1px solid #ddd;
          border-radius: 8px;
          padding: 20px;
          background: #f9f9f9;
        }

        .section h2 {
          margin: 0 0 15px 0;
          color: #333;
          border-bottom: 2px solid #007bff;
          padding-bottom: 5px;
        }

        .clause-item {
          display: flex;
          gap: 10px;
          margin-bottom: 10px;
          align-items: center;
          padding: 10px;
          background: white;
          border-radius: 5px;
          border: 1px solid #eee;
        }

        .clause-item select, 
        .clause-item input[type="text"], 
        .clause-item input[type="number"] {
          padding: 8px;
          border: 1px solid #ddd;
          border-radius: 4px;
          min-width: 150px;
        }

        .clause-item label {
          display: flex;
          align-items: center;
          gap: 5px;
          white-space: nowrap;
        }

        .connection-select {
          width: 100%;
          padding: 10px;
          border: 1px solid #ddd;
          border-radius: 4px;
          font-size: 16px;
        }

        .add-btn, .build-btn {
          background: #007bff;
          color: white;
          border: none;
          padding: 10px 20px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          transition: background-color 0.2s;
        }

        .add-btn:hover, .build-btn:hover {
          background: #0056b3;
        }

        .remove-btn {
          background: #dc3545;
          color: white;
          border: none;
          padding: 5px 10px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 12px;
        }

        .remove-btn:hover {
          background: #c82333;
        }

        .build-btn {
          font-size: 16px;
          padding: 12px 30px;
        }

        .build-btn:disabled {
          background: #6c757d;
          cursor: not-allowed;
        }

        .validation-errors {
          background: #f8d7da;
          border: 1px solid #f5c6cb;
          border-radius: 4px;
          padding: 15px;
          margin-bottom: 20px;
        }

        .validation-errors h3 {
          color: #721c24;
          margin: 0 0 10px 0;
        }

        .validation-errors ul {
          margin: 0;
          color: #721c24;
        }

        .generated-sql {
          background: #f8f9fa;
          border: 1px solid #dee2e6;
          border-radius: 4px;
          padding: 20px;
        }

        .generated-sql h2 {
          margin: 0 0 15px 0;
          color: #495057;
        }

        .generated-sql pre {
          background: #ffffff;
          border: 1px solid #e9ecef;
          border-radius: 4px;
          padding: 15px;
          margin: 0;
          overflow-x: auto;
          font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
          font-size: 14px;
          line-height: 1.4;
          color: #333;
        }

        @media (max-width: 768px) {
          .clause-item {
            flex-wrap: wrap;
            gap: 5px;
          }
          
          .clause-item select,
          .clause-item input {
            min-width: 120px;
            flex: 1;
          }
        }
      `}</style>
    </div>
  )
}

export default QueryBuilder