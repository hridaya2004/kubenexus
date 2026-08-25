# List (M3)

> Source: https://m3.material.io/components/lists/overview
> Source: https://m3.material.io/components/lists/guidelines
> Source: https://m3.material.io/components/lists/specs
> Source: https://m3.material.io/components/lists/accessibility

## Variants/types

- **Expressive list** — M3 Expressive; segmented visual style with round corners, highlighted selection states, customizable slots. Recommended for new designs.
- **Baseline list** — original M3 list; square corners and standard colors; still available but not recommended; on web, expressive lists build on baseline.

Configurations:

| Category | Configuration | Notes |
|---|---|---|
| Styles | Standard or Segmented | Visual choice only; no behavior change |
| Selection modes | Single-action, multi-action, single-select, multi-select | One mode per list at a time |
| Interactions | Expand/collapse; swipe-to-reveal | Swipe-to-reveal on Android Views only |

## Anatomy

Required: container, label text. Optional: overline, supporting text, trailing text, trailing icon, divider, leading avatar, leading icon, leading media (image or video).

Slots (expressive): leading / content / trailing containers.

- Leading slot: avatars, icons, image/video thumbnails, selection controls, badges or larger images.
- Content slot: must be the largest-width slot, centered in the item; label/supporting text plus optional badge, icon, inline label, extra text.
- Trailing slot: icons/icon buttons/trailing text, selection controls.
- Slot positions must be narrower than the content section; targets at least 48x48dp; standard item padding applies.
- One selection interaction per item maximum.

## Key dimensions

| Attribute | Value |
|---|---|
| Item height | Tallest element sets height — 56dp, 72dp, or 88dp |
| Label alignment | Center (Top when height is 88dp or taller) |
| Label left padding | 16dp |
| Leading element alignment (vertical) | Center (Top at 88dp or taller) |
| Leading element left padding | 16dp |
| Leading icon alignment (vertical) | Top |
| Leading icon top padding | 8dp (12dp at 88dp or taller) |
| Trailing element alignment (vertical) | Center (Top at 88dp or taller) |
| Trailing element left padding | 16dp |
| Trailing element right padding | 24dp |
| Padding above/below divider | 0dp |
| Targets | 48dp |
| Divider full-width | 100% |
| Divider inset left padding | 16dp |
| Divider inset right padding | 24dp |
| Unselected corner radius | 4dp inner, 16dp outer |
| Selected corner radius | 16dp |

Icon button height is dynamic, filling the item height. Elements are top-aligned when the list is 88dp or larger or contains three-plus lines of text.

## States

Default items and selected items each define: Enabled, Disabled, Hovered, Focused, Pressed, Dragged.

State layers are semi-transparent overlays of the content color at fixed opacities — see design.md.

## Behavior

- Helps users find a specific item and act on it; order logically (alphabetical/numerical); keep items short and scannable.
- Selected state is highlighted by shape morphing (corner radii above), not color alone.
- Lists can expand/collapse to include multiple items; swipe reveals more actions.
- A single-action item is one tappable area; multi-action items have a primary action plus secondary actions.

## Token group

Specs expose a common token set (baseline + expressive shapes/sizes) plus an expand set under `md.comp.list.*`, grouped as Color, Spacing, Shape, Size and typography:

- Color roles: surface, on-surface, on-surface-variant, outline-variant, primary-container, on-primary-container.

## Accessibility summary

- Users must navigate to items, select them, and perceive selection beyond color (radio/checkbox, icons, or non-color style such as underline); use two cues for selection.
- Touch: tap shows ripple. Keyboard: Tab reaches first item (or selected item); arrows cycle with wrap-around; Space or Enter selects/activates. Multi-action items: Tab focuses the first action, arrows move between all focusable actions, Space or Enter activates.
- Swipe-hidden actions need single-pointer alternatives (single/double tap, long press).
- Focus: first element receives focus unless an item is already selected.
- Labels/roles by platform — single-select: web container = List box with selection-type label, item = Option ("Selected"/"Not-selected"); Android Views and Compose item = Radio button ("Checked"/"Not-checked"). Multi-select mirrors this with Checkbox roles. Non-selectable lists read label text without a role.

## Captured spec tables

### Specs — Variants

| Variants | M3 | M3 Expressive |
|---|---|---|
| Lists (expressive) | -- | Available |
| Lists (baseline) | Available | Not recommended. Use expressive lists instead. |

### Specs — Configurations

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Styles | Standard | Available | Available |
| Styles | Segmented | -- | Available |
| Selection modes | Single-action, multi-action, single-select, multi-select | Available | Available |
| Interactions | Expand, swipe* | Available | Available |

\* Swipe-to-reveal interactions are only available on Android Views

### Specs — Measurements

| Attribute | Value |
|---|---|
| Label alignment | Center |
| Label alignment when height is 88dp or taller | Top |
| Label left padding | 16dp |
| Leading element alignment (vertical) | Center |
| Leading element alignment (vertical) when height is 88dp or taller | Top |
| Leading element left padding | 16dp |
| Leading icon alignment (vertical) | Top |
| Leading icon top padding | 8dp |
| Leading icon top padding when height is 88dp or taller | 12dp |
| Trailing element alignment (vertical) | Center |
| Trailing element alignment (vertical) when height is 88dp or taller | Top |
| Trailing element left padding | 16dp |
| Trailing element right padding | 24dp |
| Padding above/below divider | 0dp |
| Targets | 48dp |
| Divider full-width | 100% |
| Divider inset left padding | 16dp |
| Divider inset right padding | 24dp |

### Accessibility — Keyboard navigation

| Keys | Actions |
|---|---|
| Tab | To move focus to the first list item, last list item, or outside of the list component |
| Down and right arrow keys | Moves to the next element in the list; if the focused element is the last in the list, it wraps back to the top of the list |
| Up and left arrow keys | Moves to the previous element in the list; if the focused element is the first in the list, it wraps back to the bottom of the list |
| Space or Enter | To select a list item not yet selected |

### Accessibility — Single-select lists (platform traits)

| Trait | Web | Android Views (MDC-Android) | Jetpack Compose |
|---|---|---|---|
| Aria label | Container label: Should describe selection type; List item: Should match the visible label text | List item: Should match the visible label text | List item: Should match the visible label text |
| Role | Container: List box; List item: Option | List item: Radio button | List item: Radio button |
| State | Selected or Not-selected | Checked or Not-checked | Checked or Not-checked |

### Accessibility — Multi-select lists (platform traits)

| Trait | Web | Android Views (MDC-Android) | Jetpack Compose |
|---|---|---|---|
| Aria label | Container label: Should describe selection type; List item: Should match the visible label text | List item: Should match the visible label text | List item: Should match the visible label text |
| Role | Container: List box; List item: Option | List item: Checkbox | List item: Checkbox |
| State | Selected or Not-selected | Checked or Not-checked | Checked or Not-checked |

### Overview — Availability & resources

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
