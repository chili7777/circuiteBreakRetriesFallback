# Runbook

## 1. Verificación rápida

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8081/actuator/health
curl -s -X POST http://localhost:8080/api/orders -H 'Content-Type: application/json' -d '{"orderId":"demo-1","amount":10.25}'
```

## 2. Inyectar fallo controlado

Solo contra el simulador local/lab:

```bash
# 100% fallos
curl -s -X PUT http://localhost:8081/api/payments/lab/failure-mode -H 'Content-Type: application/json' -d '{"failurePercent":100,"delayMs":0}'

# proveedor lento
curl -s -X PUT http://localhost:8081/api/payments/lab/failure-mode -H 'Content-Type: application/json' -d '{"failurePercent":0,"delayMs":1200}'

# recuperar
curl -s -X PUT http://localhost:8081/api/payments/lab/failure-mode -H 'Content-Type: application/json' -d '{"failurePercent":0,"delayMs":0}'
```

Revisar `/actuator/circuitbreakers`, `/actuator/circuitbreakerevents`, `/actuator/retries`, `/actuator/retryevents` y `/actuator/prometheus`.

## 3. k6

```bash
SCENARIO=smoke k6 run load/k6.js
SCENARIO=baseline k6 run load/k6.js
SCENARIO=load k6 run load/k6.js
SCENARIO=stress k6 run load/k6.js
SCENARIO=spike k6 run load/k6.js
SCENARIO=soak SOAK_DURATION=15m k6 run load/k6.js
```

Si Orders está expuesto en otra URL, usar `BASE_URL=http://...`.

## 4. Kubernetes

Primero reemplacen `YOUR_REGISTRY` por imágenes accesibles desde SU clúster.

```bash
kubectl apply -f k8s/payment.yaml
kubectl apply -f k8s/orders.yaml
kubectl get deploy,pods,hpa -w
kubectl top pods
kubectl describe hpa orders-service
```

El HPA de CPU calcula utilización respecto de los `requests.cpu` de los contenedores objetivo. No borren requests para "hacerlo escalar más".

## 5. Evidencia mínima

Guardar: comando exacto, escenario, duración/rate, resultado k6, estado del breaker, réplicas HPA, CPU/memoria y conclusión. No confundir correlación con causalidad.
