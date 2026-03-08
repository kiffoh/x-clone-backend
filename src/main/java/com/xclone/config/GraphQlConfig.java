package com.xclone.config;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Registers custom scalar mappings for GraphQL serialization.
 * Currently maps {@link java.time.OffsetDateTime} to the {@code DateTime} scalar
 * via {@link graphql.scalars.ExtendedScalars#DateTime}.
 */
@Configuration
public class GraphQlConfig {

  @Bean
  public RuntimeWiringConfigurer runtimeWiringConfigurer() {
    return wiringBuilder -> wiringBuilder.scalar(ExtendedScalars.DateTime);
  }
}
