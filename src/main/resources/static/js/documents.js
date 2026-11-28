document.addEventListener("DOMContentLoaded", () => {
    setUpFileInput();
    watchProcessingDocuments();
});

/* =========================================
   UPLOAD: show the chosen file, reject files that are too large
   before sending 50 MB to the server for nothing.
========================================= */

function setUpFileInput() {
    const form = document.getElementById("uploadForm");
    const fileInput = document.getElementById("documentFile");
    const selectedFile = document.getElementById("selectedFile");
    const selectedFileName = document.getElementById("selectedFileName");
    const clientError = document.getElementById("clientError");

    if (!form || !fileInput || !selectedFile || !selectedFileName) {
        return;
    }

    const maxBytes = parseSize(form.dataset.maxSize);

    const showError = (message) => {
        if (!clientError) return;
        clientError.textContent = message;
        clientError.hidden = !message;
    };

    fileInput.addEventListener("change", () => {
        const file = fileInput.files[0];
        showError("");

        if (!file) {
            selectedFileName.textContent = "No file selected";
            selectedFile.classList.remove("has-file");
            return;
        }

        const fileSize = (file.size / (1024 * 1024)).toFixed(2);
        selectedFileName.textContent = `${file.name} (${fileSize} MB)`;
        selectedFile.classList.add("has-file");

        if (maxBytes && file.size > maxBytes) {
            showError(`File exceeds the maximum size of ${form.dataset.maxSize}`);
        }
    });

    form.addEventListener("submit", (event) => {
        const file = fileInput.files[0];

        if (!file) {
            event.preventDefault();
            showError("Please choose a file to upload.");
        } else if (maxBytes && file.size > maxBytes) {
            event.preventDefault();
            showError(`File exceeds the maximum size of ${form.dataset.maxSize}`);
        }
    });
}

/** "50MB" → 52428800. Same units as Spring's DataSize (B, KB, MB, GB). */
function parseSize(text) {
    const match = /^\s*(\d+)\s*(B|KB|MB|GB)?\s*$/i.exec(text || "");
    if (!match) return null;

    const units = { B: 1, KB: 1024, MB: 1024 ** 2, GB: 1024 ** 3 };
    return Number(match[1]) * units[(match[2] || "B").toUpperCase()];
}

/* =========================================
   STATUS: while a document is PENDING or PROCESSING,
   ask the server every few seconds and update its badge.
========================================= */

const POLL_EVERY_MS = 3000;
const WAITING_STATUSES = ["PENDING", "PROCESSING"];

function watchProcessingDocuments() {
    const badges = [...document.querySelectorAll(".status-badge[data-document-id]")]
        .filter((badge) => WAITING_STATUSES.includes(badge.dataset.status));

    badges.forEach((badge) => pollStatus(badge));
}

async function pollStatus(badge) {
    const documentId = badge.dataset.documentId;

    try {
        const response = await fetch(`/documents/${documentId}/status`, {
            headers: { "Accept": "application/json" }
        });

        if (response.status === 404) {
            badge.closest("tr")?.remove();   // deleted meanwhile (e.g. in another tab)
            return;
        }
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const status = (await response.json()).documentStatus;

        if (status !== badge.dataset.status) {
            updateBadge(badge, status);
        }

        if (status === "FAILED") {
            // The "Reprocess" button only exists for FAILED rows, so reload to show it.
            window.location.reload();
            return;
        }

        if (WAITING_STATUSES.includes(status)) {
            setTimeout(() => pollStatus(badge), POLL_EVERY_MS);
        }
    } catch (error) {
        // Network blip or session expired: try again later, a bit slower.
        console.warn(`Could not refresh status of document ${documentId}:`, error);
        setTimeout(() => pollStatus(badge), POLL_EVERY_MS * 3);
    }
}

function updateBadge(badge, status) {
    badge.classList.remove(`status-${badge.dataset.status.toLowerCase()}`);
    badge.classList.add(`status-${status.toLowerCase()}`);
    badge.dataset.status = status;
    badge.textContent = status;
}
