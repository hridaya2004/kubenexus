# Dialog (M3)

> Source: https://m3.material.io/components/dialogs/overview
> Source: https://m3.material.io/components/dialogs/guidelines
> Source: https://m3.material.io/components/dialogs/specs
> Source: https://m3.material.io/components/dialogs/accessibility

## Variants/types

- **Basic dialog** — modal window for critical info or decisions: alerts, quick selection, confirmation; may host lists, date pickers, time pickers.
- **Full-screen dialog** — fills the screen for multi-step tasks (e.g., creating a calendar entry); the only dialog over which other dialogs may appear; compact breakpoints only (use basic dialogs at medium/expanded).

M2→M3 changes: new colors, larger corner radius, more padding, optional custom basic-dialog positioning, larger/darker headline.

## Anatomy

Basic dialog:

- Container, Icon (optional), Headline (optional), Supporting text, Divider (optional), Button label text, Scrim.

Full-screen dialog:

- Container, Header region, Icon (close affordance), Headline (optional), Button label text, Divider (optional).

Containers sit above app content; surfaces behind are dimmed with a scrim.

## Key dimensions

### Basic dialog

| Attribute | Value |
|---|---|
| Container shape | 28dp corner radius |
| Container height | Dynamic |
| Container width | Min 280dp; Max 560dp |
| Divider height | 1dp |
| Icon size | 24dp |
| Alignment with icon | Center-aligned |
| Alignment without icon | Start-aligned |
| Top/left/right/bottom padding | 24dp |
| Padding between buttons | 8dp |
| Padding between title and body | 16dp |
| Padding between icon and title | 16dp |
| Padding between body and actions | 24dp |

### Full-screen dialog

| Attribute | Value |
|---|---|
| Container shape | 0dp corner radius |
| Container height | Dynamic |
| Container width | Container width; Max 560dp |
| Header height / width | 56dp / Container width |
| Headline text alignment | Start-aligned |
| Divider height | 1dp |
| Icon (close affordance) size | 24dp |
| Bottom action bar height / width | 56dp / Container width |
| Top/left/right padding | 24dp |
| Padding between elements | 8dp |

## States

Dialog states captured in specs: Enabled, Hovered, Focused, Pressed (ripple). Scrim has no interactive states.

State layers are semi-transparent overlays of the content color at fixed opacities — see design.md.

## Behavior

- Modal: blocks app functionality until confirmed, dismissed, or a required action completes. Use sparingly; prefer snackbars/menus for low- or medium-priority messages.
- Max two actions: one action must be an acknowledgement; two actions = confirming + dismissing. Dismissive never disabled; disable confirm until a choice is made when appropriate.
- Buttons align to the trailing edge with the confirmation button closest to the edge; mirrored in RTL.
- Headlines: brief statements/questions; avoid apologies, alarms, ambiguity ("Are you sure?"); can wrap to two lines or truncate; long full-screen headlines move into the content area, not the app bar.
- Full-screen: Save confirms; close icon/Cancel/Back dismisses; closing without saving raises a basic discard-confirmation dialog; avoid vague confirmations (Done/OK/Close); don't disable confirmation.
- A FAB can transition into a full-screen dialog via container transform.

## Token group

Specs capture per-variant **Color** token groups under `md.comp.basic-dialog.*` and `md.comp.full-screen-dialog.*`:

- Basic dialog roles: surface-container-high, secondary, on-surface, on-surface-variant, primary, scrim.
- Full-screen dialog roles: surface-container-high, on-surface, primary, on-surface-variant.

Shape/padding values above map to the same groups' shape and spacing slots.

## Accessibility summary

- Assistive tech must open/close the dialog, submit inputs, and scroll overflowing content.
- Use sparingly — dialogs interrupt screen-reader page flow; non-critical info belongs inline.
- Initial focus lands automatically on the first interactive element; focus cycles within the dialog via Tab / Shift+Tab; Space or Enter triggers the focused action; Escape closes.
- At 200% text size keep headlines within four lines; if truncated, offer one-tap access to the full text.
- On web, basic dialogs use the `alertdialog` role; label matches the title/headline; contained components follow their own guidelines.

## Captured spec tables

### Specs — Basic dialog measurements

| Attribute | Value |
|---|---|
| Container shape | 28dp corner radius |
| Container height | Dynamic |
| Container width | Min 280dp; Max 560dp |
| Divider height | 1dp |
| Icon size | 24dp |
| Minimum width | 280dp |
| Maximum width | 560dp |
| Alignment with icon | Center-aligned |
| Alignment without icon | Start-aligned |
| Top/Left/right/bottom padding | 24dp |
| Padding between buttons | 8dp |
| Padding between title and body | 16dp |
| Padding between icon and title | 16dp |
| Padding between body and actions | 24dp |

### Specs — Full-screen dialog measurements

| Attribute | Value |
|---|---|
| Container shape | 0dp corner radius |
| Container height | Dynamic |
| Container width | Container width; Max 560dp |
| Header height | 56dp |
| Header width | Container width |
| Headline text alignment | Start-aligned |
| Divider height | 1dp |
| Icon (close affordance) size | 24dp |
| Bottom action bar height | 56dp |
| Bottom action bar width | Container width |
| Top/left/right padding | 24dp |
| Padding between elements | 8dp |

### Guidelines — Similar components

| Component | Importance | Action needed |
|---|---|---|
| Snackbar | Low importance | Optional: Snackbars may not have a button, and can disappear automatically |
| Dialog | High importance | Required: Dialogs block the main content until an action is confirmed |

### Accessibility — Keyboard navigation

| Keys | Actions |
|---|---|
| Tab | Focus lands on the next interactive element contained in the dialog, or the first element if focus is currently on the last element |
| Shift + Tab | Focus lands on the previous interactive element contained in the dialog, or the last element if focus is currently on the first element |
| Space or Enter | Triggers or commits the action of the focused element |
| Escape | Closes the dialog |

### Overview — Availability & resources

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Flutter | Available |
| Implementation | Jetpack Compose | Available |
| Implementation | Android Views (MDC-Android) | Available |
| Implementation | Web | Available |
