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

import {BrowserRouter as Router, Routes, Route, Navigate} from 'react-router-dom'
import Login from './components/Login'
import Dashboard from './components/Dashboard'
import Register from './components/Register'
import SqlExecutionPage from './components/SqlExecutionPage'
import ConnectionManagementPage from './components/ConnectionManagementPage'
import SchemaViewerPage from './components/SchemaViewerPage'
import SavedQueriesPage from './components/SavedQueriesPage'
import QueryHistoryPage from './components/QueryHistoryPage'
import QueryBuilderPage from './components/QueryBuilderPage'
import {AuthProvider} from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import './App.css'

// Import test utilities for console testing
import './test/jwtUtilities.test'

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login/>}/>
          <Route path="/register" element={<Register/>}/>
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard/>
              </ProtectedRoute>
            }
          />
          <Route
            path="/sql"
            element={
              <ProtectedRoute>
                <SqlExecutionPage/>
              </ProtectedRoute>
            }
          />
          <Route
            path="/connections"
            element={
              <ProtectedRoute>
                <ConnectionManagementPage/>
              </ProtectedRoute>
            }
          />
          <Route
            path="/schema"
            element={
              <ProtectedRoute>
                <SchemaViewerPage/>
              </ProtectedRoute>
            }
          />
          <Route
            path="/queries"
            element={
              <ProtectedRoute>
                <SavedQueriesPage/>
              </ProtectedRoute>
            }
          />
          <Route
            path="/history"
            element={
              <ProtectedRoute>
                <QueryHistoryPage/>
              </ProtectedRoute>
            }
          />
          <Route
            path="/builder"
            element={
              <ProtectedRoute>
                <QueryBuilderPage/>
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" replace/>}/>
        </Routes>
      </Router>
    </AuthProvider>
  )
}

export default App
