export {};

document.addEventListener("turbo:frame-render", function (event: CustomEvent) {
  const target = event.target as Element;
  if (target.id === "frame-nav-time-clock-edit") {
    const form = target.querySelector("form");
    form.closest("details").open = form.dataset.hasErrors === "true";
  }
});
