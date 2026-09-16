#!/bin/bash
set -e

if [ -f /swapfile ]; then
  echo "Swapfile already exists, skipping."
  exit 0
fi

echo "Creating 2G swapfile..."
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
grep -q '/swapfile' /etc/fstab || echo '/swapfile swap swap defaults 0 0' >> /etc/fstab

echo "Swap enabled:"
swapon --show
