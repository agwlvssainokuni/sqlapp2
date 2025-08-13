#!/bin/bash
#
# Copyright 2025 agwlvssainokuni
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# SqlApp2 Environment Variables Validation Script
# This script validates that all required environment variables are set for production deployment

set -e

echo "==========================================="
echo "SqlApp2 Environment Variables Validation"
echo "==========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Validation status
VALIDATION_FAILED=false

# Function to check if variable is set and not empty
check_required_var() {
    local var_name="$1"
    local var_value="${!var_name}"

    if [ -z "$var_value" ]; then
        echo -e "${RED}❌ MISSING: $var_name${NC}"
        VALIDATION_FAILED=true
        return 1
    else
        echo -e "${GREEN}✅ $var_name is set${NC}"
        return 0
    fi
}

# Function to check if variable meets minimum length requirement
check_min_length() {
    local var_name="$1"
    local min_length="$2"
    local var_value="${!var_name}"

    if [ ${#var_value} -lt $min_length ]; then
        echo -e "${YELLOW}⚠️  WARNING: $var_name should be at least $min_length characters long${NC}"
        return 1
    else
        echo -e "${GREEN}✅ $var_name meets minimum length requirement${NC}"
        return 0
    fi
}

# Function to check if variable is valid base64
check_base64() {
    local var_name="$1"
    local var_value="${!var_name}"

    if ! echo "$var_value" | base64 -d > /dev/null 2>&1; then
        echo -e "${RED}❌ INVALID: $var_name is not valid base64${NC}"
        VALIDATION_FAILED=true
        return 1
    else
        echo -e "${GREEN}✅ $var_name is valid base64${NC}"
        return 0
    fi
}

echo "Checking required environment variables..."
echo

# Check Spring Profile
echo "🔍 Spring Profile Configuration:"
check_required_var "SPRING_PROFILES_ACTIVE"
echo

# Check Database Configuration (for staging/prod)
if [ "$SPRING_PROFILES_ACTIVE" = "staging" ] || [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
    echo "🔍 Database Configuration:"
    check_required_var "H2_USERNAME"
    check_required_var "H2_PASSWORD"

    if [ -n "$H2_PASSWORD" ]; then
        check_min_length "H2_PASSWORD" 8
    fi
    echo
fi

# Check JWT Configuration
echo "🔍 JWT Security Configuration:"
check_required_var "JWT_SECRET"
check_required_var "JWT_ACCESS_TOKEN_EXPIRATION"
check_required_var "JWT_REFRESH_TOKEN_EXPIRATION"

if [ -n "$JWT_SECRET" ]; then
    check_min_length "JWT_SECRET" 32
fi
echo

# Check Encryption Configuration
echo "🔍 Data Encryption Configuration:"
check_required_var "ENCRYPTION_KEY"

if [ -n "$ENCRYPTION_KEY" ]; then
    check_base64 "ENCRYPTION_KEY"

    # Check if key is 32 bytes (44 characters when base64 encoded)
    if [ ${#ENCRYPTION_KEY} -ne 44 ]; then
        echo -e "${YELLOW}⚠️  WARNING: ENCRYPTION_KEY should be exactly 32 bytes (44 base64 characters)${NC}"
    fi
fi
echo

# Check CORS Configuration (for staging/prod)
if [ "$SPRING_PROFILES_ACTIVE" = "staging" ] || [ "$SPRING_PROFILES_ACTIVE" = "prod" ]; then
    echo "🔍 CORS Configuration:"
    check_required_var "CORS_ALLOWED_ORIGINS"
    echo
fi

# Check optional performance configurations
echo "🔍 Optional Performance Configuration:"
echo -e "${GREEN}ℹ️  SQL_EXECUTION_TIMEOUT: ${SQL_EXECUTION_TIMEOUT:-'300000 (default)'}${NC}"
echo -e "${GREEN}ℹ️  SQL_EXECUTION_MAX_ROWS: ${SQL_EXECUTION_MAX_ROWS:-'1000 (default)'}${NC}"
echo -e "${GREEN}ℹ️  SQL_DEFAULT_PAGE_SIZE: ${SQL_DEFAULT_PAGE_SIZE:-'25 (default)'}${NC}"
echo -e "${GREEN}ℹ️  SQL_MAX_PAGE_SIZE: ${SQL_MAX_PAGE_SIZE:-'100 (default)'}${NC}"
echo

# Final validation result
echo "==========================================="
if [ "$VALIDATION_FAILED" = true ]; then
    echo -e "${RED}❌ VALIDATION FAILED${NC}"
    echo "Please set all required environment variables before deploying to production."
    echo "Refer to .env.example for required variables and their formats."
    exit 1
else
    echo -e "${GREEN}✅ VALIDATION PASSED${NC}"
    echo "All required environment variables are properly configured."
    echo "Your environment is ready for $SPRING_PROFILES_ACTIVE deployment."
fi
echo "==========================================="
