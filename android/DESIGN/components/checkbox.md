# Checkbox (M3)

> Source: https://m3.material.io/components/checkbox/overview · https://m3.material.io/components/checkbox/guidelines · https://m3.material.io/components/checkbox/specs · https://m3.material.io/components/checkbox/accessibility

## Variants/types

Single control with three selection modes: unselected, selected, indeterminate. Error treatments exist for each mode (added in M3). M3 adds new color mappings with dynamic-color compatibility.

Availability: Design Kit (Figma); implementations for Flutter, Jetpack Compose, Android Views (MDC-Android), Web — all Available.

## Anatomy

1. Container
2. Icon

An adjacent text label typically accompanies the control.

## Key dimensions

| Attribute | Value |
|---|---|
| Container size | 18dp |
| Container corner shape | 2dp |
| Icon size | 18dp |
| Icon alignment | Center-aligned |
| Target size | 48dp |
| State-layer size | 40dp |

## States

Enabled · Disabled · Hovered · Focused · Pressed (ripple), crossed with unselected / selected / indeterminate and their error counterparts.

Interaction feedback renders a translucent content-color overlay over the control; per-state opacities and geometry are defined in design.md (hover 8% / focus 10% / press 10% / drag 16%; 40dp layer within a 48dp target).

The adjacent text label holds the on-surface role in every state — unchanged whether or not the checkbox is selected or being interacted with.

## Behavior

- Multiple checkboxes in a list can be selected independently.
- Supports parent-child relationships:
  - Checked parent checks all children; unchecked parent unchecks all children.
  - Partial child selection turns the parent indeterminate; checking an indeterminate parent checks every child.
- When used to turn something on/off, the change executes immediately upon selection.
- Preferred over switches when several related options are selectable from a list — groups related items visually and takes less space.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Color resolves through the component's container / icon / state-layer roles per state folder; the specs module organizes tokens under Default, Light with Enabled / Disabled / Hovered / Focused / Pressed (ripple) groupings.

## Accessibility summary

- Assistive technology users must be able to navigate to a checkbox, toggle it, and receive input-appropriate feedback.
- Either the text label or the box itself activates the option.
- Do not apply density by default: it pushes targets under the 48×48 CSS-pixel best practice; any controls used to enable denser layouts must themselves remain ≥ 48×48 CSS px.
- Keyboard: Tab reaches the enabled control/group; Space or Enter toggles selection; Backspace or Delete removes the focused input chip-style entry; Arrow keys move focus between items. (The captured keyboard table reuses chip phrasing.)
- Labeling: correctly linked UI text is announced before the role; an individual checkbox's accessibility label normally equals its visible text label.

## Captured spec tables

*Reproduced as captured from notes/articles/checkbox--*.json; sparse rows reflect the original scrape.*

Availability & resources (`overview`):

| Type | Resource | Status | Design | Implementation |
|---|---|---|---|---|
|  |  |  |  |  |
|  | Design Kit (Figma) | Available |  |  |
|  |  |  |  |  |
|  | Flutter | Available |  |  |
|  | android Jetpack Compose | Available |  |  |
|  | android Android Views (MDC-Android) | Available |  |  |
|  | language Web | Available |  |  |

Measurements (`specs`):

| Attribute | Value | Container size | Container corner shape | Icon size | Icon alignment | Target size | State-layer size |
|---|---|---|---|---|---|---|---|
| 18dp |  |  |  |  |  |  |  |
| 2dp |  |  |  |  |  |  |  |
| 18dp |  |  |  |  |  |  |  |
| Center-aligned |  |  |  |  |  |  |  |
| 48dp |  |  |  |  |  |  |  |
| 40dp |  |  |  |  |  |  |  |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Moves focus to enabled An enabled state communicates an interactive component or element. More on enabled state chip or chip group |
| Space or Enter | Activates, selects, or deselects the focused chip |
| Backspace or Delete | Removes currently focused A focused state communicates when a user has highlighted an element, using an input method such as a keyboard or voice. More on focuse |
| Arrows | Moves focus between chips |
