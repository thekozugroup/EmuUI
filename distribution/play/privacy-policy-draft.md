# EmuUI privacy policy draft

Review draft — 20 October 2026. This text is not yet a public policy. Before publication, the publisher must supply its confirmed contact, policy URL, effective date, service-retention details and deletion process, and verify the final Play build and its SDK disclosures. The following describes implemented app behavior; it is not permission to publish.

EmuUI is an open-source Android emulator frontend. Supported gameplay does not require payment or an EmuUI account. The app has no advertising interface. You provide your own lawful game and BIOS files.

When you choose a game folder, EmuUI uses Android's document access system to read it and build a local library. The app stores library details, preferences, gameplay saves, save states and preview images in app-owned storage. Some game data may be cached. Your selected document provider may store files locally or in a cloud account.

When cover artwork is requested, EmuUI can contact the Libretro thumbnail service over HTTPS. The request includes the recognized game title and system in its address. The service also receives ordinary connection information. HTTPS protects the connection but does not hide those details from the service. A local illustration is used when artwork is unavailable. The app has no global artwork-network opt-out at this checkpoint.

The Google Play build obtains emulator cores through Google Play delivery. The separate free dynamic development build can download cores from GitHub; it is not the Play distribution variant. PSP runtime assets are packaged locally in the Play build.

The Play build offers optional Google Drive save synchronization. If you enable it, Google authorization supplies an account session and email for the sync interface. The app can upload and download save files, states and preview images in your Drive app-data storage, together with file names, relative paths and timestamps. Gameplay does not need Drive authorization. Signing out disconnects sync; it does not delete files already stored in Drive. Google's service terms and privacy controls also apply. The non-Play free flavor does not implement Drive synchronization.

Save export copies your saves and previews into a ZIP and writes it to the destination you choose with Android's document picker. That destination may be a cloud provider. The app removes its private temporary export file when the operation finishes, including handled failures. Export does not delete the original saves. The chosen provider controls the exported copy.

Some compatible games can use the microphone after you grant Android's microphone permission. The reviewed frontend and native renderer pass the microphone stream to the game core and do not contain a separate audio-upload feature. You may deny or revoke the permission in Android settings. Core-specific save states may contain game-defined data derived from microphone input; state files follow the save, export and optional sync behavior described above.

Android may back up or transfer eligible app data because EmuUI currently permits platform backup. These system features are separate from optional Drive sync and depend on your Android and account settings. System notifications can display app/game status. Diagnostic logs can include errors and file paths; no first-party remote analytics or crash-upload service was identified in the reviewed app source. Google Play, Google Sign-In and other included SDK behavior still needs final publisher review.

You can manage folder access and permissions through Android settings, choose whether to connect Drive, and decide where to export saves. Clearing app storage or uninstalling can erase local progress and does not necessarily remove copies in a provider, export destination or Android backup. Make a verified backup before deleting local app data. Disconnecting Drive does not delete remote app data; use the provider's data controls and the publisher's final deletion instructions.

The final policy must identify how to contact the publisher about privacy, how any developer-controlled data is retained/deleted, and how changes to this policy are communicated. These details are pending and no privacy contact or response timeframe is invented in this draft.
