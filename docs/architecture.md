# Architektura

Stan na listopad 2026. Dokument opisuje, jak zorganizowane jest repo i dlaczego tak, a nie inaczej.

## Czy jest „bulletproof-react” dla Androida?

Nie ma jednego projektu, który pełniłby tę samą rolę. Społeczność składa ten obraz z trzech źródeł:

| Źródło | Co daje | Odpowiednik w świecie React |
|---|---|---|
| [Guide to app architecture](https://developer.android.com/topic/architecture) + [Recommendations](https://developer.android.com/topic/architecture/recommendations) (Google) | warstwy UI, domain (opcjonalna) i data; jednokierunkowy przepływ danych (UDF): stan płynie w dół, zdarzenia w górę | zasady z dokumentacji Reacta / Redux style guide |
| [Now in Android](https://github.com/android/nowinandroid) (Google) | referencyjna aplikacja: moduły `feature/*` i `core/*`, `build-logic` z convention plugins, Navigation 3, testy | **najbliższy odpowiednik bulletproof-react** |
| Wzorce społeczności | struktura feature-first, „shared core”, reguły importów pilnowane automatycznie | `src/features/*`, `src/components`, reguła `import/no-restricted-paths` |

Wnioski, które przyjęliśmy:

1. **Feature-first.** Kod jest pogrupowany według funkcji (home, drawer, pause…), a nie według typu (wszystkie ViewModele w jednym katalogu). To ten sam zabieg co `src/features` w bulletproof-react.
2. **Importy tylko w jedną stronę:** `app → feature → core`. Feature'y nie importują się nawzajem. Zasada jest sprawdzana automatycznie w testach (Konsist), tak jak bulletproof-react robi to regułami ESLint.
3. **Kod trafia do `core` dopiero wtedy, gdy potrzebuje go drugi feature.** Wcześniej zostaje u właściciela.

## Decyzje

### 1. Jeden moduł Gradle, pakiety ułożone jak przyszłe moduły
Now in Android dzieli kod na kilkadziesiąt modułów Gradle. Przy jednej aplikacji, jednym autorze i kilkudziesięciu plikach to głównie koszt: convention plugins, podział na api/impl, wolniejsza konfiguracja builda. Dlatego trzymamy jeden moduł `:app`, a granice wyznaczają pakiety pilnowane przez [Konsist](https://docs.konsist.lemonappdev.com/) (`ArchitectureTest`).

Pakiety odpowiadają przyszłym modułom (`core.data` → `:core:data`, `feature.drawer` → `:feature:drawer`), więc gdy przyjdzie czas, podział będzie mechaniczny. **Kiedy dzielić na moduły:** gdy przyrostowy build zacznie trwać ponad ~30 s, gdy feature'ów będzie więcej niż ~6 albo gdy pojawi się druga aplikacja lub widget, który współdzieli `core`.

### 2. Warstwy
- **UI** (`feature/*`, `app/`): ekrany Compose są bezstanowe. Dostają stan i lambdy (`HomeScreen(favorites, onAppClick, …)`). Stan trzyma ViewModel i wystawia go jako `StateFlow`, a UI zbiera go przez `collectAsStateWithLifecycle()`. Bezstanowe ekrany da się renderować w testach screenshotów bez ViewModelu.
- **Domain** (`core/domain`): czysta logika w Kotlinie, bez Androida (pilnuje tego test). Powstaje tylko dla logiki, którą warto testować osobno albo która jest współdzielona: liczenie prób, operacje na ulubionych. Google traktuje tę warstwę jako opcjonalną i tak też ją stosujemy.
- **Data** (`core/data`): repozytoria opakowujące API systemu (`LauncherApps`) i trwałe dane (DataStore). Na zewnątrz wystawiają `Flow` i funkcje `suspend`.

### 3. Wstrzykiwanie zależności: ręcznie
`AppContainer` w `LauncherApplication` tworzy repozytoria, a fabryka ViewModelu je pobiera. Kiedy graf zależności urośnie (kilka ViewModeli z różnymi zależnościami), przejdziemy na Metro (DI sprawdzany przy kompilacji, pisany od początku pod Kotlina; używa go Square) albo Hilt (domyślny wybór Google).

### 4. Nawigacja
**Navigation 3** (1.2.1, używa go Now in Android). Stos ekranów to zwykła lista kluczy `Screen` (`Home`, `Drawer`, `Pause`, `FolderEditor`) trzymana w `LauncherViewModel`, a `NavDisplay` w `LauncherApp` rysuje jej ostatni element. Zasady launchera:
- `Home` zawsze leży na dnie stosu. Przycisk Home, wyjście z launchera (`onStop`) i „Nie teraz” przycinają stos do samego `Home` i zamykają panele.
- Wstecz zdejmuje jeden ekran. Na samym ekranie głównym przechwytuje go `BackHandler`, żeby launcher nigdy się nie zamknął.
- Panele od dołu (folder, menu aplikacji, menu folderu) nie są wpisami na stosie, tylko osobnym stanem `Sheet` w ViewModelu, bo w danej chwili może być otwarty najwyżej jeden.
- Wybór aplikacji w edytorze folderu jest lokalnym widokiem edytora z własnym `BackHandler`. Dzięki temu szkic folderu żyje w jednym miejscu (stan Compose edytora) i nie trzeba współdzielić go między wpisami stosu.

### 5. Dane trwałe
- Proste wartości: **Preferences DataStore** (liczniki prób, `AttemptsRepository`).
- Układ ekranu głównego (aplikacje i foldery): **typowany DataStore + kotlinx.serialization**, plik `home_layout.json` (`HomeLayoutRepository`). Każda zmiana to jedno atomowe `updateData {}` z czystą funkcją z `core/domain/HomeLayoutOps.kt`. Nazwy pól i `@SerialName` są formatem pliku: zmiana wymaga migracji. Dawne ulubione z Preferences przenosi jednorazowo `FavoritesMigration`.
- **Room 3** dopiero wtedy, gdy będą potrzebne zapytania relacyjne, np. statystyki prób w czasie.

### 6. Testy
| Poziom | Narzędzie | Co sprawdza |
|---|---|---|
| Logika | JUnit | `core/domain` (operacje na układzie, litery, próby), szkic folderu |
| Dane | JUnit + prawdziwy DataStore w katalogu tymczasowym | format JSON, migracja ulubionych |
| Architektura | Konsist | reguły importów między pakietami |
| UI | Roborazzi + Robolectric | screenshoty w rozdzielczości Jelly Star (349×621 dp, 220 dpi, czcionka ×1.3) |
| Urządzenie | adb | ręczna weryfikacja gestów (np. zachowanie przy krawędzi ekranu) |

Do dodania później: testy ViewModeli na fake'owych repozytoriach (z biblioteką Turbine) i Baseline Profile, czyli prekompilacja ścieżki startu, ważna przy launcherze.

### 7. Styl kodu
Spotless + ktlint (styl `android_studio`, linia do 120 znaków) + [compose-rules](https://mrmans0n.github.io/compose-rules/). Te ostatnie pilnują m.in. parametru `modifier` w komponentach i zakazu nowych `CompositionLocal`. Detekt odpada na razie: wersja 1.23 nie wspiera Kotlina 2.4, a 2.0 jest jeszcze w alfie.

## Mapa repo

```
app/src/main/java/dev/minimal/launcher/
├─ app/                 punkt wejścia: Activity, Application + AppContainer, root composable, nawigacja
├─ core/
│  ├─ model/            typy danych współdzielone przez wszystkich (AppEntry)
│  ├─ data/             repozytoria: LauncherApps, DataStore
│  ├─ domain/           czysta logika Kotlin, bez android.* i androidx.*
│  └─ designsystem/     motyw, ikony folderów, lista z paskiem liter, wiersze paneli
└─ feature/
   ├─ home/             zegar, aplikacje i foldery, gest otwarcia listy
   ├─ drawer/           lista wszystkich aplikacji
   ├─ pause/            pauza na oddech przed otwarciem aplikacji
   ├─ folders/          panel folderu, menu folderu, edytor z wyborem ikony i aplikacji
   └─ appactions/       menu po przytrzymaniu aplikacji
scripts/import_icons.py  import ikon Material Symbols do res/drawable (+ FolderIconSet.kt)
third_party/             licencje zewnętrznych zasobów
```

| Pakiet | Może importować |
|---|---|
| `app` | wszystko |
| `feature.X` | `core.*`, siebie |
| `core.*` | `core.*` |
| `core.domain` | tylko Kotlin/Java, bez `android*` |

## Jak dodać feature

1. Nowy pakiet `feature/<nazwa>/` z bezstanowym ekranem `<Nazwa>Screen(state, onEvent…, modifier)`.
2. Jeśli feature ma własny stan, dodaj `<Nazwa>ViewModel` w tym samym pakiecie i fabrykę w `companion object`.
3. Logikę bez UI wydziel do czystych funkcji i obejmij testami. Jeśli używa jej tylko ten feature, zostaje w jego pakiecie. Do `core/domain` trafia dopiero, gdy potrzebuje jej drugi feature.
4. Ekran podłącz w `app/LauncherApp.kt`.
5. Napisy dodaj do `values/strings.xml` (angielski) i `values-pl/strings.xml`.
6. Dodaj screenshot do `ScreenshotTest` i nagraj go (`./gradlew recordRoborazziDebug`).
7. Przed końcem: `./gradlew spotlessApply testDebugUnitTest`.
