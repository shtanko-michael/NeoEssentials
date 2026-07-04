# Help Permission Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `/help` list exactly the commands a player has permission for, by resolving each command's real permission node from a bundled reference instead of fabricating `neoessentials.<name>`.

**Architecture:** Ship `command_permissions.json` (already written under `docs/`) as a mod resource. A new read-only `CommandPermissionRegistry` parses it once into `name/alias → permission-node` lookups. `HelpCommand`'s visibility filter queries that registry instead of guessing the node. Registry is pure Java (no Minecraft types) so its parsing/lookup logic is unit-testable; the `HelpCommand` wiring is verified by build + in-game.

**Tech Stack:** Java 21, NeoForge 1.21.1 (moddev), Gson (provided by NeoForge), JUnit 5 (added by this plan, test-only), SLF4J.

## Global Constraints

- Java toolchain: **21** (`build.gradle` `java.toolchain.languageVersion`).
- Gson is **NeoForge-provided** for main code — do NOT add a Gson `implementation`/`jarJar` dependency (module conflict). Test code may use an explicit `testImplementation` Gson.
- New reference resource path (classloader form, no leading slash): `data/neoessentials/command_permissions.json`.
- Permission checks go through `com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(java.util.UUID, String)`.
- This is a **display-only** change: it never gates command execution (each command re-checks its own permission at runtime). Preserve existing behaviors: console (`uuid == null`) sees everything; holders of `neoessentials.admin` or `neoessentials.*` see the full list.

## Decisions (locked in with the requester)

1. **Move** `docs/command_permissions.json` into resources once (manual move, not a Gradle copy task).
2. `permission: null` in the reference ⇒ command is **NOT shown** to a regular player. Consequence: open/config-driven/op-only commands without a base node (e.g. chat channels `/g` `/l`, base `/chestshop`, `/powertooltoggle`, `/language`) disappear from `/help` for non-admins. Accepted.
3. **Phantom registry entries** (in `CommandRegistry` but absent from the reference — the `_phantom_registry_entries` list: `ac, amsg, clear, fw, killme, nickname, pong, tpacancel, whisper`) are **skipped for now** (not shown), with an explanatory code comment. Same code path also covers commands added after the reference snapshot.
4. **Safety fallback:** if the reference fails to load/parse, `/help` shows **all** commands (never blank the list over a resource error).

## File Structure

- Create `src/main/resources/data/neoessentials/command_permissions.json` — the reference, moved from `docs/`. Bundled into the jar; loaded at runtime.
- Delete `docs/command_permissions.json` — relocated (single source of truth in resources).
- Create `src/main/java/com/zerog/neoessentials/commands/CommandPermissionRegistry.java` — parses the reference once; exposes `isReady()`, `isKnown(name)`, `permissionFor(name)`. No Minecraft imports.
- Create `src/test/java/com/zerog/neoessentials/commands/CommandPermissionRegistryTest.java` — JUnit 5 tests for parsing + lookup + alias + null + phantom cases.
- Modify `build.gradle` — add `mavenCentral()` repo, JUnit 5 test deps, `test { useJUnitPlatform() }`.
- Modify `src/main/java/com/zerog/neoessentials/util/commands/HelpCommand.java:97-110` — replace the fabricated-node filter with a registry-backed one.

All paths are relative to `game/neoessentials/`.

---

### Task 1: Relocate the reference JSON into mod resources

**Files:**
- Create: `src/main/resources/data/neoessentials/command_permissions.json` (content identical to the current `docs/command_permissions.json`)
- Delete: `docs/command_permissions.json`

**Interfaces:**
- Consumes: nothing.
- Produces: classpath resource `data/neoessentials/command_permissions.json`, consumed by Task 3.

- [ ] **Step 1: Move the file (preserves exact content)**

Run from `game/neoessentials/`:

```bash
mkdir -p src/main/resources/data/neoessentials
git mv docs/command_permissions.json src/main/resources/data/neoessentials/command_permissions.json
```

