/*
 * Light / dark theme. Loaded in <head> (not deferred) so the saved choice is applied
 * before the page is drawn, without a white flash in dark mode.
 * No saved choice: the page follows the system setting (prefers-color-scheme in base.css).
 */
(function () {
    var KEY = "documind-theme";
    var root = document.documentElement;

    try {
        var saved = localStorage.getItem(KEY);
        if (saved === "light" || saved === "dark") {
            root.dataset.theme = saved;
        }
    } catch (e) {
        // Storage blocked (private window, strict settings): just follow the system.
    }

    function currentTheme() {
        if (root.dataset.theme) return root.dataset.theme;
        return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }

    function refresh(button) {
        var mode = currentTheme();
        button.dataset.mode = mode;
        button.setAttribute("aria-label", mode === "dark" ? "Switch to light mode" : "Switch to dark mode");
        button.title = button.getAttribute("aria-label");
    }

    document.addEventListener("DOMContentLoaded", function () {
        var buttons = document.querySelectorAll("[data-theme-toggle]");

        buttons.forEach(function (button) {
            refresh(button);
            button.addEventListener("click", function () {
                var next = currentTheme() === "dark" ? "light" : "dark";
                root.dataset.theme = next;
                try {
                    localStorage.setItem(KEY, next);
                } catch (e) { /* the choice lasts until the page is closed */ }
                buttons.forEach(refresh);
            });
        });

        // Keep the icon right when the system theme changes and no choice was saved.
        window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", function () {
            buttons.forEach(refresh);
        });
    });
})();
