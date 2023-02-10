export class ReportUserSelect extends HTMLAnchorElement {
  connectedCallback() {
    const href = this.getAttribute("href");
    const targetElement: HTMLElement = document.querySelector(href);

    this.setAttribute("aria-haspopup", "true");
    this.setAttribute("aria-expanded", "false");

    let expanded = false;

    targetElement.classList.add(
      "absolute",
      "top-full",
      "left-0",
      "shadow-xl",
      "rounded-lg",
      "bg-white",
      "p-6",
      "transition-all",
      "origin-top",
      "scale-y-0",
      "opacity-0",
      "overflow-hidden",
      "z-40",
      "border",
      "border-gray-100",
      "mt-1",
    );

    const hidePopup = () => {
      expanded = false;
      targetElement.classList.remove("scale-y-100", "opacity-100");
      targetElement.addEventListener(
        "transitionend",
        () => {
          targetElement.dataset.jsHidden = "";
        },
        { once: true },
      );
    };

    document.body.addEventListener("click", (event) => {
      const target = event.target as HTMLElement;
      if (
        expanded &&
        target !== this &&
        !this.contains(target) &&
        target !== targetElement &&
        !targetElement.contains(target)
      ) {
        hidePopup();
      }
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && expanded) {
        hidePopup();
      }
    });

    this.addEventListener("click", (event) => {
      event.preventDefault();

      expanded = !expanded;

      this.setAttribute("aria-expanded", expanded ? "true" : "false");

      if (expanded) {
        delete targetElement.dataset.jsHidden;
        setTimeout(() => {
          targetElement.classList.add("scale-y-100", "opacity-100");
        });
      } else {
        hidePopup();
      }
    });
  }
}

customElements.define("z-report-user-select", ReportUserSelect, {
  extends: "a",
});
