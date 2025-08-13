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

package cherry.sqlapp2.dto;

public enum ExportFormat {
    CSV("csv", "text/csv", ","),
    TSV("tsv", "text/tab-separated-values", "\t");

    private final String fileExtension;
    private final String contentType;
    private final String delimiter;

    ExportFormat(String fileExtension, String contentType, String delimiter) {
        this.fileExtension = fileExtension;
        this.contentType = contentType;
        this.delimiter = delimiter;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getDelimiter() {
        return delimiter;
    }
}