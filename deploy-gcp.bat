@echo off
REM Google Cloud Platform Artifact Registry and Cloud Run Deployment Script (Windows)
REM Prerequisites: Google Cloud SDK installed, authenticated with: gcloud auth login
REM GCP equivalent of deploy-azure.bat / deploy-aws.bat

REM ============================================================
REM  CONFIGURATION - Update these values before running
REM ============================================================
set GCP_REGION=us-central1
set PROJECT_NAME=college-services
set REPO_NAME=college-services
set PROJECT_ID=storied-shore-473817-v5

REM ============================================================
REM  Derive registry URL
REM ============================================================
if "%PROJECT_ID%"=="your-gcp-project-id" (
    for /f %%i in ('gcloud config get-value project') do set PROJECT_ID=%%i
)
set REGISTRY=%GCP_REGION%-docker.pkg.dev/%PROJECT_ID%/%REPO_NAME%

echo.
echo ==========================================
echo  GCP Deployment for College Services
echo ==========================================
echo  Project   : %PROJECT_ID%
echo  Region    : %GCP_REGION%
echo  Registry  : %REGISTRY%
echo ==========================================
echo.

REM ============================================================
REM  Step 1: Enable required GCP APIs
REM ============================================================
echo [1/9] Enabling required GCP APIs...
gcloud services enable run.googleapis.com artifactregistry.googleapis.com ^
    pubsub.googleapis.com secretmanager.googleapis.com ^
    monitoring.googleapis.com logging.googleapis.com ^
    cloudtrace.googleapis.com --project=%PROJECT_ID%
if %ERRORLEVEL% NEQ 0 (echo WARNING: Some APIs may already be enabled, continuing...)

REM ============================================================
REM  Step 2: Create Artifact Registry Repository
REM  AWS Equivalent: Amazon ECR repository
REM  Azure Equivalent: Azure Container Registry (ACR)
REM ============================================================
echo [2/9] Creating Artifact Registry repository...
gcloud artifacts repositories create %REPO_NAME% ^
    --repository-format=docker ^
    --location=%GCP_REGION% ^
    --description="College Services Docker images" ^
    --project=%PROJECT_ID%
if %ERRORLEVEL% NEQ 0 (echo WARNING: Repository may already exist, continuing...)

REM ============================================================
REM  Step 3: Authenticate Docker to Artifact Registry
REM  AWS Equivalent: aws ecr get-login-password | docker login
REM  Azure Equivalent: az acr login --name <acr>
REM ============================================================
echo [3/9] Authenticating Docker to Artifact Registry...
gcloud auth configure-docker %GCP_REGION%-docker.pkg.dev --quiet

REM ============================================================
REM  Step 4: Build Docker Images
REM ============================================================
echo [4/9] Building Docker images...
docker build -t college-admin .\CollegeAdmin
docker build -t college-notifications-backend .\CollegeNotifications
docker build -t college-updates-backend .\CollegeUpdates
docker build -t college-notifications-frontend .\college-notifications
docker build -t college-portal-frontend .\college-portal
docker build -t college-updates-frontend .\college-updates

REM ============================================================
REM  Step 5: Tag Images for Artifact Registry
REM  AWS Equivalent: docker tag <image> <ecr-repo>:<tag>
REM  Azure Equivalent: docker tag <image> <acr>.azurecr.io/<image>:<tag>
REM ============================================================
echo [5/9] Tagging images for Artifact Registry...
docker tag college-admin:latest %REGISTRY%/college-admin:latest
docker tag college-admin:latest %REGISTRY%/college-admin:v1.0.0
docker tag college-notifications-backend:latest %REGISTRY%/college-notifications-backend:latest
docker tag college-notifications-backend:latest %REGISTRY%/college-notifications-backend:v1.0.0
docker tag college-updates-backend:latest %REGISTRY%/college-updates-backend:latest
docker tag college-updates-backend:latest %REGISTRY%/college-updates-backend:v1.0.0
docker tag college-notifications-frontend:latest %REGISTRY%/college-notifications-frontend:latest
docker tag college-notifications-frontend:latest %REGISTRY%/college-notifications-frontend:v1.0.0
docker tag college-portal-frontend:latest %REGISTRY%/college-portal-frontend:latest
docker tag college-portal-frontend:latest %REGISTRY%/college-portal-frontend:v1.0.0
docker tag college-updates-frontend:latest %REGISTRY%/college-updates-frontend:latest
docker tag college-updates-frontend:latest %REGISTRY%/college-updates-frontend:v1.0.0

