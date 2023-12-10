package de.focusshift.zeiterfassung.workingtime;

import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface WorkingTimeRepository extends CrudRepository<WorkingTimeEntity, UUID> {

    List<WorkingTimeEntity> findAllByUserId(Long userId);

    List<WorkingTimeEntity> findAllByUserIdIsIn(Collection<Long> userIds);
}
