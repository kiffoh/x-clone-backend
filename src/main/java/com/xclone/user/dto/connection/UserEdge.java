package com.xclone.user.dto.connection;

import com.xclone.user.dto.UserProfile;

public record UserEdge(UserProfile node, String cursor) {}
