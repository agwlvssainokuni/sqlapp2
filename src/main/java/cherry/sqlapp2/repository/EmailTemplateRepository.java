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

package cherry.sqlapp2.repository;

import cherry.sqlapp2.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    
    /**
     * テンプレートキーと言語でテンプレートを検索します。
     */
    Optional<EmailTemplate> findByTemplateKeyAndLanguage(String templateKey, String language);
    
    /**
     * テンプレートキーでテンプレート一覧を取得します。
     */
    List<EmailTemplate> findByTemplateKey(String templateKey);
    
    /**
     * すべてのテンプレートキーを取得します。
     */
    @Query("SELECT DISTINCT et.templateKey FROM EmailTemplate et ORDER BY et.templateKey")
    List<String> findAllTemplateKeys();
    
    /**
     * 指定されたテンプレートキーで利用可能な言語一覧を取得します。
     */
    @Query("SELECT et.language FROM EmailTemplate et WHERE et.templateKey = :templateKey ORDER BY et.language")
    List<String> findLanguagesByTemplateKey(@Param("templateKey") String templateKey);
    
    /**
     * テンプレートキーと言語の組み合わせが存在するかチェックします。
     */
    boolean existsByTemplateKeyAndLanguage(String templateKey, String language);
}