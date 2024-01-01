document.addEventListener("click", (event) => {
  const button = (event.target as HTMLElement).closest("[data-toggle-button]");
  if (button) {
    toggleButton(button as HTMLButtonElement);
  }
});

document.body.addEventListener("click", (event) => {
  if ((event.target as HTMLElement).closest("[data-toggle-button]")) {
    // button clicked -> do nothing
    return;
  }
  const button = document.querySelector(
    "[data-toggle-button][aria-pressed=true]",
  );
  if (button && button.matches("[data-reset-on-body-click]")) {
    toggleButton(button as HTMLButtonElement);
  }
});

function toggleButton(button: HTMLButtonElement) {
  const currentlyPressed = button.getAttribute("aria-pressed") === "true";
  button.setAttribute("aria-pressed", currentlyPressed ? "false" : "true");
}
