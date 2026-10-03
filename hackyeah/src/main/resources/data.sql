-- Seed bazowy: szpitale i macierz czasów dojazdu (model z CLAUDE.md).
-- Uruchamiany przy każdym starcie; każdy INSERT dodaje wiersz tylko, gdy go jeszcze nie ma
-- (szpital rozpoznawany po nazwie). Istniejące dane nie są nadpisywane.
-- Wymaga: spring.sql.init.mode=always, spring.jpa.defer-datasource-initialization=true,
--         spring.sql.init.encoding=UTF-8
-- Nazwy tabel i kolumn = domyślne nazewnictwo Spring Boot dla encji
-- Hospital, TravelTime i kolekcji @ElementCollection specialties / procedures.

-- Szpitale
INSERT INTO hospitals (name, latitude, longitude, total_beds, occupied_beds, isolation_capable, duty_phone)
SELECT 'Szpital Powiatowy „Dolina”', 49.8345, 19.9385, 120, 96, FALSE, '+48 12 000 00 01'
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE name = 'Szpital Powiatowy „Dolina”');

INSERT INTO hospitals (name, latitude, longitude, total_beds, occupied_beds, isolation_capable, duty_phone)
SELECT 'Wojewódzki Szpital Specjalistyczny', 50.024, 19.952, 900, 855, TRUE, '+48 12 000 00 02'
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE name = 'Wojewódzki Szpital Specjalistyczny');

INSERT INTO hospitals (name, latitude, longitude, total_beds, occupied_beds, isolation_capable, duty_phone)
SELECT 'Centrum Neurologii i Kardiologii „Wisła”', 50.082, 19.925, 400, 300, FALSE, '+48 12 000 00 03'
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE name = 'Centrum Neurologii i Kardiologii „Wisła”');

INSERT INTO hospitals (name, latitude, longitude, total_beds, occupied_beds, isolation_capable, duty_phone)
SELECT 'Szpital Miejski „Huta”', 50.073, 20.038, 450, 380, FALSE, '+48 12 000 00 04'
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE name = 'Szpital Miejski „Huta”');

INSERT INTO hospitals (name, latitude, longitude, total_beds, occupied_beds, isolation_capable, duty_phone)
SELECT 'Szpital Specjalistyczny „Zachód”', 50.079, 19.878, 600, 588, FALSE, '+48 12 000 00 05'
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE name = 'Szpital Specjalistyczny „Zachód”');

INSERT INTO hospitals (name, latitude, longitude, total_beds, occupied_beds, isolation_capable, duty_phone)
SELECT 'Szpital Chorób Zakaźnych „Grzegórzki”', 50.059, 19.959, 150, 110, TRUE, '+48 12 000 00 06'
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE name = 'Szpital Chorób Zakaźnych „Grzegórzki”');

INSERT INTO hospitals (name, latitude, longitude, total_beds, occupied_beds, isolation_capable, duty_phone)
SELECT 'Szpital Dziecięcy „Podgórze”', 50.042, 19.964, 250, 190, TRUE, '+48 12 000 00 07'
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE name = 'Szpital Dziecięcy „Podgórze”');

INSERT INTO hospitals (name, latitude, longitude, total_beds, occupied_beds, isolation_capable, duty_phone)
SELECT 'Szpital Powiatowy „Solny”', 49.986, 20.061, 200, 150, FALSE, '+48 12 000 00 08'
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE name = 'Szpital Powiatowy „Solny”');

-- Uzupełnienie izolacji dla szpitali dodanych przed wprowadzeniem kolumny
-- (ustawia wartość tylko tam, gdzie jest pusta, więc ręczne zmiany zostają).
UPDATE hospitals SET isolation_capable = TRUE
WHERE isolation_capable IS NULL AND name IN ('Wojewódzki Szpital Specjalistyczny', 'Szpital Chorób Zakaźnych „Grzegórzki”', 'Szpital Dziecięcy „Podgórze”');
UPDATE hospitals SET isolation_capable = FALSE WHERE isolation_capable IS NULL;

