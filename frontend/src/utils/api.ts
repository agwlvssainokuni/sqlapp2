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

import type {ApiResponse, RefreshTokenResult} from "../types/api.ts";
import { isTokenExpiringSoon, isTokenExpired } from "./jwtUtils.ts";

// Global promise to prevent multiple simultaneous refresh attempts
let refreshPromise: Promise<string | null> | null = null;

/**
 * Refresh the access token using the refresh token
 * @returns New access token or null if refresh failed
 */
const refreshAccessToken = async (): Promise<string | null> => {
  const refreshToken = localStorage.getItem('refreshToken');
  
  if (!refreshToken) {
    console.warn('No refresh token available');
    return null;
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
    });

    if (!response.ok) {
      console.error('Token refresh failed:', response.status, response.statusText);
      // Clear tokens if refresh failed
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      return null;
    }

    const apiResponse = await response.json() as ApiResponse<RefreshTokenResult>;
    
    if (!apiResponse.ok || !apiResponse.data) {
      console.error('Token refresh API returned error:', apiResponse.error);
      // Clear tokens if refresh failed
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      return null;
    }

    const { access_token, refresh_token } = apiResponse.data;
    
    // Update stored tokens
    localStorage.setItem('token', access_token);
    localStorage.setItem('refreshToken', refresh_token);
    
    console.log('Access token refreshed successfully');
    return access_token;
  } catch (error) {
    console.error('Error during token refresh:', error);
    // Clear tokens on any error
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    return null;
  }
};

/**
 * Get a valid access token, refreshing if necessary
 * @returns Valid access token or null if unable to obtain one
 */
const getValidAccessToken = async (): Promise<string | null> => {
  const accessToken = localStorage.getItem('token');
  
  if (!accessToken) {
    return null;
  }

  // Check if token is expired or expiring soon (within 30 seconds)
  const needsRefresh = isTokenExpired(accessToken) || isTokenExpiringSoon(accessToken, 30);
  
  if (!needsRefresh) {
    return accessToken;
  }

  // If already refreshing, wait for the existing promise
  if (refreshPromise) {
    console.log('Token refresh already in progress, waiting...');
    return await refreshPromise;
  }

  // Start new refresh process
  console.log('Access token expired or expiring soon, refreshing...');
  refreshPromise = refreshAccessToken();
  
  try {
    const newToken = await refreshPromise;
    return newToken;
  } finally {
    refreshPromise = null;
  }
};

export const apiRequest = async <T>(url: string, options: RequestInit = {}): Promise<ApiResponse<T>> => {
  // Skip token refresh for auth endpoints to avoid infinite loops
  const isAuthEndpoint = url.includes('/api/auth/');
  
  let token: string | null = null;
  
  if (!isAuthEndpoint) {
    token = await getValidAccessToken();
    
    if (!token) {
      // No valid token available, redirect to login
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
      throw new Error('No valid access token available');
    }
  } else {
    // For auth endpoints, use token as-is if available
    token = localStorage.getItem('token');
  }

  const finalOptions: RequestInit = {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token && {Authorization: `Bearer ${token}`}),
      ...options.headers,
    },
  };

  const response = await fetch(url, finalOptions);

  if (response.status === 401) {
    if (!isAuthEndpoint) {
      // Try to refresh token once more for non-auth endpoints
      console.log('Received 401, attempting final token refresh...');
      const newToken = await refreshAccessToken();
      
      if (newToken) {
        // Retry with new token
        const retryOptions: RequestInit = {
          ...options,
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${newToken}`,
            ...options.headers,
          },
        };
        
        const retryResponse = await fetch(url, retryOptions);
        
        if (retryResponse.ok) {
          return await retryResponse.json() as ApiResponse<T>;
        }
      }
    }
    
    // Clear tokens and redirect to login
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    window.location.href = '/login';
    throw new Error('Authentication failed');
  }

  if (!response.ok) {
    const errorText = await response.text();
    console.error(`API request failed: ${response.status} ${response.statusText}`, errorText);
    throw new Error(`API request failed: ${response.status} ${response.statusText}`);
  }

  return await response.json() as ApiResponse<T>;
};
