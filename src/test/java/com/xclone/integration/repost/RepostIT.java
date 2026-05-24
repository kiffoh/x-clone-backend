package com.xclone.integration.repost;

import static com.xclone.support.helpers.PostHelpers.seedPosts;
import static com.xclone.support.helpers.PostHelpers.seedRepost;
import static com.xclone.support.helpers.PostHelpers.setPostStatusDeleted;
import static org.assertj.core.api.Assertions.assertThat;

import com.xclone.integration.base.BaseGraphQLIntegrationTest;
import com.xclone.post.model.entity.Post;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.user.model.entity.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Delete Quote/Repost tests are covered in {@link
 * com.xclone.integration.post.PostIT.deletePostTests} as this method is used for the deletion of
 * all post types.
 */
public class RepostIT extends BaseGraphQLIntegrationTest {
  List<String> handles = List.of("example1", "example2", "example3");
  List<User> users;

  List<Post> posts;
  Post sharedPost;

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
    posts = seedPosts(List.of("original post"), List.of(users.get(1)), postRepository);
    sharedPost = posts.getFirst();
  }

  @Nested
  class createRepostTests {
    @Test
    void validInput_noExistingRepost_returnsPostResponse() {
      UUID newPostId =
          authenticatedTester
              .document(
                  """
                      mutation CreateRepost($sharedPostId: ID!) {
                        createRepost(sharedPostId: $sharedPostId) {
                              code
                              success
                              post {
                                id
                                author {
                                  id
                                }
                                sharedPost {
                                  id
                                }
                              }
                            }
                          }
                      """)
              .variable("sharedPostId", sharedPost.getId())
              .execute()
              .path("createRepost")
              .matchesJson(
                  String.format(
                      """
                          {
                            "code": "200",
                            "success": true,
                            "post": {
                              "author": {
                                "id": "%s"
                              },
                              "sharedPost": {
                                "id": "%s"
                              }
                            }
                          }
                          """,
                      authenticatedUser.getId(), sharedPost.getId()))
              .path("createRepost.post.id")
              .entity(UUID.class)
              .get();

      // original post + repost
      assertThat(postRepository.findAll()).hasSize(2);

      // Clean up
      postRepository.deleteById(newPostId);
    }

    @Test
    void validInput_existingDeletedRepost_returnsPostResponse() {
      // Create a repost with the same sharedPostId and authorId
      Post repost = seedRepost(sharedPost.getId(), authenticatedUser.getId(), postRepository);
      setPostStatusDeleted(repost, postRepository);

      authenticatedTester
          .document(
              """
                  mutation CreateRepost($sharedPostId: ID!) {
                    createRepost(sharedPostId: $sharedPostId) {
                      code
                      success
                      post {
                        id
                        author {
                          id
                        }
                        sharedPost {
                          id
                        }
                      }
                    }
                  }
                  """)
          .variable("sharedPostId", sharedPost.getId())
          .execute()
          .path("createRepost")
          .matchesJson(
              String.format(
                  """
                      {
                        "code": "200",
                        "success": true,
                        "post": {
                          "id": "%s",
                          "author": {
                            "id": "%s"
                          },
                          "sharedPost": {
                            "id": "%s"
                          }
                        }
                      }
                      """,
                  repost.getId(), authenticatedUser.getId(), sharedPost.getId()));

      // original post + repost
      assertThat(postRepository.findAll()).hasSize(2);

      // Clean up
      postRepository.deleteById(repost.getId());
    }

    @Test
    void invalidInput_existingActiveRepost_returnsDuplicateRepost() {
      // Create a repost with the same sharedPostId and authorId
      Post repost = seedRepost(sharedPost.getId(), authenticatedUser.getId(), postRepository);

      authenticatedTester
          .document(
              """
                  mutation CreateRepost($sharedPostId: ID!) {
                    createRepost(sharedPostId: $sharedPostId) {
                      code
                      success
                      post {
                        id
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("sharedPostId", sharedPost.getId())
          .execute()
          .path("createRepost")
          .matchesJson(
              """
                  {
                    "code": "400",
                    "success": false,
                    "post": null,
                    "errors": [{
                      "field": "sharedPostId",
                      "message": "Repost already exists"
                    }]
                  }
                  """);

      // original post + repost
      assertThat(postRepository.findAll()).hasSize(2);

      // Clean up
      postRepository.deleteById(repost.getId());
    }

    @Test
    void invalidInput_postIdDoesNotExist_returnsPostNotFound() {
      authenticatedTester
          .document(
              """
                  mutation CreateRepost($sharedPostId: ID!) {
                    createRepost(sharedPostId: $sharedPostId) {
                      code
                      success
                      post {
                        author {
                          id
                        }
                        sharedPost {
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
          .variable("sharedPostId", UUID.randomUUID())
          .execute()
          .path("createRepost")
          .matchesJson(
              """
                  {
                    "code": "404",
                    "success": false,
                    "post": null,
                    "errors": [{
                      "field": "sharedPostId",
                      "message": "Original post cannot be found"
                    }]
                  }
                  """);

      // Only the original post
      assertThat(postRepository.findAll()).hasSize(1);
    }

    @Test
    void invalidInput_inputMissing_returnsConstraintViolation() {
      authenticatedTester
          .document(
              """
                  mutation CreateRepost($sharedPostId: ID!) {
                    createRepost(sharedPostId: $sharedPostId) {
                      code
                      success
                      post {
                        author {
                          id
                        }
                        sharedPost {
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
          .variable("sharedPostId", null)
          .execute()
          .errors()
          .satisfy(
              errors -> {
                assertThat(errors).hasSize(1);

                assertThat(errors)
                    .anySatisfy(
                        error -> {
                          assertThat(error.getMessage())
                              .contains("sharedPostId")
                              .contains("'sharedPostId' has an invalid value");
                          assertThat(error.getExtensions())
                              .containsEntry("classification", "ValidationError");
                        });
              });

      // Only the original post
      assertThat(postRepository.findAll()).hasSize(1);
    }
  }

  @Nested
  class createQuoteTests {
    @Test
    void validInput_noExistingQuote_returnsPostResponse() {
      Map<String, Object> createQuoteInput = new HashMap<>();
      createQuoteInput.put("sharedPostId", sharedPost.getId());
      createQuoteInput.put("messageContent", "this is a valid quote");

      UUID newPostId =
          authenticatedTester
              .document(
                  """
                      mutation CreateQuote($input: CreateQuoteInput!) {
                        createQuote(input: $input) {
                              code
                              success
                              post {
                                id
                                author {
                                  id
                                }
                                sharedPost {
                                  id
                                }
                              }
                            }
                          }
                      """)
              .variable("input", createQuoteInput)
              .execute()
              .path("createQuote")
              .matchesJson(
                  String.format(
                      """
                          {
                            "code": "200",
                            "success": true,
                            "post": {
                              "author": {
                                "id": "%s"
                              },
                              "sharedPost": {
                                "id": "%s"
                              }
                            }
                          }
                          """,
                      authenticatedUser.getId(), sharedPost.getId()))
              .path("createQuote.post.id")
              .entity(UUID.class)
              .get();

      // original post + repost
      assertThat(postRepository.findAll()).hasSize(2);

      // Clean up
      postRepository.deleteById(newPostId);
    }

    @Test
    void invalidInput_postIdDoesNotExist_returnsPostNotFound() {
      Map<String, Object> createQuoteInput = new HashMap<>();
      createQuoteInput.put("sharedPostId", UUID.randomUUID());
      createQuoteInput.put("messageContent", "this is the message");

      authenticatedTester
          .document(
              """
                  mutation CreateQuote($input: CreateQuoteInput!) {
                    createQuote(input: $input) {
                          code
                          success
                          post {
                            id
                            author {
                              id
                            }
                            sharedPost {
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
          .variable("input", createQuoteInput)
          .execute()
          .path("createQuote")
          .matchesJson(
              """
                  {
                    "code": "404",
                    "success": false,
                    "post": null,
                    "errors": [{
                      "field": "sharedPostId",
                      "message": "Shared post cannot be found"
                    }]
                  }
                  """);

      // Only the original post
      assertThat(postRepository.findAll()).hasSize(1);
    }

    @Test
    void invalidInput_inputMissing_returnsConstraintViolation() {
      authenticatedTester
          .document(
              """
                  mutation CreateQuote($input: CreateQuoteInput!) {
                    createQuote(input: $input) {
                          code
                          success
                          post {
                            id
                            author {
                              id
                            }
                            sharedPost {
                              id
                            }
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
                              .contains("input")
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
