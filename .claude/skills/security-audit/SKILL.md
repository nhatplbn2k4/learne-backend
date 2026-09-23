---
name: security-audit
description: "Dùng khi user nói 'security audit', 'pentest', 'kiểm tra bảo mật', 'dependency scan', hoặc trước production deploy. Deep security check /100. v2 audit format: mỗi control bind với evidence block (Command run + Output + Verdict + Evidence artifact ID)."
user-invocable: true
---

# /security-audit — Deep Security Assessment (v2 format, portable)

Score /100. Goes deeper than a quality-audit's 10-point security category. Covers dependencies, secrets, OWASP, auth, and infra. Adapt `<placeholder>` paths to your project structure.

**v2 audit format:** every sub-check MUST ship a per-control evidence block (Command run + Output + Verdict + Evidence artifact ID) aligned to SOC2 Type II / ISO27001 Annex A / OWASP ASVS baseline. Narrative-only PASS BANNED. See `reference/audit-report-template-v2.md` for the skeleton.

## Process

### 1. Run Automated Scans

```bash
# Dependency vulnerabilities (per frontend app)
cd <frontend-app> && pnpm audit --json --audit-level=high 2>/dev/null | head -50

# Secret patterns (per pre-launch-secrets-hardening-checklist.md §2.1)
grep -rnE "(password|secret|api[_-]?key|token)\s*[:=]\s*['\"][a-zA-Z0-9_-]{8,}" \
  --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.yaml" \
  <frontend-app>/ <backend-app>/ scripts/ infrastructure/ \
  | grep -vE "(test|fixture|example|template|\.md:|noreply@|localhost|change-me|placeholder)" \
  | head -50

# Hardcoded IPs/URLs (all module src dirs)
grep -rn "localhost\|127\.0\.0\.1\|0\.0\.0\.0" --include="*.java" --include="*.yml" \
  src/main/ | head -20
```

### 2. Primacy: bug-finding > scoring (BLOCKING)

> **An audit's purpose is to surface bugs the dev team cannot trust other layers to catch. A high score with hidden P0 bugs is WORSE than a low score that lists every finding honestly.** A prior audit that averaged sub-checks within a 20-pt category let major gaps hide behind passing sub-checks (e.g. an OWASP A07 gap, or hardcoded passwords later caught by secret scanning). v2 rubric: per-check pass/fail WITH per-control evidence block; any P0/P1 fail in any check = audit FAIL regardless of total.

Rules for every audit run:
1. Enumerate ALL §3 sub-checks. NEVER skip one because "obviously fine."
2. Each sub-check returns: PASS / FAIL / N/A-with-reason / ❓ UNCHECKED. No "partial credit."
3. **Per-control evidence block MANDATORY (v2)** — Command run + Output + Verdict + Evidence artifact ID. See per-category sections below + `reference/audit-report-template-v2.md`.
4. Final output starts with a **bug list** (every FAIL surfaces) BEFORE the score.
5. Score is descriptive only; the bug list is the deliverable.
6. If audit time-budget runs out, leave remaining sub-checks marked `❓ UNCHECKED` — do NOT mark PASS by default. Coordinator decides whether to defer.

### 3. Score 5 Categories with per-check rubric + v2 evidence blocks

Every category binds to (a) a per-check pass/fail rule from a sister `pre-launch-*-hardening-checklist.md` AND (b) v2 evidence-block format. Within each 20-pt category: any P0/P1 sub-check FAIL caps category total ≤ 16/20.

| # | Category (20pts) | Sister rule (per-check rubric) | Min controls (evidence blocks) |
|---|-----------------|--------------------------------|-------------------------------|
| 1 | **Dependency Vulnerabilities** | `pre-launch-dependency-hardening-checklist.md` §2 (8 sub-checks) | ≥4 (FE audit, BE audit, lockfile, Dependabot/Trivy) |
| 2 | **Secrets & Credentials** | `pre-launch-secrets-hardening-checklist.md` §2 (8 sub-checks) | ≥4 (grep source, gitignore, secrets store, IaC scan) |
| 3 | **OWASP Top 10 (A01-A06, A08-A10)** | `pre-launch-owasp-rest-hardening-checklist.md` §2 (9 sub-checks) | ≥9 (one per OWASP item ex-A07) |
| 4 | **Auth & Access Control (OWASP A07)** | `pre-launch-auth-hardening-checklist.md` §2 (8 sub-checks) | ≥4 (gateway rate-limit matrix, lockout test, 2FA enrollment, password validator) |
| 5 | **Infrastructure Security** | `pre-launch-infra-hardening-checklist.md` §2 (9 sub-checks) | ≥5 (TLS listener, CORS, Docker non-root, IAM least-priv, audit trail) |

