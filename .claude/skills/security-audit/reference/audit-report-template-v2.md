# Security Audit Report — v2 Format Template

Skeleton for an audit report v2. Every control across all 5 categories MUST have a per-control evidence block (Command run + Output + Verdict + Evidence artifact ID) per SOC2 Type II Section 4 / ISO27001 Annex A / OWASP ASVS baseline.

**Usage:** copy this template to `documents/audits/security/security-audit-YYYY-MM-DD.md`, fill section by section. Adapt `<placeholder>` paths to your project + `<cloud-provider CLI ...>` to your infra tooling.

---

## Frontmatter (mandatory)

```yaml
---
title: Security Audit — <topic / milestone>
status: complete
created: YYYY-MM-DD
phase: <pre-launch / release / ongoing>
auditor: <agent ID + model + session ID>
gaps: [<gap-ids>, ...]
baseline_security_score: <prior score + date + ref>
audit_format_version: v2
evidence_dir: documents/audits/security/evidence/YYYY-MM-DD/
---
```

---

## 1. Header

**Audit scope:** commit range `<sha1>..<sha2>` — N PRs / scope description.

**Method:** Per `security-audit/SKILL.md` v2 — per-check pass/fail (no averaging) + per-control evidence block (Command run + Output + Verdict + Evidence artifact ID).

**Baselines compared:**
- <prior audit + date>: <score>
- <prior audit + date>: <score>

---

## 2. Methodology

**Tools used:**
- `pnpm audit --json --audit-level=high` (Cat 1 FE)
- `mvn dependency-check:check -DfailBuildOnCVSS=7` (Cat 1 BE)
- `grep -rnE` (Cat 2 + Cat 3 source scan)
- Cloud provider CLI — load balancer listeners / managed DB / secrets store (Cat 5)
- Secrets store `describe-secret` equivalent (Cat 2)

**Scope coverage:**
- File paths scanned: `<frontend-app>/`, `<backend-app>/`, `scripts/`, `infrastructure/`, `documents/`
- Modules: <list>
- Environments: <production / staging / dev>
- Time window: <commit range / merge window>

**Sampling strategy:**
- Cat 1: 100% deps (all package manifests)
- Cat 2: 100% grep coverage on source (include `docker-compose*.yml` + all frontend apps + all backend modules + `scripts/` + `infrastructure/`)
- Cat 3: 100% per-OWASP-item (9 items)
- Cat 4: 100% auth endpoints
- Cat 5: 100% infra checks (cloud resources via read-only credentials)

---

## 3. Score Summary

| # | Category (20pt) | Score | Verdict | Evidence blocks |
|---|-----------------|:-----:|:-------:|:---------------:|
| 1 | Dependency Vulnerabilities | XX/20 | 🟢/🟡/🔴 | N |
| 2 | Secrets & Credentials | XX/20 | 🟢/🟡/🔴 | N |
| 3 | OWASP A01-A06/A08-A10 | XX/20 | 🟢/🟡/🔴 | N (≥9) |
| 4 | Auth & Access Control (A07) | XX/20 | 🟢/🟡/🔴 | N |
| 5 | Infrastructure Security | XX/20 | 🟢/🟡/🔴 | N |

**Total: XX/100 — Grade** (delta vs baseline).

**v2 evidence completeness:** N/Y total expected (target 100%).

---

## 4. Bug List (deliverable — surface BEFORE score)

### P0 — BLOCKING release-candidate promotion

**P0-1: <title>**
- File: <path:line>
- **Impact:** <description>
- **Fix:** <proposed>
- **Evidence:** EVIDENCE-YYYY-MM-DD-<CAT>-NNN

### P1 — Should fix before release-candidate

**P1-1: <title>**
- (same structure)

### P2 — Track for next milestone

**P2-1: <title>**

---

## 5. Per-Category Evidence Blocks (v2 mandatory — ≥15 blocks total)

### Cat 1 — Dependency Vulnerabilities (≥4 evidence blocks)

#### DEPS-001 — Frontend pnpm audit clean (P0)

**Control:** Per `pre-launch-dependency-hardening-checklist.md` §2.1 — `pnpm audit --json` returns ZERO CRITICAL/HIGH across all FE apps.

