package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.String.format;

class TenantAwareDataSource extends DelegatingDataSource {

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
            final String tenantId = tenantContextHolder.getCurrentTenantId().map(TenantId::tenantId).orElse("");
            sql.execute(format("SET app.tenant_id TO '%s'", tenantId));
        }
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
        }
    }
}
