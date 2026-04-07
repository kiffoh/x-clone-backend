package com.xclone.user.dto.connection;

import com.xclone.common.connection.Connection;
import com.xclone.common.connection.PageInfo;
import java.util.List;

/**
 * Response DTO representing interconnecting User models.
 *
 * @param edges list of unique user models
 * @param pageInfo metadata about the statefulness of edges
 */
public record UserConnection(List<UserEdge> edges, PageInfo pageInfo)
    implements Connection<UserEdge> {}
