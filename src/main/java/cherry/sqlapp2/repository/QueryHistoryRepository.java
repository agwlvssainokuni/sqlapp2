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

import cherry.sqlapp2.entity.QueryHistory;
import cherry.sqlapp2.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {

    Page<QueryHistory> findByUserOrderByExecutedAtDesc(User user, Pageable pageable);

    List<QueryHistory> findTop10ByUserOrderByExecutedAtDesc(User user);

    Optional<QueryHistory> findByIdAndUser(Long id, User user);

    @Query("SELECT qh FROM QueryHistory qh WHERE qh.user = :user AND qh.executedAt >= :fromDate ORDER BY qh.executedAt DESC")
    Page<QueryHistory> findByUserAndExecutedAtAfter(@Param("user") User user, 
                                                   @Param("fromDate") LocalDateTime fromDate, 
                                                   Pageable pageable);

    @Query("SELECT qh FROM QueryHistory qh WHERE qh.user = :user AND qh.executedAt >= :fromDate AND qh.executedAt <= :toDate ORDER BY qh.executedAt DESC")
    Page<QueryHistory> findByUserAndExecutedAtBetween(@Param("user") User user, 
                                                     @Param("fromDate") LocalDateTime fromDate, 
                                                     @Param("toDate") LocalDateTime toDate, 
                                                     Pageable pageable);

    @Query("SELECT qh FROM QueryHistory qh WHERE qh.user = :user AND qh.isSuccessful = :successful ORDER BY qh.executedAt DESC")
    Page<QueryHistory> findByUserAndIsSuccessful(@Param("user") User user, 
                                                @Param("successful") Boolean successful, 
                                                Pageable pageable);

    @Query("SELECT qh FROM QueryHistory qh WHERE qh.user = :user AND qh.isSuccessful = :successful AND qh.executedAt >= :fromDate ORDER BY qh.executedAt DESC")
    Page<QueryHistory> findByUserAndIsSuccessfulAndExecutedAtAfter(@Param("user") User user, 
                                                                  @Param("successful") Boolean successful,
                                                                  @Param("fromDate") LocalDateTime fromDate, 
                                                                  Pageable pageable);

    @Query("SELECT qh FROM QueryHistory qh WHERE qh.user = :user AND qh.isSuccessful = :successful AND qh.executedAt >= :fromDate AND qh.executedAt <= :toDate ORDER BY qh.executedAt DESC")
    Page<QueryHistory> findByUserAndIsSuccessfulAndExecutedAtBetween(@Param("user") User user, 
                                                                    @Param("successful") Boolean successful,
                                                                    @Param("fromDate") LocalDateTime fromDate, 
                                                                    @Param("toDate") LocalDateTime toDate, 
                                                                    Pageable pageable);

    @Query("SELECT qh FROM QueryHistory qh WHERE qh.user = :user " +
           "AND LOWER(qh.sqlContent) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY qh.executedAt DESC")
    Page<QueryHistory> searchByUserAndSqlContent(@Param("user") User user, 
                                                @Param("searchTerm") String searchTerm, 
                                                Pageable pageable);

    @Query("SELECT COUNT(qh) FROM QueryHistory qh WHERE qh.user = :user")
    long countByUser(@Param("user") User user);

    @Query("SELECT AVG(qh.executionTimeMs) FROM QueryHistory qh WHERE qh.user = :user AND qh.isSuccessful = true")
    Double findAverageExecutionTimeByUser(@Param("user") User user);

    @Query("SELECT COUNT(qh) FROM QueryHistory qh WHERE qh.user = :user AND qh.isSuccessful = false")
    long countFailedQueriesByUser(@Param("user") User user);

    @Query("SELECT COUNT(qh) FROM QueryHistory qh WHERE qh.user = :user AND qh.executedAt >= :fromDate")
    long countByUserAndExecutedAtAfter(@Param("user") User user, @Param("fromDate") LocalDateTime fromDate);

    void deleteByUserAndExecutedAtBefore(User user, LocalDateTime cutoffDate);
}