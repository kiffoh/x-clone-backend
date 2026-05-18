package com.xclone.integration.like;

import com.xclone.exception.GlobalGraphQlExceptionHandlerTest;
import com.xclone.exception.dto.FieldError;
import com.xclone.integration.base.BaseGraphQLIntegrationTest;
import com.xclone.post.model.entity.Post;
import com.xclone.support.fixtures.LikeFixtures;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.LikeHelpers;
import com.xclone.support.helpers.PostHelpers;
import com.xclone.user.model.entity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.validation.BindException;

public class LikeIT extends BaseGraphQLIntegrationTest {
  List<String> handles = List.of("example1", "example2", "example3");
  List<User> users;

  List<String> messageContents = List.of("one for sorrow", "two for joy", "three for a girl");
  List<Post> posts;

  @BeforeEach
  void setup() {
    // Adds 3 users to the DB under the handles
    users =
        handles.stream().map(UserFixtures::createUserWithHandle).map(userRepository::save).toList();
    authenticatedUser = users.getFirst();
    // Sets the accessToken to match that of the first user
    String accessToken = authHelpers.getUserAccessToken(users.getFirst().getId().toString());
    authenticatedTester =
        authenticatedTester.mutate().headers(headers -> headers.setBearerAuth(accessToken)).build();
    // Create posts:
    // - authenticated user authors post at index-0
    // - user at index-1 authors post at index-1
    // - user at index-2 authors post at index-2
    posts = PostHelpers.seedPosts(messageContents, users, postRepository);
  }

  /**
   * {@link GlobalGraphQlExceptionHandlerTest#handlesBindException()} proves that a {@link
   * BindException} correctly maps to a {@link ErrorType#BAD_REQUEST}. Consequently, tests for an
   * invalid UUID for post id have been omitted.
   */
  @Nested
  class likePost {
    @Test
    void validRequest_likeOtherUsersPost_returnsUpdatedPostResponse() {
      // - authenticated user authors post at index-0
      // - user at index-1 authors post at index-1

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
          .variable("postId", posts.get(1).getId())
          .execute()
          .path("likePost.code")
          .entity(String.class)
          .isEqualTo("201")
          .path("likePost.post.likeCount")
          .entity(Integer.class)
          .isEqualTo(1);
    }

    @Test
    void validRequest_likeOwnPost_returnsUpdatedPostResponse() {
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
          .variable("postId", posts.getFirst().getId())
          .execute()
          .path("likePost.code")
          .entity(String.class)
          .isEqualTo("201")
          .path("likePost.post.likeCount")
          .entity(Integer.class)
          .isEqualTo(1);
    }

    @Test
    void invalidPostId_returnsPostNotFound() {
      authenticatedTester
          .document(
              """
                  mutation AddLike($postId: ID!) {
                    likePost(postId: $postId) {
                      code
                      post {
                        likeCount
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("postId", UUID.randomUUID())
          .execute()
          .path("likePost.code")
          .entity(String.class)
          .isEqualTo("404")
          .path("likePost.post")
          .valueIsNull()
          .path("likePost.errors")
          .entityList(FieldError.class)
          .hasSize(1)
          .containsExactly(new FieldError("postId", "Post does not exist"));
    }

    /**
     * {@link LikeHelpers#likePost(HttpGraphQlTester, UUID, Integer)} is utilised as part of the
     * test setup. Removing repeated lines of code improve the readability.
     */
    @Test
    void likeAlreadyExists_returnsPostResponse() {
      // - authenticated user authors post at index-0
      // - user at index-1 authors post at index-1

      LikeHelpers.likePost(authenticatedTester, posts.get(1).getId(), 1);

      // Trigger like again
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
          .variable("postId", posts.get(1).getId())
          .execute()
          .path("likePost.code")
          .entity(String.class)
          .isEqualTo("201")
          .path("likePost.post.likeCount")
          .entity(Integer.class)
          .isEqualTo(1);
    }
  }

  /**
   * {@link GlobalGraphQlExceptionHandlerTest#handlesBindException()} proves that a {@link
   * BindException} correctly maps to a {@link ErrorType#BAD_REQUEST}. Consequently, tests for an
   * invalid UUID for post id have been omitted.
   */
  @Nested
  class unlikePost {
    @BeforeEach
    void addLike() {
      // - authenticated user authors post at index-0
      // - user at index-1 authors post at index-1

      // authenticated user likes post 1 (authored by user 1)
      likeRepository.save(LikeFixtures.createLike(posts.get(1), authenticatedUser));
    }

    @Test
    void validRequest_returnsUpdatedPostResponse() {
      authenticatedTester
          .document(
              """
                  mutation RemoveLike($postId: ID!) {
                    unlikePost(postId: $postId) {
                      code
                      post {
                        likeCount
                      }
                    }
                  }
                  """)
          .variable("postId", posts.get(1).getId())
          .execute()
          .path("unlikePost.code")
          .entity(String.class)
          .isEqualTo("200")
          .path("unlikePost.post.likeCount")
          .entity(Integer.class)
          .isEqualTo(0);
    }

    @Test
    void invalidPostId_returnsPostNotFound() {
      authenticatedTester
          .document(
              """
                  mutation RemoveLike($postId: ID!) {
                    unlikePost(postId: $postId) {
                      code
                      post {
                        likeCount
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("postId", UUID.randomUUID())
          .execute()
          .path("unlikePost.code")
          .entity(String.class)
          .isEqualTo("404")
          .path("unlikePost.post")
          .valueIsNull()
          .path("unlikePost.errors")
          .entityList(FieldError.class)
          .hasSize(1)
          .containsExactly(new FieldError("postId", "Post does not exist"));
    }

    @Test
    void likeDoesNotExist_idempotentUnlike_returnsPostResponse() {
      authenticatedTester
          .document(
              """
                  mutation RemoveLike($postId: ID!) {
                    unlikePost(postId: $postId) {
                      code
                      post {
                        likeCount
                      }
                    }
                  }
                  """)
          .variable("postId", posts.get(2).getId())
          .execute()
          .path("unlikePost.code")
          .entity(String.class)
          .isEqualTo("200")
          .path("unlikePost.post.likeCount")
          .entity(Integer.class)
          .isEqualTo(0);
    }
  }
}
