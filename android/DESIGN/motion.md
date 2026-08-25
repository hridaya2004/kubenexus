# M3 Motion — Reference Specsheet

> Original summary of factual specifications from
> https://m3.material.io/styles/motion/overview (physics system),
> https://m3.material.io/styles/motion/easing-and-duration and
> https://m3.material.io/styles/motion/transitions (verified live, Aug 2026).
> Facts only; prose paraphrased. Source licensed Apache 2.0 / CC BY 4.0.

## Two systems, one transition

- **Motion physics system** (May 2025, M3 Expressive): spring-based; replaces easing+duration for components/interactions. The legacy easing/duration system still drives transitions and is frozen (no longer maintained).
- Implementation status at crawl time: Jetpack Compose — available (21 components use physics by default); Android Views — available, not yet in components; Web — compatible via Compose spring conversions; Flutter — unavailable.

## Physics system

### Motion schemes
Two presets, swappable at product level:
- **Expressive** — opinionated; spatial springs **overshoot** the target (bounce). For hero moments and key interactions.
- **Standard** — functional, minimal bounce; eases into final values. For utilitarian products.
Custom schemes are supported (`MotionScheme` on Compose) at three levels of customization: default scheme → custom scheme → per-element scheme swap.

### Spring anatomy & tokens
A spring = **stiffness + damping + visual velocity**. Token pattern:
`md.sys.motion.spring.fast.spatial` (scheme is applied at product level, not inside the token).

| Axis | Values |
|---|---|
| Style | `spatial` (position/rotation/size/corners — may overshoot) · `effects` (color/opacity — never overshoots) |
| Speed | `fast` (small elements: switches, buttons) · `default` (partial-screen: bottom sheets) · `slow` (full-screen) |

Absolute values adapt per device class (wearable/phone/tablet) so relative speed feels consistent.

## Legacy easing & duration (transitions)

Suggested pairs from the live page:

| Easing | Duration | Transition shape |
|---|---|---|
| Emphasized | 500ms | begin + end on screen |
| Emphasized decelerate | 400ms | enter screen |
| Emphasized accelerate | 200ms | exit screen (permanent) |
| Standard | 300ms | begin + end on screen |
| Standard decelerate | 250ms | enter screen |
| Standard accelerate | 200ms | exit screen |

Selection logic:
- **Emphasized set** is the default recommendation; **Standard set** for small utility transitions and as fallback where emphasized curves aren't supported (iOS/Web).
- Enter transitions run longer than exits (exits get out of the way faster); duration scales with the screen area traversed.
- Temporary exits (drawer) use full "emphasized" ease so the element feels retrievable; permanent exits end at peak velocity ("accelerate").

## Six transition patterns

1. **Container transform** — element expands into a detail view (card→detail page). Strongest start/end relationship; uses persistent elements.
2. **Forward & backward** — hierarchical navigation with horizontal slide; Android adds fade, iOS uses parallax.
3. **Lateral** — peer-level navigation (tabs, carousels); grouped elements slide in unison, no fade/parallax.
4. **Top level** — top destinations via nav bar/rail/drawer; quick outgoing fade then incoming fade, no persistent elements.
5. **Enter & exit** — within bounds (dialogs, menus, snackbars: expand along x/y away from nearest edge) or across bounds (sheets, drawers, app bars sliding during scroll). Scale/z-axis motion avoided — implies elevation change, which M3's reduced-elevation model doesn't use.
6. **Skeleton loaders** — placeholder shimmer while content loads.

## Section map (pages crawled)

- `/styles/motion/overview` (+ `how-it-works`) — physics system
- `/styles/motion/easing-and-duration` (+ `applying-easing-and-duration`) — legacy curves/timings
- `/styles/motion/transitions` (+ `transition-patterns`) — pattern catalog
