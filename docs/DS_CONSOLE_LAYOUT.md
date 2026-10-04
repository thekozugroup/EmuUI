# Open-foldable console layout

The 0.2.1 design extends the 0.2.0 two-screen handheld composition: a broad upper preview, a centered selected-title plaque kept clear of the hinge, and a centered grid of larger square icon cards surrounded by persistent left and right controls. It adds cutout-aware native fitting, complete side-control navigation and existing manual-save screenshot previews. It translates a supplied layout reference into native Material 3 Expressive components without copying third-party logos, game artwork or interface branding. Neutral surfaces support persisted System, Light and Dark modes. When dynamic colors are enabled on Android 12 or newer, the theme uses Android's wallpaper-derived Material color APIs; older versions use the neutral fallback. Runtime verification of these paths is recorded separately in [QA.md](QA.md).

Both display panels have four rounded corners and their own background; the lower center is slightly inset. The seam uses the shell background rather than a contrasting stripe. The launcher D-pad has four independent directional targets and an open center.

## Posture contract

Normal use requires a landscape app window with a reported horizontal `FoldingFeature` on the open inner display. Both `FLAT` and `HALF_OPENED` states are accepted. The reported hinge bounds are excluded from display, controls and touch mapping.

Missing fold information is not proof that a device is closed: it may also indicate a cover display, a small multi-window area, an external display or missing OEM support. Such windows receive instructions to open the inner display and use the app full-screen. Portrait and vertical-crease layouts receive rotation guidance. There is no ordinary-phone fallback or production posture bypass.

Android's system document picker and OS permission dialogs remain system-owned surfaces. They cannot contain EmuUI's controls. Returning to the app restores its console layout and applies the posture gate again.

## Display and input contract

- Launcher: upper pane devoted to the selected game's preview; central lower library tiles and shortcuts remain between visible control wings
- Single-screen game: the largest rectangle at the live core display aspect ratio that fits the rounded upper panel and avoids reported camera/notch occlusions, with input controls below. The upper panel uses its full half without an outer margin or a blanket cutout-edge inset. The native image stays centered unless avoiding a physical cutout requires a shift. Rounded backing and shell masks stay outside the rectangular native pixels; rounding never crops the game image
- Nintendo DS: independently fitted 4:3 upper and lower native game screens from one zero-gap core framebuffer. The upper screen can use a larger scale than the lower and avoids the reported camera/notch rectangles. The lower panel has a 6 dp horizontal inset between the control wings and no vertical inset, so its screen remains centered horizontally and vertically in the lower safe half. No touch is accepted through the upper display, hinge, letterbox or control area
- Nintendo 3DS: inherited Citra availability does not imply supported playback. Its source-screen extraction and input mapping are not yet integrated and verified with this renderer; show explicit compatibility guidance instead
- Posture transitions: release held input and pause when the permitted layout is lost, retain gameplay state, and restore the valid console layout without recreating the native view solely because a hinge event arrived

The pinned-source LibretroDroid extension draws the upper and lower source regions into separate destination rectangles using one core instance, native view and emulation thread. Inverse destination-to-source mapping preserves the lower-screen stylus coordinates after independent scaling. A positive finite core display aspect ratio is used directly; only an invalid or absent ratio uses a base-size fallback corrected for frontend rotation.

Accessible button activation is observed across actual emulated frames instead of depending on a wall-clock tap duration. A pulse stays pressed for two core runs, then releases for one run before the next queued pulse. Physical holds have independent state. Losing the valid posture, pausing or shutting down cancels pending pulses; no stale press should resume later. Candidate-specific execution of these input protections is recorded in [QA.md](QA.md).

Rounded-panel fitting considers the actual corner arcs and a one-pixel raster guard, allowing letterboxed images to use the available panel area without a blanket corner-radius inset. A geometry change invalidates presentation even if the core returns an unchanged frame. Shader-chain changes wait for a real frame. EGL recreation rebuilds presentation objects without reloading the ROM or SRAM. Runtime evidence is required before claiming those paths work for a particular core or GPU; known filtering, duplicate-frame and context-loss limits are recorded in [libretrodroid/UPSTREAM.md](../libretrodroid/UPSTREAM.md).

## Cutout and safe control contract

The window requests edge-to-edge cutout layout before and after immersive transitions. Android 30 and later use `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`. Android 28 and 29 use `SHORT_EDGES`; the OS can still letterbox a cutout on a long edge, which application geometry cannot override.

Native upper gameplay uses exact display-cutout bounding rectangles when Android supplies them. If the centered native image already clears a camera, its size and position are unchanged. Otherwise, the fit selects the largest complete aspect-preserving rectangle in the remaining rounded panel, preferring the feasible center nearest the panel center. It never draws meaningful game pixels behind a physical camera or cuts pixels away to imitate full-screen coverage. When only safe insets are available, their conservative edge strips are excluded. Delivered window coordinates are translated into the root's local coordinates, including during resize.

Essential upper overlays and all lower controls/touchscreen content remain within safe drawing bounds. The lower image retains its centered fit. The hinge and physical occlusions are separate exclusions. A native modal dialog applies the same window cutout policy and uses its supplied safe center/wing bounds; no independent modal surface is shown over a blocked posture.

## Launcher side controls

