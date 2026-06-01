package de.focusshift.zeiterfassung.projecttype;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ProjectTypeServiceImpl implements ProjectTypeService {

    private final ProjectTypeRepository projectTypeRepository;

    ProjectTypeServiceImpl(ProjectTypeRepository projectTypeRepository) {
        this.projectTypeRepository = projectTypeRepository;
    }

    @Override
    public List<ProjectType> findAllActive() {
        return projectTypeRepository.findAllByActiveTrueOrderByNameAsc().stream()
            .map(this::toProjectType)
            .toList();
    }

    @Override
    public List<ProjectType> findAll() {
        return projectTypeRepository.findAllByOrderByNameAsc().stream()
            .map(this::toProjectType)
            .toList();
    }

    @Override
    public ProjectType create(String name) {
        final ProjectTypeEntity entity = new ProjectTypeEntity();
        entity.setName(name);
        entity.setActive(true);
        return toProjectType(projectTypeRepository.save(entity));
    }

    @Override
    public ProjectType update(ProjectTypeId id, String name, boolean active) {
        final ProjectTypeEntity entity = projectTypeRepository.findById(id.value())
            .orElseThrow(() -> new IllegalStateException("could not find project type id=%s".formatted(id)));
        entity.setName(name);
        entity.setActive(active);
        return toProjectType(projectTypeRepository.save(entity));
    }

    @Override
    public void delete(ProjectTypeId id) {
        projectTypeRepository.deleteById(id.value());
    }

    private ProjectType toProjectType(ProjectTypeEntity entity) {
        return new ProjectType(new ProjectTypeId(entity.getId()), entity.getName(), entity.isActive());
    }
}
