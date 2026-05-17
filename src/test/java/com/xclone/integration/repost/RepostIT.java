package com.xclone.integration.repost;

import static com.xclone.support.helpers.PostHelpers.seedPosts;
import static com.xclone.support.helpers.PostHelpers.seedRepost;
import static com.xclone.support.helpers.PostHelpers.setPostStatusDeleted;
import static org.assertj.core.api.Assertions.assertThat;

import com.xclone.integration.base.BaseIntegrationTest;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

@AutoConfigureHttpGraphQlTester
@Import(AuthHelpers.class)
public class RepostIT extends BaseIntegrationTest {
  @Autowired UserRepository userRepository;
  @Autowired PostRepository postRepository;
  @Autowired AuthHelpers authHelpers;
  @Autowired HttpGraphQlTester authenticatedTester;

  List<String> handles = List.of("example1", "example2", "example3");
  List<User> users;
  User authenticatedUser;

  List<Post> posts;
  Post quotedPost;

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
    posts = seedPosts(List.of("original post"), List.of(users.get(1)), postRepository);
    quotedPost = posts.getFirst();
  }

  void cleanupDBs() {
    // Flushes DBs
    postRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  class createRepostTests {
    @Test
    void validInput_noExistingRepost_returnsPostResponse() {
      // TODO: ADD QUOTED POST ASSERTION
      UUID newPostId =
          authenticatedTester
              .document(
                  """
                      mutation CreateRepost($originalPostId: ID!) {
                        createRepost(originalPostId: $originalPostId) {
                              code
                              success
                              post {
                                id
                                author {
                                  id
                                }
                              }
                            }
                          }
                      """)
              .variable("originalPostId", quotedPost.getId())
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
                              }
                            }
                          }
                          """,
                      authenticatedUser.getId()))
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
      // TODO: ADD QUOTED POST ASSERTION
      // Create a repost with the same originalPostId and authorId
      Post repost = seedRepost(quotedPost.getId(), authenticatedUser.getId(), postRepository);
      setPostStatusDeleted(repost, postRepository);

      authenticatedTester
          .document(
              """
                  mutation CreateRepost($originalPostId: ID!) {
                    createRepost(originalPostId: $originalPostId) {
                      code
                      success
                      post {
                        id
                        author {
                          id
                        }
                      }
                    }
                  }
                  """)
          .variable("originalPostId", quotedPost.getId())
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
                          }
                        }
                      }
                      """,
                  repost.getId(), authenticatedUser.getId()));

      // original post + repost
      assertThat(postRepository.findAll()).hasSize(2);

      // Clean up
      postRepository.deleteById(repost.getId());
    }

    @Test
    void invalidInput_existingActiveRepost_returnsDuplicateRepost() {
      // Create a repost with the same originalPostId and authorId
      Post repost = seedRepost(quotedPost.getId(), authenticatedUser.getId(), postRepository);

      authenticatedTester
          .document(
              """
                  mutation CreateRepost($originalPostId: ID!) {
                    createRepost(originalPostId: $originalPostId) {
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
          .variable("originalPostId", quotedPost.getId())
          .execute()
          .path("createRepost")
          .matchesJson(
              """
                  {
                    "code": "400",
                    "success": false,
                    "post": null,
                    "errors": [{
                      "field": "originalPostId",
                      "message": "Repost already exists"
                    }]
                    }
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
                  mutation CreateRepost($originalPostId: ID!) {
                    createRepost(originalPostId: $originalPostId) {
                      code
                      success
                      post {
                        author {
                          id
                        }
                        quotedPost {
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
          .variable("originalPostId", UUID.randomUUID())
          .execute()
          .path("createRepost")
          .matchesJson(
              """
                  {
                    "code": "404",
                    "success": false,
                    "post": null,
                    "errors": [{
                      "field": "postId",
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
                  mutation CreateRepost($originalPostId: ID!) {
                    createRepost(originalPostId: $originalPostId) {
                      code
                      success
                      post {
                        author {
                          id
                        }
                        quotedPost {
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
          .variable("originalPostId", null)
          .execute()
          .errors()
          .satisfy(
              errors -> {
                assertThat(errors).hasSize(1);

                assertThat(errors)
                    .anySatisfy(
                        error -> {
                          assertThat(error.getMessage())
                              .contains("originalPostId")
                              .contains("'originalPostId' has an invalid value");
                          assertThat(error.getExtensions())
                              .containsEntry("classification", "ValidationError");
                        });
              });

      // Only the original post
      assertThat(postRepository.findAll()).hasSize(1);
    }
  }
}
