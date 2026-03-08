package com.xclone.user.dto.connection;

import java.util.List;

public record UserConnection(List<UserEdge> edges, PageInfo pageInfo, Integer totalCount) {}
