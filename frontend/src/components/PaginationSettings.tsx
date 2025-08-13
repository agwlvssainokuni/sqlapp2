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

import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { PagingRequest } from '../types/api'

interface PaginationSettingsProps {
  paging: PagingRequest
  onPagingChange: (paging: PagingRequest) => void
  sqlContainsOrderBy: boolean
}

const PaginationSettings: React.FC<PaginationSettingsProps> = ({
  paging,
  onPagingChange,
  sqlContainsOrderBy
}) => {
  const { t } = useTranslation()
  const [showAdvanced, setShowAdvanced] = useState(false)

  const handleEnabledChange = (enabled: boolean) => {
    onPagingChange({
      ...paging,
      enabled,
      page: 0 // Reset to first page when enabling/disabling
    })
  }

  const handlePageSizeChange = (pageSize: number) => {
    onPagingChange({
      ...paging,
      pageSize,
      page: 0 // Reset to first page when changing page size
    })
  }

  const handleIgnoreOrderByWarningChange = (ignoreOrderByWarning: boolean) => {
    onPagingChange({
      ...paging,
      ignoreOrderByWarning
    })
  }

  return (
    <div className="pagination-settings">
      <div className="pagination-toggle">
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={paging.enabled}
            onChange={(e) => handleEnabledChange(e.target.checked)}
          />
          <span className="checkmark"></span>
          {t('pagination.enablePagination')}
        </label>
      </div>

      {paging.enabled && (
        <div className="pagination-options">
          <div className="page-size-setting">
            <label htmlFor="pagination-page-size">
              {t('pagination.pageSize')}:
            </label>
            <select
              id="pagination-page-size"
              value={paging.pageSize}
              onChange={(e) => handlePageSizeChange(Number(e.target.value))}
              className="page-size-select"
            >
              <option value={10}>10</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
              <option value={250}>250</option>
              <option value={500}>500</option>
              <option value={1000}>1000</option>
            </select>
          </div>

          {!sqlContainsOrderBy && (
            <div className="order-by-warning">
              <div className="warning-message">
                <span className="warning-icon">⚠️</span>
                <span>{t('pagination.orderByWarning')}</span>
              </div>
              
              <button
                type="button"
                onClick={() => setShowAdvanced(!showAdvanced)}
                className="show-advanced-btn"
              >
                {showAdvanced 
                  ? t('pagination.hideAdvanced')
                  : t('pagination.showAdvanced')
                }
              </button>

              {showAdvanced && (
                <div className="advanced-options">
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={paging.ignoreOrderByWarning}
                      onChange={(e) => handleIgnoreOrderByWarningChange(e.target.checked)}
                    />
                    <span className="checkmark"></span>
                    {t('pagination.ignoreOrderByWarning')}
                  </label>
                  <div className="help-text">
                    {t('pagination.ignoreOrderByHelp')}
                  </div>
                </div>
              )}
            </div>
          )}

          <div className="pagination-info">
            <div className="info-item">
              <span className="info-label">{t('pagination.currentPage')}:</span>
              <span className="info-value">{paging.page + 1}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default PaginationSettings