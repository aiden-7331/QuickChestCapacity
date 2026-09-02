# QuickChestCapacity 1.4.0 — Minecraft 26.2 Fabric

QuickChestCapacity now renders the capacity indicator directly on the front of the chest in the 3D world instead of using a large screen HUD popup.

## Capacity
- Single chest: 27 slots × 64 = **1,728 items**
- Double chest: 54 slots × 64 = **3,456 items**
- Counts the actual number of items currently stored in the chest.

## Chest-mounted display
When you aim at a scanned chest, the front of the chest shows:
- Actual items / maximum items
- A segmented green → yellow → orange → red capacity bar
- EMPTY / PARTIALLY FULL / NEARLY FULL / FULL
- Percentage full
- Double-chest indicators are centred across both connected chest halves.

The old bottom-centre HUD popup has been removed.

## Client-side limitation
On multiplayer servers, the client normally does not receive a chest inventory until that chest is opened. Open each chest once to scan it. Re-open it later to refresh the remembered item count after its contents change.

## Building with GitHub Actions
Upload/replace these project files in your existing QuickChestCapacity GitHub repository. Your existing workflow can build it. The finished JAR will be `quickchestcapacity-1.4.0.jar`.
