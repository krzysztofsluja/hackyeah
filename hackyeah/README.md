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

## Scenariusz demo

1. **Lekarz:** neurologia, TK + trombektomia, czas krytyczny, stabilny, bez izolacji → wyślij.
2. Szpital wojewódzki jest wykluczony z widocznym powodem (pracownia hemodynamiki zajęta).
3. **Szpital przyjmujący:** zapytanie pojawia się w skrzynce → „Przyjmuję”.
4. Lekarz widzi potwierdzenie i numer dyżurny; pozostałe zapytania są anulowane.
5. **Koordynator:** obłożenie szpitali na mapie, aktywne zgłoszenia, odmowy i eskalacje.

## Bez Dockera

Java 25: `./mvnw spring-boot:run`
