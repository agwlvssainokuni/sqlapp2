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
}

interface SchemaInfo {
  databaseProductName: string
  databaseProductVersion: string
  driverName: string
  driverVersion: string
  catalogs: string[]
  schemas: Array<{ name: string; catalog: string }>
}

interface Table {
  catalog: string
  schema: string
  name: string
  type: string
  remarks: string
}

interface Column {
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

interface TableDetails {
  tableName: string
  catalog: string
  schema: string
  columns: Column[]
  primaryKeys: Array<{ columnName: string; keySeq: number; pkName: string }>
  foreignKeys: Array<{
    pkTableName: string
    pkColumnName: string
    fkColumnName: string
    keySeq: number
    fkName: string
  }>
  indexes: Array<{
    indexName: string
    unique: boolean
    columnName: string
    ordinalPosition: number
    ascOrDesc: string
  }>
}

const SchemaViewer: React.FC = () => {
  const { } = useAuth()
  const [connections, setConnections] = useState<DatabaseConnection[]>([])
  const [selectedConnectionId, setSelectedConnectionId] = useState<number | null>(null)
  const [schemaInfo, setSchemaInfo] = useState<SchemaInfo | null>(null)
  const [tables, setTables] = useState<Table[]>([])
  const [selectedTable, setSelectedTable] = useState<string | null>(null)
  const [tableDetails, setTableDetails] = useState<TableDetails | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [selectedCatalog, setSelectedCatalog] = useState<string>('')
  const [selectedSchema, setSelectedSchema] = useState<string>('')

  useEffect(() => {
    loadConnections()
  }, [])

  useEffect(() => {
    if (selectedConnectionId) {
      loadSchemaInfo()
      loadTables()
    }
  }, [selectedConnectionId, selectedCatalog, selectedSchema])

  const loadConnections = async () => {
    try {
      const token = localStorage.getItem('token')
      const response = await fetch('/api/connections?activeOnly=true', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (response.ok) {
        const data = await response.json()
        setConnections(data)
      } else {
        setError('Failed to load connections')
      }
    } catch (err) {
      setError('Error loading connections: ' + (err as Error).message)
    }
  }

  const loadSchemaInfo = async () => {
    if (!selectedConnectionId) return

    try {
      setLoading(true)
      const token = localStorage.getItem('token')
      const response = await fetch(`/api/schema/connections/${selectedConnectionId}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (response.ok) {
        const data = await response.json()
        setSchemaInfo(data)
      } else {
        const errorData = await response.json()
        setError(typeof errorData === 'string' ? errorData : 'Failed to load schema info')
      }
    } catch (err) {
      setError('Error loading schema info: ' + (err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const loadTables = async () => {
    if (!selectedConnectionId) return

    try {
      setLoading(true)
      const token = localStorage.getItem('token')
      const params = new URLSearchParams()
      if (selectedCatalog) params.append('catalog', selectedCatalog)
      if (selectedSchema) params.append('schema', selectedSchema)
      
      const response = await fetch(`/api/schema/connections/${selectedConnectionId}/tables?${params}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (response.ok) {
        const data = await response.json()
        setTables(data)
      } else {
        const errorData = await response.json()
        setError(typeof errorData === 'string' ? errorData : 'Failed to load tables')
      }
    } catch (err) {
      setError('Error loading tables: ' + (err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const loadTableDetails = async (tableName: string) => {
    if (!selectedConnectionId) return

    try {
      setLoading(true)
      const token = localStorage.getItem('token')
      const params = new URLSearchParams()
      if (selectedCatalog) params.append('catalog', selectedCatalog)
      if (selectedSchema) params.append('schema', selectedSchema)
      
      const response = await fetch(`/api/schema/connections/${selectedConnectionId}/tables/${tableName}?${params}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (response.ok) {
        const data = await response.json()
        setTableDetails(data)
        setSelectedTable(tableName)
      } else {
        const errorData = await response.json()
        setError(typeof errorData === 'string' ? errorData : 'Failed to load table details')
      }
    } catch (err) {
      setError('Error loading table details: ' + (err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const handleConnectionChange = (connectionId: string) => {
    const id = parseInt(connectionId)
    setSelectedConnectionId(id)
    setSchemaInfo(null)
    setTables([])
    setSelectedTable(null)
    setTableDetails(null)
    setSelectedCatalog('')
    setSelectedSchema('')
    setError(null)
  }

  const handleTableClick = (tableName: string) => {
    if (selectedTable === tableName) {
      setSelectedTable(null)
      setTableDetails(null)
    } else {
      loadTableDetails(tableName)
    }
  }

  return (
    <div className="schema-viewer">
      <div className="schema-header">
        <h2>Database Schema Viewer</h2>
        
        <div className="connection-selector">
          <label>Select Connection:</label>
          <select 
            value={selectedConnectionId || ''} 
            onChange={(e) => handleConnectionChange(e.target.value)}
          >
            <option value="">Select a database connection</option>
            {connections.map(conn => (
              <option key={conn.id} value={conn.id}>
                {conn.connectionName} ({conn.databaseType})
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && (
        <div className="error-message">
          <strong>Error:</strong> {error}
        </div>
      )}

      {schemaInfo && (
        <div className="schema-info">
          <h3>Database Information</h3>
          <div className="info-grid">
            <div><strong>Database:</strong> {schemaInfo.databaseProductName} {schemaInfo.databaseProductVersion}</div>
            <div><strong>Driver:</strong> {schemaInfo.driverName} {schemaInfo.driverVersion}</div>
          </div>

          {(schemaInfo.catalogs.length > 0 || schemaInfo.schemas.length > 0) && (
            <div className="schema-filters">
              {schemaInfo.catalogs.length > 0 && (
                <div>
                  <label>Catalog:</label>
                  <select value={selectedCatalog} onChange={(e) => setSelectedCatalog(e.target.value)}>
                    <option value="">All Catalogs</option>
                    {schemaInfo.catalogs.map(catalog => (
                      <option key={catalog} value={catalog}>{catalog}</option>
                    ))}
                  </select>
                </div>
              )}

              {schemaInfo.schemas.length > 0 && (
                <div>
                  <label>Schema:</label>
                  <select value={selectedSchema} onChange={(e) => setSelectedSchema(e.target.value)}>
                    <option value="">All Schemas</option>
                    {schemaInfo.schemas.map(schema => (
                      <option key={schema.name} value={schema.name}>{schema.name}</option>
                    ))}
                  </select>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {loading && <div className="loading">Loading...</div>}

      {tables.length > 0 && (
        <div className="tables-section">
          <h3>Tables and Views</h3>
          <div className="tables-list">
            {tables.map(table => (
              <div 
                key={`${table.catalog || ''}.${table.schema || ''}.${table.name}`}
                className={`table-item ${selectedTable === table.name ? 'selected' : ''}`}
                onClick={() => handleTableClick(table.name)}
              >
                <div className="table-name">{table.name}</div>
                <div className="table-type">{table.type}</div>
                {table.remarks && <div className="table-remarks">{table.remarks}</div>}
              </div>
            ))}
          </div>
        </div>
      )}

      {tableDetails && (
        <div className="table-details">
          <h3>Table Details: {tableDetails.tableName}</h3>
          
          <div className="details-tabs">
            <div className="tab-content">
              <h4>Columns</h4>
              <div className="columns-table">
                <table>
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Type</th>
                      <th>Size</th>
                      <th>Nullable</th>
                      <th>Default</th>
                      <th>Remarks</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tableDetails.columns.map(column => (
                      <tr key={column.name}>
                        <td>{column.name}</td>
                        <td>{column.typeName}</td>
                        <td>{column.columnSize}{column.decimalDigits > 0 && `,${column.decimalDigits}`}</td>
                        <td>{column.nullable ? 'Yes' : 'No'}</td>
                        <td>{column.defaultValue || '-'}</td>
                        <td>{column.remarks || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {tableDetails.primaryKeys.length > 0 && (
                <div className="primary-keys">
                  <h4>Primary Keys</h4>
                  <ul>
                    {tableDetails.primaryKeys.map(pk => (
                      <li key={pk.columnName}>{pk.columnName} (PK: {pk.pkName})</li>
                    ))}
                  </ul>
                </div>
              )}

              {tableDetails.foreignKeys.length > 0 && (
                <div className="foreign-keys">
                  <h4>Foreign Keys</h4>
                  <ul>
                    {tableDetails.foreignKeys.map((fk, index) => (
                      <li key={index}>
                        {fk.fkColumnName} → {fk.pkTableName}.{fk.pkColumnName} (FK: {fk.fkName})
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {tableDetails.indexes.length > 0 && (
                <div className="indexes">
                  <h4>Indexes</h4>
                  <ul>
                    {tableDetails.indexes.map((index, i) => (
                      <li key={i}>
                        {index.indexName} ({index.columnName}) {index.unique ? '[UNIQUE]' : ''}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default SchemaViewer