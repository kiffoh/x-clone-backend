package com.xclone.user.service;

import com.xclone.user.dto.UserProfile;
import com.xclone.user.dto.connection.PageInfo;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.connection.UserEdge;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import com.xclone.validation.ValidHandle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public static UserConnection toUserConnection(List<User> users) {
    List<UserEdge> edges =
        users.stream()
            .map(user -> new UserEdge(user.toUserProfile(), user.getId().toString()))
            .toList();
    PageInfo pageInfo =
        new PageInfo(
            false,
            false,
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor());
    return new UserConnection(edges, pageInfo, edges.size());
  }

  public UserProfile getUserByHandle(@ValidHandle String handle) {
    Optional<User> user = userRepository.findByHandle(handle);
    return user.map(User::toUserProfile).orElse(null);
  }

  public UserProfile getUserById(UUID id) {
    Optional<User> user = userRepository.findById(id);
    return user.map(User::toUserProfile).orElse(null);
  }

  public UserConnection getUsersByHandle(String query) {
    List<User> users = userRepository.findAllByHandleContaining(query);
    return toUserConnection(users);
  }
}
