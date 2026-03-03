package com.xclone.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration enabling auditing and repository scanning.
 *
 * <p>Defined separately from the main application class to improve test isolation in MockMvc-based
 * test suites.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.xclone")
public class JpaConfig {}
