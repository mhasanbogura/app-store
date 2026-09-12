# App Store — Session Log

## App Identity
- Package: `com.mahmuduls.appstore`
- Current Version: `1.5.36` (versionCode=36)
- Increment: +1 per build (changed from +2)

## Drive Config
- Primary folder ID: `1PBrhSIvDk0QrgNS6XeTeA1RDLFPeTqKV`
- API key: configurable in Settings (default: `AIzaSyA4ymjFIbuGVhFsKjxVV46RT-qWqNHNiY4`)
- APK naming: `AppName_packageName_vX.Y.Z.apk`
- Icon naming: `AppName_packageName.png` (same folder)
- Shared folders: added in Settings, each can have `update/` subfolder

## All Changes Made

### Admin Removed
- Removed admin login/logout, admin edit/delete dialogs, AdminTab
- Removed credentials sync, admin accounts, all admin-related code
- Settings exposes Drive API key, folder IDs, shared folders to all users

### Version System
- `version.properties`: `versionCode=N`, `versionName=1.5.N`
- `incrementVersion` task: adds +1 to versionCode, sets versionName last segment
- `renameApkForUpload`: copies APK as `App Store_com.mahmuduls.appstore_vX.Y.Z.apk`
- `generateManifestJson`: creates `store_manifest.json`
- Build chain: `assembleDebug` → `renameApkForUpload` → `generateManifestJson` → `incrementVersion`

### Icon System
- Drive icon lookup: queries PNG/JPG in same Drive folder, matches by filename key (`AppName_packageName`)
- `AppStoreApplication` class with Coil disk cache (2% storage in `cacheDir/image_cache`)
- Downloads tab shows `AsyncImage` with iconUrl from store app (falls back to download icon)

### Download System
- Downloads write to `{package}_{versionCode}.tmp`, rename to `.apk` on completion
- Failed/cancelled `.tmp` files deleted on cancellation
- Downloads tab scans both `.apk` and `.tmp` files, plus synthetic entries from `downloadStates`
- `downloadedFiles` recomputes on `downloadStates` changes (dynamic updates)
- `yield()` before each `read()` loop for responsive cancellation
- `activeCalls` (ConcurrentHashMap) stores OkHttp `Call` for instant network abort on cancel
- Cancel resets state to `Idle`, deletes `.tmp` file, removes from active jobs/calls
- Auto-install on download complete (no separate Install button needed)
- Existing cached `.apk` detected and installed directly (no re-download)
- Auto-retry failed downloads after 10 minutes

### Notification System
- Init: creates two channels (`app_store_updates`, `app_store_downloads`)
- Update notification: tap opens Manage Apps tab (tab 1)
- Download complete notification: tap opens Downloaded tab (tab 2)
- Download failed notification: tap opens Store tab (tab 0)
- `REQUEST_DELETE_PACKAGES` permission added for Android 14+
- Auto-update notifications default: `true`

### UI Features
- Store tab: long-press installed → uninstall dialog, not-installed → install dialog
- Full-width `Button` in dialog (same style as store install button), no Cancel button
- App icon in dialog title and text
- Manage Apps tab: updates + installed apps with cancel buttons on in-progress downloads
- Downloads tab: progress spinner + cancel for in-progress, install/delete for completed
- Delete All skips apps in `Progress` or `Completed` state
- Dark mode default: `true` (OLED black `#000000`)
- Pull-to-refresh on Store, Manage Apps, Downloads tabs

### Self-Update Detection
- Store tab `hasUpdate`: checks `versionCode > installedVersion && versionName != installedVerName`
- Version display: `v1.5.22 → v1.5.24` (no versionCode suffix)

## Key Files
- `app/build.gradle.kts` — build config, tasks
- `version.properties` — version state
- `app/src/main/AndroidManifest.xml` — permissions, app class
- `app/src/main/java/com/example/MainActivity.kt` — all composables
- `app/src/main/java/com/example/ui/viewmodel/StoreViewModel.kt` — business logic
- `app/src/main/java/com/example/data/repository/AppRepository.kt` — Drive sync
- `app/src/main/java/com/example/util/NotificationHelper.kt` — notifications
- `app/src/main/java/com/example/AppStoreApplication.kt` — Coil disk cache init
- `app/src/main/java/com/example/ui/theme/Color.kt` — OLED palette
- `app/src/main/java/com/example/data/remote/DriveApiService.kt` — Drive API
