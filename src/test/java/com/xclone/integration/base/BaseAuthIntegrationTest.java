package com.xclone.integration.base;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

public abstract class BaseAuthIntegrationTest extends BaseIntegrationTest {
  @Container @ServiceConnection
  static GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
}
