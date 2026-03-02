#!/bin/bash
# Google Cloud Platform Artifact Registry and Cloud Run Deployment Script (Linux/Mac)
# Prerequisites: Google Cloud SDK installed, authenticated with: gcloud auth login
# GCP equivalent of deploy-azure.sh / deploy-aws.sh

set -e  # Exit on any error

# ============================================================
#  CONFIGURATION - Update these values before running
# ============================================================
GCP_REGION="us-central1"
PROJECT_NAME="college-services"
REPO_NAME="college-services"

# ============================================================
#  Derive values
# ============================================================
PROJECT_ID="${PROJECT_ID:-$(gcloud config get-value project 2>/dev/null)}"
if [ -z "$PROJECT_ID" ]; then
    echo "ERROR: GCP project not set. Run: gcloud config set project YOUR_PROJECT_ID"
    exit 1
fi
REGISTRY="${GCP_REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}"

echo ""
echo "=========================================="
echo " GCP Deployment for College Services"
echo "=========================================="
echo " Project   : $PROJECT_ID"
echo " Region    : $GCP_REGION"
echo " Registry  : $REGISTRY"
echo "=========================================="
echo ""

SERVICES=(
    "college-admin"
    "college-notifications-backend"
    "college-updates-backend"
    "college-notifications-frontend"
    "college-portal-frontend"
    "college-updates-frontend"
)

# ============================================================
#  Step 1: Enable required GCP APIs
# ============================================================
echo "[1/9] Enabling required GCP APIs..."
gcloud services enable \
    run.googleapis.com \
    artifactregistry.googleapis.com \
    pubsub.googleapis.com \
    secretmanager.googleapis.com \
    monitoring.googleapis.com \
    logging.googleapis.com \
    cloudtrace.googleapis.com \
    --project="$PROJECT_ID" || echo "Some APIs may already be enabled, continuing..."

# ============================================================
#  Step 2: Create Artifact Registry Repository
#  AWS Equivalent: Amazon ECR repository
#  Azure Equivalent: Azure Container Registry (ACR)
# ============================================================
echo "[2/9] Creating Artifact Registry repository..."
gcloud artifacts repositories create "$REPO_NAME" \
    --repository-format=docker \
    --location="$GCP_REGION" \
    --description="College Services Docker images" \
    --project="$PROJECT_ID" || echo "Repository may already exist, continuing..."

# ============================================================
#  Step 3: Authenticate Docker to Artifact Registry
#  AWS Equivalent: aws ecr get-login-password | docker login
#  Azure Equivalent: az acr login --name <acr>
# ============================================================
echo "[3/9] Authenticating Docker to Artifact Registry..."
gcloud auth configure-docker "${GCP_REGION}-docker.pkg.dev" --quiet

# ============================================================
#  Step 4: Build Docker Images
# ============================================================
echo "[4/9] Building Docker images..."
docker build -t college-admin ./CollegeAdmin
docker build -t college-notifications-backend ./CollegeNotifications
docker build -t college-updates-backend ./CollegeUpdates
docker build -t college-notifications-frontend ./college-notifications
docker build -t college-portal-frontend ./college-portal
docker build -t college-updates-frontend ./college-updates

# ============================================================
#  Step 5: Tag Images for Artifact Registry
#  AWS Equivalent: docker tag <image> <ecr-repo>:<tag>
#  Azure Equivalent: docker tag <image> <acr>.azurecr.io/<image>:<tag>
# ============================================================
echo "[5/9] Tagging images for Artifact Registry..."
for SERVICE in "${SERVICES[@]}"; do
    docker tag "${SERVICE}:latest" "${REGISTRY}/${SERVICE}:latest"
    docker tag "${SERVICE}:latest" "${REGISTRY}/${SERVICE}:v1.0.0"
    echo "  Tagged: ${SERVICE}"
done

# ============================================================
#  Step 6: Push Images to Artifact Registry
#  AWS Equivalent: docker push <ecr-registry>/<repo>:<tag>
#  Azure Equivalent: docker push <acr>.azurecr.io/<image>:<tag>
# ============================================================
echo "[6/9] Pushing images to Artifact Registry..."
for SERVICE in "${SERVICES[@]}"; do
    docker push "${REGISTRY}/${SERVICE}:latest"
    docker push "${REGISTRY}/${SERVICE}:v1.0.0"
    echo "  Pushed: ${SERVICE}"
