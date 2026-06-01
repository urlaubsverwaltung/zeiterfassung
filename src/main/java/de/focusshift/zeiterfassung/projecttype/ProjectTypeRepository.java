package de.focusshift.zeiterfassung.projecttype;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ProjectTypeRepository extends JpaRepository<ProjectTypeEntity, Long> {

    List<ProjectTypeEntity> findAllByActiveTrueOrderByNameAsc();

    List<ProjectTypeEntity> findAllByOrderByNameAsc();
}
