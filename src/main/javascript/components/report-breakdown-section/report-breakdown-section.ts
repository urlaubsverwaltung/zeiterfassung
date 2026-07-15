export class ReportBreakdownSection extends HTMLElement {
  // @ts-expect-error it is defined...
  #cleanup: () => void;

  connectedCallback() {
    const handleDaySelected = (event: CustomEvent) => {
      const { date } = event.detail;
      for (const element of this.querySelectorAll("[data-date]")) {
        if (element.parentElement !== this) {
          continue;
        }
        const element_ = element as HTMLElement;
        element_.toggleAttribute(
          "hidden",
          Boolean(date) && element_.dataset.date !== date,
        );
      }
    };

    // @ts-expect-error so it is
    document.addEventListener("graph-day-selected", handleDaySelected);

    this.#cleanup = () => {
      // @ts-expect-error so it is
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
