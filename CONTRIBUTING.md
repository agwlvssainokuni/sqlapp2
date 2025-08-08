# Contributing to SqlApp2

Thank you for your interest in contributing to SqlApp2! This guide will help you get started with contributing to our web-based SQL query tool.

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

- Java 21 or later
- Node.js 18 or later
- Git

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

```bash
# Backend tests
./gradlew test

# Frontend tests
cd frontend
npm test

# Integration tests
./gradlew integrationTest
```

## 📝 Coding Standards

### Java/Spring Boot

- Follow [Spring Boot best practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- Use proper exception handling
- Include JavaDoc for public methods
- Follow naming conventions:
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- Package structure: `cherry.sqlapp2.*`

### TypeScript/React

- Use functional components with React Hooks
- Follow TypeScript strict mode
- Use proper typing (avoid `any`)
- Follow naming conventions:
  - Components: `PascalCase`
  - Variables/functions: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- Use CSS classes with kebab-case

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

- Maintain minimum 80% test coverage
- Focus on critical business logic
- Include edge cases and error handling

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

- Never hardcode credentials or secrets
- Use parameterized queries for all database operations
- Validate all user inputs
- Follow OWASP security guidelines
- Include security tests for critical features

## 📚 Documentation

- Update README.md for user-facing changes
- Include JavaDoc/JSDoc for new APIs
- Update ROADMAP.md for feature progress
- Add examples for complex features

## 🔄 Release Process

1. Features are merged to `main` branch
2. Version tags follow semantic versioning (v1.0.0)
3. Releases include changelog and migration notes
4. Docker images are built automatically

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