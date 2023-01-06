import { post } from "../../http";
import { addNotification } from "../notification";
import { i18n } from "../../i18n";

class TimeEntryElement extends HTMLDivElement {
  connectedCallback() {
    this.#initForm();
    this.#initDeleteButton();
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

    form.addEventListener("focusout", (event: FocusEvent) => {
      const target: HTMLInputElement = event.target as HTMLInputElement;
      if (target.tagName === "INPUT") {
        handleValueChanged(target.name, target.value, target);
      }
    });

    function handleValueChanged(
      name: string,
      newValue: string,
      element: HTMLElement,
    ) {
      if (originalFormData.get(name) === newValue) {
        element.classList.remove("edited");
        if (!isFormChanged()) {
          form.classList.remove("edited");
        }
      } else {
        form.classList.add("edited");
        element.classList.add("edited");
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

  #initDeleteButton() {
    const deleteButton = this.querySelector("button[name='delete']");

    deleteButton.addEventListener("click", async (event: SubmitEvent) => {
      this.classList.add("transition-all", "duration-500", "overflow-hidden");
      event.preventDefault();

      const form = deleteButton.closest("form");
      const formData = new FormData(form);
      formData.append(deleteButton.getAttribute("name"), "");

      const { height } = this.getBoundingClientRect();
      this.style.height = `${height}px`;

      const response = await post(form.action, formData);

      if (response.ok && response.status >= 200 && response.status < 300) {
        this.addEventListener("transitionend", () => {
          setTimeout(() => {
            this.remove();
          }, 10);
        });
        this.style.height = `0`;
        this.#showDeletedNotification();
      }
    });
  }

  #showDeletedNotification() {
    const { date } = this.dataset;
    const commentElement: HTMLInputElement =
      this.querySelector("[name='comment']");
    const durationElement: HTMLInputElement =
      this.querySelector("[name='duration']");

    addNotification({
      autoClose: true,
      title: "Zeitslot wurde gelöscht.",
      iconHtml: `
        <svg class="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
        </svg>
      `,
      bodyHtml: `
        <dl class="grid gap-x-2 gap-y-1" style="grid-template-columns: max-content 1fr;">
          <dt class="text-gray-400">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
            <span class="sr-only">
              ${i18n("time-entry.date.label")}
            </span>
          </dt>
          <dd>
            ${date}
          </dd>
          <dt class="text-gray-400">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            <span class="sr-only">
              ${i18n("time-entry.duration.label")}
            </span>
          </dt>
          <dd>
            ${durationElement.value}
          </dd>
          <dt class="text-gray-400">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z"></path></svg>
            <span class="sr-only">
              ${i18n("time-entry.comment.label")}
            </span>
          </dt>
          <dd class="truncate">
            ${commentElement.value}
          </dd>
        </dl>
      `,
    });
  }
}

customElements.define("z-time-entry-element", TimeEntryElement, {
  extends: "div",
});
