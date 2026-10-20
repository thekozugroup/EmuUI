# Privacy and Data safety inventory

Source and final release-manifest review, 20 October 2026. Scope: the free/ad-free EmuUI candidate compiled from `adb8678e6d6f773fd06937b007eea7286a32fd47`, version `0.3.1-dev.20261020`, code 8. The exact `playDynamicRelease` AAB is 52,114,806 bytes, SHA-256 `eaba8193363d6023fb159a8d483806c8aff972e4bb5a8b405a0a25061aa4c1d3`, package `com.kozudigital.emuui`, minimum API 23 and target API 36. It is unsigned and non-debuggable; it is not a Play-delivered installation.

The decoded base manifest and packaging audit are reconciled in `release-manifest-inventory.json`. Bundle validation, the sixteen locked cores, 236 notices and 137 packaged PSP payloads passed their recorded checks. These establish packaged content and configuration, not network behavior, SDK collection, OAuth, gameplay or current-device 16 KB runtime. No network capture, completed Play form or legal approval is claimed. The free release decision in `docs/FREE_RELEASE_CHECKPOINT.md` supersedes historical billing proposals. The separate `freeBundleDebug` screenshot candidate omits Play-only Drive and the packaged Play PSP ZIP; do not attribute its UI captures to this release AAB.

Gameplay has no payment or EmuUI-account gate. Optional Google Drive sync is a separate Play-flavor feature; do not market the whole app as account-free or offline-only. No ads/Billing SDK or first-party analytics/crash-upload service was identified in the reviewed active app source. This finding does not certify the behavior of every Google SDK or emulator core.

## Implemented data flows

Paths below are relative to the repository root. A local-only row describes the reviewed app flow; Android backup and explicit export/sync are separate rows.

