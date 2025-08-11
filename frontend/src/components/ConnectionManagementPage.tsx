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

import React, {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'
import {useAuth} from '../context/AuthContext'
import type {ConnectionTestResult, DatabaseConnection, NewConnection} from '../types/api'
import Layout from './Layout'

const ConnectionManagementPage: React.FC = () => {
  const {t} = useTranslation()
  const {apiRequest} = useAuth()
  const [connections, setConnections] = useState<DatabaseConnection[]>([])
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [editingConnection, setEditingConnection] = useState<DatabaseConnection | null>(null)
  const [newConnection, setNewConnection] = useState<NewConnection>({
    connectionName: '',
    databaseType: 'MYSQL',
    host: '',
    port: 3306,
    databaseName: '',
    username: '',
    password: '',
    additionalParams: ''
  })
  const [testing, setTesting] = useState<{ [key: number]: boolean }>({})
  const [testResults, setTestResults] = useState<{ [key: number]: ConnectionTestResult }>({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadConnections()
  }, [])

  const loadConnections = async () => {
    try {
      setLoading(true)
      const response = await apiRequest('/api/connections')

      if (response.ok) {
        const data = response.data
        setConnections(data)
      } else {
        setError(t('connections.loadConnectionsFailed'))
      }
    } finally {
      setLoading(false)
    }
  }

  const handleDatabaseTypeChange = (type: 'MYSQL' | 'POSTGRESQL' | 'MARIADB') => {
    const defaultPorts = {
      MYSQL: 3306,
      POSTGRESQL: 5432,
      MARIADB: 3306
    }

    setNewConnection(prev => ({
      ...prev,
      databaseType: type,
      port: defaultPorts[type]
    }))
  }

  const createConnection = async () => {
    try {
      setLoading(true)
      setError(null)

      const response = await apiRequest('/api/connections', {
        method: 'POST',
        body: JSON.stringify(newConnection)
      })

      if (response.ok) {
        await loadConnections()
        setShowCreateForm(false)
        resetForm()
      } else {
        const errorData = response.error?.join('\n')
        setError(errorData || 'Failed to create connection')
      }
    } finally {
      setLoading(false)
    }
  }

  const updateConnection = async () => {
    if (!editingConnection) return

    try {
      setLoading(true)
      setError(null)

      const response = await apiRequest(`/api/connections/${editingConnection.id}`, {
        method: 'PUT',
        body: JSON.stringify(newConnection)
      })

      if (response.ok) {
        await loadConnections()
        setEditingConnection(null)
        resetForm()
      } else {
        const errorData = response.error?.join('\n')
        setError(errorData || 'Failed to update connection')
      }
    } finally {
      setLoading(false)
    }
  }

  const deleteConnection = async (id: number) => {
    if (!confirm(t('connections.confirmDelete'))) return

    try {
      setLoading(true)
      const response = await apiRequest(`/api/connections/${id}`, {
        method: 'DELETE'
      })

      if (response.ok) {
        await loadConnections()
      } else {
        const errorData = response.error?.join('\n')
        setError(errorData || 'Failed to delete connection')
      }
    } finally {
      setLoading(false)
    }
  }

  const testConnection = async (connection: DatabaseConnection) => {
    try {
      setTesting(prev => ({...prev, [connection.id]: true}))
      setTestResults(prev => ({...prev, [connection.id]: {success: false, message: t('connections.testing')}}))

      const response = await apiRequest(`/api/connections/${connection.id}/test`, {
        method: 'POST'
      })

      if (response.ok) {
        const result = response.data
        if (result.success) {
          setTestResults(prev => ({
            ...prev,
            [connection.id]: result
          }))
        } else {
          setTestResults(prev => ({
            ...prev,
            [connection.id]: {
              ...result,
              message: t('connections.connectionFailed') + ': ' + result.message
            }
          }))
        }
      } else {
        const errorData = response.error?.join('\n')
        setError(errorData || 'Failed to delete connection')
      }
    } finally {
      setTesting(prev => ({...prev, [connection.id]: false}))
    }
  }

  const resetForm = () => {
    setNewConnection({
      connectionName: '',
      databaseType: 'MYSQL',
      host: '',
      port: 3306,
      databaseName: '',
      username: '',
      password: '',
      additionalParams: ''
    })
  }

  const startEdit = (connection: DatabaseConnection) => {
    setEditingConnection(connection)
    setNewConnection({
      connectionName: connection.connectionName,
      databaseType: connection.databaseType,
      host: connection.host,
      port: connection.port,
      databaseName: connection.databaseName,
      username: connection.username,
      password: '', // Don't populate password for security
      additionalParams: connection.additionalParams || ''
    })
    setShowCreateForm(true)
  }

  const cancelEdit = () => {
    setEditingConnection(null)
    setShowCreateForm(false)
    resetForm()
  }

  const getDatabaseTypeLabel = (type: string) => {
    const labels = {
      MYSQL: 'MySQL',
      POSTGRESQL: 'PostgreSQL',
      MARIADB: 'MariaDB'
    }
    return labels[type as keyof typeof labels] || type
  }

  return (
    <Layout title={t('connections.title')}>
      <div className="connection-management">
        <div className="connection-header">
          <button
            onClick={() => setShowCreateForm(true)}
            disabled={loading}
            className="create-btn"
          >
            {t('connections.addConnection')}
          </button>
        </div>

        {error && (
          <div className="error-message">
            <strong>{t('common.error')}:</strong> {error}
          </div>
        )}

        {showCreateForm && (
          <div className="connection-form">
            <h3>{editingConnection ? t('connections.editConnection') : t('connections.addConnection')}</h3>

            <div className="form-grid">
              <div className="form-group">
                <label>{t('connections.connectionName')} *</label>
                <input
                  type="text"
                  value={newConnection.connectionName}
                  onChange={(e) => setNewConnection(prev => ({...prev, connectionName: e.target.value}))}
                  placeholder={t('connections.connectionName')}
                />
              </div>

              <div className="form-group">
                <label>{t('connections.databaseType')} *</label>
                <select
                  value={newConnection.databaseType}
                  onChange={(e) => handleDatabaseTypeChange(e.target.value as 'MYSQL' | 'POSTGRESQL' | 'MARIADB')}
                >
                  <option value="MYSQL">MySQL</option>
                  <option value="POSTGRESQL">PostgreSQL</option>
                  <option value="MARIADB">MariaDB</option>
                </select>
              </div>

              <div className="form-group">
                <label>{t('connections.host')} *</label>
                <input
                  type="text"
                  value={newConnection.host}
                  onChange={(e) => setNewConnection(prev => ({...prev, host: e.target.value}))}
                  placeholder="localhost"
                />
              </div>

              <div className="form-group">
                <label>{t('connections.port')} *</label>
                <input
                  type="number"
                  value={newConnection.port}
                  onChange={(e) => setNewConnection(prev => ({
                    ...prev,
                    port: parseInt(e.target.value) || 3306
                  }))}
                  min="1"
                  max="65535"
                />
              </div>

              <div className="form-group">
                <label>{t('connections.databaseName')} *</label>
                <input
                  type="text"
                  value={newConnection.databaseName}
                  onChange={(e) => setNewConnection(prev => ({...prev, databaseName: e.target.value}))}
                  placeholder="myapp"
                />
              </div>

              <div className="form-group">
                <label>{t('connections.username')} *</label>
                <input
                  type="text"
                  value={newConnection.username}
                  onChange={(e) => setNewConnection(prev => ({...prev, username: e.target.value}))}
                  placeholder="dbuser"
                />
              </div>

              <div className="form-group">
                <label>{t('connections.password')} *</label>
                <input
                  type="password"
                  value={newConnection.password}
                  onChange={(e) => setNewConnection(prev => ({...prev, password: e.target.value}))}
                  placeholder={editingConnection ? t('connections.passwordPlaceholder') : t('connections.password')}
                />
              </div>

              <div className="form-group full-width">
                <label>{t('connections.additionalParameters')}</label>
                <input
                  type="text"
                  value={newConnection.additionalParams}
                  onChange={(e) => setNewConnection(prev => ({
                    ...prev,
                    additionalParams: e.target.value
                  }))}
                  placeholder="e.g., useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                />
                <small className="form-help">
                  Optional JDBC URL parameters. For MySQL SSL error, use:
                  useSSL=true&allowPublicKeyRetrieval=true
                </small>
              </div>
            </div>

            <div className="form-actions">
              <button
                onClick={editingConnection ? updateConnection : createConnection}
                disabled={loading || !newConnection.connectionName || !newConnection.host || !newConnection.username}
                className="save-btn"
              >
                {loading ? `${t('connections.save')}...` : editingConnection ? t('connections.editConnection') : t('connections.addConnection')}
              </button>
              <button
                onClick={cancelEdit}
                disabled={loading}
                className="cancel-btn"
              >
                {t('connections.cancel')}
              </button>
            </div>
          </div>
        )}

        <div className="connections-list">
          <h3>{t('connections.title')}</h3>

          {loading && !showCreateForm && (
            <div className="loading">{t('common.loading')}</div>
          )}

          {connections.length === 0 && !loading ? (
            <div className="no-connections">
              <p>{t('connections.noConnections')}</p>
              <p>{t('connections.getStartedMessage', { action: t('connections.addConnection') })}</p>
            </div>
          ) : (
            <div className="connections-grid">
              {connections.map(connection => (
                <div key={connection.id} className="connection-card">
                  <div className="connection-header">
                    <h4>{connection.connectionName}</h4>
                    <div className="connection-type">
                      {getDatabaseTypeLabel(connection.databaseType)}
                    </div>
                  </div>

                  <div className="connection-details">
                    <div className="detail-row">
                      <span className="label">{t('connections.host')}:</span>
                      <span>{connection.host}:{connection.port}</span>
                    </div>
                    <div className="detail-row">
                      <span className="label">{t('connections.databaseName')}:</span>
                      <span>{connection.databaseName}</span>
                    </div>
                    <div className="detail-row">
                      <span className="label">{t('connections.username')}:</span>
                      <span>{connection.username}</span>
                    </div>
                    <div className="detail-row">
                      <span className="label">{t('connections.status')}:</span>
                      <span className={connection.isActive ? 'status-active' : 'status-inactive'}>
                      {connection.isActive ? t('connections.active') : t('connections.inactive')}
                    </span>
                    </div>
                  </div>

                  {testResults[connection.id] && (
                    <div
                      className={`test-result ${testResults[connection.id].success ? 'success' : 'error'}`}>
                      {testResults[connection.id].message}
                      {testResults[connection.id].responseTimeMs && (
                        <span
                          className="response-time"> ({testResults[connection.id].responseTimeMs}ms)</span>
                      )}
                    </div>
                  )}

                  <div className="connection-actions">
                    <button
                      onClick={() => testConnection(connection)}
                      disabled={testing[connection.id]}
                      className="test-btn"
                    >
                      {testing[connection.id] ? t('connections.testing') : t('connections.testConnection')}
                    </button>
                    <button
                      onClick={() => startEdit(connection)}
                      disabled={loading}
                      className="edit-btn"
                    >
                      {t('connections.edit')}
                    </button>
                    <button
                      onClick={() => deleteConnection(connection.id)}
                      disabled={loading}
                      className="delete-btn"
                    >
                      {t('connections.delete')}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </Layout>
  )
}

export default ConnectionManagementPage
