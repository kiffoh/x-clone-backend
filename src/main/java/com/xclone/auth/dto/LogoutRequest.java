package com.xclone.auth.dto;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

/**
 * Removes the access token from the user's cookies, removes the refresh token from Redis.
 */
@Service
public class LogoutRequest {
  //      @PostMapping("/log-out")
  public void logOut(HttpServletRequest request, HttpServletResponse response) {
    //          String token = SecurityContextHolder.getContext().getAuthentication();
    //          // Send an empty response which also resets the clients http cookies
    response.addCookie(new Cookie("accessToken", "")); // Is this correct?
  }
}
