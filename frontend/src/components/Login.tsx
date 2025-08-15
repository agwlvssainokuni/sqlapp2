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

import React, {useState} from 'react'
import {Link, useNavigate} from 'react-router-dom'
import {useTranslation} from 'react-i18next'
import {useAuth} from '../context/AuthContext'
import LanguageSwitcher from './LanguageSwitcher'

const Login: React.FC = () => {
  const {t} = useTranslation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const {login} = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    setError('')

    try {
      await login(username, password)
      
      // Enhanced redirect handling with full location state support
      const redirectData = sessionStorage.getItem('redirectAfterLogin')
      if (redirectData) {
        sessionStorage.removeItem('redirectAfterLogin')
        
        try {
          // Try to parse as full location state object
          const locationState = JSON.parse(redirectData)
          if (locationState.pathname) {
            console.log('Restoring full location state:', locationState)
            const fullPath = locationState.pathname + (locationState.search || '') + (locationState.hash || '')
            navigate(fullPath, { replace: true })
          } else {
            // Fallback for old simple string format
            navigate(redirectData, { replace: true })
          }
        } catch {
          // Fallback if JSON parsing fails - treat as simple path
          console.log('Using simple redirect path:', redirectData)
          navigate(redirectData, { replace: true })
        }
      } else {
        navigate('/dashboard', { replace: true })
      }
    } catch {
      setError(t('auth.loginFailed'))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="login-container">
      <div className="login-form">
        <div className="login-header">
          <h2>SqlApp2 {t('auth.login')}</h2>
          <LanguageSwitcher/>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">{t('auth.username')}:</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">{t('auth.password')}:</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && <div className="error-message">{error}</div>}
          <button type="submit" disabled={isLoading}>
            {isLoading ? `${t('auth.loginButton')}...` : t('auth.loginButton')}
          </button>
        </form>
        <p>
          {t('auth.dontHaveAccount')} <Link to="/register">{t('auth.register')}</Link>
        </p>
      </div>
    </div>
  )
}

export default Login
