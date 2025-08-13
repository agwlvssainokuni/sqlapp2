# SqlApp2 Monitoring System

This directory contains the complete monitoring stack configuration for SqlApp2, including metrics collection, visualization, and alerting.

## Overview

The monitoring system provides comprehensive observability for SqlApp2 using industry-standard tools:

- **Prometheus**: Metrics collection and storage
- **Grafana**: Data visualization and dashboards
- **AlertManager**: Alert handling and notifications
- **Node Exporter**: System metrics collection

## Architecture

```
SqlApp2 Application
    ↓ (metrics endpoint)
Prometheus Server
    ↓ (queries)
Grafana Dashboard ← AlertManager
    ↓ (notifications)
Email/Webhook Alerts
```

## Quick Start

### 1. Start Monitoring Stack

```bash
# Start SqlApp2 with monitoring stack
docker-compose -f docker-compose.monitoring.yml up -d

# Or start only monitoring services (requires SqlApp2 running separately)
docker-compose -f docker-compose.monitoring.yml up -d prometheus grafana alertmanager node-exporter
```

### 2. Access Interfaces

- **SqlApp2 Application**: http://localhost:8080
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin_password)
- **AlertManager**: http://localhost:9093

### 3. Configure Grafana

1. Login to Grafana (admin/admin_password)
2. Add Prometheus data source: http://prometheus:9090
3. Import the SqlApp2 dashboard from `grafana-dashboard.json`

## Metrics Collected

### Application Metrics

| Metric | Description | Type |
|--------|-------------|------|
| `sql_executions_total` | Total SQL executions | Counter |
| `sql_execution_errors_total` | SQL execution errors | Counter |
| `sql_execution_duration` | SQL execution time | Timer |
| `user_logins_total` | User login attempts | Counter |
| `user_registrations_total` | User registrations | Counter |
| `database_connections_total` | Database connection attempts | Counter |
| `database_connection_errors_total` | Database connection errors | Counter |
| `database_active_connections` | Active database connections | Gauge |
| `users_active_current` | Currently active users | Gauge |
| `sql_execution_total_rows` | Total rows returned | Gauge |

### System Metrics

- JVM memory usage (heap/non-heap)
- CPU usage and load
- HTTP request metrics
- Garbage collection statistics
- Thread pool statistics

### Custom Health Checks

- **Internal Database**: Connection validation and repository health
- **Application Metrics**: Service availability and performance thresholds  
- **System Resources**: Memory and CPU usage monitoring

## Alert Rules

### Critical Alerts
- Application down
- Critical memory usage (>95%)
- Database connection failures

### Warning Alerts  
- High SQL execution errors
- Slow SQL queries (>5s 95th percentile)
- High memory usage (>85%)
- High CPU usage (>80%)
- High database connections (>40)

### Info Alerts
- No user activity during business hours
- High user registration rate

## Configuration

### Environment Variables

Set these in your `.env` file for production:

```bash
# Monitoring Configuration
PROMETHEUS_RETENTION_DAYS=15
GRAFANA_ADMIN_PASSWORD=secure_password
ALERTMANAGER_SMTP_HOST=smtp.example.com
ALERTMANAGER_SMTP_USER=alerts@example.com  
ALERTMANAGER_SMTP_PASSWORD=smtp_password
```

### Customizing Alerts

Edit `alert_rules.yml` to modify alert thresholds:

```yaml
# Example: Change SQL error rate threshold
- alert: HighSQLExecutionErrors
  expr: rate(sql_execution_errors_total[5m]) > 0.1  # Adjust this value
  for: 3m
```

### Adding Custom Metrics

1. Add metric to `MetricsService.java`:
```java
private final Counter customCounter = Counter.builder("custom_metric_total")
    .description("Custom metric description")
    .register(meterRegistry);

public void recordCustomEvent() {
    customCounter.increment();
}
```

2. Use the metric in your service:
```java
@Autowired
private MetricsService metricsService;

public void someMethod() {
    metricsService.recordCustomEvent();
}
```

## Maintenance

### Data Retention

- **Prometheus**: 15 days (configurable)
- **Grafana**: Indefinite (dashboard configs and user data)
- **AlertManager**: 5 days for resolved alerts

### Backup

Important data to backup:
- Grafana configuration: `grafana_data` volume
- Prometheus data: `prometheus_data` volume  
- AlertManager data: `alertmanager_data` volume

### Log Rotation

Logs are automatically rotated by Docker. For custom log management:

```bash
# View container logs
docker-compose logs -f prometheus
docker-compose logs -f grafana
docker-compose logs -f alertmanager
```

## Troubleshooting

### Common Issues

1. **Metrics not appearing**: Check SqlApp2 actuator endpoints
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. **Alerts not firing**: Verify Prometheus can reach AlertManager
   ```bash
   curl http://localhost:9090/api/v1/alertmanagers
   ```

3. **Email alerts not sent**: Check AlertManager configuration and SMTP settings

### Health Checks

Monitor system health:
```bash
# Check all services status
docker-compose -f docker-compose.monitoring.yml ps

# Check specific service logs
docker-compose -f docker-compose.monitoring.yml logs prometheus
```

### Performance Tuning

For high-load environments:

1. **Prometheus**: Increase memory limits
2. **Grafana**: Enable caching and optimize query frequency
3. **SqlApp2**: Tune metrics collection intervals

## Security Considerations

- Grafana admin password should be changed from default
- Consider enabling HTTPS for production deployments
- Restrict network access to monitoring services
- Regularly update container images for security patches

## Integration with CI/CD

The monitoring stack integrates with the existing CI/CD pipeline:

1. **Health Checks**: CI/CD validates monitoring endpoints
2. **Deployment Monitoring**: Alerts trigger during deployments
3. **Performance Regression**: Metrics track deployment impact

For more details on the complete system architecture, see the main [CLAUDE.md](../CLAUDE.md) documentation.