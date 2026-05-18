package com.xclone.integration.reply;

import static com.xclone.support.helpers.PostHelpers.createPostContents;
import static com.xclone.support.helpers.PostHelpers.deletePostsInDescendingOrder;
import static com.xclone.support.helpers.PostHelpers.seedPosts;
import static com.xclone.support.helpers.PostHelpers.setPostStatusDeleted;
import static org.assertj.core.api.Assertions.assertThat;

import com.xclone.integration.base.BaseGraphQLIntegrationTest;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.model.entity.Post;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.PostHelpers;
import com.xclone.user.model.entity.User;
import com.xclone.validation.ValidationConstants;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Delete Reply tests are covered in {@link com.xclone.integration.post.PostIT.deletePostTests} as
 * this method is used for the deletion of replies.
 */
public class ReplyIT extends BaseGraphQLIntegrationTest {
  List<String> handles = List.of("example1", "example2", "example3");
  List<User> users;
  User authenticatedUser;

  List<String> messageContents = createPostContents(6);
  List<Post> posts = List.of();

  @BeforeEach
  void setup() {
    cleanupDBs();
    // Adds 3 users to the DB under the handles
    users =
        handles.stream().map(UserFixtures::createUserWithHandle).map(userRepository::save).toList();
    authenticatedUser = users.getFirst();
    // Sets the accessToken to match that of the first user
    String accessToken = authHelpers.getUserAccessToken(users.getFirst().getId().toString());
    authenticatedTester =
        authenticatedTester.mutate().headers(headers -> headers.setBearerAuth(accessToken)).build();
  }

