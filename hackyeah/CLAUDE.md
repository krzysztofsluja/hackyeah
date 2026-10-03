# CLAUDE.md

Hackathon project (Smart City / Health&Care), solo, 23 h: Sat 12:00 → Sun 11:00.
Full concept and plan live in the shared design doc; this file is the working summary for coding.

## What we're building

A system that finds a place for a patient who must be transferred between hospitals.
A doctor files a short, anonymized referral → the system filters hospitals by hard requirements,
ranks them by travel time and occupancy (weights depend on urgency) → sends requests in waves →
the first hospital to accept wins → the coordinator sees city-wide hospital load on a map.

Metric: time from transfer decision to confirmed acceptance.

### Out of scope (do NOT build)
- Emergency dispatch (stays with the state medical dispatch system)
- Transport / ambulances (roadmap only)
- Clinical decision support — we match patient to place, we never decide treatment
- Auth/login (role switcher in UI instead), real FHIR (plain JSON events), mobile, push notifications,
  external traffic APIs (static travel-time matrix + rush-hour multiplier), form personalization
- Personal data: referrals carry a clinical profile only — no name, no PESEL

## Stack
- Java + Spring Boot, **Spring MVC monolith** (no WebFlux, no microservices)
- Spring Data JPA, **H2 file mode** (data survives restarts)
- Thymeleaf + vanilla JS `EventSource`, no bundler, no SPA framework
- Leaflet (CDN) only on the coordinator dashboard
- Lombok, Validation, DevTools

```properties
spring.datasource.url=jdbc:h2:file:./data/hosdb;MODE=PostgreSQL;AUTO_SERVER=TRUE
spring.jpa.hibernate.ddl-auto=update
```

If the schema behaves oddly after changing an entity: stop the app and delete `./data`.

## Package structure (package-by-feature, NOT hexagonal)

```
<base>.hospital    // Hospital, HospitalFlag, repos, seed
<base>.matching    // PURE JAVA: filter + scoring. No Spring, no JPA. Fully unit-tested.
<base>.referral    // Referral, ReferralRequest, waves, accept/decline, timeout scheduler
<base>.stream      // SseHub, AFTER_COMMIT event listener
<base>.dashboard   // JSON endpoint for the map, ADT simulator
<base>.web         // Thymeleaf controllers + fragment endpoints
<base>.demo        // reset endpoint, scenario data
```

Inside a package: entity, repository, service, controller. No interfaces for single-implementation services.
Keep it simple — optimize for shipping, not for architecture purity.

## Domain model

- `Hospital`: name, lat/lng, totalBeds, occupiedBeds, specialties, procedures/equipment
- `HospitalFlag`: hospital, type (e.g. `TK_DOWN`, `NEURO_AVAILABLE`, `CATH_LAB_BUSY`), `validUntil`
- `Referral`: fixed 5-field schema + optional one-line note,
  `status = OPEN | ACCEPTED | ESCALATED`, `acceptedHospitalId`, originHospitalId, createdAt
  - targetSpecialty (hard), requiredProcedures (hard), urgency (`TIME_CRITICAL | URGENT_STABLE | PLANNED`),
    patientState (`STABLE | UNSTABLE | VENTILATED`), isolation (hard)
- `ReferralRequest`: referral, hospital, wave, `status = PENDING | ACCEPTED | DECLINED | EXPIRED | CANCELLED`,
  declineReason (`NO_BEDS | NO_SPECIALIST | EQUIPMENT_UNAVAILABLE | OTHER`), sentAt, deadline
- `TravelTime`: fromHospitalId → toHospitalId → minutes; global rush-hour multiplier