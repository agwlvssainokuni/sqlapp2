# CLAUDE.md

This file provides technical guidance to Claude Code (claude.ai/code) for efficient development support in this repository.

## Project Overview

SqlApp2 is a production-ready web-based SQL execution tool with enterprise-grade security and scalability.

### Technology Stack
- **Backend**: Java 21 + Spring Boot 3.5.4, Spring Security, JPA/Hibernate, H2 Database
- **Frontend**: React 19 + TypeScript, Vite 7.1.1, React Router, react-i18next v15.6.1
- **Database Support**: MySQL, PostgreSQL, MariaDB (via JDBC)
- **Deployment**: Docker + Docker Compose, GitHub Actions CI/CD, Prometheus/Grafana monitoring

### Architecture
- **Pattern**: Client-Server with REST APIs, integrated SPA deployment
- **Authentication**: JWT + Spring Security with BCrypt password hashing
- **Database**: H2 internal database + external RDBMS connections
- **Security**: AES-256-GCM encryption, PreparedStatement SQL execution, OWASP compliance

## Key Implementation Details

### Backend Structure
```
src/main/java/cherry/sqlapp2/
├── controller/         # REST APIs with OpenAPI documentation
├── service/           # Business logic layer
├── entity/            # JPA entities with validation
├── dto/               # Data Transfer Objects (many using Java records)
├── repository/        # Spring Data JPA repositories
└── config/            # Security, JWT, database configuration
```

### Frontend Structure
```
frontend/src/
├── components/        # Page components (ending with "Page.tsx")
├── context/          # React context (AuthContext with enhanced token management)
├── locales/          # i18n translation files (en/, ja/)
├── styles/           # Modular CSS architecture (8 files)
│   ├── common.css    # Layout, authentication, pagination
│   ├── Dashboard.css, SqlExecution.css, QueryBuilder.css
│   ├── ConnectionManagement.css, SchemaViewer.css
│   └── QueryHistory.css, SavedQueries.css
├── utils/            # Utility functions and API helpers
│   ├── api.ts        # Enhanced JWT token management
│   └── jwtUtils.ts   # Comprehensive token validation
└── i18n.ts           # Internationalization configuration
```

### Core Features
1. **Enhanced User Management**: 
   - JWT authentication with proactive token refresh (30s buffer)
   - Smart 401 handling with double-refresh prevention
   - Graceful session preservation and automatic page restoration
   - User registration/login with optimized authentication flow
2. **Database Connectivity**: Multi-RDBMS support, encrypted connection storage, connection testing
3. **Advanced Visual Query Builder**: 
   - Complete JOIN support (INNER/LEFT/RIGHT/FULL OUTER) with drag-and-drop interface
   - **Bidirectional Alias Synchronization**: FROM/JOIN alias changes auto-update across all SQL clauses
   - **Real-time Alias Conflict Detection**: Duplicate alias warnings with multi-language support
   - Complex multi-table queries with intelligent table reference management
4. **Enhanced SQL Execution**: 
   - Direct SQL + Visual Query Builder with seamless integration
   - **Advanced SQL Reverse Engineering**: Complex WHERE clause parsing (OR/AND operators, IS NULL/IS NOT NULL)
   - Parameterized queries with comprehensive result metadata
5. **Integrated Workflow**: Seamless Create → Execute → Save workflow with React Router state management
6. **Query Management**: Save/share queries, execution history, performance tracking
7. **Schema Browsing**: Table/column metadata display, auto-completion support
8. **Internationalization**: English/Japanese with real-time language switching and context-aware messaging
9. **Modular Architecture**: 8-file CSS structure for improved maintainability and component isolation

## Development Guidelines

### Java/Spring Boot Conventions
- **Package Structure**: `cherry.sqlapp2.*`
- **DTO Naming**: No "Response" suffix (e.g., `LoginResult`, not `LoginResultResponse`)
- **API Responses**: All REST endpoints return `ApiResponse<T>` wrapper
- **Records**: Use Java records for immutable DTOs where appropriate
- **Security**: All controllers use explicit `Authentication` parameter injection
- **Advanced SQL Processing**:
  - `SqlReverseEngineeringService.parseComplexWhereExpression()` for OR/AND/IS NULL parsing
  - `createWhereCondition()` helper for consistent condition creation
  - JSqlParser 5.3 integration with enhanced error handling
