# Production Readiness Challenge

**Taller práctico de 3 horas · Spring Boot · Hexagonal · Resilience4j · Kubernetes · Carga con Java o k6**

## Primero: NO necesitan instalar k6

**Empiecen por [START-HERE](docs/START-HERE.md).** Hay dos rutas:

| Entorno que ya tienen | Cómo participan |
|---|---|
| JDK 17/21 + Maven 3.9, también puede ser Maven del IDE | Microservicios + generador Java incluido. Sin Docker ni k6. |
| Docker autorizado y activo con Compose v2 | Compilación, microservicios y k6 dentro de contenedores. |
| Equipo restringido sin ninguna ruta | Trabajar en pareja en una máquina habilitada. No instalar sin autorización. |

Windows y Linux: **build y pruebas HTTP reales aprobados** en [esta ejecución de GitHub Actions](https://github.com/chili7777/circuiteBreakRetriesFallback/actions/runs/34609037951). Esto no garantiza permisos, red corporativa ni capacidad de cada laptop. Alcance en [VALIDATION](docs/VALIDATION.md).

## Misión

Una campaña comercial comienza hoy. `orders-service` depende de `payment-service`, un simulador de autorizaciones. El equipo debe diagnosticar fallos, implementar mejoras de resiliencia, ajustar capacidad y defender GO/NO-GO con evidencia.

No hay bancos conectados ni cobros reales. Solo infraestructura de laboratorio autorizada. Los controles de fallos NO son endpoints para producción.

## Objetivos del ejercicio

Son requisitos ficticios del negocio, no resultados ya medidos.

| Condición | Objetivo |
|---|---|
| Proveedor sano; 20 solicitudes/s sostenidas | Éxito de negocio ≥99%; p95 <500 ms; p99 <1000 ms |
| Proveedor sano; 80 solicitudes/s tras calentamiento | Mismos objetivos; cero solicitudes descartadas por el generador |
| Pico abrupto de 150 solicitudes/s | Medir degradación y recuperar objetivos dentro de 120 s al volver a 20/s |
| Proveedor caído o lento | Ninguna aprobación falsa; fallo controlado; p95 <1000 ms tras activar protecciones |
| Diseño de producción | SLO de disponibilidad de negocio 99,9% en ventana móvil de 30 días; NO certificable en tres horas |

HTTP 200 no basta: se necesita una autorización real del simulador. El fallback NO cuenta como compra exitosa. Los fallos de negocio y los descartes del generador no se esconden.

## Ruta rápida con Java, sin k6

```text
git clone https://github.com/chili7777/circuiteBreakRetriesFallback.git
cd circuiteBreakRetriesFallback
mvn clean verify
```

Maven también se puede ejecutar desde el IDE. Abrir el `pom.xml` raíz y usar JDK 17/21. Si ya clonaron y aún no lo modificaron: `git pull --ff-only`.

Arrancar cada JAR en una terminal distinta:
```text
java -jar payment-service/target/payment-service-1.0.0.jar
```
```text
java -jar orders-service/target/orders-service-1.0.0.jar
```

En una tercera terminal, desde la raíz:
```text
java tools/LabLoad.java check
java tools/LabLoad.java smoke
java tools/LabLoad.java baseline
```

También hay `load`, `stress`, `spike`, `soak` y `outage`. El runner escribe CSV en `reports/`. Es una herramienta didáctica propia, no un reemplazo equivalente a k6 para certificación de rendimiento.

## Ruta Docker, sin Java/Maven/k6 instalados en el host

Docker/Compose sí deben estar disponibles y autorizados. La primera vez necesita descargar imágenes y dependencias:

```text
docker compose up --build -d payment-service orders-service
docker compose --profile tools run --rm -e SCENARIO=smoke k6
```

Los Dockerfiles compilan el código; no necesitan JAR preexistente. Los comandos son iguales en PowerShell, CMD y shell. La ruta Java ya tiene evidencia Windows/Linux; la ruta Docker no está incluida en esa validación. Más instrucciones en [START-HERE](docs/START-HERE.md).

## Ruta del taller

| Minutos | Trabajo |
|---|---|
| 0–15 | Arranque y roles; leer contrato |
| 15–40 | Smoke/baseline; SLIs y situación inicial |
| 40–85 | Timeouts, retry, breaker y fallback; fallo y recuperación |
| 85–125 | Kubernetes, recursos, HPA y observación |
| 125–155 | Load/stress/spike y mini-soak; comparar |
| 155–180 | Demostraciones y defensa GO/NO-GO |

Roles en paralelo: resiliencia, carga/SLIs y Kubernetes. Cada integrante debe justificar una decisión. **El HPA real requiere un clúster de laboratorio con Metrics Server**; Java y Compose solos no lo demuestran. Usar clúster compartido autorizado o declarar que solo se validó el diseño. Coordinar cargas por equipo.

## Entrega

Rama `team/<nombre>`, cambios, pruebas, manifiestos, evidencia antes/después y `docs/ENTREGA.md` completado. Se acepta Java o k6: no se penaliza no tener k6 instalado. No basta con agregar anotaciones ni capturas sin interpretación.

- [START-HERE](docs/START-HERE.md): rutas de ejecución y requisitos.
- [CHALLENGE](docs/CHALLENGE.md): tareas, preguntas y criterios.
- [ARCHITECTURE](docs/ARCHITECTURE.md): controlador → puerto entrada → servicio → puerto salida → adaptador.
- [RUNBOOK](docs/RUNBOOK.md): fallos, diagnóstico y Kubernetes.
- [SLO](docs/SLO.md): indicadores y objetivos.
- [ENTREGA](docs/ENTREGA.md): resultados y decisiones.
- `tools/LabLoad.java`: carga con el JDK, sin bibliotecas externas.
- `load/k6.js`: alternativa k6, también disponible por Compose.
- `k8s/`: despliegues, servicios, recursos y HPA inicial mejorable.

**Honestidad:** no fabricar mediciones, no reducir carga sin declararlo, no usar YAML como evidencia de escalado. Un mini-soak de 15 min no sustituye una prueba larga. La configuración es didáctica, no recomendación de producción.
