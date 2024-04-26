package de.focusshift.zeiterfassung.absence;

import java.util.Collection;
import java.util.List;

public interface AbsenceTypeService {

    void updateAbsenceType(AbsenceTypeUpdate absenceTypeUpdate);

    List<AbsenceType> findAllByAbsenceTypeSourceIds(Collection<Long> absenceTypeSourceIds);
}
