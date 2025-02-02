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
    // modal has been opened, and user navigates back with browser-back-button
    // --> keep scroll position
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
