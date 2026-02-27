#!/bin/bash
# ========================================
# AWS Endpoints Configuration Script
# Retrieves all AWS endpoints needed for the application
# ========================================

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  AWS Endpoints Configuration${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo -e "${RED}❌ AWS CLI is not installed!${NC}"
    echo "Install from: https://aws.amazon.com/cli/"
    exit 1
fi

echo -e "${GREEN}✅ AWS CLI installed${NC}"
echo ""

# Set region
REGION="us-east-1"
echo -e "${YELLOW}🌎 Region: $REGION${NC}"
echo ""

# ========================================
# 1. Get RDS MySQL Endpoint
# ========================================
echo -e "${YELLOW}📊 1. Fetching RDS MySQL Endpoint...${NC}"
RDS_ENDPOINT=$(aws rds describe-db-instances \
    --region $REGION \
    --query 'DBInstances[?contains(DBInstanceIdentifier, `college`) && contains(DBInstanceIdentifier, `mysql`)].Endpoint.Address' \
    --output text)

if [ -n "$RDS_ENDPOINT" ]; then
    echo -e "${GREEN}   ✅ RDS MySQL Endpoint: $RDS_ENDPOINT${NC}"
else
    echo -e "${RED}   ❌ RDS MySQL instance not found${NC}"
    echo "   Create with: aws rds create-db-instance --db-instance-identifier college-mysql-db ..."
fi

# ========================================
# 2. Get DocumentDB Endpoint
# ========================================
echo ""
echo -e "${YELLOW}📊 2. Fetching DocumentDB Endpoint...${NC}"
DOCDB_ENDPOINT=$(aws docdb describe-db-clusters \
    --region $REGION \
    --query 'DBClusters[?contains(DBClusterIdentifier, `college`)].Endpoint' \
    --output text)

if [ -n "$DOCDB_ENDPOINT" ]; then
    echo -e "${GREEN}   ✅ DocumentDB Endpoint: $DOCDB_ENDPOINT${NC}"
else
    echo -e "${RED}   ❌ DocumentDB cluster not found${NC}"
    echo "   Create with: aws docdb create-db-cluster --db-cluster-identifier college-docdb-cluster ..."
fi

# ========================================
# 3. Get MSK (Kafka) Bootstrap Servers
# ========================================
echo ""
echo -e "${YELLOW}📊 3. Fetching MSK Kafka Bootstrap Servers...${NC}"
MSK_ARN=$(aws kafka list-clusters \
    --region $REGION \
    --query 'ClusterInfoList[?contains(ClusterName, `CollegeProjectCluster`)].ClusterArn' \
    --output text)

if [ -n "$MSK_ARN" ]; then
    echo -e "${GREEN}   ✅ MSK Cluster ARN: $MSK_ARN${NC}"
    
    # Get Bootstrap Servers (IAM)
    MSK_BOOTSTRAP_IAM=$(aws kafka get-bootstrap-brokers \
        --cluster-arn "$MSK_ARN" \
        --region $REGION \
        --query 'BootstrapBrokerStringSaslIam' \
        --output text)
    
    if [ -n "$MSK_BOOTSTRAP_IAM" ]; then
        echo -e "${GREEN}   ✅ MSK Bootstrap (IAM): $MSK_BOOTSTRAP_IAM${NC}"
    fi
    
    # Get Bootstrap Servers (TLS)
    MSK_BOOTSTRAP_TLS=$(aws kafka get-bootstrap-brokers \
        --cluster-arn "$MSK_ARN" \
        --region $REGION \
        --query 'BootstrapBrokerStringTls' \
        --output text)
    
    if [ -n "$MSK_BOOTSTRAP_TLS" ]; then
        echo -e "${GREEN}   ✅ MSK Bootstrap (TLS): $MSK_BOOTSTRAP_TLS${NC}"
    fi
else
    echo -e "${RED}   ❌ MSK cluster not found${NC}"
    echo "   Create from: https://console.aws.amazon.com/msk/"
fi

# ========================================
# 4. Get ElastiCache Redis Endpoint
# ========================================
echo ""
echo -e "${YELLOW}📊 4. Fetching ElastiCache Redis Endpoint...${NC}"
REDIS_ENDPOINT=$(aws elasticache describe-cache-clusters \
    --region $REGION \
    --query 'CacheClusters[?contains(CacheClusterId, `college`)].CacheNodes[0].Endpoint.Address' \
    --output text)

if [ -n "$REDIS_ENDPOINT" ]; then
    echo -e "${GREEN}   ✅ ElastiCache Redis: $REDIS_ENDPOINT${NC}"
else
    echo -e "${YELLOW}   ⚠️  ElastiCache not found (optional)${NC}"
fi

# ========================================
# 5. Get S3 Bucket Name
# ========================================
echo ""
echo -e "${YELLOW}📊 5. Fetching S3 Bucket...${NC}"
S3_BUCKET=$(aws s3 ls | grep college | awk '{print $3}')

if [ -n "$S3_BUCKET" ]; then
    echo -e "${GREEN}   ✅ S3 Bucket: $S3_BUCKET${NC}"
else
    echo -e "${YELLOW}   ⚠️  S3 bucket not found (optional for Step 3)${NC}"
fi

# ========================================
# 6. Get EC2 Instance Info
# ========================================
echo ""
echo -e "${YELLOW}📊 6. Fetching EC2 Instance Info...${NC}"
EC2_IP=$(aws ec2 describe-instances \
    --region $REGION \
    --filters "Name=tag:Name,Values=*college*" "Name=instance-state-name,Values=running" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text)

if [ -n "$EC2_IP" ]; then
    echo -e "${GREEN}   ✅ EC2 Public IP: $EC2_IP${NC}"
else
    echo -e "${YELLOW}   ⚠️  EC2 instance not found${NC}"
fi

# ========================================
# Generate Configuration Summary
# ========================================
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Configuration Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

cat > aws-endpoints-config.txt << EOF
# AWS Endpoints Configuration
# Generated: $(date)
# Region: $REGION

# ========================================
# 1. RDS MySQL
# ========================================
AWS_RDS_ENDPOINT=$RDS_ENDPOINT
AWS_RDS_PORT=3306
AWS_RDS_DATABASE=college_system
AWS_RDS_USERNAME=admin
AWS_RDS_PASSWORD=<your-password>  # Get from Secrets Manager

# ========================================
# 2. DocumentDB (MongoDB-compatible)
# ========================================
AWS_DOCDB_ENDPOINT=$DOCDB_ENDPOINT
AWS_DOCDB_PORT=27017
AWS_DOCDB_DATABASE=collegeDB
AWS_DOCDB_USERNAME=docdbadmin
AWS_DOCDB_PASSWORD=<your-password>  # Get from Secrets Manager

# ========================================
# 3. MSK (Kafka)
# ========================================
AWS_MSK_BOOTSTRAP_SERVERS=$MSK_BOOTSTRAP_IAM
AWS_MSK_ARN=$MSK_ARN

# ========================================
# 4. ElastiCache Redis (Optional)
# ========================================
AWS_ELASTICACHE_ENDPOINT=$REDIS_ENDPOINT
AWS_ELASTICACHE_PORT=6379

# ========================================
# 5. S3 (Optional)
# ========================================
AWS_S3_BUCKET=$S3_BUCKET

# ========================================
# 6. EC2 Instance
# ========================================
EC2_PUBLIC_IP=$EC2_IP
EUREKA_SERVER_URL=http://$EC2_IP:8761/eureka/

# ========================================
# How to Use:
# ========================================
# 1. Update application-aws.properties with these values
# 2. Store passwords in AWS Secrets Manager
# 3. Grant EC2 IAM role permissions:
#    - AmazonRDSFullAccess
#    - AmazonDocDBFullAccess
#    - AmazonMSKFullAccess
#    - SecretsManagerReadWrite
#    - ElastiCacheFullAccess
# 4. Update security groups to allow traffic between services
# 5. Run with: java -jar app.jar --spring.profiles.active=aws

# ========================================
# AWS CLI Commands
# ========================================

# Get RDS password from Secrets Manager:
aws secretsmanager get-secret-value --secret-id college-project/rds/mysql --query SecretString --output text

# Get DocumentDB password from Secrets Manager:
aws secretsmanager get-secret-value --secret-id college-project/docdb/mongodb --query SecretString --output text

# Test RDS connection:
mysql -h $RDS_ENDPOINT -u admin -p -P 3306

# Test DocumentDB connection:
mongo --ssl --host $DOCDB_ENDPOINT:27017 --username docdbadmin --password <password> --sslCAFile global-bundle.pem

# List Kafka topics:
aws kafka list-nodes --cluster-arn $MSK_ARN

# Create S3 bucket:
aws s3 mb s3://college-project-files-$(date +%s) --region $REGION

EOF

echo -e "${GREEN}✅ Configuration saved to: aws-endpoints-config.txt${NC}"
echo ""
echo -e "${YELLOW}📝 Next Steps:${NC}"
echo "   1. Review aws-endpoints-config.txt"
echo "   2. Update application-aws.properties with these values"
echo "   3. Configure AWS Secrets Manager with passwords"
echo "   4. Update security groups"
echo "   5. Grant IAM permissions to EC2 instance"
echo ""
echo -e "${BLUE}========================================${NC}"
