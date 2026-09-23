---
name: performance-audit
description: "Dùng khi user nói 'perf audit', 'performance check', 'load test', 'kiểm tra hiệu năng', 'N+1', 'bundle size', hoặc trước production deploy. Baseline performance metrics /100."
user-invocable: true
---

# /performance-audit — Performance Baseline Assessment (portable)

Score /100. Identify bottlenecks across DB, API, frontend, caching, and resources.
Adapt `<placeholder>` paths to your project structure.

## Process

### 1. Automated Checks

```bash
# DB: N+1 query detection (adapt src dir to your backend modules)
grep -rn "findAll\|findBy" --include="*.java" src/main/ | grep -v test | head -30
grep -rn "@Query" --include="*.java" src/main/ | head -20

# FE bundle: build analysis (adapt to each frontend app)
cd <frontend-app> && npm run build 2>&1 | tail -30

# Caching (broad scope — catches all submodules)
grep -rn "RedisTemplate\|@Cacheable\|cache" --include="*.java" | grep -v test | grep -v target | head -20

# Resource config (broad scope)
grep -rn "pool-size\|max-connections\|timeout\|thread" --include="*.yml" | grep -v target | head -20
```

### 2. Primacy: bug-finding > scoring (BLOCKING)

> **An audit's purpose is to surface performance bombs (N+1, missing pagination, missing bulkhead) BEFORE prod. A `/100` score with hidden P0 bombs is WORSE than a low score listing every bomb honestly.** Per `audit-skill-rubric-performance-audit.md` §4.

Rules for every audit run:
1. Enumerate ALL §3 sub-checks per category. NEVER skip "obviously fine."
2. Each sub-check returns: PASS / FAIL / N/A-with-reason / `❓ UNCHECKED`. No partial credit.
3. Final output starts with **bug list** (every FAIL with `file:line` evidence) BEFORE the score.
4. Score is descriptive only; audit-level verdict = FAIL if ANY P0 sub-check FAILS.
5. If audit time-budget runs out, mark `❓ UNCHECKED` — do NOT default to PASS.

### 3. Score 5 Categories with per-check rubric

Every category binds to a per-check pass/fail rule.

| # | Category (20pts) | Per-check rubric file |
|---|-----------------|-----------------------|
| 1 | **DB Query Efficiency** | **`audit-skill-rubric-performance-audit.md` §2.1 (6 sub-checks)** |
| 2 | **API Response Time** | **`audit-skill-rubric-performance-audit.md` §2.2 (6 sub-checks)** |
| 3 | **Frontend Bundle** | **`audit-skill-rubric-performance-audit.md` §2.3 (6 sub-checks)** |
| 4 | **Caching Strategy** | **`audit-skill-rubric-performance-audit.md` §2.4 (6 sub-checks)** |
| 5 | **Resource Utilization** | **`audit-skill-rubric-performance-audit.md` §2.5 (6 sub-checks)** |

#### Per-check scoring (all 5 categories)

For each Category N:
1. Walk through every §2 sub-check in the bound rule.
2. Mark each sub-check PASS / FAIL / N/A-with-reason / `❓ UNCHECKED`.
3. Score = `20 - (failed_P0_count * 6) - (failed_P1_count * 3) - (failed_P2_count * 1)`, floor 0; cap 20 if all PASS.
4. If ANY P0 sub-check fails → category total CAPPED at 16/20 AND audit-level verdict = FAIL.
5. Each FAIL surfaces in bug list per §2 primacy.

Legacy scoring narrative: `reference/scoring-guide.md` retained for backward-compat only.

### 4. Output

Save to `documents/audits/performance/performance-audit-[date].md`

## Context Management

Token budget ~15-25K (smallest in the audit suite). Watch:

1. **Build output** — `npm run build` output can be 100+ lines. ALWAYS `| tail -30` (only the route sizes table is needed).
2. **Grep N+1** — `| head -30` per module. Count occurrences, don't list everything.
3. **No load test during audit** — static analysis + config review only. Load test = separate task.

## Gotchas

- `findAll()` on a JPA repo without `@Query` or `Pageable` = potential N+1
- Build output (e.g. Next.js `next build`) shows route sizes — flag anything >250KB
- Redis config typically lives in `application.yml` under `spring.data.redis`
- If the project uses a bulkhead library (e.g. Resilience4j) — check thread pool sizes match expected concurrency
- Docker `deploy.resources.limits` may not be set in dev compose — check k8s/Helm for prod
- Build output can be very long — only take the final summary table
- **Multi-module scope** — a narrow grep dir may miss submodules. Use broad `--include="*.java"` from root OR explicit `<module>/src/` glob
- Always `grep -v target` to exclude compiled `target/classes/` duplicates

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category
