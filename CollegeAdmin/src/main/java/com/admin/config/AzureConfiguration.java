package com.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.monitor.query.LogsQueryClient;
import com.azure.monitor.query.LogsQueryClientBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

/**
 * Comprehensive Azure SDK Configuration for College Admin Service
 * Configures all Azure clients for complete cloud integration
 */
@Profile({"azure", "default"})
@Configuration
public class AzureConfiguration {

    @Value("${azure.storage.account-name:collegeadminstorage}")
    private String storageAccountName;

    @Value("${azure.servicebus.namespace:college-admin-sb}")
    private String serviceBusNamespace;

    @Value("${azure.keyvault.vault-url:https://college-admin-kv.vault.azure.net/}")
    private String keyVaultUrl;

    @Value("${azure.cosmosdb.endpoint:https://college-admin-cosmos.documents.azure.com:443/}")
    private String cosmosDbEndpoint;

    @Value("${azure.cosmosdb.key:}")
    private String cosmosDbKey;

    @Value("${azure.search.endpoint:https://college-admin-search.search.windows.net}")
    private String searchEndpoint;

    @Value("${azure.search.key:}")
    private String searchKey;

    @Value("${azure.search.index-name:students-index}")
    private String searchIndexName;

    @Value("${azure.cognitive.endpoint:https://college-admin-cognitive.cognitiveservices.azure.com/}")
    private String cognitiveEndpoint;

    @Value("${azure.cognitive.key:}")
    private String cognitiveKey;

    @Value("${azure.openai.endpoint:https://college-admin-openai.openai.azure.com/}")
    private String openAiEndpoint;

    @Value("${azure.openai.key:}")
    private String openAiKey;

    @Value("${azure.eventgrid.endpoint:https://college-admin-eventgrid.eastus-1.eventgrid.azure.net/api/events}")
    private String eventGridEndpoint;

    @Value("${azure.eventgrid.key:}")
    private String eventGridKey;

    @Value("${azure.subscription-id:}")
    private String subscriptionId;

    @Value("${azure.resource-group:college-admin-rg}")
    private String resourceGroupName;

    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
                .endpoint("https://" + storageAccountName + ".blob.core.windows.net")
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    @Bean
    public ServiceBusSenderClient serviceBusSenderClient() {
        return new ServiceBusClientBuilder()
                .fullyQualifiedNamespace(serviceBusNamespace + ".servicebus.windows.net")
                .credential(new DefaultAzureCredentialBuilder().build())
                .sender()
                .queueName("college-admin-queue")
                .buildClient();
    }

    @Bean
    public SecretClient keyVaultSecretClient() {
        return new SecretClientBuilder()
                .vaultUrl(keyVaultUrl)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    @Bean
    public CosmosClient cosmosClient() {
        if (cosmosDbKey != null && !cosmosDbKey.isEmpty()) {
            return new CosmosClientBuilder()
                    .endpoint(cosmosDbEndpoint)
                    .key(cosmosDbKey)
                    .buildClient();
        } else {
            return new CosmosClientBuilder()
                    .endpoint(cosmosDbEndpoint)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
        }
    }

    @Bean
    public SearchClient searchClient() {
        return new SearchClientBuilder()
                .endpoint(searchEndpoint)
                .indexName(searchIndexName)
                .credential(new DefaultAzureCredentialBuilder().build()) // Use managed identity if no key
                .buildClient();
    }

    @Bean
    public TextAnalyticsClient textAnalyticsClient() {
        return new TextAnalyticsClientBuilder()
                .endpoint(cognitiveEndpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    @Bean
    public OpenAIClient openAIClient() {
        return new OpenAIClientBuilder()
                .endpoint(openAiEndpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    @Bean
    public EventGridPublisherClient<EventGridEvent> eventGridPublisherClient() {
        return new EventGridPublisherClientBuilder()
                .endpoint(eventGridEndpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildEventGridEventPublisherClient();
    }

    @Bean
    public LogsQueryClient logsQueryClient() {
        return new LogsQueryClientBuilder()
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    @Bean
    public AzureResourceManager azureResourceManager() {
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        return AzureResourceManager.authenticate(
                new DefaultAzureCredentialBuilder().build(),
                profile
        ).withDefaultSubscription();
    }
}