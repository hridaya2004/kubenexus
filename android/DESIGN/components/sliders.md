# Sliders (M3)

> Source: https://m3.material.io/components/sliders/overview · https://m3.material.io/components/sliders/guidelines · https://m3.material.io/components/sliders/specs · https://m3.material.io/components/sliders/accessibility

## Variants/types

Three variants: standard (formerly "continuous"), centered, and range. The discrete slider is now the "stops" configuration. Sliders have five sizes — XS, S, M, L, XL — vertical and horizontal orientation, and an optional inset icon.

M3 Expressive update (May 2025): expressive configurations for orientation, shape sizes, and an inset icon; updated on Android Views (MDC-Android) and Jetpack Compose. Previous updates: Dec 2023 visual refresh for non-text contrast added the centered configuration and range selection, new shapes for tracks/handles (elements change shape when selected), handle width adjusts upon selection, tracks adjust shape when sliding to the edge, refreshed colors, a stop indicator, larger label text, a vertical handle that narrows when pressed, and centered sliders that start from the middle instead of the leading edge.

Availability: Design Kit (Figma); Flutter, Jetpack Compose (+ Expressive), Android Views (MDC-Android) (+ Expressive), Web — Available; Web: Expressive — Unavailable.

## Anatomy

1. Value indicator (optional)
2. Stop indicators (optional)
3. Active track
4. Handle
5. Inactive track
6. Inset icon (optional)

The track shows the full range of selectable values in two sections: active (minimum value to handle; between the two handles on range sliders) and inactive (handle to maximum, or outside both handles). For LTR languages values increase left to right; RTL is reversed. The handle moves along the track to choose a value and changes shape to indicate when it's pressed or dragged.

## Key dimensions

| Attribute | XS | S | M | L | XL |
|---|---|---|---|---|---|
| Track height | 16dp | 24dp | 40dp | 56dp | 96dp |
| Handle height | 44dp | 44dp | 52dp | 68dp | 108dp |
| Track shape | 8dp | 8dp | 12dp | 16dp | 28dp |
| Inset icon size | -- | -- | 24dp | 24dp | 32dp |

Size-independent:

| Attribute | Value |
|---|---|
| Label container height | 44dp |
| Label container width | 48dp |
| Handle width | 4dp |

Use larger sizes to increase targets and visual emphasis; XL is reserved for hero moments where the slider is the most important element on the page. Active and inactive tracks should always be the same size.

## States

Enabled · Disabled · Hovered · Focused · Pressed.

Interaction feedback: when tapped or dragged the handle width shrinks and the value appears; on hover the cursor changes, then click-and-drag shrinks the handle and shows the value.

Color roles used (light and dark schemes): inverse surface, inverse on surface, primary, on primary, secondary container, on secondary container.

## Behavior

- Changes must take effect immediately so people understand effects while moving the slider.
- Standard sliders select one value from a range — use when the slider should start from zero or the beginning of a sequence. Centered sliders select a value from a positive/negative range — use when zero or the default sits mid-range. Range sliders select two values with two handles defining minimum and maximum; avoid using range sliders vertically (extra cognitive load).
- Value indicator: appears when interacting with the corresponding handle; only one at a time on range sliders; not required if the value is shown elsewhere. A separate external text field can replace it, syncing both ways; make sure people can Tab to that field directly after the slider.
- Stop indicators: show predetermined values and the handle snaps to the closest stop. Avoid too many (visually crowded). All sliders have stops at the end of the inactive track ensuring at least 3:1 contrast; if the inactive track already has that contrast, end stops can be removed.
- Icons or text outside the slider (e.g., plus/minus) can indicate the range instead of stop indicators.
- Inset icon: standard sliders sized M, L, or XL only; illustrate what the slider controls; moves to the inactive track when there's no room on the active track (e.g., low value); consider swapping icons at zero (volume → mute). Don't use inset icons on track thicknesses under 40dp, or on centered/range sliders.
- Select & drag: drag the handle (smoothly, or snapping to stop indicators).
- Select jump: select part of the track; the handle moves to the location (or closest stop indicator).
- Select & arrow (keyboard): Tab focuses the handle; arrows increase/decrease by one value or stop; Space + arrows step by a larger interval or stop.

