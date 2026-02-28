package com.fransua.appointment.guest.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

  public static final String APPOINTMENT_EXCHANGE = "appointments";
  public static final String APPOINTMENT_CONFIRM_SMS_RK = "confirmation_requested.sms";
  public static final String APPOINTMENT_CONFIRM_SMS_QUEUE =
      APPOINTMENT_EXCHANGE + "." + APPOINTMENT_CONFIRM_SMS_RK;

  @Bean
  TopicExchange appointmentExchange() {
    return new TopicExchange(APPOINTMENT_EXCHANGE);
  }

  @Bean
  Queue appointmentConfirmSmsQueue() {
    return new Queue(APPOINTMENT_CONFIRM_SMS_QUEUE, true);
  }

  @Bean
  Binding appointmentConfirmSmsBinding() {
    return BindingBuilder.bind(appointmentConfirmSmsQueue())
        .to(appointmentExchange())
        .with(APPOINTMENT_CONFIRM_SMS_RK);
  }

  @Bean
  Jackson2JsonMessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
