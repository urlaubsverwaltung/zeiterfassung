export class Sticky extends HTMLElement {
  static readonly observedAttributes = ["data-sticky"];

  private readonly intersectionObserver: IntersectionObserver;

  constructor() {
    super();

    // set by JavaScript since we only know whether sticky or not with this IntersectionOberserver
    this.style.position = "sticky";

    // kudos https://css-tricks.com/how-to-detect-when-a-sticky-element-gets-pinned/
    this.intersectionObserver = new IntersectionObserver(
      ([event]) => {
        const sticky = event.intersectionRatio < 1;
        event.target.classList.toggle("z-sticky--stuck", sticky);
      },
      { threshold: [1] },
    );
  }

  attributeChangedCallback(name: string, oldValue: string, newValue: string) {
    if (name === "data-sticky") {
      if (newValue === "bottom") {
        this.style.bottom = "-1px";
        this.style.top = "";
      } else if (newValue === "top") {
        this.style.top = "-1px";
        this.style.bottom = "";
      } else {
        this.style.top = "";
        this.style.bottom = "";
      }
    }
  }

  connectedCallback() {
    this.intersectionObserver.observe(this);
  }

  disconnectedCallback() {
    this.intersectionObserver.unobserve(this);
  }

  connectedMoveCallback() {
    // prevent calling connectedCallback when element is moved
  }
}

customElements.define("z-sticky", Sticky);
