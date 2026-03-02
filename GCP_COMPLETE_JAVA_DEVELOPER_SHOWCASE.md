# GCP Complete Java Developer Showcase

## Enterprise Spring Boot Microservices on Google Cloud Platform

**Live Production Deployment** | GCP Project: `storied-shore-473817-v5` | Region: `us-central1`

---

## 🚀 Live Cloud Run Services

| Service | URL | Port |
|---------|-----|------|
| **College Admin** | [https://college-admin-324266638161.us-central1.run.app](https://college-admin-324266638161.us-central1.run.app) | 8082 |
| **College Notifications** | [https://college-notifications-backend-324266638161.us-central1.run.app](https://college-notifications-backend-324266638161.us-central1.run.app) | 8083 |
| **College Updates** | [https://college-updates-backend-324266638161.us-central1.run.app](https://college-updates-backend-324266638161.us-central1.run.app) | 8080 |
| **Notifications Frontend** | [https://college-notifications-frontend-324266638161.us-central1.run.app](https://college-notifications-frontend-324266638161.us-central1.run.app) | 8080 |
| **Portal Frontend** | [https://college-portal-frontend-324266638161.us-central1.run.app](https://college-portal-frontend-324266638161.us-central1.run.app) | 8080 |
| **Updates Frontend** | [https://college-updates-frontend-324266638161.us-central1.run.app](https://college-updates-frontend-324266638161.us-central1.run.app) | 8080 |

---

## 🏗️ GCP Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     GOOGLE CLOUD PLATFORM                        │
│                   Project: storied-shore-473817-v5               │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Cloud Load Balancing (HTTPS)                 │   │
│  └───────────────────────┬──────────────────────────────────┘   │
│                           │                                       │
│  ┌────────────────────────▼─────────────────────────────────┐   │
│  │                  Cloud Run Services                        │   │
│  │  ┌─────────────┐ ┌──────────────┐ ┌──────────────────┐  │   │
│  │  │college-admin│ │notifications │ │college-updates   │  │   │
│  │  │   :8082     │ │   :8083      │ │     :8080        │  │   │
│  │  └──────┬──────┘ └──────┬───────┘ └────────┬─────────┘  │   │
│  └─────────┼───────────────┼──────────────────┼────────────┘   │
│            │               │                  │                   │
│  ┌─────────▼───────────────▼──────────────────▼────────────┐   │
│  │                    GCP Managed Services                   │   │
│  │                                                           │   │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │   │
│  │  │  Cloud SQL  │  │ Memorystore  │  │ Managed Kafka │  │   │
│  │  │   (MySQL)   │  │   (Redis)    │  │  (Pub/Sub)    │  │   │
│  │  └─────────────┘  └──────────────┘  └───────────────┘  │   │
│  │                                                           │   │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │   │
│  │  │  Firestore  │  │   Artifact   │  │    Secret     │  │   │
│  │  │  (NoSQL)    │  │   Registry   │  │    Manager    │  │   │
│  │  └─────────────┘  └──────────────┘  └───────────────┘  │   │
│  │                                                           │   │
│  │  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │   │
│  │  │ Vertex AI   │  │Cloud Logging │  │  Cloud Mon.   │  │   │
│  │  │  Platform   │  │ & Monitoring │  │  (Dashboards) │  │   │
│  │  └─────────────┘  └──────────────┘  └───────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## ☁️ GCP Service Integration Showcase

### 1. Cloud Run — Serverless Container Hosting
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/`](CollegeAdmin/src/main/java/com/admin/service/gcp/)

- **Zero-infrastructure management** — Google handles scaling, patching, load balancing
- Containers scale from 0 to N instances automatically based on traffic
- Built-in HTTPS, GRPC support, concurrency management
- Deployed via: `gcloud run deploy college-admin --image ... --allow-unauthenticated`
- **Equivalent to**: AWS ECS Fargate / AWS Lambda (longer-running workloads)

```bash
# Live endpoint test
curl https://college-admin-324266638161.us-central1.run.app/api/admin/health
```

---

### 2. Artifact Registry — Container Image Storage
**File**: [`deploy-gcp.bat`](deploy-gcp.bat)

- Stores all 6 Docker images: `us-central1-docker.pkg.dev/storied-shore-473817-v5/college-services`
- Integrated vulnerability scanning with Container Analysis
- Fine-grained IAM permissions per repository
- VPC Service Controls support for air-gapped environments
- **Equivalent to**: AWS ECR (Elastic Container Registry)

```bash
# Docker authentication
gcloud auth configure-docker us-central1-docker.pkg.dev

# Push image
docker push us-central1-docker.pkg.dev/storied-shore-473817-v5/college-services/college-admin:latest
```

---

### 3. Cloud SQL — Managed Relational Database
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpMonitoringService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpMonitoringService.java)
**Config**: [`CollegeAdmin/src/main/resources/application-gcp.properties`](CollegeAdmin/src/main/resources/application-gcp.properties)

- MySQL 8.0 fully managed — automated backups, HA, read replicas
- Cloud SQL Auth Proxy for secure, encrypted connections (no IP whitelisting)
- IAM database authentication using Workload Identity
- Point-in-time recovery (PITR) with 7-day retention
- **Equivalent to**: AWS RDS MySQL

```properties
# Cloud SQL via Unix socket (zero-trust connection)
spring.datasource.url=jdbc:mysql:///college_system?cloudSqlInstance=\
  storied-shore-473817-v5:us-central1:college-mysql-db\
  &socketFactory=com.google.cloud.sql.mysql.SocketFactory
```

---

### 4. Memorystore for Redis — Managed Cache
**Config**: [`CollegeAdmin/src/main/resources/application-gcp.properties`](CollegeAdmin/src/main/resources/application-gcp.properties)

- Sub-millisecond latency Redis 6.x / 7.x instances
- Automatic failover, in-transit encryption, private IP connectivity
- VPC-native network isolation
- No maintenance windows — zero-downtime patches
- **Equivalent to**: AWS ElastiCache for Redis

```properties
spring.redis.host=${GCP_MEMORYSTORE_HOST:127.0.0.1}
spring.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
```

---

### 5. GCP Managed Kafka / Pub/Sub — Event Streaming
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpPubSubService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpPubSubService.java)

- GCP Managed Kafka: Apache Kafka API-compatible, zero cluster management
- Pub/Sub: globally distributed, serverless, at-least-once delivery
- SASL/OAuth authentication via Workload Identity (no credentials in code)
- Used for: enrollment events, grade notifications, course updates, graduation alerts
- **Equivalent to**: AWS MSK (Kafka) / AWS SNS+SQS

```properties
spring.kafka.bootstrap-servers=${GCP_MANAGED_KAFKA_BROKERS:localhost:9092}
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=OAUTHBEARER
```

---

### 6. Cloud Firestore — NoSQL Document Database
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpFirestoreService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpFirestoreService.java)

- Serverless, fully managed NoSQL with real-time sync capabilities
- ACID transactions across documents and collections
- Automatic multi-region replication
- Used for: ephemeral course data, real-time student dashboards, unstructured college updates
- **Equivalent to**: AWS DynamoDB / AWS DocumentDB

```java
// Firestore Java SDK example (from GcpFirestoreService.java)
Firestore db = FirestoreOptions.getDefaultInstance().getService();
ApiFuture<QuerySnapshot> query = db.collection("students").get();
```

---

### 7. Cloud Storage — Object Storage (GCS)
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpCloudStorageService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpCloudStorageService.java)

- Petabyte-scale object storage with 99.999999999% (11 9s) durability
- Storage classes: Standard, Nearline, Coldline, Archive
- Signed URLs for temporary secure access
- Object Lifecycle Management — automatic tiering/deletion
- Used for: student transcripts, course materials, graduation ceremony media
- **Equivalent to**: AWS S3 with lifecycle policies

```java
// Upload to GCS (from GcpCloudStorageService.java)
Storage storage = StorageOptions.getDefaultInstance().getService();
BlobId blobId = BlobId.of("college-media-bucket", "transcript-" + studentId + ".pdf");
storage.create(BlobInfo.newBuilder(blobId).build(), fileBytes);
```

---

### 8. Vertex AI Platform — Machine Learning
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpAiPlatformService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpAiPlatformService.java)

- End-to-end ML platform: training, hyperparameter tuning, serving, explainability
- Pre-built models: AutoML, Gemini, PaLM 2 via Model Garden
- Managed Feature Store for ML feature sharing across teams
- Pipeline orchestration with Vertex AI Pipelines (Kubeflow compatible)
- **Equivalent to**: AWS SageMaker

```java
// Vertex AI prediction (from GcpAiPlatformService.java)
PredictionServiceClient client = PredictionServiceClient.create();
EndpointName endpointName = EndpointName.of(projectId, location, endpointId);
PredictResponse response = client.predict(endpointName, instances, parameters);
```

---

### 9. Cloud Functions — Serverless Compute
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpCloudFunctionsService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpCloudFunctionsService.java)

- Event-driven functions triggered by Pub/Sub, HTTP, Cloud Storage, Firestore
- Supports Java 17/21, Node.js, Python, Go, Ruby, PHP, .NET
- Cold start optimization via minimum instance configuration
- Gen 2 functions run on Cloud Run (longer timeout, larger memory)
- **Equivalent to**: AWS Lambda

```java
// Cloud Function trigger (from GcpCloudFunctionsService.java)
@CloudEventsFunction("processEnrollmentEvent")
public void accept(CloudEvent event) {
    String data = new String(event.getData().toBytes());
    // Process enrollment notification
}
```

---

### 10. Secret Manager — Secrets Management
**Config**: All `application-gcp.properties` files

- Centralized storage for API keys, database passwords, certificates
- Version management — rotate secrets without touching code
- IAM-based access control with audit logging
- Automatic secret expiration and scheduled rotation
- **Equivalent to**: AWS Secrets Manager / AWS Parameter Store

```bash
# Store a secret
echo -n "my-db-password" | gcloud secrets create college-admin-db-password \
  --replication-policy="automatic" --data-file=-

# Access in Java via Spring Cloud GCP
@Value("${sm://college-admin-db-password}")
private String dbPassword;
```

---

### 11. Cloud Monitoring & Cloud Logging
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpMonitoringService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpMonitoringService.java)

- Cloud Monitoring: Custom dashboards, alerting policies, uptime checks
- Cloud Logging: Structured logging with full-text search, log-based metrics
- Cloud Trace: Distributed tracing across microservices
- Error Reporting: Automatic error grouping and notifications
- Micrometer integration exports Spring Boot metrics to Stackdriver
- **Equivalent to**: AWS CloudWatch + AWS X-Ray

```properties
management.metrics.export.stackdriver.enabled=true
management.metrics.export.stackdriver.project-id=storied-shore-473817-v5
management.metrics.export.stackdriver.step=1m
```

---

### 12. Identity Platform / IAP — Authentication
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpIdentityService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpIdentityService.java)

- Google Identity Platform: OAuth 2.0, OIDC, SAML federation
- Identity-Aware Proxy (IAP): Zero-trust access control for Cloud Run
- Workload Identity Federation: Keyless auth for service-to-service calls
- Firebase Authentication integration for mobile/web frontends
- **Equivalent to**: AWS Cognito + AWS IAM Identity Center

---

### 13. API Gateway — Traffic Management
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpApiGatewayService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpApiGatewayService.java)

- Cloud Endpoints + Apigee for API management
- Rate limiting, quotas, API key management, JWT validation
- OpenAPI spec integration for auto-generated documentation
- gRPC + HTTP/REST transcoding
- **Equivalent to**: AWS API Gateway

---

### 14. Cloud Workflows — Orchestration
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpWorkflowsService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpWorkflowsService.java)

- Serverless workflow orchestration with conditional logic, parallel steps
- Built-in HTTP calls, error handling, retries
- Used for: multi-step student enrollment, graduation processing pipeline
- Integrates with Cloud Run, Cloud Functions, Pub/Sub
- **Equivalent to**: AWS Step Functions

---

### 15. Cloud Search / Vertex AI Search
**File**: [`CollegeAdmin/src/main/java/com/admin/service/gcp/GcpSearchService.java`](CollegeAdmin/src/main/java/com/admin/service/gcp/GcpSearchService.java)

- Enterprise search with AI-powered relevance ranking
- Semantic search using embedding models
- Integrates with BigQuery, Datastore, websites, and custom data sources
- **Equivalent to**: AWS OpenSearch / AWS Kendra

---

## 🔧 GCP vs AWS Service Mapping

| AWS Service | GCP Equivalent | Used In |
|------------|----------------|---------|
| ECS Fargate / ECR | Cloud Run + Artifact Registry | All services |
| RDS MySQL | Cloud SQL (MySQL) | CollegeAdmin |
| ElastiCache Redis | Memorystore for Redis | All backends |
| MSK (Kafka) | GCP Managed Kafka / Pub/Sub | All backends |
| DocumentDB | MongoDB Atlas / Cloud Firestore | CollegeUpdates |
| S3 | Cloud Storage (GCS) | Media, transcripts |
| Secrets Manager | Secret Manager | All services |
| CloudWatch | Cloud Monitoring + Cloud Logging | All services |
| X-Ray | Cloud Trace | All services |
| SageMaker | Vertex AI Platform | CollegeAdmin AI |
| Lambda | Cloud Functions | Event processing |
| Cognito | Identity Platform / IAP | Auth layer |
| API Gateway | Cloud Endpoints / Apigee | API management |
| Step Functions | Cloud Workflows | Enrollment pipelines |
| OpenSearch | Vertex AI Search / Cloud Search | Course search |
| CloudFormation | Deployment Manager / Terraform | IaC |

---

## 📦 Deployment Pipeline

```bash
# 1. Build all images
docker-compose build

# 2. Authenticate with GCP
gcloud auth login
gcloud config set project storied-shore-473817-v5
gcloud auth configure-docker us-central1-docker.pkg.dev

# 3. Push images to Artifact Registry
.\push-gcp.ps1

# 4. Deploy to Cloud Run (full deployment)
.\deploy-gcp.bat
```

---

## 🛡️ Security & Compliance

| Feature | Implementation |
|---------|---------------|
| **Data in transit** | TLS 1.3 for all Cloud Run HTTPS endpoints |
| **Data at rest** | AES-256 encryption (default on Cloud SQL, GCS, Firestore) |
| **Service auth** | Workload Identity (no service account keys) |
| **Secret storage** | GCP Secret Manager (versioned, audited) |
| **Network isolation** | VPC-native Cloud Run with private services |
| **Access control** | IAM roles with least-privilege principle |
| **Audit** | Cloud Audit Logs for all API calls |

---

## 📊 Performance & Scalability

| Component | Configuration |
|-----------|--------------|
| **Cloud Run concurrency** | 80 requests/instance (default) |
| **Cloud Run scaling** | 0 to 100 instances (configurable) |
| **HikariCP pool** | Min: 10, Max: 50 connections |
| **Redis cache TTL** | 600s (Admin), 300s (Updates), 3600s (Notifications) |
| **Kafka batch** | 16KB batches, Snappy compression |
| **Cloud SQL tier** | db-n1-standard-2 (2 vCPU, 7.5 GB RAM) |

---

## 📋 Interview Talking Points

1. **"How do you handle service discovery without Eureka on Cloud Run?"**
   — Cloud Run services have stable HTTPS URLs; no client-side discovery needed. Pass URLs via environment variables in Cloud Run service configuration.

2. **"How is GCP authentication different from AWS?"**
   — GCP uses Workload Identity Federation — Cloud Run services get a Google-managed identity automatically. No access keys to rotate. AWS requires IAM roles attached to task definitions.

3. **"How does Cloud SQL compare to RDS?"**
   — Both are managed MySQL, but Cloud SQL Auth Proxy provides zero-trust encrypted connections without IP whitelisting. No security groups to manage.

4. **"How did you handle database connections in Cloud Run?"**
   — Used Cloud SQL Unix socket connection via the Cloud SQL Auth Proxy sidecar. Connection string uses `socketFactory=com.google.cloud.sql.mysql.SocketFactory`.

5. **"What was the most challenging deployment issue?"**
   — Nginx was configured to listen on port 80 (privileged port). Cloud Run requires non-root containers to use ports 1024+. Fixed all frontends to `listen 8080`. Also removed nginx `proxy_pass` blocks because backend hostnames aren't resolvable in Cloud Run's DNS at deployment time.
