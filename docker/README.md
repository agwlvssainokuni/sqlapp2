# Development Database & Mail Server Environment

Docker-based development environment for SqlApp2 providing multiple database servers and mail server for unified management.

## Available Services

### Database Servers
- **MySQL 8.0**: `localhost:13306`
- **PostgreSQL 15**: `localhost:15432`
- **MariaDB 10.11**: `localhost:13307`
- **H2 Database**: `localhost:19092` (Server mode), `localhost:18082` (Web Console)

### Management Tools
- **phpMyAdmin**: `http://localhost:10080` (MySQL/MariaDB management)
- **pgAdmin**: `http://localhost:10081` (PostgreSQL management)

### Mail Server
- **MailPit**: `http://localhost:8025` (Web UI), `localhost:1025` (SMTP)

## Usage

```bash
# Navigate to docker directory
cd docker

# Start all services
docker-compose -f docker-compose.dev.yml up -d

# Start specific services only
docker-compose -f docker-compose.dev.yml up -d mysql postgres mailpit

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop services
docker-compose -f docker-compose.dev.yml down

# Complete removal including data
docker-compose -f docker-compose.dev.yml down -v
```

## Connection Settings

### MySQL
```
Host: localhost
Port: 13306
Database: sqlapp2_dev
User: sqlapp2_user
Password: sqlapp2_pass
Root Password: sqlapp2_root
```

### PostgreSQL
```
Host: localhost
Port: 15432
Database: sqlapp2_dev
User: sqlapp2_user
Password: sqlapp2_pass
```

### MariaDB
```
Host: localhost
Port: 13307
Database: sqlapp2_dev
User: sqlapp2_user
Password: sqlapp2_pass
Root Password: sqlapp2_root
```

### H2 Database (Server Mode)
```
JDBC URL: jdbc:h2:tcp://localhost:19092/~/sqlapp2_dev
User: sa
Password: (empty)
```

### MailPit (Development Mail)
```
SMTP Settings:
- Host: localhost
- Port: 1025
- Authentication: Not required

Web UI: http://localhost:8025
```

## SqlApp2 Connection Examples

Configure in the application's connection management page as follows:

### MySQL Connection
- **Database Type**: MySQL
- **Host**: localhost
- **Port**: 13306
- **Database Name**: sqlapp2_dev
- **Username**: sqlapp2_user
- **Password**: sqlapp2_pass

### PostgreSQL Connection
- **Database Type**: PostgreSQL
- **Host**: localhost
- **Port**: 15432
- **Database Name**: sqlapp2_dev
- **Username**: sqlapp2_user
- **Password**: sqlapp2_pass

### MariaDB Connection
- **Database Type**: MySQL (MariaDB uses MySQL driver)
- **Host**: localhost
- **Port**: 13307
- **Database Name**: sqlapp2_dev
- **Username**: sqlapp2_user
- **Password**: sqlapp2_pass

### H2 Connection
- **Database Type**: H2
- **JDBC URL**: jdbc:h2:tcp://localhost:19092/~/sqlapp2_dev
- **Username**: sa
- **Password**: (leave empty)

## Sample Data

Each database automatically includes the following sample tables and data:

- **users**: User information table
- **products**: Product information table
- **orders**: Order information table

Use the sample data to test various SQL features including JOINs, aggregate functions, WHERE clauses, and more.

## Management Tools Usage

### phpMyAdmin (MySQL/MariaDB)
1. Access `http://localhost:10080`
2. Select MySQL or MariaDB server from the server selection screen
3. Login with Username: `root`, Password: `sqlapp2_root`

### pgAdmin (PostgreSQL)
1. Access `http://localhost:10081`
2. Login with Email: `admin@sqlapp2.dev`, Password: `sqlapp2_admin`
3. Add server to connect to PostgreSQL

## Troubleshooting

### Port Conflicts
If ports conflict with other services, modify the port settings in `docker-compose.dev.yml`.

### Data Reset
To reset database data:
```bash
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d
```

### Log Inspection
Check logs for each service:
```bash
# All services logs
docker-compose -f docker-compose.dev.yml logs

# Specific service logs
docker-compose -f docker-compose.dev.yml logs mysql
docker-compose -f docker-compose.dev.yml logs postgres
```