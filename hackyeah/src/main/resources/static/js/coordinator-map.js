// Coordinator map: created once, then refreshed from JSON every few seconds. Markers are kept by
// hospital id and updated in place, so zoom, pan and an open popup survive each refresh.
// Later, an SSE occupancy.changed / flag.changed event can call refresh() (debounced) instead of the interval.
(function () {
    const KRAKOW = [50.06, 19.95];
    const REFRESH_MS = 3000;

    document.addEventListener('DOMContentLoaded', () => {
        const container = document.getElementById('map');
        if (!container) {
            return;
        }
        if (typeof L === 'undefined') {
            container.classList.add('map-unavailable');
            container.textContent = 'Mapa niedostępna (brak połączenia z CDN). Aktualne dane w tabeli poniżej.';
            return;
        }

        const map = L.map(container, { scrollWheelZoom: false }).setView(KRAKOW, 12);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 18,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(map);

        const markers = new Map();
        let fitted = false;

        async function refresh() {
            let hospitals;
            try {
                const response = await fetch(container.dataset.src, { headers: { 'Accept': 'application/json' } });
                if (!response.ok) {
                    return;
                }
                hospitals = await response.json();
            } catch (e) {
                return; // keep the last state, next tick retries
            }

            hospitals.forEach(h => {
                const style = markerStyle(h);
                let marker = markers.get(h.id);
                if (!marker) {
                    marker = L.circleMarker([h.latitude, h.longitude], style)
                        .bindPopup(popupContent(h, container.dataset.hospitalUrl))
                        .bindTooltip(h.name, { direction: 'top', offset: [0, -6] })
                        .addTo(map);
                    markers.set(h.id, marker);
                } else {
                    marker.setStyle(style);
                    marker.setRadius(style.radius);
                    marker.setPopupContent(popupContent(h, container.dataset.hospitalUrl));
                }
            });

            if (!fitted && hospitals.length > 0) {
                map.fitBounds(L.latLngBounds(hospitals.map(h => [h.latitude, h.longitude])), { padding: [30, 30] });
                fitted = true;
            }
        }

        refresh();
        setInterval(refresh, REFRESH_MS);
    });

    // Colours come from the CSS variables, thresholds from the server (h.level).
    function cssVar(name) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    function markerStyle(h) {
        const fill = h.origin ? cssVar('--muted')
            : h.level === 'high' ? cssVar('--danger')
            : h.level === 'medium' ? cssVar('--warn')
            : cssVar('--ok');
        return {
            radius: 7 + Math.round(h.totalBeds / 15),
            color: h.level === 'high' && !h.origin ? cssVar('--danger') : '#ffffff',
            weight: h.level === 'high' && !h.origin ? 4 : 2,
            fillColor: fill,
            fillOpacity: 0.85
        };
    }

    // Built from DOM nodes (textContent), so names and labels are always escaped.
    function popupContent(h, hospitalUrl) {
        const root = el('div', 'map-popup');
        root.append(el('strong', null, h.name));
        root.append(el('div', 'muted small', h.district + (h.origin ? ' · szpital zlecający' : '')));
        root.append(el('div', 'popup-occupancy level-' + h.level,
            h.occupancyPercent + '% · ' + h.occupiedBeds + ' / ' + h.totalBeds + ' łóżek'));

        if (h.flags.length > 0) {
            const flags = el('div', 'popup-section');
            h.flags.forEach(f => flags.append(el('span', 'tag-warn', f)));
            root.append(flags);
        }
        if (h.pendingRequests > 0) {
            root.append(el('div', 'popup-section', 'Oczekujące zapytania: ' + h.pendingRequests));
        }
        if (h.declines.length > 0) {
            const list = el('ul', 'popup-declines');
            h.declines.forEach(d => list.append(el('li', null, d.label + ': ' + d.count)));
            const section = el('div', 'popup-section', 'Odmowy:');
            section.append(list);
            root.append(section);
        }

        const link = el('a', 'popup-link', 'Widok szpitala →');
        link.href = hospitalUrl + h.id;
        root.append(link);
        return root;
    }

    function el(tag, className, text) {
        const node = document.createElement(tag);
        if (className) {
            node.className = className;
        }
        if (text !== undefined) {
            node.textContent = text;
        }
        return node;
    }
})();
