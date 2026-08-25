# M3 Components — Reference Specsheet

> Original summary of the component catalog at https://m3.material.io/components
> (verified live, Aug 2026 via component-page side-navigation).
> Source licensed Apache 2.0 / CC BY 4.0.

## Catalog structure

Every family follows a four-article pattern:
`/components/<family>/overview` · `/guidelines` · `/specs` · `/accessibility`

Specs tabs contain token tables grouped by interaction state, then by component element.

## Component families (37 discovered)

### Actions
- Buttons (`all-buttons` hub; `buttons`; `icon-buttons`; `segmented-buttons`; `button-groups`; `split-button` — expressive-era additions)
- Floating action button (`floating-action-button`, `extended-fab`, `fab-menu`)
- Toolbars

### Communication
- Badges
- Progress indicators
- Loading indicator (expressive)
- Snackbar
- Dialogs
- Menus
- Tooltips
- Carousel (expressive)

### Containment
- Cards
- Bottom sheets
- Side sheets
- Divider

### Selection
- Checkbox
- Radio button
- Switch
- Chips
- Sliders
- Date pickers
- Time pickers
- Lists

### Navigation
- App bars
- Navigation bar
- Navigation rail
- Navigation drawer
- Tabs
- Search

### Text input
- Text fields

## Specsheet files

Each family has a dedicated specsheet under `components/`, distilled from its four live articles (overview · guidelines · specs · accessibility):

| Group | Files |
|---|---|
| Actions | `all-buttons.md` · `buttons.md` · `icon-buttons.md` · `segmented-buttons.md` · `button-groups.md` · `split-button.md` · `floating-action-button.md` · `extended-fab.md` · `fab-menu.md` · `toolbars.md` |
| Communication | `badges.md` · `progress-indicators.md` · `loading-indicator.md` · `snackbar.md` · `dialogs.md` · `menus.md` · `tooltips.md` · `carousel.md` |
| Containment | `cards.md` · `bottom-sheets.md` · `side-sheets.md` · `divider.md` |
| Selection | `checkbox.md` · `radio-button.md` · `switch.md` · `chips.md` · `sliders.md` · `date-pickers.md` · `time-pickers.md` · `lists.md` |
| Navigation | `app-bars.md` · `navigation-bar.md` · `navigation-rail.md` · `navigation-drawer.md` · `tabs.md` · `search.md` |
| Text input | `text-fields.md` |

Every specsheet's sections: Variants/types · Anatomy · Key dimensions · States · Behavior · Token group · Accessibility summary · Captured spec tables.

## Expressive-update families highlighted on the homepage

button-groups · split-button · toolbars · progress-indicators (+ loading indicator, fab-menu, carousel from side-nav)

## Emphasized-typography integration

Components that pair well with emphasized type styles per the typography page: badges, primary-action buttons, extended FAB, selected list items, selected menu items. Components keep baseline type by default; emphasized tokens must be swapped in explicitly.

## Motion integration

21 Jetpack Compose components use the spring-based motion physics system by default (May 2025 status), driven by `md.sys.motion.spring.{fast,default,slow}.{spatial,effects}` tokens.

## Develop-side libraries referenced

- `/develop/android/jetpack-compose`
- `/develop/android/mdc-android`
- `/develop/flutter`
- `/develop/web`
