package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import de.focusshift.zeiterfassung.tenancy.configuration.multi.AdminAware;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Entity(name = "oidc_client")
public class OidcClientEntity implements AdminAware<Long> {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "oidc_client_seq", sequenceName = "oidc_client_seq")
    @GeneratedValue(strategy = SEQUENCE, generator = "oidc_client_seq")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    public OidcClientEntity(Long id, String tenantId, String clientSecret) {
        this.id = id;
        this.tenantId = tenantId;
        this.clientSecret = clientSecret;
    }

    public OidcClientEntity() {
        // for hibernate
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
