import * as Turbo from "@hotwired/turbo";

/**
 * Locks the scroll position for the next turbo rendering.
 * Useful when called before window.history.back() for instance.
 */
export function preserveScrollOnce() {
  document.addEventListener(
    "turbo:render",
    function () {
      // @ts-expect-error it exists
      Turbo.navigator.currentVisit.scrolled = true;
    },
    { once: true },
  );
}
