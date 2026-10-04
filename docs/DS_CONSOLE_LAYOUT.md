# Open-foldable console layout

The 0.2.0 design uses a two-screen handheld composition: a broad upper preview, a centered selected-title plaque kept clear of the hinge, and a centered grid of larger square icon cards surrounded by persistent left and right controls. It translates a supplied layout reference into native Material 3 Expressive components without copying third-party logos, game artwork or interface branding. Neutral surfaces support persisted System, Light and Dark modes. When dynamic colors are enabled on Android 12 or newer, the theme uses Android's wallpaper-derived Material color APIs; older versions use the neutral fallback. Runtime verification of these paths is recorded separately in [QA.md](QA.md).

Both display panels have four rounded corners and their own background; the lower center is slightly inset. The seam uses the shell background rather than a contrasting stripe. The launcher D-pad has four independent directional targets and an open center.

## Posture contract

Normal use requires a landscape app window with a reported horizontal `FoldingFeature` on the open inner display. Both `FLAT` and `HALF_OPENED` states are accepted. The reported hinge bounds are excluded from display, controls and touch mapping.

Missing fold information is not proof that a device is closed: it may also indicate a cover display, a small multi-window area, an external display or missing OEM support. Such windows receive instructions to open the inner display and use the app full-screen. Portrait and vertical-crease layouts receive rotation guidance. There is no ordinary-phone fallback or production posture bypass.

Android's system document picker and OS permission dialogs remain system-owned surfaces. They cannot contain EmuUI's controls. Returning to the app restores its console layout and applies the posture gate again.

## Display and input contract

- Launcher: upper pane devoted to the selected game's preview; central lower library tiles and shortcuts remain between visible control wings
- Single-screen game: the largest centered rectangle at the live core display aspect ratio that fits the rounded upper panel, with input controls below. The upper panel uses the full safe half without an outer margin. Rounded backing and shell masks stay outside the rectangular native pixels; rounding never crops the game image
- Nintendo DS: independently fitted 4:3 upper and lower native game screens from one zero-gap core framebuffer. The upper screen can use a larger scale than the lower. The lower panel has a 6 dp horizontal inset between the control wings and no vertical inset, so its screen remains centered horizontally and vertically in the lower safe half. No touch is accepted through the upper display, hinge, letterbox or control area
- Nintendo 3DS: inherited Citra availability does not imply supported playback. Its source-screen extraction and input mapping are not yet integrated and verified with this renderer; show explicit compatibility guidance instead
- Posture transitions: release held input and pause when the permitted layout is lost, retain gameplay state, and restore the valid console layout without recreating the native view solely because a hinge event arrived

The pinned-source LibretroDroid extension draws the upper and lower source regions into separate destination rectangles using one core instance, native view and emulation thread. Inverse destination-to-source mapping preserves the lower-screen stylus coordinates after independent scaling. A positive finite core display aspect ratio is used directly; only an invalid or absent ratio uses a base-size fallback corrected for frontend rotation.

Accessible button activation is observed across actual emulated frames instead of depending on a wall-clock tap duration. A pulse stays pressed for two core runs, then releases for one run before the next queued pulse. Physical holds have independent state. Losing the valid posture, pausing or shutting down cancels pending pulses; no stale press should resume later. Candidate-specific execution of these input protections is recorded in [QA.md](QA.md).

Rounded-panel fitting considers the actual corner arcs and a one-pixel raster guard, allowing letterboxed images to use the available panel area without a blanket corner-radius inset. A geometry change invalidates presentation even if the core returns an unchanged frame. Shader-chain changes wait for a real frame. EGL recreation rebuilds presentation objects without reloading the ROM or SRAM. Runtime evidence is required before claiming those paths work for a particular core or GPU; known filtering, duplicate-frame and context-loss limits are recorded in [libretrodroid/UPSTREAM.md](../libretrodroid/UPSTREAM.md).

## Launcher artwork and spacing

The selected game's existing cover URL is rendered through the production Coil image loader. The broad upper preview fits the complete artwork behind its gradient and overlays. Square library cards use a centered crop that reaches the card edge, rather than placing the artwork inside a padded miniature card. An original procedural cartridge illustration remains the placeholder when artwork is absent or has not loaded; it is not a gameplay screenshot.

Cards have a 12 dp corner radius. The outline is 1 dp in the theme's outline-variant color when unselected and 3 dp in its primary color when selected. Selection uses the border and accessibility semantics only, with no checkmark or replacement selection badge. Full game titles remain available to accessibility semantics even though the icon cards have no visible caption; the selected title appears in the upper plaque. Incomplete rows are centered, and longer collections remain lazy and scrollable.

The four upper corner surfaces share the tallest intrinsic height, with a 48 dp minimum, so the brand, tools, game count and play action align at large text sizes. Filters and the shortcut dock occupy equal-height centered bands with matching 20 dp top and bottom spacing. These adjustments preserve the separate lower control wings and the central game shelf.

The title plaque is measured within the space between the lower corner controls and below the upper controls. If the window cannot contain the measured preview or a usable game shelf, the app shows an expand-window message while retaining selection instead of overlapping or clipping those controls.

Rounded native gameplay panels use safe inner rectangular viewports. The backing, corner masks and hinge exclusion stay outside the native pixels, so decorative rounding does not crop either Nintendo DS screen or a single-screen game's image.

## Verification boundaries

Instrumentation can publish synthetic WindowManager folding features on an ordinary emulator. Those tests validate the actual app's layout, state and native gameplay reactions to supplied metadata; they do not certify a physical hinge sensor, cover-to-inner-display transfer, OEM extensions or a particular phone model. Screenshots and reports must identify synthetic posture inputs.

An original local artwork fixture can verify actual Coil loading, card cropping, borders and theme-dependent rendering without including commercial game artwork. Such a check does not prove that a remote cover provider is reachable, that network downloads succeed, or that every imported title has a matching cover.

The source-specific executed results belong in [QA.md](QA.md). A layout specification or successful build is not evidence that its runtime checks passed.

## Android references

- [FoldingFeature API](https://developer.android.com/reference/androidx/window/layout/FoldingFeature)
- [Detecting device type and interpreting missing folding features](https://android-developers.googleblog.com/2023/06/detecting-if-device-is-foldable-tablet.html)
- [Testing foldable layouts with WindowManager](https://developer.android.com/codelabs/android-window-manager-dual-screen-foldables)
