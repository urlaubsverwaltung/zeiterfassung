package de.focusshift.zeiterfassung.activitytype;

import java.util.List;

public interface ActivityTypeService {

    List<ActivityType> findAllActive();

    List<ActivityType> findAll();

    ActivityType create(String name);

    ActivityType update(ActivityTypeId id, String name, boolean active);

    void delete(ActivityTypeId id);
}
