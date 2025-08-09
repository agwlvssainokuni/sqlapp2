# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SqlApp2 is a web-based SQL execution tool that provides a user interface for executing SQL queries against various RDBMSs.

### Technology Stack
- **Frontend**: React 18 + TypeScript (SPA - Single Page Application)
  - **Build Tool**: Vite 7.1.1
  - **Routing**: React Router DOM
  - **Styling**: CSS3 with component-based approach
- **Backend**: Java 21 + Spring Boot 3.5.4
  - **Build Tool**: Gradle 9.0.0
  - **Web**: Spring Web MVC
  - **Security**: Spring Security with BCrypt
- **Architecture**: Client-Server with REST APIs, integrated SPA deployment
- **Internal Database**: H2 Database (with JPA/Hibernate)
- **Data Access**: Spring Data JPA
- **Authentication**: Spring Security + JWT (planned)
- **Password Security**: BCrypt hashing
- **Target RDBMS**: MySQL, PostgreSQL, MariaDB (via JDBC)
- **Deployment**: Executable WAR, Container-ready (Docker + Docker Compose)

### Core Features

1. **User Authentication**
   - User registration and login functionality
   - Password hashing using BCrypt
   - Session management with Spring Security
   - User data stored in internal H2 database

2. **Database Connectivity**
   - Support for multiple RDBMS types (configurable)
   - Primary supported RDBMS: MySQL, PostgreSQL, MariaDB
   - External JDBC driver support (JAR files)
   - Connection switching capabilities
   - Per-user connection configurations

3. **SQL Execution**
   - Direct SQL input (full query)
   - Component-based SQL builder:
     - SELECT clause
     - FROM clause  
     - WHERE clause
     - GROUP BY clause
     - HAVING clause
     - ORDER BY clause
   - Parameterized queries with placeholder support:
     - Named parameters (e.g., `:userId`, `:startDate`)
     - Positional parameters (e.g., `?1`, `?2`)
     - Parameter value input interface at execution time
     - PreparedStatement-based secure execution

4. **Query Management**
   - Save executed SQL queries with parameter definitions to internal database
   - User-defined query names for saved queries
   - User-specific query history and re-execution with parameter values
   - Query result display
   - Parameter template management for reusable queries
   - Query sharing scope control:
     - Private (owner only access)
     - Public (accessible by all users)
   - Browse and execute shared public queries

5. **Execution History**
   - Record query execution metadata:
     - Execution timestamp
     - Executing user
     - Result record count
     - Execution duration (elapsed time)
   - Query execution log with performance tracking
   - User-specific execution history browsing

6. **Schema Support**
   - Read RDBMS schema information (tables, columns, data types)
   - SQL input auto-completion based on schema metadata
   - Table schema definition display (column names, data types, constraints)
   - Query building assistance based on available schema

### Internal Database (H2)
- **Purpose**: 
  - User authentication data (usernames, hashed passwords)
  - User-specific database connection configurations
  - Executed query history and metadata
  - Query management and re-execution data with sharing scope
  - Query execution history (timestamp, user, record count, duration)
- **Access Layer**: Spring Data JPA with Hibernate
- **Current Implementation**: In-memory database for development
- **Deployment Options**:
  - In-memory mode (for testing/development) ✅ **Implemented**
  - Local file storage (for standalone deployment) ✅ **Docker Ready**
  - Server mode (for multi-user environments) 🔄 **Planned**
- **Configuration**: Configurable via Spring profiles and environment variables
- **Development Access**: H2 Console available at `/h2-console` (development only)

## Deployment Architecture

### Twelve-Factor App Compliance
- **Codebase**: Single codebase tracked in Git
- **Dependencies**: Explicit dependency management via Maven/Gradle
- **Config**: Environment-based configuration (no hardcoded values)
- **Backing services**: External RDBMS connections as attached resources
- **Build/Release/Run**: Clear separation of build and runtime stages
- **Processes**: Stateless application processes
- **Port binding**: Self-contained service with embedded server
- **Concurrency**: Horizontal scaling support
- **Disposability**: Fast startup and graceful shutdown
- **Dev/Prod parity**: Environment consistency
- **Logs**: Logs as event streams to stdout
- **Admin processes**: Administrative tasks via separate processes

### Deployment Options
1. **Command Line Execution**
   ```bash
   java -jar sqlapp2.war --server.port=8080
   ```

2. **Container Deployment**
   ```dockerfile
   FROM openjdk:21-jre-slim
   COPY build/libs/sqlapp2.war app.war
   EXPOSE 8080
   ENTRYPOINT ["java", "-jar", "app.war"]
   ```

   ```yaml
   # docker-compose.yml
   services:
     sqlapp2:
       build: .
       ports:
         - "8080:8080"
       volumes:
         - sqlapp2_data:/app/data
   ```

3. **Environment Configuration**
   - Database connections via environment variables
   - External JDBC driver path configuration
   - H2 storage mode selection (memory/file/server)
   - Logging levels and output destinations

## Development Setup

### Prerequisites
- Java 21 (OpenJDK)
- Node.js 18+
- Git
- Docker (optional)

### Quick Start

1. **Backend Development**:
   ```bash
   ./gradlew bootRun
   # Runs on http://localhost:8080
   ```