- **Command run:**
  ```bash
  cd <frontend-app> && pnpm audit --json --audit-level=high
  ```
- **Output:**
  ```
  <stdout snippet — e.g. "found 0 vulnerabilities" or JSON summary>
  ```
- **Verdict:** ✅ PASS — 0 high/critical across all FE apps. (Or ❌ FAIL with hit count + rationale.)
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-DEPS-001`

#### DEPS-002 — Backend Maven dependency-check (P0)

**Control:** Per §2.2 — `mvn dependency-check` returns ZERO CRITICAL/HIGH CVSS ≥7.

- **Command run:**
  ```bash
  cd <backend-app> && ./mvnw -pl <module> dependency-check:check -DfailBuildOnCVSS=7
  ```
- **Output:**
  ```
  <BUILD SUCCESS or dependency-check report summary>
  ```
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-DEPS-002`

#### DEPS-003 — Trivy container image scan (P0)

**Control:** Per `release-deploy-standard.md` §3.1 + sister rule — Trivy scans built images for CRITICAL/HIGH OS + library CVEs.

- **Command run:**
  ```bash
  trivy image --severity HIGH,CRITICAL --format json <image:tag>
  ```
- **Output:**
  ```
  <trivy summary — count by severity>
  ```
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-DEPS-003`

#### DEPS-004 — SBOM artifact attached to release (P2)

**Control:** Per §2.8 — CycloneDX SBOM generated per release tag.

- **Command run:**
  ```bash
  ls documents/audits/security/sbom/ | grep <version>
  # OR
  gh release view <tag> --json assets --jq '.assets[].name'
  ```
- **Output:**
  ```
  <list SBOM artifact files>
  ```
- **Verdict:** ✅/❌/⚠️ PARTIAL (manual generation acceptable v1)
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-DEPS-004`

---

### Cat 2 — Secrets & Credentials (≥4 evidence blocks)

#### SEC-001 — Zero hardcoded secrets in source (P0)

**Control:** Per `pre-launch-secrets-hardening-checklist.md` §2.1 — grep mandate covering `docker-compose*.yml` + all frontend apps + all backend modules + `scripts/` + `infrastructure/`. (Mandatory scope expansion — closes the narrative-miss class.)

- **Command run:**
  ```bash
  grep -rnE "(password|secret|api[_-]?key|token)\s*[:=]\s*['\"][a-zA-Z0-9_-]{8,}" \
    --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.yaml" \
    <frontend-app>/ <backend-app>/ scripts/ infrastructure/ \
    | grep -vE "(test|fixture|example|template|\.md:|noreply@|localhost|change-me|placeholder)"
  ```
- **Output:**
  ```
  <full grep output OR explicit "0 hits"; classify each remaining hit>
  ```
- **Verdict:** ✅ PASS (0 hits) / ❌ FAIL (N hits — list line numbers)
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-SEC-001`

#### SEC-002 — .env.* gitignored + only templates committed (P0)

**Control:** Per §2.2 — runtime env files gitignored, templates only.

- **Command run:**
  ```bash
  git ls-files | grep -E "^\.env(\.|$)" | grep -vE "(template|example)$"
  ```
- **Output:**
  ```
  <expected: empty; otherwise list paths to investigate>
  ```
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-SEC-002`

#### SEC-003 — Secrets store versioning + KMS (P0)

**Control:** Per §2.3 + §2.4 — versioning enabled + customer-managed KMS key.

- **Command run:**
  ```bash
  # cloud provider secrets-store CLI: list secrets + rotation + KMS key id
  <cloud-provider CLI: list-secrets --query '[Name, RotationEnabled, KmsKeyId]'>
  ```
