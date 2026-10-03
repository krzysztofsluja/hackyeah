# Koncepcja — system przekazań międzyszpitalnych (hackathon)

Oct 3, 2026 · @Krzysztof Słuja

## Problem i pitch

**Pitch:** Zamiast godziny obdzwaniania szpitali — potwierdzone miejsce dla pacjenta w kilka minut.

**Problem:** Gdy mały szpital nie ma właściwego specjalisty lub sprzętu, lekarz obdzwania kolejne placówki. Każda odmowa to stracone minuty, a przy udarze czy zawale minuty decydują o wyniku leczenia.

**Rozwiązanie:** Lekarz wypełnia krótkie, zanonimizowane zgłoszenie. System filtruje placówki spełniające twarde wymagania, ustala ranking według czasu dojazdu i obłożenia, a następnie równolegle rozsyła zapytania falami. Pierwsza akceptacja wygrywa.

**Metryka:** czas od decyzji o przekazaniu do potwierdzonego przyjęcia.

**Powiązanie ze smart city:**

- czas dojazdu liczony z uwzględnieniem ruchu w mieście, a nie odległości w linii prostej,
- rozkładanie obciążenia między szpitalami w mieście,
- dashboard koordynatora na sytuacje kryzysowe (wypadek masowy, fala zachorowań).

**Nazwa:** robocza „Hoscom” jest słaba (brzmi jak telekom, a system dopasowuje, a nie tylko komunikuje). Do zmiany, sprawdzić zajętość.

## Użytkownicy i role

Głównym użytkownikiem jest lekarz zlecający, ale sukces zależy od szybkości strony przyjmującej. Obie strony trzeba projektować równie starannie.

| Rola | Kto | Co robi w systemie | Czego potrzebuje |
| --- | --- | --- | --- |
| Lekarz zlecający | lekarz SOR / izby przyjęć w szpitalu bez potrzebnego specjalisty | wypełnia zgłoszenie, widzi ranking i status fal | zgłoszenie w < 30 s, szybka odpowiedź |
| Szpital przyjmujący | dyżurny lekarz lub koordynator oddziału | przyjmuje lub odmawia z powodem, ustawia flagi dostępności | decyzja jednym kliknięciem, mało szumu |
| Koordynator regionalny | osoba z widokiem na cały region / miasto | obserwuje obłożenie i alerty, przejmuje eskalacje | mapa sytuacji, powody odmów |

**Zachęta dla szpitali:** każdy szpital jest jednocześnie nadawcą i odbiorcą. Kto się podłącza, ten sam szybciej przekazuje swoich pacjentów. Dodatkowo: mniej telefonów odrywających dyżurnego i raportowanie obłożenia „przy okazji”.

## Zakres MVP

MVP obsługuje wyłącznie znalezienie miejsca dla pacjenta. Nie dotyka ratownictwa ani transportu.

**Granice:**

- przekazania międzyszpitalne pilne i planowe; nagłe wezwania zostają w państwowym systemie ratownictwa i u dyspozytora medycznego,
- system nie podejmuje decyzji klinicznych, tylko dopasowuje pacjenta do miejsca (unikamy tematu certyfikacji wyrobu medycznego),
- dane zanonimizowane: profil kliniczny bez nazwiska i PESEL-u.

| Zostaje | Wycięte |
| --- | --- |
| formularz 5 pól + filtr + ranking zależny od pilności | logowanie (zamiast niego przełącznik ról w UI) |
| fale zapytań, timeout, „first accept wins” | prawdziwy FHIR (zwykłe eventy JSON) |
| widok szpitala: przyjmuję / odmawiam z powodem | personalizacja formularza |
| odświeżanie na żywo (SSE lub WebSocket) | transport (roadmapa) |
| mapa koordynatora | aplikacja mobilna, push notyfikacje |
| ok. 8 szpitali z danymi + symulator obłożenia | zewnętrzne API ruchu (statyczna macierz czasów) |

## Formularz zgłoszenia

Schemat jest stały, bo algorytm i strona przyjmująca potrzebują porównywalnych danych. Personalizować można najwyżej kolejność i ulubione wartości. Cel: wypełnienie w mniej niż 30 sekund.

