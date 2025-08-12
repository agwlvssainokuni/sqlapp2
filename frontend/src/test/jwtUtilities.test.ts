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

// Simple test for JWT utilities
import { 
  decodeJwt, 
  isTokenExpired, 
  getUsernameFromToken,
  isAccessToken,
  isRefreshToken 
} from '../utils/jwtUtils.ts';

// Test the JWT utilities
export const testJwtUtilities = () => {
  console.log('Testing JWT utilities...');

  // Sample token that expires in the future (this is a mock token for testing)
  const mockAccessToken = 'eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwic3ViIjoidGVzdHVzZXIiLCJpYXQiOjE3NTUwMTYwMTEsImV4cCI6OTk5OTk5OTk5OX0.mock-signature';
  const mockRefreshToken = 'eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsInN1YiI6InRlc3R1c2VyIiwiaWF0IjoxNzU1MDE2MDExLCJleHAiOjk5OTk5OTk5OTl9.mock-signature';
  const mockExpiredToken = 'eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwic3ViIjoidGVzdHVzZXIiLCJpYXQiOjE3NTUwMTYwMTEsImV4cCI6MTU1NTAwMDAwMH0.mock-signature';

  const tests = [
    {
      name: 'Decode valid token',
      test: () => {
        const decoded = decodeJwt(mockAccessToken);
        return decoded && decoded.sub === 'testuser' && decoded.token_type === 'access';
      }
    },
    {
      name: 'Detect expired token',
      test: () => {
        const expired = isTokenExpired(mockExpiredToken);
        return expired === true;
      }
    },
    {
      name: 'Detect valid token',
      test: () => {
        const expired = isTokenExpired(mockAccessToken);
        return expired === false;
      }
    },
    {
      name: 'Get username from token',
      test: () => {
        const username = getUsernameFromToken(mockAccessToken);
        return username === 'testuser';
      }
    },
    {
      name: 'Identify access token',
      test: () => {
        return isAccessToken(mockAccessToken) === true;
      }
    },
    {
      name: 'Identify refresh token',
      test: () => {
        return isRefreshToken(mockRefreshToken) === true;
      }
    },
    {
      name: 'Handle invalid token',
      test: () => {
        const decoded = decodeJwt('invalid.token');
        return decoded === null;
      }
    }
  ];

  let passed = 0;
  let failed = 0;

  tests.forEach(test => {
    try {
      if (test.test()) {
        console.log(`✅ ${test.name}`);
        passed++;
      } else {
        console.log(`❌ ${test.name}`);
        failed++;
      }
    } catch (error) {
      console.log(`❌ ${test.name} - Error: ${error}`);
      failed++;
    }
  });

  console.log(`\nTest Results: ${passed} passed, ${failed} failed`);
  return { passed, failed };
};

// Export for console testing
(window as any).testJwtUtilities = testJwtUtilities;