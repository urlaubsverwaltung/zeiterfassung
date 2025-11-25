package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.invoke.MethodHandles.lookup;

class TenantAwareDataSource extends DelegatingDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    private static final String FALLBACK_TENANT_ID = "DEFAULT";

    private final TenantContextHolder tenantContextHolder;

    TenantAwareDataSource(DataSource targetDataSource, TenantContextHolder tenantContextHolder) {
        super(targetDataSource);
        this.tenantContextHolder = tenantContextHolder;
    }

    @NonNull
    @Override
    public Connection getConnection() throws SQLException {
        final Connection connection = getTargetDataSource().getConnection();
        setTenantId(connection);
        return getTenantAwareConnectionProxy(connection);
    }

    @NonNull
    @Override
    public Connection getConnection(@NonNull String username, @NonNull String password) throws SQLException {
        final Connection connection = getTargetDataSource().getConnection(username, password);
        setTenantId(connection);
        return getTenantAwareConnectionProxy(connection);
    }

    private void setTenantId(Connection connection) throws SQLException {
        try (final Statement sql = connection.createStatement()) {
            final String tenantId = resolveTenantId();
            if (LOG.isDebugEnabled()) {
                LOG.debug("setting parameter app.tenant_id={}", tenantId);
            }
            sql.execute("SET app.tenant_id TO '%s'".formatted(tenantId));
        }
    }

    private String resolveTenantId() {
        return tenantContextHolder.getCurrentTenantId().map(TenantId::tenantId)
            .orElseGet(() -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("using fallback tenantId={}", FALLBACK_TENANT_ID);
                }
                return FALLBACK_TENANT_ID;
            });
    }

    // Connection Proxy that intercepts close() to reset the tenant_id
    protected Connection getTenantAwareConnectionProxy(Connection connection) {
        return (Connection) Proxy.newProxyInstance(
            ConnectionProxy.class.getClassLoader(),
            new Class[]{ConnectionProxy.class},
            new TenantAwareInvocationHandler(connection));
    }

    private record TenantAwareInvocationHandler(Connection target) implements InvocationHandler {

        @Nullable
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "Tenant-aware proxy for target Connection [" + this.target.toString() + "]";
                case "unwrap":
                    if (((Class) args[0]).isInstance(proxy)) {
                        return proxy;
                    } else {
                        return method.invoke(target, args);
                    }
                case "isWrapperFor":
                    if (((Class) args[0]).isInstance(proxy)) {
                        return true;
                    } else {
                        return method.invoke(target, args);
                    }
                case "getTargetConnection":
                    return target;
                default:
                    if (method.getName().equals("close")) {
                        // this is the main purpose of this invocation handler!
                        clearTenantId(target);
                    }
                    return method.invoke(target, args);
            }
        }

        private void clearTenantId(Connection connection) throws SQLException {
            try (final PreparedStatement sql = connection.prepareStatement("RESET app.tenant_id")) {
                sql.execute();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("parameter app.tenant_id has been reset");
            }
        }
    }
}
