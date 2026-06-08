export class TimeEntryElement extends HTMLDivElement {
  connectedCallback() {
    this.#initForm();
    this.#initCompactRow();
  }

  #initForm() {
    const form = this.querySelector("form")!;

    const originalFormData = new FormData(form);

    // name=date is not in the formData because of customElement DuetDatePicker.
    // I think it is as soon as DuetDatePicker implements FormAssociated (https://github.com/ionic-team/stencil/issues/2284)
    const duetDatePickerElement = this.querySelector(
      "duet-date-picker",
    ) as HTMLDuetDatePickerElement;

    // due-date-picker does not exist yet on dynamically added elements.
    if (duetDatePickerElement) {
      originalFormData.append(
        duetDatePickerElement.name,
        duetDatePickerElement.value,
      );
    }
    // else: field is in formData

    this.addEventListener("duetChange", (event) => {
      const datepicker = event.target as HTMLDuetDatePickerElement;
      // duetChange event is dispatched before form data is updated. therefore, put handling to the end of event loop.
      setTimeout(() => {
        // @ts-expect-error yep...
        handleValueChanged(datepicker.name, event.detail.value, datepicker);
      });
    });

    form.addEventListener("change", (event: Event) => {
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
      delete this.dataset.open;
    });

    function handleValueChanged(
      fieldName: string,
      newValue: string | boolean,
      inputElement: HTMLElement,
    ) {
      const nextFormData = new FormData(form);
      const timeslot = form.querySelector(".timeslot-form")!;
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

  #initCompactRow() {
    const readRow = this.querySelector<HTMLElement>("[data-read-row]");
    if (!readRow) return;

    this.#populateReadRow();

    readRow.addEventListener("click", () => {
      this.dataset.open = "";
    });

    readRow.addEventListener("keydown", (event_: KeyboardEvent) => {
      if (event_.key === "Enter" || event_.key === " ") {
        event_.preventDefault();
        this.dataset.open = "";
      }
    });
  }

  #populateReadRow() {
    const readRow = this.querySelector("[data-read-row]");
    if (!readRow) return;

    const form = this.querySelector("form")!;
    const projectSelect = form.querySelector<HTMLSelectElement>(
      "select[name='projectId']",
    );
    const activitySelect = form.querySelector<HTMLSelectElement>(
      "select[name='activityTypeId']",
    );

    const selectedProjectOption =
      projectSelect && projectSelect.selectedIndex > 0
        ? projectSelect.options[projectSelect.selectedIndex]
        : undefined;

    const customerName = selectedProjectOption?.dataset.customerName;
    const projectName = selectedProjectOption?.text;
    const activityName =
      activitySelect && activitySelect.selectedIndex > 0
        ? activitySelect.options[activitySelect.selectedIndex].text
        : undefined;

    const projectElement = readRow.querySelector("[data-read-project]");
    if (projectElement) {
      const parts = [customerName, projectName, activityName].filter(Boolean);
      projectElement.textContent = parts.length > 0 ? parts.join(" / ") : "";
    }
  }
}

customElements.define("z-time-entry-element", TimeEntryElement, {
  extends: "div",
});
