# QuickChestCapacity v1.3.2 — Live Hopper Update

This keeps the v1.3 bottom-centre HUD style and adds live chest capacity updates.

## New in 1.3.2
- The item number updates while hoppers add items.
- The item number updates while hoppers remove items.
- The percentage and segmented green/yellow/orange/red bar update with it.
- Single chests use 27 slots / 1728 items.
- Double chests use 54 slots / 3456 items.
- The v1.3 bottom-screen panel is unchanged in style.

## Important multiplayer note
Minecraft does not normally send a closed chest's inventory contents to a client. For live hopper updates, QuickChestCapacity now asks the logical server for the current count.

- **Singleplayer:** works automatically because the same mod JAR runs on the integrated server.
- **Multiplayer:** install the same QuickChestCapacity JAR on the Fabric server for live updates.
- **Server without the mod:** the old v1.3 "open once to scan" fallback still works, but hopper changes while the chest is closed cannot be known live.

## Build
Minecraft 26.2, Java 25, Fabric Loader 0.19.3, Fabric API 0.158.0+26.2.


## v1.3.2 visual tweak
- Slightly smaller capacity bar.
- Fixed extra space after the final bar segment so the segments fit the background exactly.
