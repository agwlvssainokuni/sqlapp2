# Contributing to SqlApp2

Welcome to SqlApp2! Thank you for your interest in contributing to our enterprise-grade SQL execution tool. This comprehensive guide will help you set up your development environment, understand our processes, and contribute effectively.

## 📊 Project Overview

**SqlApp2** is a production-ready, web-based SQL execution tool that provides secure database connectivity and query management.

### Technology Stack
- **Backend**: Java 21 + Spring Boot 3.5.4, Spring Security, JPA/Hibernate, H2 Database
- **Frontend**: React 19 + TypeScript, Vite 7.1.1, React Router, react-i18next v15.6.1
- **Database Support**: MySQL, PostgreSQL, MariaDB (external connections)
- **Deployment**: Docker + Docker Compose, GitHub Actions CI/CD
- **Monitoring**: Prometheus + Grafana + Spring Boot Actuator

### Project Status
**Current Status**: ✅ **Production Ready** - Enterprise-grade SQL execution tool
- **Test Coverage**: 356 tests (303 unit + 53 integration) with 100% success rate
- **Security**: OWASP compliant, JWT authentication, AES-256-GCM encryption
- **CI/CD**: Comprehensive GitHub Actions workflows with automated testing and security scanning
- **Monitoring**: Prometheus/Grafana integration with custom metrics and alerting

## 🚀 Quick Development Setup

### Prerequisites

#### Required Software
- **Java 21** (OpenJDK recommended)
- **Node.js 18+** (including npm)
- **Git**

#### Recommended Software
- **Docker & Docker Compose** (for containerized development and production deployment)
- **IDE**: IntelliJ IDEA Ultimate, VS Code with Java/TypeScript extensions

### Local Development Environment

#### 1. Repository Setup
```bash
# Clone your fork
git clone <repository-url>
cd sqlapp2

# Set up upstream remote (if contributing)
git remote add upstream https://github.com/ORIGINAL_OWNER/sqlapp2.git
```

#### 2. Backend Development
```bash
# Set execute permission on Gradle wrapper
chmod +x gradlew

# Build and verify dependencies
./gradlew build

# Start development server with hot reload
./gradlew bootRun
```
Backend runs at `http://localhost:8080`

#### 3. Frontend Development
```bash
# Install dependencies
cd frontend
npm install

# Start development server with hot reload
npm run dev
```
Frontend runs at `http://localhost:5173` with automatic API proxy to `http://localhost:8080`

## 🛠️ Development Workflows

### Standard Development Flow

1. **Backend Development**:
   ```bash
   ./gradlew bootRun
   ```

2. **Frontend Development** (in separate terminal):
   ```bash
   cd frontend && npm run dev
   ```

3. **Access Application**: Navigate to `http://localhost:5173`

### Integrated Testing
Build frontend and integrate with backend for full testing:
```bash
./gradlew build
./gradlew bootRun
```
Access integrated version at `http://localhost:8080`

### Docker Development Environment
```bash
# Build application
./gradlew build

# Build and run Docker containers
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## 🗄️ Database Configuration

### Development Environment (H2 in-memory)
- **H2 Console**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:sqlapp2db`
- **Username**: `sa`
- **Password**: (empty)

### Production Environment (H2 file-based)
- **Data Files**: `/app/data/sqlapp2db`
- **Persistence**: Docker volumes for data retention

## 🧪 Testing

### Current Test Achievement
**356 tests with 100% success rate** across comprehensive test suites:

```bash
# All backend tests (303 unit + 53 integration tests)
./gradlew test

# Frontend tests (vitest)
cd frontend
npm test

# Full build verification (includes all tests)
./gradlew build
```

### Testing Guidelines

#### Backend Testing
- **Unit Tests (303)**: Service layer, utilities, security components
- **Integration Tests (53)**: REST API endpoints, database integration  
- Use `@SpringBootTest` for integration tests with proper transaction isolation
- Mock external dependencies with Mockito
- Test both success scenarios and comprehensive error handling
- Use @Nested classes with Japanese @DisplayName for clear test documentation

#### Frontend Testing
- Component testing with React Testing Library and vitest
- Unit tests for utility functions and API integrations
- Integration tests for critical user workflows
- Test accessibility features and responsive design

## 📝 Coding Standards

### Java/Spring Boot Guidelines

