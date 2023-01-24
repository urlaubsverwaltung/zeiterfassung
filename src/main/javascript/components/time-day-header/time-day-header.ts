import { useSticky } from "./use-sticky";

export class TimeDayHeader extends HTMLHeadingElement {
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

customElements.define("z-time-day-header", TimeDayHeader, { extends: "h4" });
