# Axiom → NeoForge 1.21.1 — Port Checklist (100% coverage tracker)

Legend: ☐ not started · ◐ in progress · ☑ done · ⊘ excluded (with reason)

Original inventory measured from `Axiom-5.4.2-for-MC1.21.1.jar`: ~1,193 mod classes in 40 packages,
~90 mixins, ~43 network packets, ImGui UI, Sodium-coupled rendering.

**Execution strategy (chosen): bulk translation → subsystem verification.** The codebase is densely
coupled (config↔keybinds↔ImGui-editor↔packets), so modules don't compile in isolation without stubs.
Approach: translate all sources Fabric→NeoForge, get the whole mod compiling, then verify + confirm
subsystem by subsystem. End state is no-stubs / 1:1.

### ✅ MILESTONE — BUILD SUCCESSFUL (compile + package)
The whole mod **compiles cleanly** (0 errors, from 1108 at bulk-copy) on JDK 21 / NeoForge 21.1.220 and
**packages** to `build/libs/axiom-5.4.2.jar` (~46 MB, all shaded libs + AT + mixin config). Covers the
entire core (entrypoint, networking, events, config, i18n, keybinds, tools, editor, rendering manager,
world-editing, blueprints, display entities, capabilities, masks) + the ~85 vanilla-target mixins.
**Still deferred / to verify:** `mixin/compat/**` (Sodium/other-mod) + `fluid_opacity/**` (removed from the
mixin config, pending Sodium-on-NeoForge + a fluid-render port); the 2 Fabric datagen providers; and —
critically — **runtime validation** (`runClient`), since mixins/rendering only truly verify in-game.

**Primary reference: `work/sources-mojmap/`** — all 763 files decompiled after intermediary→Mojmap
remap (reads as NeoForge code). Translation surface: ~731 files Mojmap-clean (port ~as-is), 32 use
Fabric APIs, 90 mixins hand-ported. See BUILDING.md → "remap pipeline" / "translation surface".

## Phase A — Bootstrap  ☑
- ☑ Extract JAR (`work/extracted`, 6326 entries)
- ☑ Decompile mod classes → `work/sources` (763 .java)
- ☑ Gradle ModDevGradle scaffold (`net.neoforged.moddev` 2.0.141, JDK 21, neoForge 21.1.220)
- ☑ `neoforge.mods.toml` (range `[21.1.220,)`, MC `[1.21.1,1.21.2)`)
- ☑ Access widener → Access Transformer (57 entries; 4 post-1.21.1 excluded — see `work/tools/at_conversion_report.txt`)
- ☑ Resource passthrough (assets, data, default_heightmaps, downgrade, zstd_dictionaries, licenses, bluenoise.bin, legacy.json)
- ☑ Empty mod compiles against NeoForge (BUILD SUCCESSFUL); AT validated by MC recompile
- ☐ `runClient` smoke test (needs a display; not runnable in headless CI)

## Bulk translation progress (whole-mod compile)
All 763 Mojmap sources copied into `src/main/java`; bundled deps wired (embedded-libs incl. imgui
fork + natives, Lattice, axiomclientapi — all remapped intermediary→Mojmap). Mixins + datagen
excluded from the source set initially. Mechanical fixups done: 1 Vineflower indy-concat artifact,
homoglyph package (10 files), **529 residual intermediary member names → 0** (flat token subst,
`work/tools/fix_residual_names.py`).

**Compile error burn-down:** 1108 → … → 114 → 94 → 86 → **74**. Since 94: optional integrations
`AxiomClient`(ReplayMod)/`StarlightHelper` → reflection; `HeightmapApplier` Vineflower variable-aliasing
bug fixed (iterator vs map). **48 of the remaining 74 are mixin-blocked** (`WindowExt`/`NativeImageExt`/
`*Accessor`/`mixin` package). Non-mixin remainder ~26: `AxiomMixinPlugin` (mixin infra), decompiler
generics (`ServerCustomBlocks` Enum→Direction, `BlueprintIo`/`CreateDisplayEntityScreen` CAP casts,
`BlockBuffer`, misc tool files).

➡ **The mixin phase is now the dominant blocker.** Scope: un-exclude 89 mixins; convert `axiom.mixins.json`
+ register in `neoforge.mods.toml`; port `AxiomMixinPlugin` (Fabric `IMixinConfigPlugin` + drop
mixinconstraints); fix `@At`/`@Shadow`/`@Redirect` targets (residual intermediary class names inside `@At`
strings; `class_` residuals remain only here); the `*Ext`/`*Accessor` duck interfaces that non-mixin code
uses; Sodium-compat mixins against `sodium-source/neoforge`.

