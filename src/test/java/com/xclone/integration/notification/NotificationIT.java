package com.xclone.integration.notification;

import static com.xclone.support.helpers.NotificationHelpers.seedNotifications;
import static com.xclone.support.helpers.PostHelpers.seedPosts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xclone.exception.dto.FieldError;
import com.xclone.integration.base.BaseGraphQLIntegrationTest;
import com.xclone.integration.validation.ValidationIT;
import com.xclone.notification.dto.NotificationProfile;
import com.xclone.notification.dto.connection.NotificationConnection;
import com.xclone.notification.dto.connection.NotificationEdge;
import com.xclone.notification.dto.mutation.NotificationResponse;
import com.xclone.notification.model.entity.Notification;
import com.xclone.notification.model.entity.NotificationActor;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.post.model.entity.Post;
import com.xclone.reply.dto.request.CreateReplyInput;
import com.xclone.share.dto.request.CreateQuoteInput;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.user.model.entity.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

public class NotificationIT extends BaseGraphQLIntegrationTest {
  List<String> handles = List.of("example1", "example2", "example3", "example4", "example5");
  List<User> users;

  List<String> messageContents = List.of("one for sorrow", "two for joy", "three for a girl");
  List<Post> posts;

  Post likedPost;
  Post repostedPost;
  List<Notification> notifications;

  @BeforeEach
  void setup() {
    // Adds 3 users to the DB under the handles
    users =
        handles.stream().map(UserFixtures::createUserWithHandle).map(userRepository::save).toList();
    // Sets the accessToken to match that of the authenticated user
    authenticatedUser = users.getFirst();
    String accessToken = authHelpers.getUserAccessToken(authenticatedUser.getId().toString());
    authenticatedTester =
        authenticatedTester.mutate().headers(headers -> headers.setBearerAuth(accessToken)).build();
    // Create posts:
    // - authenticated user authors post at index-0, index-1 and index-2
    List<User> authenticatedUserRepeated = Collections.nCopies(3, authenticatedUser);
    posts = seedPosts(messageContents, authenticatedUserRepeated, postRepository);
  }

  void addNotifications() {
    notifications = new ArrayList<>();
    // user 1 and user 2 like authenticated users post (post 0)
    likedPost = posts.getFirst();
    notifications.add(
        seedNotifications(
            authenticatedUser,
            likedPost,
            NotificationType.LIKE,
            users.subList(1, 3),
            notificationRepository,
            notificationActorRepository));
    // user 1 and user 2 reposted post 0
    repostedPost = posts.get(1);
    notifications.add(
        seedNotifications(
            authenticatedUser,
            repostedPost,
            NotificationType.REPOST,
            users.subList(1, 3),
            notificationRepository,
            notificationActorRepository));
  }

  @Nested
  class NotificationTests {
    @BeforeEach
    void setupNotifications() {
      addNotifications();
    }

    /**
     * A malformed cursor is tested in {@link ValidationIT.malformedCursorTests} so it has been
     * omitted here.
     */
    @Nested
    class GetNotificationsTests {
      @Test
      void getNotifications_noCursor_returnsNotificationConnection() {
        NotificationProfile response =
            authenticatedTester
                .document(
                    """
                        query GetNotifications {
                          getNotifications(first: 1) {
                            edges {
                              node {
                                type
                                read
                              }
                            }
                          }
                        }
                        """)
                .execute()
                .path("getNotifications.edges[0].node")
                .entity(NotificationProfile.class)
                .get();

        assertThat(response.type()).isEqualTo(NotificationType.REPOST);
        assertFalse(response.read());
      }

