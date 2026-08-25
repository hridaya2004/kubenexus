# Navigation rail (M3)

> Source: https://m3.material.io/components/navigation-rail/overview · https://m3.material.io/components/navigation-rail/guidelines · https://m3.material.io/components/navigation-rail/specs · https://m3.material.io/components/navigation-rail/accessibility

## Variants/types

Two variants: collapsed and expanded navigation rails, introduced in the M3 Expressive update (May 2025) to replace the baseline rail — collapsed replaces it, and expanded replaces the navigation drawer. The baseline navigation rail is no longer recommended. The two new rails match visually and can transition into each other on any device; expanded rails come in non-modal and modal modality with "transition to collapsed" and "hide when collapsed" behaviors. The active label on vertical items changed from on-surface-variant to secondary.

Differences from M2: predictive back interaction, new color mappings with dynamic-color compatibility, and a pill-shaped active indicator in a contrasting color instead of relying on icon color, weight, and fill alone.

Availability: no resource-status table was captured in the overview scrape.

## Anatomy

1. Container
2. Menu (optional)
3. Floating action button (FAB) or Extended FAB (optional)
4. Icon – active / Icon – inactive
5. Label text – active / Label text – inactive
6. Active indicator
7. Large badge (optional) with Large badge label
8. Small badge (optional)

## Key dimensions

The captured specs notes carry no numeric measurement table; measurements are presented as annotated images. Qualitative geometry from the notes:

| Attribute | Value |
|---|---|
| Orientation | Vertical along the leading edge of the window — never horizontal |
| Destinations | 3–7 items, plus an optional FAB |
| Breakpoints | Medium, expanded, large, or extra-large windows |
| Item target area | Always spans the full width of the nav rail, even if the item container hugs its contents |
| Item group alignment | Top or center of the layout (center eases reachability on tablets); menu icon and FAB always top-aligned |

## States

Enabled · Hovered · Focused · Pressed — token folders under Default, Light ("Nav rail - Common" set). No state-layer percentages were captured.

Color roles used for light and dark schemes: Surface container (optional), On secondary container, Secondary container, Secondary (vertical) / On secondary container (horizontal), On surface variant, Error, On error. The baseline token set instead lists On secondary container, Secondary container, On surface, On surface variant, Error, On error, Error.

## Behavior

- Displays navigation items, a menu, and a floating action button vertically; a rail should be the only visible navigation element and stays in the same place across app screens.
- Collapsed: runs along the leading edge (left for LTR languages, right for RTL) with 3–7 items and should not be hidden. Used medium to extra-large; compact windows should always use a navigation bar, and medium windows with few destinations may consider one.
- Expanded: standard (placed beside body content, best for larger windows) or modal (overlaps content, opened from a menu icon; for information-dense layouts or many navigation items). It can reveal secondary destinations, be expanded by default on larger screens, or be hidden entirely in immersive experiences.
- The menu button transitions between collapsed and expanded; when expanded, its icon should change to represent that it can collapse.
- A nested FAB anchors to the top above navigation destinations with resting elevation level 0; avoid placing it below navigation items. Logos in the rail need caution against being mistaken for actions or destinations.
- The active indicator shows only the current page; in the expanded rail it hugs the label text (override it to fill the container to resemble the baseline drawer), while the target area spans full width.
- Icons symbolize their page's content; selecting fills the icon, changes its color, and shows an active indicator. All items require a one-word label — break long labels between words rather than truncating or shrinking type. Badges communicate counts/status: upper-right of the icon in compact rails, next to label text in expanded rails.
- An optional vertical divider positioned on the content-adjacent edge separates the rail from app content; the container fill can also be turned off if all items keep minimum 3:1 contrast.
- In adaptive layouts place the rail outside any panes; moving from large to small screens it transforms into a navigation bar. Tabs can sit alongside it as an extra visible navigation layer.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Token sets are organized under Default, Light with Enabled / Hovered / Focused / Pressed folders ("Nav rail - Common"); the baseline rail carries its own token set whose states are enumerated separately for active and inactive destinations.

## Accessibility summary

- People should be able to navigate between navigation destinations, select a particular destination from a set, and get appropriate feedback based on input type.
- Touch/cursor: tapping makes the active indicator appear with a ripple passing through it, the icon switches outlined→filled, and icon and text change color; hovering provides a visual cue that the destination is interactive.
- The target area for expanded rails spans the full width of the container even though the active indicator visually hugs the content.
- Visual indicators: filled icon for the active destination, outlined otherwise, with sufficient contrast against the container and no more than two colors; icons without filled styles use semibold weight when active.
- Text scaling: items grow vertically retaining default padding; scaled text may wrap; keep full labels visible up to 2x sizing, truncation allowed beyond.
- Initial focus lands on the first interactive item — menu, FAB, or first navigation item; from the FAB or menu, Tab brings focus to the navigation items, then Tab or Arrows move between them and Space/Enter activates.
- Keyboard: Tab / Arrows navigate between interactive elements; Space / Enter selects an interactive element.
- Labeling: the accessibility label typically equals the adjacent text label; disambiguate ambiguous text ("Recent" → "Recent images"). On Android Views (MDC-Android), a more descriptive accessibility label isn't available and the role isn't announced.

## Captured spec tables

*Reproduced as captured from notes/articles/navigation-rail--*.json; sparse rows reflect the original scrape.*

Variants (`specs`):

| Variant | M3 | M3 Expressive |
|---|---|---|
| Collapsed navigation rail | -- | Available |
| Expanded navigation rail | -- | Available |
| Navigation rail (baseline) | Available | Not recommended.Use collapsed navigation rail. |

Configurations (`specs`):

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Expanded layout | Standard (default) | Available as navigation drawer | Available |
|  | Modal | Available as navigation drawer | Available |
| Expanded behavior | Hide when collapsed | -- | Available |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab / Arrows | Navigate between interactive elements |
| Space / Enter | Selects an interactive element |

(The overview scrape contained no availability & resources table.)
