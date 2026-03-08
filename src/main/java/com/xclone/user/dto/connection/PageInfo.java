package com.xclone.user.dto.connection;

public record PageInfo(
    Boolean hasNextPage, Boolean hasPreviousPage, String startCursor, String endCursor) {}
