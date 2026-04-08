package com.xclone.post.dto.connection;

import com.xclone.post.dto.PostProfile;

/**
 * Wraps a {@link PostProfile} node with its cursor for use in a Relay-style connection.
 *
 * @param node the post profile at this position in the connection
 * @param cursor opaque string identifying this edge's position
 */
public record PostEdge(PostProfile node, String cursor) {}
