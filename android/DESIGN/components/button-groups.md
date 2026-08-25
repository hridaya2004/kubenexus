# Button groups (M3)

> Source: https://m3.material.io/components/button-groups/{overview,guidelines,specs,accessibility}

## Variants/types

- Two variants: **standard** and **connected**. Both are M3 Expressive (May 2025) catalog additions; the connected group supersedes the segmented button, which is no longer recommended.
- Configurations shared by both variants: sizes XS/S/M/L/XL; default shape round or square; selection modes single-select, multi-select, selection-required.
- Members can be buttons and/or icon buttons of any color style except standard icon buttons and text buttons (no container treatment).

## Anatomy

- Container only — button groups are invisible wrappers that add padding between children and modify child shape on interaction; they hold no buttons by default.

## Key dimensions

Captured example (standard group, extra-small set): container height **32dp**; between-space **18dp**.

- Standard group hugs its children's total width; inter-button padding gives selected/pressed buttons room to change width without shifting layout.
- Connected group spans the width of its page or surface (children stretch); cap the maximum width in large windows.

## States

Enabled · Disabled · Hovered · Focused · Pressed on each child (+ Selected for toggles). State layers per the shared model — see design.md (hover 8%, focus 10%, pressed 10%).

- Standard group: pressing a button changes that button's width and shape and temporarily adjusts adjacent buttons' widths; selecting a toggle changes its shape square ↔ round and its color.
- Connected group: no cross-button interaction; only the activated button's shape changes.

## Attributes/behavior

- Default grouping: identical size (XS–XL) and shape (round/square) across members; mixed sizes only for hero moments; shape differences reserved for selection or deliberate contrast.
- Mix variants, widths, and colors to set emphasis; keep primary actions visually dominant via size, color, or shape.
- Connected groups: use for related selectable content (view switching, sorting); avoid when nothing is toggleable; do not mix color styles within one connected group.
- Groups have no color properties of their own — children carry filled/tonal/outlined/elevated styles.
- Adaptive: resizing and presentation follow the layout guidance; groups work with all five button sizes.

## Token group

`md.comp.button-group.*` families organized per variant and size (captured example token module: "Button group standard - Size - Xsmall" with container-height 32dp and between-space 18dp folders). Child tokens resolve through the `md.comp.button.*` / `md.comp.icon-button.*` sets.

## Accessibility summary

- Every child needs a ≥ **48×48dp** target; XS and S groups use enlarged inner padding for this — never reduce it.
- The container is neither focusable nor labeled; initial focus lands on the first button, then moves button to button.
- Keyboard: Tab navigates to the next button; Space or Enter activates the focused button.
- Label each child following the button / icon button accessibility guidance; announce selection state.

## Captured spec tables

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Jetpack Compose: Expressive | Available |
| Implementation | Android Views (MDC-Android): Expressive | Available |
| Implementation | Web: Expressive | Unavailable |

| Variant | M3 | M3 Expressive |
|---|---|---|
| Standard button group | -- | Available |
| Connected button group | Available as segmented button (deprecated in the expressive update) | Available |

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Size | XS, S, M, L, XL | -- | Available |
| Default shape | Round, square | -- | Available |
| Selection | Single-select, multi-select, selection-required | Available as segmented button (deprecated in the expressive update) | Available |

Keyboard navigation:

| Keys | Actions |
|---|---|
| Tab | Navigates to the next button |
| Space or Enter | Activates the focused button |