REM ============================================================
REM  Step 6: Push Images to Artifact Registry
REM  AWS Equivalent: docker push <ecr-registry>/<repo>:<tag>
REM  Azure Equivalent: docker push <acr>.azurecr.io/<image>:<tag>
REM ============================================================
echo [6/9] Pushing images to Artifact Registry...
docker push %REGISTRY%/college-admin:latest
docker push %REGISTRY%/college-admin:v1.0.0
docker push %REGISTRY%/college-notifications-backend:latest
docker push %REGISTRY%/college-notifications-backend:v1.0.0
docker push %REGISTRY%/college-updates-backend:latest
docker push %REGISTRY%/college-updates-backend:v1.0.0
docker push %REGISTRY%/college-notifications-frontend:latest
docker push %REGISTRY%/college-notifications-frontend:v1.0.0
docker push %REGISTRY%/college-portal-frontend:latest
docker push %REGISTRY%/college-portal-frontend:v1.0.0
docker push %REGISTRY%/college-updates-frontend:latest
docker push %REGISTRY%/college-updates-frontend:v1.0.0

REM ============================================================
REM  Step 7: Create Service Account for Cloud Run
REM ============================================================
echo [7/9] Creating Service Account...
gcloud iam service-accounts create college-services-sa ^
    --display-name="College Services SA" ^
    --project=%PROJECT_ID%
if %ERRORLEVEL% NEQ 0 (echo WARNING: Service account may already exist, continuing...)

gcloud projects add-iam-policy-binding %PROJECT_ID% ^
    --member="serviceAccount:college-services-sa@%PROJECT_ID%.iam.gserviceaccount.com" ^
    --role="roles/secretmanager.secretAccessor"

gcloud projects add-iam-policy-binding %PROJECT_ID% ^
    --member="serviceAccount:college-services-sa@%PROJECT_ID%.iam.gserviceaccount.com" ^
    --role="roles/pubsub.publisher"

gcloud projects add-iam-policy-binding %PROJECT_ID% ^
    --member="serviceAccount:college-services-sa@%PROJECT_ID%.iam.gserviceaccount.com" ^
    --role="roles/pubsub.subscriber"

REM ============================================================
REM  Step 8: (Optional) Allow unauthenticated access to services
REM          Remove --allow-unauthenticated for private services
REM ============================================================

REM ============================================================
REM  Step 9: Deploy to Cloud Run
REM  AWS Equivalent: aws ecs create-service
REM  Azure Equivalent: az containerapp create
REM ============================================================
echo [9/9] Deploying to Cloud Run...

REM Deploy college-admin backend (0.5 vCPU, 512Mi)
echo   Deploying college-admin...
gcloud run deploy college-admin ^
    --image=%REGISTRY%/college-admin:latest ^
    --region=%GCP_REGION% ^
    --platform=managed ^
    --allow-unauthenticated ^
    --port=8082 ^
    --cpu=0.5 ^
    --memory=1Gi ^
    --min-instances=1 ^
    --max-instances=5 ^
    --concurrency=80 ^
    --service-account=college-services-sa@%PROJECT_ID%.iam.gserviceaccount.com ^
    --set-env-vars=SPRING_PROFILES_ACTIVE=gcp,JAVA_OPTS=-XX:MaxRAMPercentage=75.0 ^
    --project=%PROJECT_ID%
echo   college-admin deployed.

