package de.focusshift.zeiterfassung.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    List<ProjectEntity> findAllByActiveTrueOrderByNameAsc();

    List<ProjectEntity> findAllByOrderByNameAsc();

    List<ProjectEntity> findAllByCustomerIdAndActiveTrueOrderByNameAsc(Long customerId);

    List<ProjectEntity> findAllByCustomerIdOrderByNameAsc(Long customerId);
}