#### Per-check scoring (all 5 categories)

For each Category N:
1. Walk through every §2 sub-check in the bound rule.
2. Mark each sub-check `PASS` / `FAIL` / `N/A-with-reason` / `❓ UNCHECKED` (no partial credit).
3. **Produce evidence block for each check (v2 mandatory)** — see per-category template below.
4. Score = `20 - (failed_P0_count * 6) - (failed_P1_count * 3) - (failed_P2_count * 1)`, floor 0; cap 20 if all PASS.
5. If ANY P0 sub-check fails → category total CAPPED at 16/20 AND audit-level verdict = FAIL regardless of total score.
6. Each FAIL surfaces in the audit-report bug list per §2 "Primacy: bug-finding > scoring" — bug list is the deliverable.

Cross-reference: each sister rule's §2 enumerates concrete checks + grep/CLI commands. Reading the bound rule IS the rubric.

---

### Category 1 — Dependency Vulnerabilities (per-control evidence v2)

**Per-control evidence (v2 format mandatory):**

For each Cat 1 check (per `pre-launch-dependency-hardening-checklist.md` §2.1-2.8), produce this block:

```
**Control:** <e.g., "Frontend pnpm audit clean across all FE apps">

- Command run: `cd <frontend-app> && pnpm audit --json --audit-level=high`
- Output (raw, ≤20 lines snippet OR full output as artifact):
  ```
  <stdout/stderr verbatim — e.g., "found 0 vulnerabilities" OR JSON snippet with severity counts>
  ```
- Verdict: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL — with rationale (e.g., "0 high/critical; 2 moderate accepted per pnpm.overrides documented in dependency-waivers.md")
- Evidence artifact ID: `EVIDENCE-{audit-date}-DEPS-{NNN}` (e.g. `EVIDENCE-2026-05-15-DEPS-001`); store under `documents/audits/security/evidence/{audit-date}/{artifact-id}.txt` if output >20 lines
```

Min 4 controls Cat 1 (e.g. DEPS-001 pnpm audit FE, DEPS-002 mvn dependency-check BE, DEPS-003 Trivy image scan, DEPS-004 SBOM artifact).

---

### Category 2 — Secrets & Credentials (per-control evidence v2)

**Per-control evidence (v2 format mandatory):**

For each Cat 2 check (per `pre-launch-secrets-hardening-checklist.md` §2.1-2.8), produce this block:

```
**Control:** <e.g., "Zero hardcoded secrets in source per §2.1 grep mandate">

- Command run: `grep -rnE "(password|secret|api[_-]?key|token)\s*[:=]\s*['\"][a-zA-Z0-9_-]{8,}" --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.yaml" <frontend-app>/ <backend-app>/ scripts/ infrastructure/`
- Output:
  ```
  <full grep output OR explicit "0 hits"; if hits remain, classify each as placeholder/example or REAL leak>
  ```
- Verdict: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL — with hit count + cited line numbers + risk tier
- Evidence artifact ID: `EVIDENCE-{audit-date}-SEC-{NNN}` (e.g. `EVIDENCE-2026-05-15-SEC-001`)
```

Min 4 controls Cat 2 (e.g. SEC-001 grep source, SEC-002 .env.* gitignored, SEC-003 secrets store versioning + KMS, SEC-004 terraform IaC scan).

**Mandatory scope expansion:** grep MUST cover `docker-compose*.yml` + all frontend apps + all backend modules + `scripts/` + `infrastructure/`. (A narrative-only Cat 2 PASS once missed 11 hardcoded passwords in `docker-compose*.yml` — the evidence-block grep paste catches what narrative misses.)

---

### Category 3 — OWASP A01-A06/A08-A10 (per-control evidence v2)

**Per-control evidence (v2 format mandatory):**

For each OWASP item (per `pre-launch-owasp-rest-hardening-checklist.md` §2.1-2.9), produce this block — one block PER OWASP ITEM:

```
**Control:** A0X <item name>

