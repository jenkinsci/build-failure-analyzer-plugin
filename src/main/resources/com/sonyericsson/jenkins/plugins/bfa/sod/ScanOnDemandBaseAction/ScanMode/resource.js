document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".bfa-scan-mode-build-type-radio-entry").forEach((entry) => {
        const { rootUrl, optionFullUrl } = entry.querySelector(".bfa-entry-data-holder").dataset;

        entry.querySelector(".jenkins-radio").addEventListener("click", () => {
            document.location = `${rootURL}/${optionFullUrl}`;
        });
    })
});
