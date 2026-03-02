# Google Cloud Platform Deployment Guide for College Services

## Overview
This guide deploys the college microservices application to **Google Cloud Platform (GCP)** using
Docker containers, **Artifact Registry**, and **Google Cloud Run** —
the GCP equivalents of AWS ECR/ECS and Azure ACR/Container Apps.

---

## AWS → GCP Service Mapping

| AWS Service | GCP Equivalent | Purpose |
|---|---|---|
| ECR (Elastic Container Registry) | Artifact Registry | Private Docker image registry |
| ECS / Fargate | Cloud Run | Serverless container hosting |
| MSK (Managed Kafka) | Pub/Sub + Managed Kafka for Apache Kafka | Message streaming |
| DocumentDB (MongoDB API) | Firestore (Native / Datastore mode) or MongoDB Atlas | NoSQL database |
| ALB (Application Load Balancer) | Cloud Load Balancing + Cloud Armor | HTTP/S load balancing |
| CloudWatch Logs | Cloud Logging (Stackdriver) | Centralized logging |
| CloudWatch Metrics | Cloud Monitoring | Performance monitoring |
| AWS Secrets Manager | Secret Manager | Secret management |
| IAM Roles | Cloud IAM + Service Accounts | Identity & access |
| Route 53 | Cloud DNS | Domain management |
| VPC | Virtual Private Cloud (VPC) | Network isolation |
| X-Ray | Cloud Trace | Distributed tracing |

---

## Architecture

```
Internet
    │
    ▼
Cloud Load Balancing  (SSL termination, Cloud Armor WAF)
    │
    ├──▶ college-notifications-frontend  (Cloud Run, Port 80)
    ├──▶ college-portal-frontend         (Cloud Run, Port 80)
    ├──▶ college-updates-frontend        (Cloud Run, Port 80)
    │
    ├──▶ college-admin                   (Cloud Run, Port 8082)
    ├──▶ college-notifications-backend   (Cloud Run, Port 8083)
    └──▶ college-updates-backend         (Cloud Run, Port 8081)
           │                │
           ▼                ▼
    Pub/Sub / Managed    Firestore / MongoDB
    Kafka (Kafka API)    Atlas on GCP
```

---

## Prerequisites

- Google Cloud SDK (gcloud CLI) installed: https://cloud.google.com/sdk/docs/install
- Docker Desktop installed and running
- Google Cloud project with billing enabled

```bash
# Verify prerequisites
gcloud --version
docker --version

# Login to Google Cloud
gcloud auth login

# Set your project
gcloud config set project YOUR_PROJECT_ID

# Confirm active project
gcloud config get-value project

# Enable required APIs
gcloud services enable \
    run.googleapis.com \
    artifactregistry.googleapis.com \
    pubsub.googleapis.com \
    secretmanager.googleapis.com \
    monitoring.googleapis.com \
    logging.googleapis.com \
    cloudtrace.googleapis.com \
    compute.googleapis.com
```

---

## Step 1: Test Locally with Docker Compose

```bash
# Build and verify all services locally first
docker-compose up --build

# Check services:
# - http://localhost:3000  (React Notifications)
# - http://localhost:3001  (React Portal)
# - http://localhost:4200  (Angular Updates)
# - http://localhost:8081  (Updates Backend)
# - http://localhost:8082  (Admin Backend)
# - http://localhost:8083  (Notifications Backend)
```

---

## Step 2: Deploy to GCP

### Option A: Windows
```cmd
deploy-gcp.bat
```

### Option B: Linux / Mac / WSL
```bash
chmod +x deploy-gcp.sh
./deploy-gcp.sh
```

---

## Step 3: Set up Artifact Registry
**AWS Equivalent: Amazon ECR**
**Azure Equivalent: Azure Container Registry (ACR)**

```bash
PROJECT_ID="your-gcp-project-id"
REGION="us-central1"
REPO_NAME="college-services"

# Create Artifact Registry repository
gcloud artifacts repositories create $REPO_NAME \
    --repository-format=docker \
    --location=$REGION \
    --description="College Services Docker images"

# Configure Docker to authenticate with Artifact Registry
gcloud auth configure-docker ${REGION}-docker.pkg.dev

# Registry URL format:
# ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}/<image>:<tag>
echo "Registry: ${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}"
```

