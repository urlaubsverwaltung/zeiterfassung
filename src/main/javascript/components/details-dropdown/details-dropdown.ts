export class DetailsDropdown extends HTMLDetailsElement {
  // @ts-expect-error is defined...
  #cleanup: () => void;

  connectedCallback() {
    let isContentClicked = false;

    const handleDocumentClick = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      isContentClicked = this.contains(target);
      if (isContentClicked) {
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

    const handleDocumentKeyUp = (event: KeyboardEvent) => {
      if (event.key !== "Escape") {
        return;
      }

      this.open = false;
      isContentClicked = false;
    };

    const handleFocusOut = (event: FocusEvent) => {
      if ((event.target as HTMLElement).tagName !== "SUMMARY") {
        setTimeout(
          () => {
            if (
              !this.matches(":focus-within") &&
              (document.activeElement !== document.body || !isContentClicked)
            ) {
              this.open = false;
            }
          },
          Number(this.dataset.closeDelay ?? 0),
        );
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

function isInteractiveElement(element: HTMLElement) {
  return interactiveElements.some(
    (ElementType) => element instanceof ElementType,
  );
}

customElements.define("z-details-dropdown", DetailsDropdown, {
  extends: "details",
});
