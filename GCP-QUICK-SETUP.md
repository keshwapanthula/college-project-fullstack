# GCP Quick Setup — College Services

> Equivalent of AWS-QUICK-SETUP.md and AZURE-QUICK-SETUP.md but for Google Cloud Platform

---

## 1-Minute Prerequisites Check

```cmd
gcloud --version          REM Google Cloud SDK (400+)
docker --version          REM Docker Desktop
gcloud auth login         REM Authenticate to GCP
gcloud config get-value project   REM Confirm active project
```

---

## Quick Deploy (5 Commands)

```cmd
REM Windows
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
deploy-gcp.bat
```

```bash
# Linux / Mac / WSL
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
chmod +x deploy-gcp.sh && ./deploy-gcp.sh
```

---

## Service Endpoints After Deployment

After `deploy-gcp.bat` completes, run this to get all live URLs:

```cmd
gcloud run services list --region=us-central1 --format="table(SERVICE,URL)"
```

---

## AWS → GCP Quick Reference

| What you want | AWS Command | Azure Command | GCP Command |
|---|---|---|---|
| List running containers | `aws ecs list-tasks` | `az containerapp replica list` | `gcloud run services list --region=us-central1` |
| View logs | `aws logs tail /ecs/<app>` | `az containerapp logs show --follow` | `gcloud beta run services logs tail <app>` |
| Push image | `docker push <ecr-url>/<img>` | `docker push <acr>.azurecr.io/<img>` | `docker push us-central1-docker.pkg.dev/<proj>/<repo>/<img>` |
| Update service | `aws ecs update-service ...` | `az containerapp update --image ...` | `gcloud run deploy <app> --image <new-image>` |
| Scale service | `aws ecs update-service --desired-count 3` | `az containerapp update --min-replicas 3` | `gcloud run services update <app> --min-instances=3` |
| Check health | `aws ecs describe-services` | `az containerapp show` | `gcloud run services describe <app>` |
| Delete service | `aws ecs delete-service` | `az containerapp delete` | `gcloud run services delete <app> --region=us-central1` |
| Read a secret | `aws secretsmanager get-secret-value` | `az keyvault secret show` | `gcloud secrets versions access latest --secret=<name>` |

---

## Key Resource Names (defaults)

| Resource | Name |
|---|---|
| GCP Project | `your-gcp-project-id` |
| Region | `us-central1` |
| Artifact Registry Repo | `college-services` |
| Registry URL | `us-central1-docker.pkg.dev/<project>/college-services` |
| Cloud Run Environment | Serverless (no environment needed) |
| Pub/Sub Topic (notifications) | `college-notifications-topic` |
| Pub/Sub Topic (updates) | `college-updates-topic` |
| Managed Kafka Cluster | `college-kafka-cluster` |
| MongoDB (Atlas on GCP) | `cluster.mongodb.net/college-db` |
| Secret Manager - Kafka | `kafka-bootstrap-servers` |
| Secret Manager - MongoDB | `mongodb-uri` |
| Load Balancer | `college-lb-url-map` |
| Service Account | `college-services-sa` |

---

## Setup Order (Full Infrastructure)

```
1. deploy-gcp.bat / deploy-gcp.sh      → Artifact Registry + Cloud Run
2. Set up Pub/Sub or Managed Kafka      → Message streaming
3. Set up Firestore or MongoDB Atlas    → NoSQL database
4. Update Cloud Run services with env vars (Kafka + MongoDB endpoints)
5. Set up Secret Manager                → Secure secrets
6. Set up Cloud Load Balancing          → Unified HTTPS entry point
7. Set up Cloud Logging + Monitoring    → Logging + alerting
```

---

## Quickly Tear Down Everything

```bash
# Delete all Cloud Run services
for SERVICE in college-admin college-notifications-backend college-updates-backend \
               college-notifications-frontend college-portal-frontend college-updates-frontend; do
    gcloud run services delete $SERVICE --region=us-central1 --quiet
done

# Delete Artifact Registry repository
gcloud artifacts repositories delete college-services --location=us-central1 --quiet
```
> WARNING: This permanently deletes your Cloud Run services and container images.

---

## Portal

View everything in Google Cloud Console:
```
https://console.cloud.google.com/run?project=YOUR_PROJECT_ID
https://console.cloud.google.com/artifacts?project=YOUR_PROJECT_ID
https://console.cloud.google.com/logs?project=YOUR_PROJECT_ID
```
