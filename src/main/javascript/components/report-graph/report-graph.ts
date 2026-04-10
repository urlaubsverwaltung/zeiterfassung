export class ReportGraph extends HTMLDivElement {
  // @ts-expect-error it is defined...
  #cleanup: () => void;

  connectedCallback() {
    const handleClick = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      if (!target.matches("[data-date]")) {
        return;
      }

      const { date } = target.dataset;

      const currentlySelected = target.classList.contains("selected");
      if (currentlySelected) {
        this.querySelector("svg")!.dispatchEvent(
          new CustomEvent("graph-day-selected", {
            bubbles: true,
            detail: {},
          }),
        );
      } else {
        this.querySelector("svg")!.dispatchEvent(
          new CustomEvent("graph-day-selected", {
            bubbles: true,
            detail: {
              date,
            },
          }),
        );
      }

      this.querySelector(".selected")?.classList.remove("selected");
      if (!currentlySelected) {
        target.classList.add("selected");
      }
    };

    this.addEventListener("click", handleClick);

    this.#cleanup = () => {
      this.removeEventListener("click", handleClick);
    };
  }

  disconnectedCallback() {
    this.#cleanup();
  }
}

customElements.define("z-report-graph", ReportGraph, { extends: "div" });
