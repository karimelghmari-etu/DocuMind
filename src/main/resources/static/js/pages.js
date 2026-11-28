document.addEventListener("DOMContentLoaded", () => {
    setUpHistoryFilter();
});

/* History page: hide conversations whose title does not contain the filter text. */
function setUpHistoryFilter() {
    const input = document.getElementById("historyFilter");
    const count = document.getElementById("historyCount");
    const noMatch = document.getElementById("historyNoMatch");
    const rows = [...document.querySelectorAll(".history-row")];

    if (!input || rows.length === 0) {
        return;
    }

    input.addEventListener("input", () => {
        const text = input.value.trim().toLowerCase();
        let visible = 0;

        rows.forEach((row) => {
            const matches = row.dataset.title.includes(text);
            row.hidden = !matches;
            if (matches) visible++;
        });

        if (count) {
            count.textContent = text
                ? `${visible} of ${rows.length}`
                : `${rows.length} ${rows.length === 1 ? "conversation" : "conversations"}`;
        }
        if (noMatch) {
            noMatch.hidden = visible > 0;
        }
    });
}
