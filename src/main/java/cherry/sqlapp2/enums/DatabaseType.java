/*
 * Copyright 2025 SqlApp2
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cherry.sqlapp2.enums;

import java.util.LinkedHashMap;
import java.util.Map;

public enum DatabaseType {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}?useInformationSchema=false", 3306),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://{host}:{port}/{database}", 5432),
    MARIADB("MariaDB", "org.mariadb.jdbc.Driver", "jdbc:mariadb://{host}:{port}/{database}", 3306);

    private final String displayName;
    private final String driverClassName;
    private final String urlTemplate;
    private final int defaultPort;

    DatabaseType(String displayName, String driverClassName, String urlTemplate, int defaultPort) {
        this.displayName = displayName;
        this.driverClassName = driverClassName;
        this.urlTemplate = urlTemplate;
        this.defaultPort = defaultPort;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String buildUrl(String host, int port, String database) {
        return urlTemplate
                .replace("{host}", host)
                .replace("{port}", String.valueOf(port))
                .replace("{database}", database);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name());
        map.put("displayName", displayName);
        map.put("defaultPort", defaultPort);
        return map;
    }

    public static DatabaseType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return DatabaseType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}