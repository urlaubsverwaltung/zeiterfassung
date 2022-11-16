package de.focusshift.zeiterfassung.registration.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

import static de.focusshift.zeiterfassung.tenant.TenantConfigurationProperties.MULTI;

@Controller
@PreAuthorize("hasRole('ZEITERFASSUNG_OPERATOR')")
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
public class TenantRegistrationController {

    private final TenantRegistrationService tenantRegistrationService;

    public TenantRegistrationController(TenantRegistrationService tenantRegistrationService) {
        this.tenantRegistrationService = tenantRegistrationService;
    }

    @GetMapping("/tenants/registrations")
    public String items(Model model) {
        List<String> tenantRegistrations = this.tenantRegistrationService.findAll();
        model.addAttribute("tenantRegistrations", tenantRegistrations);
        return "tenants/registrations/index.html";
    }

    @GetMapping("/tenants/registrations/form")
    public String form(Model model) {
        model.addAttribute("tenantRegistration", new TenantRegistrationDTO());
        return "tenants/registrations/form.html";
    }

    @PostMapping("/tenants/registrations")
    public String save(@ModelAttribute TenantRegistrationDTO tenantRegistration) {

        this.tenantRegistrationService.registerNewTenant(new TenantRegistration(tenantRegistration.getTenantId(), tenantRegistration.getOidcClientSecret()));

        return "tenants/registrations/successful.html";
    }
}
