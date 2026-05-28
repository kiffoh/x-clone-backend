package com.xclone.notification.dto.connection;

import com.xclone.common.connection.Connection;
import com.xclone.common.connection.PageInfo;
import java.util.List;

/**
 * Response DTO representing interconnecting notification entities.
 *
 * @param edges list of unique notification edge
 * @param pageInfo metadata about the statefulness of edges
 */
public record NotificationConnection(List<NotificationEdge> edges, PageInfo pageInfo)
    implements Connection<NotificationEdge> {}
