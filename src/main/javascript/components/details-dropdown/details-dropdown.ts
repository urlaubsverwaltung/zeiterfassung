export class DetailsDropdown extends HTMLDetailsElement {
  #cleanup: () => void;

  connectedCallback() {
    let contentClicked = false;

    const handleDocumentClick = (event) => {
      const { target } = event;
      contentClicked = this.contains(target);
      if (contentClicked) {
        if (!isInteractiveElement(target)) {
          // prevent closing of detail element only when clicked element is not interactive.
          // otherwise we would prevent a form submit for instance.
          event.preventDefault();
        }
        if (target.tagName === "SUMMARY" || target.closest("summary")) {
          this.open = !this.open;
        }
      } else if (target !== this) {
        this.open = false;
      }
    };

    const handleDocumentKeyUp = (event) => {
      if (event.key === "Escape") {
        this.open = false;
        contentClicked = false;
      }
    };

    const handleFocusOut = (event) => {
      if (event.target.tagName !== "SUMMARY") {
        setTimeout(() => {
          if (
            !this.matches(":focus-within") &&
            (document.activeElement !== document.body || !contentClicked)
          ) {
            this.open = false;
          }
        }, Number(this.dataset.closeDelay ?? 0));
      }
    };

    document.addEventListener("click", handleDocumentClick);
    document.addEventListener("keyup", handleDocumentKeyUp);
    this.addEventListener("focusout", handleFocusOut);

    this.#cleanup = () => {
      document.removeEventListener("click", handleDocumentClick);
      document.removeEventListener("keyup", handleDocumentKeyUp);
      this.removeEventListener("focusout", handleFocusOut);
    };
  }

  disconnectedCallback() {
    this.#cleanup();
  }
}

const interactiveElements = [
  HTMLButtonElement,
  HTMLInputElement,
  HTMLSelectElement,
  HTMLTextAreaElement,
  HTMLLabelElement,
];

function isInteractiveElement(element) {
  return interactiveElements.some(
    (ElementType) => element instanceof ElementType,
  );
}

customElements.define("z-details-dropdown", DetailsDropdown, {
  extends: "details",
});
