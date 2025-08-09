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

import cherry.sqlapp2.entity.SavedQuery;
import cherry.sqlapp2.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedQueryRepository extends JpaRepository<SavedQuery, Long> {

    List<SavedQuery> findByUserOrderByUpdatedAtDesc(User user);

    Page<SavedQuery> findByUserOrderByUpdatedAtDesc(User user, Pageable pageable);

    @Query("SELECT sq FROM SavedQuery sq WHERE sq.sharingScope = 'PUBLIC' ORDER BY sq.updatedAt DESC")
    List<SavedQuery> findPublicQueries();

    @Query("SELECT sq FROM SavedQuery sq WHERE sq.sharingScope = 'PUBLIC' ORDER BY sq.updatedAt DESC")
    Page<SavedQuery> findPublicQueries(Pageable pageable);

    @Query("SELECT sq FROM SavedQuery sq WHERE (sq.user = :user OR sq.sharingScope = 'PUBLIC') " +
           "AND (LOWER(sq.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(sq.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY sq.updatedAt DESC")
    Page<SavedQuery> searchAccessibleQueries(@Param("user") User user, 
                                           @Param("searchTerm") String searchTerm, 
                                           Pageable pageable);

    @Query("SELECT sq FROM SavedQuery sq WHERE sq.id = :queryId AND (sq.user = :user OR sq.sharingScope = 'PUBLIC')")
    Optional<SavedQuery> findAccessibleQuery(@Param("queryId") Long queryId, @Param("user") User user);

    boolean existsByUserAndName(User user, String name);

    @Query("SELECT COUNT(sq) FROM SavedQuery sq WHERE sq.user = :user")
    long countByUser(@Param("user") User user);

    @Query("SELECT COUNT(sq) FROM SavedQuery sq WHERE sq.sharingScope = 'PUBLIC'")
    long countPublicQueries();
}