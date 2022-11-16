export function bodyOverlay() {
  const bodyOverlay = document.createElement("div");

  bodyOverlay.style.setProperty("--animation-duration", "300ms");
  bodyOverlay.classList.add("body-overlay", "animation-fade-in");

  return bodyOverlay;
}

export function removeBodyOverlay(bodyOverlay: HTMLElement): Promise<void> {
  return new Promise((resolve) => {
    bodyOverlay.addEventListener("animationend", function () {
      bodyOverlay.remove();
      resolve();
    });
    bodyOverlay.classList.remove("animation-fade-in");
    setTimeout(() => {
      bodyOverlay.classList.add("animation-fade-out");
    });
  });
}
