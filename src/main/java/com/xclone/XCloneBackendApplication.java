package com.xclone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot application for X-Clone backend.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.xclone.repository")
public class XCloneBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(XCloneBackendApplication.class, args);
  }
}