REM Deploy college-notifications-backend
echo   Deploying college-notifications-backend...
gcloud run deploy college-notifications-backend ^
    --image=%REGISTRY%/college-notifications-backend:latest ^
    --region=%GCP_REGION% ^
    --platform=managed ^
    --allow-unauthenticated ^
    --port=8083 ^
    --cpu=0.5 ^
    --memory=1Gi ^
    --min-instances=1 ^
    --max-instances=5 ^
    --concurrency=80 ^
    --service-account=college-services-sa@%PROJECT_ID%.iam.gserviceaccount.com ^
    --set-env-vars=SPRING_PROFILES_ACTIVE=gcp,JAVA_OPTS=-XX:MaxRAMPercentage=75.0 ^
    --project=%PROJECT_ID%
echo   college-notifications-backend deployed.

REM Deploy college-updates-backend
echo   Deploying college-updates-backend...
gcloud run deploy college-updates-backend ^
    --image=%REGISTRY%/college-updates-backend:latest ^
    --region=%GCP_REGION% ^
    --platform=managed ^
    --allow-unauthenticated ^
    --port=8081 ^
    --cpu=0.5 ^
    --memory=1Gi ^
    --min-instances=1 ^
    --max-instances=5 ^
    --concurrency=80 ^
    --service-account=college-services-sa@%PROJECT_ID%.iam.gserviceaccount.com ^
    --set-env-vars=SPRING_PROFILES_ACTIVE=gcp,JAVA_OPTS=-XX:MaxRAMPercentage=75.0 ^
    --project=%PROJECT_ID%
echo   college-updates-backend deployed.

REM Deploy college-notifications-frontend
echo   Deploying college-notifications-frontend...
gcloud run deploy college-notifications-frontend ^
    --image=%REGISTRY%/college-notifications-frontend:latest ^
    --region=%GCP_REGION% ^
    --platform=managed ^
    --allow-unauthenticated ^
    --port=80 ^
    --cpu=0.25 ^
    --memory=512Mi ^
    --min-instances=0 ^
    --max-instances=5 ^
    --concurrency=1000 ^
    --project=%PROJECT_ID%
echo   college-notifications-frontend deployed.

REM Deploy college-portal-frontend
echo   Deploying college-portal-frontend...
gcloud run deploy college-portal-frontend ^
    --image=%REGISTRY%/college-portal-frontend:latest ^
    --region=%GCP_REGION% ^
    --platform=managed ^
    --allow-unauthenticated ^
    --port=80 ^
    --cpu=0.25 ^
    --memory=512Mi ^
    --min-instances=0 ^
    --max-instances=5 ^
    --concurrency=1000 ^
    --project=%PROJECT_ID%
echo   college-portal-frontend deployed.

REM Deploy college-updates-frontend
echo   Deploying college-updates-frontend...
gcloud run deploy college-updates-frontend ^
    --image=%REGISTRY%/college-updates-frontend:latest ^
    --region=%GCP_REGION% ^
    --platform=managed ^
    --allow-unauthenticated ^
    --port=80 ^
    --cpu=0.25 ^
    --memory=512Mi ^
    --min-instances=0 ^
    --max-instances=5 ^
    --concurrency=1000 ^
    --project=%PROJECT_ID%
echo   college-updates-frontend deployed.

REM ============================================================
REM  Print Summary
REM ============================================================
echo.
echo =====================================================
echo  GCP Deployment Complete!
echo =====================================================
echo.
echo Getting deployed service URLs...
gcloud run services list --region=%GCP_REGION% --project=%PROJECT_ID% ^
    --format="table(SERVICE,URL)"

echo.
echo Artifact Registry: %REGISTRY%
echo.
echo Next Steps:
echo   1. Set up Pub/Sub topics or Managed Kafka - see GCP-DEPLOYMENT-GUIDE.md Step 4
echo   2. Set up Firestore or MongoDB Atlas     - see GCP-DEPLOYMENT-GUIDE.md Step 5
echo   3. Update Cloud Run services with Kafka + MongoDB env vars
echo   4. Set up Secret Manager for secure secret storage
echo   5. Set up Cloud Load Balancing for unified HTTPS entry point
echo   6. Configure Cloud Logging and Cloud Monitoring
echo.
echo View resources: https://console.cloud.google.com/run?project=%PROJECT_ID%

pause
