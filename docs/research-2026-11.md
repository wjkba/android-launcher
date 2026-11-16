# Minimalny launcher Androida – research (stan na 16.11.2026)

## 1. Werdykt technologiczny

**Kotlin + Jetpack Compose. To jest dokładnie ten case.**

- Na Google I/O 2026 Google oficjalnie ogłosił podejście "Compose first": systemowe Views są w trybie utrzymaniowym, a nowe wytyczne i biblioteki powstają pod Compose.
- Launcher to zwykła aplikacja Androida. Od innych odróżnia ją tylko `intent-filter` z kategorią `HOME` w manifeście. Nie potrzeba do tego żadnego specjalnego SDK.
- Współczesne open-source'owe minimalne launchery (ARK, Mako, Umain, FokusLauncher, Focus Launcher) są pisane w Compose.
- React Native, Flutter i KMP odpadają. Launcher mocno korzysta z API systemu (`LauncherApps`, role, profile, ikony, tapeta), a każda warstwa pośrednia tylko by tu przeszkadzała.

## 2. Mapa pojęć: React/Elixir → Android

| Znasz | Odpowiednik w Androidzie |
|---|---|
| Komponent funkcyjny React | `@Composable fun` |
| `useState` | `remember { mutableStateOf(...) }` / `rememberSaveable` |
| `useEffect` | `LaunchedEffect`, `DisposableEffect`, `SideEffect` |
| re-render | recomposition |
| lista z wirtualizacją (react-window) | `LazyColumn` / `LazyVerticalGrid` |
| props `style` / className | `Modifier` (łańcuch: `.padding().clickable()...`) |
| store (Redux/Zustand) | `ViewModel` + `StateFlow` |
| RxJS / Elixir `Stream` | Kotlin `Flow` |
| async/await, Elixir `Task` + supervisor | coroutines + `CoroutineScope` (structured concurrency: anulowanie rodzica zabija dzieci, podobnie jak w drzewie supervisora) |
| `package.json` + bundler | Gradle + `gradle/libs.versions.toml` (version catalog) |
| `localStorage` | DataStore (Preferences) |
| SQLite/Ecto | Room 3 |
| Context/Provider jako DI | ręczny DI / Hilt / Metro |
| routing (React Router) | Navigation 3 (backstack to zwykła lista stanów, którą zarządzasz sam) |

## 3. Stack i wersje (listopad 2026)

| Element | Wersja / wybór | Uwagi |
|---|---|---|
| IDE | Android Studio **Rabbit 2** (2026.2.2) | emulator, Layout Inspector, podgląd Compose |
| Kotlin | **2.4.21** (8.10.2026) | 2.5.0 planowane na grudzień 2026 |
| AGP | **9.5.1**, Gradle **9.6.0**, JDK 17 | od AGP 9 Kotlin jest wbudowany, więc **nie** dodajesz pluginu `org.jetbrains.kotlin.android` |
| Compose | **1.12.x** (BOM `2026.09.00`) | 1.12 wymaga `compileSdk 37` i AGP ≥ 9.1.1 |
| Material 3 | **1.4.0** stable, 1.5.0-rc01 | M3 Expressive jest w 1.5 (część API nadal eksperymentalna) |
| Nawigacja | Navigation 3 **1.2.1** | przy minimalnym launcherze możliwe, że wystarczy sam stan |
| Ustawienia | DataStore Preferences | ulubione, motyw, układ |
| Baza danych | Room **3.0.x** (`androidx.room3`) | dopiero gdy będzie potrzebna (np. statystyki użycia) |
| DI | na start ręczny `AppContainer` | później Hilt (domyślny wybór Google) albo Metro (kompilowany DI, Square już na nim jest) |
| SDK | `compileSdk 37`, `targetSdk 36` lub `37`, `minSdk` 26–31 | Play wymaga od 31.08.2026 `targetSdk ≥ 36`. Na własny telefon można ustawić wysokie `minSdk` |
| Testy | Compose UI tests, Robolectric (`ShadowLauncherApps`) | |

Android 17 to API 37 (wydany 16.06.2026). Wersje sprawdzaj w `libs.versions.toml` przy tworzeniu projektu. Mapowanie BOM → wersja ui jest wnioskowane ze strony BOM mapping.

**Android CLI (preview, kwiecień 2026)**: narzędzie Google pod agentów AI typu Claude Code. `android create empty-activity` tworzy projekt z oficjalnego szablonu, a CLI zarządza też emulatorem. Przydaje się, jeśli pracujemy w terminalu. Android Studio zostaje do podglądu UI i debugowania.

## 4. Co czyni aplikację launcherem: konkretne API