## Token group

`md.comp.slider.*` — token names appear in the captured specs notes: slider tokens are organized into a common token set plus a token set per size; to change size without presets, swap the default tokens `md.comp.slider.xsmall.[...]` with those of the desired size. State folders: Enabled / Disabled / Hovered / Focused / Pressed (ripple), under Default, Light.

## Accessibility summary

- Assistive technology users must be able to navigate to a slider, select a range by controlling a handle along the track, and get appropriate feedback based on input type.
- The shrinking handle plus appearing value provide the visual cue that the handle is being pressed (touch, cursor click-drag); hover changes the cursor.
- Initial focus lands directly on the handle — the primary interactive element — after which arrow keys or other keyboard navigation adjust the value.
- Contrast: use visual anchors so the end of the inactive track keeps ≥3:1 contrast against the background — the stop indicator makes the end visible on most backgrounds; alternatively icons or other elements with 3:1 contrast can mark the ends.
- Keyboard: Tab moves focus to the handle; Arrows increase/decrease by one value or one stop indicator; Space & Arrows step by one interval or one stop indicator; Home or End set the first and last values.
- Labeling: the accessibility label typically matches the slider's adjacent text label and carries the slider role; if UI text is correctly linked, assistive tech reads the UI text followed by the component's role. Icon buttons placed outside the slider should have the button role.

## Captured spec tables

*Reproduced as captured from notes/articles/sliders--*.json; sparse rows reflect the original scrape.*

Availability & resources (`overview`):

| Type | Resource | Status | Design | Implementation |
|---|---|---|---|---|
|  |  |  |  |  |
|  | Design Kit (Figma) | Available |  |  |
|  |  |  |  |  |
|  | Flutter | Available |  |  |
|  | android Jetpack Compose | Available |  |  |
|  | android Jetpack Compose: Expressive | Available |  |  |
|  | android Android Views (MDC-Android) | Available |  |  |
|  | android Android Views (MDC-Android): Expressive | Available |  |  |
|  | language Web | Available |  |  |
|  | language Web: Expressive | Unavailable |  |  |

Variants (`specs`):

| Variant | M3 | M3 Expressive |
|---|---|---|
| Standard | Available as "continuous" slider | Available |
| Centered | Available (web only) | Available |
| Range | Available | Available |
| Discrete | Available | Available as "stops" configuration |

Configurations (`specs`):

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Inset icon | No (default) | Available | Available |
| Inset icon | Yes | -- | Available |
| Orientation | Horizontal (default) | Available | Available |
| Orientation | Vertical | -- | Available |
| Size | XS (default) | Available | Available |
| Size | S, M, L, XL | -- | Available on Android Views (MDC-Android). Available as tokens on other platforms.* |
| Stop indicators | No (default), Yes | Available as "discrete" slider | Available |
| Value Indicator | No (default), Yes | Available | Available |

\* Configurations only available using tokens don't have implemented presets in code; swap `md.comp.slider.xsmall.[...]` tokens for the desired size.

Measurements (`specs`):

| Attribute | XS | S | M | L | XL |
|---|---|---|---|---|---|
| Track height | 16dp | 24dp | 40dp | 56dp | 96dp |
| Label container height | 44dp |  |  |  |  |
| Label container width | 48dp |  |  |  |  |
| Handle height | 44dp | 44dp | 52dp | 68dp | 108dp |
| Handle width | 4dp |  |  |  |  |
| Track shape | 8dp | 8dp | 12dp | 16dp | 28dp |
| Inset icon size | -- | -- | 24dp | 24dp | 32dp |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Moves focus to the slider handle |
| Arrows | Increase and decrease the value by one value or one stop indicator |
| Space & Arrows | Increase and decrease the value by one interval or one stop indicator |
| Home or End | Set the slider to the first and last values on the slider |
