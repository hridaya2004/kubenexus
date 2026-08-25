# Badge (M3)

> Source: https://m3.material.io/components/badges/overview
> Source: https://m3.material.io/components/badges/guidelines
> Source: https://m3.material.io/components/badges/specs
> Source: https://m3.material.io/components/badges/accessibility

## Variants/types

- **Small badge** — plain circle; signals an unread notification without text.
- **Large badge** — container with label text; communicates item counts.
- Content limited to four characters, including a trailing `+`.
- Used on navigation bar, navigation rail, app bars, and tabs.

## Anatomy

- Small badge: container only (no text).
- Large badge: container + label text.
- Anchored inside the icon bounding box at the upper trailing edge of the icon; width grows with count while placement stays fixed.
- Mirror position for right-to-left languages; do not move arbitrarily or cover the icon.

## Key dimensions

| Attribute | Value |
|---|---|
| Small badge shape | 3dp corner radius |
| Small badge size (HxW) | 6dp |
| Large badge shape | 8dp corner radius |
| Large badge one digit size (HxW) | 16dp |
| Large badge max character count size (HxW) | 16x34dp |
| Small badge offset from icon top-trailing corner to bottom-leading badge corner (HxW) | 6x6dp |
| Large badge offset from icon top-trailing corner to bottom-leading badge corner (HxW) | 14x12dp |
| Large badge padding between badge and text container | 4dp |

## States

Badges have no interactive states of their own; they inherit context from their host component. Color roles are specified for enabled use in navigation bar and navigation rail. When a notification is read/selected, the badge is hidden.

For general interaction-state layering (hover/focus/press overlays), see design.md.

## Behavior

- Placed on the end edge of icons, typically inside another component.
- In navigation bars, hide the badge once its destination is selected.
- Keep default color mapping to avoid color conflicts with labels, icons, and navigation elements.

## Token group

Specs capture a single **Color** token group under `md.comp.badge.*`:

- Container / label color roles: Error, On error (navigation bar and navigation rail schemes, light and dark).

Measurements above map to the shape and size token slots of the same group.

## Accessibility summary

- Assistive technology must expose dynamic badge info (counts or labels); badges announce after their navigation destination.
- Numerical badges read the number; non-counting badges announce "New notification".
- Unread-notification badges hide once selected.
- Default colors require at least 3:1 contrast; avoid custom roles unless 3:1 contrast holds.

## Captured spec tables

### Specs — Measurements

| Attribute | Value |
|---|---|
| Small badge shape | 3dp corner radius |
| Small badge size (HxW) | 6dp |
| Large badge shape | 8dp corner radius |
| Large badge one digit size (HxW) | 16dp |
| Large badge max character count size (HxW) | 16x34dp |
| Small badge: distance from top trailing icon corner to bottom leading badge corner (HxW) | 6x6dp |
| Large badge: distance from top trailing icon corner to bottom leading badge corner (HxW) | 14x12dp |
| Large badge padding between badge and text container | 4dp |

### Overview — Availability & resources

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Flutter | Available |
| Implementation | Jetpack Compose | Available |
| Implementation | Android Views (MDC-Android) | Available |
| Implementation | Web | Unavailable |
