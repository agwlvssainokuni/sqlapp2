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
import { useAuth } from '../context/AuthContext'

interface DatabaseConnection {
  id: number
  connectionName: string
  databaseType: string
  host: string
  port: number
  databaseName: string
}

interface SqlExecutionResult {
  success: boolean
  rowCount: number
  executionTimeMs: number
  columns: string[]
  rows: Array<Record<string, any>>
  message?: string
  resultType?: string
  hasMoreRows?: boolean
  note?: string
}

interface ParameterDefinition {
  name: string
  type: string
  value: string
}

const SqlExecution: React.FC = () => {
  const { } = useAuth()
  const [connections, setConnections] = useState<DatabaseConnection[]>([])
  const [selectedConnectionId, setSelectedConnectionId] = useState<number | null>(null)
  const [sql, setSql] = useState('')
  const [parameters, setParameters] = useState<ParameterDefinition[]>([])
  const [result, setResult] = useState<SqlExecutionResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadConnections()
  }, [])

  useEffect(() => {
    extractParameters()
  }, [sql])

  const loadConnections = async () => {
    try {
      const token = localStorage.getItem('token')
      const response = await fetch('/api/connections', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (response.ok) {
        const data = await response.json()
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
  }

  const extractParameters = () => {
    const paramPattern = /:([\w]+)/g
    const matches = sql.match(paramPattern)
    if (matches) {
      const paramNames = [...new Set(matches.map(m => m.substring(1)))]
      const newParams = paramNames.map(name => {
        const existing = parameters.find(p => p.name === name)
        return existing || { name, type: 'string', value: '' }
      })
      setParameters(newParams)
    } else {
      setParameters([])
    }
  }

  const validateSql = async () => {
    if (!sql.trim()) {
      setError('Please enter SQL query')
      return false
    }

    if (!selectedConnectionId) {
      setError('Please select a database connection')
      return false
    }

    try {
      const token = localStorage.getItem('token')
      const response = await fetch('/api/sql/validate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          sql: sql,
          connectionId: selectedConnectionId
        })
      })

      const data = await response.json()
      if (!response.ok) {
        setError(data.error || 'SQL validation failed')
        return false
      }

      setError(null)
      return true
    } catch (err) {
      setError('Validation error: ' + (err as Error).message)
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

      const token = localStorage.getItem('token')
      const hasParameters = parameters.length > 0

      const requestBody: any = {
        sql: sql,
        connectionId: selectedConnectionId
      }

      if (hasParameters) {
        requestBody.parameters = {}
        requestBody.parameterTypes = {}
        
        parameters.forEach(param => {
          requestBody.parameters[param.name] = param.value
          requestBody.parameterTypes[param.name] = param.type
        })
      }

      const response = await fetch('/api/sql/execute', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(requestBody)
      })

      const data = await response.json()
      if (response.ok) {
        setResult(data)
      } else {
        setError(data.error || 'SQL execution failed')
      }
    } catch (err) {
      setError('Execution error: ' + (err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const handleParameterChange = (index: number, field: keyof ParameterDefinition, value: string) => {
    const newParams = [...parameters]
    newParams[index] = { ...newParams[index], [field]: value }
    setParameters(newParams)
  }

  return (
    <div className="sql-execution">
      <div className="sql-header">
        <h2>SQL Query Execution</h2>
        <div className="connection-selector">
          <label htmlFor="connection-select">Database Connection:</label>
          <select 
            id="connection-select"
            value={selectedConnectionId || ''}
            onChange={(e) => setSelectedConnectionId(Number(e.target.value))}
          >
            <option value="">Select Connection</option>
            {connections.map(conn => (
              <option key={conn.id} value={conn.id}>
                {conn.connectionName} ({conn.databaseType})
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="sql-input-section">
        <label htmlFor="sql-textarea">SQL Query:</label>
        <textarea
          id="sql-textarea"
          value={sql}
          onChange={(e) => setSql(e.target.value)}
          placeholder="Enter your SQL query here..."
          rows={8}
        />
      </div>

      {parameters.length > 0 && (
        <div className="parameters-section">
          <h3>Query Parameters</h3>
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
                placeholder={`Enter ${param.type} value`}
              />
            </div>
          ))}
        </div>
      )}

      <div className="execution-controls">
        <button 
          onClick={executeSql} 
          disabled={loading || !selectedConnectionId || !sql.trim()}
          className="execute-btn"
        >
          {loading ? 'Executing...' : 'Execute Query'}
        </button>
        <button 
          onClick={validateSql}
          disabled={loading || !selectedConnectionId || !sql.trim()}
          className="validate-btn"
        >
          Validate SQL
        </button>
      </div>

      {error && (
        <div className="error-message">
          <strong>Error:</strong> {error}
        </div>
      )}

      {result && (
        <div className="result-section">
          <div className="result-header">
            <h3>Query Result</h3>
            <div className="result-meta">
              <span>Rows: {result.rowCount}</span>
              <span>Execution Time: {result.executionTimeMs}ms</span>
            </div>
          </div>

          {result.message && (
            <div className="result-message">
              {result.message}
            </div>
          )}

          {result.hasMoreRows && result.note && (
            <div className="result-warning">
              ⚠️ {result.note}
            </div>
          )}

          {result.columns && result.columns.length > 0 && (
            <div className="result-table-container">
              <table className="result-table">
                <thead>
                  <tr>
                    {result.columns.map((column, index) => (
                      <th key={index}>{column}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {result.rows.map((row, rowIndex) => (
                    <tr key={rowIndex}>
                      {result.columns.map((column, colIndex) => (
                        <td key={colIndex}>
                          {row[column] !== null && row[column] !== undefined 
                            ? String(row[column]) 
                            : 'NULL'}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default SqlExecution