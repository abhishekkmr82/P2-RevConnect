package com.rev.app.repository;

import com.rev.app.entity.Post;
import com.rev.app.dto.PostSummaryProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>,
                JpaSpecificationExecutor<Post> {

        // Get user's own posts ordered by created date
        @EntityGraph(attributePaths = { "author", "likes", "comments", "shares" })
        @Query("SELECT p FROM Post p WHERE p.author.id = :authorId AND p.published = true ORDER BY p.pinned DESC, p.createdAt DESC")
        List<Post> findByAuthorIdAndPublishedTrueOrderByPinnedDescCreatedAtDesc(@Param("authorId") Long authorId);

        // Personalized feed: posts from connections and followed users
        @EntityGraph(attributePaths = { "author", "likes", "comments", "shares" })
        @Query("SELECT p FROM Post p WHERE p.author.id IN :userIds " +
                        "AND p.published = true " +
                        "ORDER BY p.pinned DESC, p.createdAt DESC")
        List<Post> findFeedPostsByUserIds(@Param("userIds") List<Long> userIds);

        // Search by hashtag
        @Query("SELECT p FROM Post p WHERE p.published = true AND " +
                        "LOWER(p.hashtags) LIKE LOWER(CONCAT('%', :hashtag, '%')) " +
                        "ORDER BY p.createdAt DESC")
        List<PostSummaryProjection> findByHashtag(@Param("hashtag") String hashtag);

        // Trending posts (most likes)
        @EntityGraph(attributePaths = { "author", "likes", "comments", "shares" })
        @Query("SELECT p FROM Post p WHERE p.published = true " +
                        "ORDER BY SIZE(p.likes) DESC, p.createdAt DESC")
        List<Post> findTrendingPosts();

        // Posts to publish (scheduled)
        @Query("SELECT p FROM Post p WHERE p.published = false " +
                        "AND p.scheduledAt <= CURRENT_TIMESTAMP")
        List<Post> findPostsDueToPublish();

        // Projection: post summaries
        @Query("SELECT p FROM Post p WHERE p.author.id = :authorId AND p.published = true")
        List<PostSummaryProjection> findPostSummariesByAuthor(@Param("authorId") Long authorId);

        // Count posts for analytics
        @Query("SELECT COUNT(p) FROM Post p WHERE p.author.id = :authorId AND p.published = true")
        long countPublishedPostsByAuthor(@Param("authorId") Long authorId);
}
