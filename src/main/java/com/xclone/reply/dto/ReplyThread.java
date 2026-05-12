package com.xclone.reply.dto;

import com.xclone.post.dto.PostProfile;
import java.util.List;

/**
 * Represents the prior posts in a reply thread.
 *
 * @param ancestors list of posts sorted ascendingly that are nested replies to the original post
 * @param siblings list of posts sorted ascendingly that are direct replies to the queried post
 * @param focusedPost post sent in the request
 */
public record ReplyThread(
    List<PostProfile> ancestors, List<PostProfile> siblings, PostProfile focusedPost) {}
