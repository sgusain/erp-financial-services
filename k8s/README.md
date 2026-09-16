# ERP Financial Services — Kubernetes Manifests

Kubernetes manifests for deploying the full ERP Financial Services stack:
Postgres, Zookeeper, Kafka, Eureka, Config Server, API Gateway, and five
business microservices (user, account, budget, transaction, notification).

## Directory layout

```
k8s/
├── namespace.yml
├── configmap.yml              # erp-config: shared, non-secret env vars
├── secrets.yml                # erp-secrets: JWT/DB/config-server creds
├── postgres/
├── zookeeper/
├── kafka/
├── eureka-server/
├── config-server/             # also mounts erp-config-files ConfigMap
├── api-gateway/               # deployment, service (LoadBalancer), ingress
├── user-service/
├── account-service/
├── budget-service/
├── transaction-service/
├── notification-service/
└── hpa/                       # HorizontalPodAutoscalers
```

## Prerequisites

- A running Kubernetes cluster (EKS, or any cluster with the containers'
  images reachable) and `kubectl` configured against it.
- An NGINX ingress controller installed if you want `api-gateway/ingress.yml`
  to work (`ingressClassName: nginx`).
- A metrics-server installed for the HPAs under `k8s/hpa/` to compute CPU
  utilization.
- An image pull secret named `ecr-registry-secret` in the
  `erp-financial-services` namespace, since all custom service images are
  pulled from a private ECR repo:

  ```bash
  kubectl create secret docker-registry ecr-registry-secret \
    --namespace erp-financial-services \
    --docker-server=160184161543.dkr.ecr.us-east-1.amazonaws.com \
    --docker-username=AWS \
    --docker-password="$(aws ecr get-login-password --region us-east-1)"
  ```

  ECR passwords expire (~12h), so re-run this periodically or automate it
  (e.g. via a CronJob or an IRSA-based image pull mechanism) for long-lived
  clusters.

- `secrets.yml` ships with placeholder base64 values for local/dev use only.
  For anything beyond a sandbox, regenerate it with real values instead of
  committing decodable secrets — see the comment at the top of that file.

## Deploying

Namespace first, then everything else (order matters for `secrets.yml`/
`configmap.yml` since later manifests reference them):

```bash
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/configmap.yml
kubectl apply -f k8s/secrets.yml

kubectl apply -f k8s/postgres/
kubectl apply -f k8s/zookeeper/
kubectl apply -f k8s/kafka/

kubectl apply -f k8s/eureka-server/
kubectl apply -f k8s/config-server/

kubectl apply -f k8s/user-service/
kubectl apply -f k8s/account-service/
kubectl apply -f k8s/budget-service/
kubectl apply -f k8s/transaction-service/
kubectl apply -f k8s/notification-service/

kubectl apply -f k8s/api-gateway/
kubectl apply -f k8s/hpa/
```

Or, more simply, once the namespace/secret/configmap exist:

```bash
kubectl apply -R -f k8s/
```

## Verifying the deployment

```bash
# Everything in the namespace
kubectl get all -n erp-financial-services

# Pod status / restarts
kubectl get pods -n erp-financial-services -o wide

# Rollout status for a specific deployment
kubectl rollout status deployment/api-gateway -n erp-financial-services

# Describe a pod that isn't becoming Ready
kubectl describe pod <pod-name> -n erp-financial-services

# Confirm services and their ClusterIPs / LoadBalancer external IP
kubectl get svc -n erp-financial-services

# Ingress
kubectl get ingress -n erp-financial-services

# HPA status (current vs target CPU, current replica count)
kubectl get hpa -n erp-financial-services
```

Once `api-gateway`'s LoadBalancer service has an external IP/hostname:

```bash
curl http://<external-ip-or-hostname>/actuator/health
```

## Checking logs

```bash
# Logs for one pod
kubectl logs -n erp-financial-services <pod-name>

# Logs for all pods of a service (follow)
kubectl logs -n erp-financial-services -l app=user-service -f --tail=100

# Previous container's logs, e.g. after a CrashLoopBackOff
kubectl logs -n erp-financial-services <pod-name> --previous
```

## Scaling services

Services with an HPA (`api-gateway`, `user-service`, `transaction-service`)
scale automatically on CPU; manual scaling still works but the HPA may
override it on its next sync:

```bash
kubectl scale deployment/account-service -n erp-financial-services --replicas=3
```

To change an HPA's bounds, edit the corresponding file under `k8s/hpa/` and
re-apply, or patch directly:

```bash
kubectl patch hpa transaction-service-hpa -n erp-financial-services \
  --type merge -p '{"spec":{"maxReplicas":8}}'
```

## Updating images

CI/CD pushes new images to ECR tagged `latest` (and by git SHA). Since pods
won't repull `latest` on their own, trigger a rollout after a new push:

```bash
kubectl rollout restart deployment/api-gateway -n erp-financial-services
```

To pin a specific build instead of floating on `latest`:

```bash
kubectl set image deployment/api-gateway \
  api-gateway=160184161543.dkr.ecr.us-east-1.amazonaws.com/erp-api-gateway:<git-sha> \
  -n erp-financial-services
```

Watch the rollout and roll back if it fails:

```bash
kubectl rollout status deployment/api-gateway -n erp-financial-services
kubectl rollout undo deployment/api-gateway -n erp-financial-services
```
