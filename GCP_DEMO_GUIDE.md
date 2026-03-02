# GCP Demo Guide — College Microservices Platform

**GCP Project**: `storied-shore-473817-v5` | **Region**: `us-central1`
**Account**: sandeepkumar.vr887@gmail.com

---

## ⚡ Quick Start — Live Endpoints (No Setup Required)

All services are already deployed. Hit these URLs directly:

```bash
# Backend health checks
curl https://college-admin-324266638161.us-central1.run.app/api/admin/health
curl https://college-notifications-backend-324266638161.us-central1.run.app/api/notifications/health
curl https://college-updates-backend-324266638161.us-central1.run.app/api/updates/health

# Frontend URLs (open in browser)
https://college-portal-frontend-324266638161.us-central1.run.app
https://college-notifications-frontend-324266638161.us-central1.run.app
https://college-updates-frontend-324266638161.us-central1.run.app
```

---

## 🗺️ All Live URLs Reference

| Service | URL |
|---------|-----|
| College Admin API | https://college-admin-324266638161.us-central1.run.app |
| Notifications API | https://college-notifications-backend-324266638161.us-central1.run.app |
| Updates API | https://college-updates-backend-324266638161.us-central1.run.app |
| College Portal UI | https://college-portal-frontend-324266638161.us-central1.run.app |
| Notifications UI | https://college-notifications-frontend-324266638161.us-central1.run.app |
| Updates UI | https://college-updates-frontend-324266638161.us-central1.run.app |

---

## 📋 Pre-Demo Checklist

```
□ gcloud CLI installed and authenticated
□ Docker Desktop running
□ GCP project set: gcloud config set project storied-shore-473817-v5
□ Browser tabs open for all 3 frontend URLs
□ Terminal ready for gcloud commands
□ GCP Console open: https://console.cloud.google.com
□ Cloud Run console: https://console.cloud.google.com/run?project=storied-shore-473817-v5
```

---

## 🎯 Demo Script — 15-Minute Interview Demo

### Part 1: Show Live Cloud Run Services (2 min)

```bash
# List all deployed Cloud Run services
gcloud run services list \
  --project=storied-shore-473817-v5 \
  --region=us-central1 \
  --format="table(metadata.name,status.url,status.conditions[0].type)"
```

**Talking point**: "I have 6 microservices running on Cloud Run — 3 Spring Boot backends and 3 frontend containers (React/Angular). Cloud Run auto-scales from zero to handle any load."

---

### Part 2: Hit a Live API (2 min)

```bash
# Get all students from College Admin (live production data)
curl -s https://college-admin-324266638161.us-central1.run.app/api/admin/students \
  | python -m json.tool

# Get GCP service integrations demo
curl -s https://college-admin-324266638161.us-central1.run.app/api/gcp/services \
  | python -m json.tool

# Cloud Storage operations demo
curl -s https://college-admin-324266638161.us-central1.run.app/api/gcp/storage/list
```

**Talking point**: "This is a real production Spring Boot app with Cloud SQL MySQL, Memorystore Redis caching, and Managed Kafka for event streaming."

---

### Part 3: Show Container Registry (1 min)

```bash
# List all images in Artifact Registry
gcloud artifacts docker images list \
  us-central1-docker.pkg.dev/storied-shore-473817-v5/college-services \
  --include-tags \
  --format="table(IMAGE,TAGS,CREATE_TIME)"
```

**Talking point**: "All 6 Docker images are stored in GCP Artifact Registry — the GCP equivalent of AWS ECR — with built-in vulnerability scanning."

---

### Part 4: GCP Service Integration Demo (5 min)

```bash
# 1. Cloud Storage (GCS equivalent of S3)
curl -s https://college-admin-324266638161.us-central1.run.app/api/gcp/storage/buckets

# 2. Firestore NoSQL operations
curl -s https://college-admin-324266638161.us-central1.run.app/api/gcp/firestore/students

# 3. Pub/Sub messaging
curl -s -X POST \
  https://college-admin-324266638161.us-central1.run.app/api/gcp/pubsub/publish \
  -H "Content-Type: application/json" \
  -d '{"topic":"student-events","message":"New enrollment: Student ID 12345"}'

# 4. Vertex AI prediction
curl -s -X POST \
  https://college-admin-324266638161.us-central1.run.app/api/gcp/ai/predict \
  -H "Content-Type: application/json" \
  -d '{"studentId":"12345","action":"graduation-risk-assessment"}'

# 5. Cloud Monitoring metrics
curl -s https://college-admin-324266638161.us-central1.run.app/actuator/metrics

# 6. Cloud Workflows orchestration
curl -s https://college-admin-324266638161.us-central1.run.app/api/gcp/workflows/enrollment
```

**Talking point**: "This service integrates with 10 GCP services — Cloud Storage, Firestore, Pub/Sub, Vertex AI, Cloud Functions, Managed Kafka, Secret Manager, Cloud Monitoring, Identity Platform, and Cloud Workflows. It's the GCP mirror of our AWS implementation."

---

### Part 5: Open GCP Console — Visual Demo (3 min)

