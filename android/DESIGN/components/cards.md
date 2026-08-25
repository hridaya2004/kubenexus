# Card (M3)

> Source: https://m3.material.io/components/cards/overview
> Source: https://m3.material.io/components/cards/guidelines
> Source: https://m3.material.io/components/cards/specs
> Source: https://m3.material.io/components/cards/accessibility

## Variants/types

- **Elevated** — drop shadow; more background separation than filled, less than outlined. Container color: surface container low.
- **Filled** — subtle background separation; least emphasis. Container color: surface container highest.
- **Outlined** — visual boundary via border; greatest emphasis of the three. Colors: surface container + outline variant.

All variants share the same legibility and functionality; choice is stylistic.

## Anatomy

- Container (only required element); size driven by contents; expresses elevation.
- Optional content blocks: image, headline, subhead (subhead), supporting text, buttons, icon buttons, selection controls (chips, sliders, checkboxes), linked text, overflow menu (typically upper-right or lower-right corner).
- Dividers: full-width for expandable content; inset to separate related content.
- Media: thumbnail (avatar/logo), image, video.
- Text/icons over images are discouraged; if needed, use a translucent scrim or bounding shape for contrast.

## Key dimensions

| Attribute | Value |
|---|---|
| Shape | 12dp corner radius |
| Left/right padding | 16dp |
| Padding between cards | 8dp max |
| Label text alignment | Start-aligned |

## States

- Elevated / Filled / Outlined each define: Enabled, Hovered, Focused, Pressed (ripple), Dragged, Disabled.
- Only directly actionable cards ripple; non-actionable containers do not.

State layers are semi-transparent overlays of the content color at fixed opacities (hover 8%, etc.) — see design.md.

## Behavior

- Displays content and actions on a single topic; entry point to deeper detail/navigation.
- Collections: grid (default; staggered/mosaic possible), vertical list, carousel. Coplanar by default — shared resting elevation unless picked up or dragged.
- Filtering/sorting controls live outside the collection; a filter must apply to every card in the collection.
- Either make the whole card actionable with no inner buttons, or keep it a container holding actions — never stack an action on an actionable surface.
- Adaptive: position/alignment and orientation can change across breakpoints (e.g., horizontal at compact becomes vertical at expanded).

## Token group

Specs capture per-variant **Color** token groups plus shape/padding slots under `md.comp.elevated-card.*`, `md.comp.filled-card.*`, `md.comp.outlined-card.*`:

- Elevated: surface-container-low
- Filled: surface-container-highest
- Outlined: surface, outline-variant

## Accessibility summary

- Users must be able to reach the card and its elements and get feedback matching their input type.
- Touch: tap on directly actionable cards shows ripple across the card.
- Dragging/swiping requires a single-pointer alternative (e.g., long-press or tap opens a menu with move/delete actions).
- Keyboard: all interactive elements are tab stops; directly actionable cards are one tab stop; in non-actionable cards only inner actions are tab stops. Space or Enter confirms the focused action; arrows navigate open menus.
- Labeling: informative contents are verbalized; decorative images hidden from screen readers; directly actionable cards take button or link role; non-actionable cards take no role.

## Captured spec tables

### Specs — Measurements

| Attribute | Value | Shape | Left/right padding | Padding between cards | Label text alignment |
|---|---|---|---|---|---|
| 12dp corner radius | 16dp | 8dp max | Start-aligned | | |

### Accessibility — Keyboard navigation

| Keys | Actions |
|---|---|
| Tab | Move to the next actionable element. Directly actionable cards: Move to next card container. Non-actionable cards with actionable elements: Move to next actionable element |
| Space or Enter | Confirm action |

### Overview — Availability & resources

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Flutter | Available |
| Implementation | Android Views (MDC-Android) | Available |
| Implementation | Jetpack Compose | Available |
| Implementation | Web | Unavailable |
