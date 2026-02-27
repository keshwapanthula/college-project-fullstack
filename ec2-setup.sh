#!/bin/bash

# ========================================
# EC2 Setup Script - First Time Setup
# ========================================
# Run this script on your EC2 instance to create
# the project directory structure and all scripts

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo "========================================"
echo "  EC2 Project Setup - College Project"
echo "========================================"
echo ""

# Create directory structure
echo -e "${YELLOW}Creating directory structure...${NC}"
sudo mkdir -p /opt/colorado-health/college-project-fullstack
sudo mkdir -p /opt/colorado-health/services
sudo mkdir -p /opt/colorado-health/logs
sudo chown -R ec2-user:ec2-user /opt/colorado-health
echo -e "${GREEN}✅ Directories created${NC}"

# Set working directory
cd /opt/colorado-health/college-project-fullstack

# ========================================
# Create get-aws-endpoints.sh
# ========================================
echo -e "${YELLOW}Creating get-aws-endpoints.sh...${NC}"
cat > get-aws-endpoints.sh << 'GETENDPOINTS'
#!/bin/bash

echo ""
echo "========================================"
echo "  Getting AWS Endpoints..."
echo "========================================"
echo ""

REGION="us-east-1"

# Get RDS MySQL Endpoint
echo "📊 RDS MySQL Instances:"
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --region $REGION \
  --query "DBInstances[?Engine=='mysql'].Endpoint.Address" \
  --output text 2>/dev/null | head -1)

if [ -n "$RDS_ENDPOINT" ]; then
  echo "  ✅ RDS Endpoint: $RDS_ENDPOINT"
else
  echo "  ⚠️  No RDS MySQL instances found"
  RDS_ENDPOINT="REPLACE-WITH-RDS-ENDPOINT"
fi

# Get DocumentDB Endpoint
echo ""
echo "🍃 DocumentDB Clusters:"
DOCDB_ENDPOINT=$(aws docdb describe-db-clusters \
  --region $REGION \
  --query "DBClusters[*].Endpoint" \
  --output text 2>/dev/null | head -1)

if [ -n "$DOCDB_ENDPOINT" ]; then
  echo "  ✅ DocumentDB Endpoint: $DOCDB_ENDPOINT"
else
  echo "  ⚠️  No DocumentDB clusters found"
  DOCDB_ENDPOINT="REPLACE-WITH-DOCDB-ENDPOINT"
fi

# Get MSK Bootstrap Servers
echo ""
echo "📨 MSK Kafka Clusters:"
MSK_ARN="arn:aws:kafka:us-east-1:211125397666:cluster/CollegeProjectCluster/07dfede2-6316-40de-a1ad-760de40257c9-s3"
MSK_BOOTSTRAP=$(aws kafka get-bootstrap-brokers \
  --cluster-arn "$MSK_ARN" \
  --region $REGION \
  --query "BootstrapBrokerStringSaslIam" \
  --output text 2>/dev/null)

if [ -n "$MSK_BOOTSTRAP" ] && [ "$MSK_BOOTSTRAP" != "None" ]; then
  echo "  ✅ MSK Bootstrap Servers: $MSK_BOOTSTRAP"
else
  echo "  ⚠️  Could not get MSK bootstrap servers"
  MSK_BOOTSTRAP="REPLACE-WITH-MSK-BOOTSTRAP-SERVERS"
fi

# Get EC2 Private IP
EC2_IP=$(hostname -I | awk '{print $1}')
EC2_PUBLIC=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null)

# Write configuration file
cat > aws-endpoints-config.txt << EOF
# AWS Endpoints Configuration
# Generated: $(date)

AWS_REGION=$REGION
AWS_RDS_ENDPOINT=$RDS_ENDPOINT
AWS_RDS_PORT=3306
AWS_RDS_DATABASE=college_system

AWS_DOCDB_ENDPOINT=$DOCDB_ENDPOINT
AWS_DOCDB_PORT=27017
AWS_DOCDB_DATABASE=collegeDB

AWS_MSK_BOOTSTRAP_SERVERS=$MSK_BOOTSTRAP

EC2_PRIVATE_IP=$EC2_IP
EC2_PUBLIC_IP=$EC2_PUBLIC
EOF

