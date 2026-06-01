package de.focusshift.zeiterfassung.project;

import de.focusshift.zeiterfassung.customer.CustomerId;

public record Project(ProjectId id, String name, boolean active, CustomerId customerId, String customerName) {
}
