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
import type {DatabaseConnection, SchemaInfo, TableInfoResponse, TableDetailsResponse} from '../types/api'
import Layout from './Layout'

const SchemaViewer: React.FC = () => {
  const {t} = useTranslation()
  const {apiRequest} = useAuth()
  const [connections, setConnections] = useState<DatabaseConnection[]>([])
  const [selectedConnectionId, setSelectedConnectionId] = useState<number | null>(null)
  const [schemaInfo, setSchemaInfo] = useState<SchemaInfo | null>(null)
  const [tables, setTables] = useState<TableInfoResponse[]>([])
  const [selectedTable, setSelectedTable] = useState<string | null>(null)
  const [tableDetails, setTableDetails] = useState<TableDetailsResponse | null>(null)
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
      const response = await apiRequest('/api/connections?activeOnly=true')

      if (response.ok) {
        const data = await response.json()
        setConnections(data)
      } else {
        setError(t('schemaViewer.loadFailed'))
      }
    } catch (err) {
      setError(t('common.error') + ': ' + (err as Error).message)
    }
  }

  const loadSchemaInfo = async () => {
    if (!selectedConnectionId) return

    try {
      setLoading(true)
      const response = await apiRequest(`/api/schema/connections/${selectedConnectionId}`)

      if (response.ok) {
        const data = await response.json()
        setSchemaInfo(data)
      } else {
        const errorData = await response.json()
        setError(typeof errorData === 'string' ? errorData : t('schemaViewer.loadFailed'))
      }
    } catch (err) {
      setError(t('common.error') + ': ' + (err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const loadTables = async () => {
    if (!selectedConnectionId) return

    try {
      setLoading(true)
      const params = new URLSearchParams()
      if (selectedCatalog) params.append('catalog', selectedCatalog)
      if (selectedSchema) params.append('schema', selectedSchema)

      const response = await apiRequest(`/api/schema/connections/${selectedConnectionId}/tables?${params}`)

      if (response.ok) {
        const data = await response.json()
        setTables(data)
      } else {
        const errorData = await response.json()
        setError(typeof errorData === 'string' ? errorData : t('schemaViewer.loadFailed'))
      }
    } catch (err) {
      setError(t('common.error') + ': ' + (err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const loadTableDetails = async (tableName: string) => {
    if (!selectedConnectionId) return

    try {
      setLoading(true)
      const params = new URLSearchParams()
      if (selectedCatalog) params.append('catalog', selectedCatalog)
      if (selectedSchema) params.append('schema', selectedSchema)

      const response = await apiRequest(`/api/schema/connections/${selectedConnectionId}/tables/${tableName}?${params}`)

      if (response.ok) {
        const data = await response.json()
        setTableDetails(data)
        setSelectedTable(tableName)
      } else {
        const errorData = await response.json()
        setError(typeof errorData === 'string' ? errorData : t('schemaViewer.loadFailed'))
      }
    } catch (err) {
      setError(t('common.error') + ': ' + (err as Error).message)
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
    <Layout title={t('schemaViewer.title')}>
      <div className="schema-viewer">

        <div className="connection-selector">
          <label>{t('schemaViewer.selectConnectionLabel')}</label>
          <select
            value={selectedConnectionId || ''}
            onChange={(e) => handleConnectionChange(e.target.value)}
          >
            <option value="">{t('schemaViewer.selectConnection')}</option>
            {connections.map(conn => (
              <option key={conn.id} value={conn.id}>
                {conn.connectionName} ({conn.databaseType})
              </option>
            ))}
          </select>
        </div>

        {error && (
          <div className="error-message">
            <strong>{t('common.error')}:</strong> {error}
          </div>
        )}

        {schemaInfo && (
          <div className="schema-info">
            <h3>{t('schemaViewer.databaseInformation')}</h3>
            <div className="info-grid">
              <div>
                <strong>{t('schemaViewer.database')}</strong> {schemaInfo.databaseProductName} {schemaInfo.databaseProductVersion}
              </div>
              <div>
                <strong>{t('schemaViewer.driver')}</strong> {schemaInfo.driverName} {schemaInfo.driverVersion}
              </div>
            </div>

            {(schemaInfo.catalogs.length > 0 || schemaInfo.schemas.length > 0) && (
              <div className="schema-filters">
                {schemaInfo.catalogs.length > 0 && (
                  <div>
                    <label>{t('schemaViewer.catalog')}</label>
                    <select value={selectedCatalog}
                            onChange={(e) => setSelectedCatalog(e.target.value)}>
                      <option value="">{t('schemaViewer.allCatalogs')}</option>
                      {schemaInfo.catalogs.map(catalog => (
                        <option key={catalog} value={catalog}>{catalog}</option>
                      ))}
                    </select>
                  </div>
                )}

                {schemaInfo.schemas.length > 0 && (
                  <div>
                    <label>{t('schemaViewer.schema')}</label>
                    <select value={selectedSchema} onChange={(e) => setSelectedSchema(e.target.value)}>
                      <option value="">{t('schemaViewer.allSchemas')}</option>
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

        {loading && <div className="loading">{t('common.loading')}</div>}

        {tables.length > 0 && (
          <div className="tables-section">
            <h3>{t('schemaViewer.tables')}</h3>
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
            <h3>{t('schemaViewer.tableDetails')} {tableDetails.tableName}</h3>

            <div className="details-tabs">
              <div className="tab-content">
                <h4>{t('schemaViewer.columns')}</h4>
                <div className="columns-table">
                  <table>
                    <thead>
                    <tr>
                      <th>{t('schemaViewer.name')}</th>
                      <th>{t('schemaViewer.type')}</th>
                      <th>{t('schemaViewer.size')}</th>
                      <th>{t('schemaViewer.nullable')}</th>
                      <th>{t('schemaViewer.default')}</th>
                      <th>{t('schemaViewer.remarks')}</th>
                    </tr>
                    </thead>
                    <tbody>
                    {tableDetails.columns.map(column => (
                      <tr key={column.name}>
                        <td>{column.name}</td>
                        <td>{column.typeName}</td>
                        <td>{column.columnSize}{column.decimalDigits > 0 && `,${column.decimalDigits}`}</td>
                        <td>{column.nullable ? t('common.yes') : t('common.no')}</td>
                        <td>{column.defaultValue || '-'}</td>
                        <td>{column.remarks || '-'}</td>
                      </tr>
                    ))}
                    </tbody>
                  </table>
                </div>

                {tableDetails.primaryKeys.length > 0 && (
                  <div className="primary-keys">
                    <h4>{t('schemaViewer.primaryKeys')}</h4>
                    <ul>
                      {tableDetails.primaryKeys.map(pk => (
                        <li key={pk.columnName}>{pk.columnName} (PK: {pk.pkName})</li>
                      ))}
                    </ul>
                  </div>
                )}

                {tableDetails.foreignKeys.length > 0 && (
                  <div className="foreign-keys">
                    <h4>{t('schemaViewer.foreignKeys')}</h4>
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
                    <h4>{t('schemaViewer.indexes')}</h4>
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
    </Layout>
  )
}

export default SchemaViewer
