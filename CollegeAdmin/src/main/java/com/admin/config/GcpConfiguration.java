package com.admin.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.aiplatform.v1.EndpointServiceClient;
import com.google.cloud.aiplatform.v1.EndpointServiceSettings;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.cloud.discoveryengine.v1.SearchServiceClient;
import com.google.cloud.discoveryengine.v1.SearchServiceSettings;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.functions.v1.CloudFunctionsServiceClient;
import com.google.cloud.functions.v1.CloudFunctionsServiceSettings;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.LanguageServiceSettings;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import com.google.cloud.resourcemanager.v3.ProjectsSettings;
import com.google.cloud.run.v2.ServicesClient;
import com.google.cloud.run.v2.ServicesSettings;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretManagerServiceSettings;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.trace.v1.TraceServiceClient;
import com.google.cloud.trace.v1.TraceServiceSettings;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;
import com.google.cloud.workflows.v1.WorkflowsClient;
import com.google.cloud.workflows.v1.WorkflowsSettings;

import lombok.extern.slf4j.Slf4j;

/**
 * Google Cloud Platform (GCP) Configuration
 * Comprehensive configuration for all GCP services integration
 * Provides beans for all 10+ GCP service clients
 */
@Configuration
@Slf4j
public class GcpConfiguration {

    @Value("${gcp.project-id:college-admin-gcp-project}")
    private String projectId;

    @Value("${gcp.credentials.location:#{null}}")
    private String credentialsLocation;

    @Value("${gcp.region:us-central1}")
    private String region;

    @Value("${gcp.zone:us-central1-a}")
    private String zone;

