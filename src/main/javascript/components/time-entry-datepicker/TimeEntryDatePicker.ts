import { isToday } from "date-fns";
import format from "../../date-fns-locale-aware/format";
import { createDatepicker } from "../datepicker/datepicker";
import { i18n } from "../../i18n";

class TimeEntryDatePicker extends HTMLDivElement {
  #duetDateElement: HTMLDuetDatePickerElement;

  connectedCallback() {
    this.#initDatepicker();
  }

  #initDatepicker() {
    const originalDateInput = this.querySelector("input[type=date]");
    const originalDateInputId = originalDateInput.getAttribute("id");
    const label = document.querySelector(`[for='${originalDateInputId}']`);

    originalDateInput.classList.add("hidden");

    this.#duetDateElement = createDatepicker(originalDateInputId);
    originalDateInput.replaceWith(this.#duetDateElement);

    // date-picker should be positioned to parentElement. not to the input[date]
    this.classList.add("relative");

    // make me keyboard focusable instead of the (hidden) input[date]
    this.setAttribute("tabindex", "0");
    this.addEventListener("keydown", async (event: KeyboardEvent) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        await this.#duetDateElement.show();
      }
    });

    // actually we have to wait till duet-date-picker has been rendered successfully. but... how?
    setTimeout(() => {
      (
        this.#duetDateElement.querySelector(
          ".duet-date__input-wrapper",
        ) as HTMLElement
      ).style.display = "none";
      (
        this.#duetDateElement.querySelector(".duet-date") as HTMLElement
      ).style.position = "static"; // align datepicker to parentElement
      this.#duetDateElement.classList.remove("hidden");

      // reset width of 100% which aligns the datepicker on the left
      const duetDateDialog: HTMLElement =
        this.#duetDateElement.querySelector(".duet-date__dialog");
      duetDateDialog.style.width = "auto";
    }, 100);

    // clicking the label should show duet-date-picker
    label.addEventListener("click", async (event) => {
      event.preventDefault();
      await this.#duetDateElement.show();
    });

    // update selected date text on change
    this.#duetDateElement.addEventListener(
      "duetChange",
      (event: CustomEvent) => {
        const { valueAsDate } = event.detail;

        const dayOfWeek = format(valueAsDate, "EEEE");
        label.querySelector("[data-day]").textContent = format(
          valueAsDate,
          "dd",
        );
        label.querySelector("[data-day-text]").textContent = isToday(
          valueAsDate,
        )
          ? `${i18n("datepicker.today")}, ${dayOfWeek}`
          : dayOfWeek;
        label.querySelector("[data-date]").textContent = format(
          valueAsDate,
          "dd. MMMM yyyy",
        );
      },
    );
  }
}

customElements.define("z-time-entry-date-picker", TimeEntryDatePicker, {
  extends: "div",
});
