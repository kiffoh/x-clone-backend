package com.xclone.security;

import org.springframework.web.bind.annotation.RestController;

// Equivalent to a router
@RestController
public class AuthenticationController extends RuntimeException {
  //    private final UserRepository userRepository;
  //
  //    AuthenticationController(UserRepository userRepository) {
  //        this.userRepository = userRepository;
  //    }
  //
  //    // All routes will have /auth here - can I set this at a higher level?
  //    @PostMapping("/sign-up")
  //    // How is validation done in Spring Boot? Is there something like zod (validation
  // middleware) which can be used so that I can treat the values as non null?
  //    public User createUser(HttpServletRequest request, HttpServletResponse response) {
  //        String handle = Arrays.toString(request.getParameterValues("handle"));
  //        if (handleIsUnique(handle)) {
  //            String password = Arrays.toString(request.getParameterValues("password"));
  //            User user = new User();
  //            user.setHandle(handle);
  //            user.setPasswordHash(password); // Will have to encrypt at some point
  //            userRepository.save(user);
  //            return user;
  //        }
  //        throw new IllegalAccessError("Not a unique handle"); // Not unique error
  //    }
  //
  //    public boolean handleIsUnique(String handle) {
  //        User user = new User();
  //        user.setHandle(handle);
  //        return userRepository.exists(user);
  //    }
  //
  //    @PostMapping("/log-in")
  //    public User logIn(HttpServletRequest request, HttpServletResponse response) {
  //        AccessToken token = SecurityContextHolder.getContext().getAuthentication();
  //        userRepository.findBy()
  //    }
  //
  //    @PostMapping("/log-out")
  //    public void logIn(HttpServletRequest request, HttpServletResponse response) {
  //        AccessToken token = SecurityContextHolder.getContext().getAuthentication();
  //        // I need to set cookies on this request and return an empty access token?
  //    }
}
