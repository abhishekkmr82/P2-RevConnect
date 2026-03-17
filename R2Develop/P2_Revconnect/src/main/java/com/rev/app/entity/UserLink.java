package com.rev.app.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_links")
public class UserLink {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_links_seq")
    @SequenceGenerator(name = "user_links_seq", sequenceName = "user_links_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String label; // e.g. "Portfolio", "LinkedIn"

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type")
    private LinkType linkType = LinkType.OTHER;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum LinkType {
        SOCIAL, ENDORSEMENT, PARTNERSHIP, OTHER
    }

    public UserLink() {
    }

    public UserLink(User user, String label, String url, LinkType linkType) {
        this.user = user;
        this.label = label;
        this.url = url;
        this.linkType = linkType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(LinkType linkType) {
        this.linkType = linkType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