### Manifest
```xml
<queries>
    <!-- widoczność wszystkich aplikacji z ikoną, bez QUERY_ALL_PACKAGES -->
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>

<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:excludeFromRecents="true"
    android:theme="@style/Theme.Launcher.Wallpaper">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

### Lista aplikacji: `LauncherApps`, nie `PackageManager`
```kotlin
val la = context.getSystemService(LauncherApps::class.java)
val apps = la.profiles.flatMap { user -> la.getActivityList(null, user) }
    .map { AppEntry(it.label.toString(), it.componentName, it.user) }

// uruchomienie
la.startMainActivity(entry.component, entry.user, null, null)
```
- Obsługuje profil służbowy i Private Space (dostęp do profili tylko przez `getProfiles()`).
- Zmiany (instalacja, usunięcie, aktualizacja) przychodzą przez `LauncherApps.Callback`. Owijasz go w `callbackFlow` i odpinasz w `awaitClose`. Uwaga: aktualizacja aplikacji przychodzi jako `onPackageChanged`, a nie add+remove.

### Ustawienie jako domyślny ekran główny
```kotlin
val rm = getSystemService(RoleManager::class.java)
if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
    requestRole.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
}
```

### Zachowanie ekranu domowego
- **Wstecz** nie może zamykać launchera. Służy do tego `BackHandler` (zamyka szufladę, wyszukiwarkę itp.).
- **Przycisk Home na ekranie domowym** przychodzi jako `onNewIntent`. Konwencja: zamknij szufladę i przewiń do góry.
- **Tapeta**: motyw z `android:windowShowWallpaper=true` i przezroczystym tłem. W Compose nie malujesz tła root.
- **Edge-to-edge**: `enableEdgeToEdge()` i obsługa `WindowInsets` (wymuszone od targetSdk 35).
- **Android 17 (target 37)**: na ekranach sw ≥ 600dp system ignoruje blokadę orientacji i `resizeableActivity`, więc częściej dochodzi do odtwarzania Activity. Stan trzymaj w `ViewModel` i `rememberSaveable`.

### Ikony
- `LauncherActivityInfo.getBadgedIcon(density)` zwraca `Drawable`, który przez `toBitmap().asImageBitmap()` (core-ktx) zamieniasz na bitmapę dla Compose.
- Ikony ładuj poza wątkiem UI (`Dispatchers.IO`) i cache'uj w `LruCache`. To najczęstsze źródło lagów w launcherach.
- Ikony tematyczne: `AdaptiveIconDrawable.getMonochrome()` (API 33+). Może zwrócić `null`.
- Opcja minimalistyczna: launcher tylko tekstowy (styl Olauncher) całkowicie omija problem ikon.

### Opcjonalnie (poza MVP)
- **Widgety**: `AppWidgetHost` + `AppWidgetHostView` w `AndroidView`, plus okno zgody na bind i konfiguracja widgetu. Gotowy composable do RemoteViews ma eksperymentalna biblioteka `glance-experimental-tools/appwidget-host`.
- **Private Space** (Android 15+): wymaga `ACCESS_HIDDEN_PROFILES` + roli HOME.
- **Kropki powiadomień**: wymagają `NotificationListenerService` i zgody użytkownika.

## 5. Twarde ograniczenia (bez obejścia)

- **Ekran ostatnich aplikacji (Recents) i animacje gestów** należą do systemowego launchera (QuickStep). Google nie daje do nich API zewnętrznym launcherom. Skutki: słabsza animacja powrotu do ekranu głównego (na Androidzie 16 bywa z przycięciami), na Pixelach znikają opcje „Zaznacz” i „Zrzut” w Recents, a Xiaomi wyłącza gesty przy launcherze innej firmy.
- Niektóre nakładki producentów potrafią resetować domyślny launcher.
- **Dystrybucja**:
  - Instalacja na własny telefon przez Android Studio lub ADB działa bez ograniczeń.
  - Developer verification: egzekwowana od 30.09.2026 w Brazylii, Indonezji, Singapurze i Tajlandii, globalnie od 2027. Jest konto „student/hobbysta” (do 20 urządzeń, bez opłaty i dowodu).
  - Google Play: `targetSdk ≥ 36`, a zamiast `QUERY_ALL_PACKAGES` wystarczy `<queries>` z `LAUNCHER`.

## 6. Projekty referencyjne

| Projekt | Stack | Licencja | Do czego się przyda |
|---|---|---|---|
| [ARK-launcher](https://sourceforge.net/projects/ark-launcher/) | Compose, M3, Android 11+ | Apache-2.0 (kod można kopiować) | ogólna struktura |
| [Umain Launcher](https://github.com/jacksonfdam/UmainLauncher) | Compose, AGP 9.2, Kotlin 2.4 | — | najnowszy toolchain, minimalny przykład |
| [Mako](https://github.com/rama-io/mako) | Kotlin, minimal | — | UX skupiony na produktywności |
| [Olauncher](https://github.com/tanujnotes/Olauncher) | Kotlin (klasyczny) | GPL-3.0 | wzorzec UX tekstowego launchera |
| FokusLauncher | Compose, Hilt, Room | GPL-3.0 | pełniejsza architektura |
| [Focus Launcher (mslalith)](https://alternativeto.net/software/focus-launcher/about) | Compose | GPL-3.0 | |
| Launcher3 (AOSP) / Lawnchair | Views | Apache / GPL | odniesienie do „jak robi to system” |

Kodu na GPL nie można wklejać do projektu na innej licencji. Do podpatrywania wzorców nadaje się bez ograniczeń.

## 7. Proponowana architektura MVP

```
:app (jeden moduł na start)
├─ data/
│  ├─ AppsRepository      // LauncherApps → Flow<List<AppEntry>>
│  └─ SettingsRepository  // DataStore: ulubione, ustawienia
├─ ui/
│  ├─ home/   HomeScreen  // zegar, data, lista ulubionych
│  ├─ drawer/ AppDrawer   // wyszukiwarka + LazyColumn
│  └─ settings/
├─ LauncherViewModel      // StateFlow<UiState>, collectAsStateWithLifecycle()
└─ MainActivity           // single activity, setContent { ... }
```

Kolejność pracy:
1. Szkielet projektu, filtr HOME, ustawienie jako domyślny launcher.
2. Lista aplikacji i uruchamianie.
3. Wyszukiwarka: filtrowanie lokalne, Enter uruchamia pierwszy wynik.
4. Ulubione na ekranie głównym (DataStore).
5. Gesty: swipe w górę otwiera szufladę, długie przytrzymanie otwiera menu (info, odinstaluj, dodaj do ulubionych).
6. Ustawienia i motyw (dynamic color z tapety).
7. Opcjonalnie: ikony, widgety, Private Space.

## 8. Środowisko (Fedora)

- Android Studio z tarballa albo przez JetBrains Toolbox. Emulator wymaga KVM (użytkownik w grupie `kvm`).
- Launcher najlepiej testować na **fizycznym telefonie** (debugowanie USB lub bezprzewodowe), bo emulator słabo oddaje gesty i zachowanie OEM.

## Źródła

- Compose August '26 release: https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html
- I/O 2026, 17 rzeczy dla devów: https://android-developers.googleblog.com/2026/05/17-things-android-developers-google-io.html
- Android 17: https://android-developers.googleblog.com/2026/06/Android-17.html, https://developer.android.com/about/versions/17/behavior-changes-17
- BOM mapping: https://developer.android.com/develop/ui/compose/bom/bom-mapping
- Android Studio: https://developer.android.com/studio/releases
- AGP: https://developer.android.com/build/releases/gradle-plugin, built-in Kotlin: https://developer.android.com/build/migrate-to-built-in-kotlin
- Kotlin: https://kotlinlang.org/docs/releases.html
- Material3: https://developer.android.com/jetpack/androidx/releases/compose-material3
- Navigation 3: https://developer.android.com/jetpack/androidx/releases/navigation3
- Room 3: https://android-developers.googleblog.com/2026/03/room-30-modernizing-room.html
- Play target SDK: https://developer.android.com/google/play/requirements/target-sdk
- Package visibility: https://developer.android.com/training/package-visibility/declaring, https://support.google.com/googleplay/android-developer/answer/10158779
- LauncherApps.Callback: https://developer.android.com/reference/android/content/pm/LauncherApps.Callback
- Private space: https://source.android.com/docs/security/features/private-space
- Widget host: https://developer.android.com/develop/ui/views/appwidgets/host, https://github.com/google/glance-experimental-tools/blob/main/appwidget-host
- Adaptive/themed icons: https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable
- Gesty w launcherach innych firm: https://docs.lawnchair.app/troubleshooting/gesture-navigation-issues, https://help.niagaralauncher.app/article/114-recent-apps-screen-issues
- Developer verification: https://www.androidauthority.com/android-sideloading-changes-timeline-3679204/, https://developer.android.com/developer-verification/guides
- Android CLI: https://android-developers.googleblog.com/2026/04/build-android-apps-3x-faster-using-any-agent.html
- Metro: https://engineering.block.xyz/blog/metro-migration-at-square-android
- Dystrybucja wersji: https://androidauthority.com/android-distribution-numbers-october-2026-3729798
