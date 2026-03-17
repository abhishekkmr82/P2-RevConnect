-- ================================================
-- RevConnect P2 - Database Setup Script (Oracle)
-- ================================================
-- Run this script as SYSTEM/SYSDBA first (Part 1),
-- then reconnect as r2sample to create tables (Part 2).
-- ================================================

-- ================================================
-- PART 1: User Creation (run as SYSTEM/SYSDBA)
-- ================================================
ALTER SESSION SET "_ORACLE_SCRIPT"=true;

CREATE USER r2sample IDENTIFIED BY r2sample;
GRANT CONNECT, RESOURCE, DBA TO r2sample;
GRANT CREATE SESSION TO r2sample;
GRANT UNLIMITED TABLESPACE TO r2sample;

COMMIT;

-- ================================================
-- PART 2: Sequences & Table Creation (run as r2sample)
-- ================================================
-- Connect as: r2sample/r2sample@localhost:1521/xe
-- Sequences first, then tables in dependency order.
-- ================================================

-- ------------------------------------------------
-- Sequences
-- ------------------------------------------------
CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE posts_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE comments_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE likes_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE follows_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE connections_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE messages_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE notifications_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE notification_preferences_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ------------------------------------------------
-- 1. USERS
-- ------------------------------------------------
CREATE TABLE users (
    id              NUMBER(19) DEFAULT users_seq.NEXTVAL PRIMARY KEY,
    username        VARCHAR2(50)   NOT NULL,
    email           VARCHAR2(255)  NOT NULL,
    password        VARCHAR2(255)  NOT NULL,
    security_question VARCHAR2(255) NOT NULL,
    security_answer VARCHAR2(255)  NOT NULL,
    full_name       VARCHAR2(255),
    bio             CLOB,
    profile_picture VARCHAR2(255),
    location        VARCHAR2(255),
    website         VARCHAR2(255),
    role            VARCHAR2(20)   DEFAULT 'PERSONAL' NOT NULL,
    privacy_setting VARCHAR2(20)   DEFAULT 'PUBLIC',
    is_active       NUMBER(1)      DEFAULT 1,
    category        VARCHAR2(255),
    contact_info    VARCHAR2(255),
    business_address VARCHAR2(255),
    business_hours  VARCHAR2(255),
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_role    CHECK (role IN ('PERSONAL', 'CREATOR', 'BUSINESS')),
    CONSTRAINT chk_users_privacy CHECK (privacy_setting IN ('PUBLIC', 'PRIVATE'))
);

-- ------------------------------------------------
-- 2. POSTS
-- ------------------------------------------------
CREATE TABLE posts (
    id              NUMBER(19) DEFAULT posts_seq.NEXTVAL PRIMARY KEY,
    author_id       NUMBER(19)     NOT NULL,
    content         CLOB           NOT NULL,
    hashtags        VARCHAR2(255),
    post_type       VARCHAR2(20)   DEFAULT 'REGULAR',
    is_pinned       NUMBER(1)      DEFAULT 0,
    scheduled_at    TIMESTAMP,
    is_published    NUMBER(1)      DEFAULT 1,
    cta_label       VARCHAR2(255),
    cta_url         VARCHAR2(255),
    original_post_id NUMBER(19),
    image_url       VARCHAR2(255),
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_posts_author      FOREIGN KEY (author_id)       REFERENCES users(id),
    CONSTRAINT fk_posts_original    FOREIGN KEY (original_post_id) REFERENCES posts(id),
    CONSTRAINT chk_posts_type       CHECK (post_type IN ('REGULAR', 'PROMOTIONAL', 'ANNOUNCEMENT', 'REPOST'))
);

-- ------------------------------------------------
-- 3. COMMENTS
-- ------------------------------------------------
CREATE TABLE comments (
    id              NUMBER(19) DEFAULT comments_seq.NEXTVAL PRIMARY KEY,
    post_id         NUMBER(19)     NOT NULL,
    author_id       NUMBER(19)     NOT NULL,
    content         CLOB           NOT NULL,
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_post    FOREIGN KEY (post_id)   REFERENCES posts(id),
    CONSTRAINT fk_comments_author  FOREIGN KEY (author_id) REFERENCES users(id)
);

-- ------------------------------------------------
-- 4. LIKES
-- ------------------------------------------------
CREATE TABLE likes (
    id              NUMBER(19) DEFAULT likes_seq.NEXTVAL PRIMARY KEY,
    user_id         NUMBER(19)     NOT NULL,
    post_id         NUMBER(19)     NOT NULL,
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_likes_user       FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_likes_post       FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT uq_likes_user_post  UNIQUE (user_id, post_id)
);

-- ------------------------------------------------
-- 5. FOLLOWS
-- ------------------------------------------------
CREATE TABLE follows (
    id              NUMBER(19) DEFAULT follows_seq.NEXTVAL PRIMARY KEY,
    follower_id     NUMBER(19)     NOT NULL,
    followed_id     NUMBER(19)     NOT NULL,
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users(id),
    CONSTRAINT fk_follows_followed FOREIGN KEY (followed_id) REFERENCES users(id),
    CONSTRAINT uq_follows_pair     UNIQUE (follower_id, followed_id)
);

