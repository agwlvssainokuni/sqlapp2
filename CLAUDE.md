# CLAUDE.md

This file provides technical guidance to Claude Code (claude.ai/code) for efficient development support in this repository.

## Project Overview

SqlApp2 is a production-ready web-based SQL execution tool with enterprise-grade security and scalability.

### Technology Stack
- **Backend**: Java 21 + Spring Boot 3.5.4, Spring Security, JPA/Hibernate, H2 Database
- **Frontend**: React 19.1.1 + TypeScript 5.9.2, Vite 7.1.2, React Router 7.8.0, react-i18next v15.6.1
- **Database Support**: MySQL, PostgreSQL, MariaDB, H2 (via JDBC)
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
├── controller/         # REST APIs with OpenAPI documentation and Japanese javadoc
├── service/           # Business logic layer with comprehensive documentation
├── entity/            # JPA entities with validation and Japanese javadoc
├── dto/               # Data Transfer Objects (many using Java records)
├── repository/        # Spring Data JPA repositories
├── util/              # Utility classes (JWT, SQL analysis, parameter extraction)
└── config/            # Security, JWT, database configuration
```

### Frontend Structure
```
frontend/src/
├── components/        # Page components (ending with "Page.tsx")
├── context/          # React context (AuthContext with enhanced token management)
├── locales/          # i18n translation files (en/, ja/)
├── styles/           # Modular CSS architecture (8 files with copyright headers)
│   ├── common.css    # Layout, authentication, pagination, header/main/footer
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
   - **Admin Approval System**: User registration requires administrator approval for security
   - **Multi-Language Email Notifications**: Comprehensive email system with language-aware notifications
   - **Language Integration**: User registration emails linked with UI language selection, stored language preferences for future notifications
   - **Database-Managed Email Templates**: Configurable email templates with variable substitution
   - JWT authentication with proactive token refresh (30s buffer)
   - Smart 401 handling with double-refresh prevention
   - Graceful session preservation and automatic page restoration
2. **Database Connectivity**: Multi-RDBMS support, encrypted connection storage, connection testing
3. **Advanced Visual Query Builder**: 
   - Complete JOIN support (INNER/LEFT/RIGHT/FULL OUTER) with drag-and-drop interface
   - **Enhanced BETWEEN Predicate Support**: Dual input fields (min/max values) with proper UI/backend integration
   - **Bidirectional Alias Synchronization**: FROM/JOIN alias changes auto-update across all SQL clauses
   - **Real-time Alias Conflict Detection**: Duplicate alias warnings with multi-language support
   - Complex multi-table queries with intelligent table reference management
4. **Enhanced SQL Execution**: 
   - Direct SQL + Visual Query Builder with seamless integration
   - **Advanced SQL Reverse Engineering**: Complex WHERE clause parsing with BETWEEN/OR/AND operator protection
   - **Multi-Condition WHERE Clause Support**: Accurate parsing of mixed OR/BETWEEN conditions (e.g., `col1 IS NULL OR col2 BETWEEN 'a' AND 'b'`)
   - Parameterized queries with comprehensive result metadata
5. **Integrated Workflow**: Seamless Create → Execute → Save workflow with React Router state management
6. **Enhanced Query Management**: 
   - Save/share queries with comprehensive metadata
   - **Advanced Query History with Date Range Filtering**: All history endpoints (`/history`, `/history/successful`, `/history/failed`) support date range specification
   - **Configurable Default Period**: Default 30-day history period configurable via `app.query-history.default-period-days`
   - **Unified Date Range UI**: Consistent datetime-local input fields across all filter types
   - Performance tracking with detailed statistics
7. **Schema Browsing**: Table/column metadata display, auto-completion support
8. **Internationalization**: English/Japanese with real-time language switching and context-aware messaging
9. **Enterprise Security Features**: 
   - **Role-Based Access Control**: USER/ADMIN roles with method-level security
   - **Admin Management UI**: Complete admin interface with user approval workflow
   - **Email Template Management**: Admin interface for managing multi-language email templates
10. **Modular Architecture**: 8-file CSS structure for improved maintainability and component isolation
11. **Development Support**: MailPit integration for email testing without external delivery
12. **Comprehensive Development Environment**: Docker-based unified development environment with multiple database servers (MySQL, PostgreSQL, MariaDB, H2) and email server (MailPit)

## Development Guidelines

