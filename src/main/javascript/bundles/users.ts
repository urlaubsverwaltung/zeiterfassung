import "../components/user-selection-list";
import "../components/form/autosubmit";

document.addEventListener("turbo:render", function () {
  const main = document.querySelector("main");

  const currentLink = document.querySelector(
    "a[aria-current='page']",
  ) as HTMLAnchorElement;

  if (currentLink && currentLink.matches("[data-turbo-frame='person-frame']")) {
    main.dataset.selectedUser = currentLink.dataset.user;
  } else {
    delete main.dataset.selectedUser;
  }
});
