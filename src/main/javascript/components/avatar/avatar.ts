import { doGet } from "../../http";
import { Tooltip } from "../tooltip";

export class Avatar extends HTMLImageElement {
  connectedCallback() {
    const altText = this.getAttribute("alt");

    if (this.complete) {
      doGet(this.src).then((response) => {
        if (!response.ok || (response.status >= 400 && response.status < 500)) {
          this.#useFallback().finally(() => this.addTooltip(altText));
        }
      });
    } else {
      this.addEventListener("error", () =>
        this.#useFallback().finally(() => this.addTooltip(altText)),
      );
    }

    // add tooltip for the img element.
    // fallback replaces the img element with a svg and finally adds the tooltip again.
    this.addTooltip(altText);
  }

  async #useFallback() {
    const response = await doGet<string>(this.dataset.fallback);

    const width = this.getAttribute("width");
    const height = this.getAttribute("height");

    const t = document.createElement("template");
    t.innerHTML = response.data;

    const svgElement = t.content.querySelector("svg");
    svgElement.setAttribute("width", width);
    svgElement.setAttribute("height", height);

    const clazzes = [...this.classList.entries()].map(([, clazz]) => clazz);

    const parent = this.parentElement;
    parent.replaceChild(t.content, this);
    parent.querySelector("svg").classList.add(...clazzes, "cursor-default");
  }

  private addTooltip(altText: string) {
    if (altText) {
      const tooltipText = document.createElement("p");
      tooltipText.textContent = altText;

      const tooltip = new Tooltip();
      tooltip.append(tooltipText);

      this.parentElement.append(tooltip);
    }
  }
}

customElements.define("z-avatar", Avatar, { extends: "img" });
