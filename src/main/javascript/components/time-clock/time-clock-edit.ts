import { onTurboFrameRender } from "../../turbo-events";

onTurboFrameRender(function (event) {
  const target = event.target as Element;
  if (target.id === "frame-nav-time-clock-edit") {
    const form = target.querySelector("form");
    form.closest("details").open = form.dataset.hasErrors === "true";
  }
});
