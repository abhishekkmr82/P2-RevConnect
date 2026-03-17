package com.rev.app.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notif_pref_seq")
    @SequenceGenerator(name = "notif_pref_seq", sequenceName = "notification_preferences_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "connection_requests")
    private boolean connectionRequests = true;

    @Column(name = "connection_accepted")
    private boolean connectionAccepted = true;

    @Column(name = "new_followers")
    private boolean newFollowers = true;

    @Column(name = "post_likes")
    private boolean postLikes = true;

    @Column(name = "post_comments")
    private boolean postComments = true;

    @Column(name = "post_shares")
    private boolean postShares = true;

    @Column(name = "messages")
    private Boolean messages = true;

    // Constructors
    public NotificationPreference() {
    }

    public NotificationPreference(User user) {
        this.user = user;
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

    public boolean isConnectionRequests() {
        return connectionRequests;
    }

    public void setConnectionRequests(boolean connectionRequests) {
        this.connectionRequests = connectionRequests;
    }

    public boolean isConnectionAccepted() {
        return connectionAccepted;
    }

    public void setConnectionAccepted(boolean connectionAccepted) {
        this.connectionAccepted = connectionAccepted;
    }

    public boolean isNewFollowers() {
        return newFollowers;
    }

    public void setNewFollowers(boolean newFollowers) {
        this.newFollowers = newFollowers;
    }

    public boolean isPostLikes() {
        return postLikes;
    }

    public void setPostLikes(boolean postLikes) {
        this.postLikes = postLikes;
    }

    public boolean isPostComments() {
        return postComments;
    }

    public void setPostComments(boolean postComments) {
        this.postComments = postComments;
    }

    public boolean isPostShares() {
        return postShares;
    }

    public void setPostShares(boolean postShares) {
        this.postShares = postShares;
    }

    public boolean isMessages() {
        return messages == null || messages;
    }

    public void setMessages(boolean messages) {
        this.messages = messages;
    }
}
