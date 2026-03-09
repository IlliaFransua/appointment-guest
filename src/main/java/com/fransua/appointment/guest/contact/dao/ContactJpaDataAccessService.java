package com.fransua.appointment.guest.contact.dao;

import com.fransua.appointment.guest.contact.Contact;
import com.fransua.appointment.guest.contact.Contact.Type;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContactJpaDataAccessService implements ContactDao {

  private final ContactRepository repository;

  @Override
  public Optional<Contact> findByAppointmentIdAndType(Long appointmentId, Type type) {
    return repository.findByAppointmentIdAndType(appointmentId, type);
  }

  @Override
  public Optional<Contact> findByAppointmentId(Long appointmentId) {
    return repository.findByAppointmentId(appointmentId);
  }

  @Override
  public Optional<Contact> findByIdAndAppointmentIdAndType(
      Long contactId, Long appointmentId, Type type) {
    return repository.findByIdAndAppointmentIdAndType(contactId, appointmentId, type);
  }

  @Override
  public Contact save(Contact entity) {
    return repository.save(entity);
  }

  @Override
  public void update(Contact entity) {
    repository.save(entity);
  }

  @Override
  public List<Contact> findAllByAppointmentId(Long appointmentId) {
    return repository.findAllByAppointmentId(appointmentId);
  }

  @Override
  public void deleteAllByAppointmentId(Long appointmentId) {
    repository.deleteAllByAppointmentId(appointmentId);
  }
}
