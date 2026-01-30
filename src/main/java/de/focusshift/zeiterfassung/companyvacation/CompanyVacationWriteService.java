package de.focusshift.zeiterfassung.companyvacation;


import java.time.Instant;

public interface CompanyVacationWriteService {

    /**
     * Add a new {@linkplain CompanyVacationWrite}
     *
     * @param companyVacation company vacation to add
     */
    void addOrUpdateCompanyVacation(Instant createdAt, CompanyVacationWrite companyVacation);

    /**
     * Delete an {@linkplain CompanyVacationWrite}
     *
     * @param sourceId sourceId of company vacation to delete
     */
    void deleteCompanyVacation(Instant createdAt, String sourceId);
}