    /**
     * Google Credentials Provider
     * Uses Application Default Credentials or Service Account Key
     */
    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        try {
            if (credentialsLocation != null && !credentialsLocation.isEmpty()) {
                log.info("Loading GCP credentials from: {}", credentialsLocation);
                return ServiceAccountCredentials.fromStream(new FileInputStream(credentialsLocation));
            } else {
                log.info("Using GCP Application Default Credentials");
                return GoogleCredentials.getApplicationDefault();
            }
        } catch (Exception e) {
            log.warn("Failed to load GCP credentials, using default: {}", e.getMessage());
            return GoogleCredentials.getApplicationDefault();
        }
    }

    /**
     * Google Cloud Storage Client
     * For file storage, uploads, downloads, and blob management
     */
    @Bean
    public Storage storageClient(GoogleCredentials credentials) {
        log.info("Creating GCP Storage client for project: {}", projectId);
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }

    /**
     * Google Cloud Firestore Client
     * For NoSQL document database operations
     */
    @Bean
    public Firestore firestoreClient(GoogleCredentials credentials) {
        log.info("Creating GCP Firestore client for project: {}", projectId);
        return FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }

    /**
     * Google Cloud Pub/Sub Topic Admin Client
     * For managing Pub/Sub topics
     */
    @Bean
    public TopicAdminClient topicAdminClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Pub/Sub Topic Admin client");
        TopicAdminSettings settings = TopicAdminSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return TopicAdminClient.create(settings);
    }

    /**
     * Google Cloud Pub/Sub Subscription Admin Client
     * For managing Pub/Sub subscriptions
     */
    @Bean
    public SubscriptionAdminClient subscriptionAdminClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Pub/Sub Subscription Admin client");
        SubscriptionAdminSettings subSettings = SubscriptionAdminSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return SubscriptionAdminClient.create(subSettings);
    }

    /**
     * Google Cloud Language Service Client
     * For natural language processing and AI
     */
    @Bean
    public LanguageServiceClient languageServiceClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Language Service client");
        LanguageServiceSettings langSettings = LanguageServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return LanguageServiceClient.create(langSettings);
    }

    /**
     * Google Cloud Translation Service Client
     * For text translation services
     */
    @Bean
    public TranslationServiceClient translationServiceClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Translation Service client");
        TranslationServiceSettings transSettings = TranslationServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return TranslationServiceClient.create(transSettings);
    }

    /**
     * Google Cloud AI Platform Prediction Service Client
     * For machine learning model predictions
     */
    @Bean
    public PredictionServiceClient predictionServiceClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP AI Platform Prediction Service client");
        PredictionServiceSettings predSettings = PredictionServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return PredictionServiceClient.create(predSettings);
    }

    /**
     * Google Cloud AI Platform Endpoint Service Client
     * For managing ML model endpoints
     */
    @Bean
    public EndpointServiceClient endpointServiceClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP AI Platform Endpoint Service client");
        EndpointServiceSettings epSettings = EndpointServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return EndpointServiceClient.create(epSettings);
    }

    /**
     * Google Cloud Discovery Engine Search Service Client
     * For search and document indexing
     */
    @Bean
    public SearchServiceClient searchServiceClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Discovery Engine Search Service client");
        SearchServiceSettings searchSettings = SearchServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return SearchServiceClient.create(searchSettings);
    }

    /**
     * Google Cloud Monitoring Metric Service Client
     * For metrics, monitoring, and observability
     */
    @Bean
    public MetricServiceClient metricServiceClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Monitoring Metric Service client");
        MetricServiceSettings metricSettings = MetricServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return MetricServiceClient.create(metricSettings);
    }

    /**
     * Google Cloud Logging Client
     * For centralized logging and log analysis
     */
    @Bean
    public Logging loggingClient(GoogleCredentials credentials) {
        log.info("Creating GCP Logging client");
        return LoggingOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }

    /**
     * Google Cloud Trace Service Client
     * For distributed tracing and performance monitoring
     */
    @Bean
    public TraceServiceClient traceServiceClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Trace Service client");
        TraceServiceSettings traceSettings = TraceServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return TraceServiceClient.create(traceSettings);
    }

    /**
     * Google Cloud Functions Service Client
     * For serverless function management
     */
    @Bean
    public CloudFunctionsServiceClient cloudFunctionsServiceClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Cloud Functions Service client");
        CloudFunctionsServiceSettings fnSettings = CloudFunctionsServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return CloudFunctionsServiceClient.create(fnSettings);
    }

    /**
     * Google Cloud Run Services Client
     * For containerized application deployment and management
     */
    @Bean
    public ServicesClient servicesClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Cloud Run Services client");
        ServicesSettings runSettings = ServicesSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return ServicesClient.create(runSettings);
    }

    /**
     * Google Cloud Workflows Client
     * For workflow orchestration and automation
     */
    @Bean
    public WorkflowsClient workflowsClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Workflows client");
        WorkflowsSettings wfSettings = WorkflowsSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return WorkflowsClient.create(wfSettings);
    }

    /**
     * Google Cloud Resource Manager Projects Client
     * For project and resource management
     */
    @Bean
    public ProjectsClient projectsClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Resource Manager Projects client");
        ProjectsSettings projSettings = ProjectsSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return ProjectsClient.create(projSettings);
    }

    /**
     * Google Cloud Secret Manager Service Client
     * For secrets and credentials management
     */
    @Bean
    public SecretManagerServiceClient secretManagerServiceClient(GoogleCredentials credentials) throws IOException {
        log.info("Creating GCP Secret Manager Service client");
        SecretManagerServiceSettings smSettings = SecretManagerServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        return SecretManagerServiceClient.create(smSettings);
    }

    /**
     * Configuration properties for various GCP services
     */
    @Bean
    public GcpProperties gcpProperties() {
        return GcpProperties.builder()
                .projectId(projectId)
                .region(region)
                .zone(zone)
                .credentialsLocation(credentialsLocation)
                .build();
    }

    /**
     * GCP Properties holder class for configuration values
     */
    public static class GcpProperties {
        private String projectId;
        private String region;
        private String zone;
        private String credentialsLocation;

        public static GcpPropertiesBuilder builder() {
            return new GcpPropertiesBuilder();
        }

        public static class GcpPropertiesBuilder {
            private String projectId;
            private String region;
            private String zone;
            private String credentialsLocation;

            public GcpPropertiesBuilder projectId(String projectId) {
                this.projectId = projectId;
                return this;
            }

            public GcpPropertiesBuilder region(String region) {
                this.region = region;
                return this;
            }

            public GcpPropertiesBuilder zone(String zone) {
                this.zone = zone;
                return this;
            }

            public GcpPropertiesBuilder credentialsLocation(String credentialsLocation) {
                this.credentialsLocation = credentialsLocation;
                return this;
            }

            public GcpProperties build() {
                GcpProperties properties = new GcpProperties();
                properties.projectId = this.projectId;
                properties.region = this.region;
                properties.zone = this.zone;
                properties.credentialsLocation = this.credentialsLocation;
                return properties;
            }
        }

        // Getters
        public String getProjectId() { return projectId; }
        public String getRegion() { return region; }
        public String getZone() { return zone; }
        public String getCredentialsLocation() { return credentialsLocation; }
    }
}