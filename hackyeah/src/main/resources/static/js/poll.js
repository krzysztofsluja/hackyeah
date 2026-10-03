// Periodically reloads a Thymeleaf fragment. The fragment's root element carries the same id as
// the target, so the whole element is swapped (outerHTML) and looked up again on every tick.
// Stand-in for SSE: once the backend streams events, swap the interval for an EventSource
// that calls the same refresh() after each event.

// Single refresh interval for every live view, including the coordinator map.
const POLL_INTERVAL_MS = 1500;

function poll(url, targetId, intervalMs = POLL_INTERVAL_MS) {
    async function refresh() {
        const target = document.getElementById(targetId);
        if (!target) {
            return;
        }
        try {
            const response = await fetch(url, { headers: { 'Accept': 'text/html' } });
            if (response.ok) {
                target.outerHTML = await response.text();
            }
        } catch (e) {
            // Network hiccup: keep the last rendered state, the next tick retries.
        }
    }

    setInterval(refresh, intervalMs);
    return refresh;
}
