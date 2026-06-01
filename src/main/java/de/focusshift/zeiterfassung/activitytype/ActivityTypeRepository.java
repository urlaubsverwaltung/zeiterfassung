package de.focusshift.zeiterfassung.activitytype;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ActivityTypeRepository extends JpaRepository<ActivityTypeEntity, Long> {

    List<ActivityTypeEntity> findAllByActiveTrueOrderByNameAsc();

    List<ActivityTypeEntity> findAllByOrderByNameAsc();
}