| Pole | Typ | Wymaganie | Przykład |
| --- | --- | --- | --- |
| Specjalność docelowa | lista | twarde | neurologia |
| Procedura lub sprzęt | wielokrotny wybór | twarde | TK, trombektomia |
| Pilność | 3 poziomy | wpływa na wagi | czas krytyczny |
| Stan pacjenta | stabilny / niestabilny / wentylowany | twarde przy OIOM | stabilny |
| Izolacja | tak / nie | twarde | nie |
| Opis | jedno zdanie, opcjonalne | informacyjne | 72 l., objawy od 2 h |

Pole „stan pacjenta” przygotowuje grunt pod przyszły dobór rodzaju transportu.

## Algorytm dopasowania

Najpierw filtr twardych wymagań, potem wynik ważony zależnie od pilności. Procent spełnionych warunków jest odrzucony: szpital bez tomografu dla udaru wart jest 0%, nie 80%.

**Krok 1 — filtr.** Placówka bez wymaganej specjalności, procedury, sprzętu lub możliwości izolacji nie trafia do rankingu. Flagi ręczne („TK nieczynne”) też wykluczają.

**Krok 2 — wynik** (niższy = lepszy), każda składowa znormalizowana do 0–1:

```latex
S = w_t \cdot T + w_o \cdot O + w_r \cdot (1 - P)
```

- T: czas dojazdu z macierzy czasów z mnożnikiem godzin szczytu,
- O: obłożenie (zajęte łóżka / wszystkie łóżka),
- P: prawdopodobieństwo odpowiedzi z historii akceptacji i odmów.

**Wagi zależne od pilności** (wartości startowe do kalibracji):

| Pilność | w\_t | w\_o | w\_r | Logika |
| --- | --- | --- | --- | --- |
| Czas krytyczny (udar, zawał) | 0,7 | 0,1 | 0,2 | liczy się każda minuta |
| Pilne, stabilne | 0,4 | 0,4 | 0,2 | równowaga |
| Planowe | 0,2 | 0,6 | 0,2 | rozkładanie ruchu |

Zdanie na pitch: algorytm inaczej waży czas dojazdu i obciążenie w zależności od pilności, dzięki czemu najbliższy szpital nie zostaje zatkany.

## Przepływ zgłoszenia

Zapytania idą falami do najlepszych dopasowań, a pierwsza akceptacja zamyka zgłoszenie dla pozostałych.

&#91;embedded content: przepływ zgłoszenia · fale, timeout, eskalacja\]

- **Fale zamiast jednego strzału:** najpierw top 3, po timeoucie kolejne 3. Mniej szumu dla szpitali, a nadal szybciej niż telefon.
- **Timeout:** brak odpowiedzi w zadanym czasie oznacza automatyczną odmowę. Czas zależy od pilności (krótszy dla czasu krytycznego).
- **First accept wins:** atomowo w bazie (constraint lub optimistic locking). Przegrany dostaje jasny komunikat „już zrealizowane”.
- **Odmowa wymaga powodu:** brak łóżek, brak specjalisty lub sprzęt niedostępny. Powody zasilają ranking i dashboard.
- **Akceptacja to nie koniec:** obie strony od razu dostają bezpośredni kontakt lekarz–lekarz.
- **Fallback lepszy niż start:** gdy placówki się skończą, lekarz dostaje ranking z numerami dyżurnych i listą powodów odmów, a sprawę przejmuje koordynator.

## Dane o dostępności

Wolne łóżko to nie to samo co możliwość przyjęcia, dlatego dostępność ma dwie warstwy.

- **Automatyczna:** obłożenie łóżek z komunikatów przyjęć i wypisów (ADT) systemu szpitalnego, w produkcji przez HL7/FHIR. Uwaga: wypisy bywają rejestrowane z opóźnieniem, więc obłożenie zawsze trochę kłamie.
- **Ręczna:** kilka flag ustawianych jednym kliknięciem przez koordynatora oddziału, np. „neurolog dostępny”, „TK nieczynne do 14:00”, „pracownia hemodynamiki zajęta”. Każda flaga ma czas wygaśnięcia, żeby nie wisiała w nieskończoność.
- **Pośrednia:** powody odmów aktualizują obraz dostępności i zasilają dashboard.

