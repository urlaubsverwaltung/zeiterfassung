export class FeedbackHeart extends HTMLElement {
  connectedCallback() {
    this.render();
  }

  connectedMoveCallback() {
    // prevent connectedCallback to be called when element is moved.
    // (actually not of interest yet, since we have only one heart at the bottom left which is not moved)
  }

  unleash() {
    const heart = document.createElement("div");
    heart.classList.add("feedback-floating-heart");
    document.body.append(heart);
  }

  private render() {
    const button = document.createElement("button");
    button.setAttribute("type", "button");
    button.classList.add("block", "cursor-pointer");
    button.style.anchorName = "--feedback-heart-anchor";
    button.addEventListener("click", this.unleash.bind(this));
    button.innerHTML = `
      <svg class="w-6 h-6" width="16px" height="16px" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
        ></path>
      </svg>
    `;

    this.append(button);
  }
}

customElements.define("z-feedback-heart", FeedbackHeart);
