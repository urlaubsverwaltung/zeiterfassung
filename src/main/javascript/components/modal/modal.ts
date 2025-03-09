import { preserveScrollOnce } from "../../turbo";

const MODAL_TURBO_FRAME = "#frame-modal";

/**
 * Progressively enhance the modal (see thymeleaf modal template).
 *
 * <ul>
 *   <li>it will be wrapped with a <code>dialog</code> element</li>
 *   <li>pressing ESC will close the dialog</li>
 * </ul>
 */
export function enhanceModal() {
  globalThis.addEventListener("popstate", () => {
    // given use case:
    // - user is on the default report page with url=/report
    // - user opens a timeEntry modal
    //   - only modal-frame is updated because of custom turbo:before-frame-render implementation below
    //   - url is advanced to /report/year/{}/week/{}?timeEntryId={}
    //     - to know what to render after constraint-validation-errors (which is a redirect to this advanced url)
    //     - to know which 'close-modal' form action to use (week or month report)
    // - user clicks save
    //   - redirected to /report/year/{}/week/{}
    //   - html <meta name=turbo-refresh-scroll content=preserve> keeps the scroll position due to rendering "a different" page
    // - user opens another timeEntry modal
    //   - turbo only renders the modal-frame
    // - user goes back with browser-back-button (or closes the modal, same behavior)
    //   - turbo renders the redirected page but has lost scroll-preserve context
    //   - therefore this hack to keep the scroll position
    if (document.querySelector("dialog[open]")) {
      preserveScrollOnce();
    }
  });

  // modal X button on the top right
  // --> close modal on click without reloading html
  document.addEventListener("click", (event) => {
    const target = event.target as HTMLElement;
    if (
      // dialog is only rendered when modal has been called on the page with enabled JavaScript.
      // opening a page with a URL rendering a modal does NOT render the <dialog>
      // (in this case the page is reloaded clicking the close button)
      target.closest("dialog") &&
      target.closest("[data-modal-close-button]")
    ) {
      event.preventDefault(); // prevent form submit
      closeModal();
    }
  });

  // --> close modal when Escape is pressed
  document.addEventListener("keyup", (event) => {
    if (event.key === "Escape" && !inputElementFocused()) {
      closeModal();
    }
  });

  // enhance modal with <dialog>
  document.addEventListener(
    "turbo:before-frame-render",
    (event: CustomEvent) => {
      if (!event.detail.newFrame.matches(MODAL_TURBO_FRAME)) {
        return;
      }

      event.detail.render = (
        currentFrame: HTMLElement,
        newFrame: HTMLElement,
      ) => {
        if (event.detail.renderMethod === "replace") {
          const currentlyShowingDialog = Boolean(currentFrame.innerHTML.trim());
          const nextShowingDialog = Boolean(newFrame.innerHTML.trim());

          // next showing dialog could contain constraint validation errors
          // in this case turbo doesn't update the history, and we don't have to touch it either.
          if (currentlyShowingDialog && !nextShowingDialog) {
            // rendering the dialog advances history. (to be able to close the dialog with back-button)
            // --
            // closing the dialog via submit or close, sends a request to the server
            // which answers with an empty frame-modal. due to the request the history stack is replaced (by turbo).
            // without calling history.back() here, the user would still be on the same view going backwards the first time.
            // therefore we have to go back first programmatically :)
            globalThis.history.back();
          }

          const dialog = document.createElement("dialog");
          dialog.setAttribute("open", "");
          dialog.innerHTML = newFrame.innerHTML;
          document.querySelector("#frame-modal").innerHTML = dialog.outerHTML;
        } else {
          console.warn(
            "rendering frame-modal should use renderMethod=replace but was",
            event.detail.renderMethod,
          );
        }
      };
    },
  );
}

function closeModal() {
  // we only have ONE dialog and I think this should only be one for some time
  const dialog = document.querySelector("dialog");
  if (dialog) {
    // well... this presupposes that modals are always handled with URLs. hmm...
    preserveScrollOnce();
    globalThis.history.back();
  }
}

function inputElementFocused() {
  return document.activeElement?.matches("input");
}
