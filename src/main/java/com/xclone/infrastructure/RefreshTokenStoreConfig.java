package com.xclone.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Redis configuration enabling repository scanning. Defined separately from the main application
 * class to improve test isolation in MockMvc-based test suites.
 */
@Configuration
@EnableRedisRepositories(basePackages = "com.xclone.auth.repository")
public class RefreshTokenStoreConfig {}
