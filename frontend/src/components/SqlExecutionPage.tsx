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

import React, {useCallback, useEffect, useRef, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {useAuth} from '../context/AuthContext'
import type {
  DatabaseConnection,
  PagingRequest,
  QueryHistory,
  SavedQuery,
  SqlExecutionRequest,
  SqlExecutionResult,
  SqlValidationResult
} from '../types/api'
import {extractParameters} from '../utils/SqlParameterExtractor'
import {hasOrderByClause} from '../utils/SqlAnalyzer'
import Layout from './Layout'
import PaginationSettings from './PaginationSettings'
import Pagination from './Pagination'

interface ParameterDefinition {
  name: string
  type: string
  value: string
}

const SqlExecutionPage: React.FC = () => {
  const {t} = useTranslation()
  const {apiRequest} = useAuth()
  const [connections, setConnections] = useState<DatabaseConnection[]>([])
  const [selectedConnectionId, setSelectedConnectionId] = useState<number | null>(null)
  const [sql, setSql] = useState('')
  const [parameters, setParameters] = useState<ParameterDefinition[]>([])
  const [result, setResult] = useState<SqlExecutionResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [currentQueryId, setCurrentQueryId] = useState<number | null>(null)
  const [executionMode, setExecutionMode] = useState<'new' | 'saved_query' | 'history' | null>(null)
  const [currentQueryName, setCurrentQueryName] = useState<string>('')
  const [paging, setPaging] = useState<PagingRequest>({
    enabled: false,
    page: 0,
    pageSize: 100,
    ignoreOrderByWarning: false
  })

  // Define all useCallback functions first
  const loadConnections = useCallback(async () => {
    try {
      const response = await apiRequest('/api/connections')

      if (response.ok) {
        const data = response.data as DatabaseConnection[]
        setConnections(data)
        if (data.length > 0) {
          setSelectedConnectionId(data[0].id)
        }
      } else {
        setError('Failed to load database connections')
      }
    } catch (err) {
      setError('Error loading connections: ' + (err as Error).message)
    }
  }, [apiRequest])

  const loadFromUrl = useCallback(async () => {
    const urlParams = new URLSearchParams(window.location.search)
    const queryId = urlParams.get('queryId')
    const historyId = urlParams.get('historyId')
    const connectionId = urlParams.get('connectionId')

    // Load from saved query
    if (queryId && !isNaN(Number(queryId))) {
      try {
        const response = await apiRequest(`/api/queries/saved/${queryId}`)
        const savedQuery = response.data as SavedQuery

        if (response.ok) {
          setSql(savedQuery.sqlContent || '')
          setCurrentQueryId(Number(queryId))
          setCurrentQueryName(savedQuery.name || '')
          setExecutionMode('saved_query')
        }
      } catch (error) {
        console.error('Failed to load saved query:', error)
      }
    }

    // Load from query history
    if (historyId && !isNaN(Number(historyId))) {
      try {
        const response = await apiRequest(`/api/queries/history/${historyId}`)
        const historyItem = response.data as QueryHistory

        if (response.ok) {
          setSql(historyItem.sqlContent || '')
          setExecutionMode('history')
          // Load parameter values if available
          if (historyItem.parameterValues) {
            const paramDefs = Object.entries(historyItem.parameterValues).map(([name, value]) => ({
              name,
              type: 'string', // Default type, could be improved by storing types in history
              value: String(value)
            }))
            setParameters(paramDefs)
          }
        }
      } catch (error) {
        console.error('Failed to load query history:', error)
      }
    }

    // If no queryId or historyId, this is a new query
    if (!queryId && !historyId) {
      setExecutionMode('new')
    }

    if (connectionId && !isNaN(Number(connectionId))) {
      setSelectedConnectionId(Number(connectionId))
    }
  }, [apiRequest])

  // Use ref to track the latest parameters without causing re-renders
  const parametersRef = useRef<ParameterDefinition[]>([])
  parametersRef.current = parameters

  const extractParametersFromSql = useCallback(() => {
    // Use the sophisticated parameter extractor that ignores parameters in strings and comments
    const paramNames = extractParameters(sql)

    if (paramNames.length > 0) {
      const newParams = paramNames.map(name => {
        const existing = parametersRef.current.find(p => p.name === name)
        return existing || {name, type: 'string', value: ''}
      })
      
      // Only update if parameters actually changed
      const currentParams = parametersRef.current
      const hasChanges = currentParams.length !== newParams.length ||
        currentParams.some((param, index) => 
          !newParams[index] || param.name !== newParams[index].name
        )
        
      if (hasChanges) {
        setParameters(newParams)
      }
    } else if (parametersRef.current.length > 0) {
      setParameters([])
    }
  }, [sql])

  // useEffect hooks that use the functions defined above
  useEffect(() => {
    loadConnections()
    loadFromUrl()
  }, [loadConnections, loadFromUrl])

  useEffect(() => {
    extractParametersFromSql()
  }, [extractParametersFromSql])

  const validateSql = async () => {
    if (!sql.trim()) {
      setError(t('sqlExecution.enterQueryError'))
      return false
    }

    if (!selectedConnectionId) {
      setError(t('sqlExecution.selectConnectionError'))
      return false
    }

    try {
      const response = await apiRequest('/api/sql/validate', {
        method: 'POST',
        body: JSON.stringify({
          sql: sql,
          connectionId: selectedConnectionId
        })
      })

      const data = response.data as SqlValidationResult
      if (!response.ok) {
        setError(data.error || t('sqlExecution.validationFailed'))
        return false
      }

      setError(null)
      return true
    } catch (err) {
      setError(t('sqlExecution.validationError') + ': ' + (err as Error).message)
      return false
    }
  }

  const executeSql = async () => {
    setLoading(true)
    setError(null)
    setResult(null)

    try {
      const isValid = await validateSql()
      if (!isValid) return

      const hasParameters = parameters.length > 0

      if (selectedConnectionId === null) {
        setError(t('sqlExecution.noConnectionSelected'))
        return
      }

      const requestBody: SqlExecutionRequest = {
        sql: sql,
        connectionId: selectedConnectionId
      }

      // Include saved query ID if this is a saved query execution
      if (currentQueryId) {
        requestBody.savedQueryId = currentQueryId
      }

      // Include pagination if enabled
      if (paging.enabled) {
        requestBody.pagingRequest = paging
      }

      if (hasParameters) {
        requestBody.parameters = {}
        requestBody.parameterTypes = {}

        parameters.forEach(param => {
          requestBody.parameters![param.name] = param.value
          requestBody.parameterTypes![param.name] = param.type
        })
      }

      const response = await apiRequest('/api/sql/execute', {
        method: 'POST',
        body: JSON.stringify(requestBody)
      })

      const data = response.data as SqlExecutionResult
      if (response.ok && data.ok) {
        setResult(data)
        // Execution count is now automatically recorded on the backend
      } else {
        // Handle both API errors and SQL execution errors
        const errorMessage = data.error || 'SQL execution failed'
        setError(errorMessage || t('sqlExecution.executionFailed'))
      }
    } catch (err) {
      setError(t('sqlExecution.executionError') + ': ' + (err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const handleParameterChange = (index: number, field: keyof ParameterDefinition, value: string) => {
    const newParams = [...parameters]
    newParams[index] = {...newParams[index], [field]: value}
    setParameters(newParams)
  }

  const handlePagingChange = (newPaging: PagingRequest) => {
    setPaging(newPaging)
  }

  const handlePageChange = (page: number) => {
    setPaging(prev => ({...prev, page}))
    // Re-execute query with new page
    executeSql()
  }

  const handlePageSizeChange = (pageSize: number) => {
    setPaging(prev => ({...prev, pageSize, page: 0}))
    // Re-execute query with new page size
    executeSql()
  }


  const sqlContainsOrderBy = hasOrderByClause(sql)

  return (
    <Layout title={t('sqlExecution.title')}>
      <div className="sql-execution">
        <div className="connection-selector">
          <label htmlFor="connection-select">{t('sqlExecution.databaseConnection')}:</label>
          <select
            id="connection-select"
            value={selectedConnectionId || ''}
            onChange={(e) => setSelectedConnectionId(Number(e.target.value))}
          >
            <option value="">{t('sqlExecution.selectConnection')}</option>
            {connections.map(conn => (
              <option key={conn.id} value={conn.id}>
                {conn.connectionName} ({conn.databaseType})
              </option>
            ))}
          </select>
        </div>

        {/* Execution Mode Indicator */}
        {executionMode && executionMode !== 'new' && (
          <div className="execution-mode-indicator">
            {executionMode === 'saved_query' ? (
              <div className="mode-badge saved-query-mode">
                <span className="mode-icon">🔖</span>
                <span className="mode-text">
                {t('sqlExecution.executingQuery')}: <strong>{currentQueryName}</strong>
              </span>
                <span className="mode-note">({t('sqlExecution.sqlReadOnly')})</span>
              </div>
            ) : executionMode === 'history' ? (
              <div className="mode-badge history-mode">
                <span className="mode-icon">🕒</span>
                <span className="mode-text">
                {t('sqlExecution.reExecutingHistory')}
              </span>
                <span className="mode-note">({t('sqlExecution.sqlEditable')})</span>
              </div>
            ) : null}
          </div>
        )}

        <div className="sql-input-section">
          <label htmlFor="sql-textarea">{t('sqlExecution.sqlQuery')}:</label>
          <textarea
            id="sql-textarea"
            value={sql}
            onChange={(e) => setSql(e.target.value)}
            placeholder={t('sqlExecution.enterQuery')}
            rows={8}
            readOnly={executionMode === 'saved_query'}
            style={{
              backgroundColor: executionMode === 'saved_query' ? '#f8f9fa' : 'white',
              cursor: executionMode === 'saved_query' ? 'not-allowed' : 'text'
            }}
          />
        </div>

        {parameters.length > 0 && (
          <div className="parameters-section">
            <h3>{t('sqlExecution.queryParameters')}</h3>
            {parameters.map((param, index) => (
              <div key={param.name} className="parameter-row">
                <label>:{param.name}</label>
                <select
                  value={param.type}
                  onChange={(e) => handleParameterChange(index, 'type', e.target.value)}
                >
                  <option value="string">String</option>
                  <option value="int">Integer</option>
                  <option value="long">Long</option>
                  <option value="double">Double</option>
                  <option value="boolean">Boolean</option>
                  <option value="date">Date</option>
                  <option value="time">Time</option>
                  <option value="datetime">DateTime</option>
                </select>
                <input
                  type={param.type === 'boolean' ? 'checkbox' : 'text'}
                  value={param.type === 'boolean' ? undefined : param.value}
                  checked={param.type === 'boolean' ? param.value === 'true' : undefined}
                  onChange={(e) => handleParameterChange(
                    index,
                    'value',
                    param.type === 'boolean' ? e.target.checked.toString() : e.target.value
                  )}
                  placeholder={t('sqlExecution.enterValue', {type: param.type})}
                />
              </div>
            ))}
          </div>
        )}

        <PaginationSettings
          paging={paging}
          onPagingChange={handlePagingChange}
          sqlContainsOrderBy={sqlContainsOrderBy}
        />

        <div className="execution-controls">
          <button
            onClick={executeSql}
            disabled={loading || !selectedConnectionId || !sql.trim()}
            className="execute-btn"
          >
            {loading ? t('sqlExecution.executing') : t('sqlExecution.executeQuery')}
          </button>
          <button
            onClick={validateSql}
            disabled={loading || !selectedConnectionId || !sql.trim()}
            className="validate-btn"
          >
            {t('sqlExecution.validateSQL')}
          </button>
        </div>

        {error && (
          <div className="error-message">
            <strong>{t('common.error')}:</strong> {error}
          </div>
        )}

        {result && (
          <div className="result-section">
            <div className="result-header">
              <h3>{t('sqlExecution.queryResult')}</h3>
              <div className="result-meta">
                <span>{t('sqlExecution.rows')}: {result.data?.rowCount || 0}</span>
                <span>{t('sqlExecution.executionTime')}: {result.executionTime}{t('sqlExecution.ms')}</span>
              </div>
            </div>


            {result.data?.columns && result.data.columns.length > 0 && (
              <>
                <div className="result-table-container">
                  <table className="result-table">
                    <thead>
                    <tr>
                      {result.data!.columns.map((column, index) => (
                        <th key={index}>{column}</th>
                      ))}
                    </tr>
                    </thead>
                    <tbody>
                    {result.data!.rows.map((row, rowIndex) => (
                      <tr key={rowIndex}>
                        {result.data!.columns.map((_, colIndex) => (
                          <td key={colIndex}>
                            {row[colIndex] !== null && row[colIndex] !== undefined
                              ? String(row[colIndex])
                              : 'NULL'}
                          </td>
                        ))}
                      </tr>
                    ))}
                    </tbody>
                  </table>
                </div>

                {result.data.paging && (
                  <Pagination
                    paging={result.data.paging}
                    onPageChange={handlePageChange}
                    onPageSizeChange={handlePageSizeChange}
                  />
                )}
              </>
            )}
          </div>
        )}
      </div>
    </Layout>
  )
}

export default SqlExecutionPage
