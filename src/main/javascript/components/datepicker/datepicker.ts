import { dateAdapter as dateAdapterDE } from "./de";
import { defineCustomElements } from "@duetds/date-picker/custom-element";
import "@duetds/date-picker/dist/collection/themes/default.css";

// register @duet/datepicker
defineCustomElements(window);

export function createDatepicker(selector: string): HTMLDuetDatePickerElement {
  // eslint-disable-next-line unicorn/prefer-query-selector
  const dateElement: HTMLInputElement = document.getElementById(
    selector,
  ) as HTMLInputElement;
  const duetDateElement: HTMLDuetDatePickerElement =
    document.createElement("duet-date-picker");

  if (dateElement.value && !dateElement.dataset.isoValue) {
    throw new Error(
      "date input defines a value but no `data-iso-value` attribute is given.",
    );
  }

  if (window.navigator.language.slice(0, 2) === "de") {
    duetDateElement.dateAdapter = dateAdapterDE;
    duetDateElement.localization =
      globalThis.zeiterfassung.datepicker.localization;
  }

  duetDateElement.setAttribute("style", "--duet-radius=0");
  duetDateElement.setAttribute("class", dateElement.getAttribute("class"));
  duetDateElement.setAttribute("value", dateElement.dataset.isoValue || "");
  duetDateElement.setAttribute("identifier", dateElement.getAttribute("id"));

  return duetDateElement;
}
