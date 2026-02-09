package com.xclone.auth.dto;

import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

// Should all of these classes extend a base class so that they all use the same instance of axios
// or whatever request library I use?
//* Logic for creating new users. */
@Service // Is this correct?
public class SignUpRequest {
  private final UserRepository userRepository;


  SignUpRequest(
      UserRepository userRepository
  ) {
    this.userRepository = userRepository;
  }

  @PostMapping("/signup")
  User createUser(HttpServletRequest request, HttpServletResponse response) {
    try {
      // Take the information username and password -> hash the password and pass it to the database
      // What is the industry standard, to hash on server side?
      // - Otherwise we are trusting the client information?

      String handle = Arrays.toString(request.getParameterValues("handle"));
      if (userRepository.existsByHandle(handle)) {
        throw new IllegalAccessError("Not a unique handle");
      }
      // How is validation done in Spring Boot? Is there something like zod (validation
      // middleware) which can be used so that I can treat the values as non null?
      // Can I assume that validation is done at this point e.g. that password is a non-null string
      // with appropriate characters e.g. length and included character
      String password = Arrays.toString(request.getParameterValues("password"));
      User user = new User();
      user.setHandle(handle);
      // Can I encode the password with passwordEncoder in SecurityConfig?
      user.setPasswordHash(password); // Will have to encrypt at some point
      userRepository.save(user);
      return user;
    } catch (DataIntegrityViolationException e) {
      throw new IllegalArgumentException("Not a unique handle");
    }
  }
}