---

## Step 4: Set up Google Cloud Pub/Sub (Kafka Alternative)
**AWS Equivalent: Amazon MSK**
**Azure Equivalent: Azure Event Hubs**

### Option A: Cloud Pub/Sub (Native GCP messaging)
```bash
PROJECT_ID="your-gcp-project-id"

# Create Pub/Sub topics (equivalent to Kafka topics)
gcloud pubsub topics create college-notifications-topic
gcloud pubsub topics create college-updates-topic

# Create subscriptions (equivalent to Kafka consumer groups)
gcloud pubsub subscriptions create college-notifications-sub \
    --topic=college-notifications-topic \
    --ack-deadline=30

gcloud pubsub subscriptions create college-updates-sub \
    --topic=college-updates-topic \
    --ack-deadline=30

# Grant Cloud Run service account access
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:college-services-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/pubsub.publisher"

gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:college-services-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/pubsub.subscriber"
```

### Option B: Managed Kafka for Apache Kafka on GCP (Kafka-compatible)
```bash
PROJECT_ID="your-gcp-project-id"
REGION="us-central1"
KAFKA_CLUSTER="college-kafka-cluster"

# Enable Managed Kafka API
gcloud services enable managedkafka.googleapis.com

# Create Kafka cluster
gcloud managed-kafka clusters create $KAFKA_CLUSTER \
    --location=$REGION \
    --cpu=3 \
    --memory=3GiB \
    --subnets=projects/${PROJECT_ID}/regions/${REGION}/subnetworks/default

# Create Kafka topics
gcloud managed-kafka topics create college-notifications-topic \
    --cluster=$KAFKA_CLUSTER \
    --location=$REGION \
    --partitions=3 \
    --replication-factor=3

gcloud managed-kafka topics create college-updates-topic \
    --cluster=$KAFKA_CLUSTER \
    --location=$REGION \
    --partitions=3 \
    --replication-factor=3

# Get bootstrap server endpoint
gcloud managed-kafka clusters describe $KAFKA_CLUSTER \
    --location=$REGION \
    --format="value(bootstrapAddress)"
```

**Spring Boot Kafka config for GCP Managed Kafka:**
```yaml
# application-gcp.yml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}   # from gcloud describe
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: OAUTHBEARER
      sasl.login.callback.handler.class: com.google.cloud.hosted.kafka.auth.GcpLoginCallbackHandler
      sasl.jaas.config: org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;
```

---

## Step 5: Set up Firestore for MongoDB Workloads
**AWS Equivalent: Amazon DocumentDB**
**Azure Equivalent: Azure Cosmos DB for MongoDB API**

### Option A: Firestore in Native Mode (recommended for new apps)
```bash
PROJECT_ID="your-gcp-project-id"

# Create Firestore database (Native mode)
gcloud firestore databases create \
    --location=nam5 \
    --type=firestore-native

# Grant Cloud Run service account access
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:college-services-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/datastore.user"
```

### Option B: MongoDB Atlas on GCP (drop-in MongoDB replacement)
1. Sign up at https://www.mongodb.com/cloud/atlas
2. Create cluster → choose **Google Cloud** as provider
3. Select region (e.g., `us-central1`)
4. Get connection string: `mongodb+srv://user:pass@cluster.mongodb.net/college-db`

**Spring Boot MongoDB config for MongoDB Atlas on GCP:**
```yaml
# application-gcp.yml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}   # mongodb+srv connection string from Atlas
      database: college-notifications-db
```

---

## Step 6: Update Cloud Run Services with Environment Variables

