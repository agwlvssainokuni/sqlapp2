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
import { useAuth } from '../context/AuthContext'
import Layout from './Layout'

interface User {
  id: number
  username: string
  email: string
  role: string
  status: string
  createdAt: string
  updatedAt: string
}

interface EmailTemplate {
  id: number
  templateKey: string
  language: string
  subject: string
  body: string
  bcc?: string
}


interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

type TabType = 'users' | 'email-templates'

const AdminPage: React.FC = () => {
  const { t } = useTranslation()
  const { apiRequest } = useAuth()
  
  // Tab管理
  const [activeTab, setActiveTab] = useState<TabType>('users')
  
  // ユーザー管理
  const [pendingUsers, setPendingUsers] = useState<User[]>([])
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [pageSize] = useState(20)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>('')
  const [processingUser, setProcessingUser] = useState<number | null>(null)
  const [rejectionReason, setRejectionReason] = useState<{ [key: number]: string }>({})
  
  // メールテンプレート管理
  const [emailTemplates, setEmailTemplates] = useState<EmailTemplate[]>([])
  const [templateLoading, setTemplateLoading] = useState(false)
  const [templateError, setTemplateError] = useState<string>('')
  const [editingTemplate, setEditingTemplate] = useState<EmailTemplate | null>(null)
  const [showTemplateForm, setShowTemplateForm] = useState(false)
  const [templateForm, setTemplateForm] = useState({
    templateKey: '',
    language: '',
    subject: '',
    body: '',
    bcc: ''
  })

  const loadPendingUsers = useCallback(async () => {
    setLoading(true)
    setError('')

    try {
      const params = new URLSearchParams({
        page: currentPage.toString(),
        size: pageSize.toString()
      })

      const result = await apiRequest<PageResponse<User>>(`/api/admin/users/pending?${params}`)

      if (result.ok && result.data) {
        setPendingUsers(result.data.content)
        setTotalPages(result.data.totalPages)
        setTotalElements(result.data.totalElements)
      } else {
        setError(result.error?.join(', ') || t('admin.loadError'))
      }
    } catch (err) {
      if (err instanceof Error && err.message.includes('403')) {
        setError(t('admin.accessDenied'))
      } else {
        setError(err instanceof Error ? err.message : t('admin.loadError'))
      }
    } finally {
      setLoading(false)
    }
  }, [currentPage, pageSize, apiRequest, t])

  const handleApprove = async (userId: number) => {
    setProcessingUser(userId)
    try {
      const result = await apiRequest<User>(`/api/admin/users/${userId}/approve`, {
        method: 'POST'
      })

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
      const result = await apiRequest<User>(`/api/admin/users/${userId}/reject`, {
        method: 'POST',
        body: JSON.stringify({ reason: reason.trim() })
      })

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

  // メールテンプレート管理機能
  const loadEmailTemplates = useCallback(async () => {
    setTemplateLoading(true)
    setTemplateError('')

    try {
      const result = await apiRequest<EmailTemplate[]>('/api/admin/email-templates')

      if (result.ok && result.data) {
        setEmailTemplates(result.data)
      } else {
        setTemplateError(result.error?.join(', ') || t('admin.templateLoadError'))
      }
    } catch (err) {
      setTemplateError(err instanceof Error ? err.message : t('admin.templateLoadError'))
    } finally {
      setTemplateLoading(false)
    }
  }, [apiRequest, t])

  const handleCreateTemplate = async () => {
    try {
      const result = await apiRequest<EmailTemplate>('/api/admin/email-templates', {
        method: 'POST',
        body: JSON.stringify(templateForm)
      })

      if (result.ok && result.data) {
        setEmailTemplates(prev => [...prev, result.data!])
        setShowTemplateForm(false)
        setTemplateForm({ templateKey: '', language: '', subject: '', body: '', bcc: '' })
      } else {
        setTemplateError(result.error?.join(', ') || t('admin.templateCreateError'))
      }
    } catch (err) {
      setTemplateError(err instanceof Error ? err.message : t('admin.templateCreateError'))
    }
  }

  const handleUpdateTemplate = async () => {
    if (!editingTemplate) return

    try {
      const result = await apiRequest<EmailTemplate>(`/api/admin/email-templates/${editingTemplate.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          subject: templateForm.subject,
          body: templateForm.body,
          bcc: templateForm.bcc
        })
      })

      if (result.ok && result.data) {
        setEmailTemplates(prev => prev.map(t => t.id === editingTemplate.id ? result.data! : t))
        setEditingTemplate(null)
        setTemplateForm({ templateKey: '', language: '', subject: '', body: '', bcc: '' })
      } else {
        setTemplateError(result.error?.join(', ') || t('admin.templateUpdateError'))
      }
    } catch (err) {
      setTemplateError(err instanceof Error ? err.message : t('admin.templateUpdateError'))
    }
  }

  const handleDeleteTemplate = async (templateId: number) => {
    if (!confirm(t('admin.templateDeleteConfirm'))) return

    try {
      const result = await apiRequest<void>(`/api/admin/email-templates/${templateId}`, {
        method: 'DELETE'
      })

      if (result.ok) {
        setEmailTemplates(prev => prev.filter(t => t.id !== templateId))
      } else {
        setTemplateError(result.error?.join(', ') || t('admin.templateDeleteError'))
      }
    } catch (err) {
      setTemplateError(err instanceof Error ? err.message : t('admin.templateDeleteError'))
    }
  }

  const handleEditTemplate = (template: EmailTemplate) => {
    setEditingTemplate(template)
    setTemplateForm({
      templateKey: template.templateKey,
      language: template.language,
      subject: template.subject,
      body: template.body,
      bcc: template.bcc || ''
    })
    setShowTemplateForm(true)
  }

  const handleCancelTemplateForm = () => {
    setEditingTemplate(null)
    setShowTemplateForm(false)
    setTemplateForm({ templateKey: '', language: '', subject: '', body: '', bcc: '' })
  }

  useEffect(() => {
    if (activeTab === 'users') {
      loadPendingUsers()
    } else if (activeTab === 'email-templates') {
      loadEmailTemplates()
    }
  }, [activeTab, loadPendingUsers, loadEmailTemplates])

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString()
  }

  return (
    <Layout title={t('admin.title')}>
      <div className="admin-page">
        <div className="container">
          {/* タブナビゲーション */}
          <div className="admin-tabs">
            <button
              className={`tab-button ${activeTab === 'users' ? 'active' : ''}`}
              onClick={() => setActiveTab('users')}
            >
              {t('admin.userManagement')}
            </button>
            <button
              className={`tab-button ${activeTab === 'email-templates' ? 'active' : ''}`}
              onClick={() => setActiveTab('email-templates')}
            >
              {t('admin.emailTemplateManagement')}
            </button>
          </div>

        {/* ユーザー管理タブ */}
        {activeTab === 'users' && (
          <>
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
          </>
        )}

        {/* メールテンプレート管理タブ */}
        {activeTab === 'email-templates' && (
          <>
            {templateError && (
              <div className="error-message">
                {templateError}
              </div>
            )}

            <div className="email-templates-section">
              <div className="section-header">
                <h2>{t('admin.emailTemplates')}</h2>
                <button
                  className="btn-primary"
                  onClick={() => setShowTemplateForm(true)}
                  disabled={showTemplateForm}
                >
                  {t('admin.createTemplate')}
                </button>
              </div>

              {/* テンプレート作成・編集フォーム */}
              {showTemplateForm && (
                <div className="template-form">
                  <h3>{editingTemplate ? t('admin.editTemplate') : t('admin.createTemplate')}</h3>
                  
                  <div className="form-row">
                    <div className="form-group">
                      <label>{t('admin.templateKey')}</label>
                      <input
                        type="text"
                        value={templateForm.templateKey}
                        onChange={(e) => setTemplateForm(prev => ({ ...prev, templateKey: e.target.value }))}
                        disabled={!!editingTemplate}
                        placeholder="user-registration, user-approved, user-rejected"
                      />
                    </div>
                    <div className="form-group">
                      <label>{t('admin.language')}</label>
                      <input
                        type="text"
                        value={templateForm.language}
                        onChange={(e) => setTemplateForm(prev => ({ ...prev, language: e.target.value }))}
                        disabled={!!editingTemplate}
                        placeholder="en, ja"
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label>{t('admin.subject')}</label>
                    <input
                      type="text"
                      value={templateForm.subject}
                      onChange={(e) => setTemplateForm(prev => ({ ...prev, subject: e.target.value }))}
                      placeholder={t('admin.subjectPlaceholder')}
                    />
                  </div>

                  <div className="form-group">
                    <label>{t('admin.body')}</label>
                    <textarea
                      value={templateForm.body}
                      onChange={(e) => setTemplateForm(prev => ({ ...prev, body: e.target.value }))}
                      rows={10}
                      placeholder={t('admin.bodyPlaceholder')}
                    />
                  </div>

                  <div className="form-group">
                    <label>{t('admin.bcc')} ({t('admin.optional')})</label>
                    <input
                      type="email"
                      value={templateForm.bcc}
                      onChange={(e) => setTemplateForm(prev => ({ ...prev, bcc: e.target.value }))}
                      placeholder="admin@example.com"
                    />
                  </div>

                  <div className="form-actions">
                    <button
                      className="btn-primary"
                      onClick={editingTemplate ? handleUpdateTemplate : handleCreateTemplate}
                    >
                      {editingTemplate ? t('admin.updateTemplate') : t('admin.createTemplate')}
                    </button>
                    <button
                      className="btn-secondary"
                      onClick={handleCancelTemplateForm}
                    >
                      {t('common.cancel')}
                    </button>
                  </div>
                </div>
              )}

              {/* テンプレート一覧 */}
              {templateLoading ? (
                <div className="loading">{t('common.loading')}</div>
              ) : emailTemplates.length === 0 ? (
                <div className="no-data">{t('admin.noTemplates')}</div>
              ) : (
                <div className="templates-table-container">
                  <table className="templates-table">
                    <thead>
                      <tr>
                        <th>{t('admin.templateKey')}</th>
                        <th>{t('admin.language')}</th>
                        <th>{t('admin.subject')}</th>
                        <th>{t('admin.bcc')}</th>
                        <th>{t('admin.actions')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {emailTemplates.map((template) => (
                        <tr key={template.id}>
                          <td>{template.templateKey}</td>
                          <td>{template.language}</td>
                          <td className="subject-cell" title={template.subject}>{template.subject}</td>
                          <td>{template.bcc || '-'}</td>
                          <td className="actions-cell">
                            <button
                              className="btn-edit"
                              onClick={() => handleEditTemplate(template)}
                              disabled={showTemplateForm}
                            >
                              {t('admin.edit')}
                            </button>
                            <button
                              className="btn-delete"
                              onClick={() => handleDeleteTemplate(template.id)}
                            >
                              {t('admin.delete')}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </>
        )}
        </div>
      </div>
    </Layout>
  )
}

export default AdminPage