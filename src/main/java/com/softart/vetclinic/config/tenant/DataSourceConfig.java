package com.softart.vetclinic.config.tenant;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Wraps the default HikariCP DataSource with TenantAwareDataSource
 * to enable automatic RLS session variable management.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource tenantAwareDataSource(DataSourceProperties properties) {
        DataSource hikariDataSource = properties.initializeDataSourceBuilder().build();
        return new TenantAwareDataSource(hikariDataSource);
    }
}
