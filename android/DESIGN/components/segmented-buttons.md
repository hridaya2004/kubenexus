# Segmented buttons (M3)

> Source: https://m3.material.io/components/segmented-buttons/{overview,guidelines,specs,accessibility}

Status: **no longer recommended** as of the M3 Expressive update (May 2025) — use the connected button group instead (same functionality, updated visuals).

## Variants/types

- Two variants: **single-select** (one segment selected at a time; selection required) and **multi-select** (zero or more segments selected).
- 2–5 segments per control; each segment carries label text, an icon, or both.
- Formerly called toggle buttons; renamed with two official variants.

## Anatomy

- Segment(s) inside a container
- Icon (optional on unselected segments)
- Label text (optional)
- Selected icon (checkmark)

## Key dimensions

- Container height **40dp** (raised from the M2 height); fully rounded corners.
- Outline width **1dp**; labels center-aligned.
- Left/right padding min **12dp**; padding between elements **8dp**.
- Target size **48dp**; container width is dynamic from labels; segment width = container width / segment count.
- Density: height-only, **−4dp** per density step.

## States

Unselected: Enabled · Disabled · Hovered · Focused · Pressed. Selected: Selected · Hovered on selected · Focused on selected · Pressed on selected. State layers follow the shared model — see design.md (hover 8%, focus 10%, pressed 10%).

Color roles used: On surface · Outline · Secondary container · On secondary container.

## Attributes/behavior

- Purpose: choosing between options, switching views, sorting elements — simple choices of 2–5 items; use chips beyond five options or for complex choices.
- Fully rounded corners and sentence-case labels (M2 used small radii + all caps).
- Labels short, succinct, consistent in type across segments; never wrap onto a second line; switch to an icon alone when text won't fit.
- Don't mix icon-only and text-label segments in one control.
- A standalone icon must clearly communicate its option.
- Single-select requires one active option (size pickers); multi-select allows none/many (filters).

## Token group

`md.comp.segmented-button.*` — captured token module "Segmented button - Outlined" with state folders Enabled / Disabled / Hovered / Focused / Pressed (ripple).

## Accessibility summary

- Outline must keep ≥ **3:1** contrast against the background so segments read as a cluster of distinct buttons.
- Selection is signaled by both checkmark icon and color change — never color alone.
- Initial focus starts on the first segment regardless of selection state; side depends on language direction (leftmost in LTR, rightmost in RTL).
- Keyboard: Tab moves to the next enabled segment; Space or Enter selects/unselects — single-select toggles the focused segment, multi-select can select all or clear selections.
- Roles: single-select behaves as a Radiogroup; multi-select as Checkbox. Labels come from visible text; icon-only segments get descriptive labels.

## Captured spec tables

Measurements:

| Attribute | Value |
|---|---|
| Container width | Dynamic based on labels |
| Segment width | Container width / total segments (Example: 1/3) |
| Height | 40dp |
| Outline width | 1dp |
| Label alignment | Center |
| Left/right padding | Min 12dp |
| Padding between elements | 8dp |
| Target size | 48dp |

Keyboard navigation:

| Keys | Actions (single select) | Actions (multi select) |
|---|---|---|
| Tab | Focus lands on next enabled segment | Focus lands on next enabled segment |
| Space or Enter | Select focused segment | Select and unselect focused segment |
