package com.xclone.integration.reply;

import static com.xclone.support.helpers.PostHelpers.createPostContents;
import static org.assertj.core.api.Assertions.assertThat;

import com.xclone.integration.base.BaseIntegrationTest;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.support.helpers.PostHelpers;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import java.time.ZoneOffset;
import java.util.Arrays;
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
public class ReplyIT extends BaseIntegrationTest {
  @Autowired UserRepository userRepository;
  @Autowired PostRepository postRepository;
  @Autowired AuthHelpers authHelpers;
  @Autowired HttpGraphQlTester authenticatedTester;

  List<String> handles = List.of("example1", "example2", "example3");
  List<User> users;
  User authenticatedUser;

  List<String> messageContents = createPostContents(6);
  List<Post> posts;

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

  void cleanupDBs() {
    // Flushes DBs
    postRepository.deleteAll();
    userRepository.deleteAll();
  }

  /**
   * Posts are reassigned in each test suite. To not trigger a FK error when deleting, the posts are
   * deleted from newest -> oldest.
   */
  @AfterEach
  void cleanup() {
    PostHelpers.deletePostsInDescendingOrder(posts, postRepository);
  }

  @Nested
  class getReplyThreadTests {

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
                  queriedPost {
                    id
                  }
                 }
              }
              """)
          .variable("postId", posts.getFirst().getId())
          .execute()
          .path("getReplyThread.ancestors")
          .valueIsNull()
          .path("getReplyThread.siblings")
          .valueIsNull()
          .path("getReplyThread.queriedPost.id")
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
                  queriedPost {
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
}