done

# ============================================================
#  Step 7: Create Service Account for Cloud Run
# ============================================================
echo "[7/9] Creating Service Account..."
SA_EMAIL="college-services-sa@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud iam service-accounts create college-services-sa \
    --display-name="College Services SA" \
    --project="$PROJECT_ID" || echo "Service account may already exist, continuing..."

for ROLE in roles/secretmanager.secretAccessor roles/pubsub.publisher roles/pubsub.subscriber roles/datastore.user; do
    gcloud projects add-iam-policy-binding "$PROJECT_ID" \
        --member="serviceAccount:${SA_EMAIL}" \
        --role="$ROLE" --quiet || true
done

# ============================================================
#  Step 8: Helper function to deploy a Cloud Run service
#  AWS Equivalent: aws ecs create-service
#  Azure Equivalent: az containerapp create
# ============================================================
echo "[8/9] Preparing Cloud Run deployment..."

deploy_service() {
    local NAME=$1
    local PORT=$2
    local CPU=$3
    local MEMORY=$4
    local MIN_INSTANCES=$5
    local MAX_INSTANCES=$6
    local CONCURRENCY=$7
    local ENV_VARS=${8:-""}

    echo "  Deploying $NAME on port $PORT..."

    EXTRA_ENV=""
    if [ -n "$ENV_VARS" ]; then
        EXTRA_ENV="--set-env-vars=${ENV_VARS}"
    fi

    gcloud run deploy "$NAME" \
        --image="${REGISTRY}/${NAME}:latest" \
        --region="$GCP_REGION" \
        --platform=managed \
        --allow-unauthenticated \
        --port="$PORT" \
        --cpu="$CPU" \
        --memory="$MEMORY" \
        --min-instances="$MIN_INSTANCES" \
        --max-instances="$MAX_INSTANCES" \
        --concurrency="$CONCURRENCY" \
        --service-account="$SA_EMAIL" \
        --project="$PROJECT_ID" \
        $EXTRA_ENV

    echo "  $NAME deployed successfully."
}

# ============================================================
#  Step 9: Deploy All Services to Cloud Run
# ============================================================
echo "[9/9] Deploying all services to Cloud Run..."

# Backend services (0.5 vCPU, 1Gi RAM, keep 1 instance warm)
deploy_service "college-admin"                 8082 0.5 1Gi 1 5 80  "SPRING_PROFILES_ACTIVE=gcp,JAVA_OPTS=-XX:MaxRAMPercentage=75.0"
deploy_service "college-notifications-backend" 8083 0.5 1Gi 1 5 80  "SPRING_PROFILES_ACTIVE=gcp,JAVA_OPTS=-XX:MaxRAMPercentage=75.0"
deploy_service "college-updates-backend"       8081 0.5 1Gi 1 5 80  "SPRING_PROFILES_ACTIVE=gcp,JAVA_OPTS=-XX:MaxRAMPercentage=75.0"

# Frontend services (0.25 vCPU, 512Mi RAM, scale to zero)
deploy_service "college-notifications-frontend" 80 0.25 512Mi 0 5 1000
deploy_service "college-portal-frontend"        80 0.25 512Mi 0 5 1000
deploy_service "college-updates-frontend"       80 0.25 512Mi 0 5 1000

# ============================================================
#  Print Summary
# ============================================================
echo ""
echo "====================================================="
echo " GCP Deployment Complete!"
echo "====================================================="
echo ""
echo "Deployed Service URLs:"
gcloud run services list \
    --region="$GCP_REGION" \
    --project="$PROJECT_ID" \
    --format="table(SERVICE,URL)"

echo ""
echo "Artifact Registry: $REGISTRY"
echo ""
echo "Next Steps:"
echo "  1. Set up Pub/Sub topics or Managed Kafka  - see GCP-DEPLOYMENT-GUIDE.md Step 4"
echo "  2. Set up Firestore or MongoDB Atlas       - see GCP-DEPLOYMENT-GUIDE.md Step 5"
echo "  3. Update Cloud Run services with Kafka + MongoDB env vars"
echo "  4. Set up Secret Manager for secure secret storage"
echo "  5. Set up Cloud Load Balancing for unified HTTPS entry point"
echo "  6. Configure Cloud Logging and Cloud Monitoring"
echo ""
echo "View resources: https://console.cloud.google.com/run?project=${PROJECT_ID}"
