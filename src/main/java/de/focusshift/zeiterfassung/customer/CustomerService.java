package de.focusshift.zeiterfassung.customer;

import java.util.List;
import java.util.Optional;

public interface CustomerService {

    List<Customer> findAllActive();

    List<Customer> findAll();

    Optional<Customer> findById(CustomerId id);

    Customer create(String name);

    Customer update(CustomerId id, String name, boolean active);

    void delete(CustomerId id);
}
