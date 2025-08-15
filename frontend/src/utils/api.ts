/*
 * Copyright 2025 SqlApp2
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

import type {ApiResponse, RefreshTokenResult} from '../types/api.ts'
import {isTokenExpired, isTokenExpiringSoon} from './jwtUtils.ts'

// Navigation callback for handling redirects
let navigationCallback: ((path: string, options?: { replace?: boolean }) => void) | null = null

/**
 * Set navigation callback for React Router integration
 * This should be called from a React component that has access to navigate
 */
export const setNavigationCallback = (callback: (path: string, options?: { replace?: boolean }) => void) => {
  navigationCallback = callback
}

/**
 * Save current location state for restoration after login
 */
const saveCurrentLocationState = () => {
  try {
    // Try to get React Router location from current URL
    const currentPath = window.location.pathname + window.location.search + window.location.hash
    
    if (currentPath !== '/login') {
      const locationState = {
        pathname: window.location.pathname,
        search: window.location.search,
        hash: window.location.hash,
        timestamp: Date.now()
      }
      sessionStorage.setItem('redirectAfterLogin', JSON.stringify(locationState))
      console.log('Saved location state for redirect:', locationState)
    }
  } catch (error) {
    console.warn('Failed to save location state:', error)
    // Fallback to simple path storage
    const currentPath = window.location.pathname + window.location.search
    if (currentPath !== '/login') {
      sessionStorage.setItem('redirectAfterLogin', currentPath)
    }
  }
}

// Global promise to prevent multiple simultaneous refresh attempts
let refreshPromise: Promise<string | null> | null = null

/**
 * Refresh the access token using the refresh token
 * @returns New access token or null if refresh failed
 */
const refreshAccessToken = async (): Promise<string | null> => {
  const refreshToken = localStorage.getItem('refreshToken')

  if (!refreshToken) {
    console.warn('No refresh token available')
    return null
  }

  try {
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        refresh_token: refreshToken
      }),
    })

    if (!response.ok) {
      console.error('Token refresh failed:', response.status, response.statusText)
      // Clear tokens if refresh failed
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      return null
    }

    const apiResponse = await response.json() as ApiResponse<RefreshTokenResult>

    if (!apiResponse.ok || !apiResponse.data) {
      console.error('Token refresh API returned error:', apiResponse.error)
      // Clear tokens if refresh failed
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      return null
    }

    const {access_token, refresh_token} = apiResponse.data

    // Update stored tokens
    localStorage.setItem('token', access_token)
    localStorage.setItem('refreshToken', refresh_token)

    console.log('Access token refreshed successfully')
    return access_token
  } catch (error) {
    console.error('Error during token refresh:', error)
    // Clear tokens on any error
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    return null
  }
}

/**
 * Get a valid access token, refreshing if necessary
 * @returns Valid access token or null if unable to obtain one
 */
export const getValidAccessToken = async (): Promise<string | null> => {
  const accessToken = localStorage.getItem('token')

  if (!accessToken) {
    return null
  }

  // Check if token is expired or expiring soon (within 30 seconds)
  const needsRefresh = isTokenExpired(accessToken) || isTokenExpiringSoon(accessToken, 30)

  if (!needsRefresh) {
    return accessToken
  }

  // If already refreshing, wait for the existing promise
  if (refreshPromise) {
    console.log('Token refresh already in progress, waiting...')
    return await refreshPromise
  }

  // Start new refresh process
  console.log('Access token expired or expiring soon, refreshing...')
  refreshPromise = refreshAccessToken()

  try {
    const newToken = await refreshPromise
    return newToken
  } finally {
    refreshPromise = null
  }
}

export const apiRequest = async <T>(url: string, options: RequestInit = {}): Promise<ApiResponse<T>> => {
  // Skip token refresh for auth endpoints to avoid infinite loops
  const isAuthEndpoint = url.includes('/api/auth/')

  let token: string | null = null
  let tokenWasRefreshed = false

  if (!isAuthEndpoint) {
    token = await getValidAccessToken()
    
    // Check if token was refreshed during getValidAccessToken call
    const originalToken = localStorage.getItem('token')
    tokenWasRefreshed = originalToken !== token

    if (!token) {
      // No valid token available, handle authentication failure
      handleAuthenticationFailure('No valid access token available')
      throw new Error('No valid access token available')
    }
  } else {
    // For auth endpoints, use token as-is if available
    token = localStorage.getItem('token')
  }

  const finalOptions: RequestInit = {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token && {Authorization: `Bearer ${token}`}),
      ...options.headers,
    },
  }

  const response = await fetch(url, finalOptions)

  if (response.status === 401) {
    if (!isAuthEndpoint) {
      // Only attempt additional refresh if token wasn't already refreshed
      if (!tokenWasRefreshed) {
        console.log('Received 401, attempting token refresh...')
        const newToken = await refreshAccessToken()

        if (newToken && newToken !== token) {
          // Retry with new token
          const retryOptions: RequestInit = {
            ...options,
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${newToken}`,
              ...options.headers,
            },
          }

          const retryResponse = await fetch(url, retryOptions)

          if (retryResponse.ok) {
            return await retryResponse.json() as ApiResponse<T>
          } else if (retryResponse.status === 401) {
            console.error('Retry with refreshed token also failed with 401')
          }
        }
      } else {
        console.log('Received 401 but token was already refreshed, authentication failure')
      }
    }

    // Handle authentication failure
    handleAuthenticationFailure('Authentication failed after token refresh attempts')
    throw new Error('Authentication failed')
  }

  if (!response.ok) {
    const errorText = await response.text()
    console.error(`API request failed: ${response.status} ${response.statusText}`, errorText)
    throw new Error(`API request failed: ${response.status} ${response.statusText}`)
  }

  return await response.json() as ApiResponse<T>
}

/**
 * Handle authentication failure by clearing tokens and redirecting to login
 */
const handleAuthenticationFailure = (reason: string) => {
  console.warn('Authentication failure:', reason)
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  
  // Save current location state for restoration after login
  saveCurrentLocationState()
  
  // Use React Router navigation if available, otherwise fallback to window.location
  if (navigationCallback) {
    console.log('Using React Router navigation for login redirect')
    navigationCallback('/login', { replace: true })
  } else {
    console.log('Falling back to window.location for login redirect')
    window.location.href = '/login'
  }
}