  void cleanupDBs() {
    // Flushes DBs
    deletePostsInDescendingOrder(posts, postRepository);
    postRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  class getReplyThreadTests {
    @BeforeEach
    void setup() {
      // Reply chain:
      //                post 1
      //               /
      // null - post 0                  post 4
      //               \               /
      //                post 2 - post 3
      //                               \
      //                                post 5
      // parentIndexes corresponds to the index of the post;
      List<Integer> parentIndexes =
          Arrays.asList(
              null, // post 0
              0,
              0, // post 1 + 2
              2, // post 3
              3,
              3 // post 4 + 5
              );
      posts =
          PostHelpers.seedReplies(
              messageContents,
              List.of(
                  users.get(1), // post 0
                  users.get(2), // post 1
                  users.get(0), // post 2
                  users.get(1), // post 3
                  users.get(2), // post 4
                  users.get(0)), // post 5
              parentIndexes,
              postRepository);
    }

    /**
     * Posts are reassigned in each test suite. To not trigger a FK error when deleting, the posts
     * are deleted from newest -> oldest.
     */
    @AfterEach
    void cleanup() {
      PostHelpers.deletePostsInDescendingOrder(posts, postRepository);
    }

    @Test
    void getAllAncestorsAndSiblings_LastSibling() {
      // Reply chain:
      //                post 1
      //               /
      // null - post 0                  post 4
      //               \               /
      //                post 2 - post 3
      //                               \
      //                                post 5
      authenticatedTester
          .document(
              """
                  query GetReplyChain($postId: ID!) {
                     getReplyThread(postId: $postId) {
                      ancestors {
                        id
                        createdAt
                      }
                      siblings {
                        id
                        createdAt
                      }
                     }
                  }
                  """)
          .variable("postId", posts.get(5).getId())
          .execute()
          .path("getReplyThread.ancestors[*]")
          .entityList(PostProfile.class)
          .satisfies(
              ancestors -> {
                assertThat(ancestors).hasSize(3);
                assertThat(ancestors.getFirst().id()).isEqualTo(posts.getFirst().getId());
                assertThat(ancestors.get(1).id()).isEqualTo(posts.get(2).getId());
                assertThat(ancestors.get(2).id()).isEqualTo(posts.get(3).getId());
                // post 0 older than post 2. post 2 older than post 3
                assertThat(ancestors.getFirst().createdAt()).isBefore(ancestors.get(1).createdAt());
                assertThat(ancestors.get(1).createdAt()).isBefore(ancestors.get(2).createdAt());
              })
          .path("getReplyThread.siblings[*]")
          .entityList(PostProfile.class)
          .satisfies(
              siblings -> {
                assertThat(siblings).hasSize(1);
                assertThat(siblings.getFirst().id()).isEqualTo(posts.get(4).getId());
                // post 0 older than post 2. post 2 older than post 3
                assertThat(siblings.getFirst().createdAt())
                    .isBefore(posts.get(5).getCreatedAt().atOffset(ZoneOffset.UTC));
              });
    }

    @Test
    void getAllAncestorsAndSiblings_FirstSibling() {
      // Reply chain:
      //                post 1
      //               /
      // null - post 0                  post 4
      //               \               /
      //                post 2 - post 3
      //                               \
      //                                post 5
      authenticatedTester
          .document(
              """
                  query GetReplyChain($postId: ID!) {
                     getReplyThread(postId: $postId) {
                      ancestors {
                        id
                        createdAt
                      }
                      siblings {
                        id
                        createdAt
                      }
                     }
                  }
                  """)
          .variable("postId", posts.get(4).getId())
          .execute()
          .path("getReplyThread.ancestors[*]")
          .entityList(PostProfile.class)
          .satisfies(
              ancestors -> {
                assertThat(ancestors).hasSize(3);
                assertThat(ancestors.getFirst().id()).isEqualTo(posts.getFirst().getId());
                assertThat(ancestors.get(1).id()).isEqualTo(posts.get(2).getId());
                assertThat(ancestors.get(2).id()).isEqualTo(posts.get(3).getId());
                // post 0 older than post 2. post 2 older than post 3
                assertThat(ancestors.getFirst().createdAt()).isBefore(ancestors.get(1).createdAt());
                assertThat(ancestors.get(1).createdAt()).isBefore(ancestors.get(2).createdAt());
              })
          .path("getReplyThread.siblings[*]")
          .entityList(PostProfile.class)
          .hasSize(0);
    }

    @Test
    void getAllAncestorsAndSiblings_NoSiblings() {
      // Reply chain:
      //                post 1
      //               /
      // null - post 0                  post 4
      //               \               /
      //                post 2 - post 3
      //                               \
      //                                post 5
      authenticatedTester
          .document(
              """
                  query GetReplyChain($postId: ID!) {
                     getReplyThread(postId: $postId) {
                      ancestors {
                        id
                        createdAt
                      }
                      siblings {
                        id
                        createdAt
                      }
                     }
                  }
                  """)
          .variable("postId", posts.get(3).getId())
          .execute()
          .path("getReplyThread.ancestors[*]")
          .entityList(PostProfile.class)
          .satisfies(
              ancestors -> {
                assertThat(ancestors).hasSize(2);
                assertThat(ancestors.getFirst().id()).isEqualTo(posts.getFirst().getId());
                assertThat(ancestors.get(1).id()).isEqualTo(posts.get(2).getId());
                // post 0 older than post 2. post 2 older than post 3
                assertThat(ancestors.getFirst().createdAt()).isBefore(ancestors.get(1).createdAt());
              })
          .path("getReplyThread.siblings[*]")
          .entityList(PostProfile.class)
          .hasSize(0);
    }

    @Test
    void getAllAncestorsAndSiblings_DeletedSibling() {
      setPostStatusDeleted(posts.get(4), postRepository);
      // Reply chain:
      //                post 1
      //               /
      // null - post 0                  post 4
      //               \               X
      //                post 2 - post 3
      //                               \
      //                                post 5
      authenticatedTester
          .document(
              """
                  query GetReplyChain($postId: ID!) {
                     getReplyThread(postId: $postId) {
                      ancestors {
                        id
                        createdAt
                      }
                      siblings {
                        id
                        createdAt
                      }
                     }
                  }
                  """)
          .variable("postId", posts.get(5).getId())
          .execute()
          .path("getReplyThread.ancestors[*]")
          .entityList(PostProfile.class)
          .satisfies(
              ancestors -> {
                assertThat(ancestors).hasSize(3);
                assertThat(ancestors.getFirst().id()).isEqualTo(posts.getFirst().getId());
                assertThat(ancestors.get(1).id()).isEqualTo(posts.get(2).getId());
                assertThat(ancestors.get(2).id()).isEqualTo(posts.get(3).getId());
                // post 0 older than post 2. post 2 older than post 3
                assertThat(ancestors.getFirst().createdAt()).isBefore(ancestors.get(1).createdAt());
                assertThat(ancestors.get(1).createdAt()).isBefore(ancestors.get(2).createdAt());
              })
          .path("getReplyThread.siblings[*]")
          .entityList(PostProfile.class)
          .hasSize(0);
    }

    @Test
    void getAllAncestorsAndSiblings_DeletedAncestor() {
      setPostStatusDeleted(posts.get(2), postRepository);
      // Reply chain:
      //                post 1
      //               /
      // null - post 0                 post 4
      //               \              /
      //                null - post 3
      //                              \
      //                               post 5
      authenticatedTester
          .document(
              """
                  query GetReplyChain($postId: ID!) {
                     getReplyThread(postId: $postId) {
                      ancestors {
                        id
                        createdAt
                      }
                      siblings {
                        id
                        createdAt
                      }
                     }
                  }
                  """)
          .variable("postId", posts.get(5).getId())
          .execute()
          .path("getReplyThread.ancestors[*]")
          .entityList(PostProfile.class)
          .satisfies(
              ancestors -> {
                assertThat(ancestors).hasSize(3);
                assertThat(ancestors.getFirst().id()).isEqualTo(posts.getFirst().getId());
                assertThat(ancestors.get(1)).isNull();
                assertThat(ancestors.get(2).id()).isEqualTo(posts.get(3).getId());
                // post 0 older than post 2. post 2 older than post 3
                assertThat(ancestors.getFirst().createdAt()).isBefore(ancestors.get(2).createdAt());
              })
          .path("getReplyThread.siblings[*]")
          .entityList(PostProfile.class)
          .satisfies(
              siblings -> {
                assertThat(siblings).hasSize(1);
                assertThat(siblings.getFirst().id()).isEqualTo(posts.get(4).getId());
                // post 0 older than post 2. post 2 older than post 3
                assertThat(siblings.getFirst().createdAt())
                    .isBefore(posts.get(5).getCreatedAt().atOffset(ZoneOffset.UTC));
              });
    }

    @Test
    void getReplyChain_OriginalPost() {
      // Reply chain:
      //                post 1
      //               /
      // null - post 0                  post 4
      //               \               /
      //                post 2 - post 3
      //                               \
      //                                post 5
      authenticatedTester
          .document(
              """
                  query GetReplyChain($postId: ID!) {
                     getReplyThread(postId: $postId) {
                      ancestors {
                        id
                        createdAt
                      }
                      siblings {
                        id
                        createdAt
                      }
                      focusedPost {
                        id
                      }
                     }
                  }
                  """)
          .variable("postId", posts.getFirst().getId())
          .execute()
          .path("getReplyThread.ancestors")
          .entityList(PostProfile.class)
          .hasSize(0)
          .path("getReplyThread.siblings")
          .entityList(PostProfile.class)
          .hasSize(0)
          .path("getReplyThread.focusedPost.id")
          .entity(UUID.class)
          .isEqualTo(posts.getFirst().getId());
    }

    @Test
    void getReplyChain_PostIdDoesNotExist() {
      // Reply chain:
      //                post 1
      //               /
      // null - post 0                  post 4
      //               \               /
      //                post 2 - post 3
      //                               \
      //                                post 5
      authenticatedTester
          .document(
              """
                  query GetReplyChain($postId: ID!) {
                     getReplyThread(postId: $postId) {
                      ancestors {
                        id
                        createdAt
                      }
                      siblings {
                        id
                        createdAt
                      }
                      focusedPost {
                        id
                      }
                     }
                  }
                  """)
          .variable("postId", UUID.randomUUID())
          .execute()
          .path("getReplyThread")
          .valueIsNull();
    }
  }

  @Nested
  class createReplyTests {
    @BeforeEach
    void setup() {
      posts = seedPosts(List.of("original post"), List.of(users.get(1)), postRepository);
    }

    @Test
    void validInput_returnsPostResponse() {
      Map<String, String> replyInput =
          Map.of(
              "messageContent",
              "first direct reply",
              "parentId",
              posts.getFirst().getId().toString());
      UUID newPostId =
          authenticatedTester
              .document(
                  """
                      mutation CreateReply($input: CreateReplyInput!) {
                        createReply(input: $input) {
                          code
                          success
                          post {
                            id
                            messageContent
                            author {
                              id
                            }
                            parent {
                              id
                            }
                          }
                        }
                      }
                      """)
              .variable("input", replyInput)
              .execute()
              .path("createReply")
              .matchesJson(
                  String.format(
                      """
                          {
                            "code": "200",
                            "success": true,
                            "post": {
                              "messageContent": "%s",
                              "author": {
                                "id": "%s"
                              },
                              "parent": {
                                "id": "%s"
                              }
                            }
                          }
                          """,
                      replyInput.get("messageContent"),
                      authenticatedUser.getId(),
                      replyInput.get("parentId")))
              .path("createReply.post.id")
              .entity(UUID.class)
              .get();

      // original post + reply
      assertThat(postRepository.findAll()).hasSize(2);

      // Clean up
      postRepository.deleteById(newPostId);
    }

    @Test
    void invalidInput_postIdDoesNotExist_returnsPostNotFound() {
      Map<String, String> replyInput =
          Map.of("messageContent", "first direct reply", "parentId", UUID.randomUUID().toString());

      authenticatedTester
          .document(
              """
                  mutation CreateReply($input: CreateReplyInput!) {
                    createReply(input: $input) {
                      code
                      success
                      post {
                        messageContent
                        author {
                          id
                        }
                        parent {
                          id
                        }
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("input", replyInput)
          .execute()
          .path("createReply")
          .matchesJson(
              String.format(
                  """
                      {
                        "code": "404",
                        "success": false,
                        "post": null,
                        "errors": [{
                          "field": "parentId",
                          "message": "%s"
                        }]
                      }
                      """,
                  "Parent post cannot be found"));

      // Only the original post
      assertThat(postRepository.findAll()).hasSize(1);
    }

    @Test
    void invalidInput_messageTooLong_returnsConstraintViolation() {
      String longMessage = "this message will be over 280 characters".repeat(20);
      Map<String, String> replyInput =
          Map.of("messageContent", longMessage, "parentId", posts.getFirst().getId().toString());

      authenticatedTester
          .document(
              """
                  mutation CreateReply($input: CreateReplyInput!) {
                    createReply(input: $input) {
                      code
                      success
                      post {
                        messageContent
                        author {
                          id
                        }
                        parent {
                          id
                        }
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("input", replyInput)
          .execute()
          .path("createReply")
          .matchesJson(
              String.format(
                  """
                      {
                        "code": "400",
                        "success": false,
                        "post": null,
                        "errors": [{
                          "field": "messageContent",
                          "message": "%s"
                        }]
                      }
                      """,
                  ValidationConstants.INVALID_MESSAGE_CONTENT_SIZE));

      // Only the original post
      assertThat(postRepository.findAll()).hasSize(1);
    }

    @Test
    void invalidInput_inputMissing_returnsConstraintViolation() {
      authenticatedTester
          .document(
              """
                  mutation CreateReply($input: CreateReplyInput!) {
                    createReply(input: $input) {
                      code
                      success
                      post {
                        messageContent
                        author {
                          id
                        }
                        parent {
                          id
                        }
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("input", null)
          .execute()
          .errors()
          .satisfy(
              errors -> {
                assertThat(errors).hasSize(1);

                assertThat(errors)
                    .anySatisfy(
                        error -> {
                          assertThat(error.getMessage())
                              .contains("CreateReplyInput")
                              .contains("'input' has an invalid value");
                          assertThat(error.getExtensions())
                              .containsEntry("classification", "ValidationError");
                        });
              });

      // Only the original post
      assertThat(postRepository.findAll()).hasSize(1);
    }
  }
}
