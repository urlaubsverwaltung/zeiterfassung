import "../components/checkbox-all-option";
import "../components/report-graph";
import "../components/report-breakdown-section";
import "../components/report-user-select";

// css subgrid does not work on <details> elements
// therefore we're using a div and doing this ourselves :/ without handling accessibility stuff currently...
// TODO using click results in opening the thing despite user has just selected text for instance. we should use mouseup instead.
document.body.addEventListener("click", (event) => {
  const target = event.target as HTMLElement;
  const summary = target.closest(".time-entry-history-list__element__summary");
  if (summary) {
    const parent = summary.parentElement;
    if (parent.hasAttribute("open")) {
      parent.removeAttribute("open");
    } else {
      parent.setAttribute("open", "");
    }
  }
});
