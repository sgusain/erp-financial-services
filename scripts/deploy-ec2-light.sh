#!/bin/bash
set -e

echo "Logging into ECR..."
aws ecr get-login-password --region us-east-1 | docker login \
  --username AWS --password-stdin \
  160184161543.dkr.ecr.us-east-1.amazonaws.com

echo "Pulling latest code..."
git -C /home/ec2-user/erp pull

echo "Pulling latest images..."
docker-compose -f /home/ec2-user/erp/docker-compose.prod.yml pull

echo "Starting services..."
docker-compose -f /home/ec2-user/erp/docker-compose.prod.yml up -d

echo "Service status:"
docker-compose -f /home/ec2-user/erp/docker-compose.prod.yml ps
