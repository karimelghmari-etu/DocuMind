/*
 * Document page: the Print button.
 * PDF and text: print the file shown in the frame (same origin, so the page can ask it to print).
 * If the browser's PDF viewer refuses, open the file in a new tab, where Ctrl+P works.
 * Word files: print our reader view (app.css hides everything else when printing).
 */
document.addEventListener("DOMContentLoaded", () => {
    const button = document.getElementById("printDocument");
    if (!button) return;

    button.addEventListener("click", () => {
        const frame = document.getElementById("documentFrame");

        if (button.dataset.viewable === "true" && frame) {
            try {
                frame.contentWindow.focus();
                frame.contentWindow.print();
                return;
            } catch (error) {
                window.open(button.dataset.fileUrl, "_blank", "noopener");
                return;
            }
        }

        window.print();
    });
});
