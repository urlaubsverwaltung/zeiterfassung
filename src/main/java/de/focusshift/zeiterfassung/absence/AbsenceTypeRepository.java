package de.focusshift.zeiterfassung.absence;

import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface AbsenceTypeRepository extends CrudRepository<AbsenceTypeEntity, Long> {

    Optional<AbsenceTypeEntity> findBySourceId(Long sourceId);

    List<AbsenceTypeEntity> findBySourceIdIsIn(Collection<Long> sourceIds);
}