echo ""
echo "========================================"
echo "  Configuration saved to: aws-endpoints-config.txt"
echo "========================================"
cat aws-endpoints-config.txt
GETENDPOINTS

chmod +x get-aws-endpoints.sh
echo -e "${GREEN}✅ get-aws-endpoints.sh created${NC}"

# ========================================
# Create test-aws-connectivity.sh
# ========================================
echo -e "${YELLOW}Creating test-aws-connectivity.sh...${NC}"
cat > test-aws-connectivity.sh << 'TESTCONN'
#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo "========================================"
echo "  Testing AWS Connectivity"
echo "========================================"
echo ""

# Load config if exists
if [ -f "aws-endpoints-config.txt" ]; then
  source aws-endpoints-config.txt
  echo -e "${GREEN}✅ Loaded endpoints from aws-endpoints-config.txt${NC}"
fi

# Override with prompts if needed
if [ -z "$AWS_RDS_ENDPOINT" ] || [ "$AWS_RDS_ENDPOINT" = "REPLACE-WITH-RDS-ENDPOINT" ]; then
  read -p "Enter RDS MySQL Endpoint: " AWS_RDS_ENDPOINT
fi

if [ -z "$AWS_DOCDB_ENDPOINT" ] || [ "$AWS_DOCDB_ENDPOINT" = "REPLACE-WITH-DOCDB-ENDPOINT" ]; then
  read -p "Enter DocumentDB Endpoint: " AWS_DOCDB_ENDPOINT
fi

# Test RDS MySQL connectivity
echo ""
echo "Testing RDS MySQL ($AWS_RDS_ENDPOINT:3306)..."
if timeout 5 bash -c "echo >/dev/tcp/$AWS_RDS_ENDPOINT/3306" 2>/dev/null; then
  echo -e "${GREEN}✅ RDS MySQL port 3306 is REACHABLE${NC}"
else
  echo -e "${RED}❌ RDS MySQL port 3306 BLOCKED - Check security groups!${NC}"
  echo "   Fix: Allow EC2 security group inbound on port 3306"
fi

# Test DocumentDB connectivity
echo ""
echo "Testing DocumentDB ($AWS_DOCDB_ENDPOINT:27017)..."
if timeout 5 bash -c "echo >/dev/tcp/$AWS_DOCDB_ENDPOINT/27017" 2>/dev/null; then
  echo -e "${GREEN}✅ DocumentDB port 27017 is REACHABLE${NC}"
else
  echo -e "${RED}❌ DocumentDB port 27017 BLOCKED - Check security groups!${NC}"
  echo "   Fix: Allow EC2 security group inbound on port 27017"
fi

# Test MSK Kafka connectivity
if [ -n "$AWS_MSK_BOOTSTRAP_SERVERS" ] && [ "$AWS_MSK_BOOTSTRAP_SERVERS" != "REPLACE-WITH-MSK-BOOTSTRAP-SERVERS" ]; then
  MSK_HOST=$(echo $AWS_MSK_BOOTSTRAP_SERVERS | cut -d',' -f1 | cut -d':' -f1)
  MSK_PORT=$(echo $AWS_MSK_BOOTSTRAP_SERVERS | cut -d',' -f1 | cut -d':' -f2)
  echo ""
  echo "Testing MSK Kafka ($MSK_HOST:$MSK_PORT)..."
  if timeout 5 bash -c "echo >/dev/tcp/$MSK_HOST/$MSK_PORT" 2>/dev/null; then
    echo -e "${GREEN}✅ MSK Kafka port $MSK_PORT is REACHABLE${NC}"
  else
    echo -e "${RED}❌ MSK Kafka port $MSK_PORT BLOCKED - Check security groups!${NC}"
  fi
fi

echo ""
echo "========================================"
TESTCONN

chmod +x test-aws-connectivity.sh
echo -e "${GREEN}✅ test-aws-connectivity.sh created${NC}"

# ========================================
# Create start-services-aws.sh
# ========================================
echo -e "${YELLOW}Creating start-services-aws.sh...${NC}"
cat > start-services-aws.sh << 'STARTSERVICES'
#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SERVICES_DIR="/opt/colorado-health/services"
LOGS_DIR="/opt/colorado-health/logs"
EUREKA_URL="http://172.31.16.175:8761/eureka/"

