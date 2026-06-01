package de.focusshift.zeiterfassung.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    List<CustomerEntity> findAllByActiveTrueOrderByNameAsc();

    List<CustomerEntity> findAllByOrderByNameAsc();
}
