# QuickChestCapacity

A small client-side Fabric mod for Minecraft Java Edition 26.2.

## What it does

When a chest-style container is open, the mod displays:

- `Chest Capacity: 18/27`
- `Free Slots: 9`

It counts occupied slots, not the total number of individual items.
A double chest automatically has a capacity of 54 slots.

## Target versions

- Minecraft: 26.2
- Java: 25
- Fabric Loader: 0.19.3+
- Fabric API: 0.156.0+26.2
- Fabric Loom: 1.17

## Source layout

The main mod code is here:

`src/client/java/com/aiden/quickchestcapacity/QuickChestCapacityClient.java`

## Build

A Java 25 + Gradle/Fabric environment is required to compile source code into a mod JAR.
The resulting JAR will be placed in `build/libs/` after a successful Gradle build.
