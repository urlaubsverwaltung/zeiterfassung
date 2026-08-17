import { i18n } from "../../i18n";

function parseMinutes(value: string): number | undefined {
  const match = /^(\d{1,2}):(\d{2})$/.exec(value);
  if (!match) {
    return undefined;
  }
  return Number(match[1]) * 60 + Number(match[2]);
}

function getFormDateIso(form: Element): string | undefined {
  const duetDatePicker = form.querySelector("duet-date-picker") as
    (Element & { value?: string }) | null;
  if (duetDatePicker?.value) {
    return duetDatePicker.value;
  }
  const dateInput = form.querySelector(
    "input[name='date']",
  ) as HTMLInputElement | null;
  return dateInput?.value || undefined;
}

function getFormIsBreak(form: Element): boolean {
  const breakCheckbox = form.querySelector(
    "input[name='break']",
  ) as HTMLInputElement | null;
  return breakCheckbox ? breakCheckbox.checked : false;
}

function getFormTimespan(
  form: Element,
): { startMinutes: number; endMinutes: number } | undefined {
  const startElement = form.querySelector(
    "input[name='start']",
  ) as HTMLInputElement | null;
  const endElement = form.querySelector(
    "input[name='end']",
  ) as HTMLInputElement | null;
  const durationElement = form.querySelector(
    "input[name='duration']",
  ) as HTMLInputElement | null;

  const start = startElement?.value
    ? parseMinutes(startElement.value)
    : undefined;
  const end = endElement?.value ? parseMinutes(endElement.value) : undefined;
  const duration = durationElement?.value
    ? parseMinutes(durationElement.value)
    : undefined;

  if (start !== undefined && end !== undefined) {
    // entries spanning midnight are not checked for overlaps client-side, the backend still rejects them.
    return end > start ? { startMinutes: start, endMinutes: end } : undefined;
  }
  if (start !== undefined && duration !== undefined) {
    return { startMinutes: start, endMinutes: start + duration };
  }
  if (end !== undefined && duration !== undefined) {
    return { startMinutes: end - duration, endMinutes: end };
  }
  return undefined;
}

class TimeEntrySlotForm extends HTMLFormElement {
  #hasBeenTriedToSubmitAtLeastOnce = false;

  connectedCallback() {
    // prevent html validation messages. we're doing it ourself here with JavaScript
    this.setAttribute("novalidate", "");

    this.addEventListener("turbo:submit-start", () => {
      this.querySelector(".ajax-loader")?.classList.add("ajax-loader--loading");
    });

    this.addEventListener("submit", (event) => {
      this.#hasBeenTriedToSubmitAtLeastOnce = true;
      if (!this.#validate()) {
        event.preventDefault();
      }
    });

    const errorContainer = this.querySelector(
      "[data-error-container]",
    ) as HTMLElement;
    const startElement = this.querySelector(
      "input[name='start']",
    ) as HTMLInputElement;
    const endElement = this.querySelector(
      "input[name='end']",
    ) as HTMLInputElement;
    const durationElement = this.querySelector(
      "input[name='duration']",
    ) as HTMLInputElement;

    startElement.addEventListener("blur", () => {
      if (!this.#hasBeenTriedToSubmitAtLeastOnce) {
        return;
      }
      if (startElement.value) {
        startElement.setCustomValidity("");
        if (!endElement.value && !durationElement.value) {
          errorContainer.innerHTML = `<ul><li>${i18n(
            "time-entry.validation.endOrDuration.required",
          )}</li></ul>`;
        } else {
          errorContainer.innerHTML = ``;
          durationElement.setCustomValidity("");
        }
      } else {
        if (endElement.value && durationElement.value) {
          startElement.setCustomValidity("");
          errorContainer.innerHTML = ``;
        } else if (!endElement.value && !durationElement.value) {
          startElement.setCustomValidity("required");
          durationElement.setCustomValidity("required");
          errorContainer.innerHTML = ``;
        } else if (!endElement.value && durationElement.value) {
          startElement.setCustomValidity("required");
          endElement.setCustomValidity("required");
          errorContainer.innerHTML = `<ul><li>${i18n(
            "time-entry.validation.startOrEnd.required",
          )}</li></ul>`;
        } else {
          startElement.setCustomValidity("required");
          durationElement.setCustomValidity("required");
          errorContainer.innerHTML = `<ul><li>${i18n(
            "time-entry.validation.startOrDuration.required",
          )}</li></ul>`;
        }
      }
    });

    endElement.addEventListener("blur", () => {
      if (!this.#hasBeenTriedToSubmitAtLeastOnce) {
        return;
      }
      if (endElement.value) {
        endElement.setCustomValidity("");
        if (!startElement.value && !durationElement.value) {
          errorContainer.innerHTML = `<ul><li>${i18n(
            "time-entry.validation.startOrDuration.required",
          )}</li></ul>`;
        } else {
          errorContainer.innerHTML = ``;
          durationElement.setCustomValidity("");
        }
      } else {
        if (startElement.value && durationElement.value) {
          endElement.setCustomValidity("");
          errorContainer.innerHTML = ``;
        } else if (!startElement.value && !durationElement.value) {
          endElement.setCustomValidity("required");
          durationElement.setCustomValidity("required");
          errorContainer.innerHTML = ``;
        } else if (!startElement.value && durationElement.value) {
          endElement.setCustomValidity("required");
          startElement.setCustomValidity("required");
          errorContainer.innerHTML = `<ul><li>${i18n(
            "time-entry.validation.startOrEnd.required",
          )}</li></ul>`;
        } else {
          endElement.setCustomValidity("required");
          durationElement.setCustomValidity("required");
          errorContainer.innerHTML = `<ul><li>${i18n(
            "time-entry.validation.endOrDuration.required",
          )}</li></ul>`;
        }
      }
    });

    durationElement.addEventListener("blur", () => {
      if (durationElement.value && !/\d\d:\d\d/.test(durationElement.value)) {
        durationElement.setCustomValidity("pattern");
        errorContainer.innerHTML = `<ul><li>${i18n(
          "time-entry.validation.duration.pattern",
        )}</li></ul>`;
        return;
      } else if (!this.#hasBeenTriedToSubmitAtLeastOnce) {
        durationElement.setCustomValidity("");
        errorContainer.innerHTML = ``;
        return;
      }

      if (!this.#hasBeenTriedToSubmitAtLeastOnce) {
        return;
      }

      if (durationElement.value) {
        durationElement.setCustomValidity("");
        if (!startElement.value && !endElement.value) {
          errorContainer.innerHTML = `<ul><li>${i18n(
            "time-entry.validation.startOrEnd.required",
          )}</li></ul>`;
        } else {
          errorContainer.innerHTML = ``;
          startElement.setCustomValidity("");
          endElement.setCustomValidity("");
        }
      } else {
        if (startElement.value && endElement.value) {
          durationElement.setCustomValidity("");
          errorContainer.innerHTML = ``;
        } else if (!startElement.value && !endElement.value) {
          durationElement.setCustomValidity("required");
          errorContainer.innerHTML = ``;
        } else if (!startElement.value && endElement.value) {
          durationElement.setCustomValidity("required");
          startElement.setCustomValidity("required");
          errorContainer.innerHTML = `<ul><li>${i18n(
            "time-entry.validation.startOrDuration.required",
          )}</li></ul>`;
        } else {
          durationElement.setCustomValidity("required");
          endElement.setCustomValidity("required");
          errorContainer.innerHTML = `<ul><li>${i18n(
            "time-entry.validation.endOrDuration.required",
          )}</li></ul>`;
        }
      }
    });
  }

