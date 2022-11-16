export class NavPopupMenuButton extends HTMLAnchorElement {
  static get observedAttributes() {
    return ["data-open"];
  }

  #open = false;

  attributeChangedCallback(name, oldValue, newValue) {
    if (oldValue === newValue) {
      return;
    }

    this.#open = typeof newValue === "string";

    const menu = document.querySelector(this.getAttribute("href"));
    if (this.#open) {
      menu.classList.add("visible");
    } else {
      menu.classList.remove("visible");
    }
  }

  connectedCallback() {
    this.addEventListener("click", (event) => {
      event.preventDefault();
      const menu = document.querySelector(this.getAttribute("href"));

      for (const otherMenu of document.querySelectorAll(
        ".nav-popup-menu[data-open]",
      )) {
        if (otherMenu !== menu) {
          // only one menu should be opened
          delete (otherMenu as HTMLElement).dataset.open;
        }
      }

      if (this.#open) {
        delete this.dataset.open;
        this.#open = false;
      } else {
        this.dataset.open = "";
        this.#open = true;
      }
    });
  }
}

// hide other popups
document.body.addEventListener("click", function (event) {
  const closestClickedDatePicker = (event.target as HTMLElement).closest(
    "[is='z-nav-popup-menu-button']",
  );
  for (const picker of document.querySelectorAll(
    "[is='z-nav-popup-menu-button'][data-open]",
  )) {
    if (picker !== closestClickedDatePicker) {
      delete (picker as HTMLElement).dataset.open;
    }
  }
});

customElements.define("z-nav-popup-menu-button", NavPopupMenuButton, {
  extends: "a",
});
