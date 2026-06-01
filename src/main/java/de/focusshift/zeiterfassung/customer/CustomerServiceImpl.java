package de.focusshift.zeiterfassung.customer;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public List<Customer> findAllActive() {
        return customerRepository.findAllByActiveTrueOrderByNameAsc().stream()
            .map(this::toCustomer)
            .toList();
    }

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAllByOrderByNameAsc().stream()
            .map(this::toCustomer)
            .toList();
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return customerRepository.findById(id.value()).map(this::toCustomer);
    }

    @Override
    public Customer create(String name) {
        final CustomerEntity entity = new CustomerEntity();
        entity.setName(name);
        entity.setActive(true);
        return toCustomer(customerRepository.save(entity));
    }

    @Override
    public Customer update(CustomerId id, String name, boolean active) {
        final CustomerEntity entity = customerRepository.findById(id.value())
            .orElseThrow(() -> new IllegalStateException("could not find customer id=%s".formatted(id)));
        entity.setName(name);
        entity.setActive(active);
        return toCustomer(customerRepository.save(entity));
    }

    @Override
    public void delete(CustomerId id) {
        customerRepository.deleteById(id.value());
    }

    private Customer toCustomer(CustomerEntity entity) {
        return new Customer(new CustomerId(entity.getId()), entity.getName(), entity.isActive());
    }
}