  #validate(): boolean {
    const errorContainer = this.querySelector(
      "[data-error-container]",
    ) as HTMLElement;
    const startElement = this.querySelector(
      "input[name='start']",
    ) as HTMLInputElement;
    const endElement = this.querySelector(
      "input[name='end']",
    ) as HTMLInputElement;
    const durationElement = this.querySelector(
      "input[name='duration']",
    ) as HTMLInputElement;

    let valid = true;
    let errorMessage = "";

    if (!startElement.value && !endElement.value && !durationElement.value) {
      startElement.setCustomValidity("required");
      endElement.setCustomValidity("required");
      durationElement.setCustomValidity("required");
      valid = false;
    } else if (
      startElement.value &&
      !endElement.value &&
      !durationElement.value
    ) {
      endElement.setCustomValidity("required");
      durationElement.setCustomValidity("required");
      errorMessage = i18n("time-entry.validation.endOrDuration.required");
      valid = false;
    } else if (
      !startElement.value &&
      endElement.value &&
      !durationElement.value
    ) {
      startElement.setCustomValidity("required");
      durationElement.setCustomValidity("required");
      errorMessage = i18n("time-entry.validation.startOrDuration.required");
      valid = false;
    } else if (
      !startElement.value &&
      !endElement.value &&
      durationElement.value
    ) {
      startElement.setCustomValidity("required");
      endElement.setCustomValidity("required");
      errorMessage = i18n("time-entry.validation.startOrEnd.required");
      valid = false;
    }

    if (valid && this.#overlapsExistingEntry()) {
      errorMessage = i18n("time-entry.validation.timespan.overlaps");
      valid = false;
    }

    if (errorMessage) {
      errorContainer.innerHTML = `<ul><li>${errorMessage}</li></ul>`;
    }

    return valid;
  }

  #overlapsExistingEntry(): boolean {
    const ownDate = getFormDateIso(this);
    const ownTimespan = getFormTimespan(this);

    if (!ownDate || !ownTimespan) {
      // required-fields check already covers this. nothing to compare here.
      return false;
    }

    const ownIsBreak = getFormIsBreak(this);

    const otherForms = document.querySelectorAll(
      "form[is='z-time-entry-slot-form']",
    );

    for (const otherForm of otherForms) {
      if (otherForm === this) {
        continue;
      }
      if (getFormDateIso(otherForm) !== ownDate) {
        continue;
      }
      if (getFormIsBreak(otherForm) !== ownIsBreak) {
        continue;
      }
      const otherTimespan = getFormTimespan(otherForm);
      if (!otherTimespan) {
        continue;
      }
      if (
        otherTimespan.startMinutes < ownTimespan.endMinutes &&
        otherTimespan.endMinutes > ownTimespan.startMinutes
      ) {
        return true;
      }
    }

    return false;
  }
}

customElements.define("z-time-entry-slot-form", TimeEntrySlotForm, {
  extends: "form",
});
