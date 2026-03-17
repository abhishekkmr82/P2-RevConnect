package com.rev.app.repository;

import com.rev.app.entity.User;
import com.rev.app.dto.UserSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>,
                JpaSpecificationExecutor<User> {

        @Query("SELECT u FROM User u WHERE u.username = :username")
        Optional<User> findByUsername(@Param("username") String username);

        @Query("SELECT u FROM User u WHERE u.email = :email")
        Optional<User> findByEmail(@Param("email") String email);

        @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
        Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

        @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM User u WHERE u.username = :username")
        boolean existsByUsername(@Param("username") String username);

        @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM User u WHERE u.email = :email")
        boolean existsByEmail(@Param("email") String email);

        // Projection: lightweight user summaries for search results
        @Query("SELECT u FROM User u WHERE " +
                        "(LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "AND u.active = true")
        List<UserSummaryProjection> searchUsers(@Param("query") String query);

        // Find users by role
        @Query("SELECT u FROM User u WHERE u.role = :role")
        List<UserSummaryProjection> findByRole(@Param("role") User.UserRole role);

        // Count followers of a user
        @Query("SELECT COUNT(f) FROM Follow f WHERE f.followed.id = :userId")
        long countFollowers(@Param("userId") Long userId);

        // Count following
        @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :userId")
        long countFollowing(@Param("userId") Long userId);
}
