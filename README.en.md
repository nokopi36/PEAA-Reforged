# PEAA Reforged

[日本語](README.md) | **English**

A NeoForge add-on for **ProjectE** that rebuilds **ProjectE Advanced Alchemy (PEAA)**, written by Ryokusitai for Minecraft 1.7.10.

None of the 1.7.10 code is carried over. Its behaviour was documented first and then implemented again against the current ProjectE API. The textures and display names are the original's.

| | |
|---|---|
| Supported | Minecraft 1.21.1 / NeoForge |
| Mod id | `peaa_reforged` |
| Depends on | **[ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) 1.1.0 or later (required)** |
| Licence | [MMPL_J 1.0.1](LICENSE) |
| Original | [PEAA](https://www.curseforge.com/minecraft/mc-mods/projecte-advanced-alchemy) by Ryokusitai, itself an adaptation of EEAA by AK |

## Download

Grab the jar from [Releases](https://github.com/nokopi36/PEAA-Reforged/releases) and drop it into `mods/` alongside ProjectE. It is needed on **both the client and the server**.

## What it adds

| | |
|---|---|
| **Energy Collector MK4 / MK5** | Treats an AEGU directly above as full sunlight: a constant 320 and 1,280 EMC/s |
| **Alchemical Energy Generating Unit / Advanced AEGU / Ultimate AEGU** | 40, 1,000 and 20,000 EMC/s. Surround an Energy Condenser MK2 with 25 or more and the group starts generating |
| **Ring of the Space** | Double-tap jump to fly (four speed settings), right click to teleport within 30 blocks, immunity to fall damage |
| **Matter furnaces** | The Dark Matter Furnace always doubles ores, and both furnaces push output in every direction except up |
| **Gem Boots** | Airborne acceleration is suppressed while a ring is carried (configurable) |
| Smaller things | Water and lava orbs destroy each other, the Energy Condenser MK2 gets the original's texture, and an optional EMC mapper for the Ultimate AEGU (off by default) |

## Building

```bash
./gradlew build             # compile and build the jar
./gradlew runClient         # launch the client
./gradlew runGameTestServer # run the game tests (33 of them)
```

ProjectE publishes no Maven artifacts, so it is pulled from **CurseMaven** automatically. Nothing has to be placed by hand — clone and build.

## How it was built

This is not a conversion of the original source. The original's behaviour was written down as a specification, [`docs/SPEC.md`](docs/SPEC.md), and the mod was implemented from that. Every deliberate departure from it is recorded in [`docs/DEVIATIONS.md`](docs/DEVIATIONS.md) as D-001 to D-021, with the reasoning.

Where ProjectE's own behaviour had to change, the first choice was its API, its events or a resource override. Only two places needed a Mixin.

Numeric behaviour — EMC generation rates, ore doubling chances, the ring's drain — is pinned by 33 game tests.

## Documentation

| Path | Contents |
|---|---|
| [`docs/SPEC.md`](docs/SPEC.md) | Specification of the original mod; the source of truth |
| [`docs/DEVIATIONS.md`](docs/DEVIATIONS.md) | Deliberate departures from the specification, and why |
| [`docs/CREDITS.md`](docs/CREDITS.md) | Credits and what was found out about the licensing |
| [`docs/ASSETS.md`](docs/ASSETS.md) | Where each of the original's textures ended up |
| [`docs/SETUP.md`](docs/SETUP.md) | Development setup and the pitfalls worth knowing |
| [`CLAUDE.md`](CLAUDE.md) | Rules the implementation follows |

## Licence and credits

Released under **MMPL_J 1.0.1**, the licence the original PEAA is distributed under. As that licence requires, the complete source is available at no cost. The full text and the reasoning behind the choice are in [LICENSE](LICENSE); the licensing research is in [docs/CREDITS.md](docs/CREDITS.md).

- Original mod: **PEAA** by Ryokusitai
- Original concept: **EEAA** by AK
- Depends on: **ProjectE** by sinkillerj and contributors (MIT)

This is an unofficial port. The authors of PEAA, EEAA and ProjectE are not involved in its development and cannot support it — please report problems on [Issues](https://github.com/nokopi36/PEAA-Reforged/issues).