**URLs to open in browser during demo**:

```
Cloud Run Services:
https://console.cloud.google.com/run?project=storied-shore-473817-v5

Artifact Registry:
https://console.cloud.google.com/artifacts?project=storied-shore-473817-v5

Cloud Monitoring Dashboard:
https://console.cloud.google.com/monitoring?project=storied-shore-473817-v5

Cloud Logging (live logs):
https://console.cloud.google.com/logs/query?project=storied-shore-473817-v5

Cloud SQL Instances:
https://console.cloud.google.com/sql?project=storied-shore-473817-v5
```

---

### Part 6: Code Walkthrough (2 min)

**Key files to highlight**:

1. **GCP Service Layer** — 10 service classes, one per GCP service
   ```
   CollegeAdmin/src/main/java/com/admin/service/gcp/
   ├── GcpCloudStorageService.java     ← GCS (like AWS S3)
   ├── GcpFirestoreService.java        ← NoSQL (like AWS DynamoDB)
   ├── GcpPubSubService.java           ← Messaging (like AWS SNS/SQS)
   ├── GcpAiPlatformService.java       ← ML (like AWS SageMaker)
   ├── GcpCloudFunctionsService.java   ← Serverless (like AWS Lambda)
   ├── GcpMonitoringService.java       ← Observability (like AWS CloudWatch)
   ├── GcpIdentityService.java         ← Auth (like AWS Cognito)
   ├── GcpApiGatewayService.java       ← API Mgmt (like AWS API Gateway)
   ├── GcpSearchService.java           ← Search (like AWS OpenSearch)
   └── GcpWorkflowsService.java        ← Orchestration (like AWS Step Functions)
   ```

2. **GCP Spring Profile Config**
   ```
   CollegeAdmin/src/main/resources/application-gcp.properties
   CollegeNotifications/src/main/resources/application-gcp.properties
   CollegeUpdates/src/main/resources/application-gcp.properties
   ```

3. **REST Controller**
   ```
   CollegeAdmin/src/main/java/com/admin/controller/GcpServicesController.java
   ```

---

## 🔄 Redeploy from Scratch (Full Setup)

### Prerequisites

```bash
# 1. Install gcloud CLI
# https://cloud.google.com/sdk/docs/install

# 2. Authenticate
gcloud auth login
gcloud config set project storied-shore-473817-v5
gcloud config set compute/region us-central1

# 3. Enable required APIs
gcloud services enable run.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  sql-component.googleapis.com \
  redis.googleapis.com \
  secretmanager.googleapis.com \
  monitoring.googleapis.com \
  logging.googleapis.com \
  aiplatform.googleapis.com \
  pubsub.googleapis.com \
  firestore.googleapis.com \
  storage.googleapis.com \
  workflows.googleapis.com

# 4. Configure Docker authentication
gcloud auth configure-docker us-central1-docker.pkg.dev
```

### Build & Deploy All Services

```bash
# Windows
.\deploy-gcp.bat

# Linux/Mac
./deploy-gcp.sh
```

### Manual Deployment (Individual Service)

```bash
# Build image
docker build -t us-central1-docker.pkg.dev/storied-shore-473817-v5/college-services/college-admin:latest \
  ./CollegeAdmin

# Push to Artifact Registry
docker push us-central1-docker.pkg.dev/storied-shore-473817-v5/college-services/college-admin:latest

# Deploy to Cloud Run
gcloud run deploy college-admin \
  --image=us-central1-docker.pkg.dev/storied-shore-473817-v5/college-services/college-admin:latest \
  --platform=managed \
  --region=us-central1 \
  --port=8082 \
  --allow-unauthenticated \
  --memory=1Gi \
  --cpu=1 \
  --min-instances=0 \
  --max-instances=10 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=gcp,GCP_PROJECT_ID=storied-shore-473817-v5"
```

---

## 🛢️ Database Setup (Cloud SQL)

```bash
# Create Cloud SQL MySQL instance
gcloud sql instances create college-mysql-db \
  --database-version=MYSQL_8_0 \
  --tier=db-n1-standard-2 \
  --region=us-central1 \
  --storage-type=SSD \
  --storage-size=20GB \
  --backup-start-time=03:00 \
  --enable-bin-log \
  --deletion-protection

# Create database
gcloud sql databases create college_system \
  --instance=college-mysql-db

# Create user
gcloud sql users create collegeadmin \
  --instance=college-mysql-db \
  --password=$(gcloud secrets versions access latest --secret="college-admin-db-password")
```

---

## 📡 Kafka Setup (GCP Managed Kafka)

```bash
# Create Managed Kafka cluster
gcloud managed-kafka clusters create college-kafka-cluster \
  --location=us-central1 \
  --cpu=3 \
  --memory=3Gi \
  --subnets=projects/storied-shore-473817-v5/regions/us-central1/subnetworks/default

# Create topics
gcloud managed-kafka topics create student-enrollment-events \
  --cluster=college-kafka-cluster \
  --location=us-central1 \
  --partitions=3 \
  --replication-factor=3

gcloud managed-kafka topics create college-news-update-events \
  --cluster=college-kafka-cluster \
  --location=us-central1 \
  --partitions=3 \
  --replication-factor=3

gcloud managed-kafka topics create grade-notification-events \
  --cluster=college-kafka-cluster \
  --location=us-central1 \
  --partitions=3 \
  --replication-factor=3
```

