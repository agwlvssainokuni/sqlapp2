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

import {describe, expect, it} from 'vitest'
import {decodeJwt, getUsernameFromToken, isAccessToken, isRefreshToken, isTokenExpired} from '../utils/jwtUtils'

describe('JWT Utilities', () => {
  // Sample tokens for testing (mock tokens with predictable payloads)
  const mockAccessToken = 'eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwic3ViIjoidGVzdHVzZXIiLCJpYXQiOjE3NTUwMTYwMTEsImV4cCI6OTk5OTk5OTk5OX0.mock-signature'
  const mockRefreshToken = 'eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsInN1YiI6InRlc3R1c2VyIiwiaWF0IjoxNzU1MDE2MDExLCJleHAiOjk5OTk5OTk5OTl9.mock-signature'
  const mockExpiredToken = 'eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwic3ViIjoidGVzdHVzZXIiLCJpYXQiOjE3NTUwMTYwMTEsImV4cCI6MTU1NTAwMDAwMH0.mock-signature'

  describe('decodeJwt', () => {
    it('should decode valid JWT token', () => {
      const decoded = decodeJwt(mockAccessToken)

      expect(decoded).toBeTruthy()
      expect(decoded?.sub).toBe('testuser')
      expect(decoded?.token_type).toBe('access')
      expect(decoded?.iat).toBe(1755016011)
      expect(decoded?.exp).toBe(9999999999)
    })

    it('should return null for invalid token', () => {
      const decoded = decodeJwt('invalid.token')
      expect(decoded).toBeNull()
    })

    it('should return null for malformed token', () => {
      const decoded = decodeJwt('not.a.jwt.token.at.all')
      expect(decoded).toBeNull()
    })

    it('should return null for empty string', () => {
      const decoded = decodeJwt('')
      expect(decoded).toBeNull()
    })
  })

  describe('isTokenExpired', () => {
    it('should detect expired token', () => {
      const expired = isTokenExpired(mockExpiredToken)
      expect(expired).toBe(true)
    })

    it('should detect valid (non-expired) token', () => {
      const expired = isTokenExpired(mockAccessToken)
      expect(expired).toBe(false)
    })

    it('should return true for invalid token', () => {
      const expired = isTokenExpired('invalid.token')
      expect(expired).toBe(null)
    })

    it('should return true for empty token', () => {
      const expired = isTokenExpired('')
      expect(expired).toBe(null)
    })
  })

  describe('getUsernameFromToken', () => {
    it('should extract username from valid token', () => {
      const username = getUsernameFromToken(mockAccessToken)
      expect(username).toBe('testuser')
    })

    it('should extract username from refresh token', () => {
      const username = getUsernameFromToken(mockRefreshToken)
      expect(username).toBe('testuser')
    })

    it('should return null for invalid token', () => {
      const username = getUsernameFromToken('invalid.token')
      expect(username).toBeNull()
    })

    it('should return null for empty token', () => {
      const username = getUsernameFromToken('')
      expect(username).toBeNull()
    })
  })

  describe('isAccessToken', () => {
    it('should identify access token correctly', () => {
      const result = isAccessToken(mockAccessToken)
      expect(result).toBe(true)
    })

    it('should reject refresh token', () => {
      const result = isAccessToken(mockRefreshToken)
      expect(result).toBe(false)
    })

    it('should reject invalid token', () => {
      const result = isAccessToken('invalid.token')
      expect(result).toBe(false)
    })

    it('should reject empty token', () => {
      const result = isAccessToken('')
      expect(result).toBe(false)
    })
  })

  describe('isRefreshToken', () => {
    it('should identify refresh token correctly', () => {
      const result = isRefreshToken(mockRefreshToken)
      expect(result).toBe(true)
    })

    it('should reject access token', () => {
      const result = isRefreshToken(mockAccessToken)
      expect(result).toBe(false)
    })

    it('should reject invalid token', () => {
      const result = isRefreshToken('invalid.token')
      expect(result).toBe(false)
    })

    it('should reject empty token', () => {
      const result = isRefreshToken('')
      expect(result).toBe(false)
    })
  })

  describe('Edge cases', () => {
    it('should handle token with missing payload', () => {
      const tokenWithoutPayload = 'eyJhbGciOiJIUzUxMiJ9..signature'
      expect(decodeJwt(tokenWithoutPayload)).toBeNull()
      expect(isTokenExpired(tokenWithoutPayload)).toBe(null)
      expect(getUsernameFromToken(tokenWithoutPayload)).toBeNull()
      expect(isAccessToken(tokenWithoutPayload)).toBe(false)
      expect(isRefreshToken(tokenWithoutPayload)).toBe(false)
    })

    it('should handle token with invalid base64 payload', () => {
      const tokenWithInvalidPayload = 'eyJhbGciOiJIUzUxMiJ9.invalid-base64!@#.signature'
      expect(decodeJwt(tokenWithInvalidPayload)).toBeNull()
      expect(isTokenExpired(tokenWithInvalidPayload)).toBe(null)
      expect(getUsernameFromToken(tokenWithInvalidPayload)).toBeNull()
      expect(isAccessToken(tokenWithInvalidPayload)).toBe(false)
      expect(isRefreshToken(tokenWithInvalidPayload)).toBe(false)
    })
  })
})
