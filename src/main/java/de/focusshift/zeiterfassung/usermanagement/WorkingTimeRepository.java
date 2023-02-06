package de.focusshift.zeiterfassung.usermanagement;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

interface WorkingTimeRepository extends CrudRepository<WorkingTimeEntity, Long> {

    Optional<WorkingTimeEntity> findByUserId(Long userId);
}
