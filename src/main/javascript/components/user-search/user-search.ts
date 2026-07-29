import { TurboFrameRenderEvent } from "@hotwired/turbo";

function focusSuggestion(element: HTMLElement) {
  element.focus();
  element.closest("li")!.scrollIntoView({ block: "nearest" });
}

export class UserSearch extends HTMLElement {
  #cleanup = () => {};
  #popoverVisible = false;

  get #searchInput(): HTMLInputElement {
    return this.querySelector("input[type=search]")!;
  }

  get #submitButton(): HTMLButtonElement {
    return this.querySelector("[type=submit]")!;
  }

  #showSuggestionsPopover() {
    const popover = this.querySelector("[popover]") as HTMLDialogElement;
    popover.showPopover();
    this.#searchInput.setAttribute("aria-expanded", "true");
    this.#popoverVisible = true;
  }

  #hideSuggestionsPopover() {
    const popover = this.querySelector("[popover]") as HTMLDialogElement;
    popover.hidePopover();
    this.#searchInput.setAttribute("aria-expanded", "false");
    this.#popoverVisible = false;
  }

  #submit() {
    // always query element, do not memoize it, could be rerendered!
    const form = this.querySelector("form");
    form?.requestSubmit(this.#submitButton);
  }

  connectedCallback() {
    let isLoading = false;

    this.addEventListener("submit", () => {
      isLoading = true;
      setTimeout(() => {
        if (isLoading) {
          this.#submitButton.classList.add("button--loading");
        }
      }, 100);
    });

    const handleThisFocusin = () => {
      // trigger empty search to show initial suggestions when input is focused
      if (document.activeElement?.matches("input")) {
        this.#submit();
      }
    };

    // show popover on initial submit.
    // subsequent renders can be ignored since content is updated, not the popover itself.
    const handleFrameRender = (event: TurboFrameRenderEvent) => {
      isLoading = false;
      this.#submitButton.classList.remove("button--loading");
      if (
        !this.#popoverVisible &&
        // @ts-expect-error matches exists...
        event.target?.matches("[id=frame-users-suggestions]")
      ) {
        this.#showSuggestionsPopover();
      }
    };

    // suggestion popover should not be closed
    // when a suggestion link is supposed to be clicked
    let isPointerdownSuggestionLink = false;

    const handleGlobalPointerdown = (event: PointerEvent) => {
      const target = event.target as HTMLElement;
      isPointerdownSuggestionLink = Boolean(
        target.closest("a") && this.contains(target),
      );
    };

    const handleGlobalPointerup = () => {
      isPointerdownSuggestionLink = false;
    };

    const handleThisFocusout = (event: FocusEvent) => {
      if (
        !isPointerdownSuggestionLink &&
        !this.contains(event.relatedTarget as HTMLElement)
      ) {
        this.#hideSuggestionsPopover();
      }
    };

    const handleThisKeydown = (event: KeyboardEvent) => {
      if (
        event.key !== "ArrowDown" &&
        event.key !== "ArrowUp" &&
        event.key !== "Escape"
      ) {
        return;
      }

      const input = this.#searchInput;

      if (event.key === "Escape") {
        event.preventDefault();
        this.#hideSuggestionsPopover();
        input.focus();
        return;
      }

      const suggestions = [
        ...(this.querySelectorAll(
          "[data-user-search-suggestion]",
        ) as unknown as HTMLElement[]),
      ];

      if (!this.#popoverVisible || suggestions.length === 0) {
        return;
      }

      event.preventDefault();
      const focusedIndex = suggestions.indexOf(
        document.activeElement as unknown as HTMLElement,
      );

      if (event.key === "ArrowDown") {
        if (focusedIndex === -1) {
          focusSuggestion(suggestions[0]);
        } else if (focusedIndex < suggestions.length - 1) {
          focusSuggestion(suggestions[focusedIndex + 1]);
        }
      } else {
        if (focusedIndex === 0) {
          input.focus();
        } else if (focusedIndex > 0) {
          focusSuggestion(suggestions[focusedIndex - 1]);
        }
      }
    };

    this.addEventListener("focusin", handleThisFocusin);
    this.addEventListener("focusout", handleThisFocusout);
    this.addEventListener("keydown", handleThisKeydown);
    document.addEventListener("pointerdown", handleGlobalPointerdown);
    document.addEventListener("pointerup", handleGlobalPointerup);
    document.addEventListener("turbo:frame-render", handleFrameRender);

    this.#cleanup = () => {
      this.removeEventListener("focusin", handleThisFocusin);
      this.removeEventListener("focusout", handleThisFocusout);
      this.removeEventListener("keydown", handleThisKeydown);
      document.removeEventListener("pointerdown", handleGlobalPointerdown);
      document.removeEventListener("pointerup", handleGlobalPointerup);
      document.removeEventListener("turbo:frame-render", handleFrameRender);
    };
  }

  disconnectedCallback() {
    this.#cleanup();
  }

  connectedMoveCallback() {
    // prevent connected/disconnected callbacks to be called when element is moved
  }
}

customElements.define("z-user-search", UserSearch);
