export class TimeEntryDurationInput extends HTMLInputElement {
  #cleanup = () => {};

  connectedCallback() {
    // eslint-disable-next-line unicorn/consistent-function-scoping
    const handleInput = (event: InputEvent) => {
      if (!event.data) {
        // ignore meta keys
        return;
      }

      if (this.value.length === 2) {
        this.value += ":";
      } else if (this.value.length > 2) {
        if (this.value[2] !== ":") {
          // add `:` when it doesn't exist yet
          this.value = this.value.slice(0, 2) + ":" + this.value.slice(2);
        } else if (event.data === ":" && this.value.endsWith(":")) {
          // prevent multiple `:`
          this.value = this.value.slice(0, 3);
        }
      }
    };

    this.addEventListener("input", handleInput);

    this.#cleanup = () => {
      this.removeEventListener("input", handleInput);
    };
  }

  disconnectedCallback() {
    this.#cleanup();
  }
}

customElements.define("z-time-entry-duration-input", TimeEntryDurationInput, {
  extends: "input",
});
