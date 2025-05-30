document.addEventListener("input", function (event) {
  const target = event.target as HTMLElement;

  if (target.matches("#lock-timeentries-days-in-past-input")) {
    // update example text of lock timeentries date
    //
    const form = target.closest("form");

    const button = document.createElement("button");
    button.setAttribute("type", "submit");
    button.setAttribute("preview", "");
    button.style.display = "none";
    form.append(button);

    form.requestSubmit(button);
  }
});