- Command run: `<exact command — e.g. grep @PreAuthorize coverage OR config snapshot>`
- Output:
  ```
  <command output OR file snippet>
  ```
- Verdict: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL
- Evidence artifact ID: `EVIDENCE-{audit-date}-OWASP-A0X-{NNN}` (e.g. `EVIDENCE-2026-05-15-OWASP-A01-001`)
- Cross-reference: `pre-launch-owasp-rest-hardening-checklist.md` §2.X
```

Min 9 controls (one per OWASP item A01-A06, A08-A10 — A07 covered Cat 4):

| OWASP | Command pattern | Expected evidence |
|---|---|---|
| A01 Access Control | `grep -rn "@PreAuthorize" src/main/java/**/*AdminController.java` | Per-endpoint coverage matrix |
| A02 Crypto | `grep -rnE "MessageDigest\.getInstance\(\"(MD5\|SHA-1)\"\)" src/` | 0 hits expected |
| A03 Injection | `grep -rnE "(SELECT\|UPDATE\|DELETE).*\+\s*\w+\s*\+\|String\.format.*WHERE.*%" --include="*.java"` | 0 hits in non-test |
| A04 Insecure Design | `ls documents/architecture/threat-models/*.md` | Threat models exist for critical flows |
| A05 Misconfig | `cat src/main/resources/application-production.yml \| grep -A2 "management.endpoints.web.exposure"` | Actuator scoped, no `'*'` |
| A06 Vuln Components | Cross-ref Cat 1 SBOM artifact | Delegated |
| A08 Supply Chain | `grep -rn "image:" Dockerfile* docker-compose*.yml` | SHA-pinned OR documented |
| A09 Logging | `grep -rn "AdminAuditLog\|admin_audit_log" src/` | Audit log entity exists |
| A10 SSRF | `grep -rnE "(RestTemplate\|WebClient\|HttpClient).*\.(get\|post\|exchange)" --include="*.java" \| grep -iE "user\|input\|url"` | Each hit has allowlist |

---

### Category 4 — Auth & Access Control / OWASP A07 (per-control evidence v2)

**Per-control evidence (v2 format mandatory):**

For each Cat 4 check (per `pre-launch-auth-hardening-checklist.md` §2.1-2.8), produce this block:

```
**Control:** <e.g., "Auth endpoints rate-limited at gateway per §2.1 matrix">

- Command run: `grep -A5 'id: auth\|id: <app>-auth' <gateway-service>/src/main/resources/application.yml`
- Output:
  ```
  <YAML snippet showing route definitions with RequestRateLimiter — verify per-row matrix matches §2.1 minimum>
  ```
- Verdict: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL — with endpoint-by-endpoint coverage matrix
- Evidence artifact ID: `EVIDENCE-{audit-date}-AUTH-{NNN}`
```

Min 4 controls Cat 4 (e.g. AUTH-001 gateway rate-limit matrix, AUTH-002 lockout test output, AUTH-003 2FA enrollment endpoints listed, AUTH-004 password validator + audit-log entity verify).

---

### Category 5 — Infrastructure Security (per-control evidence v2)

**Per-control evidence (v2 format mandatory):**

For each Cat 5 check (per `pre-launch-infra-hardening-checklist.md` §2.1-2.9), produce this block:

```
**Control:** <e.g., "Load balancer TLS 1.2+ only on public listeners per §2.1">

- Command run: `<cloud-provider CLI query for listener TLS policy>`
- Output:
  ```
  <CLI output verbatim showing the negotiated TLS policy>
  ```
- Verdict: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL — with policy name verification (expect TLS 1.2+ baseline or stricter)
- Evidence artifact ID: `EVIDENCE-{audit-date}-INFRA-{NNN}`
```

Min 5 controls Cat 5 (e.g. INFRA-001 LB TLS, INFRA-002 CORS origins explicit, INFRA-003 Docker non-root USER sweep, INFRA-004 IAM/role least-priv grep, INFRA-005 cloud audit trail logging enabled).

---

### 4. Output

Save to `documents/audits/security/security-audit-[date].md` per `reference/audit-report-template-v2.md` skeleton.

**v2 mandatory sections:**
1. Header (date, scope, auditor)
2. Methodology (tools, scope coverage, sampling strategy)
3. Bug list (every FAIL — deliverable)
4. Score per category with per-check evidence blocks (≥25 blocks total = ~4 Cat1 + ~4 Cat2 + ~9 Cat3 + ~4 Cat4 + ~5 Cat5)
5. Findings table (linking back to evidence artifact IDs)
6. Verdict + score (track score / 100 + v2 evidence completeness % parallel — both required v2)
7. Recommendations + References

## Context Management

Token budget ~25-40K. Control with:

1. **pnpm audit output** — `pnpm audit --json --audit-level=high | head -50`. Do NOT read full JSON. Only need summary + critical/high count.
2. **Secret scan** — ALWAYS `| grep -v node_modules | grep -v test | head -30`. Don't scan the whole repo at once. When scope expansion is needed (per Cat 2 §2.1) MUST include `docker-compose*.yml` + all frontend apps + all backend modules + `scripts/` + `infrastructure/`.
3. **Staged execution** — Phase 1: automated scans (Cat 1-2). Phase 2: manual code review (Cat 3-5). If context low after phase 1, delegate phase 2 to a subagent.
4. **Config files** — read ONLY security-related sections of `application.yml`, not the whole file. Use `grep -A5 'security\|jwt\|cors\|csrf'`.
5. **Evidence artifacts** — large outputs (>20 lines) saved as separate file under `documents/audits/security/evidence/{audit-date}/{artifact-id}.txt`; report references artifact ID only.

## Gotchas

- If the project added an SVG sanitizer / URL allowlist / CSRF provider — verify they're ACTIVE not just coded
- `application.yml` security keys: check both main AND test profiles
- Gateway CORS config is the real enforcement point — not individual service configs
- Check the HTML-sanitizer library version (e.g. JSoup) for known CVEs on that exact version
- Rate limiting config is in the gateway `application.yml`, not the core service
- pnpm audit JSON output can be very large — ALWAYS limit output
- **Multi-module scope** — a narrow grep dir may miss submodule source files; prefer broad `--include="*.ext"` from root OR explicit `<module>/src/main/` glob
- **Evidence-block lesson** — a narrative-only Cat 2 PASS once missed 11 hardcoded passwords in `docker-compose*.yml`; the v2 evidence block (Command run + Output paste) catches what narrative misses. Mandatory grep scope: `docker-compose*.yml` + all frontend apps + all backend modules + `scripts/` + `infrastructure/`.

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category (v1 legacy — v2 supersedes for evidence format)
- `reference/audit-report-template-v2.md` — v2 audit report skeleton with ≥15 evidence-block placeholders
- `data/eval-fixtures/` — 3 synthetic scenarios for self-test

## Eval Fixtures

3 synthetic fixtures live under `data/eval-fixtures/` to keep this skill
honest when its body is edited (eval-first guidance). Each fixture has a
`# Expected: PASS|FAIL` header.

- `good.md` — clean baseline; pnpm-audit empty, no hardcoded secrets, all
  endpoints `@PreAuthorize`-guarded; expected output `100/100 Grade A`.
- `bad-secret-in-config.md` — `application.yml` contains an `sk-proj-…`
  literal + `password: admin123`; Cat 2 must report `-12+` and flag
  Severity 🛑 BLOCKER with evidence-block grep output paste.
- `edge-transitive-cve.md` — pnpm-audit / mvn report a CVE already pinned by
  the Spring Boot BOM or `pnpm.overrides`; Cat 1 must emit verify-vs-waive
  guidance instead of auto-failing.

**Run:** walk through the audit process steps mentally against the synthetic
content; each fixture's `Expected audit-report excerpt` section is the
regression contract. When extending this skill, re-walk all 3 fixtures and
confirm the expected outputs still hold + v2 evidence blocks produced for each check.

## Log

- **2026-05-15 (v2.0.0):** v2 audit format. Per-control evidence block (Command run + Output + Verdict + Evidence artifact ID) now MANDATORY for all 5 categories per SOC2 Type II / ISO27001 Annex A / OWASP ASVS baseline. Added `reference/audit-report-template-v2.md` skeleton. Reviewer: @nguyenvankiet (starter-kit upstream maintainer).
