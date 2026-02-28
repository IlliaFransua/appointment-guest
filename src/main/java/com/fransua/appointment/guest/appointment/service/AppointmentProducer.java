package com.fransua.appointment.guest.appointment.service;

import com.fransua.appointment.guest.appointment.dto.AppointmentVerificationEvent;
import com.fransua.appointment.guest.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentProducer {

  private final RabbitTemplate rabbitTemplate;

  public void sendSmsVerfication(String phone, String code) {
    rabbitTemplate.convertAndSend(
        RabbitConfig.APPOINTMENT_EXCHANGE,
        RabbitConfig.APPOINTMENT_CONFIRM_SMS_RK,
        new AppointmentVerificationEvent(phone, code));
  }
}
