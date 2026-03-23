package com.xclone.common.connection;

import java.util.List;

/**
 * Base interface for connection responses.
 *
 * @param <T> the edge type contained in this connection
 */
public interface Connection<T> {
  List<T> edges();

  PageInfo pageInfo();

  Integer totalCount();
}
