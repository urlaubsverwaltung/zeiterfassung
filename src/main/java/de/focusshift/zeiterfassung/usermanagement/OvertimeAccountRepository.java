package de.focusshift.zeiterfassung.usermanagement;

import org.springframework.data.jpa.repository.JpaRepository;

interface OvertimeAccountRepository extends JpaRepository<OvertimeAccountEntity, Long> {
}
