package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAwareDataSourceTest {

    private TenantAwareDataSource sut;

    @Mock
    private DataSource targetDataSource;
    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private Connection targetConnection;
    @Mock
    private PreparedStatement setTenantIdStatement;
    @Mock
    private PreparedStatement resetTenantIdStatement;

    @BeforeEach
    void setUp() {
        sut = new TenantAwareDataSource(targetDataSource, tenantContextHolder);
    }

    @Test
    void ensureTenantIdIsSetOnTheConnection() throws SQLException {
        givenConnection(Optional.of(new TenantId("a154bc4e")));

        sut.getConnection();

        verify(setTenantIdStatement).setString(1, "a154bc4e");
        verify(setTenantIdStatement).execute();
    }

    @Test
    void ensureTenantIdIsPassedAsBindParameterAndNotConcatenatedIntoTheStatement() throws SQLException {
        givenConnection(Optional.of(new TenantId("x'; DROP TABLE time_entry; --")));

        sut.getConnection();

        verify(targetConnection).prepareStatement("SELECT set_config('app.tenant_id', ?, false)");
        verify(setTenantIdStatement).setString(1, "x'; DROP TABLE time_entry; --");
    }

    @Test
    void ensureFallbackTenantIdWithoutTenantContext() throws SQLException {
        givenConnection(Optional.empty());

        sut.getConnection();

        verify(setTenantIdStatement).setString(1, "DEFAULT");
        verify(setTenantIdStatement).execute();
    }

    @Test
    void ensureCloseResetsTheTenantIdAndClosesTheTargetConnection() throws SQLException {
        givenConnection(Optional.of(new TenantId("a154bc4e")));
        when(targetConnection.prepareStatement("RESET app.tenant_id")).thenReturn(resetTenantIdStatement);

        sut.getConnection().close();

        verify(resetTenantIdStatement).execute();
        verify(targetConnection).close();
    }

    private void givenConnection(Optional<TenantId> tenantId) throws SQLException {
        when(targetDataSource.getConnection()).thenReturn(targetConnection);
        when(targetConnection.prepareStatement("SELECT set_config('app.tenant_id', ?, false)")).thenReturn(setTenantIdStatement);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(tenantId);
    }
}
