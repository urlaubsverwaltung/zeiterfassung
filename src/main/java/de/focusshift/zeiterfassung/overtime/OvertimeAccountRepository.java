package de.focusshift.zeiterfassung.overtime;

import org.springframework.data.jpa.repository.JpaRepository;

interface OvertimeAccountRepository extends JpaRepository<OvertimeAccountEntity, Long> {
}
