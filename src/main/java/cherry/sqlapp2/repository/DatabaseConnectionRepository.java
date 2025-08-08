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
package cherry.sqlapp2.repository;

import cherry.sqlapp2.entity.DatabaseConnection;
import cherry.sqlapp2.entity.User;
import cherry.sqlapp2.enums.DatabaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, Long> {

    List<DatabaseConnection> findByUserOrderByUpdatedAtDesc(User user);

    List<DatabaseConnection> findByUserAndIsActiveOrderByUpdatedAtDesc(User user, boolean isActive);

    Optional<DatabaseConnection> findByUserAndConnectionName(User user, String connectionName);

    boolean existsByUserAndConnectionName(User user, String connectionName);

    List<DatabaseConnection> findByUserAndDatabaseType(User user, DatabaseType databaseType);

    @Query("SELECT COUNT(dc) FROM DatabaseConnection dc WHERE dc.user = :user AND dc.isActive = true")
    long countActiveConnectionsByUser(@Param("user") User user);

    @Query("SELECT dc FROM DatabaseConnection dc WHERE dc.user = :user AND dc.id = :id")
    Optional<DatabaseConnection> findByUserAndId(@Param("user") User user, @Param("id") Long id);

    @Query("SELECT dc FROM DatabaseConnection dc WHERE dc.user = :user AND dc.isActive = true ORDER BY dc.updatedAt DESC")
    List<DatabaseConnection> findActiveConnectionsByUser(@Param("user") User user);
}