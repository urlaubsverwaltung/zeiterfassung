package de.focusshift.zeiterfassung.workingtime;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.history.RevisionRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface WorkingTimeRepository extends CrudRepository<WorkingTimeEntity, UUID>, RevisionRepository<WorkingTimeEntity, UUID, Long> {

    List<WorkingTimeEntity> findAllByUserId(Long userId);

    List<WorkingTimeEntity> findAllByUserIdIsIn(Collection<Long> userIds);
}
