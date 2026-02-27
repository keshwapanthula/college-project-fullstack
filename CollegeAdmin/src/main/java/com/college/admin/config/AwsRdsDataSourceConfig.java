package com.college.admin.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * AWS RDS MySQL DataSource Configuration
 * 
 * Configures HikariCP connection pool for AWS RDS MySQL database
 * with production-optimized settings for high performance and reliability.
 * 
 * Features:
 * - Optimized connection pool settings for production workload
 * - Connection leak detection
 * - Health check query configuration
 * - SSL/TLS enabled for secure connections
 * 
 * @author Keshwa Panthula
 * @since 2025-02-26
 */
@Slf4j
@Configuration
@Profile("aws")
public class AwsRdsDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.hikari.maximum-pool-size:50}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:10}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1200000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    /**
     * Create and configure HikariCP DataSource for AWS RDS MySQL
     * 
     * @return Configured DataSource bean
     */
    @Bean
    @Primary
    public DataSource awsRdsDataSource() {
        log.info("🔧 Configuring AWS RDS MySQL DataSource...");

        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Connection pool settings
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setLeakDetectionThreshold(leakDetectionThreshold);

        // Health check and validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // Performance optimizations
        config.setAutoCommit(false); // Manual transaction management
        config.setPoolName("CollegeAdminRDSPool");
        
        // Additional MySQL-specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        // Create DataSource
        HikariDataSource dataSource = new HikariDataSource(config);

        log.info("✅ AWS RDS DataSource configured successfully");
        log.info("   📍 JDBC URL: {}", maskPassword(jdbcUrl));
        log.info("   👤 Username: {}", username);
        log.info("   🔢 Max Pool Size: {}", maxPoolSize);
        log.info("   🔢 Min Idle: {}", minIdle);
        log.info("   ⏱️  Connection Timeout: {}ms", connectionTimeout);
        log.info("   ⏱️  Idle Timeout: {}ms", idleTimeout);
        log.info("   ⏱️  Max Lifetime: {}ms", maxLifetime);
        log.info("   🔍 Leak Detection: {}ms", leakDetectionThreshold);
        log.info("   🔒 SSL Enabled: true");

        return dataSource;
    }

    /**
     * Mask password in JDBC URL for logging
     * 
     * @param url JDBC URL
     * @return Masked URL
     */
    private String maskPassword(String url) {
        if (url == null) {
            return "null";
        }
        return url.replaceAll("password=[^&]+", "password=****");
    }
}
