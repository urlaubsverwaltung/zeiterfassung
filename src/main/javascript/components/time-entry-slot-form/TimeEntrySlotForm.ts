import { i18n } from "../../i18n";

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

    const errorContainer: HTMLElement = this.querySelector(
      "[data-error-container]",
    );
    const startElement: HTMLInputElement = this.querySelector(
      "input[name='start']",
    );
    const endElement: HTMLInputElement =
      this.querySelector("input[name='end']");
    const durationElement: HTMLInputElement = this.querySelector(
      "input[name='duration']",
    );

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
    const errorContainer: HTMLElement = this.querySelector(
      "[data-error-container]",
    );
    const startElement: HTMLInputElement = this.querySelector(
      "input[name='start']",
    );
    const endElement: HTMLInputElement =
      this.querySelector("input[name='end']");
    const durationElement: HTMLInputElement = this.querySelector(
      "input[name='duration']",
    );

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

    if (errorMessage) {
      errorContainer.innerHTML = `<ul><li>${errorMessage}</li></ul>`;
    }

    return valid;
  }
}

customElements.define("z-time-entry-slot-form", TimeEntrySlotForm, {
  extends: "form",
});