      @Test
      void getNotifications_withValidCursor_returnsNotificationConnection() {
        String cursor =
            authenticatedTester
                .document(
                    """
                        query GetNotifications {
                          getNotifications(first: 1) {
                            edges {
                              cursor
                            }
                            pageInfo {
                              hasNextPage
                              endCursor
                            }
                          }
                        }
                        """)
                .execute()
                .path("getNotifications")
                .entity(NotificationConnection.class)
                .satisfies(
                    connection ->
                        assertThat(connection.edges().getLast().cursor())
                            .isEqualTo(connection.pageInfo().endCursor()))
                .path("getNotifications.pageInfo.hasNextPage")
                .entity(Boolean.class)
                .isEqualTo(true)
                .path("getNotifications.pageInfo.endCursor")
                .entity(String.class)
                .get();

        NotificationProfile response =
            authenticatedTester
                .document(
                    """
                        query GetNotifications($cursor: String) {
                          getNotifications(first: 1, after: $cursor ) {
                            edges {
                              node {
                                type
                                read
                              }
                            }
                            pageInfo {
                              hasNextPage
                            }
                          }
                        }
                        """)
                .variable("cursor", cursor)
                .execute()
                .path("getNotifications.pageInfo.hasNextPage")
                .entity(Boolean.class)
                .isEqualTo(false)
                .path("getNotifications.edges[0].node")
                .entity(NotificationProfile.class)
                .get();

        assertThat(response.type()).isEqualTo(NotificationType.LIKE);
        assertFalse(response.read());
      }

      @Test
      void getNotifications_noNotifications_returnsEmptyNotificationConnection() {
        // Sets the accessToken to match that of the user at index 1
        // user 1 has no notifications
        User user1 = users.get(1);
        String accessToken = authHelpers.getUserAccessToken(user1.getId().toString());
        authenticatedTester =
            authenticatedTester
                .mutate()
                .headers(headers -> headers.setBearerAuth(accessToken))
                .build();

        authenticatedTester
            .document(
                """
                    query GetNotifications {
                      getNotifications(first: 1) {
                        edges {
                          node {
                            type
                            read
                          }
                        }
                      }
                    }
                    """)
            .execute()
            .path("getNotifications.edges[*]")
            .entityList(NotificationEdge.class)
            .hasSize(0);
      }
    }

    @Nested
    class PostMappingTests {
      @Test
      void notificationPostMapping_validPosts_returnsNotificationConnection() {
        authenticatedTester
            .document(
                """
                    query GetNotifications {
                      getNotifications(first: 1) {
                        edges {
                          node {
                            post {
                              id
                            }
                            type
                          }
                        }
                      }
                    }
                    """)
            .execute()
            .path("getNotifications.edges[0].node.post.id")
            .entity(UUID.class)
            .isEqualTo(repostedPost.getId())
            .path("getNotifications.edges[0].node.type")
            .entity(NotificationType.class)
            .isEqualTo(NotificationType.REPOST);
      }

      @Test
      void notificationPostMapping_followNotification_postNull() {
        // add follow notifications for user 1
        notifications.add(
            seedNotifications(
                authenticatedUser,
                null,
                NotificationType.FOLLOW,
                users.subList(1, users.size()),
                notificationRepository,
                notificationActorRepository));

        authenticatedTester
            .document(
                """
                    query GetNotifications {
                      getNotifications(first: 1) {
                        edges {
                          node {
                            post {
                              id
                            }
                            type
                          }
                        }
                      }
                    }
                    """)
            .execute()
            .path("getNotifications.edges[0].node.post")
            .valueIsNull()
            .path("getNotifications.edges[0].node.type")
            .entity(NotificationType.class)
            .isEqualTo(NotificationType.FOLLOW);
      }
    }

    @Nested
    class ActorMappingTests {
      @Test
      void notificationMapping_getActors_returnsNotificationConnection() {
        authenticatedTester
            .document(
                """
                    query GetNotifications {
                      getNotifications(first: 2) {
                        edges {
                          node {
                            actors {
                              id
                            }
                            actorCount
                          }
                        }
                      }
                    }
                    """)
            .execute()
            // Actor ordering is a per-notification assertion — target a single edge.
            .path("getNotifications.edges[0].node.actors[*].id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  assertThat(ids).hasSize(2);
                  // actors sorted by createdAt descending
                  assertThat(ids.getFirst()).isEqualTo(users.get(2).getId());
                  assertThat(ids.getLast()).isEqualTo(users.get(1).getId());
                })
            // actorCount spans both notifications — [*] is correct here.
            .path("getNotifications.edges[*].node.actorCount")
            .entityList(Integer.class)
            .satisfies(actorCounts -> assertThat(actorCounts).containsExactly(2, 2));
      }

