package com.fransua.appointment.guest.master.dto.booking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GuestFields {
  PHONE(true, "Phone Number"),
  EMAIL(false, "Email Address"),
  TELEGRAM(false, "Telegram"),
  WHATSAPP(true, "WhatsApp");

  private final boolean isPaid;
  private final String displayName;

  public boolean isFree() {
    return !isPaid;
  }
}
