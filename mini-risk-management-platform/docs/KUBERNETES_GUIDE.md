# Kubernetes Guide

## Why Kubernetes Exists

Docker can run containers on one machine. Production systems need scheduling, self-healing, service discovery, rollout control, config injection, secret handling, persistent storage, autoscaling, and load balancing across many machines. Kubernetes provides those control loops.

## Objects In This Project

| Object | Where | Why |
| --- | --- | --- |
| `Namespace` | `k8s/base/namespace.yaml` | Isolates all lab resources under `mini-risk`. |
| `ConfigMap` | `k8s/base/configmap.yaml` | Non-secret runtime configuration. |
| `Secret` | `k8s/base/secret.yaml` | Passwords and admin credentials. |
| `PersistentVolume` | `k8s/data/*.yaml` | Cluster storage backing for stateful services. |
| `PersistentVolumeClaim` | `k8s/data/*.yaml` | Namespaced storage request consumed by Pods. |
| `Deployment` | `k8s/apps/*.yaml` | Desired state and rollout controller for Pods. |
| `Service` | `k8s/apps/*.yaml` | Stable virtual IP and DNS name for Pods. |
| `Ingress` | `k8s/base/ingress.yaml` | External HTTP routing into `api-gateway`. |
| `HorizontalPodAutoscaler` | `k8s/apps/*.yaml` | Scales stateless workloads using CPU utilization. |

## Deployment Internals

When you run:

```powershell
kubectl apply -k k8s
```

Kubernetes stores desired state in the API server. Controllers compare desired state with actual state:

- Deployment controller creates ReplicaSets.
- ReplicaSet controller creates Pods.
- Scheduler assigns Pods to Nodes.
- Kubelet on each Node starts containers through the container runtime.
- EndpointSlice controller tracks Pod IPs behind Services.
- kube-proxy programs routing rules for Service traffic.

## Probes

Liveness probe asks: should this container be restarted?

Readiness probe asks: should this Pod receive traffic?

This project uses:

```text
/actuator/health/liveness
/actuator/health/readiness
```

Bad liveness probes cause restart loops. Bad readiness probes cause zero endpoints and timeouts.

## Resource Requests And Limits

Requests reserve scheduling capacity. Limits cap usage.

Example:

```yaml
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: "1"
    memory: 768Mi
```

Interview mental model:

- CPU request: placement promise.
- CPU limit: throttling boundary.
- Memory request: placement promise.
- Memory limit: kill boundary.

## ConfigMap Versus Secret

ConfigMap is for non-sensitive config such as service URLs and topic names.

Secret is for sensitive config such as passwords. Kubernetes Secrets are base64-encoded by default, not automatically encrypted unless the cluster enables encryption at rest.

## Service Discovery

Inside the namespace, a Pod can call:

```text
http://risk-service:8082
```

CoreDNS resolves `risk-service` to the ClusterIP Service, then kube-proxy routes traffic to a ready backend Pod.

## Storage

`PersistentVolume` is cluster storage. `PersistentVolumeClaim` is a namespaced request for storage. A Pod mounts the claim.

In this lab, PVs use `hostPath` for local learning. Production clusters usually use cloud disks or a CSI driver.

## Commands

```powershell
kubectl -n mini-risk get pods -o wide
kubectl -n mini-risk describe pod <pod-name>
kubectl -n mini-risk logs deployment/order-service
kubectl -n mini-risk get endpointslice
kubectl -n mini-risk get events --sort-by=.lastTimestamp
kubectl -n mini-risk top pods
kubectl -n mini-risk rollout status deployment/risk-service
kubectl -n mini-risk scale deployment/order-service --replicas=4
```

## Production Tradeoffs

- Deployments are simple for stateless apps; StatefulSets are usually better for real Kafka/PostgreSQL.
- HPAs need metrics-server and good resource requests.
- Ingress centralizes routing but becomes an important failure domain.
- Secrets need external secret management in serious environments.
- Local `hostPath` PVs are useful for labs but unsafe for multi-node production.

