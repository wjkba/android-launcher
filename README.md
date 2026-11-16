# Minimal

Tekstowy launcher Androida pod Unihertz Jelly Star (3", Android 13).

- **Ekran główny**: zegar (stuknięcie otwiera budziki), data (otwiera kalendarz), ulubione aplikacje i foldery pod nimi. Lista zaczyna się mniej więcej w połowie ekranu, więc pierwsze pozycje są pod kciukiem i widać je od razu. Dłuższa lista przewija się w górę, a wygaszona dolna krawędź pokazuje, że niżej jest coś jeszcze. Po wyjściu z launchera lista wraca na początek.
- **Przesunięcie w górę** na końcu listy ulubionych (albo stuknięcie w kreskę na dole) otwiera alfabetyczną listę wszystkich aplikacji. Lista zamyka się, gdy przeciągniesz ją w dół przy samej górze, albo przyciskiem Wstecz lub Home.
- **Przewijanie po literach**: przesuwasz kciukiem wzdłuż paska liter przy prawej krawędzi, a lista skacze do wybranej litery. Na środku ekranu pojawia się wtedy duża litera, a telefon delikatnie wibruje przy każdej zmianie.
- **Ulubione otwierają się od razu.** Każda inna aplikacja otwiera się dopiero po pauzie na jeden oddech (4 s wdechu i 4 s wydechu). Potem widać licznik dzisiejszych prób otwarcia i dwa przyciski: „Nie teraz” albo „Otwórz”.
- **Foldery** na ekranie głównym: wiersz z monochromatyczną ikoną i nazwą. Stuknięcie otwiera panel od dołu z aplikacjami folderu, które otwierają się od razu, bez pauzy. Ołówek w panelu otwiera edytor: nazwa, aplikacje i jedna z 48 ikon.
- **Przytrzymanie aplikacji** otwiera menu: dodaj do ekranu głównego lub do folderu, przenieś do innego folderu, wyjmij z folderu, przesuń wyżej lub niżej, usuń z ekranu głównego, informacje o aplikacji.
- **Przytrzymanie folderu**: edytuj, przesuń, usuń. Po usunięciu folderu jego aplikacje zostają na ekranie głównym w jego miejscu.

## Instalacja na telefonie

1. Na telefonie włącz *Opcje programisty* i *Debugowanie USB*, potem podłącz telefon kablem.
2. Zainstaluj aplikację i uruchom ją pierwszy raz:
   ```sh
   ./gradlew installRelease
   adb shell am start -n dev.minimal.launcher/.app.MainActivity
   ```
3. System zapyta o domyślny ekran główny. Wybierz **Minimal**.

Build `release` jest podpisany lokalnym kluczem debug i zoptymalizowany przez R8, więc Compose działa w nim płynniej niż w `debug`. Do szybkiego sprawdzania zmian możesz używać `./gradlew installDebug`.

**Powrót do starego launchera:** Ustawienia → Aplikacje → Aplikacje domyślne → Aplikacja ekranu głównego. Albo usuń aplikację: `adb uninstall dev.minimal.launcher`.

## Rozwój

```sh
./gradlew spotlessApply testDebugUnitTest   # formatowanie + testy logiki, architektury i screenshotów
./gradlew recordRoborazziDebug              # ponowne nagranie screenshotów po zamierzonej zmianie UI
./gradlew installRelease                    # instalacja na podłączonym telefonie
```

Screenshoty w `app/src/test/screenshots/` są renderowane w rozdzielczości Jelly Star (349×621 dp, czcionka ×1.3), więc zmiany UI da się ocenić bez telefonu.

- Architektura i decyzje: [`docs/architecture.md`](docs/architecture.md)
- Plany funkcji: [`docs/plans/`](docs/plans/)
- Research technologii: [`docs/research-2026-11.md`](docs/research-2026-11.md)
