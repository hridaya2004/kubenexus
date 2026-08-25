# Time pickers (M3)

> Source: https://m3.material.io/components/time-pickers/overview · https://m3.material.io/components/time-pickers/guidelines · https://m3.material.io/components/time-pickers/specs · https://m3.material.io/components/time-pickers/accessibility

## Variants/types

Two variants: dial and input. Time pickers are modal and cover the main content; people can select hours, minutes, or periods of time, and selection should be easy by hand on a mobile device. They suit scenarios like setting an alarm or scheduling a meeting — not nuanced or granular time selection such as stopwatch milliseconds.

Differences from M2: new color mappings compatible with dynamic color (only change). Availability: Design Kit (Figma); Flutter, Jetpack Compose, Android Views (MDC-Android) — Available; Web — Unavailable.

## Anatomy

Dial picker:

1. Label (headline)
2. Time selector separator
3. Input field / input text (selected)
4. Period selector container, outline, and text (selected/unselected)
5. Container
6. Dial selector track
7. Dial label (selected/unselected)
8. Text buttons
9. Icon button

Input picker:

1. Label (headline)
2. Time selector separator
3. Input field / input text (selected)
4. Period selector container, outline, and text (selected/unselected)
5. Container
6. Text buttons
7. Icon button

The specs anatomy adds clock dial selector center, clock dial selector track/container, time selector label text, time selector container, time input field supporting text/label text/container.

The input selector is a unique kind of text field: it adds a highlight on the selected field, a larger shape/size/font, and a label below the field. Hours and minutes have separate inputs; for a 12-hour clock an AM/PM selector sits to the right of minutes and shouldn't appear on a 24-hour clock.

## Key dimensions

Time picker dial (vertical; horizontal layout captures identical values):

| Element | Attribute | Value |
|---|---|---|
| Container | Width / Height | Dynamic |
| Container | Headline alignment | Left |
| Container | Top/bottom padding | 24dp |
| Container | Left/right padding | 24dp |
| Time selector container | Width | 96dp |
| Time selector container | Width (24h vertical) | 114dp |
| Time selector container | Height | 80dp |
| Period selector container | Width (vertical layout) | 52dp |
| Period selector container | Height (vertical layout) | 80dp |
| Period selector container | Width (horizontal layout) | 216dp |
| Period selector container | Height (horizontal layout) | 38dp |
| Clock dial container | Size | 256dp |
| Clock dial selector handle | Size | 48dp |
| Clock dial selector center | Size | 8dp |
| Clock dial selector track | Width | 2dp |

Time picker input:

| Element | Attribute | Value |
|---|---|---|
| Container | Width / Height | Dynamic |
| Container | Headline alignment | Left |
| Container | Top/bottom padding | 24dp |
| Container | Left/right padding | 24dp |
| Time input field container | Width | 96dp |
| Time input field container | Height | 72dp |
| Period selector container | Width | 52dp |
| Period selector container | Height | 72dp |

## States

Enabled · Hover · Focus · Pressed (state folders in the token module: Enabled, Hovered, Focused, Pressed (ripple)). States specs can be found in the token module above per the captured notes.

## Behavior

- Two primary selection methods: type a specific value into the hour and minute fields, or select the hour/minute field from the text input and adjust the clock dial to simultaneously update the corresponding field.
- Dial selectors always mimic a round watch face; hours and minutes are selected by tapping a number or dragging the dial selector track. On a 12-hour dial all numbers sit in the outer ring; on a 24-hour dial even numbers sit in an inner ring and odd numbers in the outer ring.
- Icon buttons switch between selectors: keyboard icon → input selector, clock icon → dial selector. Text buttons exit the dialog (Cancel) and save the input (OK).
- Landscape: stacked input and selection options position side-by-side; the picker can swap orientation or variant based on device orientation and viewport constraints, falling back to the input picker when vertical space is insufficient to show landscape without scrolling.
- Placement: time pickers shouldn't be obscured or cropped by other elements or screen edges — they should change orientation or variant to remain fully visible. They are modal windows above a scrim that focuses attention.
- Density: don't apply density to the time picker dial when the viewport is constrained — use an input picker instead.
- Appearing/disappearing: like dialogs, time pickers use an enter and exit transition pattern. Exit via OK (confirm) or Cancel (dismiss); interacting outside the dialog also dismisses it; otherwise it retains focus.
- Scrolling: avoid scrolling — swap orientation or variant instead; time pickers don't scroll with elements outside the modal window.
- 24-hour time selection is set outside the component, typically through system settings.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Color resolves through the color roles listed for the dial and input variants — including On surface variant, On surface, Surface container highest, Tertiary container, On tertiary container, Surface container high, Outline, Primary, On primary, Primary container, On primary container — organized under Default, Light with Enabled / Hovered / Focused / Pressed (ripple) groupings.

## Accessibility summary

- Assistive technology users should be able to select or enter hours/minutes (and in some cases seconds/milliseconds), choose formats including 24-hour and AM/PM views, and enter time manually through input fields.
- Manual entry must be possible through text input rather than exclusively through the dial; if a screen can't display the dial, consider showing only the input selector (currently on Android Views the dial selector is always visible). The input selector must be reachable from the dial via the keyboard icon.
- Targets for dial selectors should be 48×48dp.
- Keyboard: Tab lands focus on the (non-disabled) time slot; Space or Enter activates it.
- Labeling: if input text is correctly linked, assistive tech reads the role first, then the UI text. Hour and minute fields have the text-input role; the dial selector reads selections as totals, e.g., "Hour 7 of 12".
- Roles (Wiz and Jetpack Compose / Android Views): Hour and Minute inputs — Text input / -; AM/PM selection — Radio button (in list) / Checkbox (in list); Keyboard button "Toggle input picker" — Button / Button; Clock button "Toggle dial picker" — Button / Button; Cancel and OK — Button / Button; clock dial time selection "{Value} Hours or minutes of {Total}" — Button / -.
- For time selection that doesn't require a dial view, make the time input picker the default option.

## Captured spec tables

*Reproduced as captured from notes/articles/time-pickers--*.json; sparse rows reflect the original scrape.*

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

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Focus lands on (non-disabled) time slot |
| Space or Enter | Activates the (non-disabled) time slot |

Labeling elements — dial selector (`accessibility`):

| Element | Accessibility label | Role (Wiz and Jetpack Compose) | Role (Android Views) |
|---|---|---|---|
| Hour input (input picker) | Hour | Text input | - |
| Minutes input | Minute | Text input | - |
| AM/PM selection | AM or PM | Radio button (in list) | Checkbox (in list) |
| Keyboard button | Toggle input picker | Button | Button |
| Cancel button | Cancel | Button | Button |
| OK button | OK | Button | Button |
| Clock dial time selection (dial selector) | {Value} Hours or minutes of {Total} | Button | - |

Labeling elements — input selector (`accessibility`):

| Element | Accessibility label | Role (Wiz and Jetpack Compose) | Role (Android Views) |
|---|---|---|---|
| Hour input (input picker) | Hour | Text input | - |
| Minutes input | Minute | Text input | - |
| Clock button | Toggle dial picker | Button | Button |
| Cancel button | Cancel | Button | Button |
| OK button | OK | Button | Button |