```bash
PROJECT_ID="your-gcp-project-id"
REGION="us-central1"
KAFKA_BOOTSTRAP="bootstrap.college-kafka-cluster.us-central1.managedkafka.${PROJECT_ID}.cloud.goog:9092"
MONGODB_URI="mongodb+srv://user:pass@cluster.mongodb.net/college-db"

# Update college-notifications-backend with Kafka + MongoDB
gcloud run services update college-notifications-backend \
    --region=$REGION \
    --set-env-vars \
        SPRING_PROFILES_ACTIVE=gcp,\
        SPRING_KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP",\
        SPRING_DATA_MONGODB_URI="$MONGODB_URI"

# Update college-admin with Kafka
gcloud run services update college-admin \
    --region=$REGION \
    --set-env-vars \
        SPRING_PROFILES_ACTIVE=gcp,\
        SPRING_KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP"

# Update college-updates-backend with Kafka
gcloud run services update college-updates-backend \
    --region=$REGION \
    --set-env-vars \
        SPRING_PROFILES_ACTIVE=gcp,\
        SPRING_KAFKA_BOOTSTRAP_SERVERS="$KAFKA_BOOTSTRAP"
```

---

## Step 7: Set up Secret Manager
**AWS Equivalent: AWS Secrets Manager**
**Azure Equivalent: Azure Key Vault**

```bash
PROJECT_ID="your-gcp-project-id"

# Create secrets
echo -n "YOUR_KAFKA_BOOTSTRAP_SERVERS" | \
    gcloud secrets create kafka-bootstrap-servers \
    --data-file=- \
    --replication-policy=automatic

echo -n "YOUR_MONGODB_URI" | \
    gcloud secrets create mongodb-uri \
    --data-file=- \
    --replication-policy=automatic

echo -n "YOUR_GOOGLE_PROJECT_ID" | \
    gcloud secrets create gcp-project-id \
    --data-file=- \
    --replication-policy=automatic

# Grant Cloud Run service account access to secrets
SA_EMAIL="college-services-sa@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud secrets add-iam-policy-binding kafka-bootstrap-servers \
    --member="serviceAccount:${SA_EMAIL}" \
    --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding mongodb-uri \
    --member="serviceAccount:${SA_EMAIL}" \
    --role="roles/secretmanager.secretAccessor"

# Mount secrets as environment variables in Cloud Run
gcloud run services update college-admin \
    --region=us-central1 \
    --set-secrets="SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-bootstrap-servers:latest,SPRING_DATA_MONGODB_URI=mongodb-uri:latest"
```

---

## Step 8: Set up Cloud Load Balancing
**AWS Equivalent: Application Load Balancer (ALB)**
**Azure Equivalent: Azure Application Gateway**

```bash
PROJECT_ID="your-gcp-project-id"
REGION="us-central1"

# Create serverless NEGs (Network Endpoint Groups) for each Cloud Run service
for SERVICE in college-admin college-notifications-backend college-updates-backend \
               college-notifications-frontend college-portal-frontend college-updates-frontend; do
    gcloud compute network-endpoint-groups create "${SERVICE}-neg" \
        --region=$REGION \
        --network-endpoint-type=serverless \
        --cloud-run-service=$SERVICE
done

# Create backend services for each NEG
for SERVICE in college-admin college-notifications-backend college-updates-backend \
               college-notifications-frontend college-portal-frontend college-updates-frontend; do
    gcloud compute backend-services create "${SERVICE}-backend" \
        --global

    gcloud compute backend-services add-backend "${SERVICE}-backend" \
        --global \
        --network-endpoint-group="${SERVICE}-neg" \
        --network-endpoint-group-region=$REGION
done

# Create URL map
gcloud compute url-maps create college-lb-url-map \
    --default-service=college-admin-backend

# Add path matchers for routing
gcloud compute url-maps import college-lb-url-map \
    --global \
    --source=- <<'EOF'
name: college-lb-url-map
defaultService: college-admin-backend
hostRules:
  - hosts: ["*"]
    pathMatcher: all-paths
pathMatchers:
  - name: all-paths
    defaultService: college-admin-backend
    pathRules:
      - paths: ["/api/admin/*"]
        service: college-admin-backend
      - paths: ["/api/notifications/*"]
        service: college-notifications-backend-backend
      - paths: ["/api/updates/*"]
        service: college-updates-backend-backend
      - paths: ["/notifications", "/notifications/*"]
        service: college-notifications-frontend-backend
      - paths: ["/portal", "/portal/*"]
        service: college-portal-frontend-backend
      - paths: ["/updates", "/updates/*"]
        service: college-updates-frontend-backend
EOF

# Create HTTPS proxy with managed SSL certificate
gcloud compute managed-ssl-certificates create college-ssl-cert \
    --domains=college.your-domain.com

gcloud compute target-https-proxies create college-https-proxy \
    --url-map=college-lb-url-map \
    --ssl-certificates=college-ssl-cert

# Create forwarding rule (global IP)
gcloud compute addresses create college-lb-ip --global

gcloud compute forwarding-rules create college-https-forwarding-rule \
    --global \
    --target-https-proxy=college-https-proxy \
    --address=college-lb-ip \
    --ports=443
```