2. **Frontend Development**:
   ```bash
   cd frontend
   npm install
   npm run dev
   # Runs on http://localhost:5173 with proxy to backend
   ```

3. **Integrated Build**:
   ```bash
   ./gradlew build
   java -jar build/libs/sqlapp2-1.0.0.war
   ```

4. **Docker Deployment**:
   ```bash
   docker-compose up -d
   ```

See `DEVELOPMENT.md` for detailed setup instructions.

## Project Structure

```
sqlapp2/
├── src/main/java/cherry/sqlapp2/    # Spring Boot application
│   ├── SqlApp2Application.java      # Main application class
│   ├── config/                      # Configuration classes
│   │   └── SecurityConfig.java      # Spring Security config
│   ├── controller/                  # REST controllers
│   │   ├── AuthController.java      # Authentication endpoints
│   │   ├── HealthController.java    # Health check endpoint
│   │   └── SpaController.java       # SPA routing controller
│   ├── dto/                         # Data Transfer Objects
│   ├── entity/                      # JPA entities
│   ├── repository/                  # Spring Data repositories
│   └── service/                     # Business logic services
├── src/main/resources/
│   ├── application.properties       # Application configuration
│   └── static/                      # Built frontend assets (auto-generated)
├── frontend/                        # React application
│   ├── src/
│   │   ├── components/              # React components
│   │   │   ├── Login.tsx           # Login component
│   │   │   ├── Register.tsx        # Registration component
│   │   │   ├── Dashboard.tsx       # Main dashboard
│   │   │   └── ProtectedRoute.tsx  # Authentication guard
│   │   ├── context/
│   │   │   └── AuthContext.tsx     # Authentication context
│   │   ├── App.tsx                 # Main React app
│   │   └── main.tsx               # React entry point
│   ├── package.json               # Frontend dependencies
│   └── vite.config.ts            # Vite configuration
├── build.gradle                   # Gradle build configuration
├── Dockerfile                     # Docker image definition
├── docker-compose.yml            # Docker Compose configuration
├── DEVELOPMENT.md                # Development guide
├── ROADMAP.md                   # Project roadmap and progress
└── CLAUDE.md                    # This file
```

## Current Implementation Status

### ✅ Completed (Phase 1-2 MVP - 100%)

1. **Backend Infrastructure**:
   - Spring Boot 3.5.4 + Java 21 setup
   - H2 database with JPA/Hibernate
   - Spring Security with BCrypt password encoding
   - REST API endpoints for authentication and health checks
   - User entity and repository layer

2. **Frontend Infrastructure**:
   - Vite + React 18 + TypeScript project
   - React Router for client-side routing
   - Authentication context and protected routes
   - Login, Register, and Dashboard components
   - API integration with proxy configuration

3. **Deployment Infrastructure**:
   - Integrated build process (frontend → backend static resources)
   - WAR packaging for standalone deployment
   - Docker containerization with multi-stage build
   - Docker Compose for development and production
   - Development environment documentation

4. **Authentication System (Phase 2.1)**:
   - JWT-based authentication with secure token generation
   - CustomUserDetailsService implementation
   - JwtAuthenticationFilter for stateless authentication
   - Frontend JWT integration with automatic token handling
   - Authentication state management and protected API calls

5. **Database Connection Management (Phase 2.2)**:
   - Multi-RDBMS support (MySQL, PostgreSQL, MariaDB)
   - Encrypted password storage using AES-256-GCM
   - Database connection CRUD operations
   - Connection testing and validation
   - Dynamic database connection management
   - User-specific connection isolation

6. **SQL Execution Engine (Phase 2.3)**:
   - Secure SQL execution with PreparedStatement
   - Parameterized query support with named parameters
   - Comprehensive parameter type conversion
   - SQL validation and injection prevention
   - Result set display with metadata
   - Performance monitoring and execution time tracking
   - Memory protection with result row limits

7. **Schema Information System (Phase 2.4)**:
   - Database schema metadata retrieval
   - Table and column information display
   - Data type and constraint information
   - Schema browsing interface

8. **Modern User Interface**:
   - Responsive React components for all features
   - Connection management interface
   - SQL execution interface with parameter detection
   - Schema browsing interface
   - Real-time connection testing
   - Form validation and error handling

9. **Query Management System (Phase 3.1 - Complete)**:
   - Saved query CRUD operations with sharing scope control
   - Automatic query execution history tracking with performance metrics
   - Parameter definitions and template management
   - Public/private query sharing with access control
   - Query performance statistics and execution analytics
   - Comprehensive search and filtering capabilities
   - Complete UI/UX implementation with responsive design
   - Real-time statistics dashboard and re-execution functionality

### 🔄 Next Phase (Phase 3.2 - SQL Builder)

Advanced SQL builder interface:
- Visual query construction with drag-and-drop
- Component-based SQL clause building
- Schema-aware table and column selection

## Code Style Guidelines

### Java
- Package structure: `cherry.sqlapp2.*`
- Apache License header required on all files
- Spring Boot conventions and best practices
- Comprehensive validation and error handling

### TypeScript/React
- Functional components with React Hooks
- TypeScript strict mode with proper type definitions
- Semicolon-omitted style (following Vite defaults)
- Apache License header required on all files
- CSS-in-JS or modular CSS approach