- **Testing**: Comprehensive unit and integration tests with @DisplayName in Japanese

### React/TypeScript Conventions
- **Component Naming**: Page components end with "Page" (e.g., `SqlExecutionPage.tsx`)
- **API Integration**: Use `.data` property from ApiResponse structure
- **State Management**: React Context for authentication, local state for UI
- **Internationalization**: Use `useTranslation` hook from react-i18next with context-aware warnings
- **Advanced State Management**: 
  - Use `useCallback` for alias synchronization functions to prevent infinite re-renders
  - Implement `checkAliasConflicts` for real-time duplicate detection
  - CASCADE updates across FROM/JOIN/SELECT/WHERE/ORDER BY clauses for seamless user experience
- **Styling**: Modular CSS architecture with component-specific files
  - `src/styles/common.css` - Layout, authentication, pagination
  - `src/styles/[Component].css` - Component-specific styles
  - 8-file structure for improved maintainability and code organization
- **Authentication Integration**: 
  - Token refresh handling with `getValidAccessToken()` 
  - Automatic redirect preservation via `sessionStorage`
  - Type-safe JWT utilities with comprehensive error handling

### API Standards
- **Authentication**: Bearer JWT tokens in Authorization header
- **Error Handling**: Consistent error response format via global exception handler
- **Documentation**: OpenAPI/Swagger annotations on all endpoints
- **Validation**: Input validation with proper error messages
- **Security**: CORS configuration, CSRF protection, rate limiting

## Database Schema

### Internal H2 Database Entities
- **User**: Authentication data with BCrypt password hashing
- **DatabaseConnection**: Encrypted external database connection configurations
- **SavedQuery**: User queries with sharing scope (private/public)
- **QueryHistory**: Execution metadata (timestamp, duration, record count)

### Key Relationships
- User 1:N DatabaseConnection (user isolation)
- User 1:N SavedQuery (with sharing control)
- User 1:N QueryHistory (execution tracking)
- SavedQuery 1:N QueryHistory (saved query executions)

## Security Implementation

### Enhanced Authentication Flow
1. User login → JWT access token + refresh token generation
2. Frontend stores tokens in localStorage with AuthContext management
3. **Proactive Token Refresh**: Automatic refresh 30 seconds before expiry
4. **Smart 401 Handling**: Prevention of double refresh attempts with tokenWasRefreshed tracking
5. API requests include Bearer token in Authorization header
6. JWT validation via Spring Security filter chain
7. **Graceful Redirect**: Session preservation with automatic return to original page after login

### Advanced Token Management
- **Two-Tier Refresh Strategy**: Proactive (30s buffer) + Reactive (401 response)
- **Race Condition Prevention**: Global refreshPromise to avoid simultaneous refresh attempts
- **Token Validation**: Client-side JWT expiry checking with comprehensive error handling
- **Authentication State**: Optimized initialization without unnecessary API calls
- **Secure Storage**: localStorage with automatic cleanup on authentication failure

### Data Protection
- **Database passwords**: AES-256-GCM encryption
- **SQL injection prevention**: PreparedStatement usage
- **User isolation**: Connection and query separation by user ID
- **HTTPS enforcement**: SSL/TLS in production environments
- **Token Security**: Automatic refresh, expiry validation, and secure cleanup

## Testing Strategy

### Test Coverage (358 tests, 100% success rate)
- **Unit Tests (305)**: Service layer, utilities, security components including advanced SQL parsing tests
- **Integration Tests (53)**: REST API endpoints, database integration, complete workflow validation
- **Test Structure**: @Nested classes with Japanese @DisplayName for clear documentation
- **Mock Strategy**: Mockito for external dependencies, @SpringBootTest for complex integration scenarios
- **Database**: H2 in-memory for integration tests with comprehensive SQL dialect support

### Quality Assurance
- **Static Analysis**: SonarCloud integration
- **Security Scanning**: OWASP dependency check, Trivy container scanning
- **Performance**: Load testing capabilities, metrics collection
- **Automation**: GitHub Actions CI/CD with comprehensive test suites

## Deployment & Operations

### Environment Configuration
- **Profiles**: dev, staging, prod with specific configurations
- **External Config**: Environment variables for sensitive data
- **Database Options**: Memory/file/server modes for H2
- **Logging**: Structured JSON logging with environment-specific levels

