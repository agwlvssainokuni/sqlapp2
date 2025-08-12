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

interface JwtPayload {
  sub: string // subject (username)
  iat: number // issued at
  exp: number // expiration time
  token_type: string // access or refresh
}

/**
 * Decode JWT token without verification (client-side)
 * @param token JWT token string
 * @returns Decoded payload or null if invalid
 */
export const decodeJwt = (token: string): JwtPayload | null => {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      return null
    }

    // Decode the payload (second part)
    const payload = parts[1]
    // Add padding if needed for base64 decoding
    const paddedPayload = payload + '='.repeat((4 - payload.length % 4) % 4)
    const decodedPayload = atob(paddedPayload.replace(/-/g, '+').replace(/_/g, '/'))
    
    return JSON.parse(decodedPayload) as JwtPayload
  } catch (error) {
    console.error('Failed to decode JWT:', error)
    return null
  }
}

/**
 * Check if token is expired
 * @param token JWT token string
 * @returns true if expired, false if valid, null if token is invalid
 */
export const isTokenExpired = (token: string): boolean | null => {
  const payload = decodeJwt(token)
  if (!payload) {
    return null
  }

  const currentTime = Math.floor(Date.now() / 1000)
  return payload.exp < currentTime
}

/**
 * Check if token will expire within the specified buffer time
 * @param token JWT token string
 * @param bufferSeconds Buffer time in seconds (default: 30 seconds)
 * @returns true if token expires soon, false if not, null if token is invalid
 */
export const isTokenExpiringSoon = (token: string, bufferSeconds: number = 30): boolean | null => {
  const payload = decodeJwt(token)
  if (!payload) {
    return null
  }

  const currentTime = Math.floor(Date.now() / 1000)
  return payload.exp <= (currentTime + bufferSeconds)
}

/**
 * Get token expiration time as Date object
 * @param token JWT token string
 * @returns Date object or null if token is invalid
 */
export const getTokenExpiration = (token: string): Date | null => {
  const payload = decodeJwt(token)
  if (!payload) {
    return null
  }

  return new Date(payload.exp * 1000)
}

/**
 * Get username from token
 * @param token JWT token string
 * @returns username or null if token is invalid
 */
export const getUsernameFromToken = (token: string): string | null => {
  const payload = decodeJwt(token)
  return payload?.sub || null
}

/**
 * Check if token is an access token
 * @param token JWT token string
 * @returns true if access token, false otherwise
 */
export const isAccessToken = (token: string): boolean => {
  const payload = decodeJwt(token)
  return payload?.token_type === 'access'
}

/**
 * Check if token is a refresh token
 * @param token JWT token string
 * @returns true if refresh token, false otherwise
 */
export const isRefreshToken = (token: string): boolean => {
  const payload = decodeJwt(token)
  return payload?.token_type === 'refresh'
}