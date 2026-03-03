package com.xclone.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for JSON Web Token (JWT) settings.
 *
 * <p>Binds properties with the prefix {@code jwt} from the application configuration into a
 * strongly typed object.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
  String secret;
  String algorithm;
  String issuer;
}
