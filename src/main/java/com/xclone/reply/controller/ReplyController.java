package com.xclone.reply.controller;

import com.xclone.post.dto.PostProfile;
import com.xclone.post.service.PostService;
import com.xclone.reply.dto.ReplyThread;
import com.xclone.reply.service.ReplyService;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** GraphQL controller for reply-related operations. */
@Controller
public class ReplyController {
  ReplyService replyService;
  PostService postService;

  ReplyController(ReplyService replyService, PostService postService) {
    this.replyService = replyService;
    this.postService = postService;
  }

  /**
   * Fetches the specific reply thread for the queried {@code postId}.
   *
   * @param postId unique identifier of the starting post in a reply thread
   * @return a 2D list of posts, one of ancestors and one of siblings, both sorted by creation date
   *     ascendingly
   */
  @QueryMapping(name = "getReplyThread")
  public ReplyThread getReplyThread(@Argument UUID postId) {
    PostProfile post = postService.getPost(postId);
    if (post == null) {
      return null;
    }
    if (post.parentId() == null) {
      return new ReplyThread(null, null, post);
    }

    return replyService.getReplyThread(post);
  }
}