**Compile error burn-down (earlier):** 1108 → … → 226 → 166 → 140 → 114 → 94. Since 166: blockview classes
(dropped Fabric `FabricBlockView`/`RenderAttachedBlockView`, keep vanilla `BlockAndTintGetter`); `ItemList`
(`TagsUpdatedEvent`); **20 Vineflower duplicate-variable artifacts** fixed (`fix_dup_vars.py`);
`ScriptArgument` switch re-scoped with per-case braces; deleted the leftover Fabric `PreLaunch.java`.

**Of the remaining 94, ~40 are mixin-blocked** (reference the excluded `mixin/` package / accessors /
`NativeImageExt`) — they resolve once mixins are ported. The rest (~54): `HeightmapApplier` (missing
symbols), optional integrations `StarlightHelper` (Starlight → reflection like Iris/Nvidium),
`AxiomMixinPlugin` (Fabric `IMixinConfigPlugin` + mixinconstraints → NeoForge), `BlueprintIo`/misc
decompiler generics. **Next major subsystem: the 89 mixins** (@At/@Shadow/@Redirect → Mojmap, mixin
config, `AxiomMixinPlugin`, Sodium compat) — the last big compile blocker before runtime.

**Compile error burn-down (earlier):** 1108 → … → 264 → 238 → 226 → 166. Since 226: AxiomServer
(`ServerTickEvent.Post`/`PlayerEvent.PlayerChangedDimensionEvent`); AxiomServerboundSetBlock (Fabric
interaction-callbacks → NeoForge event posting `BlockEvent.BreakEvent`/`PlayerInteractEvent.RightClickBlock`
for protection-mod compat); BlockList + CmdStringConverter (`TagsUpdatedEvent`; client tags reimplemented
as `utils/ClientTagsHelper` reading bundled tag JSON); datagen exclusion narrowed to the 2 Fabric providers
so `MaterialBlockTagGenerator` etc. compile.

Remaining **166** across ~20 small files (6–12 each): tool decompiler-generics (`ScriptArgument`,
`HeightmapApplier`), blockview (`ChunkedBlockRegion`/`ChunkRenderOverrider`/`EmptyBlockAndTintGetter`),
`AxiomMixinPlugin` (Fabric mixin plugin), antlr `MaskParser`, `ItemList` (`CommonLifecycleEvents`), misc.
Then mixins (89).

**Compile error burn-down (earlier detail):** 1108 → 786 → 592 → 568 → 374 → 340 → 264 → 238 → 226. Since 264:
**Axiom entrypoint** restructured (`onInitialize` → `initCommon`/`initClient`/`registerServerLifecycle`,
wired to `FMLCommonSetupEvent`/`FMLClientSetupEvent` in `AxiomNeoForge`; server tick → `LevelTickEvent.Post`;
pick-block → `getCustomPickBlock()` (mixin-wired); reload listeners → `RegisterClientReloadListenersEvent`);
**ShaderManager** ported (`RegisterShadersEvent` + `ResourceManagerReloadListener`, dropped `getFabricId`).

Remaining **226**: AxiomServerboundSetBlock 20 (`UseBlockCallback`/`PlayerBlockBreak`) · BlockList 20
(`ClientTags`) · AxiomServer 16 (server lifecycle events) · ScriptArgument/HeightmapApplier (decompiler
generics) · blockview (`ChunkedBlockRegion`/`ChunkRenderOverrider`) · `AxiomMixinPlugin` (Fabric mixin
plugin) · antlr `MaskParser` · misc. Then mixins (89).

Since 374: gamerules
(`AxiomGameRules` → vanilla `GameRules.register`/`BooleanValue.create` + manual AT); Iris/Nvidium →
reflection wrappers; decompiler artifacts fixed (`(Property<Comparable<?>>)`→raw cast in 5 files;
spurious `(Enum)` casts in FamilyMap; reconstructed the `BaseWoodSet` local class Vineflower dropped).

Remaining **264**: Axiom entrypoint 28 (events/resource-reload/pick-block) · AxiomServerboundSetBlock 20
(`UseBlockCallback`) · BlockList 20 (`ClientTags`) · AxiomServer 16 (server events) · ShaderManager 10
(resource reload) · blockview (`ChunkedBlockRegion`/`ChunkRenderOverrider`) · `AxiomMixinPlugin` (Fabric
`IMixinConfigPlugin` → NeoForge) · antlr `MaskParser` · a few tool-file decompiler generics. Then mixins (89).

