package com.fransua.appointment.guest.contact.internal;

import com.fransua.appointment.guest.contact.Contact;
import com.fransua.appointment.guest.contact.ContactService;
import com.fransua.appointment.guest.contact.dao.ContactDao;
import com.fransua.appointment.guest.contact.dto.ContactResponse;
import com.fransua.appointment.guest.contact.services.PhoneVerificationProducer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactInternalService {

  private final PhoneVerificationProducer phoneVerificationProducer;

  private final ContactService contactService;

  private final ContactDao contactDao;

  public boolean isVerificationSystemDisabled() {
    return phoneVerificationProducer.isVerificationSystemDisabled();
  }

  @Transactional(readOnly = true)
  public List<ContactResponse> getAllContacts(Long appointmentId) {
    return contactService.getAllContacts(appointmentId);
  }

  @Transactional
  public void finalizePendingContacts(Long appointmentId) {
    contactDao
        .findAllByAppointmentIdAndStatus(appointmentId, Contact.Status.PENDING_VERIFICATION)
        .forEach(contact -> contact.setStatus(Contact.Status.ATTACHED));
  }
}
