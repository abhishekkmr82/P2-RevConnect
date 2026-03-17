package com.rev.app.service;

import com.rev.app.entity.Post;
import com.rev.app.entity.User;
import com.rev.app.repository.PostRepository;
import com.rev.app.repository.LikeRepository;
import com.rev.app.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final FollowService followService;
    private final UserService userService;
    private final com.rev.app.repository.ConnectionRepository connectionRepository;
    private final com.rev.app.repository.FollowRepository followRepository;

    public AnalyticsService(PostRepository postRepository,
            LikeRepository likeRepository,
            CommentRepository commentRepository,
            FollowService followService,
            UserService userService,
            com.rev.app.repository.ConnectionRepository connectionRepository,
            com.rev.app.repository.FollowRepository followRepository) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.followService = followService;
        this.userService = userService;
        this.connectionRepository = connectionRepository;
        this.followRepository = followRepository;
    }

    /**
     * Returns analytics for each published post of the given user.
     */
    public List<Map<String, Object>> getPostAnalytics(Long authorId) {
        List<Post> posts = postRepository.findByAuthorIdAndPublishedTrueOrderByPinnedDescCreatedAtDesc(authorId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> analytics = new HashMap<>();
            analytics.put("postId", post.getId());
            analytics.put("content", post.getContent().length() > 80
                    ? post.getContent().substring(0, 80) + "..."
                    : post.getContent());
            analytics.put("createdAt", post.getCreatedAt());
            analytics.put("likes", likeRepository.countByPostId(post.getId()));
            analytics.put("comments", commentRepository.countByPostId(post.getId()));
            analytics.put("shares", post.getShares() != null ? post.getShares().size() : 0);
            result.add(analytics);
        }
        return result;
    }

    /**
     * Returns summary metrics for the user's account.
     */
    public Map<String, Object> getAccountMetrics(Long userId) {
        User user = userService.findById(userId);
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalPosts", postRepository.countPublishedPostsByAuthor(userId));
        metrics.put("totalFollowers", followService.countFollowers(userId));
        metrics.put("totalFollowing", followService.countFollowing(userId));
        metrics.put("totalConnections", connectionRepository.countConnections(user.getId()));
        return metrics;
    }

    /**
     * Returns follower demographics: role distribution, top locations, join
     * timeline.
     */
    public Map<String, Object> getFollowerDemographics(Long userId) {
        List<User> followers = followRepository.findFollowerUsersByFollowedId(userId);
        Map<String, Object> demographics = new HashMap<>();

        // Role distribution
        Map<String, Long> roleDistribution = new HashMap<>();
        for (User f : followers) {
            String role = f.getRole().name();
            roleDistribution.put(role, roleDistribution.getOrDefault(role, 0L) + 1);
        }
        demographics.put("roleDistribution", roleDistribution);

        // Location distribution (top locations)
        Map<String, Long> locationDistribution = new HashMap<>();
        for (User f : followers) {
            String loc = (f.getLocation() != null && !f.getLocation().isBlank())
                    ? f.getLocation()
                    : "Not specified";
            locationDistribution.put(loc, locationDistribution.getOrDefault(loc, 0L) + 1);
        }
        demographics.put("locationDistribution", locationDistribution);

        // Join timeline (grouped by month)
        Map<String, Long> joinTimeline = new java.util.TreeMap<>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");
        for (User f : followers) {
            if (f.getCreatedAt() != null) {
                String month = f.getCreatedAt().format(fmt);
                joinTimeline.put(month, joinTimeline.getOrDefault(month, 0L) + 1);
            }
        }
        demographics.put("joinTimeline", joinTimeline);
        demographics.put("totalFollowers", (long) followers.size());

        return demographics;
    }
}
