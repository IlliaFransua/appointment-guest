package com.fransua.appointment.guest.master.feign;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import feign.okhttp.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class MasterClientConfiguration {

  @Bean
  OkHttpClient client() {
    return new OkHttpClient();
  }

  @Bean
  RequestInterceptor internalTokenInterceptor(@Value("${internal.token}") String internalToken) {
    return template -> template.header("X-Internal-Token", internalToken);
  }

  @Bean
  ErrorDecoder errorDecoder() {
    return new MasterErrorDecoder();
  }
}
