package com.fransua.appointment.guest.contact;

import com.fransua.appointment.guest.appointment.Appointment;
import com.fransua.appointment.guest.appointment.dao.AppointmentDao;
import com.fransua.appointment.guest.contact.dao.ContactDao;
import com.fransua.appointment.guest.contact.dto.ContactResponse;
import com.fransua.appointment.guest.contact.dto.PhoneRequest;
import com.fransua.appointment.guest.contact.dto.VerificationCodeRequest;
import com.fransua.appointment.guest.contact.services.PhoneVerificationProducer;
import com.fransua.appointment.guest.exception.RequestValidationException;
import com.fransua.appointment.guest.exception.ResourceNotFoundException;
import com.fransua.appointment.guest.exception.ServiceUnavailableException;
import com.fransua.appointment.guest.master.MasterClient;
import com.fransua.appointment.guest.master.dto.booking.GuestFields;
import com.fransua.appointment.guest.master.dto.booking.GuestFieldsResponse;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class ContactService {

  private final MasterClient masterClient;

  private final ContactDao contactDao;

  private final AppointmentDao appointmentDao;

  private final ContactMapper contactMapper;

  private final PhoneVerificationProducer phoneVerificationProducer;

  @Transactional(readOnly = true)
  public List<ContactResponse> getAllContacts(Long appointmentId) {
    var appt = getAppointmentByIdAndStatus(appointmentId, Appointment.Status.CREATED);
    var contacts = contactDao.findAllByAppointmentId(appt.getId());
    return contactMapper.toResponse(contacts);
  }

  @Transactional
  public ContactResponse attachPhone(Long appointmentId, PhoneRequest request) {
    var appt = getAppointmentByIdAndStatus(appointmentId, Appointment.Status.CREATED);
    ensureNoPhoneAttached(appt.getId());
    ensurePhoneIsSupported(request);

    validateFieldRequirement(
        appt.getSlug(),
        GuestFieldsResponse::guestRequiredFields,
        "Phone field is not required to be attached for this appointment");

    return contactMapper.toResponse(saveNewContact(appt.getId(), request, Contact.Status.ATTACHED));
  }

  @Transactional
  public ContactResponse editPhone(Long appointmentId, PhoneRequest request) {
    var appt = getAppointmentByIdAndStatus(appointmentId, Appointment.Status.CREATED);
    var contact = getContactByAppointmentId(appt.getId());
    ensureContactIsNotVerified(contact);
    ensurePhoneIsSupported(request);

    if (contact.getValue().equals(request.phone())) {
      contact.setStatus(Contact.Status.ATTACHED);
      return contactMapper.toResponse(contact);
    }

    updateContactValue(contact, request, Contact.Status.ATTACHED);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCommit() {
            phoneVerificationProducer.clearAllApptLimits(contact.getAppointmentId());
          }
        });
    return contactMapper.toResponse(contact);
  }

  @Transactional
  public void sendVerificationCode(Long contactId, Long appointmentId) {
    boolean isResend = false;
    processVerificationCode(contactId, appointmentId, isResend);
  }

  @Transactional
  public void resendVerificationCode(Long contactId, Long appointmentId) {
    boolean isResend = true;
    processVerificationCode(contactId, appointmentId, isResend);
  }

  @Transactional
  private void processVerificationCode(Long contactId, Long appointmentId, boolean isResend) {
    ensureVerificationSystemIsAvailable();

    var appt = getAppointmentByIdAndStatus(appointmentId, Appointment.Status.CREATED);
    var contact = getContactByIdAndAppointmentId(contactId, appt.getId(), Contact.Type.PHONE);
    String phone = contact.getValue();

    if (isResend) {
      ensureContactIsNotVerified(contact);
    } else {
      ensureContactAttached(contact);
      ensurePhoneIsSupported(new PhoneRequest(phone));
      validateFieldRequirement(
          appt.getSlug(),
          GuestFieldsResponse::guestFieldsToVerify,
          "Phone field is not required to be verified for this appointment");
    }

    contact.setStatus(Contact.Status.PENDING_VERIFICATION);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            if (isResend) {
              phoneVerificationProducer.resendVerificationCode(appt, new PhoneRequest(phone));
            } else {
              phoneVerificationProducer.sendVerificationCode(appt, new PhoneRequest(phone));
            }
          }
        });
  }

  @Transactional
  public void confirmPhone(Long contactId, Long appointmentId, VerificationCodeRequest request) {
    var appt = getAppointmentByIdAndStatus(appointmentId, Appointment.Status.CREATED);
    var contact = getContactByIdAndAppointmentId(contactId, appt.getId(), Contact.Type.PHONE);
    ensureContactIsWaitingForConfirmation(contact);
    phoneVerificationProducer.verifyCode(contact.getAppointmentId(), request.code());
    contact.setStatus(Contact.Status.VERIFIED);
  }

  // util

  private void ensureNoPhoneAttached(Long appointmentId) {
    contactDao
        .findByAppointmentIdAndType(appointmentId, Contact.Type.PHONE)
        .ifPresent(
            contact -> {
              throw new RequestValidationException("Some phone already attached");
            });
  }

  private void ensureContactAttached(Contact contact) {
    if (!contact.getStatus().equals(Contact.Status.ATTACHED)) {
      throw new RequestValidationException("Contact is not attached");
    }
  }

  private void ensureContactIsWaitingForConfirmation(Contact contact) {
    if (!contact.getStatus().equals(Contact.Status.PENDING_VERIFICATION)) {
      throw new RequestValidationException("Phone don't need to be confirmed");
    }
  }

  private void ensureContactIsNotVerified(Contact contact) {
    if (contact.getStatus().equals(Contact.Status.VERIFIED)) {
      throw new RequestValidationException("Appointment already has a verified phone attached");
    }
  }

  private void ensurePhoneIsSupported(PhoneRequest request) {
    if (phoneVerificationProducer.isPhoneUnsupported(request)) {
      throw new RequestValidationException("This phone number is unsupported");
    }
  }

  private void ensureVerificationSystemIsAvailable() {
    if (phoneVerificationProducer.isVerificationSystemDisabled()) {
      throw new ServiceUnavailableException("Verification system is unvailable now");
    }
  }

  private void validateFieldRequirement(
      String slug,
      Function<GuestFieldsResponse, List<GuestFields>> fieldSelector,
      String errorMessage) {
    var fields = masterClient.getRequiredGuestFields(slug);
    if (!fieldSelector.apply(fields).contains(GuestFields.PHONE)) {
      throw new RequestValidationException(errorMessage);
    }
  }

  private Contact saveNewContact(Long appointmentId, PhoneRequest request, Contact.Status status) {
    return contactDao.save(contactMapper.toEntity(appointmentId, status, request));
  }

  private void updateContactValue(Contact contact, PhoneRequest request, Contact.Status status) {
    contact.setValue(request.phone());
    contact.setValueHash(null);
    contact.setStatus(status);
    contactDao.update(contact);
  }

  private Appointment getAppointmentByIdAndStatus(Long appointmentId, Appointment.Status status) {
    return appointmentDao
        .findByIdAndStatus(appointmentId, status)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Appointment with id [%d] and status not found"
                        .formatted(appointmentId, status)));
  }

  private Contact getContactByIdAndAppointmentId(
      Long contactId, Long appointmentId, Contact.Type type) {
    return contactDao
        .findByIdAndAppointmentIdAndType(contactId, appointmentId, type)
        .orElseThrow(() -> new ResourceNotFoundException("Contact", contactId));
  }

  private Contact getContactByAppointmentId(Long appointmentId) {
    return contactDao
        .findByAppointmentId(appointmentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "No contact found with appointment id [%d]".formatted(appointmentId)));
  }
}