**Compile error burn-down (superseded detail):** Since 592: LWJGL nfd bundled;
`FabricLoader`/`@Environment`/`getGameDir` mechanically ported; `platform/TriState`; **networking layer**
(`network/AxiomNetworking`+`AxiomPackets`, base interfaces); **ClientEvents.java fully ported**
(commands→`RegisterClientCommandsEvent`/`CommandSourceStack`, 19 keybinds→`RegisterKeyMappingsEvent` via
`neoforge/AxiomClientBusEvents`, tick→`ClientTickEvent.Pre/Post`, connection→`ClientPlayerNetworkEvent`,
shutdown-hook config save, version→`DefaultArtifactVersion`, ReplayMod→reflection wrapper).

Remaining **374**, spread (no dominant file): ServerCustomBlocks 32 · Axiom 28 (entrypoint events) ·
FamilyMap 24 (private-access → AT) · AxiomGameRules 20 (Fabric gamerules → NeoForge) · AxiomServerboundSetBlock
20 (`UseBlockCallback`) · BlockList 20 (`ClientTags`/`TagsLoaded`) · AxiomServer 16 · then blockview
(`FabricBlockView`/`RenderAttachedBlockView`), resource-reload (`ResourceManagerHelper`), integrations
(`IrisApi`/`NvidiumAPI`), mixin accessors (`DisplayAccessor`/`MultiPlayerGameModeAccessor`), a few `get(int,int)`
decompiler generics. All bounded per-subsystem.

Earlier burn-down detail:
`@Environment` mechanically ported (→ `AxiomPlatform`, +`gameDir()`); `getGameDir` leaf files;
Fabric-compatible `platform/TriState`. Remaining 592 are file-prioritized:
- **ClientEvents.java — 194** (client commands `FabricClientCommandSource`/`ClientCommandManager`,
  keybinds `KeyBindingHelper`, networking, tick events, version nag). Needs the networking + keybind
  + command subsystems; porting it collapses ~1/3 of all errors.
- ServerCustomBlocks 32 · Axiom 28 · FamilyMap 24 (private-access → AT) · AxiomGameRules 20 (gamerules)
  · AxiomServerboundSetBlock 20 (`UseBlockCallback`/`PlayerBlockBreak`) · BlockList 20 (`ClientTags`/`TagsLoaded`)
  · AxiomServer 16 · packets base 12+12 (Phase C) · blockview (`FabricBlockView`/`RenderAttachedBlockView`).
- Optional-integration APIs (`IrisApi`/`NvidiumAPI`/`StarLightLightingProvider`) — compileOnly / reflection.

Earlier snapshot (superseded) — original 786 categories:
- ~230 Fabric-API symbols across the **32 Fabric files** to port: `FabricLoader`/`EnvType`/`ModContainer`
  (→ `AxiomPlatform`), `FabricClientCommandSource`/`ClientCommandManager` (→ NeoForge client commands),
  `KeyBindingHelper` (→ `RegisterKeyMappingsEvent`), `ServerPlayNetworking`/`ClientPlayNetworking`/
  `PayloadTypeRegistry` (→ payloads, Phase C), `ResourceManagerHelper`, `GameRuleRegistry/Factory`,
  `FabricBlockView`/`RenderAttachedBlockView`, `ClientTags`/`TagsLoaded`, `TriState`, `SimpleSynchronous…`.
- Mixin **accessor interfaces** referenced by non-mixin code (`DisplayAccessor`, `MultiPlayerGameModeAccessor`) — include when mixins land.
- Optional integration APIs (`IrisApi`, `NvidiumAPI`) — add as compileOnly / reflection wrappers.
- `NativeFileDialog`/`NFDFilterItem` — re-bundle LWJGL **nfd** module (wrongly excluded as provided).
- Scattered Vineflower artifacts: ~30 "variable already defined", ~8 "Enum→DyeColor" erasure, 26 `CustomBlockState.getProperty` generics, 16 `FamilyMap` private-access (needs AT entries).

