package com.xclone.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Intercepts requests to extract/validate tokens.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtUserDetailsService jwtUserDetailsService;
  private final String bearer;

  /**
   * Constructor; assigns components for internal use.
   */
  public JwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider, JwtUserDetailsService jwtUserDetailsService) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.jwtUserDetailsService = jwtUserDetailsService;
    this.bearer = "Bearer ";
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String token = extractTokenFromHeader(request);
    if (jwtTokenProvider.validToken(token)) {
      String userId = jwtTokenProvider.getUserIdFromToken(token);
      // UserDetails service next
      UserDetails userDetails = jwtUserDetailsService.getUserById(userId);
      // Set authentication in SecurityContext
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
    filterChain.doFilter(request, response);
  }

  private String extractTokenFromHeader(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ") || header.length() < 8) {
      return null;
    }
    return request.getHeader("Authorization").substring(this.bearer.length());
  }
}
