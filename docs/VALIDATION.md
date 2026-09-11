# Estado real de validación — 11 de septiembre de 2026

## Ejecutado y aprobado en GitHub Actions

Ejecución: https://github.com/chili7777/circuiteBreakRetriesFallback/actions/runs/34609037951

Commit del código verificado: `94c8a3ba198264ccb2a58843970da11af417524e`.

| Verificación | Windows + JDK 17 | Ubuntu Linux + JDK 17 |
|---|---|---|
| `mvn -B -ntp clean verify` | PASS | PASS |
| Compilación de `tools/LabLoad.java` con `--release 17` | PASS | PASS |
| Arranque real de los dos JAR Spring Boot | PASS | PASS |
| Endpoints readiness y orden con proveedor sano | PASS | PASS |
| Generador Java, smoke abreviado de 5 segundos | PASS | PASS |
| Caída completa: no autorizar pagos falsos | PASS | PASS |
| Generador Java, outage abreviado de 5 segundos | PASS | PASS |
| Recuperación después de la ventana OPEN | PASS | PASS |
| Dependencia lenta de 1200 ms: resultado no confirmado | PASS | PASS |

La lógica ejecutada está en `tools/verify-runtime.py` y la automatización en `.github/workflows/verify.yml`. Python es solo una dependencia del verificador de CI, NO del taller. El generador que usan los alumnos solo requiere el JDK.

Estas comprobaciones prueban build y un flujo HTTP básico. NO equivalen a ejecutar todos los perfiles completos ni a demostrar el SLO o la capacidad de 80/150 rps. La prueba de recuperación verifica resultados HTTP después de la ventana; no es una inspección exhaustiva de eventos de cada transición del breaker.

## Pruebas adicionales del generador en el entorno de autoría

JDK 21 en Linux; compilación restringida a API Java 17. Siete casos contra un servidor HTTP local de prueba: readiness, respuesta confirmada, HTTP 200 con REJECTED que falla el criterio de éxito, outage controlado, HTTP 500, descartes al saturar concurrencia y comando de inyección. Todos aprobados. Esto es independiente de las pruebas anteriores con los Spring Boot reales.

## Lo que NO se ejecutó en esta validación

- macOS.
- Build/arranque de los Dockerfiles multietapa y ejecución de k6 en Compose.
- Todos los perfiles completos de carga/stress/spike/soak.
- Despliegue Kubernetes, Metrics Server, Lens ni escalado real de HPA.
- Verificación de proxy, permisos, RAM, puertos disponibles ni restricciones de las laptops de los participantes.

Los cambios posteriores en README/guías y en Compose/k6 no se presentan como parte de la ejecución Java anterior. El código Java verificado no cambió con esas actualizaciones.

## Requisitos que siguen existiendo

Ruta Java: JDK 17/21 y Maven (CLI o del IDE) con acceso permitido a sus dependencias; puertos 8080/8081 libres. No necesita k6 ni Docker. Ruta Docker: motor autorizado con Compose, contenedores Linux y descargas permitidas. No se puede garantizar ejecución offline en una máquina sin caché.

Antes de empezar: ambos servicios UP y smoke confirmado. Si la máquina está bloqueada, trabajar en pareja en una máquina habilitada, sin evadir políticas corporativas.

Todo es material didáctico y datos sintéticos. No es una configuración de producción del banco.
