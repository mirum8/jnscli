# UI Design Patterns

This document defines the rules for terminal output in `jnscli`. Every command must follow these patterns so that the visual surface stays uniform.

## Component map

| Component                             | Responsibility                                                          |
|---------------------------------------|--------------------------------------------------------------------------|
| `shell/ShellPrinter`                  | Raw `println` / `print` to the JLine `Terminal`. Last resort only.       |
| `shell/ShellPrompter`                 | All interactive input (string, password, single-select, yes/no).         |
| `shell/Theme`                         | Color/style primitives: `label`, `header`, `success`, `failure`, `warning`, `accent`, `dim`, `bold`. |
| `shell/Symbols`                       | Glyphs (Unicode + ASCII fallback): `ok`, `fail`, `warn`, `running`, `activeDot`, `pending`, `emptyMark`, `info`, `folder`, spinner frames, etc. |
| `shell/Messages`                      | One-line status messages: `success`, `failure`, `warning`, `info`, `empty`, plus `*Text` variants returning a `String`. **Default for any standalone status line.** |
| `shell/Section`                       | Multi-line blocks: `header`, `field(label, value)` (auto-aligned colons), `line`, `blank`, `divider`. |
| `shell/Table`                         | Tabular output with column alignment and width-truncation.               |
| `shell/RefreshableMultilineRenderer`  | In-place updates for spinners and progress bars.                         |
| `runner/CommandRunner` + `Spinner` + `BuildProgressBar` | Long-running ops with live progress.                  |
| `util/StatusFormatter`                | Renders `Status` enum values with the right color.                       |

## Rules

### 1. Use `Messages` for every standalone status line

Don't hand-build `theme.success(symbols.ok()) + " " + msg` — call `Messages.success(msg)`. Same for failure/warning/info/empty. If you need a `String` to embed elsewhere, use the `*Text` variants (`successText`, `failureText`, `warningText`, `infoText`, `emptyText`).

| Type      | Method                              | Glyph (unicode / ascii) | Example                              |
|-----------|-------------------------------------|-------------------------|--------------------------------------|
| Success   | `messages.success("…")`             | `✓` / `[OK]`            | `✓ Connection established`           |
| Failure   | `messages.failure("…")`             | `✗` / `[X]`             | `✗ Job foo not found`                |
| Warning   | `messages.warning("…")`             | `!` / `[!]`             | `! Job foo is not running`           |
| Info      | `messages.info("…")`                | `·` / `.`               | `· Build cancelled`                  |
| Empty     | `messages.empty("…")`               | `—` / `-` (dimmed)      | `— No builds found.`                 |

### 2. Use `Section` for multi-field blocks

Inside a section, use `field(label, value)` — the builder pads labels so colons line up automatically across all fields. Use `header(title)` for the block title and `divider()` between repeated entries.

```java
section.builder()
    .header("Build #" + buildInfo.number())
    .field("Started By", buildInfo.startedBy().orElse("Unknown"))
    .field("Status", statusFormatter.colored(buildInfo.status()))
    .build();
```

Field labels: **Title Case with spaces** (`Started At`, `Build Number`). Never camelCase (`StartedAt`).

For empty-state lines inside a section (e.g. "No builds found"), use `messages.emptyText(...)` rather than hand-indenting strings.

### 3. Use `Table` for any list of records

Two columns or more, multiple rows → `Table` (see `JobListTableFormatter` and `AliasService.list()`). Bold the headers via `theme.bold(...)`. Render through `RefreshableMultilineRenderer`.

If the table would be empty, print `messages.empty("No X configured.")` instead.

### 4. Long-running operations go through `CommandRunner`

For any "kick off then wait" operation, build a `CommandParameters` and call `commandRunner.call(...)` / `showProgress(...)` / `callWithSpinner(...)`. The spinner and progress bar render through `Messages` already — your `onSuccess` / `onFailure` callbacks should return `messages.successText(...)` / `messages.failureText(...)`.

Never spin up your own thread or call `Thread.sleep` in a UI loop.

### 5. Status enum values go through `StatusFormatter`

Don't print `status.name()` directly. Use `statusFormatter.colored(status)` so SUCCESS/FAILED/IN_PROGRESS each get the right color.

### 6. Glyphs come from `Symbols`

If you need an icon, add it to `Symbols` (with both Unicode and ASCII fallback) and inject `Symbols`. Never hard-code the glyph in another class.

The active running dot for progress bars is `Symbols.activeDot()` (`●` / `*`). The "build is in progress" indicator next to a job in lists is `Symbols.running()` (`•` / `*`). Pending/skipped stages: `Symbols.pending()` (`○` / `o`).

### 7. Don't bypass the renderer

- No `System.out.println` / `System.err.println` — they fight the spinner and break ANSI cursor management.
- Single output line → `Messages` (preferred) or `ShellPrinter`.
- Multi-line live update → `RefreshableMultilineRenderer`.
- Section/table → `Section.builder().build()` then `shellPrinter.println(...)` or `renderer.render(...)`.

### 8. Errors

User-input errors (job not found, file not found, etc.) should `throw new IllegalArgumentException(...)`. The global `exception/CustomExceptionResolver` catches every uncaught exception and renders it as `messages.failureText(...)` — so all CLI errors look the same. Don't catch + print yourself unless you actually want to recover.

### 9. Indentation

- `Section.field(...)` adds two-space indent automatically.
- `Table` adds two-space indent automatically.
- Top-level `messages.*` lines start at column 0.
- Don't manually prepend `"  "` (two spaces) to strings — that double-indents inside sections.

### 10. Terminal capability respect

`TerminalCapabilities` decides ANSI support and Unicode support. Never assume either — they're already woven into `Theme`, `Symbols`, `Section`, `Table`, `Messages`. If you add a new component that emits styling, take `TerminalCapabilities` as a constructor arg and check `supportsAnsi()` / `supportsUnicode()` before writing escapes or non-ASCII chars.

## Cheat sheet

| You want to…                            | Use                                              |
|-----------------------------------------|--------------------------------------------------|
| Print a one-off success                 | `messages.success("Saved.")`                     |
| Print a one-off error                   | `messages.failure("Connection failed: …")`       |
| Tell the user nothing was found         | `messages.empty("No builds found.")`             |
| Print a labeled block                   | `section.builder().header(…).field(…).build()`   |
| Print a list                            | `Table.render(...)` via `RefreshableMultilineRenderer` |
| Wait for Jenkins with a spinner         | `commandRunner.callWithSpinner("Fetching…", …)`  |
| Wait for a build with a progress bar    | `commandRunner.showProgress(CommandParameters…)` |
| Ask the user a question                 | `shellPrompter.promptString` / `promptSelectFromList` / `promptForYesNo` |
| Color a `Status` enum                   | `statusFormatter.colored(status)`                |
| Add a new glyph                         | Add a method to `Symbols` (unicode + ascii)      |
