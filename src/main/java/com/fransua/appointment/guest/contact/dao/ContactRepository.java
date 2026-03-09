package com.fransua.appointment.guest.contact.dao;

import com.fransua.appointment.guest.contact.Contact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

  Optional<Contact> findByAppointmentIdAndType(Long appointmentId, Contact.Type type);

  Optional<Contact> findByAppointmentId(Long appointmentId);

  Optional<Contact> findByIdAndAppointmentIdAndType(
      Long contactId, Long appointmentId, Contact.Type type);

  List<Contact> findAllByAppointmentId(Long appointmentId);

  void deleteAllByAppointmentId(Long appointmentId);
}