      @Test
      void notificationMapping_truncatesActorsTo3_returnsNotificationConnection() {
        List<User> replyUsers = List.of(users.get(1), users.get(2), users.get(3), users.get(4));
        seedNotifications(
            authenticatedUser,
            posts.get(2),
            NotificationType.REPLY,
            replyUsers,
            notificationRepository,
            notificationActorRepository);

        authenticatedTester
            .document(
                """
                    query GetNotifications {
                      getNotifications(first: 1) {
                        edges {
                          node {
                            actors {
                              id
                            }
                            actorCount
                          }
                        }
                      }
                    }
                    """)
            .execute()
            // Actor ordering is a per-notification assertion — target a single edge.
            .path("getNotifications.edges[0].node.actors[*].id")
            .entityList(UUID.class)
            .satisfies(
                ids -> {
                  assertThat(ids).hasSize(3);
                  // actors sorted by createdAt descending
                  assertThat(ids.get(0)).isEqualTo(replyUsers.get(3).getId());
                  assertThat(ids.get(1)).isEqualTo(replyUsers.get(2).getId());
                  assertThat(ids.get(2)).isEqualTo(replyUsers.get(1).getId());
                })
            // actorCount spans both notifications — [*] is correct here.
            .path("getNotifications.edges[0].node.actorCount")
            .entity(Integer.class)
            .isEqualTo(4);
      }
    }
  }

  @Nested
  class ReadNotificationTests {
    @BeforeEach
    void setupNotifications() {
      addNotifications();
    }

    @Test
    void readNotification_validId_notificationNotRead_returnsNotificationResponse() {
      NotificationResponse response =
          authenticatedTester
              .document(
                  """
                      mutation ReadNotification($id: ID!) {
                        readNotification(notificationId: $id) {
                          code
                          success
                          notification {
                            id
                            read
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("id", notifications.getFirst().getId())
              .execute()
              .path("readNotification")
              .entity(NotificationResponse.class)
              .get();

      assertThat(response.code()).isEqualTo("200");
      assertTrue(response.success());
      assertNull(response.errors());
      assertThat(response.notification().id()).isEqualTo(notifications.getFirst().getId());
      assertTrue(response.notification().read());
    }

    @Test
    void readNotification_validId_notificationAlreadyRead_returnsNotificationResponse() {
      authenticatedTester
          .document(
              """
                  mutation ReadNotification($id: ID!) {
                    readNotification(notificationId: $id) {
                      notification {
                        read
                      }
                    }
                  }
                  """)
          .variable("id", notifications.getFirst().getId())
          .execute()
          .path("readNotification.notification.read")
          .entity(Boolean.class)
          .isEqualTo(true);

      NotificationResponse response =
          authenticatedTester
              .document(
                  """
                      mutation ReadNotification($id: ID!) {
                        readNotification(notificationId: $id) {
                          code
                          success
                          notification {
                            id
                            read
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("id", notifications.getFirst().getId())
              .execute()
              .path("readNotification")
              .entity(NotificationResponse.class)
              .get();

      assertThat(response.code()).isEqualTo("200");
      assertTrue(response.success());
      assertNull(response.errors());
      assertThat(response.notification().id()).isEqualTo(notifications.getFirst().getId());
      assertTrue(response.notification().read());
    }

    @Test
    void readNotification_invalidId_returnsNotificationNotFound() {
      NotificationResponse response =
          authenticatedTester
              .document(
                  """
                      mutation ReadNotification($id: ID!) {
                        readNotification(notificationId: $id) {
                          code
                          success
                          notification {
                            id
                            read
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("id", UUID.randomUUID())
              .execute()
              .path("readNotification")
              .entity(NotificationResponse.class)
              .get();

      assertThat(response.code()).isEqualTo("404");
      assertFalse(response.success());
      assertNull(response.notification());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactlyInAnyOrder(tuple("notificationId", "Notification does not exist"));
    }

    @Test
    void readNotification_userNotRecipient_returnsForbidden() {
      // Sets the accessToken to match that of user 1
      String accessToken = authHelpers.getUserAccessToken(users.get(1).getId().toString());
      authenticatedTester =
          authenticatedTester
              .mutate()
              .headers(headers -> headers.setBearerAuth(accessToken))
              .build();

      NotificationResponse response =
          authenticatedTester
              .document(
                  """
                      mutation ReadNotification($id: ID!) {
                        readNotification(notificationId: $id) {
                          code
                          success
                          notification {
                            id
                            read
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("id", notifications.getFirst().getId())
              .execute()
              .path("readNotification")
              .entity(NotificationResponse.class)
              .get();

      assertThat(response.code()).isEqualTo("403");
      assertFalse(response.success());
      assertNull(response.notification());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactlyInAnyOrder(
              tuple("userId", "Only the recipient can read the notification"));
    }
  }

  @Nested
  class NotificationTriggers {
    UUID originalPostId;
    UUID originalPostAuthorId;
    HttpGraphQlTester user0AuthenticatedTester;
    HttpGraphQlTester user2AuthenticatedTester;

    @BeforeEach
    void setup() {
      posts =
          seedPosts(
              messageContents,
              List.of(users.getFirst(), users.get(1), users.get(2)),
              postRepository);
      postsIdsToDeleteFirst = new ArrayList<>();
      originalPostId = posts.get(1).getId();
      originalPostAuthorId = posts.get(1).getAuthorId();
      user0AuthenticatedTester = authenticatedTester;
      // Sets the accessToken to match that of user-2
      String user2AccessToken = authHelpers.getUserAccessToken(users.get(2).getId().toString());
      user2AuthenticatedTester =
          authenticatedTester
              .mutate()
              .headers(headers -> headers.setBearerAuth(user2AccessToken))
              .build();
    }

    //    UUID createPostReturnId(HttpGraphQlTester authenticatedTester, String document,
    //                            Map<String, Object> variable) {
    //      return authenticatedTester.document(document)
    //          .variable(variable.get, originalPostId)
    //          .execute()
    //          .path("createRepost.success")
    //          .entity(Boolean.class)
    //          .isEqualTo(true)
    //          .path("createRepost.post.id")
    //          .entity(UUID.class)
    //          .get();
    //    }

    @Nested
    class upsertNotification {

      @Nested
      class followNotification {
        @Test
        void follow_noOutstandingFollows_createsNewNotification_createsNewNotificationActor() {}

        @Test
        void follow_insideTimeBucket_updatesExistingNotification_createsNewNotificationActor() {}

        @Test
        void follow_outsideTimeBucket_createsNewNotification_createsNewNotificationActor() {}

        @Test
        void unfollow_removesFromExistingNotification_deletesOnlyNotificationActor() {
          // Check that it removes from correct notification
        }

        @Test
        void unfollow_removesFromExistingNotification_cleanupNotificationAndNotificationActor() {
          // Check that it removes from correct notification
        }
      }

      @Nested
      class likeNotification {
        @Test
        void like_createsNewNotification_createsNewNotificationActor() {}

        @Test
        void like_updatesExistingNotification_createsNewNotificationActor() {}

        @Test
        void unlike_removesFromExistingNotification_deletesOnlyNotificationActor() {
          // Check that it removes from correct notification
        }

        @Test
        void unlike_removesFromExistingNotification_cleanupNotificationAndNotificationActor() {
          // Check that it removes from correct notification
        }
      }

      @Nested
      class repostNotification {

        //        @Test
        //        void repost_createsNewNotification_createsNewNotificationActor() {
        //          UUID repostId = user0AuthenticatedTester.document("""
        //                  mutation CreateRepost($sharedPostId: ID!) {
        //                    createRepost(sharedPostId: $sharedPostId) {
        //                      success
        //                      post {
        //                        id
        //                      }
        //                    }
        //                  }
        //                  """)
        //              .variable("$sharedPostId", originalPostId)
        //              .execute()
        //              .path("createRepost.success")
        //              .entity(Boolean.class)
        //              .isEqualTo(true)
        //              .path("createRepost.post.id")
        //              .entity(UUID.class)
        //              .get();
        //          postsIdsToDeleteFirst.add(repostId);
        //
        //          List<Notification> notifications = notificationRepository.findAll();
        //          List<NotificationActor> notificationActors =
        // notificationActorRepository.findAll();
        //
        //          assertThat(notifications).hasSize(1);
        //          assertThat(notificationActors).hasSize(1);
        //
        //          Notification notification = notifications.getFirst();
        //          assertThat(notification.getPostId()).isEqualTo(originalPostId);
        //          assertThat(notification.getRecipientUserId()).isEqualTo(originalPostAuthorId);
        //          assertFalse(notification.isRead());
        //
        //          NotificationActor actor = notificationActors.getFirst();
        //          assertThat(actor.getActorUserId()).isEqualTo(authenticatedUser.getId());
        //          assertThat(actor.getNotificationId()).isEqualTo(notification.getId());
        //        }
        //
        //        @Test
        //        void repost_updatesExistingNotification_createsNewNotificationActor() {
        //          UUID firstRepostId = user0AuthenticatedTester.document("""
        //                  mutation CreateRepost($sharedPostId: ID!) {
        //                    createRepost(sharedPostId: $sharedPostId) {
        //                      success
        //                      post {
        //                        id
        //                      }
        //                    }
        //                  }
        //                  """)
        //              .variable("$sharedPostId", originalPostId)
        //              .execute()
        //              .path("createRepost.success")
        //              .entity(Boolean.class)
        //              .isEqualTo(true)
        //              .path("createRepost.post.id")
        //              .entity(UUID.class)
        //              .get();
        //          postsIdsToDeleteFirst.add(firstRepostId);
        //
        //          UUID secondRepostId = user2AuthenticatedTester.document("""
        //                  mutation CreateRepost($sharedPostId: ID!) {
        //                    createRepost(sharedPostId: $sharedPostId) {
        //                      success
        //                      post {
        //                        id
        //                      }
        //                    }
        //                  }
        //                  """)
        //              .variable("$sharedPostId", originalPostId)
        //              .execute()
        //              .path("createRepost.success")
        //              .entity(Boolean.class)
        //              .isEqualTo(true)
        //              .path("createRepost.post.id")
        //              .entity(UUID.class)
        //              .get();
        //          postsIdsToDeleteFirst.add(secondRepostId);
        //
        //          List<Notification> notifications = notificationRepository.findAll();
        //          List<NotificationActor> notificationActors =
        // notificationActorRepository.findAll();
        //
        //          assertThat(notifications).hasSize(1);
        //          assertThat(notificationActors).hasSize(2);
        //
        //          Notification notification = notifications.getFirst();
        //          assertThat(notification.getPostId()).isEqualTo(originalPostId);
        //          assertThat(notification.getRecipientUserId()).isEqualTo(originalPostAuthorId);
        //          assertFalse(notification.isRead());
        //
        //          NotificationActor actor = notificationActors.getFirst();
        //          assertThat(actor.getActorUserId()).isEqualTo(users.get(2).getId());
        //          assertThat(actor.getNotificationId()).isEqualTo(notification.getId());
        //        }

        @Test
        void deleteRepost_removesFromExistingNotification_deletesOnlyNotificationActor() {
          // Check that it removes from correct notification
        }

        @Test
        void
            deleteRepost_removesFromExistingNotification_cleanupNotificationAndNotificationActor() {
          // Check that it removes from correct notification
        }
      }

      @Nested
      class quoteNotification {
        @Test
        void quote_createsDiscreteNotifications() {
          CreateQuoteInput input =
              new CreateQuoteInput(originalPostId, "this is the quote content");
          UUID quoteId =
              authenticatedTester
                  .document(
                      """
                  mutation CreateQuote($input: CreateQuoteInput!) {
                    createQuote(input: $input) {
                      success
                      post {
                        id
                      }
                    }
                  }
                  """)
                  .variable("input", input)
                  .execute()
                  .path("createQuote.success")
                  .entity(Boolean.class)
                  .isEqualTo(true)
                  .path("createQuote.post.id")
                  .entity(UUID.class)
                  .get();
          postsIdsToDeleteFirst.add(quoteId);

          List<Notification> notifications = notificationRepository.findAll();
          List<NotificationActor> notificationActors = notificationActorRepository.findAll();

          assertThat(notifications).hasSize(1);
          assertThat(notificationActors).hasSize(1);

          Notification notification = notifications.getFirst();
          assertThat(notification.getPostId()).isEqualTo(originalPostId);
          assertThat(notification.getRecipientUserId()).isEqualTo(originalPostAuthorId);
          assertFalse(notification.isRead());

          NotificationActor actor = notificationActors.getFirst();
          assertThat(actor.getActorUserId()).isEqualTo(authenticatedUser.getId());
          assertThat(actor.getNotificationId()).isEqualTo(notification.getId());
        }

        @Test
        void deleteQuote_removesNotification() {
          // Check that it removes from correct notification
        }
      }

      @Nested
      class replyNotification {
        @Test
        void reply_createsDiscreteNotifications() {
          CreateReplyInput input =
              new CreateReplyInput(originalPostId, "this is the reply content");
          UUID replyId =
              authenticatedTester
                  .document(
                      """
                  mutation CreateReply($input: CreateReplyInput!) {
                    createReply(input: $input) {
                      success
                      post {
                        id
                      }
                    }
                  }
                  """)
                  .variable("input", input)
                  .execute()
                  .path("createReply.success")
                  .entity(Boolean.class)
                  .isEqualTo(true)
                  .path("createReply.post.id")
                  .entity(UUID.class)
                  .get();
          postsIdsToDeleteFirst.add(replyId);

          List<Notification> notifications = notificationRepository.findAll();
          List<NotificationActor> notificationActors = notificationActorRepository.findAll();

          assertThat(notifications).hasSize(1);
          assertThat(notificationActors).hasSize(1);

          Notification notification = notifications.getFirst();
          assertThat(notification.getPostId()).isEqualTo(originalPostId);
          assertThat(notification.getRecipientUserId()).isEqualTo(originalPostAuthorId);
          assertFalse(notification.isRead());

          NotificationActor actor = notificationActors.getFirst();
          assertThat(actor.getActorUserId()).isEqualTo(authenticatedUser.getId());
          assertThat(actor.getNotificationId()).isEqualTo(notification.getId());
        }

        @Test
        void deleteReply_removesNotification() {
          // Check that it removes from correct notification
        }
      }
    }

    @Nested
    class deleteNotificationActorAndCleanupNotification {

      @Nested
      class followNotification {
        @Test
        void unfollow_removesFromExistingNotification_deletesOnlyNotificationActor() {
          // Check that it removes from correct notification
        }

        @Test
        void unfollow_removesFromExistingNotification_cleanupNotificationAndNotificationActor() {
          // Check that it removes from correct notification
        }
      }

      @Nested
      class likeNotification {
        @Test
        void unlike_removesFromExistingNotification_deletesOnlyNotificationActor() {
          // Check that it removes from correct notification
        }

        @Test
        void unlike_removesFromExistingNotification_cleanupNotificationAndNotificationActor() {
          // Check that it removes from correct notification
        }
      }

      @Nested
      class repostNotification {
        @Test
        void deleteRepost_removesFromExistingNotification_deletesOnlyNotificationActor() {
          // Check that it removes from correct notification
        }

        @Test
        void
            deleteRepost_removesFromExistingNotification_cleanupNotificationAndNotificationActor() {
          // Check that it removes from correct notification
        }
      }

      @Nested
      class quoteNotification {
        @Test
        void deleteQuote_removesNotification() {
          // Check that it removes from correct notification
        }
      }

      @Nested
      class replyNotification {
        Post originalPost;

        @BeforeEach
        void setup() {
          originalPost = posts.get(1);
        }

        @Test
        void deleteReply_removesNotification() {
          // Check that it removes from correct notification
        }
      }
    }
  }
}
