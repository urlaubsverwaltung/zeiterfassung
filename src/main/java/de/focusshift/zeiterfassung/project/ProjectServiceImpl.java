package de.focusshift.zeiterfassung.project;

import de.focusshift.zeiterfassung.customer.Customer;
import de.focusshift.zeiterfassung.customer.CustomerId;
import de.focusshift.zeiterfassung.customer.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final CustomerService customerService;

    ProjectServiceImpl(ProjectRepository projectRepository, CustomerService customerService) {
        this.projectRepository = projectRepository;
        this.customerService = customerService;
    }

    @Override
    public List<Project> findAllActive() {
        final List<ProjectEntity> entities = projectRepository.findAllByActiveTrueOrderByNameAsc();
        final Map<Long, String> customerNames = loadCustomerNames(entities);
        return entities.stream().map(e -> toProject(e, customerNames)).toList();
    }

    @Override
    public List<Project> findAll() {
        final List<ProjectEntity> entities = projectRepository.findAllByOrderByNameAsc();
        final Map<Long, String> customerNames = loadCustomerNames(entities);
        return entities.stream().map(e -> toProject(e, customerNames)).toList();
    }

    @Override
    public List<Project> findAllByCustomer(CustomerId customerId) {
        final String customerName = customerService.findById(customerId).map(Customer::name).orElse("");
        return projectRepository.findAllByCustomerIdOrderByNameAsc(customerId.value()).stream()
            .map(e -> toProject(e, customerName))
            .toList();
    }

    @Override
    public List<Project> findAllActiveByCustomer(CustomerId customerId) {
        final String customerName = customerService.findById(customerId).map(Customer::name).orElse("");
        return projectRepository.findAllByCustomerIdAndActiveTrueOrderByNameAsc(customerId.value()).stream()
            .map(e -> toProject(e, customerName))
            .toList();
    }

    @Override
    public Project create(CustomerId customerId, String name) {
        final ProjectEntity entity = new ProjectEntity();
        entity.setCustomerId(customerId.value());
        entity.setName(name);
        entity.setActive(true);
        final ProjectEntity saved = projectRepository.save(entity);
        final String customerName = customerService.findById(customerId).map(Customer::name).orElse("");
        return toProject(saved, customerName);
    }

    @Override
    public Project update(ProjectId id, String name, boolean active) {
        final ProjectEntity entity = projectRepository.findById(id.value())
            .orElseThrow(() -> new IllegalStateException("could not find project id=%s".formatted(id)));
        entity.setName(name);
        entity.setActive(active);
        final ProjectEntity saved = projectRepository.save(entity);
        final String customerName = customerService.findById(new CustomerId(saved.getCustomerId())).map(Customer::name).orElse("");
        return toProject(saved, customerName);
    }

    @Override
    public void delete(ProjectId id) {
        projectRepository.deleteById(id.value());
    }

    private Map<Long, String> loadCustomerNames(List<ProjectEntity> entities) {
        final List<Long> customerIds = entities.stream().map(ProjectEntity::getCustomerId).distinct().toList();
        return customerService.findAll().stream()
            .filter(c -> customerIds.contains(c.id().value()))
            .collect(Collectors.toMap(c -> c.id().value(), Customer::name));
    }

    private Project toProject(ProjectEntity entity, Map<Long, String> customerNames) {
        final String customerName = customerNames.getOrDefault(entity.getCustomerId(), "");
        return toProject(entity, customerName);
    }

    private Project toProject(ProjectEntity entity, String customerName) {
        return new Project(new ProjectId(entity.getId()), entity.getName(), entity.isActive(), new CustomerId(entity.getCustomerId()), customerName);
    }
}
