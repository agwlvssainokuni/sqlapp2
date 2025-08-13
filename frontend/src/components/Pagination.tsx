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

import React from 'react'
import { useTranslation } from 'react-i18next'
import type { PagedResult } from '../types/api'

interface PaginationProps {
  paging: PagedResult<unknown[]>
  onPageChange: (page: number) => void
  onPageSizeChange: (pageSize: number) => void
}

const Pagination: React.FC<PaginationProps> = ({
  paging,
  onPageChange,
  onPageSizeChange
}) => {
  const { t } = useTranslation()

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < paging.totalPages) {
      onPageChange(newPage)
    }
  }

  const renderPageNumbers = () => {
    const pages = []
    const currentPage = paging.page
    const totalPages = paging.totalPages
    const maxVisible = 5

    let startPage = Math.max(0, currentPage - Math.floor(maxVisible / 2))
    const endPage = Math.min(totalPages - 1, startPage + maxVisible - 1)

    // Adjust start page if we're near the end
    if (endPage - startPage < maxVisible - 1) {
      startPage = Math.max(0, endPage - maxVisible + 1)
    }

    // First page
    if (startPage > 0) {
      pages.push(
        <button
          key="first"
          onClick={() => handlePageChange(0)}
          className="page-btn"
        >
          1
        </button>
      )
      if (startPage > 1) {
        pages.push(<span key="ellipsis1" className="ellipsis">...</span>)
      }
    }

    // Page numbers
    for (let i = startPage; i <= endPage; i++) {
      pages.push(
        <button
          key={i}
          onClick={() => handlePageChange(i)}
          className={`page-btn ${i === currentPage ? 'active' : ''}`}
        >
          {i + 1}
        </button>
      )
    }

    // Last page
    if (endPage < totalPages - 1) {
      if (endPage < totalPages - 2) {
        pages.push(<span key="ellipsis2" className="ellipsis">...</span>)
      }
      pages.push(
        <button
          key="last"
          onClick={() => handlePageChange(totalPages - 1)}
          className="page-btn"
        >
          {totalPages}
        </button>
      )
    }

    return pages
  }

  return (
    <div className="pagination-container">
      <div className="pagination-info">
        <span>
          {t('pagination.showing')} {paging.page * paging.pageSize + 1} - {' '}
          {Math.min((paging.page + 1) * paging.pageSize, paging.totalElements)} {' '}
          {t('pagination.of')} {paging.totalElements} {t('pagination.results')}
        </span>
      </div>

      <div className="pagination-controls">
        <div className="page-size-selector">
          <label htmlFor="page-size">{t('pagination.rowsPerPage')}:</label>
          <select
            id="page-size"
            value={paging.pageSize}
            onChange={(e) => onPageSizeChange(Number(e.target.value))}
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

        <div className="page-navigation">
          <button
            onClick={() => handlePageChange(0)}
            disabled={!paging.hasPrevious}
            className="nav-btn"
            title={t('pagination.firstPage')}
          >
            ⟪
          </button>
          <button
            onClick={() => handlePageChange(paging.page - 1)}
            disabled={!paging.hasPrevious}
            className="nav-btn"
            title={t('pagination.previousPage')}
          >
            ⟨
          </button>

          <div className="page-numbers">
            {renderPageNumbers()}
          </div>

          <button
            onClick={() => handlePageChange(paging.page + 1)}
            disabled={!paging.hasNext}
            className="nav-btn"
            title={t('pagination.nextPage')}
          >
            ⟩
          </button>
          <button
            onClick={() => handlePageChange(paging.totalPages - 1)}
            disabled={!paging.hasNext}
            className="nav-btn"
            title={t('pagination.lastPage')}
          >
            ⟫
          </button>
        </div>
      </div>
    </div>
  )
}

export default Pagination