echo ""
echo "========================================"
echo "  Starting Services with AWS Profile"
echo "========================================"
echo ""

# Load endpoints config if exists
if [ -f "aws-endpoints-config.txt" ]; then
  source aws-endpoints-config.txt
  echo -e "${GREEN}✅ Loaded endpoints config${NC}"
else
  echo -e "${YELLOW}⚠️  aws-endpoints-config.txt not found, run ./get-aws-endpoints.sh first${NC}"
fi

# Prompt for any missing values
[ -z "$AWS_RDS_ENDPOINT" ] || [[ "$AWS_RDS_ENDPOINT" == REPLACE* ]] && read -p "RDS Endpoint: " AWS_RDS_ENDPOINT
[ -z "$AWS_DOCDB_ENDPOINT" ] || [[ "$AWS_DOCDB_ENDPOINT" == REPLACE* ]] && read -p "DocumentDB Endpoint: " AWS_DOCDB_ENDPOINT

read -p "RDS Username [admin]: " RDS_USER; RDS_USER=${RDS_USER:-admin}
read -sp "RDS Password: " RDS_PASS; echo ""
read -p "DocDB Username [docdbadmin]: " DOCDB_USER; DOCDB_USER=${DOCDB_USER:-docdbadmin}
read -sp "DocDB Password: " DOCDB_PASS; echo ""

MY_IP=$(hostname -I | awk '{print $1}')

# Check if JARs exist
if [ ! -d "$SERVICES_DIR" ] || [ -z "$(ls -A $SERVICES_DIR/*.jar 2>/dev/null)" ]; then
  echo -e "${RED}❌ No JARs found in $SERVICES_DIR${NC}"
  echo ""
  echo "Copy JARs from Windows with (run on Windows PowerShell):"
  echo "  scp -i your-key.pem CollegeAdmin\\target\\*.jar ec2-user@$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):$SERVICES_DIR/"
  echo "  scp -i your-key.pem CollegeUpdates\\target\\*.jar ec2-user@$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):$SERVICES_DIR/"
  echo "  scp -i your-key.pem CollegeNotifications\\target\\*.jar ec2-user@$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):$SERVICES_DIR/"
  exit 1
fi

mkdir -p "$LOGS_DIR"

# Stop existing
echo -e "${YELLOW}Stopping any existing services...${NC}"
pkill -f "college-admin" 2>/dev/null; pkill -f "collegeupdates" 2>/dev/null; pkill -f "college-notifications" 2>/dev/null
sleep 2

# Find JARs
ADMIN_JAR=$(ls $SERVICES_DIR/college-admin*.jar 2>/dev/null | head -1)
UPDATES_JAR=$(ls $SERVICES_DIR/collegeupdates*.jar $SERVICES_DIR/demo-college-update*.jar 2>/dev/null | head -1)
NOTIFY_JAR=$(ls $SERVICES_DIR/college-notifications*.jar 2>/dev/null | head -1)

# Start CollegeAdmin
if [ -n "$ADMIN_JAR" ]; then
  echo -e "${YELLOW}Starting CollegeAdmin...${NC}"
  java -jar "$ADMIN_JAR" \
    --spring.profiles.active=aws \
    --server.port=8082 \
    --spring.datasource.url="jdbc:mysql://${AWS_RDS_ENDPOINT}:3306/college_system?useSSL=true&requireSSL=false&serverTimezone=UTC" \
    --spring.datasource.username="$RDS_USER" \
    --spring.datasource.password="$RDS_PASS" \
    --eureka.client.service-url.defaultZone="$EUREKA_URL" \
    --eureka.instance.hostname="$MY_IP" \
    --eureka.instance.prefer-ip-address=true \
    > "$LOGS_DIR/college-admin-aws.log" 2>&1 &
  echo $! > /tmp/college-admin.pid
  echo -e "${GREEN}✅ CollegeAdmin PID: $!${NC}"
else
  echo -e "${YELLOW}⚠️  CollegeAdmin JAR not found in $SERVICES_DIR${NC}"
