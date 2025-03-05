import { createPopper, type Instance } from "@popperjs/core";

const showEvents = ["mouseenter", "focus"];
const hideEvents = ["mouseleave", "blur"];

export class Tooltip extends HTMLDivElement {
  private popperInstance: Instance;

  constructor() {
    super();
    this.setAttribute("role", "tooltip");
  }

  connectedCallback() {
    // don't know yet if this is cool or not using `this.parentElement` as popper reference element
    const parent = this.parentElement;

    this.popperInstance = createPopper(parent, this);

    for (const event of showEvents) {
      parent.addEventListener(event, () => {
        this.dataset.show = "";
        this.popperInstance.update();
        this.classList.remove("hidden");
      });
    }

    for (const event of hideEvents) {
      parent.addEventListener(event, () => {
        delete this.dataset.show;
      });
    }
  }
}

customElements.define("z-tooltip", Tooltip, { extends: "div" });
