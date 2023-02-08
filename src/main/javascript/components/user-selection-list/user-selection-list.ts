export class UserSelectionList extends HTMLDivElement {
  #cleanup: () => void;

  #updateMaxHeight() {
    const list = this.querySelector("ol");

    const { top } = this.getBoundingClientRect();
    const { height: listHeight } = list.getBoundingClientRect();

    const shouldScroll = listHeight > window.innerHeight - top;

    this.style.setProperty(
      "max-height",
      shouldScroll ? `calc(100vh - ${top}px - 2rem)` : "",
    );
  }

  connectedCallback() {
    this.classList.add("lg:overflow-y-auto");
    this.#updateMaxHeight();

    // eslint-disable-next-line unicorn/consistent-function-scoping
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
