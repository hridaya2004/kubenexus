# Carousel (M3)

> Source: https://m3.material.io/components/carousel/overview
> Source: https://m3.material.io/components/carousel/guidelines
> Source: https://m3.material.io/components/carousel/specs
> Source: https://m3.material.io/components/carousel/accessibility

## Variants/types

Six layouts (start-aligned or center-aligned):

- **Multi-browse** — at least one large, medium, and small item; browsing many visual items at once.
- **Uncontained** — single-size items scrolling past the container edge; traditional carousel behavior.
- **Uncontained multi-aspect ratio** — uncontained layout with mixed item widths (9:16 min to 16:9 max).
- **Hero** — one large spotlighted item plus a small preview.
- **Center-aligned hero** — large item centered, small items on both edges.
- **Full-screen** — one edge-to-edge large item; portrait only (compact/medium breakpoints); vertical scrolling.

New in M3 (no M2 equivalent). Added Nov 2025: uncontained multi-aspect ratio.

## Anatomy

- **Container**: rectangle holding all items; stretches to any size; visible item count varies by layout and breakpoint.
- **Carousel items**: no fixed width; three dynamic widths — large (customizable max width), medium (dynamic), small (40–56dp).
- **Item text (optional)**: brief; adapt or shorten at smaller sizes; avoid >2 lines at compact breakpoints unless the background is simple.
- Item visuals have a parallax effect when scrolled; items change size and shape as they move through the layout and snap into place.

## Key dimensions

| Layout | Alignment | Leading/trailing padding | Top/bottom padding | Padding between elements | Large width | Medium width | Small width | Corner radius |
|---|---|---|---|---|---|---|---|---|
| Multi-browse | Vertically centered | 16dp | 8dp | 8dp | Dynamic, or user-set | Dynamic | 40–56dp, dynamic | 28dp |
| Uncontained | Vertically centered | 16dp (leading) | 8dp | 8dp | — | — | — | 28dp |
| Uncontained multi-aspect ratio | Vertically centered | 16dp (leading) | 8dp | 8dp | — | — | — | 28dp |
| Hero | Vertically centered | 16dp | 8dp | 8dp | Dynamic | — | 40–56dp, dynamic | 28dp |
| Center-aligned hero | Vertically centered | 16dp | 8dp | 8dp | Dynamic | — | 40–56dp, dynamic | 28dp |
| Full-screen | Centered | 0dp | 0dp | 16dp | — | — | — | — |

Small items: minimum 40dp, maximum 56dp. Items bleed over padding when scrolling in uncontained layouts.

## States

Carousel item states: Enabled, Hovered, Focused, Pressed (ripple), Disabled.

State layers are semi-transparent overlays of the content color at fixed opacities — see design.md.

## Behavior

- Scrollable list of visually emphasized items with optional brief text.
- Scrolling modes: default scrolling (recommended for uncontained) and snap-scrolling (recommended for multi-browse, hero, full-screen). Full-screen must use snap-scrolling only.
- Hero swipes one large item at a time; multi-browse scrolls many at once.
- Items stay fully visible on-screen (except uncontained); they resize and snap to preserve the layout while scrolling.
- At compact breakpoints, multi-browse shows up to three text-bearing items; hero shows one large + one small item; center-aligned hero shows two small previews.
- On vertically-scrolling pages, provide an accessible way to reach all items without horizontal scrolling (e.g., "Show all" button below the carousel, or arrow icon button next to the header); not required for full-screen.

## Token group

Specs capture the carousel-item token set under `md.comp.carousel.*`:

- **Color** group: container = surface; item content roles per scheme.
- Shape tokens cover the 28dp item corner radius; size tokens cover dynamic widths.

## Accessibility summary

- Assistive tech must: navigate to the container, move between items, activate an item, skip the whole carousel.
- Keyboard: Tab places initial focus on the first item (not the container); Tab/arrows move between items; Space or Enter activates the focused item; up/down arrows leave the carousel.
- Container takes the container role; labels announce total item count plus current focused item.
- Touch/cursor: tapping changes shape slightly with a ripple; hover signals interactivity.
- Reduced motion: remove parallax and item expansion (all items same size); carousels must still reach window edges; reduced-motion hero shows only a partial small item.
- "Show all" button should have 4dp padding; header arrow icon button is 48dp and sits adjacent to the header on its leading edge.

## Captured spec tables

### Guidelines — Layout selection

| Layout | Best used for |
|---|---|
| Multi-browse | Browsing many visual items at once (like photos), dynamic designs |
| Uncontained | Highly-customized or text-heavy carousels, stacked images and text, traditional carousel behavior |
| Hero | Spotlighting very large visual items (like a movie or featured app) |
| Center-aligned hero | Centered, large visual items |
| Full-screen | Vertically-scrolling video or image feeds, immersive experiences |

### Specs — Multi-browse

| Attribute | Value |
|---|---|
| Alignment | Vertically centered |
| Leading/trailing padding | 16dp |
| Top/bottom padding | 8dp |
| Padding between elements | 8dp |
| Large item width | Dynamic, or user-set |
| Medium item width | Dynamic |
| Small item width | 40–56dp, dynamic |
| Item corner radius | 28dp |

### Specs — Uncontained

| Attribute | Value |
|---|---|
| Alignment | Vertically centered |
| Leading padding | 16dp |
| Top/bottom padding | 8dp |
| Padding between elements | 8dp |
| Item corner radius | 28dp |

### Specs — Uncontained multi-aspect ratio

| Attribute | Value |
|---|---|
| Alignment | Vertically centered |
| Leading padding | 16dp |
| Top/bottom padding | 8dp |
| Padding between elements | 8dp |
| Item corner radius | 28dp |

### Specs — Hero

| Attribute | Value |
|---|---|
| Alignment | Vertically centered |
| Leading/Trailing padding | 16dp |
| Top/bottom padding | 8dp |
| Padding between elements | 8dp |
| Large item width | Dynamic |
| Small item width | 40–56dp, dynamic |
| Item corner radius | 28dp |

### Specs — Center-aligned hero

| Attribute | Value |
|---|---|
| Alignment | Vertically centered |
| Leading/Trailing padding | 16dp |
| Top/bottom padding | 8dp |
| Padding between elements | 8dp |
| Large item width | Dynamic |
| Small item width | 40–56dp, dynamic |
| Item corner radius | 28dp |

### Specs — Full-screen

| Attribute | Value |
|---|---|
| Alignment | Centered |
| Leading/Trailing padding | 0dp |
| Top/bottom padding | 0dp |
| Padding between elements | 16dp |

### Accessibility — Keyboard navigation

| Keys | Actions |
|---|---|
| Tab or Arrows | Moves to the previous or next carousel item |
| Space or Enter | Activates the focused carousel item |

### Overview — Availability & resources

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Flutter | Available |
| Implementation | Jetpack Compose | Available |
| Implementation | Android Views (MDC-Android) | Available |
| Implementation | Web | Unavailable |
