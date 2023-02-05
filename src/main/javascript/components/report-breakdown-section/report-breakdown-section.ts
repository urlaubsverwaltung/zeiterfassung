export class ReportBreakdownSection extends HTMLElement {
  #cleanup: () => void;

  connectedCallback() {
    // eslint-disable-next-line unicorn/consistent-function-scoping
    const handleDaySelected = (event) => {
      const { date } = event.detail;
      for (const element of this.querySelectorAll("[data-date]")) {
        if (element.parentElement === this) {
          if (!date || (element as HTMLElement).dataset.date === date) {
            element.removeAttribute("hidden");
          } else {
            element.setAttribute("hidden", "");
          }
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

customElements.define("z-report-breakdown-section", ReportBreakdownSection, {
  extends: "section",
});
