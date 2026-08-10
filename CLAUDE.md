# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands
- Build all modules: `./gradlew build`
- Run desktop application: `./gradlew lwjgl3:run`
- Run unit tests: `./gradlew test`
- Clean build artifacts: `./gradlew clean`
- Create runnable JAR for desktop: `./gradlew lwjgl3:jar`

## Coding Guidelines
### General Principles
- **Before Any Action**: Verify required files; zero assumptions; stop and ask if data is missing or ambiguous.
- **Communication**: Tight responses (bullet points/short sentences), no preambles, no unnecessary conclusions.
- **Before Coding**: State assumptions explicitly; surface tradeoffs; stop if unclear.
- **While Coding**: Minimum code that solves the problem; no speculative features or over-abstraction.
- **Surgical Changes**: Touch only what is requested; match existing style; no unrequested refactors.
- **Goal-Driven Execution**: Transform tasks into verifiable goals; provide brief plans with checkpoints.

## Project Architecture
This is a libGDX project using a multi-module Gradle structure.
- `core`: Contains the main application logic, game state, and shared resources.
- `lwjgl3`: The desktop-specific entry point and configuration using the LWJGL3 backend.
- `assets`: Contains UI skins, fonts, and other game assets.

### Game Engine & Logic (`core`)
The project implements a 2D dungeon crawler with the following key systems:

#### Entity Component System (ECS)
Uses the **Ashley** framework to manage game entities.
- **Components**: Data containers like `TransformComponent` (position), `VelocityComponent`, `RenderComponent`, `PlayerInputComponent`, and `PlayerStatsComponent`.
- **Systems**:
    - `StatsSystem`: Manages stamina regeneration and synchronizes movement speed from stats.
    - `PlayerInputSystem`: Maps user input to entity velocity.
    - `MovementSystem`: Handles physics and collision detection against the `DungeonMap`.
    - `RenderSystem`: Handles the visual drawing of entities.

#### Procedural Dungeon Generation
- **BSP Generation**: Uses Binary Space Partitioning to recursively divide the map into partitions, placing rooms in leaf nodes and connecting them with L-shaped corridors.
- **Map Structure**: `DungeonMap` stores the tile grid (Wall, Floor, Exit).
- **DungeonRenderer**: Responsible for drawing the generated map using `ShapeRenderer`.

#### Player Stats System
- **StatSheet**: Manages a combination of base stats (HP, ATK, MAG, DEF, SPD, STAMINA) and additive bonuses from equipment.
- **PlayerStatsComponent**: Tracks dynamic state such as `currentHp` and `currentStamina`, and handles damage calculation (mitigated by DEF).

#### Screen Management
- `Main`: Extends `Game` to manage the screen stack.
- `MainMenuScreen`: Entry point.
- `GameScreen`: Main loop where the ECS engine and Dungeon are initialized and updated.
- `GameOverScreen`: Triggered on player death.
- `StatsOverlay`: A UI layer drawn on top of the `GameScreen` to display real-time player statistics.
