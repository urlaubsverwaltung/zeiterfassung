export function bodyOverlay() {
  const bodyOverlay = document.querySelector("#body-overlay") as HTMLDivElement;

  bodyOverlay.classList.add("visible");

  return bodyOverlay;
}

export function removeBodyOverlay(bodyOverlay: HTMLElement): Promise<void> {
  return new Promise((resolve) => {
    bodyOverlay.addEventListener(
      "animationend",
      function () {
        resolve();
      },
      { once: true },
    );
    setTimeout(() => {
      bodyOverlay.classList.remove("visible");
    });
  });
}