## Phase B — Core platform  ◐
- ☑ **Platform layer** `platform/AxiomPlatform` — NeoForge-backed FabricLoader replacement (configDir, isModLoaded, isDevelopment, dist, modVersion)
- ☑ **Embedded third-party libs** repackaged verbatim from the 5.4.2 jar (`libs/axiom-embedded-libs.jar`, 3684 classes: auth0 java-jwt, LuaJ+BCEL, JNoise, Configurate+Typesafe HOCON, Jackson, Caffeine, zstd-jni, JHLabs, EvalEx, quickhull3d, GeAnTyRef, image-scaling) — wired to compile classpath + shaded into final jar
- ☑ `utils/Authorization` ported (commercial-license verifier; homoglyph package normalized) — compiles, validates embedded auth0/gson linkage
- ☑ `@Mod` entrypoint logs user agent (PreLaunch parity)
- ◐ `@Mod` client/server split (`Dist.CLIENT` client class) — skeleton present, grows per phase
- ☐ Port `com.moulberry.axiom.Axiom` main init + `AxiomClient` (touches most phases; assembled incrementally)
- ☐ `configuration/` (28) — reuse Lattice annotations + Configurate HOCON serialization; port/replace thin MC config-screen (`createConfigScreen`) later
- ☐ Lattice: repackage MC-independent classes; port `createConfigScreen` → NeoForge `IConfigScreenFactory`
- ☐ `i18n/` (5) + language-manager hook
- ☐ `versioning/` (+ `input_events`)
- ☐ Keybindings (`editor/keybinds`, `Keybinds`) → `RegisterKeyMappingsEvent`
- ☐ ImGui native loading strategy (imgui.moulberry92 fork) — Phase D overlap

## Phase C — Networking protocol  ◐
- ☑ **Base layer ported** — `network/AxiomNetworking` (NeoForge `PayloadRegistrar`, `.optional()` for
  non-NeoForge-server compat) + `network/AxiomPackets.registerAll()` (all 40 registrations) wired to
  `RegisterPayloadHandlersEvent`. Both base interfaces (`AxiomClientboundPacket`/`AxiomServerboundPacket`)
  rewritten off Fabric `*PlayNetworking`. Compiles; packets resolve.
- ☑ Axiom uses native `CustomPacketPayload` → **wire format identical** (no channel/format change).
- ☑ Client `Hack*` packets are empty in the client jar (big-payload split is server-side → Phase J).
- ☐ Runtime: NeoForge serverbound 32 KiB limit vs. large `SetBuffer` — may need raw-packet send for
  big buffers + Paper compat (runtime refinement, flagged).
- ☐ `SupportedProtocol` version negotiation · `restrictions/` (4)
- ☐ Unit tests: codec round-trip; live handshake vs. AxiomPaperPlugin

## Phase D — ImGui UI  ☐
- ☐ imgui-java dependency/natives · `MixinWindow` · input routing (`MixinMouseHandler`, `MixinKeyMapping`)
- ☐ end-of-frame render hook · windows/panels/drag-drop

## Phase E — Rendering (full parity incl. Sodium)  ☐
- ☐ `render/` (55) · `core_rendering/` (12) · `rasterization/` (28)
- ☐ `mixin/render.*` (Mojmap) · `RenderLevelStageEvent`
- ☐ `mixin/compat.*` (Sodium) against `sodium-source/neoforge` · conditional sodium/no-sodium (`AxiomMixinPlugin`)
- ☐ `fluid_opacity.*`, `block_attributes.*`, `custom_block.*`, `tooltip.*` mixins

## Phase F — World-editing core  ☐
- ☐ buffers · `mask/` (102) · `operations/` (30) · `world_modification/` (28, undo/history)
- ☐ `capabilities/` (17: Bulldozer, ReplaceMode, FastPlace, NoClip, AngelPlacement, EnhancedFlight, BuildSymmetry, Tinker, SpecialPlace, ArcballCamera)
- ☐ `block_maps/` (25) · `custom_blocks/` (17) · `clipboard/` (22) · `brush_shapes/` (20) · `pather/` (13) · `scaling/` (4) · `noise/` (13)
- ☐ Hotbar swapper · flight speed (`AxiomServerboundSetFlySpeed`)

## Phase G — Tools  ☐
`tools/` (187) — 30+ subpackages:
- ☐ path · modelling · magic_select · smooth · stamp · text · sculpt_draw · script_brush · modify · generator
- ☐ weld · shape · ruler · freehand · floodfill · extrude · annotation · painter · noise_painter · lasso_select
- ☐ gradient_painter · fluidball · distort · biome_painter · slope · shatter · roughen · rock · elevation · box_select · blend
- ☐ `buildertools/` (18)

## Phase H — Editor mode & objects  ☐
- ☐ `editor/` (193: main menu bar, windows, presets, tutorial) · `screen/` (28) · `gizmo/` (12)
- ☐ `displayentity/` (14) · `annotations/` (18) · `marker/` (4) · `blueprint/` (client) · `packets/blueprint`