**Na hackathonie:** symulator w Springu (`@Scheduled`) emituje zdarzenia przyjęć i wypisów jako zwykły JSON dla około 8 szpitali. Format FHIR pokazujemy tylko na slajdzie architektury.

## Dashboard koordynatora

Dashboard jest dla koordynatora regionalnego, nie dla obywateli. Publiczne dane o obłożeniu wywołują efekt stada, więc widok publiczny trafia najwyżej na roadmapę.

Minimum na MVP:

- mapa miasta (Leaflet) ze szpitalami w kolorach obciążenia (zielony / żółty / czerwony),
- aktywne zgłoszenia i ich status (fala, czas oczekiwania),
- alert, gdy szpital przekracza próg obłożenia (np. 90%),
- licznik odmów według powodu: pokazuje, gdzie system się dusi i czego brakuje (łóżek, specjalistów, sprzętu),
- lista eskalacji, które czekają na interwencję koordynatora.

To jest główne ogniwo ze smart city: widok obciążenia całej miejskiej infrastruktury szpitalnej, przydatny zwłaszcza w sytuacji kryzysowej.

## Architektura i stack

Jeden serwis Spring Boot, jedna baza i trzy widoki w przeglądarce. Bez mikroserwisów: na 14 h pracy w pojedynkę monolit jest jedynym rozsądnym wyborem.

&#91;embedded content: architektura MVP · 3 widoki, 1 serwis, 1 baza\]

- **Backend:** Spring Boot, Spring Data JPA, `@Scheduled` do timeoutów fal i symulatora.
- **Baza:** PostgreSQL lub H2. Unikalny constraint na akceptację zgłoszenia gwarantuje „first accept wins”.
- **Na żywo:** SSE (prostsze) lub WebSocket do powiadomień i odświeżania statusów.
- **Frontend:** proste widoki (Thymeleaf lub lekki React), mapa w Leaflet.
- **Czasy dojazdu:** statyczna macierz z mnożnikiem godzin szczytu, bez zewnętrznego API.
- **Role:** przełącznik w UI zamiast logowania.

## Szczegóły techniczne

REST jest źródłem prawdy, SSE tylko powiadamia. Każda zmiana stanu idzie przez REST, a po każdym zdarzeniu SSE klient zawsze dociąga aktualny stan. Bez odtwarzania zdarzeń i bez `Last-Event-ID`.

### Ustalenia

| Obszar | Decyzja | Dlaczego |
| --- | --- | --- |
| Architektura | monolit Spring Boot (Spring MVC) | najszybsze dowiezienie solo |
| Frontend | Thymeleaf + czysty `EventSource`, bez bundlera | backendowy stack, mało JS |
| Mapa | Leaflet z CDN na dashboardzie koordynatora | lekka biblioteka, bez frameworka |
| Baza | H2 w trybie plikowym, `MODE=PostgreSQL` | dane przetrwają restart, przejście na Postgresa = zmiana URL-a |
| Odświeżanie | klient zawsze dociąga dane po evencie | jedno źródło prawdy, prosty reconnect |
| SSE | `SseEmitter`, jeden strumień na kartę | WebFlux bez zysku przy tej skali |

### Model danych

- `Hospital`: lokalizacja, łóżka (wszystkie / zajęte), specjalności, procedury
- `HospitalFlag`: typ (np. `TK_DOWN`, `NEURO_AVAILABLE`), `validUntil`
- `Referral` (zgłoszenie): 5 pól, `status` = `OPEN | ACCEPTED | ESCALATED`, `acceptedHospitalId`
- `ReferralRequest` (zapytanie do szpitala): `referralId`, `hospitalId`, `wave`, `status` = `PENDING | ACCEPTED | DECLINED | EXPIRED | CANCELLED`, `declineReason`, `deadline`
- `TravelTime`: macierz `fromId → toId → minuty` + mnożnik godzin szczytu

### REST API

