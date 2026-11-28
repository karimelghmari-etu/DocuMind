
document.addEventListener("DOMContentLoaded", () => {

    const fileInput = document.getElementById("documentFile");
    const selectedFile = document.getElementById("selectedFile");
    const selectedFileName = document.getElementById("selectedFileName");

    if (!fileInput || !selectedFile || !selectedFileName) {
        return;
    }

    fileInput.addEventListener("change", () => {

        const file = fileInput.files[0];

        if (!file) {
            selectedFileName.textContent = "No file selected";
            selectedFile.classList.remove("has-file");
            return;
        }

        const fileSize = (file.size / (1024 * 1024)).toFixed(2);

        selectedFileName.textContent =
            `${file.name} (${fileSize} MB)`;

        selectedFile.classList.add("has-file");
    });

});
