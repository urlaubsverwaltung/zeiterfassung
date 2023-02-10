/* eslint-disable unicorn/consistent-function-scoping */
export class ReportGraph extends HTMLDivElement {
  #cleanup: () => void;

  connectedCallback() {
    const handleClick = (event) => {
      if (event.target.matches("path[data-date]")) {
        const { date } = event.target.dataset;
        const currentlySelected = event.target.classList.contains("selected");
        if (currentlySelected) {
          this.querySelector("svg").dispatchEvent(
            new CustomEvent("graph-day-selected", {
              bubbles: true,
              detail: {},
            }),
          );
        } else {
          this.querySelector("svg").dispatchEvent(
            new CustomEvent("graph-day-selected", {
              bubbles: true,
              detail: {
                date,
              },
            }),
          );
        }
        this.querySelector("path.selected")?.classList.remove(
          "stroke-orange-900",
          "fill-orange-400",
          "selected",
        );
        if (!currentlySelected) {
          event.target.classList.add(
            "stroke-orange-900",
            "fill-orange-400",
            "selected",
          );
        }
      }
    };

    const handleMouseOver = (event) => {
      if (event.target.matches("path[data-date]")) {
        event.target.classList.add("stroke-orange-900", "fill-orange-400");
      }
    };

    const handleMouseOut = (event) => {
      if (
        event.target.matches("path[data-date]") &&
        !event.target.classList.contains("selected")
      ) {
        event.target.classList.remove("stroke-orange-900", "fill-orange-400");
      }
    };

    this.addEventListener("click", handleClick);
    this.addEventListener("mouseover", handleMouseOver);
    this.addEventListener("mouseout", handleMouseOut);

    // animate color transition when element is selected / deselected
    for (const path of this.querySelectorAll("path[data-date]")) {
      path.classList.add("transition-colors", "cursor-pointer");
    }

    this.#cleanup = () => {
      this.removeEventListener("click", handleClick);
      this.removeEventListener("mouseover", handleMouseOver);
      this.removeEventListener("mouseout", handleMouseOut);
    };
  }

  disconnectedCallback() {
    this.#cleanup();
  }
}

customElements.define("z-report-graph", ReportGraph, { extends: "div" });