(If `git mv` fails because the file is untracked, use a plain `mkdir -p` + `mv` instead.)

- [ ] **Step 2: Verify it lands on the build output classpath**

Run: `./gradlew processResources`
Then: `ls build/resources/main/data/neoessentials/command_permissions.json`
Expected: the file exists (proves it will be inside the jar and reachable via the classloader).

- [ ] **Step 3: Confirm nothing else referenced the old docs path**

Run: `git grep -n "docs/command_permissions.json" -- . || echo "no references"`
Expected: `no references` (the file was documentation only; this plan is its first code consumer).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore(neoessentials): move command_permissions.json into mod resources"
```

---

### Task 2: Add a JUnit 5 test harness

**Files:**
- Modify: `build.gradle` (`repositories { }` and `dependencies { }` blocks; add a `test` task config)

**Interfaces:**
- Consumes: nothing.
- Produces: a working `./gradlew test` task on the JUnit 5 platform, used by Task 3.

**Context:** The project currently has no test source set. The `repositories { }` block already declares project-level repos (flatDir, CurseMaven), so project repositories are permitted (the monorepo is not `FAIL_ON_PROJECT_REPOS`) — adding `mavenCentral()` here is safe. Main code keeps using NeoForge's Gson; the test classpath gets its own explicit Gson so tests never depend on the modular runtime.

- [ ] **Step 1: Add `mavenCentral()` to the repositories block**

In `build.gradle`, inside `repositories { ... }`, add as the first entry:

```groovy
    mavenCentral()
```

- [ ] **Step 2: Add JUnit 5 (and test-only Gson) dependencies**

In `build.gradle`, inside `dependencies { ... }`, append:

```groovy
    // Unit tests for pure (non-Minecraft) logic such as CommandPermissionRegistry.
    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    // Main code uses NeoForge-provided Gson; tests run on the plain classpath, so give them their own.
    testImplementation 'com.google.code.gson:gson:2.10.1'
```

- [ ] **Step 3: Enable the JUnit platform for the `test` task**

In `build.gradle`, add at top level (e.g. just after the `dependencies { }` block):

```groovy
test {
    useJUnitPlatform()
}
```

- [ ] **Step 4: Verify the test task resolves and runs (no tests yet)**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL` (task is `NO-SOURCE` or up-to-date — it just must not error on missing config/deps).

- [ ] **Step 5: Commit**

```bash
git add build.gradle
git commit -m "build(neoessentials): add JUnit 5 test harness"
```

---

### Task 3: `CommandPermissionRegistry` (TDD)

**Files:**
- Create: `src/main/java/com/zerog/neoessentials/commands/CommandPermissionRegistry.java`
- Test: `src/test/java/com/zerog/neoessentials/commands/CommandPermissionRegistryTest.java`

