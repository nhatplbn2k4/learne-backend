# Anti-Pattern Detectors — Grep Recipes

Concrete one-liners per category. Multi-module-safe.

> **Adapt the search roots.** The recipes below use a `$MODULES` placeholder for your
> module source roots. Set it once per session:
>
> ```bash
> MODULES="*/src/main/java"          # most multi-module repos
> # or explicit: MODULES="service-a/src/main/java service-b/src/main/java"
> ```

---

## 1. God Service / Class

```bash
# All Service.java files sorted by LOC, exclude tests
find . -path '*/src/main/java/*' -name '*Service.java' \
  -not -path '*/test/*' \
  -exec wc -l {} + 2>/dev/null | sort -rn | head -20
```

Threshold: rows with LOC > 500 = hotspot.

False positives to skip:
- `*Configuration.java`, `*Properties.java` — config classes, not services
- Generated code (`target/generated-sources/**`) — exclude `target/`
- Abstract base classes (`Abstract*Service.java`) — review case-by-case

---

## 2. Status Switch / If Cascade

```bash
# Cascade detection — at least 2 status comparisons in same file
grep -rln --include='*.java' \
  -E '(if|else if).*[sS]tatus\s*==' $MODULES \
  | xargs -I{} bash -c 'count=$(grep -cE "(if|else if).*[sS]tatus\s*==" "$1"); [ "$count" -ge 2 ] && echo "$count  $1"' _ {} \
  | sort -rn | head -20

# Switch on Status type — exclude HTTP layer + response.status() style calls
grep -rln --include='*.java' -E 'switch\s*\(.*[sS]tatus' $MODULES \
  | xargs -I{} bash -c '
      grep -lE "switch\s*\(\s*[a-zA-Z_]+\.[sS]tatus\(\)" "$1" >/dev/null && exit 0
      echo "$1"' _ {} \
  | head -20
```

Threshold: file with ≥3 cascade lines OR ≥1 switch with ≥3 case branches.

False positives:
- Test fixtures that legitimately walk states (`given().status(SCHEDULED).when()...`)
- Single-shot guards (`if (status == ARCHIVED) return null;`) — not cascades
- **HTTP `response.status()` switches** (HTTP-client error decoders, RestTemplate handlers) — not domain status. The xargs filter above strips files where every match is the `*.status()` method call, not a domain field.

---

## 3. Primitive Obsession

```bash
# Public method signatures with primitive domain types
grep -rn --include='*.java' \
  -E 'public.*\b(String\s+(color|colour|hex|email|phone|address)|BigDecimal\s+(amount|price))\b' \
  $MODULES \
  | grep -v '/test/' \
  | head -30

# Entity fields (require value object instead) — multiline-aware
# Note: multiline mode catches @Column on previous line; works on most shells.
grep -rzPo --include='*.java' \
  '@Column[^;]{0,200}\n\s*private\s+String\s+(email|phone|color|hex|address|amount)\b' \
  $MODULES 2>/dev/null \
  | tr '\0' '\n' | head -20
```

Manual review guidance:
- **Boundary DTOs at REST/JSON layer** (`*Request.java`, `*Response.java`, `dto/**`) — accepted as translation; do NOT flag even if validated by `@Size`/`@Email`. Add `dto/` to exclude path if scan adds noise.
- **JPA entity fields** (`@Entity`, `@Column`-decorated POJOs in `entity/`) → flag, suggest converter / embedded value object.
- **`BigDecimal` in invoice/payment DTOs** — accepted (currency-as-primitive at boundary); entity-side `Money` value object preferred.

---

## 4. Leaky Abstraction (Vendor types in domain)

> Replace `<Vendor>` with your external API's type prefix (e.g. `Stripe`, `Twilio`,
> an LLM client). Repeat per vendor.

```bash
# Vendor types outside the adapter package — accept /client/ as adapter convention too
grep -rn --include='*.java' \
  -E '\b(<Vendor>(Request|Response|Context|Client))\b' $MODULES \
  | grep -vE '/(adapter|client|client/external|integration/<vendor>)/' \
  | grep -vE '/[A-Za-z]*Config\.java:' \
  | head -20
```

