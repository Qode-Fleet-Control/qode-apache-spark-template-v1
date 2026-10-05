# Apache Spark template

Provisioned from [`Qode-Fleet-Control/fleet-template-v1`](https://github.com/Qode-Fleet-Control/fleet-template-v1) — the fleet
lifecycle contract (`bin/`, `fleet.conf`, deploy workflows) with a
local-mode Apache Spark job laid on top.

Apache Spark 4.2 (Scala 2.13 artifacts) on Java 21, Maven build. `world.qode.app.WordCount` starts a `SparkSession` in local mode (`local[*]`, inside the one JVM — no cluster), counts the words of a bundled text (or of the file given as the first argument) with the Dataset API, prints the top ten and exits.

## Origin

Hand-written — Spark ships no project generator. Shaped after the Spark "Quick Start"
guide's self-contained Java application (a `SparkSession`, a `Dataset<String>`, Maven with
`spark-sql_2.13`), run with plain `java -jar` instead of `spark-submit`.

## Verified

**Not yet verified end to end on docker.** On 2026-10-05 the shared docker host's disk sat at
0-1 GB free for over 90 minutes (other builds were running), under the 6 GB floor this
scaffold's verification requires, so the `docker compose` build/run check was not run.
Run it before trusting the image:

    docker compose build && docker compose run --rm app              # must exit 0

What did pass, on 2026-10-05:

- `mvn -B package` **with the test suite** in `maven:3.9-eclipse-temurin-21` (the Dockerfile's
  build image) — compiles, tests green, artifacts produced.

## Run it

**This repo is not a service.** It is a job: the image's default command runs the Spark job in local mode (`java -jar /app/app.jar`)
and exits 0 on success (non-zero on failure). `START_CMD` and `DOCKER_START_CMD`
are empty and nothing listens on `$PORT`, so on the fleet `bin/run` builds the
image and stops there.

**With docker:**

    docker compose build
    docker compose run --rm app          # runs the job
    docker compose run --rm -e SPARK_MASTER=local[2] app   # pick the master

**Without docker** — a JDK 21 and Maven 3.9 (`mvn`) on `PATH`:

| step | command |
|---|---|
| install | `mvn -B -q dependency:go-offline` |
| build | `mvn -B -q package` |
| run the job | `java -jar target/app.jar [input-file]` |

If you add an HTTP endpoint, listen on `0.0.0.0:$PORT` and serve at `/`, then set
`PORT`, `HEALTH_PATH`, `START_CMD` and `DOCKER_START_CMD` in `fleet.conf` and
publish the port in `compose.yaml` (see the HTTP templates).

## Layout

- `src/main/java/world/qode/app/WordCount.java` — the job. `SPARK_MASTER` overrides `local[*]`.
- `src/main/resources/input.txt` — the bundled input; `log4j2.properties` — Spark's logging turned down to WARN.
- `src/test/java/...` — unit test of the tokenizer (run by the image build).
- `pom.xml` — `spark-sql` at compile scope (not `provided`: there is no cluster to provide it), `target/app.jar` with its runtime jars in `target/lib`; the jar manifest's `Add-Opens` carries the JDK internals Spark needs on Java 17+ (what `spark-submit` would otherwise pass).

## What differs from stock output

- No generator exists; everything above is hand-written (see Origin).
- Runs as `java -jar`, not `spark-submit`: the job is self-contained in local mode, so the image needs only a JRE (Spark's jars are in `lib/`). The Spark UI is off (`spark.ui.enabled=false`).
- Added the fleet harness: `bin/`, `fleet.conf`, `Dockerfile`, `compose.yaml`, `.dockerignore`, `.gitignore`, `.github/workflows/`, `docs/fleet-lifecycle.md`.
