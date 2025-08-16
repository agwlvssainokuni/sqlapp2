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

import React, { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { getValidAccessToken } from '../utils/api'

interface User {
  id: number
  username: string
  email: string
  role: string
  status: string
  createdAt: string
  updatedAt: string
}

interface ApiResponse<T> {
  ok: boolean
  data?: T
  error?: string[]
}

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

const AdminPage: React.FC = () => {
  const { t } = useTranslation()
  const [pendingUsers, setPendingUsers] = useState<User[]>([])
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [pageSize] = useState(20)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>('')
  const [processingUser, setProcessingUser] = useState<number | null>(null)
  const [rejectionReason, setRejectionReason] = useState<{ [key: number]: string }>({})

  const loadPendingUsers = useCallback(async () => {
    setLoading(true)
    setError('')

    try {
      const token = await getValidAccessToken()
      const params = new URLSearchParams({
        page: currentPage.toString(),
        size: pageSize.toString()
      })

      const response = await fetch(`/api/admin/users/pending?${params}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })

      if (!response.ok) {
        if (response.status === 403) {
          throw new Error(t('admin.accessDenied'))
        }
        throw new Error(`HTTP ${response.status}`)
      }

      const result: ApiResponse<PageResponse<User>> = await response.json()

      if (result.ok && result.data) {
        setPendingUsers(result.data.content)
        setTotalPages(result.data.totalPages)
        setTotalElements(result.data.totalElements)
      } else {
        setError(result.error?.join(', ') || t('admin.loadError'))
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : t('admin.loadError'))
    } finally {
      setLoading(false)
    }
  }, [currentPage, pageSize, t])

  const handleApprove = async (userId: number) => {
    setProcessingUser(userId)
    try {
      const token = await getValidAccessToken()
      const response = await fetch(`/api/admin/users/${userId}/approve`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const result: ApiResponse<User> = await response.json()

      if (result.ok) {
        // Remove user from pending list
        setPendingUsers(prev => prev.filter(user => user.id !== userId))
        setTotalElements(prev => prev - 1)
      } else {
        setError(result.error?.join(', ') || t('admin.approveError'))
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : t('admin.approveError'))
    } finally {
      setProcessingUser(null)
    }
  }

  const handleReject = async (userId: number) => {
    const reason = rejectionReason[userId] || ''
    if (!reason.trim()) {
      setError(t('admin.rejectionReasonRequired'))
      return
    }

    setProcessingUser(userId)
    try {
      const token = await getValidAccessToken()
      const response = await fetch(`/api/admin/users/${userId}/reject`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ reason: reason.trim() })
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const result: ApiResponse<User> = await response.json()

      if (result.ok) {
        // Remove user from pending list
        setPendingUsers(prev => prev.filter(user => user.id !== userId))
        setTotalElements(prev => prev - 1)
        setRejectionReason(prev => {
          const updated = { ...prev }
          delete updated[userId]
          return updated
        })
      } else {
        setError(result.error?.join(', ') || t('admin.rejectError'))
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : t('admin.rejectError'))
    } finally {
      setProcessingUser(null)
    }
  }

  const handleRejectionReasonChange = (userId: number, reason: string) => {
    setRejectionReason(prev => ({ ...prev, [userId]: reason }))
  }

  useEffect(() => {
    loadPendingUsers()
  }, [loadPendingUsers])

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString()
  }

  return (
    <div className="admin-page">
      <div className="container">
        <h1>{t('admin.title')}</h1>
        
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        <div className="pending-users-section">
          <h2>{t('admin.pendingUsers')} ({totalElements})</h2>
          
          {loading ? (
            <div className="loading">{t('common.loading')}</div>
          ) : pendingUsers.length === 0 ? (
            <div className="no-data">{t('admin.noPendingUsers')}</div>
          ) : (
            <div className="users-table-container">
              <table className="users-table">
                <thead>
                  <tr>
                    <th>{t('admin.username')}</th>
                    <th>{t('admin.email')}</th>
                    <th>{t('admin.registeredAt')}</th>
                    <th>{t('admin.actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingUsers.map((user) => (
                    <tr key={user.id}>
                      <td>{user.username}</td>
                      <td>{user.email}</td>
                      <td>{formatDate(user.createdAt)}</td>
                      <td className="actions-cell">
                        <div className="action-buttons">
                          <button
                            className="btn-approve"
                            onClick={() => handleApprove(user.id)}
                            disabled={processingUser === user.id}
                          >
                            {processingUser === user.id ? t('common.processing') : t('admin.approve')}
                          </button>
                          
                          <div className="reject-section">
                            <input
                              type="text"
                              placeholder={t('admin.rejectionReason')}
                              value={rejectionReason[user.id] || ''}
                              onChange={(e) => handleRejectionReasonChange(user.id, e.target.value)}
                              className="rejection-reason-input"
                              disabled={processingUser === user.id}
                            />
                            <button
                              className="btn-reject"
                              onClick={() => handleReject(user.id)}
                              disabled={processingUser === user.id}
                            >
                              {processingUser === user.id ? t('common.processing') : t('admin.reject')}
                            </button>
                          </div>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {totalPages > 1 && (
            <div className="pagination">
              <button
                onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                disabled={currentPage === 0 || loading}
                className="btn-pagination"
              >
                {t('common.previous')}
              </button>
              
              <span className="page-info">
                {t('common.pageInfo', { current: currentPage + 1, total: totalPages })}
              </span>
              
              <button
                onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                disabled={currentPage >= totalPages - 1 || loading}
                className="btn-pagination"
              >
                {t('common.next')}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default AdminPage