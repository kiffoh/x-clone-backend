package com.xclone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Main Spring Boot application for X-Clone backend.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.xclone")
@EnableRedisRepositories(basePackages = "com.xclone.auth.repository")
public class XCloneBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(XCloneBackendApplication.class, args);
  }
}
