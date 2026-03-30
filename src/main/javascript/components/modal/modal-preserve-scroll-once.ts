import * as Turbo from "@hotwired/turbo";
import { onTurboRender } from "../../turbo-events";

/**
 * Locks the scroll position for the next turbo rendering.
 * Useful when called before window.history.back() for instance.
 */
export function preserveScrollOnce() {
  onTurboRender(
    function () {
      // @ts-expect-error it exists
      Turbo.navigator.currentVisit.scrolled = true;
    },
    { once: true },
  );
}
