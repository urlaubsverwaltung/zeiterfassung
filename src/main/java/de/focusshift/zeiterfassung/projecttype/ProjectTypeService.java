package de.focusshift.zeiterfassung.projecttype;

import java.util.List;

public interface ProjectTypeService {

    List<ProjectType> findAllActive();

    List<ProjectType> findAll();

    ProjectType create(String name);

    ProjectType update(ProjectTypeId id, String name, boolean active);

    void delete(ProjectTypeId id);
}
