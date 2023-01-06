export class ReportBreakdownList extends HTMLOListElement {
  #cleanup: () => void;

  connectedCallback() {
    // eslint-disable-next-line unicorn/consistent-function-scoping
    const handleDaySelected = (event) => {
      const { date } = event.detail;
      for (const li of this.querySelectorAll("li")) {
        if (!date || li.dataset.date === date) {
          li.classList.remove("hidden");
        } else {
          li.classList.add("hidden");
        }
      }
    };

    document.addEventListener("graph-day-selected", handleDaySelected);

    this.#cleanup = () => {
      document.removeEventListener("graph-day-selected", handleDaySelected);
    };
  }

  disconnectedCallback() {
    this.#cleanup();
  }
}

customElements.define("z-report-breakdown-list", ReportBreakdownList, {
  extends: "ol",
});
