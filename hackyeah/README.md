# Przekazania międzyszpitalne — demo HackYeah

Lekarz wypełnia krótkie, zanonimizowane zgłoszenie, system filtruje szpitale po twardych wymaganiach,
ustala ranking według czasu dojazdu i obłożenia, rozsyła zapytania falami, a pierwsza akceptacja wygrywa.

## Uruchomienie (wystarczy Docker)

```bash
docker build -t hackyeah .
docker run --rm -p 8090:8090 hackyeah
```

Następnie otwórz <http://localhost:8090> i wybierz rolę: lekarz, szpital przyjmujący lub koordynator.
Najlepiej w dwóch–trzech oknach obok siebie.

- Baza jest w pamięci i startuje ze scenariuszem demo; restart kontenera lub przycisk resetu przywraca stan początkowy.
- Mapa koordynatora pobiera kafelki OpenStreetMap, więc przeglądarka potrzebuje internetu.

## Skąd ten projekt

Moja siostra jest neurologiem. Pewnego dnia miała pacjenta w stanie krytycznym, który pilnie potrzebował tomografii, a tomograf w jej szpitalu był akurat zajęty. Żeby znaleźć inną placówkę, prowadziła jednocześnie dwie rozmowy telefoniczne. Zajęło to około 20 minut, w sytuacji, w której liczyła się każda minuta.

Cała moja rodzina to lekarze i każdy z nich był po obu stronach tego problemu: jako ten, kto dzwoni i szuka miejsca, oraz jako koordynator, który odbiera telefon, odkłada swoją pracę i chodzi po oddziale, żeby sprawdzić, czy ktoś może przyjąć pacjenta.

Z licznych rozmów o tym problemie wyszedł pomysł, żeby taki proces znacznie usprawnić i sprawić, że lekarze będą mieć konkretne miejsce na wymianę informacji i szukanie pomocy.

## Problem

Przekazywanie pacjentów między szpitalami odbywa się dziś głównie telefonicznie i po kolei: jeden telefon, jedna odpowiedź, często odmowa.

- **Lekarz zlecający** nie wie z góry, gdzie jest wolne łóżko, działający sprzęt i dostępny specjalista.
- **Koordynator w szpitalu przyjmującym** przy każdym telefonie musi sprawdzić to ręcznie, często tylko po to, żeby odmówić.

## Jak to działa

1. **Zgłoszenie.** Lekarz wypełnia 5 pól: specjalność, wymagane procedury, pilność, stan pacjenta, izolacja. Zgłoszenie nie zawiera danych osobowych.
2. **Filtr twardych wymagań.** Szpital, który nie może przyjąć pacjenta, odpada z widocznym powodem:
   - brak wymaganej specjalności lub procedury,
   - procedura chwilowo zablokowana flagą (np. zajęty tomograf),
   - brak możliwości izolacji,
   - brak wolnych łóżek,
   - szpital zlecający.
3. **Ranking.** Pozostałe szpitale są szeregowane według czasu dojazdu i obłożenia. Wagi zależą od pilności: przy stanach krytycznych dominuje czas dojazdu, przy planowych rozkładanie obciążenia.
4. **Zapytania.** Zgłoszenie trafia jednocześnie do najlepiej dopasowanych szpitali. Pierwsza akceptacja wygrywa, pozostałe szpitale dostają informację, że sprawa jest załatwiona.
5. **Koordynator.** Mapa miasta pokazuje obciążenie szpitali w czasie rzeczywistym.

## Co zyskuje każda strona

| Kto | Co zyskuje |
| --- | --- |
| Lekarz zlecający | Jedno zgłoszenie zamiast serii telefonów; od razu widzi, gdzie pacjent ma szansę na przyjęcie i dlaczego inne miejsca odpadają |
| Szpital przyjmujący | Oznacza z wyprzedzeniem, co jest chwilowo niedostępne; dostaje tylko zgłoszenia, które może przyjąć, i odpowiada jednym kliknięciem |
| Miasto i region | Mapa obciążenia szpitali i statystyki odmów z powodami jako podstawa do rozkładania ruchu i decyzji inwestycyjnych |

## Czego system nie robi

System nie zastępuje ratownictwa medycznego ani dyspozytora i nie podejmuje decyzji klinicznych. Decyzję o przekazaniu podejmuje lekarz, a system znajduje dla pacjenta miejsce.

## Stan projektu: dane symulowane

Projekt powstał w ramach hackathonu, dlatego w demo:

| Obszar | W demo | Docelowo |
| --- | --- | --- |
| Czas dojazdu | wyliczany ze współrzędnych szpitali | API natężenia ruchu z miejskich systemów |
| Zajętość łóżek | symulator przyjęć i wypisów | systemy szpitalne (HL7/FHIR) w czasie rzeczywistym |
| Szpitale | 8 fikcyjnych placówek w realnych lokalizacjach | rzeczywiste placówki regionu |

Źródło czasu dojazdu jest schowane za interfejsem, więc kolejne integracje podpina się bez zmian w algorytmie dopasowania.

## Dalszy rozwój

1. Integracja z API natężenia ruchu: realny czas dojazdu z uwzględnieniem korków i zamknięć dróg.
2. Integracja z systemami szpitalnymi: zajętość łóżek z przyjęć i wypisów na żywo.
3. Obłożenie na poziomie oddziałów, a nie całego szpitala.
4. Dobór rodzaju transportu do stanu pacjenta.
5. Tryb kryzysowy dla miasta: rozkład wielu pacjentów przy wypadku masowym.

## Technologie

Java, Spring Boot (monolit), Spring Data JPA, H2, [warstwa UI], Leaflet (mapa koordynatora).


## Scenariusz demo

1. **Lekarz:** neurologia, TK + trombektomia, czas krytyczny, stabilny, bez izolacji → wyślij.
2. Szpital wojewódzki jest wykluczony z widocznym powodem (pracownia hemodynamiki zajęta).
3. **Szpital przyjmujący:** zapytanie pojawia się w skrzynce → „Przyjmuję”.
4. Lekarz widzi potwierdzenie i numer dyżurny; pozostałe zapytania są anulowane.
5. **Koordynator:** obłożenie szpitali na mapie, aktywne zgłoszenia, odmowy i eskalacje.

## Bez Dockera

Java 25: `./mvnw spring-boot:run`
