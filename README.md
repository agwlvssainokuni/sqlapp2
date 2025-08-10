# SqlApp2 🎉

**A Modern Web-based SQL Query Tool with Multi-RDBMS Support**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

SqlApp2 is a secure, modern web application that provides an intuitive interface for executing SQL queries against multiple database systems. Built with Spring Boot and React, it offers enterprise-grade security, multi-database support, and a responsive user experience.

## 🚀 Key Features

### 🔐 Security First
- **JWT Authentication**: Stateless token-based authentication system
- **Password Encryption**: AES-256-GCM encryption for database passwords
- **SQL Injection Protection**: PreparedStatement-based parameterized queries
- **Spring Security Integration**: Enterprise-grade security framework

### 🗄️ Multi-Database Support
- **MySQL** - Full support with optimized drivers
- **PostgreSQL** - Complete compatibility and features
- **MariaDB** - Native integration and performance

### 📊 SQL Execution Engine
- **Parameterized Queries**: Named parameter support (`:param`) with automatic type conversion
- **Real-time Validation**: SQL syntax checking before execution
- **Result Display**: Tabular data presentation with metadata
- **Performance Monitoring**: Execution time tracking and statistics

### 📚 Query Management System
- **Save & Share Queries**: Store frequently used SQL queries with parameter templates
- **Public/Private Sharing**: Share queries with team members or keep them private
- **Query History**: Automatic tracking of all SQL executions with performance metrics
- **Re-execution**: One-click re-run of previous queries with parameter restoration
- **Performance Analytics**: Execution time tracking, success rates, and usage statistics

### 🎨 Visual SQL Query Builder
- **Drag-and-Drop Interface**: Build SQL queries visually with intuitive components
- **Schema-Aware Selection**: Automatic table and column suggestions from database schema
- **Real-time SQL Generation**: Live preview of generated SQL as you build
- **Comprehensive Clause Support**: SELECT, FROM, WHERE, JOIN, GROUP BY, HAVING, ORDER BY, LIMIT
- **Advanced Functions**: Aggregate functions, conditional operators, aliases, and DISTINCT
- **Parameter Integration**: Seamless parameter detection and value input

### 🎨 Modern User Interface
- **Responsive Design**: Mobile-first approach with CSS Grid/Flexbox
- **React + TypeScript**: Type-safe component architecture
- **Real-time Updates**: Dynamic parameter detection and form generation
- **Connection Management**: Visual database connection testing and management

### 🐳 Deployment Ready
- **Single WAR Deployment**: Integrated frontend and backend
- **Docker Support**: Containerized deployment with Docker Compose
- **Environment Configuration**: Twelve-Factor App compliance
- **H2 Internal Database**: Zero-configuration development setup

## 📸 Screenshots

### Dashboard
![Dashboard](docs/images/dashboard.png)
*Main dashboard with feature overview and navigation*

### Connection Management
![Connection Management](docs/images/connections.png)
*Database connection creation and testing interface*

### SQL Execution
![SQL Execution](docs/images/sql-execution.png)
*Interactive SQL query execution with parameterized query support*

## 🛠️ Technology Stack

### Backend
- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 3.5.4** - Production-ready framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database abstraction layer
- **H2 Database** - Internal data storage
- **Gradle 9.0.0** - Build automation

### Frontend
- **React 18** - Modern component-based UI library
- **TypeScript** - Type-safe JavaScript development
- **Vite 7.1.1** - Fast development build tool
- **React Router** - Client-side routing
- **CSS3** - Modern styling with Grid/Flexbox

### Database Drivers
- **MySQL Connector/J** - MySQL database connectivity
- **PostgreSQL JDBC Driver** - PostgreSQL integration
- **MariaDB Connector/J** - MariaDB support

## 🚀 Quick Start

### Prerequisites
- Java 21 or later
- Node.js 18 or later
- Docker (optional)

### Option 1: Docker Deployment (Recommended)

