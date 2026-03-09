package com.fransua.appointment.guest.contact.services;

import com.fransua.appointment.guest.config.RabbitConfig;
import com.fransua.appointment.guest.contact.event.PhoneVerificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhoneVerificationListener {

  private final SmartValidator validator;

  @RabbitListener(queues = RabbitConfig.APPOINTMENT_CONFIRM_PHONE_QUEUE)
  void handleVerificationEvent(PhoneVerificationEvent event) {
    validateEvent(event);
    System.out.println("Send sms: " + event.code());
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
