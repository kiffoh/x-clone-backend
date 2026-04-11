package com.xclone.post.dto.connection;

import com.xclone.common.connection.Connection;
import com.xclone.common.connection.PageInfo;
import java.util.List;

/**
 * Response DTO representing interconnecting Post models.
 *
 * @param edges list of unique post models
 * @param pageInfo metadata about the statefulness of edges
 */
public record PostConnection(List<PostEdge> edges, PageInfo pageInfo)
    implements Connection<PostEdge> {}