### Java/Spring Boot Conventions
- **Package Structure**: `cherry.sqlapp2.*`
- **DTO Naming**: No "Response" suffix (e.g., `LoginResult`, not `LoginResultResponse`)
- **API Responses**: All REST endpoints return `ApiResponse<T>` wrapper
- **Records**: Use Java records for immutable DTOs where appropriate
- **Security**: All controllers use explicit `Authentication` parameter injection
- **Advanced SQL Processing**:
  - `SqlReverseEngineeringService.parseComplexWhereExpression()` with BETWEEN-aware OR/AND parsing
  - `splitRespectingBetween()` algorithm for protected BETWEEN clause handling
  - `createBetweenWhereCondition()` for dual-value BETWEEN condition creation
  - **Complete Aggregate Function Support**: `parseSelectExpression()`, `parseOrderByExpression()` for comprehensive function parsing
  - **Enhanced HAVING/WHERE Parsing**: `createWhereConditionWithAggregateSupport()` for aggregate function recognition in conditions
  - **Multi-Clause Aggregate Processing**: COUNT, SUM, AVG, MAX, MIN functions across SELECT, HAVING, ORDER BY, BETWEEN clauses
  - JSqlParser 5.3 integration with enhanced error handling and aggregate function extraction
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
- **User**: Authentication data with BCrypt password hashing and language preference storage
- **DatabaseConnection**: Encrypted external database connection configurations
- **SavedQuery**: User queries with sharing scope (private/public)
- **QueryHistory**: Execution metadata (timestamp, duration, record count)
- **EmailTemplate**: Multi-language email templates with variable substitution support

### Key Relationships
- User 1:N DatabaseConnection (user isolation)
- User 1:N SavedQuery (with sharing control)
- User 1:N QueryHistory (execution tracking)
- SavedQuery 1:N QueryHistory (saved query executions)
- WhereCondition: Enhanced with minValue/maxValue for BETWEEN operator support

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
- **Admin Approval System**: Registration requires administrator approval to prevent unauthorized access
- **Email Security**: MailPit for development to prevent accidental email delivery

## Testing Strategy

### Test Coverage (361 tests, 100% success rate)
- **Unit Tests (306)**: Service layer, utilities, security components including advanced SQL parsing and date range filtering tests
- **Integration Tests (55)**: REST API endpoints, database integration, complete workflow validation including date range parameters
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
# Development environment (multiple databases + MailPit)
cd docker
docker-compose -f docker-compose.dev.yml up -d

# Production deployment
docker-compose up -d

# Production with monitoring
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

### Development Database Environment
The project includes a comprehensive Docker-based development environment:
- **MySQL 8.0**: `localhost:13306`
- **PostgreSQL 15**: `localhost:15432`
- **MariaDB 10.11**: `localhost:13307`
- **H2 Database**: `localhost:19092` (Server), `localhost:18082` (Console)
- **MailPit**: `localhost:1025` (SMTP), `http://localhost:8025` (Web UI)
- **phpMyAdmin**: `http://localhost:10080` (MySQL/MariaDB management)
- **pgAdmin**: `http://localhost:10081` (PostgreSQL management)

All databases include sample data (users, products, orders tables) for testing purposes.

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

### Email Template Management
1. Create templates using admin interface at `/admin` page
2. Support both English and Japanese languages
3. Use variable substitution with {{variable}} syntax
4. Test with MailPit during development
5. Configure fallback language in application.properties

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
- **QueryBuilder DISTINCT Issues**: Check both QueryStructure.isDistinct() global flag and individual SelectColumn.isDistinct() flags are processed in buildSelectClause()
- **React Hooks Warnings**: Use useCallback for functions, useRef for parameter state management to prevent infinite loops
- **Alias Synchronization Issues**: FROM/JOIN alias changes should automatically update all related SQL clauses via bidirectional synchronization
- **WHERE Clause Parsing**: Complex OR/AND/BETWEEN mixed conditions require splitRespectingBetween() for accurate parsing
- **BETWEEN Operator Issues**: Use minValue/maxValue fields instead of values array for dual-input BETWEEN conditions
- **Reverse Engineering Failures**: Check OR conditions with BETWEEN clauses - ensure BETWEEN AND keywords are protected from OR splitting
- **Aggregate Function Parsing Issues**: Verify parseSelectExpression(), parseOrderByExpression(), and createWhereConditionWithAggregateSupport() methods handle COUNT/SUM/AVG/MAX/MIN correctly
- **Complex SQL Parsing Failures**: For aggregate functions in nested conditions, ensure JSqlParser 5.3 compatibility and proper parentheses extraction
- **Test Integration Issues**: Use @SpringBootTest instead of @WebMvcTest for complex security integration scenarios to avoid JWT dependency conflicts
- **CSS Module Issues**: Verify correct import paths for component-specific CSS files in src/styles/
- **Authentication Flow**: Check initialization optimization - avoid unnecessary checkAuthStatus() calls during app startup
- **Email Template Issues**: Use fallback language (configured in application.properties) when template not found for user's language
- **Admin Access Issues**: Ensure user has ADMIN role and check @PreAuthorize annotations on admin endpoints
- **MailPit Connection**: Verify MailPit is running on port 1025 (SMTP) and 8025 (Web UI) for email testing
- **Docker Volume Permission Issues**: Fixed with UID/GID standardization (1001:1001) in Dockerfile for consistent volume access across environments

### Development Tools
- **H2 Console**: Available at `/h2-console` (dev environment only)
- **Swagger UI**: Available at `/api/swagger-ui.html`
- **Admin Interface**: Available at `/admin` for user management and email templates
- **MailPit Web UI**: Available at `http://localhost:8025` for email testing
- **Development Environment**: Complete Docker stack available at `docker/docker-compose.dev.yml` with multiple databases and management tools
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
- Comprehensive Japanese javadoc documentation on all Java classes and methods
- Consistent error handling and logging
- Security-first mindset in all implementations
- Performance considerations for database operations

