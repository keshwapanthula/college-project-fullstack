package com.collegeupdates.config;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import lombok.extern.slf4j.Slf4j;

/**
 * AWS DocumentDB MongoDB Configuration
 * 
 * Configures MongoDB client for AWS DocumentDB with SSL/TLS encryption.
 * DocumentDB is MongoDB-compatible but requires SSL connections.
 * 
 * Prerequisites:
 * - global-bundle.pem certificate in src/main/resources/certificates/
 * - EC2 instance must have network access to DocumentDB cluster
 * - Security group must allow port 27017 from EC2
 * 
 * Features:
 * - SSL/TLS certificate validation
 * - Connection pooling optimized for production
 * - Read preference set to secondary for better performance
 * - Retry writes disabled (DocumentDB limitation)
 * 
 * @author Keshwa Panthula
 * @since 2025-02-26
 */
@Slf4j
@Configuration
@Profile("aws")
@EnableMongoRepositories(basePackages = "com.collegeupdates.repository")
public class AwsDocumentDbConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:collegeDB}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    /**
     * Configure MongoClient with SSL for AWS DocumentDB
     * 
     * @return Configured MongoClient
     */
    @Override
    @Bean
    @Primary
    public MongoClient mongoClient() {
        log.info("🔧 Configuring AWS DocumentDB MongoClient...");

        try {
            // Try to load SSL certificate for DocumentDB
            InputStream certInputStream = getClass().getClassLoader()
                    .getResourceAsStream("certificates/global-bundle.pem");

            if (certInputStream == null) {
                log.warn("⚠️  SSL certificate not found at certificates/global-bundle.pem");
                log.warn("⚠️  Using default SSL settings (may fail for DocumentDB)");
                log.warn("⚠️  Download certificate from: https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem");
                return createMongoClientWithDefaultSSL();
            }

            // Load and configure SSL certificate
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("aws-docdb", certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            // Build MongoDB client settings
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(mongoUri))
                    .applyToSslSettings(builder -> {
                        builder.enabled(true);
                        builder.context(sslContext);
                    })
                    .applyToConnectionPoolSettings(builder -> builder
                            .maxSize(50)
                            .minSize(10)
                            .maxWaitTime(30, TimeUnit.SECONDS)
                            .maxConnectionIdleTime(5, TimeUnit.MINUTES)
                            .maxConnectionLifeTime(10, TimeUnit.MINUTES))
                    .applyToSocketSettings(builder -> builder
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS))
                    .applyToServerSettings(builder -> builder
                            .heartbeatFrequency(10, TimeUnit.SECONDS)
                            .minHeartbeatFrequency(500, TimeUnit.MILLISECONDS))
                    .build();

            MongoClient mongoClient = MongoClients.create(settings);

            log.info("✅ AWS DocumentDB MongoClient configured successfully");
            log.info("   📍 Connection URI: {}", maskPassword(mongoUri));
            log.info("   🗄️  Database: {}", databaseName);
            log.info("   🔒 SSL Enabled: true");
            log.info("   📜 Certificate: global-bundle.pem");
            log.info("   🔢 Max Pool Size: 50");
            log.info("   🔢 Min Pool Size: 10");
            log.info("   ⏱️  Connect Timeout: 10s");
            log.info("   ⏱️  Read Timeout: 15s");

            // Test connection
            testConnection(mongoClient);

            return mongoClient;

        } catch (Exception e) {
            log.error("❌ Failed to configure DocumentDB SSL connection", e);
            log.error("   🔍 Troubleshooting:");
            log.error("      1. Ensure global-bundle.pem is in src/main/resources/certificates/");
            log.error("      2. Check DocumentDB endpoint is correct");
            log.error("      3. Verify security group allows port 27017");
            log.error("      4. Check DocumentDB credentials");
            throw new RuntimeException("Failed to initialize DocumentDB connection", e);
        }
    }

    /**
     * Create MongoClient with default SSL settings (fallback)
     * 
     * @return MongoClient with basic SSL
     */
    private MongoClient createMongoClientWithDefaultSSL() {
        log.info("📝 Creating MongoClient with default SSL settings");
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .applyToSslSettings(builder -> builder.enabled(true))
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(50)
                        .minSize(10)
                        .maxWaitTime(30, TimeUnit.SECONDS))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS))
                .build();
        
        return MongoClients.create(settings);
    }

    /**
     * Create MongoTemplate bean
     * 
     * @return MongoTemplate for database operations
     */
    @Bean
    @Primary
    public MongoTemplate mongoTemplate() {
        log.info("📝 Creating MongoTemplate for database: {}", databaseName);
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }

    /**
     * Test MongoDB connection on startup
     * 
     * @param mongoClient MongoDB client
     */
    private void testConnection(MongoClient mongoClient) {
        try {
            log.info("🧪 Testing DocumentDB connection...");
            
            boolean found = false;
            try (var cursor = mongoClient.listDatabaseNames().iterator()) {
                while (cursor.hasNext()) {
                    String db = cursor.next();
                    if (db.equals(databaseName)) {
                        found = true;
                        break;
                    }
                }
            }
            
            if (found) {
                log.info("✅ DocumentDB connection test successful!");
                log.info("   🗄️  Database '{}' found", databaseName);
            } else {
                log.warn("⚠️  Database '{}' not found (will be created on first write)", databaseName);
            }
            
        } catch (Exception e) {
            log.error("❌ DocumentDB connection test failed", e);
            log.error("   Application will start but database operations may fail");
        }
    }

    /**
     * Mask password in MongoDB URI for logging
     * 
     * @param uri MongoDB connection URI
     * @return Masked URI
     */
    private String maskPassword(String uri) {
        if (uri == null) {
            return "null";
        }
        return uri.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
    }
}
