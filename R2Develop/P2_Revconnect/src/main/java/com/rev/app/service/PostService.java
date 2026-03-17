package com.rev.app.service;

import com.rev.app.dto.PostDTO;
import com.rev.app.entity.Post;
import com.rev.app.entity.User;
import com.rev.app.exception.AccessDeniedException;
import com.rev.app.exception.ResourceNotFoundException;
import com.rev.app.dto.PostSummaryProjection;
import com.rev.app.repository.PostRepository;
import com.rev.app.repository.PostSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PostService {

    private static final Logger logger = LogManager.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final com.rev.app.mapper.PostMapper postMapper;
    private final com.rev.app.repository.ProductRepository productRepository;
    private final com.rev.app.repository.PostProductRepository postProductRepository;

    public PostService(PostRepository postRepository,
            NotificationService notificationService,
            com.rev.app.mapper.PostMapper postMapper,
            com.rev.app.repository.ProductRepository productRepository,
            com.rev.app.repository.PostProductRepository postProductRepository) {
        this.postRepository = postRepository;
        this.notificationService = notificationService;
        this.postMapper = postMapper;
        this.productRepository = productRepository;
        this.postProductRepository = postProductRepository;
    }

    public Post createPost(User author, PostDTO dto, org.springframework.web.multipart.MultipartFile image)
            throws java.io.IOException {
        logger.info("Service: Creating post for user: {} (Image attached: {})", author.getUsername(),
                (image != null && !image.isEmpty()));
        Post post = postMapper.toEntity(dto, author);

        if (image != null && !image.isEmpty()) {
            String filename = java.util.UUID.randomUUID() + "_" + image.getOriginalFilename();
            java.nio.file.Path rootPath = java.nio.file.Paths.get("uploads/post-images").toAbsolutePath();
            if (!java.nio.file.Files.exists(rootPath)) {
                java.nio.file.Files.createDirectories(rootPath);
            }
            java.nio.file.Files.copy(image.getInputStream(), rootPath.resolve(filename),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            post.setImageUrl("/uploads/post-images/" + filename);
            logger.info("Service: Image saved successfully at: {}", post.getImageUrl());
        }

        Post savedPost = postRepository.save(post);

        // Handle tagged products
        if (dto.getTaggedProductIds() != null && !dto.getTaggedProductIds().isEmpty()) {
            for (Long productId : dto.getTaggedProductIds()) {
                productRepository.findById(productId).ifPresent(product -> {
                    com.rev.app.entity.PostProduct postProduct = new com.rev.app.entity.PostProduct(savedPost, product);
                    postProductRepository.save(postProduct);
                });
            }
        }

        return savedPost;
    }

    public Post updatePost(Long postId, Long currentUserId, PostDTO dto) {
        Post post = findById(postId);
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only edit your own posts.");
        }
        post.setContent(dto.getContent());
        String tags = dto.getHashtags();
        if (tags != null) {
            tags = tags.replace(" ", ",").replaceAll(",+", ",");
        }
        post.setHashtags(tags);
        if (dto.getCtaLabel() != null)
            post.setCtaLabel(dto.getCtaLabel());
        if (dto.getCtaUrl() != null)
            post.setCtaUrl(dto.getCtaUrl());
        post.setPinned(dto.isPinned());
        Post savedPost = postRepository.save(post);

        // Update tagged products
        postProductRepository.deleteByPostId(savedPost.getId());
        if (dto.getTaggedProductIds() != null && !dto.getTaggedProductIds().isEmpty()) {
            for (Long productId : dto.getTaggedProductIds()) {
                productRepository.findById(productId).ifPresent(product -> {
                    com.rev.app.entity.PostProduct postProduct = new com.rev.app.entity.PostProduct(savedPost, product);
                    postProductRepository.save(postProduct);
                });
            }
        }

        return savedPost;
    }

    public void deletePost(Long postId, Long currentUserId) {
        Post post = findById(postId);
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only delete your own posts.");
        }
        // Clean up logical references in notifications
        notificationService.deletePostNotifications(postId);

        postRepository.delete(post);
        logger.info("Post {} deleted by user {}", postId, currentUserId);
    }

    public Post sharePost(Long originalPostId, User sharer) {
        Post original = findById(originalPostId);
        Post share = new Post();
        share.setAuthor(sharer);
        share.setContent(original.getContent());
        share.setHashtags(original.getHashtags());
        share.setPostType(Post.PostType.REPOST);
        share.setOriginalPost(original);
        share.setPublished(true);
        Post saved = postRepository.save(share);
        // notify original author
        notificationService.notifyPostShared(original.getAuthor(), sharer, original.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Post> getUserPosts(Long userId) {
        return postRepository.findByAuthorIdAndPublishedTrueOrderByPinnedDescCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Post> getFeed(List<Long> userIds) {
        return postRepository.findFeedPostsByUserIds(userIds);
    }

    @Transactional(readOnly = true)
    public List<PostSummaryProjection> searchByHashtag(String hashtag) {
        return postRepository.findByHashtag(hashtag);
    }

    @Transactional(readOnly = true)
    public List<Post> getTrendingPosts() {
        return postRepository.findTrendingPosts();
    }

    // Filter feed using Specifications
    @Transactional(readOnly = true)
    public List<Post> filterPosts(Post.PostType type, String hashtag) {
        Specification<Post> spec = PostSpecification.isPublished();
        if (type != null)
            spec = spec.and(PostSpecification.hasPostType(type));
        if (hashtag != null && !hashtag.isBlank())
            spec = spec.and(PostSpecification.containsHashtag(hashtag));
        return postRepository.findAll(Specification.where(spec));
    }

    // Scheduled: auto-publish posts when scheduledAt has passed
    @Scheduled(fixedDelay = 60000) // every 60 seconds
    public void publishScheduledPosts() {
        List<Post> due = postRepository.findPostsDueToPublish();
        for (Post post : due) {
            post.setPublished(true);
            postRepository.save(post);
            logger.info("Auto-published scheduled post: {}", post.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<Post> findAll() {
        return postRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long countPostsByAuthor(Long authorId) {
        return postRepository.countPublishedPostsByAuthor(authorId);
    }
}
