package com.softart.vetclinic.config.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * DataSource wrapper for PostgreSQL Row-Level Security (RLS).
 * On each getConnection(): sets app.current_clinic_id from ClinicContextHolder.
 * On connection close(): resets the variable before returning to pool.
 * Uses Java Dynamic Proxy to avoid implementing all Connection methods.
 */
@Slf4j
@RequiredArgsConstructor
public class TenantAwareDataSource implements DataSource {

    private final DataSource delegate;

    @Override
    public Connection getConnection() throws SQLException {
        return wrapConnection(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return wrapConnection(delegate.getConnection(username, password));
    }

    private Connection wrapConnection(Connection raw) throws SQLException {
        UUID clinicId = ClinicContextHolder.get();
        final boolean rlsWasSet = (clinicId != null);
        if (rlsWasSet) {
            try (Statement stmt = raw.createStatement()) {
                stmt.execute("SET app.current_clinic_id = '" + clinicId + "'");
            }
            log.trace("RLS SET clinic_id={}", clinicId);
        }

        // Dynamic proxy: intercepts close() to RESET, delegates everything else
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        if (rlsWasSet) {
                            try {
                                resetTenantContext(raw);
                            } catch (Throwable t) {
                                // Nikad ne blokirati delegirani close — zombi konekcija je gora od stale RLS var-a
                                log.warn("Unexpected error during RLS reset; proceeding with close", t);
                            }
                        }
                        return method.invoke(raw, args);
                    }
                    return method.invoke(raw, args);
                });
    }

    private void resetTenantContext(Connection connection) {
        try {
            if (connection.isClosed()) {
                log.trace("Connection already closed, skipping RLS RESET");
                return;
            }
        } catch (SQLException e) {
            // Ako ne možemo ni da proverimo stanje, konekcija je problematična — preskoči RESET
            log.trace("Failed to check connection.isClosed(), skipping RLS RESET: {}", e.getMessage());
            return;
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("RESET app.current_clinic_id");
            log.trace("RLS RESET clinic_id");
        } catch (SQLException e) {
            log.warn("Failed to reset app.current_clinic_id: {}", e.getMessage());
        }
    }

    // --- DataSource delegate methods ---

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger(getClass().getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }
}
