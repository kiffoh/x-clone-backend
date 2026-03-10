package com.xclone.user.dto.connection;

import com.xclone.common.connection.PageInfo;
import java.util.List;

/**
 * Response DTO representing interconnecting User models.
 *
 * @param edges list of unique user models
 * @param pageInfo metadata about the statefulness of edges
 * @param totalCount total number of matching users; currently equals the number of edges as
 *     pagination is not yet implemented
 */
public record UserConnection(List<UserEdge> edges, PageInfo pageInfo, Integer totalCount) {}