---

**Status**: Enterprise-Grade Visual SQL Query Builder - Complete Development Environment & Volume Permission Resolution
**Last Updated**: 2025-08-17
**Total Tests**: 361 (306 unit + 55 integration) - 100% success rate
**Development Phases**: 51+ phases complete - Complete Backend Dead Code Cleanup + Comprehensive Docker Development Environment + Volume Permission Resolution + Language-Integrated Admin Approval System + Database-Managed Email Templates + Query History Date Range Filtering + Complete Metrics Integration + Aggregate Function Support + Advanced SQL Reverse Engineering + Production Ready
**Recent Enhancements**: 
- **Phase 33: Complete Backend Dead Code Cleanup** - Comprehensive removal of unused @Deprecated methods
  - Eliminated 27 unused methods across repository, service, and DTO layers
  - Removed 27 corresponding test methods testing deprecated functionality
  - Reduced codebase by 598 lines while maintaining 100% functionality
  - RefreshTokenService and JwtUtil internal dependency refactoring
  - Zero @Deprecated annotations remaining in codebase
  - Updated test count: 361 tests (306 unit + 55 integration) with 100% success rate
- **Phase 32: Comprehensive Docker Development Environment** - Complete unified development infrastructure
  - Multi-database Docker environment with MySQL 8.0, PostgreSQL 15, MariaDB 10.11, H2 Database Server
  - MailPit integration for email testing with Web UI and SMTP server
  - Database management tools: phpMyAdmin for MySQL/MariaDB, pgAdmin for PostgreSQL, H2 Console
  - Sample data initialization across all database servers for testing
  - Port standardization (base + 10000) to avoid conflicts with main application
  - Docker volume permission resolution using fixed UID/GID (1001:1001) for consistent access
  - Elimination of special initialization containers while maintaining security (non-root execution)
- **Phase 31: Language Integration for Email Notifications** - Complete UI language linking with email system
  - User registration emails now linked with UI language selection via API parameter integration
  - Language preferences stored in User entity for consistent approval/rejection email delivery
  - Approval and rejection emails sent in the language selected during user registration
  - Frontend integration sending current i18n language to registration API
  - Comprehensive test coverage for language integration across all email notification scenarios
- **Phase 30: Complete Admin Approval System with Email Notifications** - Enterprise-grade user management with multi-language email support
  - Admin approval required for all new user registrations for enhanced security
  - Multi-language email notification system (English/Japanese) with database-managed templates
  - Admin management interface with user approval/rejection workflow and email template management
  - MailPit integration for development email testing without external delivery
  - Automatic initial admin account creation and email template initialization
  - Role-based access control (USER/ADMIN) with Spring Security method-level protection
- **Query History Date Range Filtering Enhancement** - Complete date range filtering across all query history endpoints
  - All endpoints (`/api/queries/history`, `/history/successful`, `/history/failed`) support `fromDate`/`toDate` parameters
  - Configurable default period (30 days) via `app.query-history.default-period-days` setting
  - Repository layer methods for success/failure + date range combinations
  - Unified frontend UI for date range specification across all filter types
  - Comprehensive test coverage for all new date range filtering scenarios
- **Phase 28: Complete MetricsService Integration** - 100% method utilization with comprehensive monitoring
  - SQL execution metrics (timing, result counts, error tracking)
  - User activity metrics (login, logout, registration)
  - Database connection metrics (success/failure tracking by DB type)
  - Query management metrics (save, update, delete operations)
  - Health check integration with real-time statistics
- **Phase 27: Complete Aggregate Function Parsing** - Full support for aggregate functions (COUNT, SUM, AVG, MAX, MIN) in SELECT, HAVING, and ORDER BY clauses
- **Advanced SQL Reverse Engineering** - Accurate parsing and structuring of complex SQL queries with aggregate functions
- **Enhanced Query Builder Compatibility** - Seamless bidirectional conversion of aggregate function queries (SQL ↔ Visual Builder)
- **Phase 25: BETWEEN Predicate Complete Support** - Dual min/max input fields, proper SQL generation, reverse engineering
- **Phase 26: Complex WHERE Clause Parsing** - OR+BETWEEN mixed conditions, BETWEEN AND keyword protection, accurate reverse engineering
- **Enhanced Dashboard i18n** - Complete multilingual support for all feature descriptions including H2 database references
- **Comprehensive Japanese Javadoc Documentation** - All Java classes and methods documented in Japanese
- QueryBuilder DISTINCT functionality fix: Global distinct flag now properly processed in SQL generation
- React Router 7.8.0 integration with proper component hierarchy (Router → AuthProvider → Routes)
- Enhanced CSS architecture with copyright headers and complete style organization
- Advanced JWT token refresh with double-refresh prevention and navigation callback system
- Comprehensive dependency updates (React 19.1.1, TypeScript 5.9.2, Vite 7.1.2)
- Modular CSS architecture (8-file structure) for improved maintainability