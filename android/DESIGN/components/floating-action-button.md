# Floating action button (M3)

> Source: https://m3.material.io/components/floating-action-button/{overview,guidelines,specs,accessibility}

## Variants/types

- Three size variants: **FAB**, **medium FAB** (most recommended), **large FAB**. The baseline **small FAB** still exists but is no longer recommended (May 2025 update).
- Color styles: primary container / secondary container / tertiary container (defaults; renamed to match their color roles) plus primary / secondary / tertiary (new in M3 Expressive). Surface FAB colors remain but are not recommended.
- M2 → M3 change: circle with constant shadow → boxier shape, dynamic color support, and the large-FAB addition.

## Anatomy

- Container
- Icon

## Key dimensions

- Hovered state rests at elevation **4**.
- Margins: **16dp** from screen edges (raised to 24dp in large/extra-large windows per the adaptive guidance).
- Placement: lower-right corner in compact/medium breakpoints; navigation rail region in expanded+ windows; alignment may be left, center, or right, above or nested in the navigation bar.
- Measurements are organized per size (FAB / medium / large) in the token explorer.

## States

Enabled · Hovered (8% state layer, elevation 4) · Focused (10% state layer) · Pressed (10% state layer) — see design.md for the shared layer model.

- For non-default color mappings, the state-layer color must equal the icon color (e.g., `md.sys.color.primary` for the primary mapping).
- Disabled is not used — see accessibility.

## Attributes/behavior

- Represents a screen's most important action, rendered above all other content; persists while content scrolls.
- One FAB per screen; never cover the container with badges or other elements; container must contrast sufficiently with its surface.
- Constructive actions only: create, favorite, share, start a process. Avoid destructive, minor, overflow, or unclear actions and toolbar-type controls.
- Can transform into an extended FAB on larger screens, or open a FAB menu when selected.
- Icon: filled style preferred over outlined; clear and simple glyphs; web hover shows a tooltip with an icon text label.
- No notifications and no duplicate actions from elsewhere on the screen.

## Token group

`md.comp.fab.*` families organized by size and color (e.g., `md.comp.fab.primary.container.color`); baseline token sets cover only the deprecated small/surface variants.

## Accessibility summary

- Never disable a FAB — if its action is unavailable, omit the FAB entirely.
- Icon requires ≥ **3:1** contrast against the container.
- Prioritize the FAB early in focus order (focus may jump past page content straight to it); show a tooltip on focus (web).
- On expanded breakpoints consider upper-left placement so screen-reader users reach the primary action sooner; test with users. In compact/medium, lower-right corner is best.
- Don't position the FAB where it fully hides another element's focus indicator (partial overlap is acceptable).
- Keyboard: Tab focuses the FAB; Space or Enter performs its action. Label describes the action ("Compose a new message").

## Captured spec tables

| Variant | M3 | M3 Expressive |
|---|---|---|
| FAB | Available | Available |
| Medium FAB | -- | Available |
| Large FAB | Available | Available |
| Small FAB | Available | Not recommended. Use a larger size. |

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Color | Primary container, secondary container, tertiary container | Available as primary, secondary, tertiary | Available |
| Color | Primary, secondary, tertiary | -- | Available |

Keyboard navigation:

| Keys | Actions |
|---|---|
| Tab | Focus lands on the FAB |
| Space or Enter | Perform the default action on an item |

State summary captured from specs captions:

| State | State layer | Elevation |
|---|---|---|
| Enabled | — | default |
| Hovered | 8% | 4 |
| Focused | 10% | — |
| Pressed | 10% | — |
