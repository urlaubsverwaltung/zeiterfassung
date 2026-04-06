import { TurboFrameRenderEvent } from "@hotwired/turbo";

export class UserSearch extends HTMLElement {
  private cleanup = () => {};
  private popoverVisible = false;

  connectedCallback() {
    const handleThisFocusin = () => {
      // trigger empty search to show initial suggestions when input is focused
      if (document.activeElement?.matches("input")) {
        this.submit();
      }
    };

    // show popover on initial submit.
    // subsequent renders can be ignored since content is updated, not the popover itself.
    const handleFrameRender = (event: TurboFrameRenderEvent) => {
      if (
        !this.popoverVisible &&
        // @ts-expect-error matches exists...
        event.target?.matches("[id=frame-users-suggestions]")
      ) {
        this.showSuggestionsPopover();
      }
    };

    // suggestion popover should not be closed
    // when a suggestion link is supposed to be clicked
    let pointerdownSuggestionLink = false;

    const handleGlobalPointerdown = (event: PointerEvent) => {
      const target = event.target as HTMLElement;
      pointerdownSuggestionLink = Boolean(
        target.closest("a") && this.contains(target),
      );
    };

    const handleGlobalPointerup = () => {
      pointerdownSuggestionLink = false;
    };

    const handleThisFocusout = (event: FocusEvent) => {
      if (
        !pointerdownSuggestionLink &&
        !this.contains(event.relatedTarget as Element)
      ) {
        this.hideSuggestionsPopover();
      }
    };

    this.addEventListener("focusin", handleThisFocusin);
    this.addEventListener("focusout", handleThisFocusout);
    document.addEventListener("pointerdown", handleGlobalPointerdown);
    document.addEventListener("pointerup", handleGlobalPointerup);
    document.addEventListener("turbo:frame-render", handleFrameRender);

    this.cleanup = function () {
      this.removeEventListener("focusin", handleThisFocusin);
      this.removeEventListener("focusout", handleThisFocusout);
      document.removeEventListener("pointerdown", handleGlobalPointerdown);
      document.removeEventListener("pointerup", handleGlobalPointerup);
      document.removeEventListener("turbo:frame-render", handleFrameRender);
    };
  }

  disconnectedCallback() {
    this.cleanup();
  }

  connectedMoveCallback() {
    // prevent connected/disconnected callbacks to be called when element is moved
  }

  private showSuggestionsPopover() {
    const popover = this.querySelector("[popover]") as HTMLElement;
    popover.showPopover();
    this.popoverVisible = true;
  }

  private hideSuggestionsPopover() {
    const popover = this.querySelector("[popover]") as HTMLElement;
    popover.hidePopover();
    this.popoverVisible = false;
  }

  private submit() {
    // always query element, do not memoize it, could be rerendered!
    const form = this.querySelector("form") as HTMLFormElement | null;
    const button = form?.querySelector("[type=submit]") as HTMLElement | null;
    form?.requestSubmit(button);
  }
}

customElements.define("z-user-search", UserSearch);
