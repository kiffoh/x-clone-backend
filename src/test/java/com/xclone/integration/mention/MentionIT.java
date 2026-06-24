package com.xclone.integration.mention;

import static com.xclone.support.helpers.PostHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.xclone.integration.base.BaseGraphQLIntegrationTest;
import com.xclone.mention.model.entity.Mention;
import com.xclone.post.dto.request.CreatePostInput;
import com.xclone.post.dto.request.UpdatePostInput;
import com.xclone.post.model.entity.Post;
import com.xclone.reply.dto.request.CreateReplyInput;
import com.xclone.share.dto.request.CreateQuoteInput;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.FollowHelpers;
import com.xclone.user.model.entity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MentionIT extends BaseGraphQLIntegrationTest {
  List<String> handles = List.of("user0", "user1", "user2", "user3");
  List<User> users;

  List<String> messageContents = createPostContents(4);
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

  @Nested
  @DisplayName("createMention")
  class CreateMention {
    @Test
    void createMention_createPost_success() {
      UUID mentionedUserId = users.get(1).getId();
      CreatePostInput createPostInput =
          new CreatePostInput("@user1 this is the message content", List.of(mentionedUserId));

      UUID postId =
          authenticatedTester
              .document(
                  """
            mutation CreatePost($input: CreatePostInput!) {
                createPost(input: $input) {
                    success
                    post {
                     id
                    }
                }
            }
            """)
              .variable("input", createPostInput)
              .execute()
              .path("createPost.success")
              .entity(Boolean.class)
              .isEqualTo(true)
              .path("createPost.post.id")
              .entity(UUID.class)
              .get();

      List<Mention> mentions = mentionRepository.findAll();
      assertThat(mentions).hasSize(1);
      Mention mention = mentions.getFirst();
      assertThat(mention.getPostId()).isEqualTo(postId);
      assertThat(mention.getMentionedUserId()).isEqualTo(mentionedUserId);
    }

    @Test
    void createMention_createReply_success() {
      UUID mentionedUserId = users.get(2).getId();
      UUID parentPostId = posts.getFirst().getId();
      CreateReplyInput createReplyInput =
          new CreateReplyInput(parentPostId, "@user2 replying to you", List.of(mentionedUserId));

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
              .variable("input", createReplyInput)
              .execute()
              .path("createReply.success")
              .entity(Boolean.class)
              .isEqualTo(true)
              .path("createReply.post.id")
              .entity(UUID.class)
              .get();

      List<Mention> mentions = mentionRepository.findAll();
      assertThat(mentions).hasSize(1);
      Mention mention = mentions.getFirst();
      assertThat(mention.getPostId()).isEqualTo(replyId);
      assertThat(mention.getMentionedUserId()).isEqualTo(mentionedUserId);

      // cleanup
      postIdsToDeleteFirst = List.of(replyId);
    }

    @Test
    void createMention_createQuote_success() {
      UUID mentionedUserId = users.get(2).getId();
      UUID sharedPostId = posts.getFirst().getId();
      CreateQuoteInput createQuoteInput =
          new CreateQuoteInput(sharedPostId, "@user2 quoting this", List.of(mentionedUserId));

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
              .variable("input", createQuoteInput)
              .execute()
              .path("createQuote.success")
              .entity(Boolean.class)
              .isEqualTo(true)
              .path("createQuote.post.id")
              .entity(UUID.class)
              .get();

      List<Mention> mentions = mentionRepository.findAll();
      assertThat(mentions).hasSize(1);
      Mention mention = mentions.getFirst();
      assertThat(mention.getPostId()).isEqualTo(quoteId);
      assertThat(mention.getMentionedUserId()).isEqualTo(mentionedUserId);

      // cleanup
      postIdsToDeleteFirst = List.of(quoteId);
    }

    @Test
    void createMention_noMentionedUsers() {
      CreatePostInput createPostInput = new CreatePostInput("no mentions here", List.of());

      authenticatedTester
          .document(
              """
            mutation CreatePost($input: CreatePostInput!) {
                createPost(input: $input) {
                    success
                    post {
                     id
                    }
                }
            }
            """)
          .variable("input", createPostInput)
          .execute()
          .path("createPost.success")
          .entity(Boolean.class)
          .isEqualTo(true);

      List<Mention> mentions = mentionRepository.findAll();
      assertThat(mentions).hasSize(0);
    }

    @Test
    void createMention_inactiveUser_skippedSilently() {
      UUID activeUserId = users.get(1).getId();
      UUID nonExistentUserId = UUID.randomUUID();
      CreatePostInput createPostInput =
          new CreatePostInput("@user1 @ghost hello both", List.of(activeUserId, nonExistentUserId));

      authenticatedTester
          .document(
              """
            mutation CreatePost($input: CreatePostInput!) {
                createPost(input: $input) {
                    success
                    post {
                     id
                    }
                }
            }
            """)
          .variable("input", createPostInput)
          .execute()
          .path("createPost.success")
          .entity(Boolean.class)
          .isEqualTo(true);

      List<Mention> mentions = mentionRepository.findAll();
      assertThat(mentions).hasSize(1);
      assertThat(mentions.getFirst().getMentionedUserId()).isEqualTo(activeUserId);
    }

    @Test
    void createMention_multipleMentionedUsers() {
      UUID mentionedUserId1 = users.get(1).getId();
      UUID mentionedUserId2 = users.get(2).getId();
      CreatePostInput createPostInput =
          new CreatePostInput(
              "@user1 @user2 hello both", List.of(mentionedUserId1, mentionedUserId2));

      UUID postId =
          authenticatedTester
              .document(
                  """
            mutation CreatePost($input: CreatePostInput!) {
                createPost(input: $input) {
                    success
                    post {
                     id
                    }
                }
            }
            """)
              .variable("input", createPostInput)
              .execute()
              .path("createPost.success")
              .entity(Boolean.class)
              .isEqualTo(true)
              .path("createPost.post.id")
              .entity(UUID.class)
              .get();

      List<Mention> mentions = mentionRepository.findAll();
      assertThat(mentions).hasSize(2);
      assertThat(mentions).extracting(Mention::getPostId).containsOnly(postId);
      assertThat(mentions)
          .extracting(Mention::getMentionedUserId)
          .containsExactlyInAnyOrder(mentionedUserId1, mentionedUserId2);
    }
  }

  @Nested
  @DisplayName("updateMention")
  class UpdateMention {

    @Test
    void updateMention_nullMentionedUserIds_skipsUpdate() {
      List<UUID> mentionedUserId = List.of(users.get(1).getId());
      UUID createdPostId = addPostWithMentions(authenticatedTester, mentionedUserId);
      UpdatePostInput updatePostInput =
          new UpdatePostInput(createdPostId, "new message content", null);
      authenticatedTester
          .document(
              """
                mutation UpdatePost($input: UpdatePostInput!) {
                    updatePostContent(input: $input) {
                        success
                        post {
                         mentions {
                            id
                         }
                        }
                    }
                }
                """)
          .variable("input", updatePostInput)
          .execute()
          .path("updatePostContent.success")
          .entity(Boolean.class)
          .isEqualTo(true)
          .path("updatePostContent.post.mentions[*].id")
          .entityList(UUID.class)
          .hasSize(1)
          .isEqualTo(mentionedUserId);
    }

    @Test
    void updateMention_sameMentionedUsers() {
      List<UUID> mentionedUserId = List.of(users.get(1).getId());
      UUID createdPostId = addPostWithMentions(authenticatedTester, mentionedUserId);
      UpdatePostInput updatePostInput =
          new UpdatePostInput(createdPostId, "new message content", mentionedUserId);
      authenticatedTester
          .document(
              """
                mutation UpdatePost($input: UpdatePostInput!) {
                    updatePostContent(input: $input) {
                        success
                        post {
                         mentions {
                            id
                         }
                        }
                    }
                }
                """)
          .variable("input", updatePostInput)
          .execute()
          .path("updatePostContent.success")
          .entity(Boolean.class)
          .isEqualTo(true)
          .path("updatePostContent.post.mentions[*].id")
          .entityList(UUID.class)
          .hasSize(1)
          .isEqualTo(mentionedUserId);
    }

    @Test
    void updateMention_addsMentionedUsers() {
      UUID createdPostId = addPostWithMentions(authenticatedTester, List.of(users.get(1).getId()));
      List<UUID> newMentions = List.of(users.get(1).getId(), users.get(2).getId());
      UpdatePostInput updatePostInput =
          new UpdatePostInput(createdPostId, "new message content", newMentions);
      authenticatedTester
          .document(
              """
                  mutation UpdatePost($input: UpdatePostInput!) {
                      updatePostContent(input: $input) {
                          success
                          post {
                           mentions {
                              id
                           }
                          }
                      }
                  }
                  """)
          .variable("input", updatePostInput)
          .execute()
          .path("updatePostContent.success")
          .entity(Boolean.class)
          .isEqualTo(true)
          .path("updatePostContent.post.mentions[*].id")
          .entityList(UUID.class)
          .hasSize(2)
          .isEqualTo(newMentions);
    }

    @Test
    void updateMention_deletesMentionedUsers_lessMentionedUserIdsSent() {
      UUID createdPostId =
          addPostWithMentions(
              authenticatedTester, List.of(users.get(1).getId(), users.get(2).getId()));
      List<UUID> lessMentions = List.of(users.get(1).getId());
      UpdatePostInput updatePostInput =
          new UpdatePostInput(createdPostId, "new message content", lessMentions);
      authenticatedTester
          .document(
              """
                mutation UpdatePost($input: UpdatePostInput!) {
                    updatePostContent(input: $input) {
                        success
                        post {
                         mentions {
                            id
                         }
                        }
                    }
                }
                """)
          .variable("input", updatePostInput)
          .execute()
          .path("updatePostContent.success")
          .entity(Boolean.class)
          .isEqualTo(true)
          .path("updatePostContent.post.mentions[*].id")
          .entityList(UUID.class)
          .hasSize(1)
          .isEqualTo(lessMentions);
    }

    @Test
    void updateMention_deletesMentionedUsers_emptyMentionedUserIdsSent() {
      UUID createdPostId =
          addPostWithMentions(
              authenticatedTester, List.of(users.get(1).getId(), users.get(2).getId()));
      List<UUID> emptyMentions = List.of();
      UpdatePostInput updatePostInput =
          new UpdatePostInput(createdPostId, "new message content", emptyMentions);
      authenticatedTester
          .document(
              """
                mutation UpdatePost($input: UpdatePostInput!) {
                    updatePostContent(input: $input) {
                        success
                        post {
                         mentions {
                            id
                         }
                        }
                    }
                }
                """)
          .variable("input", updatePostInput)
          .execute()
          .path("updatePostContent.success")
          .entity(Boolean.class)
          .isEqualTo(true)
          .path("updatePostContent.post.mentions[*].id")
          .entityList(UUID.class)
          .hasSize(0)
          .isEqualTo(emptyMentions);
    }

    @Test
    void updateMention_addsAndDeletesMentionedUsers() {
      UUID createdPostId =
          addPostWithMentions(
              authenticatedTester, List.of(users.get(1).getId(), users.get(2).getId()));
      List<UUID> updatedMentions = List.of(users.get(1).getId(), users.get(3).getId());
      UpdatePostInput updatePostInput =
          new UpdatePostInput(createdPostId, "new message content", updatedMentions);
      authenticatedTester
          .document(
              """
                  mutation UpdatePost($input: UpdatePostInput!) {
                      updatePostContent(input: $input) {
                          success
                          post {
                           mentions {
                              id
                           }
                          }
                      }
                  }
                  """)
          .variable("input", updatePostInput)
          .execute()
          .path("updatePostContent.success")
          .entity(Boolean.class)
          .isEqualTo(true)
          .path("updatePostContent.post.mentions[*].id")
          .entityList(UUID.class)
          .hasSize(2)
          .isEqualTo(updatedMentions);
    }
  }
}
