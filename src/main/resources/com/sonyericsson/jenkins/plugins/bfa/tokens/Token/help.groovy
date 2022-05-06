dt("\${BUILD_FAILURE_ANALYZER}")
dd() {
    span("Displays found causes by the Build Failure Analyzer.")
    dl() {
        dt("includeIndications")
        dd("When true, the indication numbers and links into the console log are included in the token replacement text.")

        dt("useHtmlFormat")
        dd("When true, the replacement text will be an HTML snippet.")

        dt("includeTitle")
        dd("When true, the title will appear in the token replacement text.")

        dt("wrapWidth")
        dd("Wrap long lines at this width. If wrapWidth is 0, the text isn't wrapped. Only applies if useHtmlFormat == false.")

        dt("noFailureText")
        dd("Text to return when no failure cause is present.")

        dt("escapeHtml")
        dd("If true, any HTML specific code will be escaped. Only applies if useHtmlFormat == false. Defaults to false.")
    }
}