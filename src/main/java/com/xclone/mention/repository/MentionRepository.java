package com.xclone.mention.repository;

import com.xclone.mention.dto.PostMention;
import com.xclone.mention.model.entity.Mention;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** JPA repository for {@link Mention} entities. */
public interface MentionRepository extends JpaRepository<Mention, UUID> {
  @Query(
      "select new com.xclone.mention.dto.PostMention(m.postId, u) from Mention m join User u "
          + "on m.mentionedUserId = u.id where m.postId in :postIds"
          + " and u.status = com.xclone.user.model.enums.UserStatus.ACTIVE")
  List<PostMention> findPostMentions(@Param("postIds") List<UUID> postIds);
}
