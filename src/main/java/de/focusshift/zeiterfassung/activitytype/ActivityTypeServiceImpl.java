package de.focusshift.zeiterfassung.activitytype;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ActivityTypeServiceImpl implements ActivityTypeService {

    private final ActivityTypeRepository activityTypeRepository;

    ActivityTypeServiceImpl(ActivityTypeRepository activityTypeRepository) {
        this.activityTypeRepository = activityTypeRepository;
    }

    @Override
    public List<ActivityType> findAllActive() {
        return activityTypeRepository.findAllByActiveTrueOrderByNameAsc().stream()
            .map(this::toActivityType)
            .toList();
    }

    @Override
    public List<ActivityType> findAll() {
        return activityTypeRepository.findAllByOrderByNameAsc().stream()
            .map(this::toActivityType)
            .toList();
    }

    @Override
    public ActivityType create(String name) {
        final ActivityTypeEntity entity = new ActivityTypeEntity();
        entity.setName(name);
        entity.setActive(true);
        return toActivityType(activityTypeRepository.save(entity));
    }

    @Override
    public ActivityType update(ActivityTypeId id, String name, boolean active) {
        final ActivityTypeEntity entity = activityTypeRepository.findById(id.value())
            .orElseThrow(() -> new IllegalStateException("could not find activity type id=%s".formatted(id)));
        entity.setName(name);
        entity.setActive(active);
        return toActivityType(activityTypeRepository.save(entity));
    }

    @Override
    public void delete(ActivityTypeId id) {
        activityTypeRepository.deleteById(id.value());
    }

    private ActivityType toActivityType(ActivityTypeEntity entity) {
        return new ActivityType(new ActivityTypeId(entity.getId()), entity.getName(), entity.isActive());
    }
}
