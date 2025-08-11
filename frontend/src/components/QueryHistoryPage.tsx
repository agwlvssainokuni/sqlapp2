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
import type {QueryHistory, UserStatisticsResponse} from '../types/api'
import Layout from './Layout'

const QueryHistoryPage: React.FC = () => {
  const {t} = useTranslation()
  const {apiRequest} = useAuth()
  const [history, setHistory] = useState<QueryHistory[]>([])
  const [statistics, setStatistics] = useState<UserStatisticsResponse | null>(null)
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

      const historyRes = historyResp.data as { content: QueryHistory[] }
      const statsRes = statsResp.data as UserStatisticsResponse

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
      const historyRes = historyResp.data as { content: QueryHistory[] }
      setHistory(historyRes.content || [])
    } catch (err) {
      console.error('Failed to search query history:', err)
      setError(t('queryHistory.searchFailed'))
    } finally {
      setLoading(false)
    }
  }

  const handleReExecute = (item: QueryHistory) => {
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
    <Layout title={t('queryHistory.title')}>
      <div className="query-history">

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

      </div>
    </Layout>
  )
}

interface HistoryCardProps {
  item: QueryHistory
  onReExecute: (item: QueryHistory) => void
}

const HistoryCard: React.FC<HistoryCardProps> = ({item, onReExecute}) => {
  const {t} = useTranslation()
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
    <div className="query-history">
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

      </div>
    </div>
  )
}

export default QueryHistoryPage
