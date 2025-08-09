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
import { useAuth } from '../context/AuthContext'

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth()

  const handleLogout = () => {
    logout()
  }

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1>SqlApp2 Dashboard</h1>
        <div className="user-info">
          <span>Welcome, {user?.username}</span>
          <button onClick={handleLogout} className="logout-btn">
            Logout
          </button>
        </div>
      </header>
      <main className="dashboard-content">
        <h2>SqlApp2 Features</h2>
        <div className="feature-cards">
          <div className="feature-card">
            <h3>SQL Query Execution</h3>
            <p>Execute SQL queries with parameterized support against your configured databases.</p>
            <a href="/sql" className="feature-link">Go to SQL Execution</a>
          </div>
          <div className="feature-card">
            <h3>Database Connections</h3>
            <p>Manage your database connections for MySQL, PostgreSQL, and MariaDB.</p>
            <a href="/connections" className="feature-link">Manage Connections</a>
          </div>
          <div className="feature-card">
            <h3>Schema Viewer</h3>
            <p>Browse database schemas, tables, columns, and their relationships.</p>
            <a href="/schema" className="feature-link">View Schema</a>
          </div>
          <div className="feature-card">
            <h3>Saved Queries</h3>
            <p>Save, organize, and share your SQL queries with parameter templates.</p>
            <a href="/queries" className="feature-link">Manage Queries</a>
          </div>
          <div className="feature-card">
            <h3>Query History</h3>
            <p>Track and analyze your SQL execution history with performance metrics.</p>
            <a href="/history" className="feature-link">View History</a>
          </div>
          <div className="feature-card">
            <h3>SQL Query Builder</h3>
            <p>Build SQL queries visually with drag-and-drop interface and schema assistance.</p>
            <a href="/builder" className="feature-link">Use Query Builder</a>
          </div>
        </div>
      </main>
    </div>
  )
}

export default Dashboard