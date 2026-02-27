#!/bin/bash
# ========================================
# AWS RDS & DocumentDB Integration Test
# Tests connectivity to RDS MySQL and DocumentDB
# ========================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  AWS Database Connection Tests${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# ========================================
# Test 1: RDS MySQL Connection
# ========================================
echo -e "${YELLOW}Test 1: RDS MySQL Connection${NC}"
echo "Enter RDS endpoint (or press Enter to use from config):"
read -r RDS_ENDPOINT
RDS_ENDPOINT=${RDS_ENDPOINT:-"college-mysql-db.xxxxx.us-east-1.rds.amazonaws.com"}

echo "Enter username (default: admin):"
read -r RDS_USER
RDS_USER=${RDS_USER:-"admin"}

echo "Enter password:"
read -s RDS_PASSWORD

echo ""
echo "Testing connection to $RDS_ENDPOINT..."

# Test using telnet first
if command -v telnet &> /dev/null; then
    echo "Testing port 3306..."
    timeout 5 bash -c "echo > /dev/tcp/$RDS_ENDPOINT/3306" 2>/dev/null && echo -e "${GREEN}✅ Port 3306 is open${NC}" || echo -e "${RED}❌ Port 3306 is closed${NC}"
fi

# Test using mysql client
if command -v mysql &> /dev/null; then
    echo "Attempting MySQL connection..."
    mysql -h "$RDS_ENDPOINT" -u "$RDS_USER" -p"$RDS_PASSWORD" -P 3306 -e "SELECT 'Connection successful!' AS Status, VERSION() AS Version;" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ MySQL connection successful!${NC}"
    else
        echo -e "${RED}❌ MySQL connection failed${NC}"
        echo "Troubleshooting:"
        echo "  1. Check security group allows port 3306 from your IP"
        echo "  2. Verify RDS instance is available"
        echo "  3. Check username and password"
    fi
else
    echo -e "${YELLOW}⚠️  MySQL client not installed. Install with: sudo yum install mysql -y${NC}"
fi

echo ""

# ========================================
# Test 2: DocumentDB Connection
# ========================================
echo -e "${YELLOW}Test 2: DocumentDB Connection${NC}"
echo "Enter DocumentDB endpoint (or press Enter to use from config):"
read -r DOCDB_ENDPOINT
DOCDB_ENDPOINT=${DOCDB_ENDPOINT:-"college-docdb-cluster.cluster-xxxxx.us-east-1.docdb.amazonaws.com"}

echo "Enter username (default: docdbadmin):"
read -r DOCDB_USER
DOCDB_USER=${DOCDB_USER:-"docdbadmin"}

echo "Enter password:"
read -s DOCDB_PASSWORD

echo ""
echo "Testing connection to $DOCDB_ENDPOINT..."

# Test using telnet
if command -v telnet &> /dev/null; then
    echo "Testing port 27017..."
    timeout 5 bash -c "echo > /dev/tcp/$DOCDB_ENDPOINT/27017" 2>/dev/null && echo -e "${GREEN}✅ Port 27017 is open${NC}" || echo -e "${RED}❌ Port 27017 is closed${NC}"
fi

# Download DocumentDB certificate if not exists
if [ ! -f "global-bundle.pem" ]; then
    echo "Downloading DocumentDB SSL certificate..."
    wget -q https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Certificate downloaded${NC}"
    else
        echo -e "${RED}❌ Failed to download certificate${NC}"
    fi
fi

# Test using mongo client (mongo shell v4/v5)
if command -v mongo &> /dev/null; then
    echo "Attempting MongoDB connection..."
    mongo --ssl --host "$DOCDB_ENDPOINT:27017" \
          --username "$DOCDB_USER" \
          --password "$DOCDB_PASSWORD" \
          --sslCAFile global-bundle.pem \
          --eval "db.adminCommand('ping')" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ DocumentDB connection successful!${NC}"
    else
        echo -e "${RED}❌ DocumentDB connection failed${NC}"
        echo "Troubleshooting:"
        echo "  1. Check security group allows port 27017 from your IP"
        echo "  2. Verify DocumentDB cluster is available"
        echo "  3. Check username and password"
        echo "  4. Ensure global-bundle.pem exists"
    fi
else
    # Try mongosh (MongoDB Shell v6+)
    if command -v mongosh &> /dev/null; then
        echo "Attempting MongoDB connection (mongosh)..."
        mongosh "mongodb://$DOCDB_USER:$DOCDB_PASSWORD@$DOCDB_ENDPOINT:27017/?ssl=true&replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false" \
                --tls --tlsCAFile global-bundle.pem \
                --eval "db.adminCommand('ping')" 2>/dev/null
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ DocumentDB connection successful!${NC}"
        else
            echo -e "${RED}❌ DocumentDB connection failed${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  MongoDB client not installed${NC}"
        echo "Install with:"
        echo "  wget https://repo.mongodb.org/yum/amazon/2023/mongodb-org/7.0/x86_64/mongodb-mongosh-2.1.1.x86_64.rpm"
        echo "  sudo yum install mongodb-mongosh-2.1.1.x86_64.rpm -y"
    fi
fi

echo ""

# ========================================
# Test 3: MSK Kafka Connection
# ========================================
echo -e "${YELLOW}Test 3: MSK Kafka Connection${NC}"
echo "Enter MSK bootstrap servers:"
read -r MSK_BOOTSTRAP
MSK_BOOTSTRAP=${MSK_BOOTSTRAP:-"b-1.collegeprojectcluster.xxxxx.kafka.us-east-1.amazonaws.com:9098"}

echo ""
echo "Testing connection to MSK..."

# Extract hostname and port
MSK_HOST=$(echo "$MSK_BOOTSTRAP" | cut -d':' -f1 | cut -d',' -f1)
MSK_PORT=$(echo "$MSK_BOOTSTRAP" | cut -d':' -f2 | cut -d',' -f1)

if command -v telnet &> /dev/null; then
    echo "Testing port $MSK_PORT..."
    timeout 5 bash -c "echo > /dev/tcp/$MSK_HOST/$MSK_PORT" 2>/dev/null && echo -e "${GREEN}✅ Port $MSK_PORT is open${NC}" || echo -e "${RED}❌ Port $MSK_PORT is closed${NC}"
fi

echo -e "${YELLOW}Note: Full MSK testing requires Kafka client tools and IAM authentication${NC}"

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "Save these connection strings if successful:"
echo ""
echo "RDS MySQL JDBC URL:"
echo "jdbc:mysql://$RDS_ENDPOINT:3306/college_system?useSSL=true&serverTimezone=UTC"
echo ""
echo "DocumentDB MongoDB URI:"
echo "mongodb://$DOCDB_USER:<password>@$DOCDB_ENDPOINT:27017/collegeDB?ssl=true&replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false"
echo ""
echo "MSK Bootstrap Servers:"
echo "$MSK_BOOTSTRAP"
echo ""
