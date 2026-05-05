package com.xclone.reply.controller;

import com.xclone.post.dto.PostProfile;
import com.xclone.reply.dto.ReplyThread;
import com.xclone.reply.service.ReplyService;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** GraphQL controller for reply-related operations. */
@Controller
public class ReplyController {
  ReplyService replyService;

  ReplyController(ReplyService replyService) {
    this.replyService = replyService;
  }

  /**
   * Fetches the specific reply thread for the queried {@code postId}.
   *
   * @param post starting post in a reply thread
   * @return a 2D list of posts, one of ancestors and one of siblings, both sorted by creation date
   *     ascendingly
   */
  // Should I have a toggle for the backend to do the filtering?
  // - Java is faster, and it means the request is sending less information (improved latency)
  @QueryMapping(name = "getReplyThread")
  public ReplyThread getReplyThread(PostProfile post) {
    if (post.parentId() == null) {
      return null;
    }

    return replyService.getReplyThread(post);
  }
}
