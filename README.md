# SqlApp2

**Enterprise-Grade Web-based SQL Execution Tool**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

SqlApp2 is a production-ready, secure web application that provides an intuitive interface for executing SQL queries against multiple database systems. Built with enterprise-grade security, comprehensive testing (361 tests with 100% success rate), and modern technology stack.

## ✨ Key Features

### 🔐 Enterprise Security
- **JWT Authentication** with refresh tokens and secure session management
- **AES-256-GCM Encryption** for database passwords and sensitive data
- **SQL Injection Protection** through PreparedStatement-based parameterized queries
- **OWASP Compliance** with comprehensive security testing and vulnerability scanning

### 🗄️ Multi-Database Support
- **MySQL, PostgreSQL, MariaDB** - Full compatibility with optimized drivers
- **Connection Management** - Secure storage with encrypted passwords
- **Real-time Connection Testing** - Verify connectivity before query execution
- **User Isolation** - Complete separation of database connections by user

### 📊 Advanced SQL Features
- **Direct SQL Execution** with sophisticated parameter processing
- **Visual Query Builder** - Drag-and-drop interface with schema-aware suggestions
- **Parameterized Queries** - Named parameter support (`:param`) with type conversion
- **Query Management** - Save, share, and organize SQL queries with history tracking
- **Schema Browsing** - Explore database tables, columns, and metadata

### 🌐 Modern User Experience
- **Internationalization** - Complete English and Japanese interface with real-time switching
- **Responsive Design** - Mobile-first approach optimized for all devices
- **Real-time Validation** - SQL syntax checking and parameter detection
- **Performance Tracking** - Query execution history with timing and analytics

## 🚀 Quick Start

### Option 1: Docker Deployment (Recommended)

```bash
git clone https://github.com/your-username/sqlapp2.git
cd sqlapp2
docker-compose up -d
```

Access the application at **http://localhost:8080**

### Option 2: Local Development

```bash
# Clone and build
git clone https://github.com/your-username/sqlapp2.git
cd sqlapp2
./gradlew build

# Run the application
java -jar build/libs/sqlapp2-1.0.0.war

# Access at http://localhost:8080
```

### Option 3: Development Mode

```bash
# Terminal 1: Backend (with hot reload)
./gradlew bootRun

# Terminal 2: Frontend (with hot reload)
cd frontend && npm install && npm run dev

# Backend: http://localhost:8080
# Frontend: http://localhost:5173
```

## 🛠️ Technology Stack

### Backend
- **Java 21** - Latest LTS with modern language features
- **Spring Boot 3.5.4** - Production-ready application framework
- **Spring Security + JWT** - Enterprise authentication and authorization
- **JPA/Hibernate** - Database abstraction with H2 internal database
- **Comprehensive Testing** - 361 tests (306 unit + 55 integration) with 100% success rate

### Frontend
- **React 19** - Modern component-based UI library
- **TypeScript** - Type-safe JavaScript development
- **Vite 7.1.1** - Fast development build tool
- **react-i18next v15.6.1** - Internationalization with 590+ translation keys
- **Responsive CSS3** - Mobile-first design with Grid/Flexbox

### Operations & Monitoring
- **Docker + Docker Compose** - Containerized deployment
- **GitHub Actions CI/CD** - Automated testing, security scanning, and deployment
- **OpenAPI/Swagger** - Interactive API documentation

## 📖 User Guide

### Getting Started
1. **Registration & Login**: Create an account and receive JWT authentication
2. **Database Setup**: Add your database connections with encrypted password storage
3. **SQL Execution**: Write and execute queries with parameter support
4. **Query Management**: Save frequently used queries and browse execution history
5. **Visual Builder**: Create queries using the drag-and-drop interface

### Key Workflows

**Parameterized Queries:**
```sql
SELECT * FROM users 
WHERE age > :minAge AND department = :dept AND created_date > :startDate
```
SqlApp2 automatically detects parameters and provides type-appropriate input fields.

**Visual Query Building:**
- Select tables and columns from schema browser
- Build conditions with visual operators
- Real-time SQL generation with validation
- One-click execution with result display

### Database Connections
Supports secure connections to:
- **MySQL** (5.7+)
- **PostgreSQL** (10+)
- **MariaDB** (10+)

Connection details are encrypted using AES-256-GCM and stored securely.

## 🔧 Configuration

### Environment Variables
```bash
# Application settings
SERVER_PORT=8080
PROFILE=prod                    # dev, staging, prod

# JWT Configuration
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400            # 24 hours
JWT_REFRESH_EXPIRATION=604800   # 7 days

# Database settings (internal H2)
DB_MODE=file                    # memory, file, server
DB_PATH=/app/data/sqlapp2       # for file mode

# Monitoring (optional)
METRICS_ENABLED=true
```

### Production Deployment
```bash
# Production deployment
docker-compose up -d

# Environment-specific configuration
export SPRING_PROFILES_ACTIVE=prod
java -jar sqlapp2-1.0.0.war
```

## 🧪 Quality Assurance

**Test Coverage**: 361 tests with 100% success rate
- **Unit Tests (306)**: Service layer, security components, utilities
- **Integration Tests (55)**: REST API endpoints, database integration
- **Security Testing**: JWT validation, encryption, SQL injection prevention
- **Performance Testing**: Query execution timing and optimization

**Security Standards**: OWASP compliant with regular vulnerability scanning
**Code Quality**: SonarCloud integration with comprehensive analysis

## 🔗 API Documentation

Interactive API documentation available at:
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **Health Checks**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics

## 🤝 Contributing

We welcome contributions! Please see our guides:
- **[Contributing Guide](CONTRIBUTING.md)** - Contribution process and coding standards
- **[Development Guide](DEVELOPMENT.md)** - Local setup and development workflows
- **[Technical Guide](CLAUDE.md)** - Architecture details and implementation guidelines

## 📚 Documentation

- **[ROADMAP.md](ROADMAP.md)** - Project progress and completion status
- **[DEVELOPMENT.md](DEVELOPMENT.md)** - Development environment setup
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contribution guidelines and standards
- **[CLAUDE.md](CLAUDE.md)** - Technical architecture and development guidance

## 🐛 Support

- **Issues**: [GitHub Issues](https://github.com/your-username/sqlapp2/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-username/sqlapp2/discussions)
- **API Documentation**: [Swagger UI](http://localhost:8080/api/swagger-ui.html)

## 📜 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🏆 Project Status

**Status**: ✅ **Production Ready** - Enterprise-grade SQL execution tool

**Key Achievements**:
- 🎯 Complete feature implementation with 33+ development phases
- 🧪 361 tests with 100% success rate ensuring reliability
- 🔒 Enterprise security with OWASP compliance and comprehensive encryption
- 🌐 Full internationalization support (English/Japanese)
- 📊 Advanced metrics collection and observability
- 🚀 Complete CI/CD automation with GitHub Actions

---

**SqlApp2** - Empowering database professionals with modern, secure, enterprise-grade SQL tools.

*Made with ❤️ using Java 21, Spring Boot 3.5.4, and React 19*