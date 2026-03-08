package com.xclone.user.dto.connection;

import com.xclone.user.dto.UserProfile;

/**
 * Wraps a {@link UserProfile} node with its cursor for use in a Relay-style connection.
 *
 * @param node   the user profile at this position in the connection
 * @param cursor opaque string identifying this edge's position; currently the user's UUID
 */
public record UserEdge(UserProfile node, String cursor) {
}
