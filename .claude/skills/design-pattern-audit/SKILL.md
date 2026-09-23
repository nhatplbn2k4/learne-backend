---
name: design-pattern-audit
description: "Dùng khi user nói 'design pattern audit', 'pattern check', 'kiểm tra design patterns', 'God service đâu', 'anti-pattern hotspot', hoặc trước một refactor planning cycle. Score code against `.claude/rules/design-patterns.md` §3 BANNED list. Output: hotspot list (file path + LOC + violated pattern) + score /100."
user-invocable: true
---

# /design-pattern-audit — Verify Code ↔ Pattern Rules

Score /100. Walk every service module, flag `design-patterns.md` §3 anti-patterns.

## Process

### 1. Collect Modules

```bash
# Adapt the search roots to your repo layout
find . -maxdepth 4 -type d -name src 2>/dev/null | grep main/java
```

### 2. Per-Module: 5 Anti-Pattern Categories (20 pts each)

For EACH service module, run detectors and apply rubric in `reference/scoring-guide.md`:

| # | Category (20pts) | Detector | Pass criteria |
|---|------------------|----------|---------------|
| 1 | **God Service / Class** | `find . -name "*Service.java" -exec wc -l {} +` | No service > 500 LOC (per `design-patterns.md` §3.1) |
| 2 | **Status switch/if cascade** | grep `if.*Status ==` / `switch.*[Ss]tatus` | State Pattern used for entities with ≥3 lifecycle states (per `design-patterns.md` §3.3) |
| 3 | **Primitive Obsession** | grep public method signatures with `String color\|String email\|String hex` | Value objects (e.g. `Email`, `Money`) preferred for validated primitives (per `design-patterns.md` §3.2) |
| 4 | **Leaky Abstraction** | grep external-vendor response types outside `*adapter*` packages | External API types isolated to Adapter layer (per `design-patterns.md` §3.4 / §3.10) |
| 5 | **Direct event publish (no Outbox)** | grep `rabbitTemplate.send\|streamBridge.send` outside `outbox/` packages | Events go through the outbox writer (per `design-patterns.md` §3.5) |

Detector recipes: `reference/anti-pattern-detectors.md`

### 3. Output

Save report to `documents/audits/design-pattern-audit-[date].md` with structure:
- Score /100 (sum of 5 categories × 20)
- Per-module table (rows = modules, columns = 5 categories)
- Hotspot list — each violation = 1 row (file path + LOC + category + suggested fix)
- Comparison vs previous audit if exists

### 4. Hotspot → Gap Pipeline

For each P0/P1 hotspot, follow `audit-to-gap-pipeline.md` Step 2 (duplicate check) + Step 2.5 (state-check) + Step 3 (gap file). Do NOT fix in audit session.

## Grep Scope — CRITICAL

Multi-module repos hide false-negatives behind narrow grep scope. Narrow scope = silent false-positive risk.

```bash
# Preferred — broad with extension filter
grep -rnE "pattern" --include="*.java"

# Or — explicit submodules
grep -rn "pattern" */src/ --include="*.java"
```

## Calibration Notes

First-run baseline: expect ~50-65/100. Self-audits tend to overstate 15-20pts vs an independent specialist review. Trust the delta, not the absolute number. Don't treat a first low score as a regression.

Threshold rationale (`design-patterns.md` §3.1): ">500 lines = refactor required". Adjust ONLY if all modules cluster between 450-550 (legacy noise floor).

## Context Management

5-25K tokens depending on module count. Tactics:

1. **Pipe greps** — `| head -30` for cascade detectors (only need count, not full match list)
2. **Per-module staging** — score 2 modules deeply, apply pattern to rest
3. **Subagent split** — split by module group if >10K tokens

## Gotchas

- **Status switches in test files** — exclude `*Test.java`; tests legitimately exercise state transitions via switch
- **Lombok `@Builder` does NOT trigger Primitive Obsession** — generated code, not signature design choice
- **Outbox detector** — the outbox publisher's own `publish()` IS the right path; only flag `rabbitTemplate.send` OUTSIDE the outbox package
- **God Service threshold** — measure `wc -l` against the `.java` file, not LOC of method bodies
- **Calibration first run** — DO NOT file gaps until baseline reviewed; threshold may need module-specific calibration

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category with point deductions
- `reference/anti-pattern-detectors.md` — Grep one-liners for each category, false-positive avoidance
