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

package cherry.sqlapp2.controller;

import cherry.sqlapp2.dto.SqlParseResult;
import cherry.sqlapp2.service.SqlReverseEngineeringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("QueryBuilderController統合テスト")
class QueryBuilderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SqlReverseEngineeringService sqlReverseEngineeringService;

    @Test
    @WithMockUser
    @DisplayName("SQLパース - 成功ケース")
    void parseSQL_Success() throws Exception {
        // Mock service response
        SqlParseResult mockResult = SqlParseResult.success(null);
        when(sqlReverseEngineeringService.parseSQL(anyString())).thenReturn(mockResult);

        String requestJson = "{\"sql\": \"SELECT * FROM users\"}";

        mockMvc.perform(post("/api/query-builder/parse")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("SQLパース - エラーケース")
    void parseSQL_Error() throws Exception {
        // Mock service response
        SqlParseResult mockResult = SqlParseResult.error("Invalid SQL syntax");
        when(sqlReverseEngineeringService.parseSQL(anyString())).thenReturn(mockResult);

        String requestJson = "{\"sql\": \"INVALID SQL\"}";

        mockMvc.perform(post("/api/query-builder/parse")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.errorMessage").value("Invalid SQL syntax"));
    }
}