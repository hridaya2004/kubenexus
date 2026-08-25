# Side sheets (M3)

> Source: https://m3.material.io/components/side-sheets/overview · /guidelines · /specs · /accessibility

Fixed-width supplementary surface docked to a window edge for contextual actions and information that coexist with primary content. Mostly medium-to-expanded breakpoints; modal form preferred at compact sizes.

## Variants/types

Two variants:

- **Standard** — stays visible beside primary content while people interact with it (filters, supplemental info); body area shrinks to accommodate it, keeping a margin on the body's trailing edge
- **Modal** — scrim-backed and blocking; same content kinds as standard but must be dismissed to reach underlying content; preferred on compact screens; can transition to standard at larger sizes

M2 → M3: RTL support with left-side sheets; new color mappings + dynamic color; modal side sheets get a 16dp corner radius.

## Anatomy

- **Container** — the only required element; sized by its contents
- **Headline**
- **Back icon button** (optional) — exits toward a different experience
- **Close icon button** (optional but strongly recommended; required by accessibility guidance)
- **Action buttons** (optional) — e.g., Save/Edit/Download; use elevation/fill/tone for emphasis
- **Divider** (optional) — separates action buttons from content, or user-generated from system-generated
- **Content** (optional)
- **Scrim** (modal)

Placement: screen edge (right in LTR to avoid left-edge navigation components), may be inset 16dp; RTL mirrors everything to the left edge.

## Key dimensions

Standard:

| Attribute | Value |
|---|---|
| Start/end padding | 24dp |
| Padding between top elements | 12dp |
| Bottom actions height | 72dp |
| Bottom actions top padding | 16dp |
| Bottom actions bottom padding | 24dp |
| Bottom actions alignment (horizontal) | Left |
| Max-width | 400dp |
| Margins (when detached) | 16dp |

Modal adds one attribute; all others identical:

| Attribute | Value |
|---|---|
| Start padding with icon | 16dp |

Corner radius (modal): 16dp.

## States

Token module captured with state folders: Enabled · Hovered · Focused · Pressed (ripple).

State-layer model shared across components: hover 8%, focus 10%, pressed 10%, drag 16% content-color overlay — see design.md.

## Behavior

- Scrolls vertically independent of the page; scroll position persists; never scrolls horizontally
- Standard entrance shrinks the body area while preserving its trailing margin
- Android predictive back: swipe detaches the sheet from top/bottom edges, previews the previous screen; sheet/content scale in gesture direction; release/fling commits
- Default width is resizeable per layout needs

Color roles — standard: Outline variant · On surface variant · Surface. Modal: On surface variant (icons/close) · Surface container low · On surface variant.

## Token group

Token set "Sheets - Side" captured at Default, Light context under Enabled/Hovered/Focused/Pressed folders → prefix `md.comp.sheet.side.*` (standard/docked) and `md.comp.sheet.modal.side.*` (modal), following the `md.comp.<component>.<element>.<attribute>` grammar in tokens.md.

## Accessibility summary

Users must be able to dismiss a side sheet via assistive technology, so Material requires an always-present close affordance (without one, open/close behavior and transience are unpredictable). Actions are reachable through tab order with keyboard or switch input. Keys: Tab focuses the next non-disabled icon button; Space or Enter activates it. The accessibility role of a side sheet is Dialog.

## Captured spec tables

Availability:

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Flutter | Unavailable |
| android Jetpack Compose | | Unavailable |
| android Android Views (MDC-Android) | | Available |
| language Web | | Unavailable |

Standard measurements:

| Attribute | Value |
|---|---|
| Start/end padding | 24dp |
| Padding between top elements | 12dp |
| Bottom actions height | 72dp |
| Bottom actions top padding | 16dp |
| Bottom actions bottom padding | 24dp |
| Bottom actions alignment (horizontal) | Left |
| Max-width | 400dp |
| Margins (when detached) | 16dp |

Modal measurements:

| Attribute | Value |
|---|---|
| Start/end padding | 24dp |
| Start padding with icon | 16dp |
| Padding between top elements | 12dp |
| Bottom actions height | 72dp |
| Bottom actions top padding | 16dp |
| Bottom actions bottom padding | 24dp |
| Bottom actions alignment (horizontal) | Left |
| Max-width | 400dp |
| Margins (when detached) | 16dp |

Accessibility keyboard map:

| Keys | Tab | Space or Enter |
|---|---|---|
| Actions | Focus lands on (non-disabled) icon button | Activates the (non-disabled) icon button |
