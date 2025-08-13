/*
 * Copyright 2025 agwlvssainokuni
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

package cherry.sqlapp2.service;

import cherry.sqlapp2.dto.ExportFormat;
import cherry.sqlapp2.dto.SqlExecutionResult;
import cherry.sqlapp2.entity.DatabaseConnection;
import cherry.sqlapp2.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    @Autowired
    private SqlExecutionService sqlExecutionService;

    @Autowired
    private DatabaseConnectionService connectionService;

    /**
     * Export SQL execution result to CSV/TSV format
     */
    public ResponseEntity<byte[]> exportSqlResult(
            String sql,
            Map<String, Object> parameters,
            Long connectionId,
            ExportFormat format,
            String filename,
            User user
    ) {
        try {
            // Get database connection
            DatabaseConnection connection = connectionService.getConnectionById(user, connectionId);
            if (connection == null) {
                throw new RuntimeException("Database connection not found");
            }

            // Execute SQL query
            SqlExecutionResult result = sqlExecutionService.executeParameterizedSql(
                    sql, parameters, connection
            );

            // Generate export data
            String exportData = generateExportData(result, format);

            // Generate filename if not provided
            if (filename == null || filename.trim().isEmpty()) {
                filename = generateDefaultFilename(format);
            } else if (!filename.toLowerCase().endsWith("." + format.getFileExtension())) {
                filename += "." + format.getFileExtension();
            }

            // Prepare response
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, 
                    format.getContentType() + "; charset=UTF-8");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(exportData.getBytes("UTF-8"));

        } catch (Exception e) {
            logger.error("Error exporting SQL result", e);
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate CSV/TSV data from SQL execution result
     */
    private String generateExportData(SqlExecutionResult result, ExportFormat format) {
        StringWriter writer = new StringWriter();
        String delimiter = format.getDelimiter();

        try {
            // Write header row
            if (result.getData() != null && result.getData().getColumns() != null) {
                List<String> columnNames = result.getData().getColumns().stream()
                        .map(col -> col.getName())
                        .toList();
                
                writer.write(String.join(delimiter, 
                        columnNames.stream()
                                .map(this::escapeValue)
                                .toArray(String[]::new)));
                writer.write("\n");

                // Write data rows
                if (result.getData().getRows() != null) {
                    for (List<Object> row : result.getData().getRows()) {
                        String[] values = new String[row.size()];
                        for (int i = 0; i < row.size(); i++) {
                            Object value = row.get(i);
                            values[i] = escapeValue(value != null ? value.toString() : "");
                        }
                        writer.write(String.join(delimiter, values));
                        writer.write("\n");
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error generating export data", e);
            throw new RuntimeException("Failed to generate export data", e);
        }

        return writer.toString();
    }

    /**
     * Escape field values for CSV/TSV format
     */
    private String escapeValue(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains delimiter, quotes, or newlines, wrap in quotes
        if (value.contains(",") || value.contains("\t") || value.contains("\"") || value.contains("\n")) {
            // Escape existing quotes by doubling them
            String escaped = value.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        
        return value;
    }

    /**
     * Generate default filename with timestamp
     */
    private String generateDefaultFilename(ExportFormat format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "export_" + timestamp + "." + format.getFileExtension();
    }
}