- **Output:**
  ```
  <table output showing secrets + rotation + KMS key>
  ```
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-SEC-003`

#### SEC-004 — IaC scan (P1)

**Control:** Per §2.7 — terraform `*.tf` files free of secret literals.

- **Command run:**
  ```bash
  grep -rnE "(password|api_key|secret|token)\s*=\s*\"[a-zA-Z0-9_-]{8,}\"" \
    infrastructure/terraform/*.tf
  ```
- **Output:**
  ```
  <expected: 0 hits>
  ```
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-SEC-004`

---

### Cat 3 — OWASP A01-A06/A08-A10 (≥9 evidence blocks — 1 per item)

#### OWASP-A01-001 — Broken Access Control (P0)

**Control:** Per `pre-launch-owasp-rest-hardening-checklist.md` §2.1 — every admin/privileged endpoint has explicit `@PreAuthorize`.

- **Command run:**
  ```bash
  grep -rn "@PreAuthorize" src/main/java/**/*AdminController.java | wc -l
  grep -rnE "@(Post|Put|Patch|Delete|Get)Mapping" src/main/java/**/*AdminController.java | wc -l
  # Compare counts — should match (every admin endpoint has @PreAuthorize)
  ```
- **Output:**
  ```
  @PreAuthorize count: N
  Mapping count: M
  Coverage: N/M (X%)
  ```
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A01-001`

#### OWASP-A02-001 — Cryptographic Failures (P0)

**Control:** Per §2.2 — no weak ciphers (MD5/SHA1/DES/RC4).

- **Command run:**
  ```bash
  grep -rnE "MessageDigest\.getInstance\(\"(MD5|SHA-1)\"\)" src/ --include="*.java"
  ```
- **Output:** `<expected 0 hits>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A02-001`

#### OWASP-A03-001 — Injection (P0)

**Control:** Per §2.3 — parameterized queries only.

- **Command run:**
  ```bash
  grep -rnE "(SELECT|UPDATE|DELETE|INSERT).*\+\s*\w+\s*\+|String\.format.*WHERE.*%" \
    src/ --include="*.java"
  ```
- **Output:** `<expected 0 hits non-test>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A03-001`

#### OWASP-A04-001 — Insecure Design (P1)

**Control:** Per §2.4 — threat models per critical flow.

- **Command run:**
  ```bash
  ls documents/architecture/threat-models/*.md
  ```
- **Output:** `<list files OR "directory not found">`
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A04-001`

#### OWASP-A05-001 — Security Misconfiguration (P1)

**Control:** Per §2.5 — production profile hardened (actuator scoped, no stacktrace).

- **Command run:**
  ```bash
  grep -A2 "management.endpoints.web.exposure" \
    src/main/resources/application-production.yml
  grep -A2 "include-stacktrace\|include-message" \
    src/main/resources/application-production.yml
  ```
- **Output:**
  ```
  <config snippets — actuator should be 'health' only; stacktrace 'never'>
  ```
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A05-001`

#### OWASP-A06-001 — Vulnerable Components (delegated to Cat 1)

**Control:** Cross-reference DEPS-001 + DEPS-002 + DEPS-003 evidence blocks.

