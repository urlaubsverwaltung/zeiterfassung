import { animationInterval } from "./animation-interval";

export class TimeClockDuration extends HTMLSpanElement {
  #controller: AbortController;

  connectedCallback() {
    this.#controller = new AbortController();

    const start = Date.now() - new Date(this.dataset.startedAt).getTime();

    animationInterval(1000, this.#controller.signal, (time) => {
      this.render(start + time);
    });

    this.classList.add("tabular-nums");
  }

  disconnectedCallback() {
    this.#controller.abort();
  }

  render(time) {
    const seconds = Math.floor((time / 1000) % 60);
    const minutes = Math.floor((time / (1000 * 60)) % 60);
    const hours = Math.floor(time / (1000 * 60 * 60));

    const hh = hours < 10 ? "0" + hours : hours;
    const mm = minutes < 10 ? "0" + minutes : minutes;
    const ss = seconds < 10 ? "0" + seconds : seconds;

    this.textContent = `${hh}:${mm}:${ss}`;
  }
}

customElements.define("z-time-clock-duration", TimeClockDuration, {
  extends: "span",
});