```bash
git clone https://github.com/your-username/sqlapp2.git
cd sqlapp2
docker-compose up -d
```

Access the application at http://localhost:8080

### Option 2: Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/sqlapp2.git
   cd sqlapp2
   ```

2. **Build and run**
   ```bash
   ./gradlew build
   java -jar build/libs/sqlapp2-1.0.0.war
   ```

3. **Access the application**
   - Open http://localhost:8080 in your browser
   - Create an account or login
   - Add your database connections
   - Start executing SQL queries!

### Option 3: Development Mode

For active development with hot reload:

```bash
# Terminal 1: Start backend
./gradlew bootRun

# Terminal 2: Start frontend
cd frontend
npm install
npm run dev
```

- Backend: http://localhost:8080
- Frontend: http://localhost:5173 (proxies to backend)

## 📖 User Guide

### 1. User Registration and Login
1. Navigate to http://localhost:8080
2. Click "Register" to create a new account
3. Enter your username, email, and password
4. Login with your credentials to receive a JWT token

### 2. Database Connection Setup
1. Click "Manage Connections" from the dashboard
2. Click "Add New Connection"
3. Fill in connection details:
   - **Connection Name**: Friendly name for your connection
   - **Database Type**: MySQL, PostgreSQL, or MariaDB
   - **Host/Port**: Database server location
   - **Database Name**: Target database
   - **Username/Password**: Database credentials
4. Click "Test Connection" to verify connectivity
5. Save the connection for future use

### 3. SQL Query Execution
1. Click "Go to SQL Execution" from the dashboard
2. Select your database connection
3. Enter your SQL query:
   ```sql
   SELECT * FROM users WHERE age > :minAge AND status = :status
   ```
4. SqlApp2 automatically detects parameters (`:minAge`, `:status`)
5. Fill in parameter values with appropriate data types
6. Click "Execute Query" to run your SQL
7. View results in an interactive table format

### 4. Advanced Features
- **Parameter Types**: Supports string, int, long, double, boolean, date, time, datetime
- **Query Validation**: Real-time SQL syntax checking
- **Connection Testing**: Verify database connectivity before query execution
- **Responsive Design**: Works seamlessly on desktop and mobile devices

## 🔧 Configuration

### Environment Variables

```bash
# Database configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=myapp
DB_USER=username
DB_PASSWORD=password

# JWT configuration
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400

# Server configuration
SERVER_PORT=8080
```

### Application Properties

```properties
# H2 Database (Internal)
spring.datasource.url=jdbc:h2:mem:sqlapp2db
spring.h2.console.enabled=true

# JPA Configuration
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Security
app.jwt.secret=${JWT_SECRET:defaultSecretKey}
app.jwt.expiration=${JWT_EXPIRATION:86400}
```

## 🧪 Testing

### Run Backend Tests
```bash
./gradlew test
```

### Run Frontend Tests
```bash
cd frontend
npm test
```

### Integration Testing
```bash
# Start the application
./gradlew bootRun

# Run integration tests
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'
```

## 📁 Project Structure

```
sqlapp2/
├── src/main/java/cherry/sqlapp2/           # Spring Boot backend
│   ├── controller/                         # REST API controllers
│   ├── service/                           # Business logic services
│   ├── entity/                            # JPA entities
│   ├── repository/                        # Data access layer
│   ├── dto/                              # Data transfer objects
│   ├── config/                           # Configuration classes
│   └── security/                         # Security components
├── src/main/resources/
│   ├── application.properties            # Backend configuration
│   └── static/                          # Built frontend assets (auto-generated)
├── frontend/                            # React application
│   ├── src/
│   │   ├── components/                  # React components
│   │   ├── context/                     # React context providers
│   │   └── utils/                       # Utility functions
│   ├── package.json                     # Frontend dependencies
│   └── vite.config.ts                   # Vite configuration
├── build.gradle                         # Gradle build configuration
├── Dockerfile                           # Docker image definition
├── docker-compose.yml                   # Docker Compose configuration
└── README.md                           # This file
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes following our coding standards
4. Add tests for new functionality
5. Commit your changes: `git commit -m 'Add amazing feature'`
6. Push to the branch: `git push origin feature/amazing-feature`
7. Open a Pull Request

