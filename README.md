# Production Readiness Challenge

**Taller práctico de 3 horas · Spring Boot · Arquitectura hexagonal · Resilience4j · Kubernetes · k6**

## Misión

Una campaña comercial comienza hoy. `orders-service` depende de `payment-service`, un simulador de autorizaciones de pago. El sistema arranca, pero su configuración inicial NO demuestra preparación para producción. Su equipo debe diagnosticar fallos, implementar resiliencia, ajustar capacidad y defender sus decisiones con evidencia.

No se conectan bancos ni se realizan cobros reales. Ejecutar solamente en infraestructura de laboratorio autorizada. Los controles de fallos NO son endpoints para producción.

## Objetivos del ejercicio

Estos números son requisitos ficticios del negocio para el taller, no resultados ya medidos.

| Condición | Objetivo |
|---|---|
| Proveedor sano; 20 solicitudes/s sostenidas | Éxito de negocio ≥99%; p95 <500 ms; p99 <1000 ms |
| Proveedor sano; 80 solicitudes/s tras calentamiento | Mismos objetivos; cero iteraciones descartadas por el generador |
| Pico abrupto de 150 solicitudes/s | Medir degradación y recuperar objetivos dentro de 120 s al volver a 20/s |
| Proveedor caído o lento | Ninguna aprobación falsa; respuesta de fallo controlada; p95 <1000 ms tras activar protecciones |
| Diseño de producción | SLO de disponibilidad de negocio 99,9% en ventana móvil de 30 días; NO certificable en una prueba de tres horas |

Un HTTP 200 no basta: una operación exitosa exige un pago realmente autorizado por el simulador. Un fallback no es un pago aprobado. Los fallos controlados se miden aparte; no se ocultan del SLO de negocio.

## Comenzar

Requisitos: Java 17+ y Maven 3.9+; o Docker con Compose. Para escalado real: clúster Kubernetes de laboratorio con Metrics Server y capacidad disponible. Lens es opcional; se incluyen comandos equivalentes con kubectl.

```bash
git clone https://github.com/chili7777/circuiteBreakRetriesFallback.git
cd circuiteBreakRetriesFallback
mvn clean verify
```

Levantar ambos servicios con Docker:

```bash
docker compose up --build -d payment-service orders-service
```

O, después de `mvn clean package`, en dos terminales:

```bash
java -jar payment-service/target/payment-service-1.0.0.jar
java -jar orders-service/target/orders-service-1.0.0.jar
```

Orders escucha en 8080; Payments en 8081. Instrucciones de comprobación, fallos, carga y despliegue en `docs/RUNBOOK.md`.

## Ruta del taller

| Minutos | Trabajo |
|---|---|
| 0–15 | Arranque, reparto de roles y lectura del contrato |
| 15–40 | Smoke/baseline; definición de SLIs y registro de situación inicial |
| 40–85 | Timeouts, retry, circuit breaker y fallback; pruebas de fallo y recuperación |
| 85–125 | Kubernetes, requests/limits, HPA y observación de pods/CPU/memoria |
| 125–155 | Load/stress/spike y mini-soak; comparar antes/después |
| 155–180 | Demostraciones y defensa de decisiones |

En equipos grandes, trabajo en paralelo: resiliencia, carga/SLIs y Kubernetes. Rotar quién explica; cada integrante debe justificar una decisión.

## Entrega

Crear una rama `team/<nombre>`. Entregar cambios, pruebas, manifiestos, evidencia antes/después y `docs/ENTREGA.md` completado. No basta con agregar anotaciones ni capturas sin interpretación.

- `docs/CHALLENGE.md`: tareas, preguntas y criterios de aceptación.
- `docs/ARCHITECTURE.md`: controlador → puerto de entrada → servicio → puerto de salida → adaptador.
- `docs/RUNBOOK.md`: comandos para ejecutar y diagnosticar.
- `docs/SLO.md`: denominadores, ventanas y significado de cada indicador.
- `docs/ENTREGA.md`: plantilla de resultados y decisiones.
- `load/k6.js`: smoke, baseline, load, stress, spike, soak y outage.
- `k8s/`: despliegues, servicios, recursos y HPA inicial deliberadamente mejorable.

**Restricción de honestidad:** no fabricar mediciones, no reducir la carga sin declararlo, no presentar YAML como evidencia de que el HPA escaló. Cuando no hay clúster, documentar la limitación y entregar el diseño, no un resultado inventado.

## Estado de validación

Consultar `docs/VALIDATION.md` para distinguir pruebas realmente ejecutadas de las que requieren Maven, Docker o Kubernetes en el equipo del participante. La configuración inicial es material didáctico, no una recomendación de producción.
