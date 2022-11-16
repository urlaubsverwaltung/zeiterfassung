export class CheckboxAllOption extends HTMLInputElement {
  connectedCallback() {
    this.addEventListener("change", (event) => {
      const elements = [
        ...document.querySelectorAll(
          `input[type='checkbox'][name='${this.dataset.name}']`,
        ),
      ] as HTMLInputElement[];
      const checked = (event.target as HTMLInputElement).checked;
      for (const input of elements) {
        input.checked = checked;
      }
    });

    document.addEventListener("change", (event) => {
      const target = event.target as HTMLInputElement;
      if (
        target.tagName === "INPUT" &&
        target.getAttribute("name") === this.dataset.name &&
        !target.checked
      ) {
        this.checked = false;
      }
    });
  }
}

customElements.define("z-checkbox-all-option", CheckboxAllOption, {
  extends: "input",
});
