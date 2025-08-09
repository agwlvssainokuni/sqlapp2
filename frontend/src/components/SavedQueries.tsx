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
  const { apiRequest } = useAuth()
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
      
      const [savedQueriesRes, publicQueriesRes, connectionsRes] = await Promise.all([
        apiRequest('/api/queries/saved'),
        apiRequest('/api/queries/public'),
        apiRequest('/api/connections')
      ])

      console.log('API Response - savedQueries:', savedQueriesRes)
      console.log('API Response - publicQueries:', publicQueriesRes)
      console.log('API Response - connections:', connectionsRes)
      
      setSavedQueries(Array.isArray(savedQueriesRes) ? savedQueriesRes : [])
      setPublicQueries(Array.isArray(publicQueriesRes) ? publicQueriesRes : [])
      setConnections(Array.isArray(connectionsRes) ? connectionsRes : [])
    } catch (err) {
      console.error('Failed to load data:', err)
      setError('データの読み込みに失敗しました')
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async () => {
    try {
      setError(null)
      
      if (!formData.name.trim() || !formData.sqlContent.trim()) {
        setError('クエリ名とSQL内容は必須です')
        return
      }

      const endpoint = editingQuery 
        ? `/api/queries/saved/${editingQuery.id}`
        : '/api/queries/saved'
      
      const method = editingQuery ? 'PUT' : 'POST'

      await apiRequest(endpoint, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      })

      await loadData()
      resetForm()
    } catch (err) {
      console.error('Failed to save query:', err)
      setError('クエリの保存に失敗しました')
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
    if (!window.confirm('このクエリを削除しますか？')) {
      return
    }

    try {
      setError(null)
      await apiRequest(`/api/queries/saved/${queryId}`, { method: 'DELETE' })
      await loadData()
    } catch (err) {
      console.error('Failed to delete query:', err)
      setError('クエリの削除に失敗しました')
    }
  }

  const handleExecute = (query: SavedQuery) => {
    // Navigate to SQL execution with pre-filled query
    const sqlExecutionUrl = `/sql?query=${encodeURIComponent(query.sqlContent)}&connection=${query.defaultConnection?.id || ''}`
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

  // Debug logs
  console.log('State - savedQueries:', savedQueries, 'Array?', Array.isArray(savedQueries))
  console.log('State - publicQueries:', publicQueries, 'Array?', Array.isArray(publicQueries))
  console.log('Filtered - My queries:', filteredMyQueries, 'Length:', filteredMyQueries.length)
  console.log('Filtered - Public queries:', filteredPublicQueries, 'Length:', filteredPublicQueries.length)
  console.log('Active tab:', activeTab)
  console.log('Search term:', searchTerm)

  if (loading) {
    return (
      <div className="container">
        <div className="loading">読み込み中...</div>
      </div>
    )
  }

  return (
    <div className="container">
      <div className="header">
        <h1>保存済みクエリ</h1>
        <button 
          className="btn-primary" 
          onClick={() => setShowForm(true)}
        >
          新規クエリ作成
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
          マイクエリ ({Array.isArray(savedQueries) ? savedQueries.length : 0})
        </button>
        <button 
          className={`tab ${activeTab === 'public-queries' ? 'active' : ''}`}
          onClick={() => setActiveTab('public-queries')}
        >
          公開クエリ ({Array.isArray(publicQueries) ? publicQueries.length : 0})
        </button>
      </div>

      {/* Search */}
      <div className="search-container">
        <input
          type="text"
          placeholder="クエリ名または説明で検索..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="search-input"
        />
      </div>

      {/* Query List */}
      <div className="query-list">
        {activeTab === 'my-queries' ? (
          filteredMyQueries.length === 0 ? (
            <div className="no-data">保存済みクエリがありません</div>
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
            <div className="no-data">公開クエリがありません</div>
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
              <h3>{editingQuery ? 'クエリ編集' : '新規クエリ作成'}</h3>
              <button className="close-btn" onClick={resetForm}>×</button>
            </div>
            
            <div className="form-group">
              <label>クエリ名 *</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({...formData, name: e.target.value})}
                placeholder="クエリ名を入力..."
              />
            </div>
            
            <div className="form-group">
              <label>説明</label>
              <input
                type="text"
                value={formData.description}
                onChange={(e) => setFormData({...formData, description: e.target.value})}
                placeholder="クエリの説明を入力..."
              />
            </div>
            
            <div className="form-group">
              <label>SQL内容 *</label>
              <textarea
                value={formData.sqlContent}
                onChange={(e) => setFormData({...formData, sqlContent: e.target.value})}
                placeholder="SQL文を入力..."
                rows={10}
              />
            </div>
            
            <div className="form-row">
              <div className="form-group">
                <label>共有設定</label>
                <select
                  value={formData.sharingScope}
                  onChange={(e) => setFormData({...formData, sharingScope: e.target.value as 'PRIVATE' | 'PUBLIC'})}
                >
                  <option value="PRIVATE">プライベート</option>
                  <option value="PUBLIC">公開</option>
                </select>
              </div>
              
              <div className="form-group">
                <label>デフォルト接続</label>
                <select
                  value={formData.defaultConnectionId || ''}
                  onChange={(e) => setFormData({...formData, defaultConnectionId: e.target.value ? parseInt(e.target.value) : undefined})}
                >
                  <option value="">選択なし</option>
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
                キャンセル
              </button>
              <button className="btn-primary" onClick={handleSave}>
                {editingQuery ? '更新' : '保存'}
              </button>
            </div>
          </div>
        </div>
      )}

      <style>{`
        .container {
          padding: 20px;
          max-width: 1200px;
          margin: 0 auto;
        }
        
        .header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
        }
        
        .header h1 {
          margin: 0;
        }
        
        .btn-primary {
          background-color: #007bff;
          color: white;
          border: none;
          padding: 10px 20px;
          border-radius: 5px;
          cursor: pointer;
        }
        
        .btn-primary:hover {
          background-color: #0056b3;
        }
        
        .btn-secondary {
          background-color: #6c757d;
          color: white;
          border: none;
          padding: 10px 20px;
          border-radius: 5px;
          cursor: pointer;
          margin-right: 10px;
        }
        
        .error-message {
          background-color: #f8d7da;
          border: 1px solid #f5c6cb;
          color: #721c24;
          padding: 10px;
          border-radius: 5px;
          margin-bottom: 20px;
        }
        
        .tabs {
          border-bottom: 2px solid #e9ecef;
          margin-bottom: 20px;
        }
        
        .tab {
          background: none;
          border: none;
          padding: 10px 20px;
          cursor: pointer;
          border-bottom: 2px solid transparent;
          margin-right: 10px;
        }
        
        .tab.active {
          border-bottom-color: #007bff;
          color: #007bff;
          font-weight: bold;
        }
        
        .search-container {
          margin-bottom: 20px;
        }
        
        .search-input {
          width: 100%;
          max-width: 400px;
          padding: 10px;
          border: 1px solid #ccc;
          border-radius: 5px;
        }
        
        .query-list {
          display: grid;
          gap: 20px;
        }
        
        .no-data {
          text-align: center;
          color: #6c757d;
          padding: 40px;
        }
        
        .loading {
          text-align: center;
          padding: 40px;
        }
        
        .modal-backdrop {
          position: fixed;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          justify-content: center;
          align-items: center;
          z-index: 1000;
        }
        
        .modal {
          background: white;
          padding: 20px;
          border-radius: 10px;
          width: 90%;
          max-width: 800px;
          max-height: 90%;
          overflow-y: auto;
        }
        
        .modal-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
        }
        
        .close-btn {
          background: none;
          border: none;
          font-size: 24px;
          cursor: pointer;
        }
        
        .form-group {
          margin-bottom: 15px;
        }
        
        .form-row {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 20px;
        }
        
        .form-group label {
          display: block;
          margin-bottom: 5px;
          font-weight: bold;
        }
        
        .form-group input,
        .form-group select,
        .form-group textarea {
          width: 100%;
          padding: 10px;
          border: 1px solid #ccc;
          border-radius: 5px;
          font-family: inherit;
        }
        
        .form-group textarea {
          resize: vertical;
          font-family: 'Consolas', 'Monaco', monospace;
        }
        
        .modal-actions {
          display: flex;
          justify-content: flex-end;
          margin-top: 20px;
        }
        
        @media (max-width: 768px) {
          .header {
            flex-direction: column;
            gap: 15px;
          }
          
          .form-row {
            grid-template-columns: 1fr;
          }
        }
      `}</style>
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

const QueryCard: React.FC<QueryCardProps> = ({ query, isOwner, onEdit, onDelete, onExecute }) => {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('ja-JP')
  }

  return (
    <div className="query-card">
      <div className="query-header">
        <div className="query-title">
          <h3>{query.name}</h3>
          <span className={`scope-badge ${query.sharingScope?.toLowerCase() || 'private'}`}>
            {query.sharingScope === 'PUBLIC' ? '公開' : 'プライベート'}
          </span>
        </div>
        <div className="query-actions">
          <button className="btn-execute" onClick={() => onExecute(query)}>
            実行
          </button>
          {isOwner && onEdit && (
            <button className="btn-edit" onClick={() => onEdit(query)}>
              編集
            </button>
          )}
          {isOwner && onDelete && (
            <button className="btn-delete" onClick={() => onDelete(query.id)}>
              削除
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
          <span>作成者: <strong>{query.username}</strong></span>
          {query.defaultConnection && (
            <span>接続: <strong>{query.defaultConnection.connectionName}</strong></span>
          )}
        </div>
        <div className="meta-row">
          <span>作成日: {formatDate(query.createdAt)}</span>
          <span>実行回数: {query.executionCount || 0}回</span>
        </div>
        {query.lastExecutedAt && (
          <div className="meta-row">
            <span>最終実行: {formatDate(query.lastExecutedAt)}</span>
          </div>
        )}
      </div>

      <style>{`
        .query-card {
          border: 1px solid #e9ecef;
          border-radius: 8px;
          padding: 20px;
          background: white;
        }
        
        .query-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 15px;
        }
        
        .query-title {
          display: flex;
          align-items: center;
          gap: 10px;
        }
        
        .query-title h3 {
          margin: 0;
        }
        
        .scope-badge {
          padding: 4px 8px;
          border-radius: 12px;
          font-size: 12px;
          font-weight: bold;
        }
        
        .scope-badge.private {
          background-color: #e9ecef;
          color: #495057;
        }
        
        .scope-badge.public {
          background-color: #d4edda;
          color: #155724;
        }
        
        .query-actions {
          display: flex;
          gap: 10px;
        }
        
        .btn-execute {
          background-color: #28a745;
          color: white;
          border: none;
          padding: 6px 12px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 12px;
        }
        
        .btn-edit {
          background-color: #ffc107;
          color: #212529;
          border: none;
          padding: 6px 12px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 12px;
        }
        
        .btn-delete {
          background-color: #dc3545;
          color: white;
          border: none;
          padding: 6px 12px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 12px;
        }
        
        .query-description {
          color: #6c757d;
          margin-bottom: 15px;
        }
        
        .query-sql {
          background-color: #f8f9fa;
          padding: 15px;
          border-radius: 5px;
          margin-bottom: 15px;
          overflow-x: auto;
        }
        
        .query-sql pre {
          margin: 0;
          font-family: 'Consolas', 'Monaco', monospace;
          font-size: 14px;
          white-space: pre-wrap;
        }
        
        .query-meta {
          color: #6c757d;
          font-size: 14px;
        }
        
        .meta-row {
          display: flex;
          justify-content: space-between;
          margin-bottom: 5px;
        }
        
        .meta-row:last-child {
          margin-bottom: 0;
        }
        
        @media (max-width: 768px) {
          .query-header {
            flex-direction: column;
            gap: 15px;
          }
          
          .query-actions {
            width: 100%;
            justify-content: flex-end;
          }
          
          .meta-row {
            flex-direction: column;
            gap: 5px;
          }
        }
      `}</style>
    </div>
  )
}

export default SavedQueries