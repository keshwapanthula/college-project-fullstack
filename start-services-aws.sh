#!/bin/bash

# ========================================
# Start Services with AWS Profile
# ========================================

SERVICES_DIR="/opt/colorado-health/services"
LOGS_DIR="/opt/colorado-health/logs"
EUREKA_URL="http://172.31.16.175:8761/eureka/"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create logs directory if not exists
mkdir -p "$LOGS_DIR"

echo ""
echo "========================================"
echo "  Starting Services with AWS Profile"
echo "========================================"
echo ""

# Check if AWS endpoints are configured
if [ ! -f "aws-endpoints-config.txt" ]; then
    echo -e "${YELLOW}⚠️  Warning: aws-endpoints-config.txt not found!${NC}"
    echo "   Run ./get-aws-endpoints.sh first to retrieve AWS endpoints"
    echo ""
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Function to check if service is already running
check_service() {
    local service_name=$1
    local pid_file="/tmp/${service_name}.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p "$pid" > /dev/null 2>&1; then
            echo -e "${YELLOW}⚠️  ${service_name} is already running (PID: ${pid})${NC}"
            read -p "Kill and restart? (y/N): " -n 1 -r
            echo ""
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                kill "$pid"
                sleep 2
                rm -f "$pid_file"
            else
                return 1
            fi
        else
            rm -f "$pid_file"
        fi
    fi
    return 0
}

# Stop all running services
stop_all_services() {
    echo -e "${YELLOW}Stopping all running services...${NC}"
    
    for service in college-admin collegeupdates college-notifications; do
        pid_file="/tmp/${service}.pid"
        if [ -f "$pid_file" ]; then
            pid=$(cat "$pid_file")
            if ps -p "$pid" > /dev/null 2>&1; then
                echo "  Stopping ${service} (PID: ${pid})"
                kill "$pid"
                sleep 1
            fi
            rm -f "$pid_file"
        fi
    done
    
    echo ""
}

# Prompt user for AWS endpoints
echo "Enter your AWS endpoints (press Enter to use environment variables):"
echo ""

read -p "RDS MySQL Endpoint [${AWS_RDS_ENDPOINT}]: " rds_endpoint
RDS_ENDPOINT=${rds_endpoint:-$AWS_RDS_ENDPOINT}

read -p "RDS Username [admin]: " rds_username
RDS_USERNAME=${rds_username:-admin}

read -sp "RDS Password: " rds_password
echo ""
RDS_PASSWORD=${rds_password}

read -p "DocumentDB Endpoint [${AWS_DOCDB_ENDPOINT}]: " docdb_endpoint
DOCDB_ENDPOINT=${docdb_endpoint:-$AWS_DOCDB_ENDPOINT}

read -p "DocumentDB Username [docdbadmin]: " docdb_username
DOCDB_USERNAME=${docdb_username:-docdbadmin}

read -sp "DocumentDB Password: " docdb_password
echo ""
DOCDB_PASSWORD=${docdb_password}

read -p "MSK Bootstrap Servers [${AWS_MSK_BOOTSTRAP_SERVERS}]: " msk_servers
MSK_SERVERS=${msk_servers:-$AWS_MSK_BOOTSTRAP_SERVERS}

echo ""

# Ask if user wants to stop existing services
read -p "Stop all existing services before starting? (Y/n): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    stop_all_services
fi

# ===========================================
# Start CollegeAdmin with AWS RDS
# ===========================================
echo ""
echo -e "${YELLOW}Starting CollegeAdmin with AWS RDS...${NC}"

if check_service "college-admin"; then
    java -jar "$SERVICES_DIR/college-admin-0.0.1-SNAPSHOT.jar" \
        --spring.profiles.active=aws \
        --server.port=8082 \
        --spring.datasource.url="jdbc:mysql://${RDS_ENDPOINT}:3306/college_system?useSSL=true&requireSSL=false&serverTimezone=UTC" \
        --spring.datasource.username="${RDS_USERNAME}" \
        --spring.datasource.password="${RDS_PASSWORD}" \
        --eureka.client.service-url.defaultZone="${EUREKA_URL}" \
        --eureka.instance.hostname=$(hostname -I | awk '{print $1}') \
        --eureka.instance.prefer-ip-address=true \
        > "$LOGS_DIR/college-admin-aws.log" 2>&1 &
    
    echo $! > /tmp/college-admin.pid
    echo -e "${GREEN}✅ CollegeAdmin started with PID: $(cat /tmp/college-admin.pid)${NC}"
    echo "   📋 Logs: tail -f $LOGS_DIR/college-admin-aws.log"