| Metoda | Ścieżka | Kto | Co robi |
| --- | --- | --- | --- |
| POST | `/api/referrals` | lekarz | tworzy zgłoszenie, liczy ranking, wysyła falę 1 |
| GET | `/api/referrals/{id}` | lekarz | stan, fale, odpowiedzi, ranking z powodami |
| GET | `/api/hospitals/{hid}/inbox` | szpital | otwarte zapytania |
| POST | `/api/requests/{rid}/accept` | szpital | akceptacja; `409 Conflict`, gdy ktoś był pierwszy |
| POST | `/api/requests/{rid}/decline` | szpital | odmowa, body `{reason}` |
| PUT | `/api/hospitals/{hid}/flags` | szpital | ustawia flagi dostępności |
| GET | `/api/dashboard` | koordynator | obłożenie, aktywne zgłoszenia, odmowy, eskalacje (JSON dla mapy) |
| GET | `/fragments/...` | widoki | fragmenty Thymeleaf podmieniane przez `innerHTML` po evencie |
| POST | `/api/demo/reset` | demo | przywraca stan startowy scenariusza |

### Akceptacja: first accept wins

Warunkowy UPDATE zamiast `@Version`. Zwraca `1` = wygrana (request → `ACCEPTED`, pozostałe `PENDING` → `CANCELLED`), `0` = `409 Conflict`. Wszystko w jednej transakcji.

```java
@Modifying
@Query("""
  update Referral r set r.status = 'ACCEPTED', r.acceptedHospitalId = :hid
  where r.id = :id and r.status = 'OPEN'
""")
int tryAccept(Long id, Long hid);
```

### SSE

Jeden strumień na kartę: `GET /api/stream?role=doctor&referralId=12`, `role=hospital&hospitalId=3`, `role=coordinator`. W środku `SseHub` z mapą `topic → CopyOnWriteArrayList<SseEmitter>` (topiki `referral:12`, `hospital:3`, `coordinator`).

| Zdarzenie | Do kogo | Payload |
| --- | --- | --- |
| `request.created` | szpital | id zapytania |
| `request.cancelled` | szpital | id zapytania („już zrealizowane”) |
| `referral.updated` | lekarz, koordynator | id zgłoszenia |
| `occupancy.changed` | koordynator | id szpitala |
| `flag.changed` | koordynator | id szpitala |

**Pułapki:**

1. Publikacja po commicie: `@TransactionalEventListener(phase = AFTER_COMMIT)`, inaczej klient dociągnie stan sprzed commita.
2. Timeout emitera: `new SseEmitter(0L)` albo długi czas.
3. Heartbeat co 15–20 s (`event().comment("ping")`) utrzymuje połączenie i wykrywa martwych klientów.
4. Sprzątanie w `onCompletion`, `onTimeout`, `onError`, inaczej emitery gniją po resetach.
5. Limit około 6 połączeń na domenę przy HTTP/1.1: jeden strumień na kartę.
6. Debounce na dashboardzie: jeden fetch na około 500 ms przy serii `occupancy.changed`.

### Timeouty fal

Jeden `@Scheduled(fixedDelay = 2000)`: oznacza `PENDING` z `deadline < now` jako `EXPIRED`, a dla zgłoszeń `OPEN` z zamkniętą falą wysyła kolejną albo ustawia `ESCALATED`. Bez timerów per zapytanie, odporne na restart.

Parametr `demo.time-scale`: w trybie demo fala wygasa po 20–30 s zamiast 10 min.

### Baza i dane startowe

```properties
spring.datasource.url=jdbc:h2:file:./data/hosdb;MODE=PostgreSQL;AUTO_SERVER=TRUE
spring.jpa.hibernate.ddl-auto=update
```

- Seed tylko przy pustej bazie (`CommandLineRunner` + `count() == 0`).
- `/api/demo/reset` czyści zgłoszenia, zapytania i flagi, przywraca obłożenie startowe, zostawia szpitale.
- `ddl-auto=update` nie usuwa starych kolumn: przy dziwnym zachowaniu po zmianie encji usuń katalog `./data`.
- Kafelki OpenStreetMap wymagają internetu: przy awarii Wi-Fi mapa będzie pusta, stąd wideo zapasowe.

## Checkpointy (sob. 12:00 → niedz. 11:00)

23 h: około 15 h kodowania, 6 h snu, reszta na pitch i bufor. Każdy checkpoint ma kryterium „gotowe” i godzinę graniczną. Spóźnienie powyżej 30 minut uruchamia regułę cięcia, a nie pracę dłużej.

**Kamień milowy MVP to CP4.** Jeśli o 21:00 działa pełny scenariusz w dwóch oknach, masz produkt do pokazania. Wszystko po nim to wartość dodana.