- **Modern Java 21**: Use records, var type inference, Stream API, and latest language features
- **Spring Boot Standards**: Follow [Spring Boot best practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- **API Response Format**: All REST endpoints return `ApiResponse<T>` wrapper format
- **DTO Naming**: No "Response" suffix (e.g., `LoginResult`, not `LoginResponse`)
- **Authentication**: Controllers use explicit `Authentication` parameter injection
- **Package Structure**: `cherry.sqlapp2.*`
- **Naming Conventions**:
  - Classes: `PascalCase`
  - Methods: `camelCase`  
  - Constants: `UPPER_SNAKE_CASE`
- **Testing**: Comprehensive unit and integration tests with Japanese @DisplayName

### TypeScript/React Guidelines

- **Modern React**: Use functional components with React Hooks (React 19)
- **Component Naming**: Page components end with "Page" suffix (e.g., `SqlExecutionPage.tsx`)
- **Type Safety**: Follow TypeScript strict mode, avoid `any` types
- **API Integration**: Use `.data` property from ApiResponse structure
- **Internationalization**: Use `useTranslation` hook from react-i18next v15.6.1
- **Naming Conventions**:
  - Components: `PascalCase`
  - Variables/functions: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **CSS**: Use kebab-case classes with responsive-first design approach

### Git Commit Standards

Use clear, descriptive commit messages with conventional format:
```
feat: add user profile management feature
fix: resolve SQL injection vulnerability in parameter processing
docs: update API documentation with new endpoints
style: format code according to project standards
refactor: optimize database connection pooling logic
test: add comprehensive unit tests for authentication service
chore: update dependencies to latest stable versions
```

## 🏗️ Architecture Guidelines

### Backend Architecture
- **MVC Pattern**: Clear separation of Controller, Service, Repository layers
- **Dependency Injection**: Use Spring's IoC container effectively
- **Layer Responsibilities**:
  - **Controllers**: REST endpoints with proper HTTP status codes
  - **Services**: Business logic and transaction management
  - **Repositories**: Data access using Spring Data JPA
  - **Entities**: Data models with proper JPA annotations

### Frontend Architecture
- **Component-Based**: Focused, reusable React components
- **State Management**: React Context for global state (authentication, language)
- **Separation of Concerns**: UI logic separated from business logic
- **Responsive Design**: Mobile-first approach with CSS Grid/Flexbox

### Database Design
- **Internal Database**: H2 with JPA entities for user data, connections, queries
- **External Connections**: JDBC support for MySQL, PostgreSQL, MariaDB
- **Naming Conventions**: Follow database best practices
- **Schema Changes**: Document migrations and include proper constraints

## 🔒 Security Standards

**Current Implementation**: Enterprise-grade security with OWASP compliance

### Security Measures
- **Authentication**: JWT with refresh tokens, BCrypt password hashing
- **Encryption**: AES-256-GCM for database passwords and sensitive data  
- **SQL Security**: PreparedStatement usage prevents SQL injection attacks
- **Input Validation**: Comprehensive validation with proper error handling
- **User Isolation**: Complete separation of connections and queries by user ID
- **Security Testing**: Comprehensive test coverage for security features

### Security Guidelines
- Never commit credentials, API keys, or sensitive data
- Follow OWASP security guidelines and best practices
- Use parameterized queries exclusively for dynamic SQL
- Implement proper access controls and user authorization
- Regular security audits using OWASP dependency check

## 🔄 API Documentation

### Available Endpoints

#### Authentication API
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - Login (JWT token acquisition)  
- `POST /api/auth/refresh` - Refresh token renewal
- `POST /api/auth/logout` - Logout
- `GET /api/auth/me` - Current user information

#### Database Connection API
- `GET /api/connections` - List user connections
- `POST /api/connections` - Create new connection
- `PUT /api/connections/{id}` - Update connection
- `DELETE /api/connections/{id}` - Delete connection
- `POST /api/connections/{id}/test` - Test connection

#### SQL Execution API
- `POST /api/sql/execute` - Execute SQL query
- `POST /api/sql/validate` - Validate SQL syntax

#### Query Management API
- `GET /api/queries/saved` - List saved queries
- `POST /api/queries/save` - Save query
- `GET /api/queries/history` - Execution history

#### Schema Information API
- `GET /api/schema/{connectionId}` - Database schema metadata

#### System API
- `GET /api/health` - Application health check
- `GET /api/swagger-ui.html` - Interactive API documentation (OpenAPI/Swagger)
- `GET /actuator/*` - Spring Boot Actuator monitoring endpoints

### Interactive Documentation
- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **Health Checks**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/metrics`

## 🤝 Contribution Process

### Reporting Issues

Before creating bug reports, check existing issues to avoid duplicates. Include:
- **Clear description** of the issue
- **Reproduction steps** with specific details
- **Expected vs actual behavior**
- **Environment details** (OS, Java version, browser)
- **Screenshots** or error messages when applicable

### Suggesting Enhancements

Enhancement suggestions are welcome! Provide:
- **Clear description** of the proposed enhancement
- **Use case** or problem it addresses  
- **Proposed solution** with technical details
- **Additional context** or examples

### Pull Request Process

1. **Fork Repository**: Create your own fork of the project
2. **Create Feature Branch**: Branch from `main` with descriptive name
3. **Implement Changes**: Follow coding standards and architecture guidelines
4. **Add Tests**: Include unit/integration tests for new functionality
5. **Verify Quality**: Ensure all 356 tests pass and code meets standards
6. **Clear Commits**: Use conventional commit message format
7. **Submit PR**: Create pull request with comprehensive description

### Code Review Guidelines

- **Functionality**: Code works as intended and handles edge cases
- **Testing**: Adequate test coverage with both positive and negative scenarios
- **Security**: No security vulnerabilities or sensitive data exposure
- **Performance**: Efficient code that doesn't introduce performance regressions
- **Standards**: Follows project coding standards and conventions

## 🚢 Deployment & Production

### Environment Configuration
```bash
# Application settings
SERVER_PORT=8080
PROFILE=prod                    # dev, staging, prod

# JWT Configuration  
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400            # 24 hours
JWT_REFRESH_EXPIRATION=604800   # 7 days

# Internal Database (H2)
DB_MODE=file                    # memory, file, server
DB_PATH=/app/data/sqlapp2       # for file mode

# Monitoring (optional)
METRICS_ENABLED=true
PROMETHEUS_ENABLED=true
```

### Production Deployment Options

#### Standalone WAR Deployment
```bash
./gradlew build
java -jar build/libs/sqlapp2-1.0.0.war --server.port=8080
```

#### Docker Deployment (Recommended)
```bash
# Standard deployment
docker-compose up -d

# Production with monitoring stack
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

#### Environment-Specific Configuration
- **dev**: Development environment (H2 in-memory)
- **staging**: Staging environment (H2 file-based)
- **prod**: Production environment (H2 file + structured logging + metrics)

## 📊 Monitoring & Operations

### Spring Boot Actuator Endpoints
- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`  
- **Application Info**: `GET /actuator/info`
- **Prometheus**: `GET /actuator/prometheus`

### Monitoring Stack (Docker Compose)
- **Prometheus**: Metrics collection (`http://localhost:9090`)
- **Grafana**: Dashboards and visualization (`http://localhost:3000`)
- **AlertManager**: Alert routing and notification (`http://localhost:9093`)

### CI/CD Pipeline

**Current Implementation**: Comprehensive GitHub Actions automation

#### Continuous Integration Features
- **Automated Testing**: All 356 tests run on every PR
- **Security Scanning**: OWASP dependency check, Trivy container scanning
- **Quality Assurance**: SonarCloud integration, ESLint validation
- **Build Verification**: Backend and frontend build validation
- **Dependency Management**: Weekly automated dependency updates

#### Release Process
1. Features merged to `main` after code review
2. Automated comprehensive test execution
3. Security scans must pass before merge
4. Docker images built and tagged automatically
5. Semantic versioning for releases (v1.0.0)
6. Monitoring metrics updated automatically

## 🛠️ Troubleshooting

### Common Issues

#### Port Conflicts
```bash
# Check processes using ports
lsof -i :8080
lsof -i :5173

# Terminate processes if needed
kill -9 <PID>
```

#### NPM Dependency Issues
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

#### Gradle Build Issues
```bash
./gradlew clean build
```

#### Docker Permission Issues
```bash
sudo docker-compose up
```

### Log Analysis
- **Spring Boot**: Console output and `app.log`
- **React**: Browser Developer Tools console
- **Docker**: `docker-compose logs -f`

## 📚 Documentation Resources

### Project Documentation
- **[README.md](README.md)** - End-user guide and getting started
- **[ROADMAP.md](ROADMAP.md)** - Project progress and completion status  
- **[CLAUDE.md](CLAUDE.md)** - Technical architecture and development guidance

### External Resources
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [React 19 Documentation](https://react.dev)
- [Vite Documentation](https://vitejs.dev)
- [Docker Documentation](https://docs.docker.com)

## 🤔 Getting Help

- **Issues**: [GitHub Issues](https://github.com/YOUR_USERNAME/sqlapp2/issues)
- **Discussions**: [GitHub Discussions](https://github.com/YOUR_USERNAME/sqlapp2/discussions)  
- **API Documentation**: [Swagger UI](http://localhost:8080/api/swagger-ui.html)

## 📜 Code of Conduct

This project follows the [Contributor Covenant](https://www.contributor-covenant.org/) Code of Conduct. By participating, you agree to uphold this code and maintain a welcoming, inclusive environment.

## 🙏 Recognition

Contributors are recognized in:
- README.md acknowledgments
- Release notes and changelogs
- Project documentation

---

**Thank you for contributing to SqlApp2!** 🎉

Your contributions help make SqlApp2 a better tool for database professionals worldwide. Whether you're fixing bugs, adding features, improving documentation, or helping other users, every contribution is valuable and appreciated.