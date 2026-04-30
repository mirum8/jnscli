# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`jnscli` (binary name: `jns`) is a Spring Shell-based command-line client for Jenkins. It is compiled to a GraalVM native image, so all reflective access must be declared in `reflect-config.json`. The Maven artifactId is `jnscli` but the source root is `src/main/java/com/github/mirum8/jnscli` and the Spring Boot entry point is `JshellApplication`.

## Build & Run

The project uses Maven (`./mvnw`) and targets Java 21. There are two build modes:

- **JVM build / tests**: `./mvnw clean package` (also produces a Spring Boot fat jar).
- **Native binary**: `./mvnw clean native:compile -Pnative` — produces `target/jns`. This is the shipping format; `install.sh` and the Dockerfile both use it.
- **Run a single test**: `./mvnw test -Dtest=JenkinsAPITest` (or `-Dtest=ClassName#methodName`).
- **Run native tests**: `./mvnw test -PnativeTest`.

JaCoCo is wired into the build (`prepare-agent` + `report` during `prepare-package`).

The `local` Spring profile (`application-local.properties`) enables `spring.shell.interactive.enabled=true` for running an interactive shell from the IDE; the production binary runs commands non-interactively from argv.

## Architecture

### Command layer (Spring Shell)

Each user-facing command lives in its own package and consists of two classes: a `XxxCommand` annotated with `@Command` (Spring Shell) that only parses options, and a `XxxService` that does the work. `JshellApplication` uses `@CommandScan` to discover them. Current commands: `build`, `abort`, `connect`, `alias`, `info`, `error` (in `diagnose/`), `list`, `ai`. Add new commands by following the same Command/Service split.

`StartingBean` runs after Spring context init: it sets up file logging to `~/.config/jns/error.log`, validates settings, and — when invoked with no args — falls through to `ListService.listJobs()` so bare `jns` shows the job list.

### Jenkins integration

`jenkins/JenkinsAPI` and `jenkins/PipelineAPI` are the only places that talk to Jenkins. They use `java.net.http.HttpClient` (not RestTemplate/WebClient) plus `HttpRequestBuilder` from `http/`. JSON is parsed via Jackson with a shared `ObjectMapper` from `JenkinsApiUtils`. Domain records (`Job`, `Build`, `BuildInfo`, `WorkflowRun`, `QueueItem`, …) are Jackson-friendly Java records — keep them as records and don't add Lombok.

### Settings & job context

`SettingsService` persists config to `~/.config/jns/config` as a Java `Properties` file (server/username/key/aliases, plus AI settings written by `AiSettings.writeToProps`). It caches the parsed `Settings` after the first read. The path comes from `app.settings.directory` (`SettingsProperties`).

`context/JobsContext` keeps a separate `~/.config/jns/mapping` file that assigns numeric IDs to jobs. The `list` command rewrites it; `build`/`info`/`abort` resolve `%<n>` ID references against it via `JobDescriptorProvider` (`common/`).

### Long-running operations

`runner/CommandRunner` is the shared engine for "kick off, then poll until done with a progress bar/spinner" flows used by `build`, `abort`, etc. It owns a virtual-thread `ScheduledExecutorService`, a `RefreshableMultilineRenderer` (in `shell/`) for in-place TTY updates, and a `CommandParameters` builder describing the polling/completion/failure/timeout transitions. Reuse `CommandRunner.callWithSpinner` / `runWithSpinner` for short ops and the full `call(...)` form for build-style polling. **Do not** spin up your own threads or progress bars.

### AI subsystem

`ai/` is provider-agnostic: `AiClient` is the interface, `AiClientFactory` selects an implementation via pattern matching on `AiSettings` (sealed under `LlmSettings` with `Ollama` and `OpenAI` cases). Adding a new provider means: new `LlmSettings.Foo` record, new `FooClient implements AiClient`, new `FooSettingsPrompter`, register both in `AiClientFactory` and `AiSettingsPrompterFactory`. `--ai` flags on `build` / `error` route through `AiService` to summarise failures.

### Shell I/O

All terminal output goes through `shell/ShellPrinter`, prompts through `shell/ShellPrompter`, formatting via `shell/Theme` + `Symbols` + `Messages` (default for status lines), structured blocks via `shell/Section`, tables via `shell/Table`. Don't `System.out.println` — it bypasses the renderer and breaks spinners. Multi-line live updates must go through `RefreshableMultilineRenderer`. **See [`ui-design.md`](ui-design.md) for the full UI pattern catalog and rules — read it before adding any new command output.**

### Build parameters

`build/parameters/` handles Jenkins parameter prompting. `ParameterPrompterRegistry` maps Jenkins parameter classes to `ParameterPrompter` implementations (String, Boolean, Choice, Password, File, DynamicReferenced, ActiveChoicesReactive). Active Choices uses jsoup-based scraping of the Jenkins config UI (`activechoises/ActiveChoiceExtractor`) — the test fixtures in `src/test/resources/html` are real captured HTML.

## GraalVM constraints

Because the shipping artifact is a native image:
- New reflective access (Jackson on a new class, Spring proxying a new component, JDK proxies, etc.) usually needs an entry in `reflect-config.json`. If a test passes on the JVM but the native binary throws `MissingReflectionRegistrationError`, this is why.
- HTTPS works because `--enable-url-protocols=https` is set in `pom.xml` build args — keep it.
- Avoid dynamic classloading and `Class.forName` on user-supplied names.

## Conventions specific to this repo

- Domain types are `record`s; mappers/services are `@Component`/`@Service` constructor-injected.
- Config lives in `~/.config/jns/` (config, mapping, error.log). Never hardcode a different path — go through `FileUtil.resolveHomeDir` and `SettingsProperties`.
- The shell prompt is `jns:>` (set in `JshellApplication.getPrompt`).

### After Task Completion (don't ask for confirmations, just do)

- You MUST run `find-bugs` on the diff, then `/simplify`, `/security-review`, and `/sonar` — fix every issue each one
  reports. Never skip these steps. Run without confirmation.
