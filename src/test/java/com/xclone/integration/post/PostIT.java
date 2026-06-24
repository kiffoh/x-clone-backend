package com.xclone.integration.post;

import static com.xclone.support.helpers.PostHelpers.createPostContents;
import static com.xclone.support.helpers.PostHelpers.deletePostsInDescendingOrder;
import static com.xclone.support.helpers.PostHelpers.seedMention;
import static com.xclone.support.helpers.PostHelpers.seedPosts;
import static com.xclone.support.helpers.PostHelpers.seedQuotes;
import static com.xclone.support.helpers.PostHelpers.setPostStatusDeleted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xclone.common.enums.Status;
import com.xclone.common.mutation.DeleteResponse;
import com.xclone.exception.GlobalGraphQlExceptionHandlerTest;
import com.xclone.exception.dto.FieldError;
import com.xclone.integration.base.BaseGraphQLIntegrationTest;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostEdge;
import com.xclone.post.dto.mutation.PostResponse;
import com.xclone.post.model.entity.Post;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.FollowHelpers;
import com.xclone.support.helpers.LikeHelpers;
import com.xclone.support.helpers.PostHelpers;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
import com.xclone.validation.ValidationConstants;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.validation.BindException;

public class PostIT extends BaseGraphQLIntegrationTest {
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
    // User 0 follows user 1
    FollowHelpers.seedFollow(followRepository, users.getFirst(), users.get(1));
    // Create posts:
    // - authenticated user authors post at index-0
    // - user at index-1 authors post at index-1
    // - user at index-2 authors post at index-2
    posts = seedPosts(messageContents, users, postRepository);
  }

  /**
   * {@link GlobalGraphQlExceptionHandlerTest#handlesBindException()} proves that a {@link
   * BindException} correctly maps to a {@link ErrorType#BAD_REQUEST}. Consequently, tests for an
   * invalid UUID for post id have been omitted.
   */
  @Nested
  class getPostTests {
    @Test
    void getOwnPost_returnsPostProfile() {
      PostProfile post =
          authenticatedTester
              .document(
                  """
                      query GetPost($id: ID!) {
                        getPost(postId: $id) {
                          author {
                            id
                          }
                          messageContent
                          createdAt
                          updatedAt
                        }
                      }
                      """)
              .variable("id", posts.getFirst().getId())
              .execute()
              .path("getPost.author.id")
              .entity(UUID.class)
              .isEqualTo(authenticatedUser.getId())
              .path("getPost")
              .entity(PostProfile.class)
              .get();

      assertThat(post.messageContent()).isEqualTo(messageContents.getFirst());
      assertThat(post.createdAt()).isExactlyInstanceOf(OffsetDateTime.class);
      assertThat(post.updatedAt()).isExactlyInstanceOf(OffsetDateTime.class);
    }

    @Test
    void getOthersPost_returnsPostProfile() {
      PostProfile post =
          authenticatedTester
              .document(
                  """
                      query GetPost($id: ID!) {
                        getPost(postId: $id) {
                          author {
                            id
                          }
                          messageContent
                          createdAt
                          updatedAt
                        }
                      }
                      """)
              .variable("id", posts.get(1).getId())
              .execute()
              .path("getPost.author.id")
              .entity(UUID.class)
              .isEqualTo(users.get(1).getId())
              .path("getPost")
              .entity(PostProfile.class)
              .get();

      assertThat(post.messageContent()).isEqualTo(messageContents.get(1));
      assertThat(post.createdAt()).isExactlyInstanceOf(OffsetDateTime.class);
      assertThat(post.updatedAt()).isExactlyInstanceOf(OffsetDateTime.class);
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

    @Test
    void getPost_postDeleted_returnsNull() {
      // Delete post 0
      setPostStatusDeleted(posts.getFirst(), postRepository);

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
                assertThat(firstPost.createdAt()).isAfterOrEqualTo(secondPost.createdAt());

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

    @Test
    void getFeed_quotesAppearInFeed_returnsPostConnection() {
      // Initialise a new quote in feed for post 1 by user 2
      Post sharedPost = posts.get(1);
      Post quote =
          seedQuotes(
                  sharedPost.getId(),
                  List.of(users.get(2)),
                  List.of(messageContents.getFirst()),
                  postRepository)
              .getFirst();
      // user 0 follows user 1 + user 2 post initialisation
      FollowHelpers.seedFollow(followRepository, authenticatedUser, users.get(2));

      authenticatedTester
          .document(
              """
                  {
                    feed {
                      edges {
                        node {
                          sharedPostId
                        }
                      }
                    }
                  }
                  """)
          .execute()
          .path("feed.edges[*].node.sharedPostId")
          .entityList(UUID.class)
          .satisfies(
              ids -> {
                assertThat(ids).hasSize(3);

                // feed is returned in created at descendingly
                assertThat(ids).containsExactly(sharedPost.getId(), null, null);
              });

      // clean up
      postRepository.delete(quote);
    }
  }

  @Nested
  class createPostTests {

    // wipes post repository so that rows in post repository can be used in assertions
    @BeforeEach
    void wipePostDB() {
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

  /**
   * {@link GlobalGraphQlExceptionHandlerTest#handlesBindException()} proves that a {@link
   * BindException} correctly maps to a {@link ErrorType#BAD_REQUEST}. Consequently, tests for an
   * invalid UUID for post id have been omitted.
   */
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
                  Map.of("postId", posts.getFirst().getId(), "messageContent", updatedPostContent))
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
                  Map.of("postId", posts.getLast().getId(), "messageContent", updatedPostContent))
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
                  "input",
                  Map.of("postId", UUID.randomUUID(), "messageContent", updatedPostContent))
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
  }

  /**
   * {@link GlobalGraphQlExceptionHandlerTest#handlesBindException()} proves that a {@link
   * BindException} correctly maps to a {@link ErrorType#BAD_REQUEST}. Consequently, tests for an
   * invalid UUID for post id have been omitted.
   */
  @Nested
  public class deletePostTests {

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
                  assertThat(firstPost.createdAt()).isAfterOrEqualTo(secondPost.createdAt());

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
                  assertThat(firstPost.createdAt()).isAfterOrEqualTo(secondPost.createdAt());

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
                  assertThat(firstPost.createdAt()).isAfterOrEqualTo(secondPost.createdAt());

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
            seedPosts(List.of("postWithNoLikes"), List.of(authenticatedUser), postRepository)
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

  @Nested
  class replyTests {
    /**
     * Posts are reassigned in each test suite. To not trigger a FK error when deleting, the posts
     * are deleted from newest -> oldest.
     */
    @AfterEach
    void cleanup() {
      deletePostsInDescendingOrder(posts, postRepository);
    }

    @Nested
    class parentTests {
      // Do I want to do a nested reply test?
      // Save this for the getReplyChain
      @BeforeEach
      void setup() {
        // parentIndexes corresponds to the index of the post;
        List<Integer> parentIndexes = Arrays.asList(null, 0, null);
        posts = PostHelpers.seedReplies(messageContents, users, parentIndexes, postRepository);
        // Reply chain:
        // null <- post 0 <- post 1
      }

      @Test
      void validReply_returnsParentForSingularReply() {
        // Reply chain:
        // null <- post 0 <- post 1

        authenticatedTester
            .document(
                """
                    query GetParent($childId: ID!) {
                      getPost(postId: $childId) {
                        parent {
                          id
                        }
                      }
                    }
                    """)
            .variable("childId", posts.get(1).getId())
            .execute()
            .path("getPost.parent.id")
            .entity(UUID.class)
            .isEqualTo(posts.get(0).getId());
      }

      @Test
      void postHasNoParent_returnsNull() {
        // Reply chain:
        // null <- post 0

        authenticatedTester
            .document(
                """
                    query GetParent($parentId: ID!) {
                      getPost(postId: $parentId) {
                        parent {
                          id
                        }
                      }
                    }
                    """)
            .variable("parentId", posts.get(0).getId())
            .execute()
            .path("getPost.parent")
            .valueIsNull();
      }

      @Test
      void parentIsDeleted_returnsNull() {
        // Reply chain:
        // null <- post 0 <- post 1
        authenticatedTester
            .document(
                """
                    query GetParent($childId: ID!) {
                      getPost(postId: $childId) {
                        parent {
                          id
                        }
                      }
                    }
                    """)
            .variable("childId", posts.get(1).getId())
            .execute()
            .path("getPost.parent.id")
            .entity(UUID.class)
            .isEqualTo(posts.get(0).getId());
        // Reply chain:
        // null <- post 0 X post 1
        setPostStatusDeleted(posts.get(0), postRepository);

        authenticatedTester
            .document(
                """
                    query GetParent($childId: ID!) {
                      getPost(postId: $childId) {
                        parent {
                          id
                        }
                      }
                    }
                    """)
            .variable("childId", posts.get(1).getId())
            .execute()
            .path("getPost.parent")
            .valueIsNull();
      }
    }

    @Nested
    class replyCountTests {
      @BeforeEach
      void setup() {
        // parentIndexes corresponds to the index of the post;
        List<Integer> parentIndexes = Arrays.asList(null, 0, 0);
        posts = PostHelpers.seedReplies(messageContents, users, parentIndexes, postRepository);
        // Reply chain:
        //                post 1
        //               /
        // null - Post 0
        //               \
        //                post 2
      }

      @Test
      void fetchingIndividualReply_noReplies() {
        // Reply chain:
        //                post 1
        //               /
        // null - Post 0
        //               \
        //                post 2

        authenticatedTester
            .document(
                """
                    query GetParent($parentId: ID!) {
                      getPost(postId: $parentId) {
                        replyCount
                      }
                    }
                    """)
            .variable("parentId", posts.get(1).getId())
            .execute()
            .path("getPost.replyCount")
            .entity(Integer.class)
            .isEqualTo(0);
      }

      @Test
      void fetchingIndividualReply_hasReplies() {
        // Reply chain:
        //                post 1
        //               /
        // null - Post 0
        //               \
        //                post 2

        authenticatedTester
            .document(
                """
                    query GetParent($parentId: ID!) {
                      getPost(postId: $parentId) {
                        replyCount
                      }
                    }
                    """)
            .variable("parentId", posts.get(0).getId())
            .execute()
            .path("getPost.replyCount")
            .entity(Integer.class)
            .isEqualTo(2);
      }

      @Test
      void fetchingIndividualReply_hasRepliesFromDeletedReplies() {
        // Reply chain:
        //                post 1
        //               /
        // null - Post 0
        //               \
        //                post 2
        authenticatedTester
            .document(
                """
                    query GetParent($parentId: ID!) {
                      getPost(postId: $parentId) {
                        replyCount
                      }
                    }
                    """)
            .variable("parentId", posts.get(0).getId())
            .execute()
            .path("getPost.replyCount")
            .entity(Integer.class)
            .isEqualTo(2);
        // Reply chain:
        //                post 1
        //               X
        // null - Post 0
        //               \
        //                post 2
        setPostStatusDeleted(posts.get(1), postRepository);

        authenticatedTester
            .document(
                """
                    query GetParent($parentId: ID!) {
                      getPost(postId: $parentId) {
                        replyCount
                      }
                    }
                    """)
            .variable("parentId", posts.get(0).getId())
            .execute()
            .path("getPost.replyCount")
            .entity(Integer.class)
            .isEqualTo(1);
      }

      List<Post> createFeed() {
        deletePostsInDescendingOrder(posts, postRepository);
        postRepository.deleteAll();
        // Reply chain:
        //                post 3
        //               /
        // null - post 0
        //               \
        //                post 4
        //
        // null - post 1 - post 5
        //
        // null - post 2
        int numberOfPosts = 6;
        List<String> feedMessageContents = createPostContents(numberOfPosts);
        return PostHelpers.seedReplies(
            feedMessageContents,
            // Original posts
            List.of(
                users.get(1),
                users.get(2),
                users.get(1),
                // replies
                users.get(0),
                users.get(2),
                users.get(1)),
            Arrays.asList(
                null,
                null,
                null,
                // replies parent posts
                0,
                0,
                1),
            postRepository);
      }

      @Test
      void fetchingFeed_eachPostHasReplyCount() {
        // After setup: user 0 follows user 1 + user 2
        FollowHelpers.seedFollow(followRepository, users.getFirst(), users.get(2));
        posts = createFeed();
        // Reply chain:
        //                post 3
        //               /
        // null - post 0
        //               \
        //                post 4
        //
        // null - post 1 - post 5
        //
        // null - post 2

        authenticatedTester
            .document(
                """
                    {
                      feed {
                          edges {
                            node {
                              id
                              sharedPostId
                              replyCount
                              parent {
                                id
                              }
                            }
                          }
                      }
                    }
                    """)
            .execute()
            .path("feed.edges[*].node.id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  // Reply chain:
                  //                post 3
                  //               /
                  // null - post 0
                  //               \
                  //                post 4
                  //
                  // null - post 1 - post 5
                  //
                  // null - post 2
                  assertThat(ids).hasSize(3);

                  // feed shows most recent posts first
                  assertThat(ids.get(0)).isEqualTo(posts.get(2).getId());
                  assertThat(ids.get(1)).isEqualTo(posts.get(1).getId());
                  assertThat(ids.get(2)).isEqualTo(posts.get(0).getId());
                })
            .path("feed.edges[*].node.replyCount")
            .entityList(Integer.class)
            .satisfies(
                replyCounts -> {
                  // Reply chain:
                  //                post 3
                  //               /
                  // null - post 0
                  //               \
                  //                post 4
                  //
                  // null - post 1 - post 5
                  //
                  // null - post 2
                  assertThat(replyCounts).hasSize(3);

                  // feed shows most recent posts first
                  assertThat(replyCounts.get(0)).isEqualTo(0);
                  assertThat(replyCounts.get(1)).isEqualTo(1);
                  assertThat(replyCounts.get(2)).isEqualTo(2);
                });
      }
    }

    @Nested
    class repliesTests {
      @BeforeEach
      void setup() {
        // parentIndexes corresponds to the index of the post;
        List<Integer> parentIndexes = Arrays.asList(null, 0, 0);
        posts = PostHelpers.seedReplies(messageContents, users, parentIndexes, postRepository);
        // Reply chain:
        //                post 1
        //               /
        // null - Post 0
        //               \
        //                post 2
      }

      @Test
      void noReplies_returnsEmptyPostConnection() {
        // Reply chain:
        //                post 1
        //               /
        // null - Post 0
        //               \
        //                post 2

        authenticatedTester
            .document(
                """
                    query GetParent($parentId: ID!) {
                      getPost(postId: $parentId) {
                        replies {
                          edges {
                            node {
                              id
                            }
                          }
                        }
                      }
                    }
                    """)
            .variable("parentId", posts.get(1).getId())
            .execute()
            .path("getPost.replies.edges[*]")
            .entityList(PostEdge.class)
            .hasSize(0);
      }

      @Test
      void hasReplies_NoCursor_returnsPostConnection() {
        // Reply chain:
        //                post 1
        //               /
        // null - Post 0
        //               \
        //                post 2

        authenticatedTester
            .document(
                """
                    query GetParent($parentId: ID!) {
                      getPost(postId: $parentId) {
                        replies {
                          edges {
                            node {
                              id
                            }
                          }
                        }
                      }
                    }
                    """)
            .variable("parentId", posts.get(0).getId())
            .execute()
            .path("getPost.replies.edges[*].node")
            .entityList(PostProfile.class)
            .satisfies(
                postProfiles -> {
                  assertThat(postProfiles).hasSize(2);

                  // Replies are sorted by creation date descendingly
                  assertThat(postProfiles.getFirst().id()).isEqualTo(posts.get(2).getId());
                  assertThat(postProfiles.getLast().id()).isEqualTo(posts.get(1).getId());
                });
      }

      @Test
      void hasReplies_WithValidCursor_returnsPostConnection() {
        // Reply chain:
        //                post 1
        //               /
        // null - Post 0
        //               \
        //                post 2

        String endCursor =
            authenticatedTester
                .document(
                    """
                        query GetParent($parentId: ID!) {
                          getPost(postId: $parentId) {
                            replies(first: 1) {
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
                .variable("parentId", posts.get(0).getId())
                .execute()
                .path("getPost.replies.edges[0].node.id")
                .entity(UUID.class)
                // Replies are sorted by creation date descendingly
                .isEqualTo(posts.get(2).getId())
                .path("getPost.replies.pageInfo.hasNextPage")
                .entity(Boolean.class)
                .isEqualTo(true)
                .path("getPost.replies.pageInfo.endCursor")
                .entity(String.class)
                .get();

        authenticatedTester
            .document(
                """
                    query GetParent($parentId: ID!, $cursor: String) {
                      getPost(postId: $parentId) {
                        replies(first: 1, after: $cursor) {
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
            .variable("parentId", posts.get(0).getId())
            .variable("cursor", endCursor)
            .execute()
            .path("getPost.replies.edges[0].node.id")
            .entity(UUID.class)
            // Replies are sorted by creation date descendingly
            .isEqualTo(posts.get(1).getId())
            .path("getPost.replies.pageInfo.hasNextPage")
            .entity(Boolean.class)
            .isEqualTo(false);
      }

      @Test
      void hasDeletedReplies_returnsPostConnection() {
        // Reply chain:
        //                post 1
        //               /
        // null - Post 0
        //               \
        //                post 2
        authenticatedTester
            .document(
                """
                    query GetParent($parentId: ID!) {
                      getPost(postId: $parentId) {
                        replies {
                          edges {
                            node {
                              id
                            }
                          }
                        }
                      }
                    }
                    """)
            .variable("parentId", posts.get(0).getId())
            .execute()
            .path("getPost.replies.edges[*].node")
            .entityList(PostProfile.class)
            .hasSize(2);
        // Reply chain:
        //                post 1
        //               X
        // null - Post 0
        //               \
        //                post 2
        setPostStatusDeleted(posts.get(1), postRepository);

        authenticatedTester
            .document(
                """
                    query GetParent($parentId: ID!) {
                      getPost(postId: $parentId) {
                        replies {
                          edges {
                            node {
                              id
                            }
                          }
                        }
                      }
                    }
                    """)
            .variable("parentId", posts.get(0).getId())
            .execute()
            .path("getPost.replies.edges[*].node")
            .entityList(PostProfile.class)
            .satisfies(
                postProfiles -> {
                  assertThat(postProfiles).hasSize(1);
                  assertThat(postProfiles.getFirst().id()).isEqualTo(posts.get(2).getId());
                });
      }
    }
  }

  @Nested
  class shareTests {
    @Nested
    class quotesTests {
      Post sharedPost;
      List<Post> quotes;

      @BeforeEach
      void setup() {
        sharedPost = posts.get(1);
        quotes =
            seedQuotes(
                sharedPost.getId(),
                List.of(authenticatedUser, users.get(2)),
                createPostContents(2),
                postRepository);
      }

      @AfterEach
      void deleteQuotes() {
        postRepository.deleteAll(quotes);
      }

      @Test
      void noQuotes_returnsEmptyPostConnection() {
        // Quote chain:
        // quote 0
        //         \
        //           post 1 -> null
        //         /
        // quote 1

        authenticatedTester
            .document(
                """
                    query GetQuotes($postId: ID!) {
                      getPost(postId: $postId) {
                        quotes {
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
            .path("getPost.quotes.edges[*].node.id")
            .entityList(UUID.class)
            .hasSize(0);
      }

      @Test
      void hasQuotes_NoCursor_returnsPostConnection() {
        // Quote chain:
        // quote 0
        //         \
        //           post 1 -> null
        //         /
        // quote 1

        authenticatedTester
            .document(
                """
                    query GetQuotes($postId: ID!) {
                      getPost(postId: $postId) {
                        quotes {
                          edges {
                            node {
                              id
                            }
                          }
                        }
                      }
                    }
                    """)
            .variable("postId", sharedPost.getId())
            .execute()
            .path("getPost.quotes.edges[*].node.id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  assertThat(ids).hasSize(2);

                  // quotes are returned with created date descendingly
                  assertThat(ids.getFirst()).isEqualTo(quotes.getLast().getId());
                  assertThat(ids.getLast()).isEqualTo(quotes.getFirst().getId());
                });
      }

      @Test
      void hasQuotes_WithValidCursor_returnsPostConnection() {
        // Quote chain:
        // quote 0
        //         \
        //           post 1 -> null
        //         /
        // quote 1

        String endCursor =
            authenticatedTester
                .document(
                    """
                        query GetQuotes($postId: ID!) {
                          getPost(postId: $postId) {
                            quotes(first: 1) {
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
                .variable("postId", sharedPost.getId())
                .execute()
                .path("getPost.quotes.edges[*].node.id")
                .entityList(UUID.class)
                .satisfies(
                    ids -> {
                      assertThat(ids).hasSize(1);

                      // quotes are returned with created date descendingly
                      assertThat(ids.getFirst()).isEqualTo(quotes.getLast().getId());
                    })
                .path("getPost.quotes.pageInfo.hasNextPage")
                .entity(Boolean.class)
                .isEqualTo(true)
                .path("getPost.quotes.pageInfo.endCursor")
                .entity(String.class)
                .get();

        authenticatedTester
            .document(
                """
                    query GetQuotes($postId: ID!, $cursor: String) {
                      getPost(postId: $postId) {
                        quotes(first: 1, after: $cursor) {
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
            .variable("postId", sharedPost.getId())
            .variable("cursor", endCursor)
            .execute()
            .path("getPost.quotes.edges[*].node.id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  assertThat(ids).hasSize(1);

                  // quotes are returned with created date descendingly
                  assertThat(ids.getFirst()).isEqualTo(quotes.getFirst().getId());
                })
            .path("getPost.quotes.pageInfo.hasNextPage")
            .entity(Boolean.class)
            .isEqualTo(false);
      }

      @Test
      void hasDeletedQuotes_returnsPostConnection() {
        setPostStatusDeleted(quotes.getLast(), postRepository);
        // Quote chain:
        // quote 0
        //         \
        //           post 1 -> null
        //         X
        // quote 1

        authenticatedTester
            .document(
                """
                    query GetQuotes($postId: ID!) {
                      getPost(postId: $postId) {
                        quotes {
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
            .variable("postId", sharedPost.getId())
            .execute()
            .path("getPost.quotes.edges[*].node.id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  assertThat(ids).hasSize(1);
                  assertThat(ids.getFirst()).isEqualTo(quotes.getFirst().getId());
                })
            .path("getPost.quotes.pageInfo.hasNextPage")
            .entity(Boolean.class)
            .isEqualTo(false);
      }
    }

    @Nested
    class sharedPostTests {
      Post sharedPost;
      Post quote;

      @BeforeEach
      void setup() {
        sharedPost = posts.get(1);
        quote =
            seedQuotes(
                    sharedPost.getId(),
                    List.of(authenticatedUser),
                    List.of(messageContents.getFirst()),
                    postRepository)
                .getFirst();
      }

      @AfterEach
      void deleteQuotes() {
        postRepository.delete(quote);
      }

      @Test
      void validQuote_returnsSharedPost() {
        // Quote chain:
        // post 0 -> post 1 -> null
        authenticatedTester
            .document(
                """
                    query GetSharedPost($quoteId: ID!) {
                      getPost(postId: $quoteId) {
                        sharedPostId
                        sharedPost {
                          id
                        }
                      }
                    }
                    """)
            .variable("quoteId", quote.getId())
            .execute()
            .path("getPost.sharedPostId")
            .entity(UUID.class)
            .path("getPost.sharedPost.id")
            .entity(UUID.class)
            .isEqualTo(sharedPost.getId());
      }

      @Test
      void sharedPostIsDeleted_returnsNull() {
        setPostStatusDeleted(sharedPost, postRepository);
        // Quote chain:
        // post 0 X post 1 -> null
        authenticatedTester
            .document(
                """
                    query GetSharedPost($quoteId: ID!) {
                      getPost(postId: $quoteId) {
                        sharedPostId
                        sharedPost {
                          id
                        }
                      }
                    }
                    """)
            .variable("quoteId", quote.getId())
            .execute()
            .path("getPost.sharedPostId")
            .hasValue()
            .path("getPost.sharedPost")
            .valueIsNull();
      }

      @Test
      void postIsNotAQuote_returnsNull() {
        // Quote chain:
        // post 2 (original post) -> null
        authenticatedTester
            .document(
                """
                    query GetSharedPost($quoteId: ID!) {
                      getPost(postId: $quoteId) {
                        sharedPostId
                        sharedPost {
                          id
                        }
                      }
                    }
                    """)
            .variable("quoteId", posts.get(2).getId())
            .execute()
            .path("getPost.sharedPostId")
            .valueIsNull()
            .path("getPost.sharedPost")
            .valueIsNull();
      }
    }

    @Nested
    class reposts {
      Post sharedPost;
      List<Post> reposts;
      List<User> repostedBy;

      @BeforeEach
      void setup() {
        sharedPost = posts.get(1);
        repostedBy = List.of(authenticatedUser, users.get(2));
        reposts = PostHelpers.seedReposts(sharedPost.getId(), repostedBy, postRepository);
      }

      @AfterEach
      void deleteReposts() {
        postRepository.deleteAll(reposts);
      }

      @Test
      void noReposts_returnsEmptyUserConnection() {
        // Repost chain:
        // repost 0
        //         \
        //           post 1 -> null
        //         /
        // repost 1

        authenticatedTester
            .document(
                """
                    query GetReposts($postId: ID!) {
                      getPost(postId: $postId) {
                        reposts {
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
            .path("getPost.reposts.edges[*].node.id")
            .entityList(UUID.class)
            .hasSize(0);
      }

      @Test
      void hasReposts_NoCursor_returnsUserConnection() {
        // Repost chain:
        // repost 0
        //         \
        //           post 1 -> null
        //         /
        // repost 1

        authenticatedTester
            .document(
                """
                    query GetReposts($postId: ID!) {
                      getPost(postId: $postId) {
                        reposts {
                          edges {
                            node {
                              id
                            }
                          }
                        }
                      }
                    }
                    """)
            .variable("postId", sharedPost.getId())
            .execute()
            .path("getPost.reposts.edges[*].node.id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  assertThat(ids).hasSize(2);

                  // reposts are returned with created date descendingly
                  assertThat(ids.getFirst()).isEqualTo(repostedBy.getLast().getId());
                  assertThat(ids.getLast()).isEqualTo(repostedBy.getFirst().getId());
                });
      }

      @Test
      void hasReposts_WithValidCursor_returnsUserConnection() {
        // Repost chain:
        // repost 0
        //         \
        //           post 1 -> null
        //         /
        // repost 1

        String endCursor =
            authenticatedTester
                .document(
                    """
                        query GetReposts($postId: ID!) {
                          getPost(postId: $postId) {
                            reposts(first: 1) {
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
                .variable("postId", sharedPost.getId())
                .execute()
                .path("getPost.reposts.edges[*].node.id")
                .entityList(UUID.class)
                .satisfies(
                    ids -> {
                      assertThat(ids).hasSize(1);

                      // reposts are returned with created date descendingly
                      assertThat(ids.getFirst()).isEqualTo(repostedBy.getLast().getId());
                    })
                .path("getPost.reposts.pageInfo.hasNextPage")
                .entity(Boolean.class)
                .isEqualTo(true)
                .path("getPost.reposts.pageInfo.endCursor")
                .entity(String.class)
                .get();

        authenticatedTester
            .document(
                """
                    query GetReposts($postId: ID!, $cursor: String) {
                      getPost(postId: $postId) {
                        reposts(first: 1, after: $cursor) {
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
            .variable("postId", sharedPost.getId())
            .variable("cursor", endCursor)
            .execute()
            .path("getPost.reposts.edges[*].node.id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  assertThat(ids).hasSize(1);

                  // reposts are returned with created date descendingly
                  assertThat(ids.getFirst()).isEqualTo(repostedBy.getFirst().getId());
                })
            .path("getPost.reposts.pageInfo.hasNextPage")
            .entity(Boolean.class)
            .isEqualTo(false);
      }

      @Test
      void hasDeletedReposts_returnsUserConnection() {
        setPostStatusDeleted(reposts.getLast(), postRepository);
        // Repost chain:
        // repost 0
        //         \
        //           post 1 -> null
        //         /
        // repost 1

        authenticatedTester
            .document(
                """
                    query GetReposts($postId: ID!) {
                      getPost(postId: $postId) {
                        reposts {
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
            .variable("postId", sharedPost.getId())
            .execute()
            .path("getPost.reposts.edges[*].node.id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  assertThat(ids).hasSize(1);
                  assertThat(ids.getFirst()).isEqualTo(repostedBy.getFirst().getId());
                })
            .path("getPost.reposts.pageInfo.hasNextPage")
            .entity(Boolean.class)
            .isEqualTo(false);
      }
    }

    @Nested
    class shareCount {
      Post sharedPost;
      List<Post> quotes;
      List<Post> reposts;

      @BeforeEach
      void setup() {
        // Seeds 2 reposts and 2 quotes from authenticated user and user 2
        sharedPost = posts.get(1);
        quotes =
            seedQuotes(
                sharedPost.getId(),
                List.of(authenticatedUser, users.get(2)),
                createPostContents(2),
                postRepository);
        reposts =
            PostHelpers.seedReposts(
                sharedPost.getId(), List.of(authenticatedUser, users.get(2)), postRepository);
      }

      @AfterEach
      void cleanup() {
        postRepository.deleteAll(quotes);
        postRepository.deleteAll(reposts);
      }

      @Test
      void fetchingIndividualPost_noShares_postHasShareCount() {
        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        shareCount
                      }
                    }
                    """)
            .variable("postId", posts.getFirst().getId())
            .execute()
            .path("getPost.shareCount")
            .entity(Integer.class)
            .isEqualTo(0);
      }

      @Test
      void fetchingIndividualPost_hasShares_postHasShareCount() {
        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        shareCount
                      }
                    }
                    """)
            .variable("postId", posts.get(1).getId())
            .execute()
            .path("getPost.shareCount")
            .entity(Integer.class)
            .isEqualTo(4);
      }

      @Test
      void fetchingIndividualPost_hasDeletedShares_postHasShareCount() {
        setPostStatusDeleted(reposts.getFirst(), postRepository);
        setPostStatusDeleted(quotes.getLast(), postRepository);

        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        shareCount
                      }
                    }
                    """)
            .variable("postId", posts.get(1).getId())
            .execute()
            .path("getPost.shareCount")
            .entity(Integer.class)
            .isEqualTo(2);
      }

      @Test
      void fetchingFeed_eachPostHasShareCount() {
        // user 0 follows user 1 + user 2 post initialisation
        FollowHelpers.seedFollow(followRepository, authenticatedUser, users.get(2));
        // user 1 reposts post 2 (user 2)
        Post user1Repost =
            PostHelpers.seedReposts(posts.get(2).getId(), List.of(users.get(1)), postRepository)
                .getFirst();
        Post user2Repost = reposts.get(1);
        Post user2Quote = quotes.get(1);

        authenticatedTester
            .document(
                """
                    {
                      feed {
                        edges {
                          node {
                            id
                            shareCount
                          }
                        }
                      }
                    }
                    """)
            .execute()
            .path("feed.edges[*].node.id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  // Posts are sorted descendingly by created date
                  // Feed excludes authenticated user posts
                  // - quote from user 1
                  // - repost from user 1 and user 2
                  // - posts at index 1 and 2 in posts
                  assertThat(ids).hasSize(5);
                  assertThat(ids)
                      .containsExactly(
                          user1Repost.getId(),
                          user2Repost.getId(),
                          user2Quote.getId(),
                          posts.get(2).getId(),
                          posts.get(1).getId());
                })
            .path("feed.edges[*].node.shareCount")
            .entityList(Integer.class)
            .satisfies(
                shareCounts -> {
                  // Share chain (not by authenticated user):
                  // user 2 repost
                  //             \
                  //               post 1 -> null
                  //             /
                  // user 2 quote
                  //
                  // user 1 repost - post 2 -> null
                  assertThat(shareCounts).hasSize(5);

                  // feed shows most recent posts first
                  assertThat(shareCounts.get(0)).isEqualTo(0);
                  assertThat(shareCounts.get(1)).isEqualTo(0);
                  assertThat(shareCounts.get(2)).isEqualTo(0);
                  assertThat(shareCounts.get(3)).isEqualTo(1);
                  assertThat(shareCounts.get(4)).isEqualTo(4);
                });

        // clean up
        postRepository.delete(user1Repost);
      }
    }

    @Nested
    class sharedByMe {
      List<Post> reposts;

      @AfterEach
      void deleteReposts() {
        postRepository.deleteAll(reposts);
      }

      @Test
      void fetchingIndividualPost_noShares_sharedByMeFalse() {
        // user 2 reposts post 1
        Post sharedPost = posts.get(1);
        List<User> repostedBy = List.of(users.get(2));
        reposts = PostHelpers.seedReposts(sharedPost.getId(), repostedBy, postRepository);

        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        sharedByMe
                        shareCount
                      }
                    }
                    """)
            .variable("postId", sharedPost.getId())
            .execute()
            .path("getPost.shareCount")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("getPost.sharedByMe")
            .entity(Boolean.class)
            .isEqualTo(false);
      }

      @Test
      void fetchingIndividualPost_isSharedByMe_sharedByMeTrue() {
        // user 2 + authenticated user reposts post 1
        Post sharedPost = posts.get(1);
        List<User> repostedBy = List.of(authenticatedUser, users.get(2));
        reposts = PostHelpers.seedReposts(sharedPost.getId(), repostedBy, postRepository);

        authenticatedTester
            .document(
                """
                    query GetPost($postId: ID!){
                      getPost(postId: $postId) {
                        sharedByMe
                        shareCount
                      }
                    }
                    """)
            .variable("postId", sharedPost.getId())
            .execute()
            .path("getPost.shareCount")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("getPost.sharedByMe")
            .entity(Boolean.class)
            .isEqualTo(true);
      }

      @Test
      void fetchingFeed_onePostSharedByMe() {
        // authenticated user follows user 1 + user 2 post initialisation
        FollowHelpers.seedFollow(followRepository, authenticatedUser, users.get(2));
        // user 2 + authenticated user reposts post 1
        Post sharedPost = posts.get(1);
        List<User> repostedBy = List.of(authenticatedUser, users.get(2));
        reposts = PostHelpers.seedReposts(sharedPost.getId(), repostedBy, postRepository);
        // Share chain:
        //      user 2 repost
        //                   \
        //                    post 1 -> null
        //                  /
        // auth. user repost

        authenticatedTester
            .document(
                """
                    {
                      feed {
                        edges {
                          node {
                            id
                            sharedByMe
                          }
                        }
                      }
                    }
                    """)
            .execute()
            .path("feed.edges[*].node.id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  // Posts are sorted descendingly by created date
                  // Feed excludes authenticated user posts
                  // - user 2 repost of post 1
                  // - posts at index 1 and 2 in posts
                  assertThat(ids).hasSize(3);
                  assertThat(ids)
                      .containsExactly(
                          reposts.get(1).getId(), posts.get(2).getId(), posts.get(1).getId());
                })
            .path("feed.edges[*].node.sharedByMe")
            .entityList(Boolean.class)
            .satisfies(
                sharedByMe -> {
                  // Share chain:
                  //      user 2 repost
                  //                   \
                  //                    post 1 -> null
                  //                  /
                  // auth. user repost

                  // Feed excludes auth. users posts
                  assertThat(sharedByMe).hasSize(3);

                  // feed shows most recent posts first
                  assertFalse(sharedByMe.get(0)); // user 2 repost
                  assertFalse(sharedByMe.get(1)); // post 2
                  assertTrue(sharedByMe.get(2)); // post 1
                });
      }

      @Test
      void fetchingFeed_everyPostSharedByMe() {
        // authenticated user follows user 1 + user 2 post initialisation
        FollowHelpers.seedFollow(followRepository, authenticatedUser, users.get(2));
        // user 2 + authenticated user reposts post 1
        List<Post> post1Reposts =
            PostHelpers.seedReposts(
                posts.get(1).getId(), List.of(authenticatedUser, users.get(2)), postRepository);
        // authenticated user reposts post 2
        List<Post> post2repost =
            PostHelpers.seedReposts(
                posts.get(2).getId(), List.of(authenticatedUser), postRepository);
        reposts = new ArrayList<>(post1Reposts);
        reposts.addAll(post2repost);
        // Share chain:
        //      user 2 repost
        //                   \
        //                    post 1 -> null
        //                  /
        // auth. user repost
        //
        // auth. user repost - post 2 -> null

        authenticatedTester
            .document(
                """
                    {
                      feed {
                        edges {
                          node {
                            id
                            sharedByMe
                          }
                        }
                      }
                    }
                    """)
            .execute()
            .path("feed.edges[*].node.id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  // Posts are sorted descendingly by created date
                  // Feed excludes authenticated user posts
                  // - user 2 repost of post 1
                  // - posts at index 1 and 2 in posts
                  assertThat(ids).hasSize(3);
                  assertThat(ids)
                      .containsExactly(
                          reposts.get(1).getId(), posts.get(2).getId(), posts.get(1).getId());
                })
            .path("feed.edges[*].node.sharedByMe")
            .entityList(Boolean.class)
            .satisfies(
                sharedByMe -> {
                  // Share chain:
                  //      user 2 repost
                  //                   \
                  //                    post 1 -> null
                  //                  /
                  // auth. user repost
                  //
                  // auth. user repost - post 2 -> null

                  // Feed excludes auth. users posts
                  assertThat(sharedByMe).hasSize(3);

                  // feed shows most recent posts first
                  assertFalse(sharedByMe.get(0)); // user 2 repost
                  assertTrue(sharedByMe.get(1)); // post 2
                  assertTrue(sharedByMe.get(2)); // post 1
                });
      }
    }
  }

  @Nested
  class mentionTests {
    Post postWithMention;
    User firstMentionedUser;

    void addMention(Post post, User author) {
      seedMention(post, author, mentionRepository);
    }

    @BeforeEach
    void setup() {
      // User 0 follows user 1 (outer setup) + user 2
      FollowHelpers.seedFollow(followRepository, users.getFirst(), users.get(2));
      // posts:
      // - authenticated user authors post at index-0
      // - user at index-1 authors post at index-1
      // - user at index-2 authors post at index-2
      postWithMention = posts.get(1);
      firstMentionedUser = users.get(2);
      addMention(postWithMention, firstMentionedUser);
    }

    @Test
    void getPostWithNoMentions_returnsPostProfile() {
      authenticatedTester
          .document(
              """
                query GetPost($id: ID!) {
                  getPost(postId: $id) {
                    mentions {
                      id
                    }
                  }
                }
                """)
          .variable("id", posts.get(2).getId())
          .execute()
          .path("getPost.mentions[*].id")
          .entityList(UUID.class)
          .hasSize(0);
    }

    @Test
    void getPostWithOneMention_returnsPostProfile() {
      authenticatedTester
          .document(
              """
                query GetPost($id: ID!) {
                  getPost(postId: $id) {
                    mentions {
                      id
                    }
                  }
                }
                """)
          .variable("id", postWithMention.getId())
          .execute()
          .path("getPost.mentions[*].id")
          .entityList(UUID.class)
          .hasSize(1)
          .contains(firstMentionedUser.getId());
    }

    @Test
    void getPostWithDeletedMentionedUser_excludesDeletedUser() {
      firstMentionedUser.setStatus(UserStatus.DELETED);
      userRepository.saveAndFlush(firstMentionedUser);

      authenticatedTester
          .document(
              """
                query GetPost($id: ID!) {
                  getPost(postId: $id) {
                    mentions {
                      id
                    }
                  }
                }
                """)
          .variable("id", postWithMention.getId())
          .execute()
          .path("getPost.mentions[*].id")
          .entityList(UUID.class)
          .hasSize(0);
    }

    @Test
    void getPostWithMultipleMentions_returnsPostProfile() {
      User secondMentionedUser = authenticatedUser;
      addMention(postWithMention, secondMentionedUser);

      authenticatedTester
          .document(
              """
                query GetPost($id: ID!) {
                  getPost(postId: $id) {
                    mentions {
                      id
                    }
                  }
                }
                """)
          .variable("id", postWithMention.getId())
          .execute()
          .path("getPost.mentions[*].id")
          .entityList(UUID.class)
          .hasSize(2)
          .contains(firstMentionedUser.getId(), secondMentionedUser.getId());
    }
  }
}
