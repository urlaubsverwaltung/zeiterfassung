import { TurboFrameRenderEvent } from "@hotwired/turbo";

export class TimeEntryUserSearch extends HTMLElement {
  private cleanup = () => {};
  private popoverVisible = false;

  connectedCallback() {
    const handleFocusin = () => {
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

    const handleFocusout = () => {
      if (this.popoverVisible && !this.matches(":focus-within")) {
        // safari doesn't like hiding this synchronously...
        // clicking a link first triggers focusout, THEN the link click is handled (click, document.activeElement, ...)
        // therefore delay hiding so clicking a link has an effect.
        // this should not be a problem in other browsers since the full page is loaded which "closes" the popover, too.
        setTimeout(() => {
          this.hideSuggestionsPopover();
        }, 150);
      }
    };

    this.addEventListener("focusin", handleFocusin);
    this.addEventListener("focusout", handleFocusout);
    document.addEventListener("turbo:frame-render", handleFrameRender);

    this.cleanup = function () {
      this.removeEventListener("focusin", handleFocusin);
      this.removeEventListener("focusout", handleFocusout);
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

customElements.define("z-time-entry-user-search", TimeEntryUserSearch);