- **Verdict:** PASS if Cat 1 PASS; else delegate findings.
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A06-001` → references DEPS-001 to DEPS-003

#### OWASP-A08-001 — Software & Data Integrity (P1)

**Control:** Per §2.7 — Docker images + CI Actions SHA-pinned.

- **Command run:**
  ```bash
  grep -rn "image:" Dockerfile* docker-compose*.yml | grep -v "@sha256"
  grep -rn "uses:" .github/workflows/*.yml | grep -v "@[a-f0-9]\{40\}"
  ```
- **Output:** `<list non-SHA-pinned refs>`
- **Verdict:** ✅/❌/⚠️ PARTIAL (tag-pinned + Dependabot active = acceptable v1)
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A08-001`

#### OWASP-A09-001 — Logging & Monitoring (P1)

**Control:** Per §2.8 — admin_audit_log entity + PII scrubbing per `logs-format-standard.md`.

- **Command run:**
  ```bash
  grep -rn "AdminAuditLog\|admin_audit_log" src/ --include="*.java"
  ```
- **Output:** `<list entity + interceptor refs>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A09-001`

#### OWASP-A10-001 — SSRF (P1)

**Control:** Per §2.9 — outbound HTTP clients have URL allowlist.

- **Command run:**
  ```bash
  grep -rnE "(RestTemplate|WebClient|HttpClient).*\.(get|post|exchange)" \
    src/ --include="*.java" | grep -iE "user|input|url"
  ```
- **Output:** `<each hit reviewed for allowlist>`
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-OWASP-A10-001`

---

### Cat 4 — Auth & Access Control (OWASP A07) (≥4 evidence blocks)

#### AUTH-001 — Auth endpoints rate-limited (P0)

**Control:** Per `pre-launch-auth-hardening-checklist.md` §2.1 — gateway RequestRateLimiter coverage matrix.

- **Command run:**
  ```bash
  grep -A5 'id: auth\|id: <app>-auth' \
    <gateway-service>/src/main/resources/application.yml
  ```
- **Output:**
  ```
  <YAML route snippets — verify per §2.1 matrix (register/login/refresh/verify-email/resend/password-reset/signup)>
  ```
- **Verdict:** ✅/❌/⚠️ with coverage matrix M/N
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-AUTH-001`

#### AUTH-002 — Account lockout (P0)

**Control:** Per §2.2 — 5 failed attempts / 15-min lockout + exponential backoff.

- **Command run:**
  ```bash
  grep -rn "failedLoginAttempts\|accountLocked\|lockoutUntil" \
    src/ --include="*.java"
  ./mvnw -pl <auth-module> test -Dtest=AuthServiceLockoutTest
  ```
- **Output:**
  ```
  <entity field + service logic + test output BUILD SUCCESS>
  ```
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-AUTH-002`

#### AUTH-003 — 2FA mandatory for privileged admin (P1)

**Control:** Per §2.4 — 2FA service + admin login enforces TOTP challenge.

- **Command run:**
  ```bash
  grep -rn "TwoFactorAuthService\|TotpSecretCipher\|@TwoFactorRequired" \
    src/ --include="*.java"
  grep -rn "twofactor\|2fa" \
    src/main/java/**/controller/*.java
  ```
- **Output:** `<list endpoints + service implementations>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-AUTH-003`

#### AUTH-004 — Password complexity (P1)

**Control:** Per §2.3 — PasswordValidator min 12 chars + complexity + reuse check.

- **Command run:**
  ```bash
  grep -rn "PasswordValidator\|MIN_PASSWORD_LENGTH\|zxcvbn" \
    src/ --include="*.java" --include="*.ts"
  ```
- **Output:** `<list validator + applied at registration + reset>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-AUTH-004`

---

### Cat 5 — Infrastructure Security (≥5 evidence blocks)

#### INFRA-001 — TLS 1.2+ on load balancer (P0)

**Control:** Per `pre-launch-infra-hardening-checklist.md` §2.1 — every public listener enforces TLS 1.2+.

- **Command run:**
  ```bash
  # cloud provider CLI: query public listener TLS/SSL policy
  <cloud-provider CLI: describe-listeners --query '[Port, SslPolicy]'>
  ```
- **Output:**
  ```
  <listener TLS policy — expect TLS 1.2+ baseline or stricter>
  ```
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-INFRA-001`

#### INFRA-002 — CORS origins explicit (P0)

**Control:** Per §2.2 — production CORS has no `*`.

- **Command run:**
  ```bash
  grep -rn "CORS_ALLOWED_ORIGINS\|allowedOrigins" <gateway-service>/ infrastructure/
  # OR cloud provider config/parameter store CLI for the CORS param
  ```
- **Output:** `<CORS config value — expect explicit domain list>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-INFRA-002`

#### INFRA-003 — Docker non-root USER (P0)

**Control:** Per §2.4 — every Dockerfile has `USER <non-root>` before CMD.

- **Command run:**
  ```bash
  for dockerfile in $(find . -name "Dockerfile*" -not -path "*/node_modules/*"); do
    grep -E "^USER " "$dockerfile" || echo "MISSING USER: $dockerfile"
  done
  ```
- **Output:** `<expected: no MISSING outputs>`
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-INFRA-003`

#### INFRA-004 — IAM/role least-privilege (P0)

**Control:** Per §2.5 — no `Action: "*" + Resource: "*"` admin patterns.

- **Command run:**
  ```bash
  grep -rnE "(Action|Resource).*\"\*\"" infrastructure/terraform/*.tf
  ```
- **Output:** `<list overly-permissive policies — should be only bounded exceptions documented>`
- **Verdict:** ✅/❌/⚠️
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-INFRA-004`

#### INFRA-005 — Cloud audit trail enabled (P0)

**Control:** Per §2.8 — multi-region cloud audit trail with logging enabled.

- **Command run:**
  ```bash
  # cloud provider CLI: query audit-trail logging status + multi-region flag
  <cloud-provider CLI: get-trail-status --query 'IsLogging'>
  <cloud-provider CLI: describe-trails --query '[IsMultiRegion, IncludeGlobalServiceEvents]'>
  ```
- **Output:**
  ```
  True
  [[true, true]]
  ```
- **Verdict:** ✅/❌
- **Evidence artifact ID:** `EVIDENCE-YYYY-MM-DD-INFRA-005`

---

## 6. Findings Table (linking to evidence artifact IDs)

| Finding ID | Severity | Category | Title | Evidence | Status |
|---|---|---|---|---|---|
| F-001 | P0 | Cat 2 | <title> | EVIDENCE-YYYY-MM-DD-SEC-001 | 🔵 OPEN GAP-NNN |
| F-002 | P1 | Cat 3 A05 | <title> | EVIDENCE-YYYY-MM-DD-OWASP-A05-001 | 🔵 OPEN GAP-NNN |

---

## 7. Aggregate Verdict + Score Delta

| Baseline | Date | Score | This audit delta |
|---|---|:---:|:---:|
| <prior baseline> | YYYY-MM-DD | XX/100 | +/- N |

**Production-readiness threshold ≥80:** ✅ PASS / ❌ FAIL with buffer N points.

**v2 evidence completeness:** N/M total expected (target 100%).

---

## 8. Recommendations

1. <Priority + recommendation + owner>
2. <...>

---

## 9. Pending (post-audit actions)

| Action | Owner | Notes |
|---|---|---|
| File N new gap files | Coordinator | Per `audit-to-gap-pipeline.md` §3 |
| Update the gap tracker with new rows | Coordinator | Canonical gap index |
| Update the audits index row for this audit | Coordinator | Canonical audits index |
| Update ROADMAP / status snapshot | Coordinator | Per `audit-to-gap-pipeline.md` |
| Update `output-review-mandate.md` §3 Security Baseline row | Coordinator | Reflect new score + version |

---

## 10. References

- **Audit skill:** `security-audit/SKILL.md` v2
- **Audit format template:** `security-audit/reference/audit-report-template-v2.md` (this file)
- **Sister rules (Cat 1-5 per-check):**
  - `pre-launch-dependency-hardening-checklist.md`
  - `pre-launch-secrets-hardening-checklist.md`
  - `pre-launch-owasp-rest-hardening-checklist.md`
  - `pre-launch-auth-hardening-checklist.md`
  - `pre-launch-infra-hardening-checklist.md`
- **Baseline audits:** `documents/audits/security/<earlier-reports>.md`
- **Governance:**
  - `post-wave-audit-mandate.md` (audit trigger per file-pattern matrix)
  - `audit-to-gap-pipeline.md` §3 (gap filing pipeline)
  - `output-review-mandate.md` §3 (Security audit row)

---

## Evidence directory structure

```
documents/audits/security/evidence/YYYY-MM-DD/
├── EVIDENCE-YYYY-MM-DD-DEPS-001.txt
├── EVIDENCE-YYYY-MM-DD-DEPS-002.txt
├── EVIDENCE-YYYY-MM-DD-SEC-001.txt
├── EVIDENCE-YYYY-MM-DD-SEC-002.txt
├── EVIDENCE-YYYY-MM-DD-OWASP-A01-001.txt
├── EVIDENCE-YYYY-MM-DD-OWASP-A02-001.txt
├── ...
└── README.md  # index + audit cross-reference
```

Each `EVIDENCE-*.txt` file contains full Command run + raw Output. The audit report cites the artifact ID only (compact); the reader opens the artifact file for full evidence.

---

## Self-test (worked example — Cat 2 hardcoded-secret class)

v2 format catches what v1 narrative missed: a Cat 2 hardcoded-password grep evidence block, run with the mandatory expanded scope (`docker-compose*.yml` + all frontend + all backend + `scripts/` + `infrastructure/`), surfaces the leaked literals directly in the pasted Output. A narrative-only "Secrets PASS" claim would have hidden them — the evidence-block paste is the regression contract.

---

## Version

- **v2.0** — Per-control evidence block mandatory for all 5 categories. SOC2 Type II / ISO27001 / OWASP ASVS aligned.
