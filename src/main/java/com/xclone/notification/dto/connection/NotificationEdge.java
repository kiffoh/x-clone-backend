package com.xclone.notification.dto.connection;

import com.xclone.notification.dto.NotificationProfile;

/**
 * Wraps a {@link NotificationProfile} node with its cursor for use in a Relay-style connection.
 *
 * @param node the notification profile at this position in the connection
 * @param cursor opaque string identifying this edge's position
 */
public record NotificationEdge(NotificationProfile node, String cursor) {}