-- Uzupełnienie telefonu dyżurnego dla szpitali dodanych przed wprowadzeniem kolumny.
-- Numery są fikcyjne (demo) - nie wstawiamy prawdziwych numerów szpitali.
UPDATE hospitals SET duty_phone = '+48 12 000 00 01' WHERE duty_phone IS NULL AND name = 'Szpital Powiatowy „Dolina”';
UPDATE hospitals SET duty_phone = '+48 12 000 00 02' WHERE duty_phone IS NULL AND name = 'Wojewódzki Szpital Specjalistyczny';
UPDATE hospitals SET duty_phone = '+48 12 000 00 03' WHERE duty_phone IS NULL AND name = 'Centrum Neurologii i Kardiologii „Wisła”';
UPDATE hospitals SET duty_phone = '+48 12 000 00 04' WHERE duty_phone IS NULL AND name = 'Szpital Miejski „Huta”';
UPDATE hospitals SET duty_phone = '+48 12 000 00 05' WHERE duty_phone IS NULL AND name = 'Szpital Specjalistyczny „Zachód”';
UPDATE hospitals SET duty_phone = '+48 12 000 00 06' WHERE duty_phone IS NULL AND name = 'Szpital Chorób Zakaźnych „Grzegórzki”';
UPDATE hospitals SET duty_phone = '+48 12 000 00 07' WHERE duty_phone IS NULL AND name = 'Szpital Dziecięcy „Podgórze”';
UPDATE hospitals SET duty_phone = '+48 12 000 00 08' WHERE duty_phone IS NULL AND name = 'Szpital Powiatowy „Solny”';

-- specialties
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'INTERNAL_MEDICINE' FROM hospitals h WHERE h.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'INTERNAL_MEDICINE');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'GENERAL_SURGERY' FROM hospitals h WHERE h.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'GENERAL_SURGERY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'ORTHOPEDICS' FROM hospitals h WHERE h.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'ORTHOPEDICS');

INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'NEUROLOGY' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'NEUROLOGY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'CARDIOLOGY' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'CARDIOLOGY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'INTERNAL_MEDICINE' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'INTERNAL_MEDICINE');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'GENERAL_SURGERY' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'GENERAL_SURGERY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'ORTHOPEDICS' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'ORTHOPEDICS');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'INFECTIOUS_DISEASES' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'INFECTIOUS_DISEASES');

INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'NEUROLOGY' FROM hospitals h WHERE h.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'NEUROLOGY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'CARDIOLOGY' FROM hospitals h WHERE h.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'CARDIOLOGY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'INTERNAL_MEDICINE' FROM hospitals h WHERE h.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'INTERNAL_MEDICINE');

INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'NEUROLOGY' FROM hospitals h WHERE h.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'NEUROLOGY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'INTERNAL_MEDICINE' FROM hospitals h WHERE h.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'INTERNAL_MEDICINE');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'GENERAL_SURGERY' FROM hospitals h WHERE h.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'GENERAL_SURGERY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'ORTHOPEDICS' FROM hospitals h WHERE h.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'ORTHOPEDICS');

INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'NEUROLOGY' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'NEUROLOGY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'CARDIOLOGY' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'CARDIOLOGY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'INTERNAL_MEDICINE' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'INTERNAL_MEDICINE');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'GENERAL_SURGERY' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'GENERAL_SURGERY');

INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'INFECTIOUS_DISEASES' FROM hospitals h WHERE h.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'INFECTIOUS_DISEASES');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'INTERNAL_MEDICINE' FROM hospitals h WHERE h.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'INTERNAL_MEDICINE');

INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'PEDIATRICS' FROM hospitals h WHERE h.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'PEDIATRICS');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'GENERAL_SURGERY' FROM hospitals h WHERE h.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'GENERAL_SURGERY');

INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'CARDIOLOGY' FROM hospitals h WHERE h.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'CARDIOLOGY');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'INTERNAL_MEDICINE' FROM hospitals h WHERE h.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'INTERNAL_MEDICINE');
INSERT INTO hospital_specialties (hospital_id, specialties)
SELECT h.id, 'GENERAL_SURGERY' FROM hospitals h WHERE h.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM hospital_specialties x WHERE x.hospital_id = h.id AND x.specialties = 'GENERAL_SURGERY');

-- procedures
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'CT' FROM hospitals h WHERE h.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'CT');

INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'CT' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'CT');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'MRI' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'MRI');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'THROMBOLYSIS' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'THROMBOLYSIS');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'THROMBECTOMY' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'THROMBECTOMY');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'PCI' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'PCI');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'ICU' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'ICU');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'VENTILATION' FROM hospitals h WHERE h.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'VENTILATION');

INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'CT' FROM hospitals h WHERE h.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'CT');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'MRI' FROM hospitals h WHERE h.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'MRI');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'THROMBOLYSIS' FROM hospitals h WHERE h.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'THROMBOLYSIS');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'THROMBECTOMY' FROM hospitals h WHERE h.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'THROMBECTOMY');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'PCI' FROM hospitals h WHERE h.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'PCI');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'ICU' FROM hospitals h WHERE h.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'ICU');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'VENTILATION' FROM hospitals h WHERE h.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'VENTILATION');

INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'CT' FROM hospitals h WHERE h.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'CT');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'MRI' FROM hospitals h WHERE h.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'MRI');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'THROMBOLYSIS' FROM hospitals h WHERE h.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'THROMBOLYSIS');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'ICU' FROM hospitals h WHERE h.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'ICU');

INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'CT' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'CT');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'MRI' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'MRI');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'THROMBOLYSIS' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'THROMBOLYSIS');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'THROMBECTOMY' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'THROMBECTOMY');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'PCI' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'PCI');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'ICU' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'ICU');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'VENTILATION' FROM hospitals h WHERE h.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'VENTILATION');

INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'CT' FROM hospitals h WHERE h.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'CT');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'ICU' FROM hospitals h WHERE h.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'ICU');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'VENTILATION' FROM hospitals h WHERE h.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'VENTILATION');

INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'CT' FROM hospitals h WHERE h.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'CT');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'MRI' FROM hospitals h WHERE h.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'MRI');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'ICU' FROM hospitals h WHERE h.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'ICU');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'VENTILATION' FROM hospitals h WHERE h.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'VENTILATION');

INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'CT' FROM hospitals h WHERE h.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'CT');
INSERT INTO hospital_procedures (hospital_id, procedures)
SELECT h.id, 'PCI' FROM hospitals h WHERE h.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM hospital_procedures x WHERE x.hospital_id = h.id AND x.procedures = 'PCI');

