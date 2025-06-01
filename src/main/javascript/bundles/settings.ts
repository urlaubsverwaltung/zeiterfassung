import * as Turbo from "@hotwired/turbo";
import { onTurboBeforeFrameRender } from "../turbo-events";
import { morphWithoutTouchingValueOfActiveElement } from "../morph";

// submit settings form in preview mode
// every time user does something, e.g. clicks checkbox or enters a char in a text field
document.addEventListener("input", function (event) {
  const target = event.target as HTMLElement;
  const settingsForm = target.closest("#settings-form");
  if (settingsForm) {
    submitSettingsFormPreview(event);
  }
});

let progressBarTimer;

function submitSettingsFormPreview(event: Event) {
  const target = event.target as HTMLElement;
  const form = target.closest("form");

  form.dataset.turbo = "true";

  const button = document.createElement("button");
  button.setAttribute("type", "submit");
  button.setAttribute("preview", "");
  button.style.display = "none";
  form.append(button);

  form.requestSubmit(button);
  showProgressBar();
}

// turbo re-renders the frame, however, the activeElement (e.g. a text input)
// will be re-rendered and therefore looses the focus. we could append the
// data-turbo-permanent attribute to enforce keeping it. this, however, results
// in not getting aria-invalid attribute updated. therefore -> morph it manually
onTurboBeforeFrameRender(function (event) {
  if (event.detail.newFrame.matches("#frame-settings-form")) {
    event.detail.render = function (currentFrame, newFrame) {
      morphWithoutTouchingValueOfActiveElement(currentFrame, newFrame);
      Turbo.navigator.delegate.adapter.progressBar.hide();
      hideProgressBar();
    };
  }
});

function showProgressBar() {
  if (!progressBarTimer) {
    progressBarTimer = setTimeout(function () {
      Turbo.navigator.delegate.adapter.progressBar.show();
    }, Turbo.config.drive.progressBarDelay);
  }
}

function hideProgressBar() {
  clearTimeout(progressBarTimer);
  progressBarTimer = undefined;
}