| Data or action | Storage / destination | Trigger and controls | Evidence and practical implication |
| --- | --- | --- | --- |
| Imported game files and BIOS supplied by the user | Selected Storage Access Framework provider; app may cache game data in app-owned storage | User chooses a folder and grants access | `retrograde-app-shared/.../storage/local/StorageAccessFrameworkProvider.kt`, `GameLoader.kt`. The source folder may itself be backed by a cloud document provider; do not promise provider-local storage. No game-file uploader is identified in the import flow. |
| Library metadata, titles, system, favorites and recent play | App Room database; preferences | Import and library use | `retrograde-app-shared/.../library/db/entity/Game.kt`, `GameDao.kt`. Names/URIs can reveal the contents of a user's library. |
| Cover artwork | HTTPS `thumbnails.libretro.com` request whose URL path contains normalized game title and system; provider also sees connection metadata | Cover loading when artwork is requested; no global artwork opt-out established | `lemuroid-metadata-libretro-db/.../LibretroDBMetadataProvider.kt`, `lemuroid-app/.../shared/covers/CoverUtils.kt`, `SecureThumbnailInterceptor.kt`. Old HTTP thumbnail URLs are upgraded before connection; main network security config denies cleartext. Provider retention/secondary use not established here. HTTPS does not conceal the requested title from the provider. |
| Artwork fallback | Generated local drawable; legacy TV fallback is a local `android.resource` URI | Artwork unavailable | `CoverUtils.kt`, `app/tv/channel/ChannelHandler.kt`. The prior `fakeimg.pl` title-bearing URL is removed in this task; do not carry it into the current active-endpoint list. TV has no exported launcher in this product. |
| Emulator executable delivery | Play: Google Play feature modules. Free dynamic preview: HTTPS GitHub release downloads | A selected core is needed | Flavor-specific `ext/feature/core/CoreUpdaterImpl.kt`. `freeDynamic` is not the Play submission variant. Play delivery needs final signed-track testing. |
| PSP runtime assets | Play: packaged traced local assets. Free flavor retains its upstream asset retrieval path | PSP preparation | `retrograde-app-shared/.../core/assetsmanager/PPSSPPAssetsManager.kt`, `qa/psp-assets-provenance.json`. Do not claim the Play app downloads PSP assets from GitHub. |
| SRAM, save states, preview stills and core-specific state metadata | App-owned external/internal files; temporary in-memory recreation checkpoints | Gameplay, save/load and lifecycle checkpoint | `DirectoriesManager.kt`, `SavesManager.kt`, `StatesManager.kt`, `StatesPreviewManager.kt`, `GameRecreationCheckpoint.kt`. States can include game-defined names/text and gameplay imagery. Manual preview reads do not launch a core or create a fresh screenshot. |
| Google identity (Play flavor only) | Google Sign-In SDK/account session; email displayed in sync settings | User explicitly activates optional Drive sync; settings action signs out | `lemuroid-app-ext-play/.../savesync/ActivateGoogleDriveActivity.kt` requests ID, email and `DRIVE_APPDATA`. `SaveSyncManagerImpl.kt` reads the email. Google SDK-specific data disclosures and new-package/signing OAuth validation remain pending. No EmuUI backend is present. |
| Save synchronization (Play flavor only) | User-authorized Google Drive `appDataFolder`: `saves`, `states`, `state-previews`; relative paths, file names, timestamps, sizes and MD5s | Enabled Google session; configured sync work; selected cores control state inclusion | `SaveSyncManagerImpl.kt`, `DriveFactory.kt`, `app/shared/savesync/SaveSyncWork.kt`. Sync uploads and downloads actual files, including preview imagery. Free flavor implementation is a no-op. Disconnect signs out; it does not delete Drive app data. Retention follows user/provider controls and needs publisher review. |
| Save export | ZIP snapshot staged in private cache, then written to a user-chosen document URI (local or cloud provider) | Explicit Save export action; export refused during active gameplay | `app/mobile/feature/billing/BillingActivity.kt` (historical class name), `app/shared/billing/SaveArchive.kt`. Original saves are copied, not removed; private temporary ZIP is deleted in `finally`. The chosen provider receives the exported content and controls its retention. End-to-end cloud-provider deletion is not verified. |
| Optional microphone | Native in-memory Oboe stream to the enabled core | Android runtime RECORD_AUDIO grant and core support; user can deny/revoke in OS settings | `HomeScreen.kt`, `GameViewModelRetroGameView.kt`, `libretrodroid/src/main/cpp/microphone/{microphone,microphoneinterface}.cpp`. No audio-file uploader/recorder is identified in the frontend/renderer. Core save-state serialization could retain game-defined audio-derived state; verify each supported core before making a stronger local-only claim alongside sync. |
| Touch, stylus, external pad and haptics | App/native input state; vibration API | User play/control actions; haptic settings | No input-upload path identified. Native game pixels/stylus are not a semantic accessibility tree. |
| Notifications and legacy TV recommendations | Android system notification / TV content provider | Notifications for active work/game; inherited TV code if reached | Source manifests, `NotificationsManager.kt`, `ChannelHandler.kt`. Titles/status may be visible to the OS; TV recommendation reachability is not a certified release feature. |
| Platform backup / device transfer | Android backup transport under OS/account settings | Final release manifest confirms `allowBackup=true`; neither `fullBackupContent` nor `dataExtractionRules` is specified | Separate from Drive. Do not claim all app data remains on-device, or that uninstall erases all possible backups. Actual eligibility, exclusions, backup/restore and Android version behavior remain untested. |
| Diagnostics | Android logcat via Timber / native logs | Errors and debug output | `retrograde-app-shared/.../logging/TimberLoggingHandler.kt` and call sites include file/path and sync diagnostics. No app-owned remote crash/analytics endpoint identified. Do not publish raw user logs without review. Google services/OS diagnostics require their own disclosure review. |
| Browser links | User's browser opens GitHub source/license pages | Explicit Help/About link | Browser controls the destination request; no embedded first-party tracking page identified. |

## Form preparation: decisions still required

Google's [Data safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en), checked 20 October 2026, treats off-device transmission by the app or its SDKs as collection, while purely local processing is outside that definition. Sharing exceptions depend on the actual user-directed transfer or provider relationship. Do not infer an exception or ephemeral retention from a URL or a successful request.

