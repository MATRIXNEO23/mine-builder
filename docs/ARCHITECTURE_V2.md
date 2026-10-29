# Mine Builder — Architecture v2 (design, not yet implemented)

## Goal
Turn a natural-language request into a coherent, inspectable architectural specification, not an unbounded list of blocks. Keep existing OAuth, no paid APIs or extra hosting.

## Pipeline
1. Interpret intent: building type, style, footprint, floors, roof geometry, materials, openings and optional reference.
2. Validate typed JSON schema locally; reject unknown blocks and out-of-bounds requests. No commands from model.
3. Plan geometry: foundation, structural shell, rooms, doors, windows, stairways, roof, facade details, landscaping. Support multiple footprints (rectangle, L, U, courtyard), roof types (gable, hip, shed, flat), and architectural palettes.
4. Verify geometry: enclosed walls, walkable interior, door clearance, roof continuity, stair connections, window openings, no isolated floating elements. Show warnings and preview before confirmation.
5. Build in stages on integrated server; track per-block success, loaded chunks, world identity, cancellation and resumption. Save progress atomically in world directory.
6. Inspect existing structures only after user selects two corners and confirms a bounded region. Read block palette and geometry from local world, never automatically copy or destroy existing buildings. Use inspection as reference, not as a claim of perfect reconstruction.

## Safety
- No destructive clearing of arbitrary terrain without preview and confirmation.
- Do not overwrite nonreplaceable blocks or another build by default.
- No world mutation during inspection.
- Never mark queued blocks as completed before successful server placement.
- Preserve OAuth and singleplayer restrictions.
- 1 block = 1 metre for 1:1 reconstructions; prioritize accuracy, with checkpoints and no arbitrary total-block cap.

## Acceptance gates
- Small cottage has pitched roof, door, windows, walkable interior and coherent materials.
- Different prompts produce distinct, recognizable structures.
- Inspect a known sample building and report measured dimensions and palette accurately.
- Interrupted builds resume without duplicating work or silently losing unloaded-chunk placements.
- Existing buildings remain intact unless user explicitly approves replacement.
- GitHub CI passes, followed by in-game runtime verification; CI alone is not proof of building quality.

## Current implementation
The existing parametric house generator is only an early prototype. This document does not mean the full pipeline or structure inspection is implemented.
