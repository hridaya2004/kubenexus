# Bottom sheets (M3)

> Source: https://m3.material.io/components/bottom-sheets/overview · /guidelines · /specs · /accessibility

Supplementary surface for secondary content and actions on mobile screens; content is additional, never the app's primary region, and can be dismissed to reach main content. Intended for compact and medium window sizes.

## Variants/types

Two variants:

- **Standard** — coexists with the screen's primary UI region; both stay visible and interactive (e.g., a music player over a browsing view). At full-screen height it carries a collapse icon in an app bar to return to its initial position; supports preset positions from full-screen height down to a preview.
- **Modal** — blocking, scrim-backed alternative to inline menus or simple dialogs on mobile; must be confirmed/dismissed before other app interaction. Mobile only.

M2 → M3: new color mappings + dynamic color; 28dp top corner radius; new 640dp max-width; optional drag handle with accessible 48dp hit target.

## Anatomy

- **Container** — the only required element; sized by its contents
- **Drag handle** (optional) — selects/drags through preset heights
- **Scrim** (modal only)
- Optional content: list items (labels, icons, text buttons), dividers, media (thumbnail/avatar/logo, images, video)

## Key dimensions

| Attribute | Value |
|---|---|
| Top corner radius | 28dp |
| Width | Full width, up to max-width 640dp |
| Height | Variable |
| Drag handle alignment (horizontal) | Center |
| Drag handle padding top/bottom | 22dp |
| Drag handle touch target | 48dp (top portion of sheet when resizable) |
| Top margin | 72dp |
| Top margin (window width > 640dp) | 56dp |
| Start/end margin (window width > 640dp) | 56dp |

## States

Enabled (modal sheets render above a scrim; standard has none). Specs token module organizes values under an Enabled state folder with Color group.

State-layer model shared across components: hover 8%, focus 10%, pressed 10%, drag 16% content-color overlay — see design.md.

## Behavior

- Initial modal position capped at 50% of screen height so top actions stay reachable; taller sheets pull up full-screen and scroll internally
- Modal dismissal: tapping a menu item/action, tapping the scrim, swiping the sheet down, or an in-sheet close affordance
- Selecting the drag handle cycles preset heights; selecting the scrim always closes; if no drag handle, Material requires a single-pointer alternative for resizing
- Horizontally scrollable independent of the screen's content
- Compact: full screen width, elevated above primary content. Medium/expanded: default max-width applies (overrideable); complex tasks belong on non-transient surfaces like floating sheets; desktop swaps to a side sheet
- Android predictive back: swipe left/right detaches sheet edges, previews previous screen; release/fling commits, cancel restores

Color roles: Scrim* · On surface variant · Surface container low. *On Android the scrim color/opacity is system-managed.

## Token group

Token set "Sheets - Bottom" captured at Default, Light context with state folder Enabled → prefix `md.comp.sheet.bottom.*` per the `md.comp.<component>.<element>.<attribute>` grammar in tokens.md (modal variant within the same family).

## Accessibility summary

Users must be able to resize sheets without touch gestures. The top 48dp of the sheet is the interactive area when user-initiated resizing is available with a drag handle present. The drag handle participates in tab order and works from keyboard/switch input. Keys: Tab focuses the drag handle; Space/Enter toggles between available heights. Label only the drag handle; its accessibility role is "button". Any drag-only action needs a single-pointer alternative.

## Captured spec tables

Availability:

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Flutter | Available |
| android Android Views (MDC-Android) | | Available |
| android Jetpack Compose | | Available |
| language Web | | Unavailable |

Measurements:

| Attribute | Value |
|---|---|
| Drag handle alignment (horizontal) | Center |
| Drag handle padding top/bottom | 22dp |
| Top margin | 72dp |
| Top margin (window width > 640dp) | 56dp |
| Start/end margin (window width > 640dp) | 56dp |
| Width | Full width, up to max-width 640dp |
| Height | Variable |

Accessibility keyboard map:

| Keys | Actions |
|---|---|
| Tab | Focus lands on drag handle |
| Space / Enter | Toggles between available heights |
