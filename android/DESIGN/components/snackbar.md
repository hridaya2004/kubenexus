# Snackbar (M3)

> Source: https://m3.material.io/components/snackbar/overview · https://m3.material.io/components/snackbar/guidelines · https://m3.material.io/components/snackbar/specs · https://m3.material.io/components/snackbar/accessibility

## Variants/types

Single component; M3 clarified that snackbars can appear temporarily (dismissive) or persist until the user takes an action (non-dismissive). Captured configurations: single line, single line with action, two lines, two lines with action, two lines with longer action.

Differences from M2: new color mappings compatible with dynamic color. Availability: Design Kit (Figma); Flutter, Jetpack Compose, Android Views (MDC-Android) — Available; Web — Unavailable.

## Anatomy

1. Container
2. Supporting text
3. Action (optional)
4. Close button / icon (optional close affordance)

Snackbars contain a text label that directly relates to the process being performed; on compact breakpoints it can hold up to two lines.

## Key dimensions

| Attribute | Value |
|---|---|
| Container height (compact) | 48dp expanding vertically to 64dp for one or two lines of text |
| Compact margins | Fixed distance from leading, trailing, and bottom screen edges |
| Line length target (medium & expanded) | 40–60 characters ideal |

The captured specs notes carry no numeric measurement table.

## States

Enabled · Hovered · Focused · Pressed (ripple) — token folders under Default, Light; no disabled state captured.

## Behavior

- Snackbars inform users of a process the app has performed or will perform; they appear temporarily toward the bottom of the screen and shouldn't interrupt the user experience — people can browse content without interacting.
- Frequency: only one snackbar may be displayed at a time; consecutive snackbars queue one at a time. A snackbar with updated information can immediately replace an outdated one.
- Actions: a single text button at most ("Dismiss"/"Cancel" optional); colored text distinguishes it from the label. Snackbars shouldn't be the only way to access a core use case. Long actions can take a third line; "Undo" lets people amend choices; a dedicated dismiss action is unnecessary since snackbars disappear on their own by default.
- Text labels are short, clear process updates — one line when possible, up to two on mobile. Avoid icons (use a dialog if one is needed), stylized text, and inline links (add a button or use another component).
- Container: rectangular with a grey background and shadow to stand out against content; completely opaque so text stays legible, though slight transparency is acceptable while text remains clearly legible. Avoid significantly altering container shape; don't match label color to button color; don't use filled/elevated buttons; in wide layouts extend container width for longer labels.
- Placement: bottom of the UI, in front of main content; nudge upward to avoid overlapping FABs or docked toolbars. Never place snackbars in front of frequently used touch targets, navigation components, or FABs (above a FAB, not in front of or behind it). Full-width spans only when the UI has no persistent navigation like app bars/navigation bars; spanning snackbars can push FABs up when appearing. On web, don't let a snackbar fully cover elements in focus. In wider layouts, left- or center-align consistently; never flush to one layout edge or side-by-side consecutively.
- Responsive: compact expands 48→64dp as above; medium/expanded scale horizontally for longer strings, aiming for a single line plus optional button with flexible trailing-edge distance.
- Appearing/disappearing: snackbars appear without warning but never block interaction with page content. Without actions they can auto-dismiss after 4–10 seconds depending on platform; with actions they remain until acted on or dismissed. Avoid auto-dismissing snackbars on web unless there's also inline feedback.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Color resolves through inverse roles used for light and dark schemes — inverse surface, inverse on surface, inverse primary — organized under Default, Light with Enabled / Hovered / Focused / Pressed (ripple) groupings.

## Accessibility summary

- Users should be alerted but not disrupted when a snackbar appears, be able to move focus to an actionable snackbar, and act on it using assistive technology.
- Actionable snackbars shouldn't auto-dismiss, letting users read and interact at their own pace; non-actionable ones may auto-dismiss after a sufficient duration (common acceptable range: 4–10 seconds per platform), though this still presents difficulties on web without additional feedback.
- Web requirements: auto-dismissing snackbars must also communicate their information inline or near the triggering action (e.g., relabel "Save" to "Saved"), or become actionable so they persist until acted on. Material Web doesn't yet include the snackbar component, but this guidance applies to custom-made snackbars.
- Focus: announce the message without moving focus; don't trap focus — users navigate freely in and out. On web, provide a documented shortcut (like Alt+G) to move focus to actionable snackbars. On exit, focus ideally returns to the triggering element or the next most logical element; on Android Compose it may move to the nearest visible element or first actionable item.
- Announcements: use a live region with a polite (queued) announcement on Android and web rather than assertive; iOS 17+ uses polite announcements by default. A snackbar shown at app launch announces after the page title without receiving focus.
- Use the default standout color mapping to avoid color-conflict issues against UI elements.
- Keyboard: Tab moves focus between interactive elements; Esc dismisses the snackbar when in focus.

## Captured spec tables

*Reproduced as captured from notes/articles/snackbar--*.json; sparse rows reflect the original scrape.* (The specs scrape contained no measurement tables.)

Availability & resources (`overview`):

| Type | Resource | Status | Design | Implementation |
|---|---|---|---|---|
|  |  |  |  |  |
|  | Design Kit (Figma) | Available |  |  |
|  |  |  |  |  |
|  | Flutter | Available |  |  |
|  | android Jetpack Compose | Available |  |  |
|  | android Android Views (MDC-Android) | Available |  |  |
|  | language Web | Unavailable |  |  |

Configurations (`specs`):

| Configuration |
|---|
| Single line |
| Single line with action |
| Two lines |
| Two lines with action |
| Two lines with longer action |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Moves focus between interactive elements |
| Esc | Dismisses the snackbar when in focus |