---

## Step 9: Configure Auto Scaling
**AWS Equivalent: ECS Auto Scaling**
**Azure Equivalent: Container Apps auto-scale rules**

Cloud Run scales automatically from 0 to N instances based on request concurrency.

```bash
REGION="us-central1"

# Configure scaling for backend services (heavier workload)
gcloud run services update college-admin \
    --region=$REGION \
    --min-instances=1 \
    --max-instances=10 \
    --concurrency=80

gcloud run services update college-notifications-backend \
    --region=$REGION \
    --min-instances=1 \
    --max-instances=5 \
    --concurrency=80

gcloud run services update college-updates-backend \
    --region=$REGION \
    --min-instances=1 \
    --max-instances=5 \
    --concurrency=80

# Configure scaling for frontend services (scale to zero when not in use)
for SERVICE in college-notifications-frontend college-portal-frontend college-updates-frontend; do
    gcloud run services update $SERVICE \
        --region=$REGION \
        --min-instances=0 \
        --max-instances=5 \
        --concurrency=1000
done
```

---

## Step 10: Set up Cloud Logging + Cloud Monitoring
**AWS Equivalent: CloudWatch Logs + CloudWatch Metrics + X-Ray**
**Azure Equivalent: Azure Monitor + Application Insights**

```bash
PROJECT_ID="your-gcp-project-id"

# Cloud Logging is enabled automatically for Cloud Run
# View logs via gcloud
gcloud logging read \
    "resource.type=cloud_run_revision AND resource.labels.service_name=college-admin" \
    --limit=50 \
    --format="value(textPayload)" \
    --project=$PROJECT_ID

# Stream live logs (equivalent to az containerapp logs show --follow)
gcloud beta run services logs tail college-admin \
    --region=us-central1 \
    --project=$PROJECT_ID

# Create uptime checks (equivalent to ALB health checks)
gcloud monitoring uptime-check-configs create college-admin-uptime \
    --display-name="College Admin Uptime" \
    --resource-type=uptime-url \
    --monitored-resource="labels:project_id=${PROJECT_ID}"

# Create alerting policy for high error rate
gcloud alpha monitoring policies create \
    --policy-from-file=monitoring-policy.json
```

**Spring Boot Cloud Monitoring integration (add to pom.xml):**
```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-starter-trace</artifactId>
</dependency>
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-starter-logging</artifactId>
</dependency>
```

```yaml
# application-gcp.yml
spring:
  cloud:
    gcp:
      trace:
        enabled: true
        sampling-rate: 1.0
      logging:
        enabled: true
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

## Monitoring & Troubleshooting

### Check Service Status
**AWS:** `aws ecs describe-services`
**Azure:** `az containerapp list`
**GCP:**
```bash
# List all Cloud Run services
gcloud run services list --region=us-central1

# Show specific service details
gcloud run services describe college-admin --region=us-central1

