# QuickChestCapacity v1.3.4 — Customisable HUD + Live Hoppers

QuickChestCapacity shows how full the chest you are looking at is, while keeping the v1.3 segmented HUD style.

## New in 1.3.4 — in-game settings menu
Press **K** while playing to open the QuickChestCapacity settings screen. The key binding also appears in **Options → Controls → Key Binds → QuickChestCapacity**, so players can change it.

Settings include:
- **HUD Size:** Small, Normal, or Large.
- **Position:** Top Left, Top Centre, Top Right, Centre Left, Centre, Centre Right, Bottom Left, Bottom Centre, or Bottom Right.
- **Fine position controls:** move the HUD left, right, up, or down in 5-pixel steps.
- **Reset:** return to the default Normal / Bottom Centre layout.
- Settings are saved in `config/quickchestcapacity.properties` and persist after restarting Minecraft.

## Existing features
- Live item number updates while hoppers add or remove items when the server side of the mod is available.
- Percentage and segmented green/yellow/orange/red capacity bar update live.
- Single chests: 27 slots / 1728 items.
- Double chests: 54 slots / 3456 items.
- The segmented bar fills cleanly to the end with no extra trailing gap.

## Multiplayer note
Minecraft does not normally send a closed chest's contents to the client.

- **Singleplayer:** live updates work automatically because the JAR runs on the integrated server.
- **Multiplayer:** install the same QuickChestCapacity JAR on the Fabric server for live hopper updates.
- **Server without the mod:** the open-once scan fallback still works, but hopper changes while the chest is closed cannot be known live.

## Build
Minecraft 26.2, Java 25, Fabric Loader 0.19.3, Fabric API 0.158.0+26.2.
