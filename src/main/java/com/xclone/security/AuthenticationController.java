package com.xclone.security;

import org.springframework.web.bind.annotation.RestController;

/**
 * Router for authentication REST API calls.
 */
@RestController
public class AuthenticationController extends RuntimeException {
  //       private final UserRepository userRepository;

  //      AuthenticationController(UserRepository userRepository) {
  //          this.userRepository = userRepository;
  //      }

  //      // All routes will have /auth here - can I set this at a higher level?
  //      // How is validation done in Spring Boot? Is there something like zod (validation
  //    // middleware) which can be used so that I can treat the values as non null?
  //      @PostMapping("/sign-up")
  //      public User createUser(HttpServletRequest request, HttpServletResponse response) {
  //          // Take the information username and password
  //          -> hash the password and pass it to the datbase.
  //          // What is the industry standard, to hash on server side?
  //          Otherwise we are trusting the client information?

  //          String handle = Arrays.toString(request.getParameterValues("handle"));
  //          if (handleIsUnique(handle)) {
  //              String password = Arrays.toString(request.getParameterValues("password"));
  //              User user = new User();
  //              user.setHandle(handle);
  //              // Can I encode the password with passwordEncoder in SecurityConfig?
  //              user.setPasswordHash(password); // Will have to encrypt at some point
  //              userRepository.save(user);
  //              return user;
  //          }
  //          throw new IllegalAccessError("Not a unique handle"); // Not unique error
  //      }

  //      public boolean handleIsUnique(String handle) {
  //          User user = new User();
  //          user.setHandle(handle);
  //          return userRepository.exists(user);
  //      }

  //       @PostMapping("/log-in")
  //      public Optional<User> logIn(HttpServletRequest request, HttpServletResponse response) {

  //      }

  //      @PostMapping("/log-out")
  //      public void logOut(HttpServletRequest request, HttpServletResponse response) {
  //          AccessToken token = SecurityContextHolder.getContext().getAuthentication();
  //          // Send an empty response which also resets the clients http cookies
  //      }
}
