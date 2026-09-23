# Eval Fixture — edge-transitive-cve.md

# Expected: FAIL (informational) — transitive CVE flagged but already mitigated by Spring Boot starter override

**Skill:** `quality/security-audit`
**Scenario:** Synthetic case where `npm audit` / `mvn dependency:tree` reports
a transitive vulnerability (e.g. log4shell-class) that is **already pinned
upstream** by the Spring Boot BOM or pnpm.overrides — but the audit tool
doesn't know that and emits a noisy report.
**Which check fires:** Category 1 — Dependency Vulnerabilities (informational
finding); reviewer must classify whether to file gap or document waiver.

---

## Setup (synthetic)

### `pom.xml` (backend service)

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.5.14</version>   <!-- bumps log4j2 to 2.24.x via BOM -->
</parent>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- pulls in log4j2 transitively, version managed by parent BOM -->
  </dependency>
</dependencies>
```

### `npm audit --json` output (synthetic)

```json
{
  "vulnerabilities": {
    "log4js": {
      "severity": "high",
      "via": ["log4js@<6.4.0"],
      "fixAvailable": false,
      "isDirect": false
    }
  }
}
```

### `<frontend-app>/package.json` (with override)

```json
{
  "pnpm": {
    "overrides": {
      "log4js": ">=6.4.0"
    }
  }
}
```

The npm-audit tool sometimes reports the transitive dep BEFORE applying the
override resolution, leading to false positives.

---

## Why this is an "edge" case

Two distinct issues collapsed into one:

1. **Genuine transitive CVE** (Java side) — log4j2 was vulnerable; the
   project pinned it via Spring Boot 3.5.14 parent BOM. Audit tool may not
   walk the BOM correctly and report log4j2 as still-vulnerable.

2. **False-positive from tool** (Node side) — pnpm.overrides pin log4js
   ≥6.4.0, but `npm audit` reads top-level package.json without applying
   overrides, so it reports the old version.

Both look like CVEs at first glance but neither is actionable.

---

## Expected audit-report excerpt

```
## Cat 1 — Dependency Vulnerabilities  16/20  (-4 informational)

### Findings (require human review):
1. log4j2 reported as CVE-2021-44228 — but Spring Boot 3.5.14 parent BOM
   pins log4j2 to 2.24.x (NOT vulnerable). Verify with:
       ./mvnw dependency:tree -Dincludes=org.apache.logging.log4j
   Actual version expected: 2.24.x → no action needed.

2. log4js reported as <6.4.0 — but `pnpm.overrides` in
   <frontend-app>/package.json pins to >=6.4.0. Verify with:
       cd <frontend-app> && pnpm why log4js
   If actual installed version is ≥6.4.0 → false positive, document waiver.

### Reviewer action:
- If both verifications confirm pinned-clean → no gap, document waiver in
  `documents/audits/security/cve-waivers.md`.
- If verification finds genuine vulnerability → file P0 gap, rotate any
  leaked artifacts.
```

---

## Why this matters

Two recurring patterns this fixture guards against:
- Dependabot can't auto-fix transitive deps for pnpm; a manual
  `pnpm.overrides` pin is required — audit tools may still report the
  pre-override version.
- First-run security tooling produces noisy results that need waiver review.

Audit skill must guide the reviewer through the verify-vs-waive decision
instead of just emitting "FAIL". The check value is in **classifying**
whether the CVE is real or already mitigated.

---

## How to use this fixture

When extending security-audit Cat 1, this fixture catches the trap of
"tool says vuln → file gap automatically". Reviewer must verify whether the
vulnerability is reachable in this codebase before filing a gap. Saving the
verify steps inline (mvn dependency:tree, pnpm why) is the value-add.

Regression test: any change to Cat 1 logic must still emit a verify-step
guidance for transitive CVEs. If a refactor removes the guidance, reviewer
loses the false-positive filter and gap quality drops.
