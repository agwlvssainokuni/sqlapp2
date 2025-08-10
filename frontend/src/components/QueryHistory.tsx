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

interface QueryHistoryItem {
  id: number
  sqlContent: string
  parameterValues?: Record<string, any>
  executionTimeMs: number
  resultCount?: number
  isSuccessful: boolean
  errorMessage?: string
  connectionName: string
  databaseType: string
  connectionId?: number
  savedQueryId?: number
  savedQueryName?: string
  executedAt: string
}

interface Statistics {
  savedQueryCount: number
  executionCount: number
  averageExecutionTime?: number
  failedQueryCount: number
}

const QueryHistory: React.FC = () => {
  const { t } = useTranslation()
  const { apiRequest } = useAuth()
  const [history, setHistory] = useState<QueryHistoryItem[]>([])
  const [statistics, setStatistics] = useState<Statistics | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  
  // Filter state
  const [filterType, setFilterType] = useState<'all' | 'successful' | 'failed'>('all')
  const [searchTerm, setSearchTerm] = useState('')
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize] = useState(20)

  useEffect(() => {
    loadData()
  }, [filterType, currentPage])

  const loadData = async () => {
    try {
      setLoading(true)
      setError(null)
      
      let endpoint = '/api/queries/history'
      
      if (filterType === 'successful') {
        endpoint = '/api/queries/history/successful'
      } else if (filterType === 'failed') {
        endpoint = '/api/queries/history/failed'
      }
      
      const params = new URLSearchParams({
        page: currentPage.toString(),
        size: pageSize.toString()
      })
      
      const [historyResp, statsResp] = await Promise.all([
        apiRequest(`${endpoint}?${params}`),
        apiRequest('/api/queries/stats')
      ])

      const historyRes = await historyResp.json()
      const statsRes = await statsResp.json()

      setHistory(historyRes.content || [])
      setStatistics(statsRes)
    } catch (err) {
      console.error('Failed to load query history:', err)
      setError(t('queryHistory.loadFailed'))
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = async () => {
    if (!searchTerm.trim()) {
      loadData()
      return
    }

    try {
      setLoading(true)
      setError(null)
      
      const params = new URLSearchParams({
        searchTerm,
        page: currentPage.toString(),
        size: pageSize.toString()
      })
      
      const historyResp = await apiRequest(`/api/queries/history/search?${params}`)
      const historyRes = await historyResp.json()
      setHistory(historyRes.content || [])
    } catch (err) {
      console.error('Failed to search query history:', err)
      setError(t('queryHistory.searchFailed'))
    } finally {
      setLoading(false)
    }
  }

  const handleReExecute = (item: QueryHistoryItem) => {
    // If this history item originated from a saved query, re-execute the saved query
    // Otherwise, re-execute as history
    if (item.savedQueryId) {
      // Re-execute saved query - this will increment execution count
      const sqlExecutionUrl = `/sql?queryId=${item.savedQueryId}&connectionId=${item.connectionId || ''}`
      window.location.href = sqlExecutionUrl
    } else {
      // Re-execute as history - allows SQL editing
      const sqlExecutionUrl = `/sql?historyId=${item.id}&connectionId=${item.connectionId || ''}`
      window.location.href = sqlExecutionUrl
    }
  }

  const formatExecutionTime = (timeMs: number) => {
    if (timeMs < 1000) {
      return `${timeMs}ms`
    }
    return `${(timeMs / 1000).toFixed(2)}s`
  }

  const filteredHistory = searchTerm
    ? history.filter(item =>
        item.sqlContent.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.connectionName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (item.savedQueryName && item.savedQueryName.toLowerCase().includes(searchTerm.toLowerCase()))
      )
    : history

  if (loading && history.length === 0) {
    return (
      <div className="container">
        <div className="loading">{t('queryHistory.loading')}</div>
      </div>
    )
  }

  return (
    <div className="container">
      <div className="header">
        <h1>{t('queryHistory.title')}</h1>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {/* Statistics */}
      {statistics && (
        <div className="stats-container">
          <div className="stat-card">
            <div className="stat-value">{statistics.savedQueryCount}</div>
            <div className="stat-label">{t('queryHistory.savedQueries')}</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">{statistics.executionCount}</div>
            <div className="stat-label">{t('queryHistory.totalExecutions')}</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">
              {statistics.averageExecutionTime 
                ? formatExecutionTime(Math.round(statistics.averageExecutionTime))
                : 'N/A'
              }
            </div>
            <div className="stat-label">{t('queryHistory.averageExecutionTime')}</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">{statistics.failedQueryCount}</div>
            <div className="stat-label">{t('queryHistory.failedQueries')}</div>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="filters">
        <div className="filter-group">
          <label>{t('queryHistory.filter')}:</label>
          <select
            value={filterType}
            onChange={(e) => {
              setFilterType(e.target.value as 'all' | 'successful' | 'failed')
              setCurrentPage(0)
            }}
          >
            <option value="all">{t('queryHistory.all')}</option>
            <option value="successful">{t('queryHistory.successfulOnly')}</option>
            <option value="failed">{t('queryHistory.failedOnly')}</option>
          </select>
        </div>
        
        <div className="search-group">
          <input
            type="text"
            placeholder={t('queryHistory.searchPlaceholder')}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            className="search-input"
          />
          <button className="btn-search" onClick={handleSearch}>
            {t('queryHistory.search')}
          </button>
        </div>
      </div>

      {/* History List */}
      <div className="history-list">
        {filteredHistory.length === 0 ? (
          <div className="no-data">
            {searchTerm ? t('queryHistory.noSearchResults') : t('queryHistory.noHistory')}
          </div>
        ) : (
          filteredHistory.map(item => (
            <HistoryCard
              key={item.id}
              item={item}
              onReExecute={handleReExecute}
            />
          ))
        )}
      </div>

      {/* Pagination */}
      <div className="pagination">
        <button
          className="btn-page"
          disabled={currentPage === 0}
          onClick={() => setCurrentPage(currentPage - 1)}
        >
          {t('queryHistory.previous')}
        </button>
        <span className="page-info">{t('queryHistory.page')} {currentPage + 1}</span>
        <button
          className="btn-page"
          disabled={filteredHistory.length < pageSize}
          onClick={() => setCurrentPage(currentPage + 1)}
        >
          {t('queryHistory.next')}
        </button>
      </div>

      <style>{`
        .container {
          padding: 20px;
          max-width: 1200px;
          margin: 0 auto;
        }
        
        .header {
          margin-bottom: 20px;
        }
        
        .header h1 {
          margin: 0;
        }
        
        .error-message {
          background-color: #f8d7da;
          border: 1px solid #f5c6cb;
          color: #721c24;
          padding: 10px;
          border-radius: 5px;
          margin-bottom: 20px;
        }
        
        .stats-container {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
          gap: 20px;
          margin-bottom: 30px;
        }
        
        .stat-card {
          background: white;
          border: 1px solid #e9ecef;
          border-radius: 8px;
          padding: 20px;
          text-align: center;
        }
        
        .stat-value {
          font-size: 2em;
          font-weight: bold;
          color: #007bff;
          margin-bottom: 5px;
        }
        
        .stat-label {
          color: #6c757d;
          font-size: 14px;
        }
        
        .filters {
          background: white;
          padding: 20px;
          border-radius: 8px;
          margin-bottom: 20px;
          display: flex;
          justify-content: space-between;
          align-items: center;
          flex-wrap: wrap;
          gap: 15px;
        }
        
        .filter-group {
          display: flex;
          align-items: center;
          gap: 10px;
        }
        
        .filter-group label {
          font-weight: bold;
        }
        
        .filter-group select {
          padding: 8px 12px;
          border: 1px solid #ccc;
          border-radius: 4px;
        }
        
        .search-group {
          display: flex;
          gap: 10px;
        }
        
        .search-input {
          padding: 8px 12px;
          border: 1px solid #ccc;
          border-radius: 4px;
          width: 300px;
        }
        
        .btn-search {
          background-color: #007bff;
          color: white;
          border: none;
          padding: 8px 16px;
          border-radius: 4px;
          cursor: pointer;
        }
        
        .btn-search:hover {
          background-color: #0056b3;
        }
        
        .history-list {
          display: grid;
          gap: 15px;
        }
        
        .no-data {
          text-align: center;
          color: #6c757d;
          padding: 40px;
          background: white;
          border-radius: 8px;
        }
        
        .loading {
          text-align: center;
          padding: 40px;
        }
        
        .pagination {
          display: flex;
          justify-content: center;
          align-items: center;
          gap: 20px;
          margin-top: 30px;
        }
        
        .btn-page {
          background-color: #007bff;
          color: white;
          border: none;
          padding: 10px 20px;
          border-radius: 5px;
          cursor: pointer;
        }
        
        .btn-page:disabled {
          background-color: #6c757d;
          cursor: not-allowed;
        }
        
        .page-info {
          font-weight: bold;
        }
        
        @media (max-width: 768px) {
          .filters {
            flex-direction: column;
            align-items: stretch;
          }
          
          .search-group {
            flex-direction: column;
          }
          
          .search-input {
            width: 100%;
          }
          
          .stats-container {
            grid-template-columns: repeat(2, 1fr);
          }
        }
      `}</style>
    </div>
  )
}

interface HistoryCardProps {
  item: QueryHistoryItem
  onReExecute: (item: QueryHistoryItem) => void
}

const HistoryCard: React.FC<HistoryCardProps> = ({ item, onReExecute }) => {
  const { t } = useTranslation()
  const formatExecutionTime = (timeMs: number) => {
    if (timeMs < 1000) {
      return `${timeMs}ms`
    }
    return `${(timeMs / 1000).toFixed(2)}s`
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('ja-JP')
  }

  return (
    <div className={`history-card ${item.isSuccessful ? 'successful' : 'failed'}`}>
      <div className="card-header">
        <div className="query-type-info">
          {item.savedQueryId ? (
            <span className="query-type-badge saved-query">
              🔖 {t('queryHistory.savedQueryType')}{item.savedQueryName ? `: ${item.savedQueryName}` : ''}
            </span>
          ) : (
            <span className="query-type-badge direct-query">
              📝 {t('queryHistory.directQueryType')}
            </span>
          )}
        </div>
        <div className="status-info">
          <span className={`status-badge ${item.isSuccessful ? 'success' : 'error'}`}>
            {item.isSuccessful ? t('queryHistory.success') : t('queryHistory.failed')}
          </span>
          <span className="execution-time">
            {formatExecutionTime(item.executionTimeMs)}
          </span>
          {item.resultCount !== undefined && (
            <span className="result-count">
              {item.resultCount} {t('queryHistory.rows')}
            </span>
          )}
          <div className="card-actions">
            <button className="btn-rerun" onClick={() => onReExecute(item)}>
              {t('queryHistory.rerun')}
            </button>
          </div>
        </div>
      </div>

      <div className="sql-content">
        <pre>{item.sqlContent.substring(0, 300)}{item.sqlContent.length > 300 ? '...' : ''}</pre>
      </div>

      {item.parameterValues && Object.keys(item.parameterValues).length > 0 && (
        <div className="parameters">
          <strong>{t('queryHistory.parameters')}:</strong>
          <div className="param-list">
            {Object.entries(item.parameterValues).map(([key, value]) => (
              <span key={key} className="param-item">
                {key}: {String(value)}
              </span>
            ))}
          </div>
        </div>
      )}

      {item.errorMessage && (
        <div className="error-details">
          <strong>{t('queryHistory.error')}:</strong> {item.errorMessage}
        </div>
      )}

      <div className="card-meta">
        <div className="meta-row">
          <span>{t('queryHistory.executedAt')}: <strong>{formatDate(item.executedAt)}</strong></span>
          <span>{t('queryHistory.connection')}: <strong>{item.connectionName}</strong> ({item.databaseType})</span>
        </div>
        {item.savedQueryName && (
          <div className="meta-row">
            <span>{t('queryHistory.savedQuery')}: <strong>{item.savedQueryName}</strong></span>
          </div>
        )}
      </div>

      <style>{`
        .history-card {
          background: white;
          border: 1px solid #e9ecef;
          border-radius: 8px;
          padding: 20px;
        }
        
        .history-card.successful {
          border-left: 4px solid #28a745;
        }
        
        .history-card.failed {
          border-left: 4px solid #dc3545;
        }
        
        .card-header {
          display: flex;
          flex-direction: column;
          gap: 10px;
          margin-bottom: 15px;
        }
        
        .query-type-info {
          display: flex;
          align-items: center;
        }
        
        .query-type-badge {
          padding: 6px 12px;
          border-radius: 16px;
          font-size: 13px;
          font-weight: 600;
          display: inline-block;
        }
        
        .query-type-badge.saved-query {
          background-color: #e7f3ff;
          color: #0066cc;
          border: 1px solid #b3d9ff;
        }
        
        .query-type-badge.direct-query {
          background-color: #f0f8f0;
          color: #2d5d2d;
          border: 1px solid #a5d6a5;
        }
        
        .status-info {
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 15px;
        }
        
        .status-badge {
          padding: 4px 8px;
          border-radius: 12px;
          font-size: 12px;
          font-weight: bold;
        }
        
        .status-badge.success {
          background-color: #d4edda;
          color: #155724;
        }
        
        .status-badge.error {
          background-color: #f8d7da;
          color: #721c24;
        }
        
        .execution-time {
          color: #007bff;
          font-weight: bold;
        }
        
        .result-count {
          color: #28a745;
          font-weight: bold;
        }
        
        .card-actions {
          display: flex;
          gap: 10px;
        }
        
        .btn-rerun {
          background-color: #17a2b8;
          color: white;
          border: none;
          padding: 6px 12px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 12px;
        }
        
        .btn-rerun:hover {
          background-color: #138496;
        }
        
        .sql-content {
          background-color: #f8f9fa;
          padding: 15px;
          border-radius: 5px;
          margin-bottom: 15px;
          overflow-x: auto;
        }
        
        .sql-content pre {
          margin: 0;
          font-family: 'Consolas', 'Monaco', monospace;
          font-size: 14px;
          white-space: pre-wrap;
        }
        
        .parameters {
          margin-bottom: 15px;
          padding: 10px;
          background-color: #e3f2fd;
          border-radius: 4px;
        }
        
        .param-list {
          margin-top: 5px;
          display: flex;
          flex-wrap: wrap;
          gap: 10px;
        }
        
        .param-item {
          background-color: #bbdefb;
          padding: 2px 6px;
          border-radius: 3px;
          font-size: 12px;
          font-family: monospace;
        }
        
        .error-details {
          margin-bottom: 15px;
          padding: 10px;
          background-color: #f8d7da;
          border-radius: 4px;
          color: #721c24;
        }
        
        .card-meta {
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
          .card-header {
            flex-direction: column;
            align-items: flex-start;
            gap: 15px;
          }
          
          .card-actions {
            width: 100%;
            justify-content: flex-end;
          }
          
          .meta-row {
            flex-direction: column;
            gap: 5px;
          }
          
          .param-list {
            flex-direction: column;
            gap: 5px;
          }
        }
      `}</style>
    </div>
  )
}

export default QueryHistory