---

## 🔐 Secret Manager Setup

```bash
# Create secrets for all services
echo -n "your-db-password" | gcloud secrets create college-admin-db-password \
  --replication-policy="automatic" --data-file=-

echo -n "your-redis-auth" | gcloud secrets create college-redis-auth \
  --replication-policy="automatic" --data-file=-

echo -n "your-mongo-uri" | gcloud secrets create college-updates-mongodb-uri \
  --replication-policy="automatic" --data-file=-

echo -n "your-email-api-key" | gcloud secrets create college-notifications-email-api-key \
  --replication-policy="automatic" --data-file=-

# Grant Cloud Run service account access
gcloud secrets add-iam-policy-binding college-admin-db-password \
  --member="serviceAccount:$(gcloud projects describe storied-shore-473817-v5 \
    --format='value(projectNumber)')-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

---

## 🔍 Troubleshooting

### Check service logs
```bash
# Stream real-time logs
gcloud run services logs tail college-admin \
  --project=storied-shore-473817-v5 \
  --region=us-central1

# Query logs from Cloud Logging
gcloud logging read \
  'resource.type="cloud_run_revision" AND resource.labels.service_name="college-admin"' \
  --project=storied-shore-473817-v5 \
  --limit=50 \
  --format=json
```

### Check service status
```bash
gcloud run services describe college-admin \
  --project=storied-shore-473817-v5 \
  --region=us-central1 \
  --format="yaml(status)"
```

### Test health endpoints
```bash
for service in college-admin college-notifications-backend college-updates-backend; do
  echo "=== $service ==="
  gcloud run services describe $service \
    --region=us-central1 \
    --project=storied-shore-473817-v5 \
    --format="value(status.url)"
done
```

### Common Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| Container fails to start | Wrong PORT | Cloud Run injects `PORT` env var; app must listen on it |
| Nginx 502 | proxy_pass hostname unresolvable | Remove proxy_pass blocks; use direct Cloud Run URLs |
| DB connection refused | Cloud SQL proxy not configured | Use Cloud SQL Auth Proxy or Unix socket connection |
| Kafka auth failure | Missing SASL config | Set `security.protocol=SASL_SSL` and OAuth token URL |
| Image not found | Wrong region in image URL | Use `us-central1-docker.pkg.dev` prefix |

---

## 📊 Monitoring During Demo

```bash
# Check Cloud Monitoring metrics via CLI
gcloud monitoring metrics-scopes list --project=storied-shore-473817-v5

# View Custom Metrics (Micrometer → Stackdriver)
gcloud monitoring time-series list \
  --filter='metric.type="custom.googleapis.com/jvm_memory_used_bytes"' \
  --project=storied-shore-473817-v5
```

### Spring Boot Actuator Metrics
```bash
# JVM metrics
curl https://college-admin-324266638161.us-central1.run.app/actuator/metrics/jvm.memory.used

# HTTP request metrics
curl https://college-admin-324266638161.us-central1.run.app/actuator/metrics/http.server.requests

# Hikari pool status
curl https://college-admin-324266638161.us-central1.run.app/actuator/metrics/hikaricp.connections.active
```

---

## 💡 Key Interview Talking Points

### Architecture Decisions

**Q: Why Cloud Run over GKE?**
> "Cloud Run is perfect for stateless microservices. Zero cluster management, automatic scaling to zero (cost savings), built-in HTTPS, and fast deployments. I'd choose GKE for stateful workloads or when needing DaemonSets or custom node configs."

**Q: How do services communicate without Eureka?**
> "Cloud Run services get stable HTTPS URLs. I pass these URLs as environment variables at deployment time. No side-car discovery proxy needed — Cloud Run's internal DNS + HTTPS handles it. It's actually simpler and more secure than Eureka."

**Q: How did you handle the nginx port issue on Cloud Run?**
> "Cloud Run containers run as non-root by default. Port 80 requires root privileges. Cloud Run injects a `PORT` environment variable (default 8080). Fixed all nginx configs to `listen 8080` and added `${PORT:-8080}` to the `server {}` block."

**Q: Compare GCP Managed Kafka to AWS MSK**
> "Both are fully managed Apache Kafka. GCP Managed Kafka uses Workload Identity for auth — no API keys to manage. MSK uses either IAM or SASL/SCRAM. GCP integrates natively with Pub/Sub for fan-out scenarios. Cost model differs: MSK charges for broker hours; GCP Kafka charges for throughput."

**Q: What's different about Cloud SQL vs RDS?**
> "Functionally similar — both managed MySQL. Cloud SQL Auth Proxy is a standout feature: creates encrypted TCP tunnels using IAM auth. No security groups, no VPC peering needed for development. For production, Cloud SQL supports Private Service Connect for zero-public-IP deployments."
