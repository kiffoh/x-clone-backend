package com.xclone.user.dto.connection;

/**
 * Represents metadata for a GraphQL relay-style connection.
 *
 * @param hasNextPage true if additional results exist after the current page
 * @param hasPreviousPage true if results exist before the current page
 * @param startCursor cursor of the starting node
 * @param endCursor cursor of the final node
 */
public record PageInfo(
    Boolean hasNextPage, Boolean hasPreviousPage, String startCursor, String endCursor) {}
