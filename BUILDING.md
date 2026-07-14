# Building the Axiom NeoForge port

## Toolchain
- **JDK 21** (project-local `../jdk-21.0.11+10`). Gradle must run on JDK 21, not the system Java 8.
- **Gradle 8.12** via the wrapper (`./gradlew`).
- **ModDevGradle** `net.neoforged.moddev` 2.0.141.
- **NeoForge** 21.1.220 (compile floor); runtime range `[21.1.220,)` targeting 21.1.220–230+.

## Build
```bash
export JAVA_HOME="E:/Projects/Axiom-port/jdk-21.0.11+10"    # Git Bash
./gradlew build --no-daemon
```
PowerShell:
```powershell
$env:JAVA_HOME = "E:\Projects\Axiom-port\jdk-21.0.11+10"
.\gradlew.bat build --no-daemon
```

Run the client (needs a display): `./gradlew runClient`. Dedicated server: `./gradlew runServer`.

## Layout
- `src/main/java/com/moulberry/axiom/**` — ported mod code (package structure preserved 1:1 with the original).
- `src/main/resources/META-INF/neoforge.mods.toml` — mod metadata, dependency ranges, AT + (later) mixin declarations.
- `src/main/resources/META-INF/accesstransformer.cfg` — generated from the original access widener (see below).
- `src/main/resources/{assets,data,default_heightmaps,downgrade,zstd_dictionaries,licenses}` — passthrough resources.
- `docs/PORT_CHECKLIST.md` — living 100%-coverage tracker.

## Reference / tooling (in `../work/`)
- `work/extracted/` — full unpacked original JAR.
- `work/sources/` — first-pass decompile (MC refs in **intermediary**). Superseded by:
- **`work/sources-mojmap/`** — PRIMARY porting reference: 763 files decompiled from the mod jar
  after remapping intermediary→Mojmap. Reads as idiomatic NeoForge code (e.g. `buf.readUUID()`),
  so MC references match NeoForge runtime names. Residual `class_/method_/field_` (~630) are
  concentrated in the 90 mixin files (their `@At` string targets / `@Shadow` sigs are hand-ported).
- `work/tools/build_at.py` — access widener → Access Transformer converter (intermediary→obf→mojmap).
  Regenerate: `python work/tools/build_at.py`. Report: `work/tools/at_conversion_report.txt`.

### Regenerating the Mojmap source base (remap pipeline)
1. `python work/tools/build_mappings.py` → composes `intermediary-to-mojmap.tiny` (8254 classes /
   41243 methods / 37679 fields) from the Fabric intermediary tiny + Mojang proguard mappings.
2. tiny-remapper the mod jar, **with an intermediary-namespace MC jar on the classpath** (critical —
   a Mojmap classpath leaves ~16k method refs unremapped; the intermediary MC jar drops that to ~256):
   ```
   java -jar work/tools/tiny-remapper.jar work/axiom-only.jar work/remapped/axiom-mojmap.jar \
     work/tools/intermediary-to-mojmap.tiny intermediary mojmap \
     <fabric-loom cache>/minecraft-merged-intermediary-1.21.1-*.jar
   ```
3. `java -jar work/tools/vineflower.jar work/remapped/axiom-mojmap.jar work/sources-mojmap`

### Translation surface (bulk-translation strategy)
- **~731 files**: pure MC/mod code, Mojmap-clean — port largely as-is (modulo Lattice/imgui refs).
- **32 files** use Fabric APIs directly: `loader.api` (→ `AxiomPlatform`), `api` (ModInitializer/EnvType),
  `networking.v1` (→ NeoForge payloads), `event.lifecycle`/`event.player` (→ NeoForge events),
  `resource`, `command.v2`, `datagen.v1`, `blockview`/`rendering`/`fluid`, `gamerule`, `tag.client`.
- **90 mixins**: hand-port `@At`/`@Shadow`/`@Redirect` targets to Mojmap + conditional (Sodium) config.

## Notes
- Bundled `mixinextras` is dropped (NeoForge ships it); `mixinconstraints` is replaced by NeoForge-native
  conditional mixin logic. Bundled libs (imgui-java, lattice, JNoise, LuaJ) are added via `jarJar` in later phases.
- 4 access-widener entries targeting post-1.21.1 render/GPU APIs are intentionally excluded (see checklist).
