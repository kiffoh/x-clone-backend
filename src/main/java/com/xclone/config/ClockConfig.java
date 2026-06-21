package com.xclone.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a system-wide {@link Clock} bean for the application.
 *
 * <p>This abstraction is used instead of directly calling {@link java.time.Clock#systemUTC()} to
 * allow the clock to be overridden in integration tests, enabling deterministic time-based testing
 * and control over the current time.
 */
@Configuration
public class ClockConfig {
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
