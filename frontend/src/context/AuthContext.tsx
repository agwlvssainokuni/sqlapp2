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

import React, {createContext, type ReactNode, useContext, useEffect, useState} from 'react'
import {useNavigate} from 'react-router-dom'
import {apiRequest, getValidAccessToken, setNavigationCallback} from '../utils/api'
import {isTokenExpired} from '../utils/jwtUtils'
import type {ApiResponse, LoginResult, LoginUser} from '../types/api.ts'

interface AuthContextType {
  user: LoginUser | null
  isAuthenticated: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  isLoading: boolean
  checkAuthStatus: () => Promise<boolean>
  apiRequest: <T>(url: string, options?: RequestInit) => Promise<ApiResponse<T>>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

interface AuthProviderProps {
  children: ReactNode
}

export const AuthProvider: React.FC<AuthProviderProps> = ({children}) => {
  const [user, setUser] = useState<LoginUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const navigate = useNavigate()

  // Set navigation callback for API utilities
  useEffect(() => {
    setNavigationCallback((path: string, options?: { replace?: boolean }) => {
      navigate(path, options)
    })
  }, [navigate])

  const clearAuthData = () => {
    console.log('Clearing authentication data')
    setUser(null)
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }

  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('token')
      const storedUser = localStorage.getItem('user')

      if (token && storedUser) {
        try {
          const userData = JSON.parse(storedUser)
          
          // First, validate stored user data structure
          if (!userData?.username || !userData?.id) {
            console.warn('Invalid stored user data, clearing tokens')
            clearAuthData()
            setIsLoading(false)
            return
          }

          // Basic token validation - if token is obviously expired, don't make API call
          try {
            if (isTokenExpired(token)) {
              console.log('Stored token is expired, attempting silent refresh')
              // Try to get a valid token through refresh
              const validToken = await getValidAccessToken()
              
              if (validToken) {
                console.log('Token refreshed successfully during initialization')
                setUser(userData)
              } else {
                console.log('Token refresh failed during initialization')
                clearAuthData()
              }
              setIsLoading(false)
              return
            }
          } catch (jwtError) {
            console.warn('JWT validation failed during init:', jwtError)
            clearAuthData()
            setIsLoading(false)
            return
          }

          // Token appears valid, set user without additional API call
          // The API calls will handle token validation and refresh as needed
          console.log('Setting user from stored data with valid token')
          setUser(userData)
        } catch (error) {
          console.error('Error during auth initialization:', error)
          clearAuthData()
        }
      }
      setIsLoading(false)
    }

    initAuth()
  }, [])

  const login = async (username: string, password: string): Promise<void> => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({username, password}),
    })

    if (!response.ok) {
      throw new Error('Login failed')
    }

    const apiResponse = await response.json()
    const data = apiResponse.data as LoginResult
    setUser(data.user)
    localStorage.setItem('token', data.access_token)
    localStorage.setItem('refreshToken', data.refresh_token)
    localStorage.setItem('user', JSON.stringify(data.user))
  }

  const checkAuthStatus = async (): Promise<boolean> => {
    try {
      const response = await apiRequest('/api/auth/me')
      return response.ok
    } catch {
      return false
    }
  }

  const logout = () => {
    clearAuthData()
    // Clear any stored redirect path as well
    sessionStorage.removeItem('redirectAfterLogin')
  }

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    login,
    logout,
    isLoading,
    checkAuthStatus,
    apiRequest,
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