- [ ] **CP0 · 12:00–12:45 · Setup**
  - [ ] Projekt ze Spring Initializr: Web, Thymeleaf, Data JPA, H2, Validation, Lombok, DevTools
  - [ ] Konfiguracja H2 plikowej, encje, seed 8 szpitali + macierz czasów dojazdu
  - [ ] Gotowe: aplikacja startuje, dane widać w konsoli H2 i przetrwają restart
- [ ] **CP1 · 12:45–14:30 · Dopasowanie**
  - [ ] Filtr twardych wymagań + flagi, wynik z wagami zależnymi od pilności
  - [ ] Gotowe: test jednostkowy scenariusza udaru — wojewódzki z flagą wypada, drugi ośrodek na 1. miejscu
  - [ ] Cięcie: zamiast prawdopodobieństwa odpowiedzi stała wartość
- [ ] **CP2 · 14:30–16:30 · Przepływ zgłoszenia**
  - [ ] `POST /api/referrals`, fale, `tryAccept`, odmowa z powodem, scheduler timeoutów, eskalacja, `demo.time-scale`
  - [ ] Gotowe: pełny przepływ z pliku `.http`, w tym `409` przy drugiej akceptacji
  - [ ] Cięcie: eskalacja tylko jako status `ESCALATED`, bez dodatkowej logiki
- [ ] **CP3 · 16:30–17:30 · SSE**
  - [ ] `SseHub`, publikacja po commicie, heartbeat, sprzątanie emiterów
  - [ ] Gotowe: `curl -N` na strumieniu pokazuje zdarzenia przy akceptacji i odmowie
- [ ] **Przerwa · 17:30–18:00 · jedzenie, z dala od ekranu**
- [ ] **CP4 · 18:00–21:00 · Widoki lekarza i szpitala (MVP)**
  - [ ] Formularz, ranking z powodami, inbox szpitala, fragmenty Thymeleaf + `EventSource`, przełącznik ról
  - [ ] Gotowe: cały scenariusz udaru w dwóch oknach bez dotykania pliku `.http`
  - [ ] Cięcie: jeśli o 21:30 nie działa, dashboard w CP5 jako zwykła tabela, bez mapy
- [ ] **CP5 · 21:00–23:00 · Dashboard i symulator**
  - [ ] Leaflet z kolorami obłożenia, alerty, licznik odmów, symulator ADT z debounce
  - [ ] Gotowe: kolory na mapie zmieniają się na żywo
- [ ] **CP6 · 23:00–00:00 · Gotowość demo**
  - [ ] `/api/demo/reset`, dane scenariusza (wojewódzki z flagą), poprawki błędów
  - [ ] Gotowe: scenariusz zresetowany i przeprowadzony 3 razy z rzędu bez błędu
  - [ ] Commit i push — kod bezpieczny przed snem
- [ ] **Sen · 00:00–06:00** (nieprzesuwalny)
- [ ] **CP7 · 06:00–08:00 · Szlif i bufor**
  - [ ] Poprawki z nocnej listy, wygląd widoków, ewentualnie wyścig dwóch akceptacji na demo
  - [ ] Zasada: zero nowych funkcji, tylko stabilizacja
- [ ] **CP8 · 08:00–09:30 · Pitch**
  - [ ] Slajdy: problem, rozwiązanie, demo, architektura, roadmapa
- [ ] **CP9 · 09:30–10:30 · Próby**
  - [ ] 3 próby z zegarkiem, nagranie wideo zapasowego
- [ ] **10:30–11:00 · Rezerwa i zgłoszenie projektu**

## Scenariusz demo

Demo pokazuje, że system wybiera nieoczywiście i trafnie: szpital wojewódzki jest zatkany, a system od razu kieruje do drugiego ośrodka. Trzy okna przeglądarki: lekarz, szpital przyjmujący, koordynator.

