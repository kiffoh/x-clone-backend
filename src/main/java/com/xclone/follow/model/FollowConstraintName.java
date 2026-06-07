package com.xclone.follow.model;

/** Constants representing business constraint names for follow relationships. */
public class FollowConstraintName {
  public static final String FOLLOW_EXISTS = "follow_constraint_follow_exists";
  public static final String SELF_FOLLOW = "follow_constraint_self_follow";
  public static final String FOLLOWER_FK = "follow_constraint_follower_fk";
  public static final String FOLLOWING_FK = "follow_constraint_following_fk";
}
