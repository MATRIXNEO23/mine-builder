# Real-world measured references

Place a JSON file under the Minecraft config folder: `config/minebuilder/references/<id>.json`.

Example for a hypothetical measured building (NOT a real landmark):

```json
{
  "name": "Sample measured building",
  "width_m": 12,
  "depth_m": 9,
  "height_m": 7,
  "source": "My measured floor plan"
}
```

Type `riferimento <id>` in Banco IA and press `Invia a GPT` to view the recorded dimensions and source. IDs use lowercase letters, digits, underscore and hyphen.

The reference reader works entirely locally, with no extra services. It does not yet parse photographs or floor plans, perform web research, or automatically reconstruct the measured building. Measurements must be supplied and verified by the user; the system must not invent dimensions.
