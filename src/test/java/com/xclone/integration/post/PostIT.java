package com.xclone.integration.post;

import com.xclone.follow.repository.FollowRepository;
import com.xclone.integration.base.BaseIntegrationTest;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.support.helpers.FollowHelpers;
import com.xclone.support.helpers.PostHelpers;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

@AutoConfigureHttpGraphQlTester
@Import(AuthHelpers.class)
public class PostIT extends BaseIntegrationTest {
  @Autowired UserRepository userRepository;
  @Autowired PostRepository postRepository;
  @Autowired FollowRepository followRepository;
  @Autowired AuthHelpers authHelpers;
  @Autowired HttpGraphQlTester authenticatedTester;

  List<String> handles = List.of("example1", "example2", "example3");
  List<User> users;
  User authenticatedUser;

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
    // User 0 follows user 1
    FollowHelpers.seedFollow(followRepository, users.getFirst(), users.get(1));
    // Create posts
    posts = PostHelpers.seedPosts(messageContents, users, postRepository);
  }

  @AfterEach
  void cleanup() {
    // Flushes DBs
    postRepository.deleteAll();
    followRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  class schemaMappingTests {}

  @Nested
  class getPostTests {
    @Test
    void getOwnPost_returnsPostProfile() {
      authenticatedTester
          .document(
              """
              query GetPost($id: ID!) {
                getPost(postId: $id) {
                  messageContent
                  author {
                    id
                  }
                }
              }
              """)
          .variable("id", posts.getFirst().getId())
          .execute()
          .path("getPost.messageContent")
          .entity(String.class)
          .isEqualTo(messageContents.getFirst())
          .path("getPost.author.id")
          .entity(UUID.class)
          .isEqualTo(authenticatedUser.getId());
    }

    @Test
    void getOthersPost_returnsPostProfile() {
      authenticatedTester
          .document(
              """
              query GetPost($id: ID!) {
                getPost(postId: $id) {
                  messageContent
                  author {
                    id
                  }
                }
              }
              """)
          .variable("id", posts.get(1).getId())
          .execute()
          .path("getPost.messageContent")
          .entity(String.class)
          .isEqualTo(messageContents.get(1))
          .path("getPost.author.id")
          .entity(UUID.class)
          .isEqualTo(users.get(1).getId());
    }

    @Test
    void getPost_invalidUuid_returnsBindException() {
      authenticatedTester
          .document(
              """
              query GetPost($id: ID!) {
                getPost(postId: $id) {
                  messageContent
                  author {
                    id
                  }
                }
              }
              """)
          .variable("id", "not a valid UUID")
          .execute()
          .errors()
          .filter(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
          .expect(error -> error.getMessage().contains("not a valid UUID"));
    }

    @Test
    void getPost_invalidId_returnsNull() {
      authenticatedTester
          .document(
              """
              query GetPost($id: ID!) {
                getPost(postId: $id) {
                  messageContent
                  author {
                    id
                  }
                }
              }
              """)
          .variable("id", UUID.randomUUID())
          .execute()
          .path("getPost")
          .valueIsNull();
    }
  }

  @Nested
  class getFeedTests {
    @Test
    void getFeed_followsSingleUser_returnsPostConnection() {
      authenticatedTester
          .document(
              """
              {
                feed {
                  edges {
                    node {
                      messageContent
                      author {
                        id
                      }
                    }
                  }
                }
              }
              """)
          .execute()
          .path("feed")
          .matchesJson(
              String.format(
                  """
                  {
                    "edges": [
                    {
                      "node": {
                        "messageContent": "%s",
                        "author": {
                          "id": "%s"
                        }
                      }
                    }
                    ]
                  }
                  """,
                  posts.get(1).getMessageContent(), users.get(1).getId()));
    }

    @Test
    void getFeedNoCursor_followsMultipleUsers_returnsPostConnection() {
      // user 0 follows user 1 + user 2 post initialisation
      FollowHelpers.seedFollow(followRepository, authenticatedUser, users.get(2));

      authenticatedTester
          .document(
              """
              {
                feed {
                  edges {
                    node {
                      messageContent
                      author {
                        id
                      }
                    }
                  }
                }
              }
              """)
          .execute()
          .path("feed")
          .matchesJson(
              String.format(
                  """
                  {
                    "edges": [
                    {
                      "node": {
                        "messageContent": "%s",
                        "author": {
                          "id": "%s"
                        }
                      }
                    },
                    {
                      "node": {
                        "messageContent": "%s",
                        "author": {
                          "id": "%s"
                        }
                      }
                    }
                    ]
                  }
                  """,
                  posts.get(1).getMessageContent(),
                  users.get(1).getId(),
                  posts.get(2).getMessageContent(),
                  users.get(2).getId()));
    }

    @Test
    void getFeedWithValidCursor_followsMultipleUsers_returnsPostConnection() {
      // user 0 follows user 1 + user 2 post initialisation
      FollowHelpers.seedFollow(followRepository, authenticatedUser, users.get(2));
      String endCursor =
          authenticatedTester
              .document(
                  """
              query GetFeed($first: Int){
                feed(first: $first) {
                  pageInfo {
                    hasNextPage
                    endCursor
                  }
                }
              }
              """)
              .variable("first", 1)
              .execute()
              .path("feed.pageInfo.hasNextPage")
              .entity(Boolean.class)
              .isEqualTo(true)
              .path("feed.pageInfo.endCursor")
              .entity(String.class)
              .get();

      // Split the two posts across two pages with first = 1
      authenticatedTester
          .document(
              """
              query GetFeed($first: Int, $cursor: String){
                feed(first: $first, after: $cursor) {
                  edges {
                    node {
                      messageContent
                      author {
                        id
                      }
                    }
                  }
                  pageInfo {
                    hasNextPage
                  }
                }
              }
              """)
          .variable("first", 1)
          .variable("cursor", endCursor)
          .execute()
          .path("feed")
          .matchesJson(
              String.format(
                  """
                  {
                    "edges": [
                      {
                        "node": {
                          "messageContent": "%s",
                          "author": {
                            "id": "%s"
                          }
                        }
                      }
                    ],
                    "pageInfo": {
                      "hasNextPage": false
                    }
                  }
                  """,
                  // Most recent posts are displayed first
                  // Posts created in ascending numerical order
                  // Therefore older posts shown in last page request
                  posts.get(1).getMessageContent(), users.get(1).getId()));
    }

    @Test
    void getFeed_followsNoUsers_returnsEmptyPostConnection() {
      // Initialise new user that follows nobody
      User newUser = UserFixtures.createUserWithHandle("newHandle");
      User userThatFollowsNobody = userRepository.save(newUser);
      String accessToken = authHelpers.getUserAccessToken(userThatFollowsNobody.getId().toString());
      authenticatedTester =
          authenticatedTester
              .mutate()
              .headers(headers -> headers.setBearerAuth(accessToken))
              .build();

      authenticatedTester
          .document(
              """
              {
                feed {
                  edges {
                    node {
                      messageContent
                      author {
                        id
                      }
                    }
                  }
                }
              }
              """)
          .execute()
          .path("feed.edges")
          .entityList(PostProfile.class)
          .hasSize(0);
    }
  }
}
