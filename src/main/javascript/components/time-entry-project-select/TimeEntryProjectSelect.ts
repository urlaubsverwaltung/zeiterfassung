/**
 * Customized built-in <div> that wraps the Project select in the time-entry form.
 *
 * Markup contract (set by Thymeleaf):
 *   <div is="z-time-entry-project-select">
 *     <select name="projectId" data-project-select>
 *       <option value="">— No project —</option>
 *       <option value="1" data-customer-id="10" data-customer-name="Acme Corp">Widget</option>
 *       ...
 *     </select>
 *     <label data-project-label>Project</label>
 *   </div>
 *
 * When more than one customer is present the component reorganises the flat
 * option list into <optgroup> elements (one per customer) so that same-named
 * projects from different customers are always unambiguous.
 * Single-customer setups are left as-is (no redundant grouping).
 */
class TimeEntryProjectSelect extends HTMLDivElement {
  connectedCallback() {
    const projectSelect = this.querySelector<HTMLSelectElement>(
      "select[data-project-select]",
    );
    if (!projectSelect) return;

    const blankOption = [...projectSelect.options].find((o) => o.value === "");
    const projectOptions = [...projectSelect.options].filter(
      (o) => o.value !== "",
    );

    // Build unique customer list in insertion order
    const seenIds = new Set<string>();
    const customers: { id: string; name: string }[] = [];
    for (const opt of projectOptions) {
      const id = opt.dataset.customerId ?? "";
      const name = opt.dataset.customerName ?? "";
      if (id && !seenIds.has(id)) {
        seenIds.add(id);
        customers.push({ id, name });
      }
    }

    // Nothing to group
    if (customers.length === 0) return;

    const selectedValue = projectSelect.value;

    // Rebuild the select with optgroups
    projectSelect.innerHTML = "";
    if (blankOption) projectSelect.append(blankOption);

    for (const customer of customers) {
      const group = document.createElement("optgroup");
      group.label = customer.name;
      for (const opt of projectOptions) {
        if (opt.dataset.customerId === customer.id) {
          group.append(opt);
        }
      }
      projectSelect.append(group);
    }

    // Restore selection (editing an existing entry)
    projectSelect.value = selectedValue;
  }
}

customElements.define("z-time-entry-project-select", TimeEntryProjectSelect, {
  extends: "div",
});
