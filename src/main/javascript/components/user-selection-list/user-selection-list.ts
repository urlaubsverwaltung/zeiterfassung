export class UserSelectionList extends HTMLDivElement {
  // @ts-expect-error is defined...
  #cleanup: () => void;

  #updateMaxHeight() {
    const list = this.querySelector("ol")!;

    const { top } = this.getBoundingClientRect();
    const { height: listHeight } = list.getBoundingClientRect();

    const shouldScroll = listHeight > window.innerHeight - top;

    this.style.setProperty(
      "max-height",
      shouldScroll ? `calc(100vh - ${top}px - 2rem)` : "",
    );
  }

  connectedCallback() {
    this.classList.add("@5xl/main:overflow-y-auto");
    this.#updateMaxHeight();

    const onResize = () => {
      this.#updateMaxHeight();
    };

    window.addEventListener("resize", onResize, { passive: true });

    this.#cleanup = () => {
      window.removeEventListener("resize", onResize);
    };
  }

  disconnectedCallback() {
    this.#cleanup();
  }
}

customElements.define("z-user-selection-list", UserSelectionList, {
  extends: "div",
});
