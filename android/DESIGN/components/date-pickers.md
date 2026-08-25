# Date pickers (M3)

> Source: https://m3.material.io/components/date-pickers/overview · https://m3.material.io/components/date-pickers/guidelines · https://m3.material.io/components/date-pickers/specs · https://m3.material.io/components/date-pickers/accessibility

## Variants/types

Three variants: docked date picker, modal date picker, modal date input. Date pickers can display past, present, or future dates; they should clearly indicate important dates such as current and selected days and follow common patterns like a calendar view.

Differences from M2: titles and labels are larger with increased spacing to accommodate the 48dp target size; new color mappings compatible with dynamic color (M2 had a drop shadow). Variant names are no longer device-dependent — the former desktop date picker is now the docked date picker; the former mobile date picker and date input are now the modal date picker and modal date input to reinforce that the user must take an action.

Availability: Design Kit (Figma); Flutter, Jetpack Compose, Android Views (MDC-Android) — Available; Web — Unavailable.

## Anatomy

- Docked date picker: text field, menu button, icon button, label text, menu, text buttons.
- Modal date picker (day selection): headline, supporting text, container, icon button, previous/next month buttons, day-of-week labels, today's date, unselected date, selected date, menu button, text buttons, divider. Year selection adds unselected year / selected year.
- Modal date input: headline, supporting text, container, icon button, date input, text buttons, divider.
- Full-screen date picker (compact): headline, supporting text, icon button, container, text button, icon button, divider, day-of-week labels, today's date, selected date range, unselected date, text buttons, selected date range start date, month label.

Date pickers can be embedded into dialogs on compact breakpoints (like mobile) or text field drop-downs on medium and expanded breakpoints (like tablet and desktop).

## Key dimensions

| Attribute | Value |
|---|---|
| Touch target size | 48×48dp minimum for all elements |
| Responsive scaling | Docked and modal picker sizing doesn't scale responsively across breakpoints |

The captured specs notes carry no numeric measurement tables — only element/color/state listings per configuration.

## States

Element states for date and year selection: Default (enabled) · Disabled · Hovered · Focused · Pressed (ripple).

## Behavior

- Docked: displays a date input field by default; a dropdown calendar appears when the input field is tapped, and either form of entry can be interacted with. Dates can be added via keyboard or the calendar UI, both immediately available on access. Month and year selection navigate with back/next arrows or by tapping the dropdown menus. The picker adjusts size dynamically and is ideal for both near and distant past/future dates.
- Modal: navigate months by horizontal swipe, years by vertical scroll; tap the year to open the year picker. Don't use a modal date picker for distant past/future dates such as a date of birth — use a modal input or docked picker instead. Range selection provides start and end dates (tap both on the calendar; scroll vertically across months) for use cases like booking a flight or reserving a hotel.
- Modal input: manual numeric keyboard entry of a date or range in a dialog; can be the default view when a calendar isn't needed, or replaced by a text field with hint text in a form. Swap between modal picker and input using the edit or calendar icon.
- Breakpoints: on compact breakpoints a full-screen modal date picker is recommended to increase readability and touch target size (it can cover the entire screen); the docked picker works best on medium and expanded breakpoints.
- Selection is indicated through color; in ranges, start and end dates are selected while dates between appear connected with a subtle highlight.
- Appearing/disappearing: modal pickers use an enter/exit transition pattern like other dialogs. Exit by confirming (OK) or dismissing (Cancel); interacting outside the dialog also dismisses it. Unless one of these actions occurs, the dialog retains focus. Mobile full-screen pickers add a close (×) icon button and Save confirmation. Docked pickers appear just below the input field.
- Don't scale the date picker responsively to a larger size.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Color resolves through color roles listed per variant/configuration — docked picker, docked picker menu, modal picker day/year/range selector, and modal input — organized under Default, Light state folders. Roles include Primary, On surface, On surface variant, Surface container high, Surface variant, Outline variant, On primary, Secondary container, On secondary container.

## Accessibility summary

- People should be able to enter dates manually as text without the picker and use multiple input methods; the docked picker's text field supports input, and on the modal picker the edit icon makes the date input available.
- The calendar icon is the exclusive entry point for the date picker — this keeps interaction optional for screen-reader and keyboard users and reduces key presses. Each input is a separate tab stop, improving discoverability.
- Format dates automatically only after Enter or on leaving the field; don't apply input masks while typing (it changes what screen-reader users typed). Accept a range of formats — dashes, spaces, slashes, dots, and leading zeros — to reduce errors.
- Remove the Clear button if not needed to reduce tab stops; provide keyboard shortcuts in tooltips and hint descriptions; truncated labels get tooltips (days of the week aren't keyboard-focusable, so their tooltip shows on hover only).
- Dates need at least 4.5:1 contrast between link text colors and background.
- Labeling: the text field's accessibility label states the input's purpose (e.g., event date) and matches the placeholder when empty; helper text below specifies the format (default "MM/DD/YYYY") and acts as the field's description. Screen readers verbalize the complete date — "Monday, August 17", not just "17".
- Keyboard: Enter/return closes the calendar and saves; Page up/down moves a month; Home/End jumps to the first day of the month; Shift + Page up/down moves a year; Shift + M / Shift + Y reach the month/year dropdowns.

## Captured spec tables

*Reproduced as captured from notes/articles/date-pickers--*.json; sparse rows reflect the original scrape.* (The specs scrape contained no measurement tables.)

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
| Enter/return | Closes the calendar and saves the selected date |
| Page up/down | Move to the same date on next/previous month |
| Home/End | Move to the first day of the month |
| Shift + Page up/down | Moves to the same date in the next/previous year |
| Shift + M | Moves to the month list dropdown |
| Shift + Y | Moves to the year list dropdown |

Labeling elements (`accessibility`):

| Element | A11y label | Role |
|---|---|---|
| Previous / next month and year | "{label}" | Button |
| Month and year dropdowns | "{label}" | Button |
| Days of the week | Column header |  |
| Month grid | Grid |  |
