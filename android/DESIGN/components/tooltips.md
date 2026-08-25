# Tooltips (M3)

> Source: https://m3.material.io/components/tooltips/overview · https://m3.material.io/components/tooltips/guidelines · https://m3.material.io/components/tooltips/specs · https://m3.material.io/components/tooltips/accessibility

## Variants/types

Two variants: plain tooltips (briefly describe a UI element; best for labelling elements with no text, like icon-only buttons and fields) and rich tooltips (additional context; can optionally contain a subhead, buttons, and hyperlinks; best for longer text like definitions or explanations).

M2 → M3 differences: new color mappings with dynamic-color compatibility, and rich tooltips have more rounded corners than M2's slightly rounded ones.

Availability: Design Kit (Figma) and Jetpack Compose — Available; Flutter, Android Views (MDC-Android), Web — Unavailable.

Don't hide critical information in tooltips (easy to miss) — use an interruptive dialog instead.

## Anatomy

Plain tooltip:

1. Container
2. Supporting text

Rich tooltip:

1. Subhead (optional)
2. Container
3. Supporting text
4. Text button (optional)

Subheads stay brief — ideally one line — summarizing the tooltip's message, and are important when the tooltip appears automatically (like on page load). Rich tooltips hold up to two text buttons, brief and side by side rather than stacked.

## Key dimensions

| Attribute | Value |
|---|---|
| Plain container height | 24dp |
| Plain padding | 8dp |
| Rich top padding | 12dp |
| Rich bottom padding | 8dp |
| Rich left and right padding | 16dp |

Placement distances: 4dp from a visual boundary (e.g., a button), 8dp without one (e.g., text baselines); below app bar elements at the same distance; rich-tooltip positions adjust in increments of 8dp to avoid going off-screen.

## States

The captured specs module organizes tokens under Default, Light with an Enabled folder only — no hover/focus/press interaction-state set was captured for tooltips themselves (they appear in response to parent-element interaction).

## Behavior

- Show by hovering the parent element on desktop or tapping-and-holding it on mobile; persistent rich tooltips only appear when clicked/tapped.
- Transient by default: both variants disappear 1.5 seconds after navigating away from the target region; triggering a new tooltip immediately closes any open one — display only one tooltip at a time.
- Plain placement: directly above the parent element by default (4dp with a visual boundary, 8dp without); below the element inside app bars.
- Rich placement: bottom right of the parent by default, adjusting position to avoid going off-screen; never cover the parent. On desktop, may appear centered below the parent and remain visible while moving within the target region.
- Persistent rich tooltips remain active when leaving the target region, disappearing only once another UI element is interacted with; they can introduce new features on page load. Hovering doesn't trigger them, and avoid using them on icon buttons.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Color roles captured: plain tooltip uses Inverse surface and Inverse on surface; rich tooltip uses Surface container, On surface variant, and Primary (the scraped role lists don't map each role to its element). Token sets are selectable per variant (Tooltip - Plain / rich) organized under Default, Light → Enabled.

## Accessibility summary

- Assistive technology users must be able to receive the tooltip message and activate a tooltip via keyboard or switch input.
- Tooltips without required actions should stay on screen long enough for people to receive the information without disrupting their flow; plain tooltips should linger briefly after the cursor moves away.
- Appearance: tooltips can show when an actionable element is hovered or focused (without hiding crucial information); rich tooltips can also appear when an element is selected instead of hovered/focused.
- Focus order: containers must not block important information or prevent completing actions; focus moves top-to-bottom between interactive elements within a rich tooltip; avoid trapping screen reader/keyboard focus — people should move linearly through the rest of the page.
- Keyboard: Tab focuses the button if available; Space or Enter activates the focused element.
- Labeling: tooltips should have the Tooltip role or similar; label all elements inside per their own accessibility guidance.

## Captured spec tables

*Reproduced as captured from notes/articles/tooltips--*.json; sparse rows reflect the original scrape.*

Availability & resources (`overview`):

| Type | Resource | Status | Design | Implementation |
|---|---|---|---|---|
|  |  |  |  |  |
|  | Design Kit (Figma) | Available |  |  |
|  |  |  |  |  |
|  | Flutter | Unavailable |  |  |
|  | android Jetpack Compose | Available |  |  |
|  | android Android Views (MDC-Android) | Unavailable |  |  |
|  | language Web | Unavailable |  |  |

Measurements (`specs`):

| Variant | Attribute | Value |
|---|---|---|
| Plain | Container height | 24dp |
| Plain | Padding | 8dp |
| Rich | Top padding | 12dp |
| Rich | Bottom padding | 8dp |
| Rich | Left and right padding | 16dp |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Focus lands on button, if available |
| Space or Enter | Activates the focused element |
