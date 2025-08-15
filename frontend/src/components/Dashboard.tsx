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
import {useTranslation} from 'react-i18next'
import {Link} from 'react-router-dom'
import {useAuth} from '../context/AuthContext'
import Layout from './Layout'

const Dashboard: React.FC = () => {
  const {t} = useTranslation()
  const {user} = useAuth()

  return (
    <Layout title={t('dashboard.title')}>
      <div className="dashboard-welcome">
        <div>{t('dashboard.welcome')}, <strong>{user?.username}</strong></div>
      </div>

      <section className="dashboard-content">
        <h2>{t('dashboard.quickActions')}</h2>
        <div className="feature-cards">
          <div className="feature-card">
            <h3>{t('navigation.sqlExecution')}</h3>
            <p>Execute SQL queries with parameterized support against your configured databases.</p>
            <Link to="/sql" className="feature-link">{t('dashboard.executeSQL')}</Link>
          </div>
          <div className="feature-card">
            <h3>{t('navigation.connections')}</h3>
            <p>Manage your database connections for MySQL, PostgreSQL, and MariaDB.</p>
            <Link to="/connections" className="feature-link">{t('dashboard.manageConnections')}</Link>
          </div>
          <div className="feature-card">
            <h3>{t('navigation.schemaViewer')}</h3>
            <p>Browse database schemas, tables, columns, and their relationships.</p>
            <Link to="/schema" className="feature-link">{t('dashboard.browseSchema')}</Link>
          </div>
          <div className="feature-card">
            <h3>{t('navigation.savedQueries')}</h3>
            <p>Save, organize, and share your SQL queries with parameter templates.</p>
            <Link to="/queries" className="feature-link">{t('dashboard.viewQueries')}</Link>
          </div>
          <div className="feature-card">
            <h3>{t('navigation.queryHistory')}</h3>
            <p>Track and analyze your SQL execution history with performance metrics.</p>
            <Link to="/history" className="feature-link">{t('dashboard.viewHistory')}</Link>
          </div>
          <div className="feature-card">
            <h3>{t('navigation.queryBuilder')}</h3>
            <p>Build SQL queries visually with drag-and-drop interface and schema assistance.</p>
            <Link to="/builder" className="feature-link">{t('dashboard.buildQuery')}</Link>
          </div>
        </div>
      </section>
    </Layout>
  )
}

export default Dashboard
