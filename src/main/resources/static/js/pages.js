document.addEventListener("DOMContentLoaded", () => {
    setUpHistoryFilter();
    highlightSearchTerms();
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

/*
 * Search page: mark the words of the query inside each excerpt.
 * Search works by meaning, so a passage can match without any of these words;
 * the marks only help the eye when the words are there.
 * Built with text nodes, never innerHTML, so document text can't inject HTML.
 */
function highlightSearchTerms() {
    const excerpts = document.querySelectorAll(".search-result-content");
    const query = document.querySelector(".search-page-form input[name='q']")?.value || "";

    const terms = [...new Set(query.toLowerCase().split(/[^\p{L}\p{N}]+/u).filter((t) => t.length >= 3))];
    if (excerpts.length === 0 || terms.length === 0) {
        return;
    }

    const escaped = terms.map((t) => t.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"));
    const pattern = new RegExp(`(${escaped.join("|")})`, "giu");

    excerpts.forEach((excerpt) => {
        const text = excerpt.textContent;
        const parts = text.split(pattern);
        if (parts.length === 1) return;

        excerpt.textContent = "";
        parts.forEach((part, i) => {
            if (i % 2 === 1) {
                const mark = document.createElement("mark");
                mark.className = "hit";
                mark.textContent = part;
                excerpt.append(mark);
            } else if (part) {
                excerpt.append(document.createTextNode(part));
            }
        });
    });
}