else
    echo -e "${RED}❌ Skipped CollegeAdmin${NC}"
fi

sleep 3

# ===========================================
# Start CollegeUpdates with DocumentDB & MSK
# ===========================================
echo ""
echo -e "${YELLOW}Starting CollegeUpdates with DocumentDB & MSK...${NC}"

if check_service "collegeupdates"; then
    java -jar "$SERVICES_DIR/collegeupdates-0.0.1-SNAPSHOT.jar" \
        --spring.profiles.active=aws \
        --server.port=8084 \
        --spring.data.mongodb.uri="mongodb://${DOCDB_USERNAME}:${DOCDB_PASSWORD}@${DOCDB_ENDPOINT}:27017/collegeDB?ssl=true&replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false" \
        --spring.kafka.bootstrap-servers="${MSK_SERVERS}" \
        --eureka.client.service-url.defaultZone="${EUREKA_URL}" \
        --eureka.instance.hostname=$(hostname -I | awk '{print $1}') \
        --eureka.instance.prefer-ip-address=true \
        > "$LOGS_DIR/collegeupdates-aws.log" 2>&1 &
    
    echo $! > /tmp/collegeupdates.pid
    echo -e "${GREEN}✅ CollegeUpdates started with PID: $(cat /tmp/collegeupdates.pid)${NC}"
    echo "   📋 Logs: tail -f $LOGS_DIR/collegeupdates-aws.log"
else
    echo -e "${RED}❌ Skipped CollegeUpdates${NC}"
fi

sleep 3

# ===========================================
# Start CollegeNotifications (Kafka consumer)
# ===========================================
echo ""
echo -e "${YELLOW}Starting CollegeNotifications with MSK...${NC}"

if check_service "college-notifications"; then
    java -jar "$SERVICES_DIR/college-notifications-0.0.1-SNAPSHOT.jar" \
        --spring.profiles.active=aws \
        --server.port=8083 \
        --spring.kafka.bootstrap-servers="${MSK_SERVERS}" \
        --eureka.client.service-url.defaultZone="${EUREKA_URL}" \
        --eureka.instance.hostname=$(hostname -I | awk '{print $1}') \
        --eureka.instance.prefer-ip-address=true \
        > "$LOGS_DIR/college-notifications-aws.log" 2>&1 &
    
    echo $! > /tmp/college-notifications.pid
    echo -e "${GREEN}✅ CollegeNotifications started with PID: $(cat /tmp/college-notifications.pid)${NC}"
    echo "   📋 Logs: tail -f $LOGS_DIR/college-notifications-aws.log"
else
    echo -e "${RED}❌ Skipped CollegeNotifications${NC}"
fi

echo ""
echo -e "${GREEN}========================================"
echo "  All Services Started!"
echo "========================================${NC}"
echo ""
echo "Service Status:"
echo "  - CollegeAdmin:         http://$(hostname -I | awk '{print $1}'):8082"
echo "  - CollegeUpdates:       http://$(hostname -I | awk '{print $1}'):8084"
echo "  - CollegeNotifications: http://$(hostname -I | awk '{print $1}'):8083"
echo "  - Eureka Dashboard:     http://172.31.16.175:8761"
echo ""
echo "Quick Commands:"
echo "  - View logs:     tail -f $LOGS_DIR/college-admin-aws.log"
echo "  - Check health:  curl http://localhost:8082/actuator/health"
echo "  - Stop all:      kill \$(cat /tmp/college-*.pid)"
echo "  - Check Eureka:  curl http://172.31.16.175:8761/eureka/apps"
echo ""
echo "Waiting 10 seconds for services to start..."
sleep 10

# Check service health
echo ""
echo "Checking service health..."
echo ""

for port in 8082 8084 8083; do
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health 2>/dev/null)
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✅ Service on port $port is healthy${NC}"
    else
        echo -e "${RED}❌ Service on port $port is not responding (HTTP $response)${NC}"
    fi
done

echo ""
echo "Done! Check logs if any service failed to start."
