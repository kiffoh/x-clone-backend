package com.xclone.support.helpers;

import static com.xclone.support.fixtures.PostFixtures.createQuote;
import static com.xclone.support.fixtures.PostFixtures.createRepost;

import com.xclone.common.enums.Status;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.fixtures.PostFixtures;
import com.xclone.user.model.entity.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PostHelpers {
  /**
   * Seeds one post per index position across messageContents and authors.
   *
   * @param messageContents list of strings containing each posts text content
   * @param authors list of users which each post belongs to
   * @param postRepository interface for connecting Post entities to the database
   * @return list of posts from database
   * @throws IllegalArgumentException if messageContents and authors are different lengths
   */
  public static List<Post> seedPosts(
      List<String> messageContents, List<User> authors, PostRepository postRepository) {
    if (messageContents.size() != authors.size()) {
      throw new IllegalArgumentException("messageContents and authors must be the same length");
    }
    List<Post> posts = new ArrayList<>();
    for (int i = 0; i < messageContents.size(); i++) {
      Post post = PostFixtures.createPostWithContent(messageContents.get(i), authors.get(i));
      Post savedPost = postRepository.save(post);
      posts.add(savedPost);
    }
    return posts;
  }

  public static Post seedRepost(UUID quotedPostId, UUID authorId, PostRepository postRepository) {
    Post newRepost = new Post();
    newRepost.setAuthorId(authorId);
    newRepost.setQuotedPostId(quotedPostId);
    return postRepository.save(newRepost);
  }

  public static List<Post> seedReplies(
      List<String> messageContents,
      List<User> authors,
      List<Integer> parentIndexes,
      PostRepository postRepository) {
    if ((messageContents.size() != authors.size())
        || (messageContents.size() != parentIndexes.size())) {
      throw new IllegalArgumentException(
          "messageContents, authors and postIndexes must be the same length");
    }
    List<Post> posts = new ArrayList<>();
    for (int i = 0; i < messageContents.size(); i++) {
      UUID parentId =
          (parentIndexes.get(i) == null) ? null : posts.get(parentIndexes.get(i)).getId();
      Post post =
          PostFixtures.createReplyWithContent(messageContents.get(i), authors.get(i), parentId);
      Post savedPost = postRepository.save(post);
      posts.add(savedPost);
    }
    return posts;
  }

  public static List<Post> seedQuotes(
      UUID quotedPostId,
      List<User> authors,
      List<String> messageContents,
      PostRepository postRepository) {
    if ((messageContents.size() != authors.size())) {
      throw new IllegalArgumentException(
          "messageContents, authors and postIndexes must be the same length");
    }
    List<Post> quotes = new ArrayList<>();
    for (int i = 0; i < messageContents.size(); i++) {
      Post quote = createQuote(quotedPostId, authors.get(i), messageContents.get(i));
      Post savedPost = postRepository.save(quote);
      quotes.add(savedPost);
    }
    return quotes;
  }

  public static List<Post> seedReposts(
      UUID quotedPostId, List<User> authors, PostRepository postRepository) {
    List<Post> reposts = new ArrayList<>();
    for (User author : authors) {
      Post repost = createRepost(quotedPostId, author);
      Post savedPost = postRepository.save(repost);
      reposts.add(savedPost);
    }
    return reposts;
  }

  public static void setPostStatusDeleted(Post post, PostRepository postRepository) {
    post.setStatus(Status.DELETED);
    postRepository.saveAndFlush(post);
  }

  public static List<String> createPostContents(int numberOfPosts) {
    return new ArrayList<>(PostFixtures.magpieRhyme.subList(0, numberOfPosts));
  }

  public static void deletePostsInDescendingOrder(List<Post> posts, PostRepository postRepository) {
    for (int i = posts.size() - 1; i >= 0; i--) {
      postRepository.delete(posts.get(i));
    }
  }
}
