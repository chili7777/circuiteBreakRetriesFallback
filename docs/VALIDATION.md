# Validation status

## Verified structurally

- Hexagonal flow is represented explicitly: inbound adapter → inbound port → application service → outbound port → outbound adapter.
- Payment simulator contains bounded failure percentage and delay controls.
- Orders fallback returns a rejected business result rather than a false authorization.
- Kubernetes HPA is `autoscaling/v2`, has CPU requests to make utilization meaningful, min 2 / max 6 and 120 s scale-down stabilization.
- k6 scenarios request smoke, baseline, load, stress, spike, soak and outage profiles and check business status separately from HTTP transport.

## Not executed in the authoring environment

The authoring runtime did not have Maven, Docker, kubectl or k6 installed, and its shell had no external DNS access. Therefore this repository has **not** been claimed as Maven-built, container-started, k6-executed or Kubernetes-deployed from that runtime.

Participants must run `mvn clean verify` first. If a build/runtime issue appears, fixing and documenting it is acceptable workshop work; do not hide it.

## Important

The values in `application.yml`, k8s manifests and load scenarios are teaching baselines. They are not Banco Pichincha production configuration and must not be copied to production without measurement and review.
