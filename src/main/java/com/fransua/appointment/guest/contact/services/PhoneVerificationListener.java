package com.fransua.appointment.guest.contact.services;

import com.fransua.appointment.guest.config.RabbitConfig;
import com.fransua.appointment.guest.contact.dto.gatewayapieu.MobileMessageRequest;
import com.fransua.appointment.guest.contact.dto.gatewayapieu.MobileMessageResponse;
import com.fransua.appointment.guest.contact.event.PhoneVerificationEvent;
import com.fransua.appointment.guest.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;

// TODO: сделать лучше чтобы раз в минуту слушал ивенты и отправлял пачкой
@Slf4j
@Component
@RequiredArgsConstructor
public class PhoneVerificationListener {

  private final SmartValidator validator;

  private final SmsSender smsSender;

  @RabbitListener(queues = RabbitConfig.APPOINTMENT_CONFIRM_PHONE_QUEUE)
  void handleVerificationEvent(PhoneVerificationEvent event) {
    String maskedPhone = PhoneUtil.maskPhoneNumber(event.phoneNumber());
    log.info("Processing SMS verification for phone: {}", maskedPhone);

    validateEvent(event);

    try {
      MobileMessageRequest req = buildRequest(event);
      MobileMessageResponse res = smsSender.send(req);

      if (res != null) {
        log.info("SMS successfully accepted by provider. Message ID: {}", res.msgId());
        // TODO: billingService.deductSmsCredit(event.masterId());
      }

    } catch (NumberFormatException ex) {
      log.error("Fatal error: invalid phone number format. Dropping message.");
      throw new AmqpRejectAndDontRequeueException("Invalid phone format", ex);

    } catch (AmqpRejectAndDontRequeueException ex) {
      throw ex;

    } catch (Exception ex) {
      log.error("Unexpected error during SMS delivery to {}: {}", maskedPhone, ex.getMessage());
      throw new AmqpRejectAndDontRequeueException("Delivery failed due to technical issues", ex);
    }
  }

  private MobileMessageRequest buildRequest(PhoneVerificationEvent event) {
    return MobileMessageRequest.builder()
        .sender("Fransua")
        .recipient(Long.parseLong(event.phoneNumber()))
        .message("Your code is " + event.code())
        .expiration("PT10M")
        .priority("urgent")
        .build();
  }

  private void validateEvent(PhoneVerificationEvent event) {
    Errors errors = new BeanPropertyBindingResult(event, "event");
    validator.validate(event, errors);

    if (errors.hasErrors()) {
      log.error("Validation failed for event: {}", errors.getAllErrors());
      throw new AmqpRejectAndDontRequeueException("Invalid event data, skipping...");
    }
  }
}
