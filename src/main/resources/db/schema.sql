
CREATE TYPE user_status AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name VARCHAR(100) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    bio TEXT,
    profile_image VARCHAR(500),
    status user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE follows (
    follower_id UUID NOT NULL,
    following_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    PRIMARY KEY (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT check_not_self_follow CHECK (follower_id != following_id)
);

CREATE TYPE post_status AS ENUM ('ACTIVE', 'DELETED', 'HIDDEN');

CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL,
    message_content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    status post_status NOT NULL DEFAULT 'ACTIVE',

    parent_id UUID, -- For comments
    quoted_post_id UUID, -- For reposts and quotes

    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,-- Implement soft delete by never deleting users
    FOREIGN KEY (parent_id) REFERENCES posts(id) ON DELETE SET NULL,
    FOREIGN KEY (quoted_post_id) REFERENCES posts(id) ON DELETE SET NULL
);

CREATE TABLE likes (
    user_id UUID NOT NULL,
    post_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    PRIMARY KEY (user_id, post_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) On DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
-- Check for no self like in application code

CREATE TABLE post_mentions (
    post_id UUID NOT NULL,
    mentioned_user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    PRIMARY KEY (mentioned_user_id, post_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) On DELETE CASCADE,
    FOREIGN KEY (mentioned_user_id) REFERENCES users(id) ON DELETE CASCADE
);
-- Check for no self mention in application code


CREATE TYPE notification_type AS ENUM ('LIKE', 'COMMENT', 'REPOST', 'QUOTE', 'FOLLOW', 'MENTION');

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL,
    post_id UUID,
    type notification_type NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE TABLE notification_actors (
    notification_id UUID NOT NULL,
    actor_user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    PRIMARY KEY (actor_user_id, notification_id),
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_users_handle ON users(handle);
CREATE INDEX idx_users_status ON users(status);

CREATE INDEX idx_follows_follower ON follows(follower_id);
CREATE INDEX idx_follows_following ON follows(following_id);

CREATE INDEX idx_posts_author ON posts(author_id);
CREATE INDEX idx_posts_parent ON posts(parent_id);
CREATE INDEX idx_posts_quoted_post ON posts(quoted_post_id);
CREATE INDEX idx_posts_created_at ON posts(created_at);
CREATE INDEX idx_posts_status ON posts(status);
-- Why do we want an index on status?

CREATE INDEX idx_likes_post ON likes(post_id);
CREATE INDEX idx_likes_user ON likes(user_id);

CREATE INDEX idx_notifications_recipient on notifications(recipient_user_id);
CREATE INDEX idx_notifications_created_at on notifications(created_at);
CREATE INDEX idx_notifications_read on notifications(read);
CREATE INDEX idx_notifications_post ON notifications(post_id);