### Monitoring & Observability
- **Metrics**: Spring Boot Actuator + Prometheus + custom metrics
- **Health Checks**: Database connectivity, application health, system resources
- **Dashboards**: Grafana dashboards for performance monitoring
- **Alerting**: AlertManager for critical issue notifications

### Docker Deployment
```bash
# Development
docker-compose up -d

# Production with monitoring
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

## Common Development Tasks

### Adding New REST Endpoint
1. Create request/response DTOs (prefer records for immutable data)
2. Add service layer method with business logic
3. Implement controller method with OpenAPI annotations
4. Return `ApiResponse.success(data)` or `ApiResponse.error(errors)`
5. Add comprehensive unit and integration tests
6. Update frontend API integration if needed

### Adding New React Component
1. Follow naming convention (PageName + "Page.tsx" for pages)
2. Use TypeScript strict typing
3. Implement i18n with useTranslation hook
4. Follow existing patterns for API calls and error handling
5. Create component-specific CSS file in `src/styles/ComponentName.css`
6. Import CSS in App.tsx following modular structure
7. Ensure responsive design compatibility
8. Add appropriate testing coverage

### Database Schema Changes
1. Update JPA entities with proper validation annotations
2. Add migration scripts for production environments
3. Update integration test data as needed
4. Consider backward compatibility for rolling deployments
5. Update API documentation for affected endpoints

## Troubleshooting

### Common Issues
- **JWT Token Issues**: 
  - Check token expiration, refresh token flow
  - Verify proactive refresh (30s buffer) is working
  - Monitor double refresh prevention with tokenWasRefreshed flag
  - Check sessionStorage for redirect preservation
- **Database Connection**: Verify encryption/decryption, JDBC driver availability
- **CORS Errors**: Update SecurityConfig for new origins
- **i18n Missing Keys**: Add translations to both en/ and ja/ files
- **Test Failures**: Check database state isolation, mock configurations
- **SQL Standards Violation**: Ensure QueryBuilder uses proper table alias references (e.g., `SELECT u.name FROM users AS u` not `SELECT users.name FROM users AS u`)
- **React Hooks Warnings**: Use useCallback for functions, useRef for parameter state management to prevent infinite loops
- **Alias Synchronization Issues**: FROM/JOIN alias changes should automatically update all related SQL clauses via bidirectional synchronization
- **WHERE Clause Parsing**: Complex OR/AND conditions and IS NULL operators require parseComplexWhereExpression method for proper reverse engineering
- **Test Integration Issues**: Use @SpringBootTest instead of @WebMvcTest for complex security integration scenarios to avoid JWT dependency conflicts
- **CSS Module Issues**: Verify correct import paths for component-specific CSS files in src/styles/
- **Authentication Flow**: Check initialization optimization - avoid unnecessary checkAuthStatus() calls during app startup

### Development Tools
- **H2 Console**: Available at `/h2-console` (dev environment only)
- **Swagger UI**: Available at `/api/swagger-ui.html`
- **Actuator Endpoints**: Health, metrics, info at `/actuator/*`
- **Log Files**: Structured JSON output to stdout/files based on profile

## Code Style Guidelines

### Java
- Use modern Java features (records, var, stream operations)
- Comprehensive JavaDoc for public APIs
- Follow Spring Boot best practices
- Minimize `@Deprecated` usage, clean up legacy code promptly

### TypeScript/React
- Strict TypeScript configuration with comprehensive type safety
- Functional components with hooks and proper dependency arrays
- Semicolon-omitted style (Vite default)
- Modular CSS approach with 8-file component-specific structure
- Type-only imports for enhanced build optimization

### General
- Apache License header required on all source files
- Consistent error handling and logging
- Security-first mindset in all implementations
- Performance considerations for database operations

---

**Status**: Enterprise-Grade Visual SQL Query Builder - Enhanced Authentication & Modular Architecture
**Last Updated**: 2025-08-15
**Total Tests**: 358 (305 unit + 53 integration) - 100% success rate
**Development Phases**: 40 phases complete - JWT Token Management Optimization + CSS Architecture Refactoring + Production Ready
**Recent Enhancements**: 
- Advanced JWT token refresh with double-refresh prevention
- Modular CSS architecture (8-file structure) for improved maintainability
- Enhanced authentication flow with graceful session preservation
- Comprehensive error handling and debugging improvements