**Interfaces:**
- Consumes: classpath resource `data/neoessentials/command_permissions.json` (Task 1); JUnit + Gson (Task 2).
- Produces (relied on by Task 4):
  - `CommandPermissionRegistry.getInstance()` → singleton (loads once).
  - `boolean isReady()` → `true` when the reference parsed successfully.
  - `boolean isKnown(String command)` → `true` if the command/alias appears in the reference (including entries whose `permission` is `null`).
  - `String permissionFor(String command)` → the base node, or `null` if the command has no node **or** is unknown. Lookups are case-insensitive.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/zerog/neoessentials/commands/CommandPermissionRegistryTest.java`:

```java
package com.zerog.neoessentials.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPermissionRegistryTest {

    @Test
    void loadsReferenceAndResolvesCategorizedNodes() {
        CommandPermissionRegistry r = CommandPermissionRegistry.getInstance();
        assertTrue(r.isReady(), "reference JSON should load from the classpath");
        assertEquals("neoessentials.teleport.home", r.permissionFor("home"));
        assertEquals("neoessentials.chat.msg", r.permissionFor("msg"));
        assertEquals("neoessentials.economy.balance", r.permissionFor("balance"));
    }

    @Test
    void resolvesAliasesToTheSameNode() {
        CommandPermissionRegistry r = CommandPermissionRegistry.getInstance();
        assertEquals("neoessentials.economy.balance", r.permissionFor("bal"));
        assertEquals("neoessentials.chat.msg", r.permissionFor("tell"));
    }

    @Test
    void lookupIsCaseInsensitive() {
        CommandPermissionRegistry r = CommandPermissionRegistry.getInstance();
        assertEquals("neoessentials.teleport.home", r.permissionFor("HOME"));
    }

    @Test
    void nullPermissionCommandsAreKnownButHaveNoNode() {
        CommandPermissionRegistry r = CommandPermissionRegistry.getInstance();
        assertTrue(r.isKnown("permissions"));
        assertNull(r.permissionFor("permissions"));
    }

    @Test
    void phantomEntriesAreNotKnown() {
        CommandPermissionRegistry r = CommandPermissionRegistry.getInstance();
        assertFalse(r.isKnown("ac"));
        assertNull(r.permissionFor("ac"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests "com.zerog.neoessentials.commands.CommandPermissionRegistryTest"`
Expected: FAIL — compilation error `cannot find symbol: class CommandPermissionRegistry`.

- [ ] **Step 3: Write the minimal implementation**

Create `src/main/java/com/zerog/neoessentials/commands/CommandPermissionRegistry.java`:

```java
package com.zerog.neoessentials.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Read-only index mapping every NeoEssentials command (and its aliases) to its
 * base permission node, loaded once from the bundled
 * {@code data/neoessentials/command_permissions.json} reference.
 *
 * <p>This is the source of truth {@link CommandRegistry} lacks: CommandInfo stores
 * only name/description/aliases, so {@code HelpCommand} used to fabricate
 * {@code "neoessentials." + name} and hid every command that uses a categorized
 * node ({@code neoessentials.<category>.<command>}). Used by /help only — a
 * display concern; command execution enforces permissions independently.
 */
public final class CommandPermissionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandPermissionRegistry.class);
    private static final String RESOURCE = "data/neoessentials/command_permissions.json";

    private static volatile CommandPermissionRegistry instance;

    /** command-or-alias (lowercase) -> permission node; only non-null nodes stored. */
    private final Map<String, String> permissionByName = new HashMap<>();
    /** every command-or-alias (lowercase) present in the reference, incl. null-permission ones. */
    private final Set<String> knownNames = new HashSet<>();
    /** false if the reference could not be loaded/parsed — callers must degrade gracefully. */
    private boolean ready;

    private CommandPermissionRegistry() {
        load();
    }

    public static CommandPermissionRegistry getInstance() {
        CommandPermissionRegistry local = instance;
        if (local == null) {
            synchronized (CommandPermissionRegistry.class) {
                local = instance;
                if (local == null) {
                    local = new CommandPermissionRegistry();
                    instance = local;
                }
            }
        }
        return local;
    }

    private void load() {
        try (InputStream in = CommandPermissionRegistry.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LOGGER.error("{} not found on classpath — /help will fall back to showing all commands", RESOURCE);
                return;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject commands = root.getAsJsonObject("commands");
            for (Map.Entry<String, JsonElement> entry : commands.entrySet()) {
                JsonObject cmd = entry.getValue().getAsJsonObject();
                JsonElement perm = cmd.get("permission");
                String node = (perm != null && !perm.isJsonNull()) ? perm.getAsString() : null;
                register(entry.getKey(), node);
                JsonElement aliases = cmd.get("aliases");
                if (aliases != null && aliases.isJsonArray()) {
                    for (JsonElement a : aliases.getAsJsonArray()) {
                        register(a.getAsString(), node);
                    }
                }
            }
            ready = true;
            LOGGER.info("Loaded command permission index: {} names, {} with a permission node",
                knownNames.size(), permissionByName.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load {} — /help will fall back to showing all commands", RESOURCE, e);
        }
    }

    private void register(String name, String node) {
        String key = name.toLowerCase();
        knownNames.add(key);
        if (node != null) {
            permissionByName.put(key, node);
        }
    }

    /** true once the reference parsed successfully; when false, callers should not hide commands. */
    public boolean isReady() {
        return ready;
    }

    /** true if the command/alias appears in the reference (even with a null permission). */
    public boolean isKnown(String command) {
        return knownNames.contains(command.toLowerCase());
    }

    /** base permission node for a command/alias, or null if it has none (or is unknown). */
    public String permissionFor(String command) {
        return permissionByName.get(command.toLowerCase());
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test --tests "com.zerog.neoessentials.commands.CommandPermissionRegistryTest"`
Expected: PASS — 5 tests green. (If it fails with `NullPointerException` at `getResourceAsStream`, the reference did not land on the test classpath — re-check Task 1 Step 2.)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/zerog/neoessentials/commands/CommandPermissionRegistry.java \
        src/test/java/com/zerog/neoessentials/commands/CommandPermissionRegistryTest.java
git commit -m "feat(neoessentials): add CommandPermissionRegistry backed by the reference JSON"
```

---

### Task 4: Rewire the `/help` visibility filter

**Files:**
- Modify: `src/main/java/com/zerog/neoessentials/util/commands/HelpCommand.java:97-110`

**Interfaces:**
- Consumes: `CommandPermissionRegistry.getInstance()`, `.isReady()`, `.isKnown(name)`, `.permissionFor(name)` (Task 3); existing `PermissionAPI.hasPermission(UUID, String)`.
- Produces: corrected `/help` listing. No new outbound interface.

**Context:** The current filter is at [HelpCommand.java:97-110]. It builds `perm = "neoessentials." + cmd.getName()` and shows the command if that fabricated node (or `neoessentials.*`) is held. Replace the whole `.filter(...)` lambda body. `HelpCommand` already imports `com.zerog.neoessentials.commands.CommandRegistry` and has a `LOGGER`; add one import for `CommandPermissionRegistry`.

- [ ] **Step 1: Add the import**

In `HelpCommand.java`, next to the existing `import com.zerog.neoessentials.commands.CommandRegistry;`, add:

```java
import com.zerog.neoessentials.commands.CommandPermissionRegistry;
```

- [ ] **Step 2: Replace the filter block**

Replace this existing block (lines ~95-109):

```java
        // Build list of commands accessible to this player
        // Show all registered commands; individual commands handle their own permission checks
        List<CommandRegistry.CommandInfo> accessible = allCommands.stream()
            .filter(cmd -> {
                // Console can see everything; for players check admin or generic perm
                if (uuid == null) return true;
                String perm = "neoessentials." + cmd.getName().toLowerCase();
                // Admin can see all
                if (PermissionAPI.hasPermission(uuid, "neoessentials.admin")) return true;
                // Try the command-specific permission; if not explicitly denied, show it
                return PermissionAPI.hasPermission(uuid, perm)
                    || PermissionAPI.hasPermission(uuid, "neoessentials.*");
            })
            .sorted(Comparator.comparing(CommandRegistry.CommandInfo::getName))
            .collect(Collectors.toList());
```

with:

```java
        // Build the list of commands visible to this player. Visibility is resolved from
        // the bundled command→permission reference (CommandPermissionRegistry): each command
        // is shown only if the player holds its real permission node. This replaces the old
        // fabricated "neoessentials.<name>" check, which hid every command using a categorized
        // node (neoessentials.<category>.<command>). Display-only: execution is still gated by
        // each command's own permission check.
        CommandPermissionRegistry perms = CommandPermissionRegistry.getInstance();
        List<CommandRegistry.CommandInfo> accessible = allCommands.stream()
            .filter(cmd -> {
                // Console sees everything.
                if (uuid == null) return true;
                // Admins / wildcard holders see the full list regardless of per-command nodes.
                if (PermissionAPI.hasPermission(uuid, "neoessentials.admin")
                    || PermissionAPI.hasPermission(uuid, "neoessentials.*")) return true;
                // Safety net: if the reference failed to load, do not blank the help list —
                // fall back to showing every command.
                if (!perms.isReady()) return true;

                String node = perms.permissionFor(cmd.getName());
                if (node != null) {
                    return PermissionAPI.hasPermission(uuid, node);
                }
                // node == null covers two cases, both HIDDEN by decision:
                //   1. The command IS in the reference with "permission": null — open,
                //      config-driven, or op-only (e.g. chat channels, /permissions root).
                //      Project decision: do not show these to regular players.
                //   2. The command is a PHANTOM registry entry (see _phantom_registry_entries
                //      in command_permissions.json: ac, amsg, clear, fw, killme, nickname,
                //      pong, tpacancel, whisper) or was added after the reference snapshot.
                //      Skipped for now; logged at debug so staleness is discoverable.
                if (!perms.isKnown(cmd.getName())) {
                    LOGGER.debug("/help: '{}' has no entry in command_permissions.json (phantom or stale reference)",
                        cmd.getName());
                }
                return false;
            })
            .sorted(Comparator.comparing(CommandRegistry.CommandInfo::getName))
            .collect(Collectors.toList());
```

- [ ] **Step 3: Verify it compiles and unit tests still pass**

Run: `./gradlew compileJava test`
Expected: `BUILD SUCCESSFUL`; the 5 `CommandPermissionRegistryTest` tests stay green.

- [ ] **Step 4: In-game verification (real behavior, non-admin player)**

Build and launch a server:

Run: `./gradlew runServer`
Then, in the server console, op yourself is NOT what we want — instead grant a single categorized node to a test player and confirm the fix:

1. Join with a non-op client account (or a second profile).
2. From the server console, grant one categorized node without admin/wildcard, e.g.:
   `permissions user <player> add neoessentials.economy.balance`
   (Use the mod's own `/permissions` command, or your LuckPerms setup.)
3. As that player run `/help` and page through it.

Expected:
- `balance` (and its alias `bal`) now **appears** — previously hidden because `/help` checked `neoessentials.balance`.
- A categorized command whose node the player does NOT hold (e.g. `home` → `neoessentials.teleport.home`) does **not** appear.
- A `permission: null` command (e.g. a chat channel) does **not** appear for this player (accepted consequence of Decision 2).
- Granting `neoessentials.admin` makes the full list reappear (bypass preserved).

Record the observed `/help` output for the reviewer.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/zerog/neoessentials/util/commands/HelpCommand.java
git commit -m "fix(neoessentials): resolve /help visibility from real permission nodes"
```

---

## Self-Review

**Spec coverage:**
- Move JSON to resources once → Task 1. ✓
- `CommandPermissionRegistry` reading the file, knowing command↔permission + aliases → Task 3 (`permissionFor`, alias registration, `isKnown`). ✓
- `/help` uses the registry for visibility → Task 4. ✓
- `permission: null` ⇒ not shown → Task 4 Step 2 (`node == null` returns `false`). ✓
- Phantom entries skipped + code comment → Task 4 Step 2 comment + `isKnown` debug log. ✓

**Type consistency:** `getInstance()`, `isReady()`, `isKnown(String)`, `permissionFor(String)` are declared in Task 3 and used with those exact names/signatures in Task 4. ✓

**Placeholder scan:** every code step contains full code; no TODO/TBD. ✓

**Risk notes for the executor:**
- If `./gradlew test` cannot resolve JUnit, confirm `mavenCentral()` was added (Task 2 Step 1) and that the monorepo `settings.gradle` does not force `FAIL_ON_PROJECT_REPOS` (the pre-existing `repositories { }` block indicates it does not).
- If the Task 3 test throws NPE at resource load, the JSON is not on the test classpath — recheck Task 1 Step 2 (`build/resources/main/...`).
- `runServer` requires the moddev run config; if unavailable, use `./gradlew buildDev` and load the jar into the existing `../dev-server`.