fi

sleep 3

# Start CollegeUpdates
if [ -n "$UPDATES_JAR" ]; then
  echo -e "${YELLOW}Starting CollegeUpdates...${NC}"
  java -jar "$UPDATES_JAR" \
    --spring.profiles.active=aws \
    --server.port=8084 \
    --spring.data.mongodb.uri="mongodb://${DOCDB_USER}:${DOCDB_PASS}@${AWS_DOCDB_ENDPOINT}:27017/collegeDB?ssl=true&replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false" \
    --spring.kafka.bootstrap-servers="${AWS_MSK_BOOTSTRAP_SERVERS}" \
    --eureka.client.service-url.defaultZone="$EUREKA_URL" \
    --eureka.instance.hostname="$MY_IP" \
    --eureka.instance.prefer-ip-address=true \
    > "$LOGS_DIR/collegeupdates-aws.log" 2>&1 &
  echo $! > /tmp/collegeupdates.pid
  echo -e "${GREEN}✅ CollegeUpdates PID: $!${NC}"
else
  echo -e "${YELLOW}⚠️  CollegeUpdates JAR not found${NC}"
fi

sleep 3

# Start CollegeNotifications
if [ -n "$NOTIFY_JAR" ]; then
  echo -e "${YELLOW}Starting CollegeNotifications...${NC}"
  java -jar "$NOTIFY_JAR" \
    --spring.profiles.active=aws \
    --server.port=8083 \
    --spring.kafka.bootstrap-servers="${AWS_MSK_BOOTSTRAP_SERVERS}" \
    --eureka.client.service-url.defaultZone="$EUREKA_URL" \
    --eureka.instance.hostname="$MY_IP" \
    --eureka.instance.prefer-ip-address=true \
    > "$LOGS_DIR/college-notifications-aws.log" 2>&1 &
  echo $! > /tmp/college-notifications.pid
  echo -e "${GREEN}✅ CollegeNotifications PID: $!${NC}"
fi

echo ""
echo -e "${GREEN}========================================"
echo "  Services Started!"
echo "========================================${NC}"
echo ""
echo "  Eureka:          http://172.31.16.175:8761"
echo "  CollegeAdmin:    http://$MY_IP:8082"
echo "  CollegeUpdates:  http://$MY_IP:8084"
echo ""
echo "Logs:  tail -f $LOGS_DIR/*.log"
echo "Stop:  pkill -f 'college-admin'; pkill -f 'collegeupdates'; pkill -f 'college-notifications'"
STARTSERVICES

chmod +x start-services-aws.sh
echo -e "${GREEN}✅ start-services-aws.sh created${NC}"

# ========================================
# Done
# ========================================
echo ""
echo -e "${GREEN}========================================"
echo "  EC2 Setup Complete!"
echo "========================================"
echo ""
echo "Directories:"
echo "  Project:  /opt/colorado-health/college-project-fullstack"
echo "  Services: /opt/colorado-health/services  (put JARs here)"
echo "  Logs:     /opt/colorado-health/logs"
echo ""
echo "Scripts ready:"
echo "  ./get-aws-endpoints.sh       - Retrieve AWS RDS/MSK endpoints"
echo "  ./test-aws-connectivity.sh   - Test DB connectivity"
echo "  ./start-services-aws.sh      - Start all services"
echo ""
echo "Next Steps:"
echo "  1. Copy JARs from Windows (see below)"
echo "  2. Run: ./get-aws-endpoints.sh"
echo "  3. Run: ./test-aws-connectivity.sh"
echo "  4. Run: ./start-services-aws.sh"
echo ""
echo "Copy JARs from Windows (PowerShell):"
EC2_PUB=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null)
echo "  scp -i your-key.pem CollegeAdmin\\target\\*.jar ec2-user@${EC2_PUB}:/opt/colorado-health/services/"
echo "  scp -i your-key.pem CollegeUpdates\\target\\*.jar ec2-user@${EC2_PUB}:/opt/colorado-health/services/"
echo "  scp -i your-key.pem CollegeNotifications\\target\\*.jar ec2-user@${EC2_PUB}:/opt/colorado-health/services/"
echo ""