The library has three navigation sections: games, filters and shortcuts. Select cycles games, filters and shortcuts. The D-pad moves among cards; Up from the first game row enters filters and Down from the last row enters shortcuts. Left/Right moves within a filter or shortcut row, and the inward direction returns to games. L/R cycles the active library filter. A and Start activate the focused section: launch the selected visible game, apply a filter or invoke a shortcut. B first returns to games from another section, then follows the visible Back action.

Import, Search, Systems, Settings and Help remain reachable from the shortcut row. Empty filtered results offer Show all; scan failures offer Retry; an empty library offers Import; loading or scanning can disable the primary action. A filtered-out or stale remembered selection is not a hidden play target. X opens Search and Y opens the selected game's options, or Settings when there is no game.

On routes such as Settings, the wings move focus within the visible center content and A/Start activate it. The shared virtual-menu helper requests keyboard input mode and restores the saved child when a real touchscreen press changes Android input mode, so repeated virtual-control taps continue from the current item. Native settings dialogs own a separate pair of wings inside the dialog window: D-pad and L/R/Select move modal focus, A/Start activate, B closes, and X/Y move forward/backward. Focus cannot escape into the obscured launcher. System document pickers and OS permission prompts retain their own controls.

## Saved gameplay preview

The upper launcher preview first attempts to show an existing screenshot for the selected game's newest valid manual save. The resolver checks the existing four numbered slots across that system's configured cores and requires both a readable nonempty state and a complete decodable JPEG. Newest means state modification time, then image modification time, with deterministic core/slot tie-breaking. An image older than its matching state is stale and is skipped; if the newest candidate is invalid, an older valid slot may be used. Autosaves currently have no associated screenshot and are not preview candidates.

Reading a preview never launches a game, deserializes a state, starts a core, captures a new frame or writes save data. The saved still uses an aspect-preserving fit and is identified as saved gameplay in its accessibility description. It is not a live video preview. The image retains the existing save-preview resolution; enlarging a small thumbnail can look soft, and this feature does not add a new high-resolution capture path. A missing, empty, corrupt, oversized, truncated or concurrently changing image falls back to the next valid slot, then the selected game's cover art. The resolver bounds input files and decode dimensions, reads them off the main thread, verifies file stamps around decoding and uses the image bytes in its revision identity.

Selection changes clear the prior game's image immediately, including a changed file identity with the same database ID. Resume and Activity recreation re-read persisted files. While the launcher is resumed, file observers watch only the selected title's state/preview paths, coalesce relevant event bursts for 100 ms and rebuild watches when folders change. Collection cancellation releases the watches. The interval is a debounce, not periodic polling or a maximum refresh latency.

## Launcher artwork and spacing

When no valid saved screenshot exists, the selected game's existing cover URL is rendered through the production Coil image loader. The broad upper preview fits the complete artwork behind its gradient and overlays. Square library cards continue to show cover artwork using a centered crop that reaches the card edge, rather than placing the artwork inside a padded miniature card. An original procedural cartridge illustration remains the final placeholder when artwork is absent or has not loaded; it is not a gameplay screenshot.

Cards have a 12 dp corner radius. The outline is 1 dp in the theme's outline-variant color when unselected and 3 dp in its primary color when selected. Selection uses the border and accessibility semantics only, with no checkmark or replacement selection badge. Full game titles remain available to accessibility semantics even though the icon cards have no visible caption; the selected title appears in the upper plaque. Incomplete rows are centered, and longer collections remain lazy and scrollable.

The four upper corner surfaces share the tallest intrinsic height, with a 48 dp minimum, so the brand, tools, game count and play action align at large text sizes. Filters and the shortcut dock occupy equal-height centered bands with matching 20 dp top and bottom spacing. These adjustments preserve the separate lower control wings and the central game shelf.

The title plaque is measured within the space between the lower corner controls and below the upper controls. If the window cannot contain the measured preview or a usable game shelf, the app shows an expand-window message while retaining selection instead of overlapping or clipping those controls.

Rounded native gameplay panels use safe inner rectangular viewports. The backing, corner masks and hinge exclusion stay outside the native pixels, so decorative rounding does not crop either Nintendo DS screen or a single-screen game's image.

## Verification boundaries

Instrumentation can publish synthetic WindowManager folding features on an ordinary emulator. Those tests validate the actual app's layout, state and native gameplay reactions to supplied metadata; they do not certify a physical hinge sensor, cover-to-inner-display transfer, OEM extensions or a particular phone model. Screenshots and reports must identify synthetic posture inputs.

An original local artwork fixture can verify actual Coil loading, card cropping, borders and theme-dependent rendering without including commercial game artwork. Such a check does not prove that a remote cover provider is reachable, that network downloads succeed, or that every imported title has a matching cover.

The source-specific executed results belong in [QA.md](QA.md). A layout specification or successful build is not evidence that its runtime checks passed.

Synthetic cutout metadata and lawful local manual-save fixtures can exercise geometry, preview decoding and refresh behavior. Such tests do not certify an OEM's physical camera bounds, every core's screenshot format, all external-storage conditions or full assistive-technology behavior. Pre-existing 0.2.0 evidence is historical and must not be presented as a 0.2.1 runtime pass.

## Android references

- [FoldingFeature API](https://developer.android.com/reference/androidx/window/layout/FoldingFeature)
- [Detecting device type and interpreting missing folding features](https://android-developers.googleblog.com/2023/06/detecting-if-device-is-foldable-tablet.html)
- [Testing foldable layouts with WindowManager](https://developer.android.com/codelabs/android-window-manager-dual-screen-foldables)
