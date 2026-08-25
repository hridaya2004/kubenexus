# Extended FAB (M3)

> Source: https://m3.material.io/components/extended-fab/{overview,guidelines,specs,accessibility}

## Variants/types

- Three size variants added in M3 Expressive (May 2025): **small extended FAB** (**56dp**), **medium extended FAB** (**80dp**), **large extended FAB** (**96dp**) — heights align with the FAB sizes for easy transitions between them.
- The baseline extended FAB (56dp) remains available but is no longer recommended; replace it with the small variant. Surface color styles are likewise not recommended.
- M2 → M3 change: pill shape with its own height/elevation → same height, boxier smaller-radius shape, and the FAB's simpler elevation model.

## Anatomy

- Container (rounded rectangle that hugs its contents)
- Label text
- Icon (optional — unlike the standard FAB; an icon without a label is not allowed)

## Key dimensions

Baseline captured measurements:

| Attribute | Value |
|---|---|
| Container height | 56dp |
| Container width | Dynamic, 80dp min |
| Container shape | 16dp corner radius |
| Icon size | 24dp |
| Padding | 16dp |

- Margins: **16dp**.
- Expressive sizes update typography upward (small uses title-medium versus baseline label-large) and reduce inner padding.

## States

Enabled · Hovered (elevation 4) · Focused · Pressed — state layers per the shared model in design.md (hover 8%, focus 10%, pressed 10%).

- For non-default color mappings the state-layer color matches the icon color (e.g., `md.sys.color.primary` for the primary mapping).

## Attributes/behavior

- Use for persistent access to a primary action above long, scrolling content (e.g., checkout), or when a label is needed to clarify an ambiguous icon.
- One per screen; multiple extended FABs compete for attention.
- Not for choosing among a set of options — use filled buttons for that emphasis level.
- Container width grows and shrinks with label length; labels of 1–2 words, accounting for localization-driven width changes; never wrap or truncate text.
- Place above other UI, off elements like app bars and toolbars (keeps elevation/surface layers consistent).
- Can transform to/from a regular FAB; appears with entrance motion and persists during scrolling.
- Placement: lower-right corner in compact/medium windows; medium or large sizes in expanded+ breakpoints (large also suits compact windows needing one prominent action); layout mirrors in RTL.

## Token group

`md.comp.extended-fab.*` families organized by size and color. Color mappings: primary container + on primary container (default), secondary container pair, tertiary container pair, primary pair, secondary pair, tertiary pair; baseline sets add surface-container styles (deprecated).

## Accessibility summary

- Icon and visible label behave as one focusable element; no tooltip required since the label is always visible.
- Accessibility label must begin with the visible label's first word ("Create" → "Create a new invite") and share the icon+label's single purpose.
- Keyboard: Tab moves focus to the extended FAB; Space or Enter activates it.
- Prioritize it in focus order; place where it is easy to reach without obstructing other actions or covering actionable elements.

## Captured spec tables

| Variant | M3 | M3 Expressive |
|---|---|---|
| Small extended FAB | -- | Available |
| Medium extended FAB | -- | Available |
| Large extended FAB | -- | Available |
| Extended FAB (baseline) | Available | Not recommended. Use small extended FAB. |

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Flutter | Available |
| Implementation | Jetpack Compose | Available |
| Implementation | Jetpack Compose: Expressive | Available |
| Implementation | Android Views (MDC-Android) | Available |
| Implementation | Android Views (MDC-Android): Expressive | Available |
| Implementation | Web | Available |
| Implementation | Web: Expressive | Unavailable |

Keyboard navigation:

| Keys | Actions |
|---|---|
| Tab | Moves focus to the extended FAB |
| Space or Enter | Activates the extended FAB |
