package de.focusshift.zeiterfassung.usermanagement;

import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface WorkingTimeRepository extends CrudRepository<WorkingTimeEntity, UUID> {

    Optional<WorkingTimeEntity> findByUserId(Long userId);

    List<WorkingTimeEntity> findAllByUserIdIsIn(Collection<Long> userIds);
}