-- Czasy dojazdu w minutach (oba kierunki)
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 25 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Dolina”' AND t.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 38 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Dolina”' AND t.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 40 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Dolina”' AND t.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 40 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Dolina”' AND t.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 33 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Dolina”' AND t.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 30 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Dolina”' AND t.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 25 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Dolina”' AND t.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 25 FROM hospitals f, hospitals t WHERE f.name = 'Wojewódzki Szpital Specjalistyczny' AND t.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 17 FROM hospitals f, hospitals t WHERE f.name = 'Wojewódzki Szpital Specjalistyczny' AND t.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 22 FROM hospitals f, hospitals t WHERE f.name = 'Wojewódzki Szpital Specjalistyczny' AND t.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 20 FROM hospitals f, hospitals t WHERE f.name = 'Wojewódzki Szpital Specjalistyczny' AND t.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 12 FROM hospitals f, hospitals t WHERE f.name = 'Wojewódzki Szpital Specjalistyczny' AND t.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 9 FROM hospitals f, hospitals t WHERE f.name = 'Wojewódzki Szpital Specjalistyczny' AND t.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 18 FROM hospitals f, hospitals t WHERE f.name = 'Wojewódzki Szpital Specjalistyczny' AND t.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 38 FROM hospitals f, hospitals t WHERE f.name = 'Centrum Neurologii i Kardiologii „Wisła”' AND t.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 17 FROM hospitals f, hospitals t WHERE f.name = 'Centrum Neurologii i Kardiologii „Wisła”' AND t.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 20 FROM hospitals f, hospitals t WHERE f.name = 'Centrum Neurologii i Kardiologii „Wisła”' AND t.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 10 FROM hospitals f, hospitals t WHERE f.name = 'Centrum Neurologii i Kardiologii „Wisła”' AND t.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 12 FROM hospitals f, hospitals t WHERE f.name = 'Centrum Neurologii i Kardiologii „Wisła”' AND t.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 15 FROM hospitals f, hospitals t WHERE f.name = 'Centrum Neurologii i Kardiologii „Wisła”' AND t.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 30 FROM hospitals f, hospitals t WHERE f.name = 'Centrum Neurologii i Kardiologii „Wisła”' AND t.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 40 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Miejski „Huta”' AND t.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 22 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Miejski „Huta”' AND t.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 20 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Miejski „Huta”' AND t.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 28 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Miejski „Huta”' AND t.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 12 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Miejski „Huta”' AND t.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 15 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Miejski „Huta”' AND t.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 20 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Miejski „Huta”' AND t.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 40 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Specjalistyczny „Zachód”' AND t.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 20 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Specjalistyczny „Zachód”' AND t.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 10 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Specjalistyczny „Zachód”' AND t.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 28 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Specjalistyczny „Zachód”' AND t.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 17 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Specjalistyczny „Zachód”' AND t.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 20 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Specjalistyczny „Zachód”' AND t.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 33 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Specjalistyczny „Zachód”' AND t.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 33 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Chorób Zakaźnych „Grzegórzki”' AND t.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 12 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Chorób Zakaźnych „Grzegórzki”' AND t.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 12 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Chorób Zakaźnych „Grzegórzki”' AND t.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 12 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Chorób Zakaźnych „Grzegórzki”' AND t.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 17 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Chorób Zakaźnych „Grzegórzki”' AND t.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 7 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Chorób Zakaźnych „Grzegórzki”' AND t.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 22 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Chorób Zakaźnych „Grzegórzki”' AND t.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 30 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Dziecięcy „Podgórze”' AND t.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 9 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Dziecięcy „Podgórze”' AND t.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 15 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Dziecięcy „Podgórze”' AND t.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 15 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Dziecięcy „Podgórze”' AND t.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 20 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Dziecięcy „Podgórze”' AND t.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 7 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Dziecięcy „Podgórze”' AND t.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 18 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Dziecięcy „Podgórze”' AND t.name = 'Szpital Powiatowy „Solny”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 25 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Solny”' AND t.name = 'Szpital Powiatowy „Dolina”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 18 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Solny”' AND t.name = 'Wojewódzki Szpital Specjalistyczny'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 30 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Solny”' AND t.name = 'Centrum Neurologii i Kardiologii „Wisła”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 20 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Solny”' AND t.name = 'Szpital Miejski „Huta”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 33 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Solny”' AND t.name = 'Szpital Specjalistyczny „Zachód”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 22 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Solny”' AND t.name = 'Szpital Chorób Zakaźnych „Grzegórzki”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
INSERT INTO travel_time (from_hospital_id, to_hospital_id, minutes)
SELECT f.id, t.id, 18 FROM hospitals f, hospitals t WHERE f.name = 'Szpital Powiatowy „Solny”' AND t.name = 'Szpital Dziecięcy „Podgórze”'
AND NOT EXISTS (SELECT 1 FROM travel_time x WHERE x.from_hospital_id = f.id AND x.to_hospital_id = t.id);
