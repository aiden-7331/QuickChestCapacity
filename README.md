# QuickChestCapacity 1.3.0 — Minecraft 26.2 Fabric

QuickChestCapacity shows an image-style chest capacity card while you look at a chest.

## Capacity
- Single chest: 27 slots × 64 = **1,728 items**
- Double chest: 54 slots × 64 = **3,456 items**
- The mod counts the actual number of items currently stored in the chest.

## Display
When you point at a scanned chest, a dark Minecraft-style panel appears above the hotbar with:
- SINGLE CHEST / DOUBLE CHEST
- Actual items / maximum items
- A segmented green → yellow → orange → red capacity bar
- EMPTY / PARTIALLY FULL / NEARLY FULL / FULL
- Percentage full

## Client-side limitation
On multiplayer servers, the client normally does not receive the inventory of a chest until it is opened. Open each chest once to scan it. The saved reading is then shown when you look at it later. Re-open the chest to refresh the saved count after its contents change.

## Building with GitHub Actions
Upload/replace these project files in your existing QuickChestCapacity GitHub repository. Your existing build workflow can build the mod. The finished JAR will be `quickchestcapacity-1.3.0.jar`.