## Mixin phase (infrastructure done; per-mixin fixes in progress)
- ☑ Recreated mixinconstraints annotations `@IfModLoaded`/`@IfModAbsent` (`com.moulberry.mixinconstraints.annotations`).
- ☑ Ported `AxiomMixinPlugin` (Fabric `IMixinConfigPlugin`): drops MixinExtras/mixinconstraints bootstrap;
  evaluates `@IfMod*` conditions via ASM + NeoForge `LoadingModList` (class-level; method-level = later refinement).
- ☑ `axiom.mixins.json` copied (compat level → JAVA_21), registered in `neoforge.mods.toml` `[[mixins]]`.
- ☑ 98 residual intermediary class names in `@At`/descriptor strings → Mojmap (`fix_mixin_class_refs.py`); 0 `class_` left.
- ☑ Mixins un-excluded from source (except `mixin/compat/**` — Sodium/other-mod targets, need those on classpath).
- ☑ **Mixin `((Target)(Object)this)` casts restored** (`fix_mixin_this_casts.py`, 18 files) —
  `(Type)this`/`this instanceof`/`this ==` → `(Object)` inserted. Cleared ~60 errors.
- ☑ **`*Ext` duck-interface casts** on final MC classes (`WindowExt`/`NativeImageExt`/`BlockItemStatePropertiesExt`)
  → cast through `(Object)`.
- ☑ Fabric fluid-render mixins (`fluid_opacity/**`) excluded pending a NeoForge fluid-render port.
- ☑ **~50 decompiler-generic errors fixed** (per-line): lost casts (`Object`→`Tag`/`CompoundTag`/`NbtTarget`,
  `(String)`/`(Boolean)`/`(Direction)`/`(T)value`), `ImmutableList<X>`→`List<CustomBlockState>` (`(List<?>)` casts),
  raw-type parameterization (`ObjectHeapPriorityQueue<>`, `Maps.<String,String>`, `CycleButton.<T>builder`,
  `new Factory<>`), interface-super (`FunctionDictionaryIfc.super`), and several Vineflower variable-aliasing
  bugs (`blockEntitiesTag`→`x`, `radius`→`radiusx`, `blockState`→`blockStatex`, `result`→`resultx`).
- ◐ **Down to a handful of Vineflower lambda-capture artifacts** ("local variable must be final or effectively
  final") — a reassigned local (`search`/`i`/`blockRegion`) captured by a filter lambda; fix = an effectively-final
  copy. Fixed so far: MoveBuilderTool, Placement, ItemList, TagListWidget, BlueprintBrowserWindow, BlockList.
  Surfacing one file per compile; near BUILD SUCCESSFUL. (Final compile confirmation pending — command tooling
  was briefly unavailable.)
- ☐ `mixin/compat/**` Sodium mixins vs `sodium-source/neoforge`; `fluid_opacity` NeoForge port; method-level `@IfMod*`.
- ☐ Runtime validation (mixins only truly verify when applied — needs `runClient`).

## Phase I — Client integrated-server support  ☐
- ☐ Server mixins: `MixinServerGamePacketListenerImpl`, `MixinEntityServer`, `MixinThreadedLevelLightEngine`
- ☐ `no_physical_trigger.*` · `vanilla_structure.*` · `world_properties.*` mixins
- ☐ `world_properties/` (30) package

## Phase J — Optional server modules (NeoForge dedicated server)  ☐
- ☐ **J1 Multiplayer**: block buffers, `operations/`, permission/`SectionPermissionChecker`, `Hack*` receive, integrations (WorldGuard/PlotSquared/CoreProtect)
- ☐ **J2 Custom Server APIs**: `Axiom*Event` → NeoForge bus, `ServerAnnotations`, blueprint registry, `AxiomReflection` equiv, AxiomClientAPI compat
- ☐ **J3 History Persistence**: server undo/history persistence
- ☐ Each toggleable; core builds/runs without them

## Phase K — Integration, tests, docs  ☐
- ☐ Reconcile this checklist (nothing missed/simplified) · error/edge-case parity
- ☐ Tests (protocol, buffers, masks) · docs (architecture, optional modules, build/usage)

## Excluded / deferred (with reasons)
- ⊘ 4 access-widener entries (`RenderType.create(RenderSetup)`, 2× `GuiGraphics.submitBlit(GpuTextureView…)`, `GuiGraphics.deferredTooltip`) — target post-1.21.1 render/GPU APIs absent in 1.21.1.
- Bundled `mixinextras-fabric` — dropped (NeoForge ships MixinExtras). `mixinconstraints` — replaced by NeoForge-native conditional mixin logic.
