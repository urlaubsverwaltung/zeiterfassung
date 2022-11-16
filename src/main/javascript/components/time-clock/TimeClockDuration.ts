export class TimeClockDuration extends HTMLSpanElement {
  #startedAtMillis: number;

  connectedCallback() {
    this.#startedAtMillis = new Date(this.dataset.startedAt).getTime();
    setInterval(() => this.render(), 0);

    this.classList.add("tabular-nums");
  }

  render() {
    const elapseMillis = Date.now() - this.#startedAtMillis;

    const seconds = Math.floor((elapseMillis / 1000) % 60);
    const minutes = Math.floor((elapseMillis / (1000 * 60)) % 60);
    const hours = Math.floor(elapseMillis / (1000 * 60 * 60));

    const hh = hours < 10 ? "0" + hours : hours;
    const mm = minutes < 10 ? "0" + minutes : minutes;
    const ss = seconds < 10 ? "0" + seconds : seconds;

    this.textContent = `${hh}:${mm}:${ss}`;
  }
}

customElements.define("z-time-clock-duration", TimeClockDuration, {
  extends: "span",
});
