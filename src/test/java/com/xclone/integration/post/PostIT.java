package com.xclone.integration.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xclone.common.enums.Status;
import com.xclone.common.mutation.DeleteResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.follow.repository.FollowRepository;
import com.xclone.integration.base.BaseIntegrationTest;
import com.xclone.like.repository.LikeRepository;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.mutation.PostResponse;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.support.helpers.FollowHelpers;
import com.xclone.support.helpers.LikeHelpers;
import com.xclone.support.helpers.PostHelpers;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
import com.xclone.user.repository.UserRepository;
import com.xclone.validation.ValidationConstants;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
public class PostIT extends BaseIntegrationTest {
  @Autowired UserRepository userRepository;
  @Autowired PostRepository postRepository;
  @Autowired FollowRepository followRepository;
  @Autowired LikeRepository likeRepository;
  @Autowired AuthHelpers authHelpers;
  @Autowired HttpGraphQlTester authenticatedTester;

  List<String> handles = List.of("example1", "example2", "example3");
  List<User> users;
  User authenticatedUser;

  List<String> messageContents = List.of("one for sorrow", "two for joy", "three for a girl");
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
    // User 0 follows user 1
    FollowHelpers.seedFollow(followRepository, users.getFirst(), users.get(1));
    // Create posts:
    // - authenticated user authors post at index-0
    // - user at index-1 authors post at index-1
    // - user at index-2 authors post at index-2
    posts = PostHelpers.seedPosts(messageContents, users, postRepository);
  }

  void cleanupDBs() {
    // Flushes DBs
    likeRepository.deleteAll();
    postRepository.deleteAll();
    followRepository.deleteAll();
    userRepository.deleteAll();
  }

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
                          createdAt
                        }
                      }
                    }
                  }
                  """)
          .execute()
          .path("feed.edges[*].node")
          .entityList(PostProfile.class)
          .satisfies(
              nodes -> {
                assertThat(nodes).hasSize(2);

                // Posts are sorted descendingly by created date
                // post 2 is first as it was created last
                PostProfile firstPost = nodes.getFirst();
                PostProfile secondPost = nodes.getLast();
                assertThat(firstPost.createdAt()).isAfter(secondPost.createdAt());

                assertThat(firstPost.messageContent()).isEqualTo(posts.get(2).getMessageContent());
                assertThat(secondPost.messageContent()).isEqualTo(posts.get(1).getMessageContent());
              });
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

  @Nested
  class createPostTests {
    @BeforeEach
    void removeAllPosts() {
      postRepository.deleteAll();
    }

    @Test
    void validInput_returnsPostResponse() {
      String messageContent = "hello this is my new post";

      assertThat(postRepository.findAll()).hasSize(0);

      authenticatedTester
          .document(
              """
                  mutation CreatePost($input: CreatePostInput!) {
                    createPost(input: $input) {
                      code
                      success
                      post {
                        messageContent
                        author {
                          id
                        }
                      }
                    }
                  }
                  """)
          .variable("input", Map.of("messageContent", messageContent))
          .execute()
          .path("createPost")
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
                          }
                        }
                      }
                      """,
                  messageContent, authenticatedUser.getId()));

      assertThat(postRepository.findAll()).hasSize(1);
    }

    @Test
    void invalidInput_messageTooLong_returnsConstraintViolation() {
      String longMessage = "this message will be over 280 characters".repeat(20);

      assertThat(postRepository.findAll()).hasSize(0);

      authenticatedTester
          .document(
              """
                  mutation CreatePost($input: CreatePostInput!) {
                    createPost(input: $input) {
                      code
                      success
                      post {
                        messageContent
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("input", Map.of("messageContent", longMessage))
          .execute()
          .path("createPost")
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

      assertThat(postRepository.findAll()).hasSize(0);
    }

    @Test
    void invalidInput_inputMissing_returnsConstraintViolation() {
      assertThat(postRepository.findAll()).hasSize(0);

      authenticatedTester
          .document(
              """
                  mutation CreatePost($input: CreatePostInput!) {
                    createPost(input: $input) {
                      code
                      success
                      post {
                        messageContent
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
                              .contains("CreatePostInput")
                              .contains("'input' has an invalid value");
                          assertThat(error.getExtensions())
                              .containsEntry("classification", "ValidationError");
                        });
              });

      assertThat(postRepository.findAll()).hasSize(0);
    }
  }

  @Nested
  class updatePostContentTests {
    @Test
    void validInput_returnsPostResponse() {
      String updatedPostContent = "new content";

      // authenticated user is the author of post at index 0 in posts
      PostResponse response =
          authenticatedTester
              .document(
                  """
                      mutation UpdatePost($input: UpdatePostInput!) {
                        updatePostContent(input: $input) {
                          code
                          success
                          post {
                            messageContent
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable(
                  "input",
                  Map.of("id", posts.getFirst().getId(), "messageContent", updatedPostContent))
              .execute()
              .path("updatePostContent")
              .entity(PostResponse.class)
              .get();

      assertThat(response.code()).isEqualTo("200");
      assertTrue(response.success());
      assertNull(response.errors());
      assertThat(response.post().messageContent()).isEqualTo(updatedPostContent);
    }

    @Test
    void invalidInput_notPostAuthor_returnsErrors() {
      String updatedPostContent = "new content";

      // authenticated user is the author of post at index 0 in posts
      PostResponse response =
          authenticatedTester
              .document(
                  """
                      mutation UpdatePost($input: UpdatePostInput!) {
                        updatePostContent(input: $input) {
                          code
                          success
                          post {
                            messageContent
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable(
                  "input",
                  Map.of("id", posts.getLast().getId(), "messageContent", updatedPostContent))
              .execute()
              .path("updatePostContent")
              .entity(PostResponse.class)
              .get();

      assertThat(response.code()).isEqualTo("403");
      assertFalse(response.success());
      assertNull(response.post());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactlyInAnyOrder(
              tuple("updatePostContent", "Only the author can update the post"));
    }

    @Test
    void invalidInput_postIdDoesNotExist_returnsErrors() {
      String updatedPostContent = "new content";

      PostResponse response =
          authenticatedTester
              .document(
                  """
                      mutation UpdatePost($input: UpdatePostInput!) {
                        updatePostContent(input: $input) {
                          code
                          success
                          post {
                            messageContent
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable(
                  "input", Map.of("id", UUID.randomUUID(), "messageContent", updatedPostContent))
              .execute()
              .path("updatePostContent")
              .entity(PostResponse.class)
              .get();

      assertThat(response.code()).isEqualTo("404");
      assertFalse(response.success());
      assertNull(response.post());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactlyInAnyOrder(tuple("updatePostContent", "Post does not exist"));
    }

    @Test
    void invalidInput_invalidUuid_returnsErrors() {
      String updatedPostContent = "new content";

      authenticatedTester
          .document(
              """
                  mutation UpdatePost($input: UpdatePostInput!) {
                    updatePostContent(input: $input) {
                      code
                      success
                      post {
                        messageContent
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("input", Map.of("id", "not a valid UUID", "messageContent", updatedPostContent))
          .execute()
          .errors()
          .satisfy(
              errors -> {
                assertThat(errors).hasSize(2);

                assertThat(errors)
                    .anySatisfy(
                        error -> {
                          assertThat(error.getMessage()).contains("not a valid UUID");
                          assertThat(error.getExtensions())
                              .containsEntry("classification", "BAD_REQUEST");
                        });
                assertThat(errors)
                    .anySatisfy(
                        error -> {
                          assertThat(error.getMessage())
                              .contains("updatePostContent")
                              .contains("returned a null value");
                          assertThat(error.getExtensions())
                              .containsEntry("classification", "NullValueInNonNullableField");
                        });
              });
    }
  }

  @Nested
  class deletePostTests {

    @Test
    void validInput_returnsDeleteResponse() {
      // authenticated user is the author of post at index 0 in posts
      DeleteResponse response =
          authenticatedTester
              .document(
                  """
                      mutation DeletePost($id: ID!) {
                        deletePost(postId: $id) {
                          code
                          success
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("id", posts.getFirst().getId())
              .execute()
              .path("deletePost")
              .entity(DeleteResponse.class)
              .get();

      Post deletedPost = postRepository.findById(posts.getFirst().getId()).orElseThrow();

      assertThat(response.code()).isEqualTo("200");
      assertTrue(response.success());
      assertNull(response.errors());
      assertThat(deletedPost.getStatus()).isEqualTo(Status.DELETED);
    }

    @Test
    void invalidInput_notPostAuthor_returnsErrors() {
      // authenticated user is the author of post at index 0 in posts
      DeleteResponse response =
          authenticatedTester
              .document(
                  """
                      mutation DeletePost($id: ID!) {
                        deletePost(postId: $id) {
                          code
                          success
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("id", posts.getLast().getId())
              .execute()
              .path("deletePost")
              .entity(DeleteResponse.class)
              .get();

      assertThat(response.code()).isEqualTo("403");
      assertFalse(response.success());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactlyInAnyOrder(tuple("deletePost", "Only the author can delete the post"));
    }

    @Test
    void invalidInput_postIdDoesNotExist_returnsErrors() {
      DeleteResponse response =
          authenticatedTester
              .document(
                  """
                      mutation DeletePost($id: ID!) {
                        deletePost(postId: $id) {
                          code
                          success
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("id", UUID.randomUUID())
              .execute()
              .path("deletePost")
              .entity(DeleteResponse.class)
              .get();

      assertThat(response.code()).isEqualTo("404");
      assertFalse(response.success());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactlyInAnyOrder(tuple("deletePost", "Post does not exist"));
    }

    @Test
    void invalidInput_invalidUuid_returnsErrors() {
      authenticatedTester
          .document(
              """
                  mutation DeletePost($id: ID!) {
                    deletePost(postId: $id) {
                      code
                      success
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("id", "not a valid UUID")
          .execute()
          .errors()
          .satisfy(
              errors -> {
                assertThat(errors).hasSize(2);

                assertThat(errors)
                    .anySatisfy(
                        error -> {
                          assertThat(error.getMessage()).contains("not a valid UUID");
                          assertThat(error.getExtensions())
                              .containsEntry("classification", "BAD_REQUEST");
                        });
                assertThat(errors)
                    .anySatisfy(
                        error -> {
                          assertThat(error.getMessage())
                              .contains("deletePost")
                              .contains("returned a null value");
                          assertThat(error.getExtensions())
                              .containsEntry("classification", "NullValueInNonNullableField");
                        });
              });
    }
  }

  @Nested
  class likeTests {
    @Nested
    class likeCountTests {
      @Test
      void fetchingIndividualPost_noLikes_postHasLikeCount() {
        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        likeCount
                      }
                    }
                    """)
            .variable("postId", posts.getFirst().getId())
            .execute()
            .path("getPost")
            .matchesJson(
                """
                    {
                      "likeCount": 0
                    }
                    """);
      }

      @Test
      void fetchingIndividualPost_hasLikes_postHasLikeCount() {
        // authenticated users post (first post) has 2 likes: user 1 and user 2
        List<Post> postsToLike = Collections.nCopies(2, posts.getFirst());
        List<User> usersToLike = List.of(users.get(1), users.get(2));
        LikeHelpers.seedLikes(postsToLike, usersToLike, likeRepository);

        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        likeCount
                      }
                    }
                    """)
            .variable("postId", posts.getFirst().getId())
            .execute()
            .path("getPost")
            .matchesJson(
                """
                    {
                      "likeCount": 2
                    }
                    """);
      }

      @Test
      void fetchingIndividualPost_hasLikesFromDeletedUser_postHasLikeCount() {
        // authenticated users post (first post) has 2 likes: user 1 and user 2
        List<Post> postsToLike = Collections.nCopies(2, posts.getFirst());
        List<User> usersToLike = List.of(users.get(1), users.get(2));
        LikeHelpers.seedLikes(postsToLike, usersToLike, likeRepository);
        // user 2 is set to deleted
        User userToDelete = users.get(2);
        userToDelete.setStatus(UserStatus.DELETED);
        userRepository.saveAndFlush(userToDelete);

        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        likeCount
                      }
                    }
                    """)
            .variable("postId", posts.getFirst().getId())
            .execute()
            .path("getPost")
            .matchesJson(
                """
                    {
                      "likeCount": 1
                    }
                    """);
      }

      @Test
      void fetchingFeed_eachPostHasLikeCount() {
        // user 0 follows user 1 + user 2 post initialisation
        FollowHelpers.seedFollow(followRepository, authenticatedUser, users.get(2));
        // user 1 authors post 1; user 2 authors post 2;
        // post 1 has 1 like
        LikeHelpers.seedLikes(List.of(posts.get(1)), List.of(authenticatedUser), likeRepository);
        // post 2 has 2 likes
        LikeHelpers.seedLikes(
            List.of(posts.get(2), posts.get(2)),
            List.of(authenticatedUser, users.get(1)),
            likeRepository);

        authenticatedTester
            .document(
                """
                    {
                      feed {
                        edges {
                          node {
                            id
                            likeCount
                            createdAt
                          }
                        }
                      }
                    }
                    """)
            .execute()
            .path("feed.edges[*].node")
            .entityList(PostProfile.class)
            .satisfies(
                nodes -> {
                  assertThat(nodes).hasSize(2);

                  // Posts are sorted descendingly by created date
                  // post 2 is first as it was created last
                  PostProfile firstPost = nodes.getFirst();
                  PostProfile secondPost = nodes.getLast();
                  assertThat(firstPost.createdAt()).isAfter(secondPost.createdAt());

                  assertThat(firstPost.id()).isEqualTo(posts.get(2).getId());
                  assertThat(secondPost.id()).isEqualTo(posts.get(1).getId());
                })
            .path("feed.edges[0].node.likeCount")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("feed.edges[1].node.likeCount")
            .entity(Integer.class)
            .isEqualTo(1);
      }
    }

    @Nested
    class likedByMeTests {
      @Test
      void fetchingIndividualPost_noLikes_likedByMeFalse() {
        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        likedByMe
                      }
                    }
                    """)
            .variable("postId", posts.getFirst().getId())
            .execute()
            .path("getPost")
            .matchesJson(
                """
                    {
                      "likedByMe": false
                    }
                    """);
      }

      @Test
      void fetchingIndividualPost_hasLikeByMe_likedByMeTrue() {
        // first post has 3 likes
        List<Post> postsToLike = Collections.nCopies(3, posts.getFirst());
        List<User> usersToLike = List.of(users.get(0), users.get(1), users.get(2));
        LikeHelpers.seedLikes(postsToLike, usersToLike, likeRepository);

        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        likedByMe
                      }
                    }
                    """)
            .variable("postId", posts.getFirst().getId())
            .execute()
            .path("getPost")
            .matchesJson(
                """
                    {
                      "likedByMe": true
                    }
                    """);
      }

      @Test
      void fetchingFeed_onePostLikedByMe() {
        // user 0 follows user 1 + user 2 post initialisation
        FollowHelpers.seedFollow(followRepository, authenticatedUser, users.get(2));
        // user 1 authors post 1; user 2 authors post 2;
        // post 1 has 1 like; not liked by authenticated user
        LikeHelpers.seedLikes(List.of(posts.get(1)), List.of(users.get(2)), likeRepository);
        // post 2 has 2 likes; liked by authenticated user
        LikeHelpers.seedLikes(
            List.of(posts.get(2), posts.get(2)),
            List.of(authenticatedUser, users.get(1)),
            likeRepository);

        authenticatedTester
            .document(
                """
                    {
                      feed {
                        edges {
                          node {
                            id
                            likedByMe
                            createdAt
                          }
                        }
                      }
                    }
                    """)
            .execute()
            .path("feed.edges[*].node")
            .entityList(PostProfile.class)
            .satisfies(
                nodes -> {
                  assertThat(nodes).hasSize(2);

                  // Posts are sorted descendingly by created date
                  // post 2 is first as it was created last
                  PostProfile firstPost = nodes.getFirst();
                  PostProfile secondPost = nodes.getLast();
                  assertThat(firstPost.createdAt()).isAfter(secondPost.createdAt());

                  assertThat(firstPost.id()).isEqualTo(posts.get(2).getId());
                  assertThat(secondPost.id()).isEqualTo(posts.get(1).getId());
                })
            .path("feed.edges[0].node.likedByMe")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("feed.edges[1].node.likedByMe")
            .entity(Boolean.class)
            .isEqualTo(false);
      }

      @Test
      void fetchingFeed_everyPostLikedByMe() {
        // user 0 follows user 1 + user 2 post initialisation
        FollowHelpers.seedFollow(followRepository, authenticatedUser, users.get(2));
        // user 1 authors post 1; user 2 authors post 2;
        // post 1 has 1 like; not liked by authenticated user
        LikeHelpers.seedLikes(List.of(posts.get(1)), List.of(authenticatedUser), likeRepository);
        // post 2 has 2 likes; liked by authenticated user
        LikeHelpers.seedLikes(
            List.of(posts.get(2), posts.get(2)),
            List.of(authenticatedUser, users.get(1)),
            likeRepository);

        authenticatedTester
            .document(
                """
                    {
                      feed {
                        edges {
                          node {
                            id
                            likedByMe
                            createdAt
                          }
                        }
                      }
                    }
                    """)
            .execute()
            .path("feed.edges[*].node")
            .entityList(PostProfile.class)
            .satisfies(
                nodes -> {
                  assertThat(nodes).hasSize(2);

                  // Posts are sorted descendingly by created date
                  // post 2 is first as it was created last
                  PostProfile firstPost = nodes.getFirst();
                  PostProfile secondPost = nodes.getLast();
                  assertThat(firstPost.createdAt()).isAfter(secondPost.createdAt());

                  assertThat(firstPost.id()).isEqualTo(posts.get(2).getId());
                  assertThat(secondPost.id()).isEqualTo(posts.get(1).getId());
                })
            .path("feed.edges[0].node.likedByMe")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("feed.edges[1].node.likedByMe")
            .entity(Boolean.class)
            .isEqualTo(true);
      }
    }

    @Nested
    class likesTests {

      @Test
      void getLikes_notPostAuthor_returnsNotPostAuthor() {
        // authenticated user is only the author of the 0-index in posts
        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        likes {
                          edges {
                            node {
                              id
                            }
                          }
                        }
                      }
                    }
                    """)
            .variable("postId", posts.getLast().getId())
            .execute()
            .errors()
            .satisfy(
                errors -> {
                  assertThat(errors).hasSize(1);
                  assertThat(errors.getFirst().getMessage()).isEqualTo("Not post author");
                });
      }

      @Test
      void getLikesNoCursor_postAuthor_returnsLikes() {
        // authenticated user is only the author of the 0-index in posts
        // 0-index in posts has 2 likes
        List<Post> postsToLike = Collections.nCopies(2, posts.getFirst());
        List<User> usersToLike = List.of(users.get(1), users.get(2));
        LikeHelpers.seedLikes(postsToLike, usersToLike, likeRepository);

        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        likes {
                          edges {
                            node {
                              id
                            }
                          }
                        }
                      }
                    }
                    """)
            .variable("postId", posts.getFirst().getId())
            .execute()
            .path("getPost.likes.edges[*].node")
            .entityList(UserProfile.class)
            .satisfies(
                usersThatLikedPost -> {
                  assertThat(usersThatLikedPost).hasSize(2);

                  UserProfile firstLike = usersThatLikedPost.getFirst();
                  UserProfile secondLike = usersThatLikedPost.get(1);

                  // user 2 liked the post last; therefore, it should be the first user returned
                  // likes are sorted in created descendingly
                  assertThat(firstLike.id()).isEqualTo(users.get(2).getId());
                  assertThat(secondLike.id()).isEqualTo(users.get(1).getId());
                });
      }

      @Test
      void getLikesWithValidCursor_postAuthor_returnsUserConnection() {
        // authenticated user is only the author of the 0-index in posts
        // 0-index in posts has 2 likes
        List<Post> postsToLike = Collections.nCopies(2, posts.getFirst());
        List<User> usersToLike = List.of(users.get(1), users.get(2));
        LikeHelpers.seedLikes(postsToLike, usersToLike, likeRepository);

        // Split the two posts across two pages with first = 1
        String endCursor =
            authenticatedTester
                .document(
                    """
                        query GetPost($postId: ID!){
                          getPost(postId: $postId) {
                            likes(first: 1) {
                              edges {
                                node {
                                  id
                                }
                              }
                              pageInfo {
                                hasNextPage
                                endCursor
                              }
                            }
                          }
                        }
                        """)
                .variable("postId", posts.getFirst().getId())
                .execute()
                .path("getPost.likes.edges[*].node")
                .entityList(UserProfile.class)
                .hasSize(1)
                .path("getPost.likes.edges[0].node.id")
                .entity(UUID.class)
                // likes are sorted by created date descendingly; 2nd like is first
                .isEqualTo(usersToLike.getLast().getId())
                .path("getPost.likes.pageInfo.hasNextPage")
                .entity(Boolean.class)
                .isEqualTo(true)
                .path("getPost.likes.pageInfo.endCursor")
                .entity(String.class)
                .get();

        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!, $cursor: String){
                      getPost(postId: $postId) {
                        likes(first: 1, after: $cursor) {
                          edges {
                            node {
                              id
                            }
                          }
                          pageInfo {
                            hasNextPage
                          }
                        }
                      }
                    }
                    """)
            .variable("postId", posts.getFirst().getId())
            .variable("cursor", endCursor)
            .execute()
            .path("getPost.likes.edges[*].node")
            .entityList(UserProfile.class)
            .hasSize(1)
            .path("getPost.likes.edges[0].node.id")
            .entity(UUID.class)
            // likes are sorted by created date descendingly; 1st like is second
            .isEqualTo(usersToLike.getFirst().getId())
            .path("getPost.likes.pageInfo.hasNextPage")
            .entity(Boolean.class)
            .isEqualTo(false);
      }

      @Test
      void getLikes_noLikes_postAuthor_returnsEmptyUserConnection() {
        // authenticated user authors a post with no likes
        Post postWithNoLikes =
            PostHelpers.seedPosts(
                    List.of("postWithNoLikes"), List.of(authenticatedUser), postRepository)
                .getFirst();

        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        likes {
                          edges {
                            node {
                              id
                            }
                          }
                        }
                      }
                    }
                    """)
            .variable("postId", postWithNoLikes.getId())
            .execute()
            .path("getPost.likes.edges[*].node")
            .entityList(UserProfile.class)
            .hasSize(0);
      }
    }
  }
}
