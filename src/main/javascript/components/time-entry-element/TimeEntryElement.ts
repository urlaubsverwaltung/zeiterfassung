export class TimeEntryElement extends HTMLDivElement {
  connectedCallback() {
    this.#initForm();
  }

  #initForm() {
    const form = this.querySelector("form");

    const originalFormData = new FormData(form);

    // name=date is not in the formData because of customElement DuetDatePicker.
    // I think it is as soon as DuetDatePicker implements FormAssociated (https://github.com/ionic-team/stencil/issues/2284)
    const duetDatePickerElement: HTMLDuetDatePickerElement =
      this.querySelector("duet-date-picker");

    // due-date-picker does not exist yet on dynamically added elements.
    if (duetDatePickerElement) {
      originalFormData.append(
        duetDatePickerElement.name,
        duetDatePickerElement.value,
      );
    }
    // else: field is in formData

    this.addEventListener("duetChange", (event: CustomEvent) => {
      const datepicker = event.target as HTMLDuetDatePickerElement;
      // duetChange event is dispatched before form data is updated. therefore, put handling to the end of event loop.
      setTimeout(() => {
        handleValueChanged(datepicker.name, event.detail.value, datepicker);
      });
    });

    form.addEventListener("change", (event: FocusEvent) => {
      const target: HTMLInputElement = event.target as HTMLInputElement;
      if (target.matches("input[type='checkbox']")) {
        handleValueChanged(target.name, target.checked, target);
      } else if (target.matches("input")) {
        handleValueChanged(target.name, target.value, target);
      }
    });

    form.addEventListener("reset", () => {
      for (const element of form.querySelectorAll(".edited")) {
        element.classList.remove("edited");
      }
    });

    function handleValueChanged(
      fieldName: string,
      newValue: string | boolean,
      inputElement: HTMLElement,
    ) {
      const nextFormData = new FormData(form);
      const timeslot = form.querySelector(".timeslot-form-existing");
      if (originalFormData.get(fieldName) === nextFormData.get(fieldName)) {
        inputElement.classList.remove("edited");
        if (!isFormChanged()) {
          timeslot.classList.remove("edited");
        }
      } else {
        timeslot.classList.add("edited");
        inputElement.classList.add("edited");
      }
    }

    function isFormChanged() {
      const currentFormData: FormData = new FormData(form);
      return ![...currentFormData.entries()].every((entry) => {
        return (
          originalFormData.has(entry[0]) &&
          originalFormData.get(entry[0]) === entry[1]
        );
      });
    }
  }
}

customElements.define("z-time-entry-element", TimeEntryElement, {
  extends: "div",
});
