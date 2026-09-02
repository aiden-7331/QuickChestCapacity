# QuickChestCapacity v1.3.9 — Customisable HUD + Live Hoppers

QuickChestCapacity shows how full the chest you are looking at is, while keeping the v1.3 segmented HUD style.

## New in 1.3.8 — draggable HUD + in-game settings menu
Press **K** while playing to open the QuickChestCapacity settings screen. The key binding also appears in **Options → Controls → Key Binds → QuickChestCapacity**, so players can change it.

Settings include:
- **HUD Size:** XXSmall, XSmall, Small, Normal, or Large.
- **Position:** Top Left, Top Centre, Top Right, Centre Left, Centre, Centre Right, Bottom Left, Bottom Centre, or Bottom Right.
- **Drag positioning:** click and drag the live HUD preview anywhere on the screen, then release to save.
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


## HUD sizes
The settings menu now includes **XXSmall**, **XSmall**, **Small**, **Normal**, and **Large**. XXSmall and XSmall use a compact layout so the text and capacity bar remain readable.


## v1.3.8
- Added a bottom-right resize handle in the K settings menu.
- Drag the HUD card to move it.
- Drag the gold corner handle to resize it smaller or larger.
- Resizing keeps the HUD proportions and saves automatically.
- Custom resize percentage is shown in the size button.


## v1.3.8 HUD editor changes
- HUD size is now a 1%-100% slider.
- Removed the four movement arrow buttons.
- Drag the HUD directly to position it.
- While dragging/resizing, the settings GUI hides so placement is easy to see.
- Release the mouse and the settings GUI immediately returns.
- Bottom-right resize handle still works and updates the slider value.
- The whole HUD is proportionally scaled, fixing text/number overlap at very small sizes.
- Reset and Done buttons have been moved higher in the menu.


## v1.3.9 — crisp drag preview
- Removed Minecraft's screen blur while the HUD is being dragged or resized.
- The settings GUI still hides during placement and returns when the mouse is released.
- The HUD panel becomes fully opaque only while dragging/resizing so text and the bar stay sharp and easy to position.
- Normal gameplay keeps the original slightly transparent HUD appearance.
