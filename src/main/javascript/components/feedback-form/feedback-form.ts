import { bodyOverlay, removeBodyOverlay } from "../body-overlay";

const feedbackElement: HTMLDivElement = document.querySelector("#feedback");
const toggleElement: HTMLButtonElement = document.querySelector(
  "#feedback-form-toggle-button",
);
const formElement: HTMLFormElement = document.querySelector("#feedback-form");
const closeElement: HTMLButtonElement = document.querySelector(
  "#feedback-form-close-button",
);
const cancelButton: HTMLButtonElement = document.querySelector(
  "#feedback-form-cancel-button",
);

let overlay;

toggleElement.addEventListener("click", function () {
  if (formElement.classList.contains("feedback-form--visible")) {
    hideForm();
  } else {
    showForm();
  }
});

closeElement.addEventListener("click", function () {
  hideForm();
});

cancelButton.addEventListener("click", function () {
  hideForm();
});

function showForm() {
  formElement.classList.add("feedback-form--visible");
  document.body.classList.add("overflow-hidden");
  overlay = bodyOverlay();
  feedbackElement.before(overlay);
}

async function hideForm() {
  formElement.classList.remove("feedback-form--visible");
  document.body.classList.remove("overflow-hidden");
  await removeBodyOverlay(overlay);
  formElement.reset();
}

export {};
