# Plan: foldery na ekranie głównym

Status: **wdrożone** 16.11.2026 i sprawdzone na telefonie. Różnice względem planu:
- Wybór aplikacji to lokalny widok edytora, a nie osobny wpis na stosie Nav3, więc szkic folderu nie musi być współdzielony między ekranami. Szkic żyje w stanie Compose edytora (`FolderDraft`), bez osobnego ViewModelu.
- Edycję folderu otwiera ołówek w nagłówku panelu (wyraźniejsze niż stuknięcie w tytuł).
- Zrobione też: ponowne podpięcie ID aplikacji, gdy aktualizacja zmieni jej główną aktywność (`relinkedAppIds`).
- Jeszcze nie ma: testów ViewModelu (wymagają interfejsów repozytoriów z fake'ami) ani animacji wejścia dla wierszy folderów.

## Cel

Kilka aplikacji można zgrupować w folder na ekranie głównym. Folder ma nazwę i wybraną przez użytkownika minimalistyczną, monochromatyczną ikonę.

## Decyzje właściciela (16.11.2026)

| Pytanie | Decyzja |
|---|---|
| Wygląd na ekranie głównym | wiersz na liście ulubionych: **ikona + nazwa**; aplikacje zostają czystym tekstem |
| Otwieranie folderu | **panel od dołu** (bottom sheet) z listą aplikacji |
| Pauza na oddech | aplikacje z folderu otwierają się **od razu**, tak jak ulubione |
| Usunięcie folderu | jego aplikacje **wracają na ekran główny** jako pojedyncze ulubione, w miejscu folderu |

Ustalenia, które przyjmuję domyślnie (do zmiany na prośbę właściciela):

- Aplikacja może być na ekranie głównym tylko w jednym miejscu: albo jako pojedyncze ulubione, albo w jednym folderze. Dodanie jej do folderu przenosi ją tam.
- Aplikacje w folderze są posortowane alfabetycznie, bez ręcznej kolejności. Mniej obsługi na małym ekranie.
- Foldery nie mogą być zagnieżdżone. Kolejność elementów zmienia się przez „Przesuń wyżej/niżej”, tak jak dziś; przeciągania nie będzie.
- Pusty folder (np. po odinstalowaniu jego jedynej aplikacji) zostaje na ekranie z podpisem „pusty”, żeby można było dodać do niego aplikacje.

## UX (349×621 dp, czcionka ×1.3)

### Ekran główny
```
17:19
Monday, November 16


◎  Social             ← folder: ikona 24dp + nazwa (headlineSmall), cały wiersz klikalny
▣  Work
Phone                 ← aplikacje bez zmian
Messages
          ───
```
- Stuknięcie w folder otwiera jego panel. Przytrzymanie otwiera menu folderu.
- Wysokość wierszy dalej dopasowuje się do miejsca (40–56 dp). Folder liczy się jako jeden wiersz.

### Panel folderu (ModalBottomSheet)
```
╭──────────────────────────╮
│          ───             │
│  ◎  Social               │  ← nagłówek; stuknięcie lub przytrzymanie → edycja folderu
│  Discord                 │
│  Signal                  │  ← stuknięcie: otwiera od razu (bez pauzy)
│  Telegram                │  ← przytrzymanie: menu aplikacji („Usuń z folderu”, „Informacje”)
╰──────────────────────────╯
```
Przy większej liczbie aplikacji niż mieści się na ~60% wysokości ekranu panel się przewija.

### Menu (przytrzymanie)
- **Aplikacja na ekranie głównym**: Przenieś do folderu… / Usuń z ekranu głównego / Przesuń wyżej / Przesuń niżej / Informacje o aplikacji
- **Aplikacja w liście wszystkich**: Dodaj do ekranu głównego / Dodaj do folderu… / Informacje o aplikacji
- **Aplikacja w panelu folderu**: Wyjmij z folderu (trafia na ekran główny) / Usuń z ekranu głównego / Informacje o aplikacji
- **Folder**: Edytuj folder / Przesuń wyżej / Przesuń niżej / Usuń folder
- „Przenieś/Dodaj do folderu…” otwiera drugi panel z listą folderów i opcją „+ Nowy folder”.

### Edytor folderu (pełny ekran)
```
← Nowy folder         Zapisz
┌──────────────────────────┐
│ Nazwa: Social_           │   ← TextField, autofocus tylko przy nowym folderze
└──────────────────────────┘
Ikona
 ◎  ▣  ♫  ✉  ☎             ← siatka 5 kolumn (~60 dp), zaznaczenie obwódką
 ⚑  ✈  ☕  ⚙  ★
 …
Aplikacje (3)              ← otwiera wybór aplikacji: lista z checkboxami + pasek liter
```
- Na ekranie 621 dp klawiatura zajmuje około połowy wysokości. Dlatego edytor przewija się w pionie, a pole nazwy ma `imePadding()`. Przycisk „Zapisz” jest w górnym pasku, więc klawiatura go nie zasłania.
- Bez nazwy nie da się zapisać. Nazwa ma maksymalnie 24 znaki. Jeśli użytkownik nie wybierze ikony, folder dostaje ikonę `folder`.
- Wybór aplikacji to osobny pod-ekran z alfabetyczną listą i tym samym paskiem liter co w drawerze. Aplikacje, które są już w innym folderze, mają dopisek „w: Work”. Zaznaczenie takiej aplikacji przeniesie ją do edytowanego folderu.

## Ikony

**Źródło:** [Material Symbols](https://github.com/google/material-design-icons) (Apache 2.0), styl *Rounded*, waga **300** (cienka kreska), bez wypełnienia.

Dlaczego te, a nie inne:
- Każda ikona to jedna ścieżka SVG o stałym viewBox `0 -960 960 960`. Konwersja do VectorDrawable sprowadza się do przepisania jednej ścieżki i przesunięcia o 960 (`<group android:translateY="960">`). Sprawdziłem, że pliki są dostępne pod `symbols/web/<nazwa>/materialsymbolsrounded/<nazwa>_wght300_24px.svg`.
- Pasują do Material 3, a waga 300 daje minimalistyczny, cienki wygląd.
- Alternatywy: Lucide (ISC) i Tabler (MIT) są rysowane kreską z wieloma elementami (`circle`, `line`, `rect`), więc konwersja byłaby trudniejsza. Phosphor (MIT) ma sens, gdyby Material okazał się zbyt „googlowy”.
- Biblioteka `material-icons-extended` jest przestarzała i ciężka, więc z niej nie korzystamy.

**Monochromatyczność:** ikony są renderowane przez `Icon(tint = onSurface)`, czyli zawsze w jednym kolorze tekstu. To spełnia wymóg „czarno-białe” niezależnie od pliku źródłowego.

**Import:** skrypt `scripts/import-icons.py` z listą nazw pobiera SVG z przypiętego commita repo Google, konwertuje je do `app/src/main/res/drawable/folder_icon_<nazwa>.xml` i kończy się błędem, jeśli którejś ikony brakuje. Wygenerowane pliki trafiają do repo razem z `third_party/material-symbols/LICENSE`.

**Rejestr:** `FolderIcons` mapuje stabilny klucz tekstowy (`"chat"`) na `R.drawable.folder_icon_chat`. Zapisywany jest klucz, a nie ID zasobu, bo ID zmieniają się między buildami. Nieznany klucz pokazuje ikonę `folder`.

**Startowy zestaw (~48 ikon, do korekty przez właściciela):**
folder, star, favorite, bolt, home, work, school, chat, call, mail, forum, group, person, public, photo_camera, image, movie, music_note, headphones, podcasts, sports_esports, menu_book, edit_note, calendar_month, schedule, checklist, payments, account_balance, shopping_bag, shopping_cart, restaurant, local_cafe, directions_car, directions_bus, flight, map, fitness_center, directions_run, spa, pets, eco, wb_sunny, build, settings, terminal, lock, lightbulb, newspaper

## Model danych i zapis

Nowy model w `core/model` (pure Kotlin + `@Serializable`):
```kotlin
@Serializable data class HomeLayout(val items: List<HomeItem> = emptyList())

@Serializable sealed interface HomeItem
@Serializable @SerialName("app") data class HomeApp(val appId: String) : HomeItem
@Serializable @SerialName("folder") data class HomeFolder(
    val id: String,          // UUID, stabilny
    val name: String,
    val iconKey: String,     // klucz z FolderIcons
    val appIds: List<String>,
) : HomeItem
```

- **Zapis:** typowany `DataStore<HomeLayout>` z serializerem kotlinx.serialization (JSON), w pliku `home_layout.json`. Każda zmiana jest atomowym `updateData { ops(it) }`. Uszkodzony plik daje pusty układ (`CorruptionHandler`). Liczniki prób zostają w Preferences DataStore.
- **Migracja:** `DataMigration` czyta stary klucz `favorites` z Preferences i zamienia go na `HomeLayout(items = ids.map(::HomeApp))`, a potem usuwa ten klucz. Migracja ma test jednostkowy.
- **Reguła pauzy:** `HomeLayout.appIdsOnHome()` zwraca zbiór wszystkich aplikacji na ekranie głównym, łącznie z tymi w folderach. Ten zbiór zastępuje dzisiejsze `favoriteIds`.
- **Nowe zależności:** plugin `org.jetbrains.kotlin.plugin.serialization` (w wersji Kotlina), `kotlinx-serialization-json` 1.13.0, `androidx.datastore:datastore` 1.3.0.

## Logika (`core/domain/HomeLayoutOps.kt`, czysty Kotlin)

Każda operacja zwraca nowy `HomeLayout` i ma własne testy:

| Operacja | Najważniejsze przypadki testowe |
|---|---|
| `addApp(appId)` | dodaje na koniec; aplikacja, która już jest (też w folderze), nic nie zmienia |
| `removeApp(appId)` | usuwa ją z ekranu głównego i z każdego folderu |
| `createFolder(id, name, icon, appIds, at)` | wybrane aplikacje znikają z dotychczasowych miejsc; folder trafia na pozycję `at` |
| `updateFolder(id, name, icon, appIds)` | aplikacje dodane z innych folderów są z nich zabierane; aplikacje usunięte z folderu wracają na ekran główny |
| `moveAppToFolder(appId, folderId)` | z ekranu głównego albo z innego folderu |
| `takeAppOutOfFolder(appId)` | wraca na ekran główny tuż pod folderem |
| `deleteFolder(id)` | aplikacje wstawiane są w miejscu folderu, alfabetycznie |
| `move(itemKey, delta)` | jak dzisiejsze `Favorites.move`, w granicach listy |
| `appIdsOnHome()` | obejmuje aplikacje w folderach |

## Zmiany w architekturze

- Nowy pakiet **`feature/folders`**: `FolderSheet`, `FolderEditorScreen`, `IconPicker`, `AppPicker` i `FolderEditorViewModel` (stan szkicu: nazwa, ikona, zaznaczone aplikacje, walidacja).
- **Wspólne komponenty trafiają do `core/designsystem`:** `AlphabetScroller` i logika liter (`Alphabet.kt`), bo drugim konsumentem będzie `AppPicker`. To zgodne z zasadą „do `core` dopiero przy drugim użyciu”.
- **Navigation 3:** dochodzą edytor i wybór aplikacji, czyli prawdziwy stos ekranów z przyciskiem Wstecz. Migracja przenosi obecne `Screen` na klucze `NavKey`. Home i Drawer zostają jedną „powierzchnią” z gestem przesunięcia, a pozostałe ekrany są wpisami na stosie. Przycisk Home czyści stos.
- **`LauncherViewModel`** zostaje przy nawigacji i regule „otwórz albo pauza”. Operacje na układzie przechodzą z `LauncherStore` do nowego `HomeLayoutRepository`. `LauncherStore` zostaje z licznikami prób i dostaje nazwę `AttemptsRepository`.

## Etapy

Każdy etap kończy się przechodzącymi testami (`spotlessApply testDebugUnitTest`), screenshotami i sprawdzeniem na telefonie.

1. **Model i zapis bez zmian w UI.** `HomeLayout`, serializer, migracja z `favorites`, `HomeLayoutOps` z testami, `HomeLayoutRepository`. Ulubione działają jak dotąd, ale na nowym modelu.
   *Gotowe gdy:* po aktualizacji na telefonie dotychczasowe ulubione są na miejscu.
2. **Ikony.** Skrypt importu, ~48 drawable, `FolderIcons`, komponent `IconPicker` i jego screenshot.
3. **Wyświetlanie folderów.** Wiersz folderu na ekranie głównym, `FolderSheet`, otwieranie aplikacji bez pauzy, kropka „na ekranie głównym” w drawerze także dla aplikacji z folderów.
4. **Navigation 3.** Migracja `Screen` na Nav3 bez zmian w wyglądzie, plus testy zachowania przycisków Wstecz i Home.
5. **Zarządzanie folderami.** Edytor (nazwa, ikona, wybór aplikacji), menu folderu, „Dodaj/Przenieś do folderu…”, usuwanie z rozpakowaniem aplikacji.
6. **Dopracowanie.** Animacje panelu, opisy dla czytników ekranu (np. „Folder Social, 3 aplikacje”), komplet screenshotów, test na telefonie przy czcionce ×1.3.

## Ryzyka

- **Klawiatura na ekranie 3":** pole nazwy i siatka ikon mogą się nie zmieścić. Mitygacja: edytor się przewija, „Zapisz” jest w górnym pasku, sprawdzenie na telefonie.
- **Stabilność ID aplikacji:** ID to `komponent#numer_użytkownika`. Gdy aplikacja zmieni główną aktywność po aktualizacji, ID przestaje pasować i aplikacja „znika” z folderu. Mitygacja: przy niedopasowaniu szukamy po samej nazwie pakietu.
- **Nav3 a zachowanie launchera:** Home musi zawsze wracać na czysty ekran główny. Przed etapem 5 będzie to pokryte testem.

## Poza zakresem

Przeciąganie i upuszczanie, zagnieżdżone foldery, ikony aplikacji, pauza dla wybranych folderów, widgety.
