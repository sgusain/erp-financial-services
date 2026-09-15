#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")/.."

aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  160184161543.dkr.ecr.us-east-1.amazonaws.com

git pull
docker-compose pull
docker-compose up -d
docker-compose ps
