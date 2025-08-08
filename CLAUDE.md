# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SQLapp2 is a web-based SQL execution tool that provides a user interface for executing SQL queries against various RDBMSs.

### Technology Stack
- **Frontend**: React (SPA - Single Page Application)
- **Backend**: Java (Spring Boot)
- **Architecture**: Client-Server with REST APIs
- **Internal Database**: H2 Database (with JPA/Hibernate)
- **Data Access**: Spring Data JPA
- **Authentication**: Spring Security
- **Password Security**: BCrypt hashing
- **Target RDBMS**: MySQL, PostgreSQL, MariaDB (via JDBC)
- **Deployment**: Executable WAR, Container-ready (Docker)

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
- **Deployment Options**:
  - In-memory mode (for testing/development)
  - Local file storage (for standalone deployment)  
  - Server mode (for multi-user environments)
- **Configuration**: Configurable via Spring profiles

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
   FROM openjdk:17-jre-slim
   COPY sqlapp2.war /app.war
   EXPOSE 8080
   ENTRYPOINT ["java", "-jar", "/app.war"]
   ```

3. **Environment Configuration**
   - Database connections via environment variables
   - External JDBC driver path configuration
   - H2 storage mode selection (memory/file/server)
   - Logging levels and output destinations

## Development Setup

*To be established once project structure is created*

## Project Structure

*To be established based on Spring Boot + React architecture*