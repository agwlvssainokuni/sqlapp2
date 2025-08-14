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
import {useAuth} from '../context/AuthContext'
import {Link, useNavigate} from 'react-router-dom'

const Header: React.FC = () => {
  const {t} = useTranslation()
  const {logout} = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="app-header">
      <div className="header-content">
        <div className="header-left">
          <h1 className="app-title">SqlApp2</h1>
        </div>
        <nav className="header-nav">
          <Link to="/dashboard" className="nav-link">
            {t('common.dashboard')}
          </Link>
          <button className="logout-btn" onClick={handleLogout}>
            {t('common.logout')}
          </button>
        </nav>
      </div>
    </header>
  )
}

export default Header
