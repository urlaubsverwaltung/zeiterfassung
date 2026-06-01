package de.focusshift.zeiterfassung.project;

import de.focusshift.zeiterfassung.customer.CustomerId;

import java.util.List;

public interface ProjectService {

    List<Project> findAllActive();

    List<Project> findAll();

    List<Project> findAllByCustomer(CustomerId customerId);

    List<Project> findAllActiveByCustomer(CustomerId customerId);

    Project create(CustomerId customerId, String name);

    Project update(ProjectId id, String name, boolean active);

    void delete(ProjectId id);
}
