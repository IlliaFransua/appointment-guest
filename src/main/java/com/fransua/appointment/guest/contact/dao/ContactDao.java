package com.fransua.appointment.guest.contact.dao;

import com.fransua.appointment.guest.contact.Contact;
import java.util.List;
import java.util.Optional;

public interface ContactDao {

  Optional<Contact> findByAppointmentIdAndType(Long appointmentId, Contact.Type type);

  Optional<Contact> findByAppointmentId(Long appointmentId);

  Optional<Contact> findByIdAndAppointmentIdAndType(
      Long contactId, Long appointmentId, Contact.Type type);

  Contact save(Contact entity);

  void update(Contact entity);

  void deleteAllByAppointmentId(Long appointmentId);

  List<Contact> findAllByAppointmentId(Long appointmentid);

  List<Contact> findAllByAppointmentIdAndStatus(Long appointmentId, Contact.Status status);
}
