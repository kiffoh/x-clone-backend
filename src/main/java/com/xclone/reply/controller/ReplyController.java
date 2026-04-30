package com.xclone.reply.controller;

import com.xclone.post.dto.PostProfile;
import com.xclone.reply.service.ReplyService;
import java.util.List;
import java.util.UUID;
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
   * @param replyThreadId unique identifier for all posts which replied to the original post
   * @return a list of posts sorted by creation date ascendingly
   */
  // Should I have a toggle for the backend to do the filtering?
  // - Java is faster, and it means the request is sending less information (improved latency)
  @QueryMapping(name = "getReplyThread")
  private List<PostProfile> getReplyThread(UUID replyThreadId, UUID postId) {
    return replyService.getReplyThread(replyThreadId, postId);
  }
}
