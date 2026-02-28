package com.xclone.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.xclone.auth.repository")
public class RefreshTokenStoreConfig {
}

