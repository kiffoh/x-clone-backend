package com.xclone.config;

import com.xclone.exception.dto.ErrorResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.exception.dto.ValidationErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  private final ApiResponse badRequest = new ApiResponse()
      .description("Bad request")
      .content(new Content().addMediaType(
          "application/json",
          new MediaType().schema(
              new Schema<ValidationErrorResponse>()
                  .$ref("#/components/schemas/ValidationErrorResponse")
          )
      ));
  private final ApiResponse unauthorizedRequest = new ApiResponse()
      .description("Unauthorized request")
      .content(new Content().addMediaType(
          "application/json",
          new MediaType().schema(
              new Schema<ErrorResponse>()
                  .$ref("#/components/schemas/ErrorResponse")
          )
      ));
  private final ApiResponse conflictingResource = new ApiResponse()
      .description("Conflicting Resource")
      .content(new Content().addMediaType(
          "application/json",
          new MediaType().schema(
              new Schema<ErrorResponse>()
                  .$ref("#/components/schemas/ErrorResponse")
          )
      ));
  private final ApiResponse forbiddenResource = new ApiResponse()
      .description("Forbidden Resource")
      .content(new Content().addMediaType(
          "application/json",
          new MediaType().schema(
              new Schema<ErrorResponse>()
                  .$ref("#/components/schemas/ErrorResponse")
          )
      ));
  private final ApiResponse internalServerError = new ApiResponse()
      .description("Internal Server Error")
      .content(new Content().addMediaType(
          "application/json",
          new MediaType().schema(
              new Schema<ErrorResponse>()
                  .$ref("#/components/schemas/ErrorResponse")
          )
      ));

  private final Map<String, Schema> errorSchema =
      ModelConverters.getInstance().readAll(ErrorResponse.class);
  private final Map<String, Schema> validationErrorSchema =
      ModelConverters.getInstance().readAll(ValidationErrorResponse.class);
  private final Map<String, Schema> fieldErrorSchema =
      ModelConverters.getInstance().readAll(FieldError.class);

  @Bean
  public OpenAPI customOpenApi() {
    return new OpenAPI().components(new Components()
        .addSchemas("ValidationErrorResponse", validationErrorSchema.get("ValidationErrorResponse"))
        .addSchemas("ErrorResponse", errorSchema.get("ErrorResponse"))
        .addSchemas("FieldError", fieldErrorSchema.get("FieldError"))
        .addResponses("BadRequestError", badRequest)
        .addResponses("UnauthorizedError", unauthorizedRequest)
        .addResponses("ConflictError", conflictingResource)
        .addResponses("ForbiddenError", forbiddenResource)
        .addResponses("InternalServerError", internalServerError)
    );
  }

  @Bean
  public OpenApiCustomizer orderPaths() {
    return openApi -> {
      openApi.setPaths(createCustomOrder(openApi.getPaths()));
    };
  }

  private Paths createCustomOrder(Paths currentPaths) {
    Paths customPaths = new Paths();
    customPaths.addPathItem("/api/auth/signup", currentPaths.get("/api/auth/signup"));
    customPaths.addPathItem("/api/auth/login", currentPaths.get("/api/auth/login"));
    customPaths.addPathItem("/api/auth/logout", currentPaths.get("/api/auth/logout"));
    customPaths.addPathItem("/api/auth/refresh", currentPaths.get("/api/auth/refresh"));
    return customPaths;
  }
}
