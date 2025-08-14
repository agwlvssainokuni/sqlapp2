# Contributing to SqlApp2

Thank you for your interest in contributing to SqlApp2! This guide will help you get started with contributing to our production-ready, enterprise-grade SQL execution tool.

## 📊 Project Status

**Current Status**: ✅ Production Ready - Enterprise-grade SQL execution tool
- **Test Coverage**: 356 tests (303 unit + 53 integration) - 100% success rate
- **Security**: OWASP compliant, JWT authentication, AES-256-GCM encryption
- **CI/CD**: Comprehensive GitHub Actions workflows with automated testing and security scanning
- **Monitoring**: Prometheus/Grafana integration with custom metrics and alerting

## 🤝 How to Contribute

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates. When creating a bug report, include:

- **Clear description** of the issue
- **Steps to reproduce** the problem
- **Expected behavior** vs actual behavior
- **Environment details** (OS, Java version, browser)
- **Screenshots** or error messages (if applicable)

### Suggesting Enhancements

Enhancement suggestions are welcome! Please provide:

- **Clear description** of the enhancement
- **Use case** or problem it solves
- **Proposed solution** (if you have one)
- **Additional context** or examples

### Pull Requests

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Add or update tests as needed
5. Ensure all tests pass
6. Follow the coding standards below
7. Commit with clear, descriptive messages
8. Push to your fork and submit a pull request

## 💻 Development Setup

### Prerequisites

- **Java 21** (OpenJDK)
- **Node.js 18+** 
- **Git**
- **Docker** (optional, for containerized development)
- **IDE**: IntelliJ IDEA, VS Code, or Eclipse with Java/TypeScript support

### Local Development

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/sqlapp2.git
cd sqlapp2

# Set up upstream remote
git remote add upstream https://github.com/ORIGINAL_OWNER/sqlapp2.git

# Backend development
./gradlew bootRun

# Frontend development (in another terminal)
cd frontend
npm install
npm run dev
```

### Running Tests

**Current Test Suite**: 356 tests with 100% success rate

```bash
# All backend tests (303 unit + 53 integration tests)
./gradlew test

# Frontend tests (with vitest)
cd frontend
npm test

# Build verification (includes all tests)
./gradlew build
```

## 📝 Coding Standards

### Java/Spring Boot

- Follow [Spring Boot best practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- Use modern Java 21 features (records, var, Stream API)
- All REST endpoints return `ApiResponse<T>` wrapper format
- DTO naming: No "Response" suffix (e.g., `LoginResult`, not `LoginResponse`)
- Controllers use explicit `Authentication` parameter injection
- Package structure: `cherry.sqlapp2.*`
- Comprehensive unit and integration tests with Japanese @DisplayName
- Follow naming conventions:
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`

### TypeScript/React

- Use functional components with React Hooks
- Page components end with "Page" suffix (e.g., `SqlExecutionPage.tsx`)
- Follow TypeScript strict mode (avoid `any`)
- API integration uses `.data` property from ApiResponse structure
- Internationalization with `useTranslation` hook from react-i18next
- Follow naming conventions:
  - Components: `PascalCase`
  - Variables/functions: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- CSS classes with kebab-case, responsive design approach

### Git Commit Messages

Use clear, descriptive commit messages:

```
feat: add user profile management
fix: resolve SQL injection vulnerability
docs: update API documentation
style: format code with prettier
refactor: improve connection pooling logic
test: add unit tests for authentication
chore: update dependencies
```

### Code Formatting

- **Java**: Use built-in IDE formatters or Checkstyle
- **TypeScript**: Use Prettier with provided configuration
- **CSS**: Use consistent indentation (2 spaces)

## 🧪 Testing Guidelines

### Backend Testing

- Unit tests for services and utilities
- Integration tests for controllers
- Use `@SpringBootTest` for integration tests
- Mock external dependencies
- Test both success and error scenarios

### Frontend Testing

- Component testing with React Testing Library
- Unit tests for utility functions
- Integration tests for user workflows
- Test accessibility features

### Test Coverage

**Current Achievement**: 356 tests with 100% success rate
- **Unit Tests (303)**: Service layer, utilities, security components
- **Integration Tests (53)**: REST API endpoints, database integration
- Focus on critical business logic and security features
- Include edge cases and comprehensive error handling
- Use @Nested classes with Japanese @DisplayName for clear test documentation

## 🏗️ Architecture Guidelines

### Backend Architecture

- Follow MVC pattern
- Use dependency injection
- Implement proper layering:
  - Controllers (REST endpoints)
  - Services (business logic)
  - Repositories (data access)
  - Entities (data models)

### Frontend Architecture

- Component-based architecture
- Use React Context for global state
- Keep components focused and reusable
- Separate UI logic from business logic

### Database Design

- Use JPA entities for internal H2 database
- Follow database naming conventions
- Include proper constraints and validations
- Document any schema changes

## 🔒 Security Considerations

**Current Security Implementation**: Enterprise-grade security with OWASP compliance

- **Authentication**: JWT with refresh tokens, BCrypt password hashing
- **Encryption**: AES-256-GCM for database passwords and sensitive data
- **SQL Security**: PreparedStatement usage prevents SQL injection
- **Input Validation**: Comprehensive validation with proper error handling
- **User Isolation**: Connection and query separation by user ID
- **Security Testing**: Comprehensive security test coverage
- Never hardcode credentials or secrets
- Follow OWASP security guidelines

## 📚 Documentation

- Update README.md for user-facing changes
- Include JavaDoc/JSDoc for new APIs
- Update ROADMAP.md for feature progress
- Add examples for complex features

## 🔄 CI/CD & Release Process

**Current CI/CD**: Comprehensive GitHub Actions automation

### Continuous Integration
- **Automated Testing**: All 356 tests run on every PR
- **Security Scanning**: OWASP dependency check, Trivy container scanning
- **Quality Assurance**: SonarCloud integration, ESLint validation
- **Build Verification**: Backend and frontend build validation

### Release Process
1. Features are merged to `main` branch after code review
2. GitHub Actions automatically run comprehensive test suites
3. Security scans must pass before merge
4. Docker images are built and tagged automatically
5. Version tags follow semantic versioning (v1.0.0)
6. Monitoring and metrics are updated automatically

## 🤔 Questions?

- Check existing [Issues](https://github.com/YOUR_USERNAME/sqlapp2/issues)
- Start a [Discussion](https://github.com/YOUR_USERNAME/sqlapp2/discussions)
- Review the [Documentation](docs/)

## 📜 Code of Conduct

This project follows the [Contributor Covenant](https://www.contributor-covenant.org/) Code of Conduct. By participating, you agree to uphold this code.

## 🙏 Recognition

Contributors will be recognized in:

- README.md acknowledgments
- Release notes
- Project documentation

Thank you for contributing to SqlApp2! 🎉