False positives:
- Adapter package itself (good — that's where vendor types belong)
- **`/client/` packages** are accepted as adapter convention even though the design-patterns rule's Mandatory Patterns table says "Adapter". A `<Vendor>Client` under `.../client/` IS isolating vendor types correctly; package-name choice is a separate convention question.
- Test mocks using a real vendor type to validate the adapter (acceptable in `*AdapterTest.java`)
- Bean wiring code in `*Config.java` that constructs vendor clients (config-time, not domain-time)

---

## 5. Direct Event Publish (No Outbox)

Reference rule: `.claude/rules/design-patterns.md` §3.5 + §3.5.1 Outbox Bypass Policy (documented Exceptions). The detector must distinguish silent bypass (BANNED) from documented exceptions (allowed).

```bash
# RabbitMQ direct publish outside outbox package:
#  - Skip javadoc/comment-only references (lines starting with " *")
#  - Recognize documented "fast-path" / "outbox is the reliability net" markers
#  - Skip *Config.java (bean-wiring exception)
grep -rn --include='*.java' \
  -E '\brabbitTemplate\.(send|convertAndSend)\b' $MODULES \
  | grep -vE '/outbox/' \
  | grep -vE ':\s*\*\s' \
  | grep -vE '/[A-Za-z]*Config\.java:' \
  | xargs -I{} bash -c '
      line="$1"
      file=$(echo "$line" | cut -d: -f1)
      # Documented fast-path exception — file contains marker comment near the call
      if grep -qE "(outbox is the reliability net|fast-path|best-effort fast-path)" "$file"; then
          exit 0
      fi
      echo "$line"
  ' _ {} \
  | head -20

# Spring Cloud Stream direct
grep -rn --include='*.java' \
  -E '\bstreamBridge\.send\b' $MODULES \
  | grep -vE '/outbox/|:\s*\*\s|/[A-Za-z]*Config\.java:' \
  | head -20

# Kafka direct
grep -rn --include='*.java' \
  -E '\bkafkaTemplate\.send\b' $MODULES \
  | grep -vE '/outbox/|:\s*\*\s|/[A-Za-z]*Config\.java:' \
  | head -20
```

False positives to skip (baked into the pipeline above where possible):
- **Fast-path with outbox backup** (per `design-patterns.md` §3.5.1): file contains marker comment `outbox is the reliability net` OR `fast-path` near the call.
- **Bean wiring** (`*Config.java`, `*Configuration.java`) and javadoc comment examples (lines starting with ` *`)
- **Test fixtures** (`*Test.java`, `src/test/**` paths) — already excluded by `--include='*.java'` + path
- The outbox publisher itself (the legitimate sender)
- In-process Spring `ApplicationEventPublisher` — not cross-service, OK

---

## Composite Run

For one-shot baseline:

```bash
MODULES="*/src/main/java"
{
  echo "=== God Services (>500 LOC) ==="
  find . -path '*/src/main/java/*' -name '*Service.java' \
    -not -path '*/test/*' -exec wc -l {} + 2>/dev/null | sort -rn | awk '$1>500'
  echo
  echo "=== Status switch density (top 10) ==="
  grep -rlE 'switch\s*\(.*[sS]tatus' --include='*.java' $MODULES | head -10
  echo
  echo "=== Vendor type leaks ==="
  grep -rlE '<Vendor>(Request|Response)' --include='*.java' $MODULES \
    | grep -v adapter | head -10
  echo
  echo "=== Direct event publish ==="
  grep -rlE 'rabbitTemplate\.(send|convertAndSend)' --include='*.java' $MODULES \
    | grep -v outbox | head -10
} > /tmp/dp-audit-raw.txt

wc -l /tmp/dp-audit-raw.txt
```

Then human-curate raw → scored audit report.

---

## Calibration Notes

- **Shell glob quirk**: globs like `*/src/main/java` work; `**/src/main/java` does NOT (no recursive glob in some shells). Use `find` for recursion.
- **Performance**: full scan of a mid-size repo ~3-5s; safe for in-session use.
- **First-run heuristic**: if a category returns 0 hits, re-run with broader scope before claiming clean — a false-zero is worse than a false-positive.
- **Calibrate after first baseline**: the detectors above already strip the common false-positive classes (HTTP `response.status()` switches, vendor client under `/client/`, fast-path-with-outbox-backup, bean-wiring `*Config.java`). If a new false-positive class shows up, tighten the matching grep and document it here so the next cycle compares like-for-like.
