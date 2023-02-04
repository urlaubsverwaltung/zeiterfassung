export class ReportBreakdownList extends HTMLOListElement {
  #cleanup: () => void;

  connectedCallback() {
    // eslint-disable-next-line unicorn/consistent-function-scoping
    const handleDaySelected = (event) => {
      const { date } = event.detail;
      for (const li of this.querySelectorAll("li")) {
        if (li.parentElement === this) {
          if (!date || li.dataset.date === date) {
            li.removeAttribute("hidden");
          } else {
            li.setAttribute("hidden", "");
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

customElements.define("z-report-breakdown-list", ReportBreakdownList, {
  extends: "ol",
});