1. **Kontekst (20 s):** godzina 3:00, mały szpital powiatowy, brak neurologa. Pacjent zgłasza się sam z objawami udaru.
2. **Bez systemu (20 s):** lekarz dzwoni do wojewódzkiego, czeka, słyszy „pracownia trombektomii zajęta”, dzwoni dalej. Około 20 minut straty.
3. **Zgłoszenie:** lekarz bada pacjenta, podejrzewa udar i wypełnia 5 pól (neurologia, TK + trombektomia, czas krytyczny, stabilny, bez izolacji).
4. **Ranking:** szpital wojewódzki jest nisko z widocznym powodem (flaga „pracownia zajęta”). Na górze drugi ośrodek z czasem dojazdu.
5. **Fala 1:** zapytanie trafia do top 3. W oknie szpitala przyjmującego pojawia się powiadomienie.
6. **Akceptacja:** drugi ośrodek klika „przyjmuję”. Pozostałe dostają „już zrealizowane”. Lekarz widzi potwierdzenie i bezpośredni numer do dyżurnego.
7. **Zlecenie transportu:** lekarz zleca transport poza systemem.
8. **Dashboard:** koordynator widzi przeciążony szpital wojewódzki, aktywne zgłoszenie i powód odmowy na mapie.
9. **Puenta:** licznik „około 20 min przez telefony vs 2 min w systemie”.

Opcjonalnie: pokazać wyścig dwóch akceptacji w tej samej sekundzie i to, że wygrywa dokładnie jedna.

**Uwagi merytoryczne:** to lekarz stawia podejrzenie udaru, nie rejestracja. Transport nie jest częścią systemu, więc mówimy „zleca transport”.

## Pytania jury i odpowiedzi

| Pytanie | Odpowiedź |
| --- | --- |
| Czy to zastępuje dyspozytora medycznego? | Nie. Nagłe wezwania zostają w państwowym systemie. My obsługujemy przekazania międzyszpitalne. |
| Czy system podejmuje decyzje kliniczne? | Nie. Decyzję o przekazaniu podejmuje lekarz. System tylko znajduje miejsce. |
| Co z RODO? | Zgłoszenie jest zanonimizowane: profil kliniczny bez nazwiska i PESEL-u. Dane osobowe przekazywane dopiero po akceptacji, kanałem szpitala. |
| Dlaczego szpital ma się podłączyć? | Każdy szpital jest też nadawcą. Podłączony szybciej przekazuje własnych pacjentów i odbiera mniej telefonów. |
| Co gdy wszyscy odmówią? | Kolejne fale, potem eskalacja do koordynatora. Lekarz dostaje ranking z numerami dyżurnych i listą powodów odmów, więc wie, gdzie nie dzwonić. |
| Dlaczego nie telefon? | Telefon jest sekwencyjny, system równoległy. Mierzymy czas od decyzji do potwierdzonego przyjęcia. |
| Skąd dane o dostępności? | ADT z systemu szpitalnego (HL7/FHIR) + ręczne flagi oddziału + powody odmów. Na demo symulator. |
| Co jeśli dwa szpitale przyjmą naraz? | Atomowe „first accept wins” w bazie. Drugi dostaje informację, że zgłoszenie jest już zrealizowane. |
| Kto odpowiada za akceptację, która się nie powiedzie? | Akceptacja otwiera bezpośredni kontakt lekarz–lekarz. Odpowiedzialność kliniczna zostaje po stronie lekarzy, jak dziś. |
| Gdzie tu smart city? | Czas dojazdu z uwzględnieniem ruchu, rozkładanie obciążenia między szpitalami i dashboard kryzysowy dla miasta. |

## Otwarte kwestie i roadmapa

**Do rozstrzygnięcia przed kodowaniem:**

- [ ] Ostateczna nazwa projektu i sprawdzenie, czy nie jest zajęta
- [ ] Wartości K i timeoutu (start: fale po 3, timeout 10 min dla planowych, krócej dla czasu krytycznego)
- [ ] Progi alertu obłożenia na dashboardzie
- [ ] Liczba i rozmieszczenie szpitali w danych demo (konkretne miasto czy fikcyjne)

**Roadmapa (jeden slajd):**

1. Transport: dobór rodzaju karetki na podstawie stanu pacjenta.
2. Integracja HL7/FHIR z systemami szpitalnymi.
3. Dane o ruchu na żywo z miejskich systemów transportowych.
4. Tryb kryzysowy: masowe przekazania przy wypadku masowym.
5. Widok publiczny (np. czasy oczekiwania na SOR) — tylko po rozwiązaniu problemu efektu stada.