| Candidate category | Why it needs review | Proposed scope, not a filed answer |
| --- | --- | --- |
| App activity / app interactions | Title-bearing cover requests disclose library interest to the artwork service | Functionality. Requests are automatic when matching covers are loaded; no universal opt-out established. Confirm provider retention/sharing and map the exact current form category. |
| Personal info / email address and user IDs | Optional Google Sign-In ID/email access | Optional sync/account management/functionality. Review exact Google SDK data disclosures and whether any provider relationship changes sharing treatment. |
| Files and docs / other user-generated content | Drive sync and user-directed save ZIP export include game-defined progress, names, metadata | Optional functionality. App accesses the file data; do not assume the narrow external-drive upload exemption automatically applies. |
| Photos / app activity | Save-state preview stills can be synchronized or exported | Generated game imagery needs a concrete category decision, not omission because it is not a camera photo. |
| Audio | Microphone processed locally by native core | Do not declare remote audio collection solely from RECORD_AUDIO; also verify state/sync behavior and dependencies before claiming none. |
| Diagnostics / device or other identifiers | Play delivery, review and Sign-In SDKs; service connection metadata | SDK-level disclosures/runtime inspection pending. Source did not identify an app analytics ID or advertising SDK. IP receipt alone does not prove the app uses location. |
| Security and deletion | Active app HTTP denied; provider/SDK paths not exhaustively observed | Do not assert that all collected data is encrypted, ephemeral, deleted on request or never shared until final endpoint/SDK review. Disconnect is not deletion. |

No purchase-history category is introduced by this free release; obsolete billing code/docs are not evidence of current collection. The exact release manifest contains neither BILLING nor AD_ID. No evidence was found for an app feature collecting location, contacts, calendar, payments or messages. The manifest has no location, contacts, calendar, camera, SMS or call-log permission. This permission finding does not establish the absence of all related network or SDK behavior. Final SDK disclosures and runtime inspection remain necessary before answering absence questions in Console.

## Permissions in the exact final release manifest

The decoded base manifest contains exactly thirteen `uses-permission` entries. `release-manifest-inventory.json` preserves their full names and the decoded-manifest SHA-256 (`64770c3e39ee397cf67cd4274268ed1695e371fe2fa359a88a3d8602e4d625f9`).

| Permission | Reviewed purpose / limit |
| --- | --- |
| `android.permission.INTERNET` | Cover requests, optional Google flows and Play-related network operations; presence does not identify all transferred data |
| `android.permission.ACCESS_NETWORK_STATE` | Connectivity-aware work |
| `android.permission.READ_EXTERNAL_STORAGE` | Inherited legacy storage path; current SAF grants are separate; behavior varies by Android version |
| `android.permission.RECORD_AUDIO` | Optional native microphone input; runtime grant is still required |
| `android.permission.VIBRATE` | Haptics |
| `android.permission.POST_NOTIFICATIONS` | Work/game notifications, subject to runtime permission on applicable Android versions |
| `android.permission.FOREGROUND_SERVICE` | Foreground service capability |
| `android.permission.FOREGROUND_SERVICE_DATA_SYNC` | Declared data-sync foreground service type; declarations and real service behavior need final policy/runtime review |
| `android.permission.WAKE_LOCK` | Background-work/device-awake support; no continuous-wakelock claim is made |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Scheduled work restoration |
| `com.android.providers.tv.permission.READ_EPG_DATA` | Retained TV provider code; not evidence of a supported TV release |
| `com.android.providers.tv.permission.WRITE_EPG_DATA` | Retained TV recommendation code; no TV launcher is enabled |
| `com.kozudigital.emuui.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | App-defined receiver permission, declared with protection value `0x00000002` |

`GameService` and WorkManager's `SystemForegroundService` declare foreground-service type `0x00000001` (data sync). The merged manifest includes the optional Google Drive activation Activity and Google Sign-In revocation service. The historical `BillingActivity` class is the current Save export screen; its class name is not evidence of purchases. Runtime permission prompts, microphone behavior, service duration/notification behavior and any required Play foreground-service declarations still need acceptance testing. A manifest audit alone does not settle Data safety responses.

The Google API client embeds `google.jks` and `google.p12` as public CA truststores. The artifact audit matched them to the dependency resources and found seventy trusted certificate entries and zero private/secret key entries in each. They are not application/upload signing credentials. This classification does not certify all TLS/SDK network behavior; no truststore binaries or private signing material are added to this pack.

## Open publication work

Confirm the responsible publisher's contact and public privacy-policy URL; review artwork-service and exact SDK retention/processing terms; decide how users request deletion of any developer-controlled data; document Google Drive hidden-app-data removal separately from sign-out; test OAuth and user-directed export; observe the final signed Play installation. No publisher credentials, Google accounts, services, consent or agreements were created or changed by this audit. Do not submit this inventory as an approved privacy policy or automatically import it into Play Console.
