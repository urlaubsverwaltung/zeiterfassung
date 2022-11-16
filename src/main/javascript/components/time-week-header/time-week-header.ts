import { useSticky } from "./use-sticky";

export class TimeWeekHeader extends HTMLHeadingElement {
  #cleanup: () => void;

  connectedCallback() {
    const unsubscribeSticky = useSticky(this).subscribe(({ sticky }) => {
      this.classList.toggle("is-pinned", sticky);
    });

    this.#cleanup = () => {
      unsubscribeSticky();
    };
  }

  disconnectedCallback() {
    this.#cleanup();
  }
}

customElements.define("z-time-week-header", TimeWeekHeader, { extends: "h3" });
