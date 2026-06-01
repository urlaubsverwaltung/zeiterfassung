package de.focusshift.zeiterfassung.customer;

import java.util.List;

public interface CustomerService {

    List<Customer> findAllActive();

    List<Customer> findAll();

    Customer create(String name);

    Customer update(CustomerId id, String name, boolean active);

    void delete(CustomerId id);
}
