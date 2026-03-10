package com.fransua.appointment.guest.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayApiEuConfig {

  private final String token;

  public GatewayApiEuConfig(@Value("${gatewayapieu.token}") String token) {
    this.token = token;
  }

  @Bean
  @Qualifier("gatewayApiEuRestClient")
  RestClient restClientSingle() {
    return RestClient.builder()
        .baseUrl("https://messaging.gatewayapi.eu")
        .defaultHeader("Authorization", "Token " + token)
        .defaultHeader("Content-Type", "application/json")
        .build();
  }
}
