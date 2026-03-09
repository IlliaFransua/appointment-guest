package com.fransua.appointment.guest.contact;

import com.fransua.appointment.guest.contact.dto.ContactResponse;
import com.fransua.appointment.guest.contact.dto.PhoneRequest;
import com.fransua.appointment.guest.contact.dto.VerificationCodeRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact/{appointmentId}/phone")
@RequiredArgsConstructor
public class ContactController {

  private final ContactService contactService;

  @GetMapping("/get-all")
  public List<ContactResponse> getAllContacts(@PathVariable Long appointmentId) {
    return contactService.getAllContacts(appointmentId);
  }

  @PostMapping("/attach")
  @ResponseStatus(HttpStatus.CREATED)
  public ContactResponse attachPhone(
      @PathVariable Long appointmentId, @RequestBody @Valid PhoneRequest request) {
    return contactService.attachPhone(appointmentId, request);
  }

  @PostMapping("/edit")
  @ResponseStatus(HttpStatus.OK)
  public ContactResponse editPhone(
      @PathVariable Long appointmentId, @RequestBody @Valid PhoneRequest request) {
    return contactService.editPhone(appointmentId, request);
  }

  @PostMapping("/send-verification-code/{contactId}")
  @ResponseStatus(HttpStatus.OK)
  public void sendVerificationCode(@PathVariable Long appointmentId, @PathVariable Long contactId) {
    contactService.sendVerificationCode(contactId, appointmentId);
  }

  @PostMapping("/resend-verification-code/{contactId}")
  @ResponseStatus(HttpStatus.OK)
  public void resendVerificationCode(
      @PathVariable Long appointmentId, @PathVariable Long contactId) {
    contactService.resendVerificationCode(contactId, appointmentId);
  }

  @PostMapping("/confirm/{contactId}")
  @ResponseStatus(HttpStatus.OK)
  public void confirmPhone(
      @PathVariable Long appointmentId,
      @PathVariable Long contactId,
      @RequestBody @Valid VerificationCodeRequest request) {
    contactService.confirmPhone(contactId, appointmentId, request);
  }
}