# List running revisions
gcloud run revisions list --service=college-admin --region=us-central1
```

### View Logs
**AWS:** `aws logs tail /ecs/college-admin --follow`
**Azure:** `az containerapp logs show --follow`
**GCP:**
```bash
gcloud beta run services logs tail college-admin --region=us-central1
```

### Update Service with New Image
**AWS:** `aws ecs update-service --task-definition college-admin-task:2`
**Azure:** `az containerapp update --image <new-image>`
**GCP:**
```bash
gcloud run deploy college-admin \
    --image us-central1-docker.pkg.dev/YOUR_PROJECT/college-services/college-admin:v1.0.1 \
    --region=us-central1
```

### Common Issues

| Issue | AWS Solution | Azure Solution | GCP Solution |
|--|--|--|--|
| Service won't start | Check CloudWatch Logs | `az containerapp logs show` | `gcloud run services logs tail` |
| Cannot connect to Kafka | Check MSK security groups | Check Event Hubs firewall | Check Managed Kafka VPC config |
| Frontend 502 errors | Check ALB target group | Check Container App health probes | Check Cloud Run health check |
| DB connection issues | Check DocDB security groups | Check Cosmos DB firewall | Check Firestore IAM / Atlas VPC |
| Image pull failure | Check ECR permissions | Check ACR admin/Managed Identity | Check Artifact Registry IAM |
| Cold start latency | ECS keeps containers warm | Set min-replicas >= 1 | Set `--min-instances=1` |

---

## Cleanup
**AWS:** `aws ecs delete-cluster --cluster college-services`
**Azure:** `az group delete --name rg-college-services --yes`
**GCP:**
```bash
PROJECT_ID="your-gcp-project-id"
REGION="us-central1"

# Delete Cloud Run services
for SERVICE in college-admin college-notifications-backend college-updates-backend \
               college-notifications-frontend college-portal-frontend college-updates-frontend; do
    gcloud run services delete $SERVICE --region=$REGION --quiet
done

# Delete Artifact Registry repository
gcloud artifacts repositories delete college-services \
    --location=$REGION \
    --quiet

# Delete Pub/Sub topics
gcloud pubsub topics delete college-notifications-topic
gcloud pubsub topics delete college-updates-topic

# Delete Managed Kafka cluster
gcloud managed-kafka clusters delete college-kafka-cluster \
    --location=$REGION \
    --quiet

# Delete secrets
gcloud secrets delete kafka-bootstrap-servers --quiet
gcloud secrets delete mongodb-uri --quiet

# Delete load balancer resources
gcloud compute forwarding-rules delete college-https-forwarding-rule --global --quiet
gcloud compute target-https-proxies delete college-https-proxy --quiet
gcloud compute url-maps delete college-lb-url-map --quiet
```

---

## Security Best Practices

- Use **Workload Identity** for Cloud Run → avoid storing service account keys
- Store all secrets in **Secret Manager** (not env vars directly)
- Enable **Cloud Armor** on the Load Balancer for WAF and DDoS protection
- Use **VPC Service Controls** to restrict Artifact Registry and Secret Manager access
- Apply **least-privilege IAM** with custom service accounts per service
- Enable **Binary Authorization** for verified container image policies
- Use **Private Google Access** for VPC subnets to avoid internet egress

---

## Cost Optimization

- Cloud Run bills per request + CPU/memory only while handling requests
- Scale-to-zero by default (`--min-instances=0`) for dev/test environments
- Set `--min-instances=1` only for production services requiring low latency
- Use **Cloud Run CPU allocation** = "request" (default) vs "always-on"
- Use **Spot (preemptible)** VMs if running on GKE instead of Cloud Run
- Choose **Pub/Sub** over Managed Kafka for cost savings on low-throughput workloads
- Use **Firestore** free tier (1 GB storage, 50K reads/day free)

---

## Next Steps

1. Set up CI/CD with **GitHub Actions** + Artifact Registry + Cloud Run (`gcloud run deploy`)
2. Implement **traffic splitting** with Cloud Run revisions for blue/green deployments
3. Add **Cloud CDN** + **Cloud Armor** for global CDN and DDoS protection
4. Set up **Firestore multi-region** or MongoDB Atlas global clusters for disaster recovery
5. Implement **distributed tracing** with Cloud Trace + OpenTelemetry
6. Explore **Cloud Run Jobs** for batch processing workloads
