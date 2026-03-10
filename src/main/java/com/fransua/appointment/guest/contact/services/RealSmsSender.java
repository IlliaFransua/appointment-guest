package com.fransua.appointment.guest.contact.services;

import com.fransua.appointment.guest.contact.dto.gatewayapieu.MobileMessageRequest;
import com.fransua.appointment.guest.contact.dto.gatewayapieu.MobileMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@ConditionalOnProperty(name = "gatewayapieu.is-test-mode", havingValue = "false")
public class RealSmsSender implements SmsSender {

  private final RestClient restClient;

  private final PhoneVerificationProducer phoneVerificationProducer;

  public RealSmsSender(
      @Qualifier("gatewayApiEuRestClient") RestClient restClient,
      PhoneVerificationProducer phoneVerificationProducer) {
    this.restClient = restClient;
    this.phoneVerificationProducer = phoneVerificationProducer;
  }

  @Override
  public MobileMessageResponse send(MobileMessageRequest request) {
    return restClient
        .post()
        .uri("/mobile/single")
        .body(request)
        .retrieve()
        .onStatus(
            status -> status.value() == 422,
            (req, res) -> {
              String errorJson = new String(res.getBody().readAllBytes());
              log.error("GatewayAPI rejected data (422). Details: {}", errorJson);
              throw new AmqpRejectAndDontRequeueException(
                  "Validation failed at Gateway: " + errorJson);
            })
        .onStatus(
            HttpStatusCode::isError,
            (req, res) -> {
              log.error("GatewayAPI returned an error. Status: {}", res.getStatusCode());
              phoneVerificationProducer.recordSystemFault();
              throw new AmqpRejectAndDontRequeueException("Provider error, message rejected");
            })
        .body(MobileMessageResponse.class);
  }
}
