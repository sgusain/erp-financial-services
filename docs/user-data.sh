#!/bin/bash
yum update -y

# t3.small (2GB RAM) can't hold all 11 containers' JVM/Kafka heaps without
# swap; without this the box thrashes and burns its CPU credit balance until
# even sshd stops responding. See scripts/setup-swap.sh for the same steps
# applied to an already-running instance.
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile swap swap defaults 0 0' >> /etc/fstab
fi

yum install -y docker git
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

aws ecr get-login-password --region us-east-1 | docker login \
  --username AWS --password-stdin \
  160184161543.dkr.ecr.us-east-1.amazonaws.com

git clone https://github.com/sgusain/erp-financial-services.git /home/ec2-user/erp

bash /home/ec2-user/erp/scripts/deploy-ec2-light.sh
