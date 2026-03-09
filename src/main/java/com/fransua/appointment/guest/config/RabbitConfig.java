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

  public static final String APPOINTMENT_EXCHANGE = "appointment";
  public static final String APPOINTMENT_CONFIRM_PHONE_RK = "confirmation_requested.phone";
  public static final String APPOINTMENT_CONFIRM_PHONE_QUEUE =
      APPOINTMENT_EXCHANGE + "." + APPOINTMENT_CONFIRM_PHONE_RK;

  @Bean
  TopicExchange appointmentExchange() {
    return new TopicExchange(APPOINTMENT_EXCHANGE);
  }

  @Bean
  Queue phoneQueue() {
    return new Queue(APPOINTMENT_CONFIRM_PHONE_QUEUE, true);
  }

  @Bean
  Binding phoneBinding() {
    return BindingBuilder.bind(phoneQueue())
        .to(appointmentExchange())
        .with(APPOINTMENT_CONFIRM_PHONE_RK);
  }

  @Bean
  Jackson2JsonMessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
