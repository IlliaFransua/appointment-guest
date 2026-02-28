package com.fransua.appointment.guest.appointment.service;

import com.fransua.appointment.guest.appointment.dto.AppointmentVerificationEvent;
import com.fransua.appointment.guest.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentListener {

  @RabbitListener(queues = RabbitConfig.APPOINTMENT_CONFIRM_SMS_QUEUE)
  void handleAppointmentVerificationEvent(AppointmentVerificationEvent event) {
    System.out.println("Listener: " + event);
  }
}
