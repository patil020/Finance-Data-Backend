# Kubernetes Deployment Guide for Finance App

## Prerequisites

1. Kubernetes cluster running (v1.20+)
2. kubectl CLI installed and configured
3. Docker images built and pushed to registry:
   - finance-app/eureka-server:latest
   - finance-app/config-server:latest
   - finance-app/auth-service:latest
   - finance-app/user-service:latest
   - finance-app/record-service:latest
   - finance-app/dashboard-service:latest
   - finance-app/api-gateway:latest

## Deployment Order

The services must be deployed in a specific order to ensure dependencies are available:

### 1. Infrastructure Layer (Deploy First)
```bash
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
kubectl apply -f pvc.yaml
```

### 2. Data & Messaging Layer (Deploy Second)
```bash
kubectl apply -f kafka-zookeeper.yaml
kubectl apply -f redis.yaml
kubectl apply -f mysql.yaml
```

Wait for all pods to be ready:
```bash
kubectl wait --for=condition=ready pod -l app=zookeeper -n finance-app --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n finance-app --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n finance-app --timeout=300s
kubectl wait --for=condition=ready pod -l app=mysql-auth -n finance-app --timeout=300s
kubectl wait --for=condition=ready pod -l app=mysql-user -n finance-app --timeout=300s
kubectl wait --for=condition=ready pod -l app=mysql-record -n finance-app --timeout=300s
```

### 3. Service Discovery & Configuration (Deploy Third)
```bash
kubectl apply -f eureka-server.yaml
kubectl apply -f config-server.yaml
```

Wait for services to be ready:
```bash
kubectl wait --for=condition=ready pod -l app=eureka-server -n finance-app --timeout=300s
kubectl wait --for=condition=ready pod -l app=config-server -n finance-app --timeout=300s
```

### 4. Microservices (Deploy Fourth)
```bash
kubectl apply -f auth-service.yaml
kubectl apply -f user-service.yaml
kubectl apply -f record-service.yaml
kubectl apply -f dashboard-service.yaml
kubectl apply -f api-gateway.yaml
```

### 5. Ingress & Networking (Deploy Last)
```bash
kubectl apply -f ingress.yaml
```

## One-Command Deployment (Manual Ordering)

To deploy everything at once with proper ordering:

```bash
#!/bin/bash

echo "=== Deploying Finance App to Kubernetes ==="

echo "1. Setting up infrastructure..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/pvc.yaml

echo "2. Deploying data and messaging layer..."
kubectl apply -f k8s/kafka-zookeeper.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/mysql.yaml

echo "3. Waiting for data layer to be ready..."
kubectl wait --for=condition=ready pod -l app=zookeeper -n finance-app --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n finance-app --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n finance-app --timeout=300s
kubectl wait --for=condition=ready pod -l app=mysql-auth,mysql-user,mysql-record -n finance-app --timeout=300s

echo "4. Deploying service discovery..."
kubectl apply -f k8s/eureka-server.yaml
kubectl apply -f k8s/config-server.yaml

echo "5. Waiting for service discovery..."
kubectl wait --for=condition=ready pod -l app=eureka-server -n finance-app --timeout=300s
kubectl wait --for=condition=ready pod -l app=config-server -n finance-app --timeout=300s

echo "6. Deploying microservices..."
kubectl apply -f k8s/auth-service.yaml
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/record-service.yaml
kubectl apply -f k8s/dashboard-service.yaml
kubectl apply -f k8s/api-gateway.yaml

echo "7. Deploying ingress..."
kubectl apply -f k8s/ingress.yaml

echo "=== Deployment Complete ==="
```

## Monitoring Deployments

Check deployment status:
```bash
kubectl get deployments -n finance-app
kubectl get pods -n finance-app
kubectl get svc -n finance-app
```

View pod logs:
```bash
kubectl logs -f <pod-name> -n finance-app
```

Port Forwarding (for local testing):
```bash
# API Gateway
kubectl port-forward svc/api-gateway 8080:8080 -n finance-app

# Eureka Dashboard
kubectl port-forward svc/eureka-server 8761:8761 -n finance-app

# Config Server
kubectl port-forward svc/config-server 8888:8888 -n finance-app
```

## Environment Variables

Update `secrets.yaml` before deploying:
- `SPRING_DATASOURCE_PASSWORD`: Change to a secure password
- `MYSQL_ROOT_PASSWORD`: Change to a secure password
- `APP_JWT_SECRET`: Change to a secure JWT secret

## Scaling Services

Scale individual services:
```bash
kubectl scale deployment auth-service -n finance-app --replicas=3
kubectl scale deployment user-service -n finance-app --replicas=3
kubectl scale deployment record-service -n finance-app --replicas=3
```

## Cleanup

Delete all resources:
```bash
kubectl delete namespace finance-app
```

## Notes

- Default image pull policy is set to `IfNotPresent`. Change to `Always` for production if using latest tags
- PersistentVolumes must be provisioned before MySQL pods start
- Services are configured with health checks (liveness and readiness probes)
- Memory and CPU limits are set conservatively; adjust based on load testing
- Ingress requires an Ingress Controller (nginx, traefik, etc.) to be installed
