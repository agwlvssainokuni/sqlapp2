# CLAUDE.md

This file provides technical guidance to Claude Code (claude.ai/code) for efficient development support in this repository.

## Project Overview

SqlApp2 is a production-ready web-based SQL execution tool with enterprise-grade security and scalability.

### Technology Stack
- **Backend**: Java 21 + Spring Boot 3.5.4, Spring Security, JPA/Hibernate, H2 Database
- **Frontend**: React 18 + TypeScript, Vite 7.1.1, React Router, react-i18next v15.6.1
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
├── context/          # React context (AuthContext)
├── locales/          # i18n translation files (en/, ja/)
├── utils/            # Utility functions and API helpers
└── i18n.ts           # Internationalization configuration
```

### Core Features
1. **User Management**: JWT authentication, user registration/login, profile management
2. **Database Connectivity**: Multi-RDBMS support, encrypted connection storage, connection testing
3. **SQL Execution**: Direct SQL + Visual Query Builder, parameterized queries, result metadata
4. **Query Management**: Save/share queries, execution history, performance tracking
5. **Schema Browsing**: Table/column metadata display, auto-completion support
6. **Internationalization**: English/Japanese with real-time language switching

## Development Guidelines

### Java/Spring Boot Conventions
- **Package Structure**: `cherry.sqlapp2.*`
- **DTO Naming**: No "Response" suffix (e.g., `LoginResult`, not `LoginResultResponse`)
- **API Responses**: All REST endpoints return `ApiResponse<T>` wrapper
- **Records**: Use Java records for immutable DTOs where appropriate
- **Security**: All controllers use explicit `Authentication` parameter injection
- **Testing**: Comprehensive unit and integration tests with @DisplayName in Japanese

### React/TypeScript Conventions
- **Component Naming**: Page components end with "Page" (e.g., `SqlExecutionPage.tsx`)
- **API Integration**: Use `.data` property from ApiResponse structure
- **State Management**: React Context for authentication, local state for UI
- **Internationalization**: Use `useTranslation` hook from react-i18next
- **Styling**: CSS3 with component-based approach, responsive design

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

### Authentication Flow
1. User login → JWT access token + refresh token generation
2. Frontend stores tokens in AuthContext
3. API requests include Bearer token in Authorization header
4. JWT validation via Spring Security filter chain

### Data Protection
- **Database passwords**: AES-256-GCM encryption
- **SQL injection prevention**: PreparedStatement usage
- **User isolation**: Connection and query separation by user ID
- **HTTPS enforcement**: SSL/TLS in production environments

## Testing Strategy

### Test Coverage (356 tests, 100% success rate)
- **Unit Tests (303)**: Service layer, utilities, security components
- **Integration Tests (53)**: REST API endpoints, database integration
- **Test Structure**: @Nested classes with Japanese @DisplayName
- **Mock Strategy**: Mockito for external dependencies
- **Database**: H2 in-memory for integration tests

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
5. Ensure responsive design compatibility
6. Add appropriate testing coverage

### Database Schema Changes
1. Update JPA entities with proper validation annotations
2. Add migration scripts for production environments
3. Update integration test data as needed
4. Consider backward compatibility for rolling deployments
5. Update API documentation for affected endpoints

## Troubleshooting

### Common Issues
- **JWT Token Issues**: Check token expiration, refresh token flow
- **Database Connection**: Verify encryption/decryption, JDBC driver availability
- **CORS Errors**: Update SecurityConfig for new origins
- **i18n Missing Keys**: Add translations to both en/ and ja/ files
- **Test Failures**: Check database state isolation, mock configurations

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
- Strict TypeScript configuration
- Functional components with hooks
- Semicolon-omitted style (Vite default)
- CSS-in-JS or modular CSS approach

### General
- Apache License header required on all source files
- Consistent error handling and logging
- Security-first mindset in all implementations
- Performance considerations for database operations

---

**Status**: Production Ready - Full enterprise-grade SQL execution tool
**Last Updated**: 2025-08-14
**Total Tests**: 356 (303 unit + 53 integration) - 100% success rate