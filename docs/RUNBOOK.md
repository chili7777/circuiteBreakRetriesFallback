# Runbook

Para instalación/arranque, abrir primero [START-HERE](START-HERE.md). **k6 es opcional**: se acepta evidencia del generador Java incluido o de k6, indicando cuál se usó.

## 1. Verificación sin curl ni k6

Con los dos servicios levantados, desde la raíz del proyecto:

```text
java tools/LabLoad.java check
java tools/LabLoad.java smoke
```

El primer comando comprueba readiness de Orders en 8080 y Payments en 8081. El segundo envía órdenes. JDK 17/21 debe estar en PATH. Los puertos deben estar libres antes de arrancar los servicios.

## 2. Fallos desde Java; mismo comando en Windows/macOS/Linux

Solo contra el simulador local/lab:

```text
java tools/LabLoad.java failure 100 0
java tools/LabLoad.java outage
java tools/LabLoad.java failure 0 0
```

Proveedor lento:
```text
java tools/LabLoad.java failure 0 1200
java tools/LabLoad.java outage
java tools/LabLoad.java failure 0 0
```

El perfil outage presupone 100% de indisponibilidad/timeout, no fallos parciales. Tras recuperar, observar OPEN → HALF_OPEN → CLOSED y volver a probar órdenes.

En Orders, revisar `/actuator/circuitbreakers`, `/actuator/circuitbreakerevents`, `/actuator/retries`, `/actuator/retryevents` y `/actuator/prometheus`. Se pueden abrir con navegador, cliente HTTP del IDE o herramienta permitida. No asumir que ver una anotación demuestra intentos reales.

### Alternativa curl (ejemplos para Bash; no copiar estas comillas a cualquier PowerShell)

```bash
curl -s http://localhost:8080/actuator/health
curl -s -X POST http://localhost:8080/api/orders -H 'Content-Type: application/json' -d '{"orderId":"demo-1","amount":10.25}'
curl -s -X PUT http://localhost:8081/api/payments/lab/failure-mode -H 'Content-Type: application/json' -d '{"failurePercent":100,"delayMs":0}'
```

Para restaurar, enviar `{"failurePercent":0,"delayMs":0}` con PUT al mismo endpoint. En Docker sin JDK local se puede usar ese request en el cliente HTTP del IDE, curl autorizado u otro cliente existente.

## 3. Carga sin k6

```text
java tools/LabLoad.java baseline
java tools/LabLoad.java load
java tools/LabLoad.java stress
java tools/LabLoad.java spike
java tools/LabLoad.java soak
```

Resultados en terminal y `reports/*.csv`. Detalles, fases, denominadores y límites en [START-HERE](START-HERE.md). Java stress usa escalones, k6 usa rampa; no son trazas idénticas. No interpretar un PASS de stress/spike como aprobación automática de los SLO.

### k6 dentro de Docker: no instalar k6 en el host

```text
docker compose --profile tools run --rm -e SCENARIO=smoke k6
docker compose --profile tools run --rm -e SCENARIO=baseline k6
docker compose --profile tools run --rm -e SCENARIO=load k6
```

Usar stress/spike/soak/outage en SCENARIO cuando corresponda. La red Compose y BASE_URL ya están configuradas. Para ejecutar k6 nativo, si ya está instalado, el comando multiplataforma es:

```text
k6 run -e SCENARIO=baseline -e BASE_URL=http://localhost:8080 load/k6.js
```

El SLI es `business_success`, no el promedio de todos los `checks`. Los health checks de arranque no se mezclan con los percentiles de órdenes. `dropped_iterations` debe ser cero para demostrar la tasa solicitada.

## 4. Kubernetes: solo laboratorio autorizado

Requiere clúster, Metrics Server y capacidad. No se obtiene HPA por levantar los JAR ni por usar Compose. Reemplacen `YOUR_REGISTRY` por imágenes accesibles desde SU clúster y usen namespace/contexto de laboratorio, nunca producción.

```text
kubectl apply -f k8s/payment.yaml
kubectl apply -f k8s/orders.yaml
kubectl get deploy,pods,hpa
kubectl get hpa orders-service -w
```

En otra terminal:
```text
kubectl top pods
kubectl describe hpa orders-service
```

El HPA de CPU interpreta utilización respecto de `requests.cpu`. No borren requests para hacerlo escalar. Lens puede mostrar datos equivalentes; no requiere entrar por exec a los contenedores.

Para carga que alcance varias réplicas, usar un generador dentro del clúster contra el Service o un ingreso/balanceador autorizado. `kubectl port-forward service/...` selecciona un pod y no demuestra distribución entre todas las réplicas.

Los controles de fallos de Payments son estado en memoria POR POD. Con dos réplicas, cambiar una mediante un Service no asegura que ambas tengan el mismo modo. Configurar cada pod de forma explícita o realizar la prueba de resiliencia primero en local con una réplica; restablecer todos los modos al terminar. No compartir fallos/cargas entre equipos sin coordinación.

Sin clúster: entregar el diseño, explicar cálculos y declarar que el escalado no se ejecutó. No inventar capturas ni presentar el YAML como prueba.

## 5. Evidencia mínima

Guardar herramienta/versión, comando, escenario, duración/tasa objetivo y realmente enviada, p95/p99, confirmadas/fallbacks/errores, descartes, estado del breaker, réplicas, CPU/memoria y conclusión. Distinguir una ejecución funcional de una prueba completa de capacidad. Carga y microservicios en una misma laptop comparten CPU/RAM.

Referencias oficiales:
- https://grafana.com/docs/k6/latest/set-up/install-k6/
- https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html
- https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/
- https://kubernetes.io/docs/reference/kubectl/generated/kubectl_port-forward/
