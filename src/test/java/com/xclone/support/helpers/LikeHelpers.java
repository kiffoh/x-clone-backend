package com.xclone.support.helpers;

import com.xclone.like.model.entity.Like;
import com.xclone.like.repository.LikeRepository;
import com.xclone.post.model.entity.Post;
import com.xclone.support.fixtures.LikeFixtures;
import com.xclone.user.model.entity.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

public class LikeHelpers {
  /**
   * Seeds one like per index position across posts and users.
   *
   * @param posts list of posts to like
   * @param users list of users which each liked the respective post
   * @param likeRepository interface for connecting Like entities to the database
   * @return list of likes from database
   * @throws IllegalArgumentException if posts and users are different lengths
   */
  public static List<Like> seedLikes(
      List<Post> posts, List<User> users, LikeRepository likeRepository) {
    if (posts.size() != users.size()) {
      throw new IllegalArgumentException("posts and users must be the same length");
    }
    List<Like> likes = new ArrayList<>();
    for (int i = 0; i < posts.size(); i++) {
      Like like = LikeFixtures.createLike(posts.get(i), users.get(i));
      Like savedLike = likeRepository.save(like);
      likes.add(savedLike);
    }
    return likes;
  }

  /**
   * Triggers the {@code likePost} mutation for the queried post id and asserts a successful
   * response with the queried {@code numberOfLikes}.
   *
   * <p>The user id associated with the like comes from the access token as part of the {@code
   * authenticatedTester}.
   *
   * @param authenticatedTester graphql tester with a valid access token attached in the
   *     authorisation headers
   * @param postId unique identifier of the post to like
   * @param numberOfLikes amount of likes to assert the queried post has
   */
  public static void likePost(
      HttpGraphQlTester authenticatedTester, UUID postId, Integer numberOfLikes) {
    authenticatedTester
        .document(
            """
                mutation AddLike($postId: ID!) {
                  likePost(postId: $postId) {
                    code
                    post {
                      likeCount
                    }
                  }
                }
                """)
        .variable("postId", postId)
        .execute()
        .path("likePost.code")
        .entity(String.class)
        .isEqualTo("201")
        .path("likePost.post.likeCount")
        .entity(Integer.class)
        .isEqualTo(numberOfLikes);
  }
}
