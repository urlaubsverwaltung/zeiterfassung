package de.focusshift.zeiterfassung.web;

public final class HotwiredTurboConstants {

    public static final String TURBO_STREAM_MEDIA_TYPE = "text/vnd.turbo-stream.html";
    public static final String TURBO_FRAME_HEADER = "Turbo-Frame";

    /**
     * Defines the view model attribute name. The value has to be {@linkplain ScrollPreservation}.
     */
    public static final String TURBO_REFRESH_SCROLL_ATTRIBUTE = "turboRefreshScroll";

    /**
     * You can configure how Turbo handles scrolling with a <meta name="turbo-refresh-scroll"> in the page’s head.
     *
     * <pre>{@code
     * <head>
     *   ...
     *   <meta name="turbo-refresh-scroll" content="preserve">
     * </head>
     * }</pre>
     *
     * <p>
     * The possible values are preserve or reset (the default). When it is preserve, when a page refresh happens,
     * Turbo will keep the page’s vertical and horizontal scroll.
     */
    public enum ScrollPreservation {

        RESET("reset"),

        /**
         * When a page refresh happens, Turbo will keep the page’s vertical and horizontal scroll.
         */
        PRESERVE("preserve");

        private final String value;

        ScrollPreservation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private HotwiredTurboConstants() {
        //
    }
}