### Code Style

- **Java**: Follow Spring Boot conventions and best practices
- **TypeScript/React**: Use functional components with hooks
- **Documentation**: Include JSDoc/JavaDoc for public APIs
- **Testing**: Maintain high test coverage for new features

## 🐛 Troubleshooting

### Common Issues

**Connection refused errors:**
- Ensure your database server is running
- Verify connection parameters (host, port, credentials)
- Check firewall settings

**JWT token expired:**
- Login again to refresh your authentication token
- Check if system clock is synchronized

**Build failures:**
- Ensure Java 21 and Node.js 18+ are installed
- Clear Gradle cache: `./gradlew clean`
- Clear npm cache: `npm cache clean --force`

**H2 Console Access (Development):**
- Navigate to http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:sqlapp2db`
- Username: `sa`, Password: (empty)

### Support

- 📚 [Documentation](docs/)
- 🐛 [Issue Tracker](https://github.com/your-username/sqlapp2/issues)
- 💬 [Discussions](https://github.com/your-username/sqlapp2/discussions)

## 📜 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) - Enterprise Java framework
- [React](https://reactjs.org/) - UI component library
- [Vite](https://vitejs.dev/) - Fast build tool
- [H2 Database](https://www.h2database.com/) - Embedded database
- [JWT.io](https://jwt.io/) - JSON Web Tokens

## 🎯 Roadmap

### ✅ Phase 1-2: MVP (Completed)
- [x] User authentication with JWT
- [x] Multi-database connection management
- [x] SQL execution with parameterized queries
- [x] Schema information retrieval and browsing
- [x] Modern React frontend
- [x] Docker deployment support

### ✅ Phase 3: Advanced Features (Completed)
#### Phase 3.1: Query Management
- [x] Save and manage SQL queries with parameter templates
- [x] Public/private query sharing with access control
- [x] Automatic execution history tracking with performance metrics
- [x] Query statistics dashboard and analytics
- [x] One-click query re-execution with parameter restoration
- [x] Complete UI/UX implementation with responsive design

#### Phase 3.2: Visual SQL Query Builder
- [x] Comprehensive QueryStructure backend implementation
- [x] SQL generation service with validation and formatting
- [x] REST API endpoints for query building and suggestions
- [x] Visual query builder frontend interface
- [x] Schema-aware table and column selection
- [x] Real-time SQL generation and validation
- [x] Support for all SQL clauses and advanced functions
- [x] Responsive design with mobile support

#### Phase 3.3: UI/UX Improvements & Issue Resolution
- [x] Fixed query execution history display issues
- [x] Enhanced saved query execution count tracking
- [x] Visual query type indicators in history (saved vs direct input)
- [x] Execution mode display labels for SQL execution screen
- [x] Intelligent re-execute logic for saved queries vs history
- [x] Proper saved query ID tracking in execution history
- [x] Comprehensive parameter handling and JSON parsing

### 🔄 Phase 4+: Enterprise Features (Optional)
- [ ] Advanced JOIN clause builder with visual relationship mapping
- [ ] Query performance optimization suggestions
- [ ] Advanced result export (CSV, JSON, Excel)
- [ ] Query templates and snippet management

### 🚀 Phase 5: Enterprise Features (Optional)
- [ ] User role management and permissions
- [ ] Audit logging and compliance features
- [ ] Advanced performance analytics dashboard
- [ ] AI-powered query optimization suggestions

---

**Made with ❤️ by the SqlApp2 team**

*SqlApp2 - Empowering database professionals with modern, secure SQL tools.*