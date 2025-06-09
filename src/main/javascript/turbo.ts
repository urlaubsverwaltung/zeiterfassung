import * as Turbo from "@hotwired/turbo";
import {
  onTurboClick,
  onTurboFetchRequestError,
  onTurboRender,
  onTurboSubmitStart,
} from "./turbo-events";

// opt-in to turbo with `data-turbo="true"`
Turbo.session.drive = false;

/**
 * Locks the scroll position for the next turbo rendering.
 * Useful when called before window.history.back() for instance.
 */
export function preserveScrollOnce() {
  onTurboRender(
    function () {
      Turbo.navigator.currentVisit.scrolled = true;
    },
    { once: true },
  );
}

onTurboClick(function (event) {
  maybeToggleAjaxLoading(event.target as HTMLElement);
});

onTurboSubmitStart(function (event) {
  maybeToggleAjaxLoading(
    event.detail.formSubmission.submitter as HTMLButtonElement,
  );
});

onTurboFetchRequestError(function (event) {
  // e.g. happens when user does a request after session-timeout

  console.debug(
    "[turbo:fetch-request-error]. reloading current page to restore state.",
    {
      detail: event.detail,
    },
  );

  // GIVEN application session timed out but not the auth server one.
  // THEN user sees the same site again, at the same scroll position.
  // THEN user is confused and clicks the same turbo link again
  // THEN user is happy and keeps on using the app :o)

  globalThis.location.reload();
});

/**
 * Ajax loading class will be added to the element if required.
 *
 * <p>
 * Removing the class is not required as the whole element will be re-rendered by hotwired/turbo
 *
 * @param element
 */
function maybeToggleAjaxLoading(element: HTMLElement) {
  if (element.classList.contains("ajax-loader")) {
    element.classList.add("ajax-loader--loading");
  }
}
