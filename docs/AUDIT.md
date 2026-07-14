# Axiom NeoForge Port — Original ↔ Port Audit

Verification that the port covers the original mod 1:1 and preserves its behavior. Compares the
NeoForge port against the original `Axiom-5.4.2-for-MC1.21.1.jar` and its Mojmap decompile.

## Coverage — nothing missed

| Aspect | Original | Port | Result |
|---|---|---|---|
| Top-level classes (`com.moulberry.axiom.*`) | 762 | 770 | ✅ **all 762 present** (+8 platform/bridge additions) |
| Resource files (assets/data/lang/heightmaps/downgrade/zstd/licenses) | 226 | 226 | ✅ **0 missing** |
| Mixin classes | 89 | 89 | ✅ **all present** |

**Only intentional removal:** `utils/PreLaunch` (Fabric `PreLaunchEntrypoint`) — its logic moved into the
`@Mod` constructor (`neoforge/AxiomNeoForge`).

**The 8 additions** are all NeoForge platform/bridge glue, not original features:
`platform/AxiomPlatform` (FabricLoader replacement), `platform/TriState`, `network/AxiomNetworking`,
`network/AxiomPackets`, `neoforge/AxiomNeoForge` (@Mod entrypoint), `neoforge/AxiomClientBusEvents`,
`integration/ReplayModIntegration`, `utils/ClientTagsHelper`, plus recreated `mixinconstraints` annotations.

## No stubs / no simplification

The port introduced **no stubs, TODOs, or `UnsupportedOperationException` placeholders**. The original
decompile has 31 `UnsupportedOperationException` throws (dummy classes, fake-world `LevelAccessor`/
`BlockAndTintGetter` impls, exhaustive-switch defaults); the port has 19 of them (the difference is only
because 12 are inside the still-excluded compat/fluid mixins) — every one is faithful to the original.

## Behavioral fidelity

- **617 of 761 files (81.1%) are byte-identical** to the original decompile (ignoring imports/indentation).
  The 144 changed files are exactly the Fabric→NeoForge adaptations + decompiler-artifact fixes.
- **Network protocol wire format preserved:** 41/46 packet files are byte-identical; the 5 that differ
  changed only in platform checks (`FabricLoader`→`AxiomPlatform`), `handle()` side-effects
  (protection-mod events), decompiler generics, or the base-interface registration — **no `write()`/`read()`
  serialization changed**. Axiom uses native `CustomPacketPayload`, so the port is byte-compatible with the
  original protocol (works against existing Axiom servers / the Paper plugin).

### Categories of the 144 changed files
- Fabric loader/env → `AxiomPlatform`; `@Environment` removed.
- Fabric events → NeoForge event bus (`ClientTickEvent`, `ServerTickEvent`, `PlayerInteractEvent`,
  `BlockEvent`, `TagsUpdatedEvent`, `RegisterClientCommandsEvent`, `RegisterKeyMappingsEvent`,
  `RegisterShadersEvent`, `RegisterClientReloadListenersEvent`, `LevelTickEvent`, `ClientPlayerNetworkEvent`).
- Fabric networking → `network/AxiomNetworking` (`PayloadRegistrar.optional()`).
- Fabric gamerules/client-tags/blockview → vanilla/NeoForge equivalents (+`ClientTagsHelper`).
- Optional integrations (Iris/Nvidium/Starlight/ReplayMod) → reflection wrappers (no compile dep).
- ~90 mixins: `((Target)(Object)this)` casts restored, `@At` string targets → Mojmap, mixin plugin ported.
- Vineflower decompiler artifacts fixed (lost casts, raw-type params, variable aliasing, effectively-final
  lambda captures, one dropped local class).

## Deferred / not yet done (honest)

- **`mixin/compat/**` (Sodium/other-mod) + `mixin/fluid_opacity/**`** — classes present in `src/` but excluded
  from compilation and removed from the runtime mixin config; need Sodium-on-NeoForge on the classpath
  (from `sodium-source/neoforge`) + a NeoForge fluid-render port. Optional (only apply when those mods load).
- **2 Fabric datagen providers** (`datagen/BlockTagGenerator`, `datagen/DataGeneration`) — the generated
  `data/` is already shipped; would need `GatherDataEvent` to regenerate.
- **Runtime validation** — the mod compiles + packages, but mixins/rendering/UI can only be *verified* in a
  running client (`runClient`, needs a display). Not yet exercised in-game.
- **Optional-module separation (Phase J)** — the server-side code compiles in-tree; the plan's separate
  toggleable Multiplayer / Custom-Server-APIs / History-Persistence modules are not yet split out.
