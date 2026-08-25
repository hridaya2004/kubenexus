# Radio button (M3)

> Source: https://m3.material.io/components/radio-button/overview · https://m3.material.io/components/radio-button/guidelines · https://m3.material.io/components/radio-button/specs · https://m3.material.io/components/radio-button/accessibility

## Variants/types

Single control with selected and unselected modes; selected items are more prominent than unselected ones. Use radio buttons (not switches) when only one item can be selected from a list, with scannable labels. What's new: color mappings with dynamic-color compatibility.

Availability: Design Kit (Figma); implementations for Flutter, Jetpack Compose, Android Views (MDC-Android), Web — all Available.

## Anatomy

1. Selected icon
2. Adjacent label text
3. Unselected icon
4. Adjacent label text

Radio buttons are always paired with an adjacent label describing what the button selects; because only one can be selected at a time, each choice must have its own label.

## Key dimensions

| Attribute | Value |
|---|---|
| Icon size | 20dp |
| State-layer size | 40dp |
| Target size | 48dp |

## States

Enabled · Disabled · Hovered · Focused · Pressed (ripple) — token folders under Default, Light; state specs are in the token module.

Color roles used for light and dark themes: Primary, On surface variant. The adjacent text label uses on surface and remains unchanged whether the button is selected or being interacted with.

## Behavior

- The recommended way to make a single selection from a list of options: only one radio button can be selected at a time, unlike multi-select checkboxes.
- Selection succeeds when a person clicks or taps either the radio button icon or its label.
- Changes take effect immediately, unless the buttons sit in a dialog or page that needs to be saved.
- Use when exposing all available options with five or fewer choices; consider a drop-down menu instead to save screen space, at the cost of extra clicks and cognitive effort.
- Arrange in stacked, vertically listed layouts with one option always pre-selected; avoid horizontal radio button lists.
- Don't nest radio buttons or use them to select multiple options; switches and checkboxes are the alternative selection controls for settings/preferences.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Color resolves through the component's Primary / On-surface-variant roles per state folder; the specs module organizes tokens under Default, Light with Enabled / Disabled / Hovered / Focused / Pressed (ripple) groupings.

## Accessibility summary

- People should be able to navigate to a radio button, select it, and get appropriate feedback based on input type using assistive technology.
- Selecting one radio button deselects any others; a group may start with one selected or none. Once a selection is made the group can't be deselected — provide a Not applicable/No option or a separate way to clear all (like Clear selection).
- Either the text label or the radio button itself selects the option.
- Do not apply density by default: it pushes targets below the 48×48 CSS-pixel recommendation; any controls used to enable denser layouts must themselves remain ≥ 48×48 CSS px.
- Keyboard: Tab moves focus into the group to the selected radio button (or the first if none); Shift + Tab enters at the selected one or the last; Arrows move focus and select the previous/next button, wrapping between first and last; Space selects a focused button and does nothing if already selected.
- Labeling: correctly linked UI text is announced before the component's role. A radio group's accessibility label typically equals its title with role Radio group; an individual button's label equals its adjacent text label.

## Captured spec tables

*Reproduced as captured from notes/articles/radio-button--*.json; sparse rows reflect the original scrape.*

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

| Attribute | Value |
|---|---|
| Icon size | 20dp |
| State layer size | 40dp |
| Target size | 48dp |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Moves focus into the group to the selected radio button, or the first if none are selected |
| Shift + Tab | Moves focus into the group to the selected radio button, or the last if none are selected |
| Arrows | Moves focus and selects the previous or next radio button. Wraps focus and selection between the first and last radio buttons. |
| Space | Selects a focused radio button. If already selected, does nothing. |