-- ------------------------------------------------
-- 6. CONNECTIONS
-- ------------------------------------------------
CREATE TABLE connections (
    id              NUMBER(19) DEFAULT connections_seq.NEXTVAL PRIMARY KEY,
    sender_id       NUMBER(19)     NOT NULL,
    receiver_id     NUMBER(19)     NOT NULL,
    status          VARCHAR2(20)   DEFAULT 'PENDING' NOT NULL,
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_connections_sender   FOREIGN KEY (sender_id)   REFERENCES users(id),
    CONSTRAINT fk_connections_receiver FOREIGN KEY (receiver_id) REFERENCES users(id),
    CONSTRAINT uq_connections_pair     UNIQUE (sender_id, receiver_id),
    CONSTRAINT chk_connections_status  CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED'))
);

-- ------------------------------------------------
-- 7. MESSAGES
-- ------------------------------------------------
CREATE TABLE messages (
    id              NUMBER(19) DEFAULT messages_seq.NEXTVAL PRIMARY KEY,
    sender_id       NUMBER(19)     NOT NULL,
    recipient_id    NUMBER(19)     NOT NULL,
    content         CLOB           NOT NULL,
    is_read         NUMBER(1)      DEFAULT 0,
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_sender    FOREIGN KEY (sender_id)    REFERENCES users(id),
    CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id) REFERENCES users(id)
);

-- ------------------------------------------------
-- 8. NOTIFICATIONS
-- ------------------------------------------------
CREATE TABLE notifications (
    id              NUMBER(19) DEFAULT notifications_seq.NEXTVAL PRIMARY KEY,
    recipient_id    NUMBER(19)     NOT NULL,
    actor_id        NUMBER(19),
    type            VARCHAR2(30)   NOT NULL,
    message         CLOB,
    reference_id    NUMBER(19),
    is_read         NUMBER(1)      DEFAULT 0,
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users(id),
    CONSTRAINT fk_notifications_actor     FOREIGN KEY (actor_id)     REFERENCES users(id),
    CONSTRAINT chk_notifications_type     CHECK (type IN (
        'CONNECTION_REQUEST', 'CONNECTION_ACCEPTED', 'NEW_FOLLOWER',
        'POST_LIKED', 'POST_COMMENTED', 'POST_SHARED', 'MESSAGE_RECEIVED', 'MENTION'
    ))
);

-- ------------------------------------------------
-- 9. NOTIFICATION_PREFERENCES
-- ------------------------------------------------
CREATE TABLE notification_preferences (
    id                   NUMBER(19) DEFAULT notification_preferences_seq.NEXTVAL PRIMARY KEY,
    user_id              NUMBER(19) NOT NULL,
    connection_requests  NUMBER(1)  DEFAULT 1,
    connection_accepted  NUMBER(1)  DEFAULT 1,
    new_followers        NUMBER(1)  DEFAULT 1,
    post_likes           NUMBER(1)  DEFAULT 1,
    post_comments        NUMBER(1)  DEFAULT 1,
    post_shares          NUMBER(1)  DEFAULT 1,
    messages             NUMBER(1)  DEFAULT 1,
    CONSTRAINT fk_notifpref_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_notifpref_user UNIQUE (user_id)
);

-- ================================================
-- Indexes for common query patterns
-- ================================================
CREATE INDEX idx_posts_author        ON posts(author_id);
CREATE INDEX idx_posts_created       ON posts(created_at);
CREATE INDEX idx_comments_post       ON comments(post_id);
CREATE INDEX idx_comments_author     ON comments(author_id);
CREATE INDEX idx_likes_post          ON likes(post_id);
CREATE INDEX idx_follows_follower    ON follows(follower_id);
CREATE INDEX idx_follows_followed    ON follows(followed_id);
CREATE INDEX idx_connections_sender   ON connections(sender_id);
CREATE INDEX idx_connections_receiver ON connections(receiver_id);
CREATE INDEX idx_messages_sender     ON messages(sender_id);
CREATE INDEX idx_messages_recipient  ON messages(recipient_id);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);

COMMIT;

-- ================================================
-- MIGRATION: Add security question columns to existing users table
-- Run this only if the users table already exists without these columns.
-- ================================================
-- ALTER TABLE users ADD (security_question VARCHAR2(255));
-- ALTER TABLE users ADD (security_answer VARCHAR2(255));
-- UPDATE users SET security_question = 'What is your favourite colour?' WHERE security_question IS NULL;
-- UPDATE users SET security_answer = 'unknown' WHERE security_answer IS NULL;
-- ALTER TABLE users MODIFY (security_question NOT NULL);
-- ALTER TABLE users MODIFY (security_answer NOT NULL);
-- COMMIT;
