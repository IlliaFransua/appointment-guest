package com.fransua.appointment.guest.contact.services;

import com.fransua.appointment.guest.contact.dto.gatewayapieu.MobileMessageRequest;
import com.fransua.appointment.guest.contact.dto.gatewayapieu.MobileMessageResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(
    name = "gatewayapieu.is-test-mode",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
@Service
public class MockSmsSender implements SmsSender {

  @Override
  public MobileMessageResponse send(MobileMessageRequest request) {
    log.info(
        "[MOCK SMS] Test mode active: SMS delivery suppressed. Recipient: {}, Content: '{}'",
        request.getRecipient(),
        request.getMessage());

    return new MobileMessageResponse("mock_id_" + UUID.randomUUID(), request.getRecipient());
  }
}
