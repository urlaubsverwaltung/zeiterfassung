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

toggleElement?.addEventListener("click", function () {
  if (formElement.classList.contains("feedback-form--visible")) {
    hideForm();
  } else {
    showForm();
  }
});

closeElement?.addEventListener("click", function () {
  hideForm();
});

cancelButton?.addEventListener("click", function () {
  hideForm();
});

function showForm() {
  formElement.classList.add("feedback-form--visible");
}

function hideForm() {
  formElement.classList.remove("feedback-form--visible");
  formElement.reset();
}

export {};
