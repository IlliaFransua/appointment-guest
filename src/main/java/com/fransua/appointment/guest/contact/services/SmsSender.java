package com.fransua.appointment.guest.contact.services;

import com.fransua.appointment.guest.contact.dto.gatewayapieu.MobileMessageRequest;
import com.fransua.appointment.guest.contact.dto.gatewayapieu.MobileMessageResponse;

public interface SmsSender {

  MobileMessageResponse send(MobileMessageRequest request);
}
