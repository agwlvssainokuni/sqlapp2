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

interface SavedQuery {
  id: number
  name: string
  sqlContent: string
  description: string
  parameterDefinitions?: Record<string, string>
  sharingScope: 'PRIVATE' | 'PUBLIC'
  username: string
  userId: number
  defaultConnection?: {
    id: number
    connectionName: string
    databaseType: string
  }
  createdAt: string
  updatedAt: string
  lastExecutedAt?: string
  executionCount?: number
}

interface DatabaseConnection {
  id: number
  connectionName: string
  databaseType: string
  host: string
  port: number
}

interface SavedQueryForm {
  name: string
  sqlContent: string
  description: string
  sharingScope: 'PRIVATE' | 'PUBLIC'
  defaultConnectionId?: number
}

const SavedQueries: React.FC = () => {
  const {t} = useTranslation()
  const {apiRequest} = useAuth()
  const [savedQueries, setSavedQueries] = useState<SavedQuery[]>([])
  const [publicQueries, setPublicQueries] = useState<SavedQuery[]>([])
  const [connections, setConnections] = useState<DatabaseConnection[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Form state
  const [showForm, setShowForm] = useState(false)
  const [editingQuery, setEditingQuery] = useState<SavedQuery | null>(null)
  const [formData, setFormData] = useState<SavedQueryForm>({
    name: '',
    sqlContent: '',
    description: '',
    sharingScope: 'PRIVATE'
  })

  // View state
  const [activeTab, setActiveTab] = useState<'my-queries' | 'public-queries'>('my-queries')
  const [searchTerm, setSearchTerm] = useState('')

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)

      const [savedQueriesResp, publicQueriesResp, connectionsResp] = await Promise.all([
        apiRequest('/api/queries/saved'),
        apiRequest('/api/queries/public'),
        apiRequest('/api/connections')
      ])

      const savedQueriesRes = await savedQueriesResp.json()
      const publicQueriesRes = await publicQueriesResp.json()
      const connectionsRes = await connectionsResp.json()

      setSavedQueries(Array.isArray(savedQueriesRes) ? savedQueriesRes : [])
      setPublicQueries(Array.isArray(publicQueriesRes) ? publicQueriesRes : [])
      setConnections(Array.isArray(connectionsRes) ? connectionsRes : [])
    } catch (err) {
      console.error('Failed to load data:', err)
      setError(t('savedQueries.loadFailed'))
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async () => {
    try {
      setError(null)

      if (!formData.name.trim() || !formData.sqlContent.trim()) {
        setError(t('savedQueries.nameRequired'))
        return
      }

      const endpoint = editingQuery
        ? `/api/queries/saved/${editingQuery.id}`
        : '/api/queries/saved'

      const method = editingQuery ? 'PUT' : 'POST'

      const response = await apiRequest(endpoint, {
        method,
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(formData)
      })

      if (!response.ok) {
        throw new Error(`API request failed: ${response.status}`)
      }

      await loadData()
      resetForm()
    } catch (err) {
      console.error('Failed to save query:', err)
      setError(t('savedQueries.saveFailed'))
    }
  }

  const handleEdit = (query: SavedQuery) => {
    setEditingQuery(query)
    setFormData({
      name: query.name,
      sqlContent: query.sqlContent,
      description: query.description,
      sharingScope: query.sharingScope,
      defaultConnectionId: query.defaultConnection?.id
    })
    setShowForm(true)
  }

  const handleDelete = async (queryId: number) => {
    if (!window.confirm(t('savedQueries.confirmDelete'))) {
      return
    }

    try {
      setError(null)
      const response = await apiRequest(`/api/queries/saved/${queryId}`, {method: 'DELETE'})

      if (!response.ok) {
        throw new Error(`Delete failed: ${response.status}`)
      }
      await loadData()
    } catch (err) {
      console.error('Failed to delete query:', err)
      setError(t('savedQueries.deleteFailed'))
    }
  }

  const handleExecute = (query: SavedQuery) => {
    // Navigate to SQL execution with query ID
    const sqlExecutionUrl = `/sql?queryId=${query.id}&connectionId=${query.defaultConnection?.id || ''}`
    window.location.href = sqlExecutionUrl
  }

  const resetForm = () => {
    setShowForm(false)
    setEditingQuery(null)
    setFormData({
      name: '',
      sqlContent: '',
      description: '',
      sharingScope: 'PRIVATE'
    })
  }

  const filteredMyQueries = Array.isArray(savedQueries) ? savedQueries.filter(query =>
    query.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    query.description?.toLowerCase().includes(searchTerm.toLowerCase())
  ) : []

  const filteredPublicQueries = Array.isArray(publicQueries) ? publicQueries.filter(query =>
    query.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    query.description?.toLowerCase().includes(searchTerm.toLowerCase())
  ) : []

  if (loading) {
    return (
      <div className="container">
        <div className="loading">{t('savedQueries.loading')}</div>
      </div>
    )
  }

  return (
    <div className="saved-queries">
      <div className="container">
        <div className="header">
          <h1>{t('savedQueries.title')}</h1>
          <button
            className="btn-primary"
            onClick={() => setShowForm(true)}
          >
            {t('savedQueries.createQuery')}
          </button>
        </div>

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {/* Tabs */}
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'my-queries' ? 'active' : ''}`}
            onClick={() => setActiveTab('my-queries')}
          >
            {t('savedQueries.myQueries')} ({Array.isArray(savedQueries) ? savedQueries.length : 0})
          </button>
          <button
            className={`tab ${activeTab === 'public-queries' ? 'active' : ''}`}
            onClick={() => setActiveTab('public-queries')}
          >
            {t('savedQueries.publicQueries')} ({Array.isArray(publicQueries) ? publicQueries.length : 0})
          </button>
        </div>

        {/* Search */}
        <div className="search-container">
          <input
            type="text"
            placeholder={t('savedQueries.searchPlaceholder')}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
          />
        </div>

        {/* Query List */}
        <div className="query-list">
          {activeTab === 'my-queries' ? (
            filteredMyQueries.length === 0 ? (
              <div className="no-data">{t('savedQueries.noSavedQueries')}</div>
            ) : (
              filteredMyQueries.map(query => (
                <QueryCard
                  key={query.id}
                  query={query}
                  isOwner={true}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onExecute={handleExecute}
                />
              ))
            )
          ) : (
            filteredPublicQueries.length === 0 ? (
              <div className="no-data">{t('savedQueries.noPublicQueries')}</div>
            ) : (
              filteredPublicQueries.map(query => (
                <QueryCard
                  key={query.id}
                  query={query}
                  isOwner={false}
                  onExecute={handleExecute}
                />
              ))
            )
          )}
        </div>

        {/* Form Modal */}
        {showForm && (
          <div className="modal-backdrop" onClick={() => resetForm()}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h3>{editingQuery ? t('savedQueries.editQuery') : t('savedQueries.createQuery')}</h3>
                <button className="close-btn" onClick={resetForm}>×</button>
              </div>

              <div className="form-group">
                <label>{t('savedQueries.queryName')} *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  placeholder={t('savedQueries.enterQueryName')}
                />
              </div>

              <div className="form-group">
                <label>{t('savedQueries.description')}</label>
                <input
                  type="text"
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  placeholder={t('savedQueries.enterDescription')}
                />
              </div>

              <div className="form-group">
                <label>{t('savedQueries.sqlContent')} *</label>
                <textarea
                  value={formData.sqlContent}
                  onChange={(e) => setFormData({...formData, sqlContent: e.target.value})}
                  placeholder={t('savedQueries.enterSQL')}
                  rows={10}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>{t('savedQueries.sharingScope')}</label>
                  <select
                    value={formData.sharingScope}
                    onChange={(e) => setFormData({
                      ...formData,
                      sharingScope: e.target.value as 'PRIVATE' | 'PUBLIC'
                    })}
                  >
                    <option value="PRIVATE">{t('savedQueries.private')}</option>
                    <option value="PUBLIC">{t('savedQueries.public')}</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>{t('savedQueries.defaultConnection')}</label>
                  <select
                    value={formData.defaultConnectionId || ''}
                    onChange={(e) => setFormData({
                      ...formData,
                      defaultConnectionId: e.target.value ? parseInt(e.target.value) : undefined
                    })}
                  >
                    <option value="">{t('savedQueries.noneSelected')}</option>
                    {connections.map(conn => (
                      <option key={conn.id} value={conn.id}>
                        {conn.connectionName} ({conn.databaseType})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="modal-actions">
                <button className="btn-secondary" onClick={resetForm}>
                  {t('savedQueries.cancel')}
                </button>
                <button className="btn-primary" onClick={handleSave}>
                  {editingQuery ? t('savedQueries.update') : t('savedQueries.save')}
                </button>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  )
}

interface QueryCardProps {
  query: SavedQuery
  isOwner: boolean
  onEdit?: (query: SavedQuery) => void
  onDelete?: (queryId: number) => void
  onExecute: (query: SavedQuery) => void
}

const QueryCard: React.FC<QueryCardProps> = ({query, isOwner, onEdit, onDelete, onExecute}) => {
  const {t} = useTranslation()
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('ja-JP')
  }

  return (
    <div className="saved-queries">
      <div className="query-card">
        <div className="query-header">
          <div className="query-title">
            <h3>{query.name}</h3>
            <span className={`scope-badge ${query.sharingScope?.toLowerCase() || 'private'}`}>
            {query.sharingScope === 'PUBLIC' ? t('savedQueries.public') : t('savedQueries.private')}
          </span>
          </div>
          <div className="query-actions">
            <button className="btn-execute" onClick={() => onExecute(query)}>
              {t('savedQueries.execute')}
            </button>
            {isOwner && onEdit && (
              <button className="btn-edit" onClick={() => onEdit(query)}>
                {t('savedQueries.edit')}
              </button>
            )}
            {isOwner && onDelete && (
              <button className="btn-delete" onClick={() => onDelete(query.id)}>
                {t('savedQueries.delete')}
              </button>
            )}
          </div>
        </div>

        {query.description && (
          <p className="query-description">{query.description}</p>
        )}

        <div className="query-sql">
          <pre>{query.sqlContent?.substring(0, 200)}{(query.sqlContent?.length || 0) > 200 ? '...' : ''}</pre>
        </div>

        <div className="query-meta">
          <div className="meta-row">
            <span>{t('savedQueries.createdBy')}: <strong>{query.username}</strong></span>
            {query.defaultConnection && (
              <span>{t('savedQueries.connection')}: <strong>{query.defaultConnection.connectionName}</strong></span>
            )}
          </div>
          <div className="meta-row">
            <span>{t('savedQueries.createdAt')}: {formatDate(query.createdAt)}</span>
            <span>{t('savedQueries.executionCount')}: {query.executionCount || 0}</span>
          </div>
          {query.lastExecutedAt && (
            <div className="meta-row">
              <span>{t('savedQueries.lastExecuted')}: {formatDate(query.lastExecutedAt)}</span>
            </div>
          )}
        </div>

      </div>
    </div>
  )
}

export default SavedQueries
