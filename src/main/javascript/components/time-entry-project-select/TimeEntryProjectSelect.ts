/**
 * Customized built-in <div> that wraps the Customer + Project cascading selects
 * in the time-entry form.
 *
 * Markup contract (set by Thymeleaf):
 *   <div is="z-time-entry-project-select"
 *        data-customer-label="Customer"
 *        data-customer-none="— All customers —">
 *     <select name="projectId" data-project-select>
 *       <option value="">— No project —</option>
 *       <option value="1" data-customer-id="10" data-customer-name="Acme Corp">Widget Redesign</option>
 *       ...
 *     </select>
 *     <label data-project-label>Project</label>
 *   </div>
 *
 * The component inserts a Customer filter <select> above the project select.
 * Selecting a customer shows only that customer's project options.
 * The customer select is never submitted (no `name` attribute).
 * When editing an existing entry the component pre-selects the matching customer.
 */
class TimeEntryProjectSelect extends HTMLDivElement {
  connectedCallback() {
    const projectSelect = this.querySelector<HTMLSelectElement>(
      "select[data-project-select]",
    );
    if (!projectSelect) return;

    // Stamp data-customer-id / data-customer-name onto every project option
    // (they come from Thymeleaf th:attr, just ensure they're there)
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

    // Only add the filter when there is more than one customer
    if (customers.length === 0) return;

    const customerLabel = this.dataset.customerLabel ?? "Customer";
    const customerNone = this.dataset.customerNone ?? "— All customers —";

    // Build customer select (not submitted)
    const customerSelect = document.createElement("select");
    customerSelect.className = projectSelect.className;
    customerSelect.dataset.customerFilter = "";
    if (projectSelect.disabled) customerSelect.disabled = true;

    const blankOpt = document.createElement("option");
    blankOpt.value = "";
    blankOpt.textContent = customerNone;
    customerSelect.append(blankOpt);

    for (const c of customers) {
      const o = document.createElement("option");
      o.value = c.id;
      o.textContent = c.name;
      customerSelect.append(o);
    }

    // Build customer label element, mirroring the project label's classes
    const projectLabel = this.querySelector<HTMLElement>(
      "[data-project-label]",
    );
    const customerLabelElement = document.createElement("label");
    customerLabelElement.textContent = customerLabel;
    if (projectLabel) {
      customerLabelElement.className = projectLabel.className;
    }

    // Insert a new cell above (before) this component in the container
    const cell = document.createElement("div");
    cell.className = this.className; // same "time-entry-cell" classes
    cell.append(customerSelect);
    cell.append(customerLabelElement);
    this.insertAdjacentElement("beforebegin", cell);

    // --- Filtering logic ---
    const applyFilter = (customerId: string) => {
      for (const opt of projectOptions) {
        const matches = !customerId || opt.dataset.customerId === customerId;
        opt.hidden = !matches;
        opt.disabled = !matches;
      }
      // If the selected project no longer belongs to the chosen customer, clear it
      const currentOpt = projectOptions.find(
        (o) => o.value === projectSelect.value,
      );
      if (currentOpt && currentOpt.hidden) {
        projectSelect.value = "";
      }
    };

    // Pre-select customer when editing an existing entry
    const preselectedProject = projectOptions.find(
      (o) => o.value === projectSelect.value,
    );
    if (preselectedProject?.dataset.customerId) {
      customerSelect.value = preselectedProject.dataset.customerId;
      applyFilter(preselectedProject.dataset.customerId);
    }

    customerSelect.addEventListener("change", () => {
      applyFilter(customerSelect.value);
    });
  }
}

customElements.define("z-time-entry-project-select", TimeEntryProjectSelect, {
  extends: "div",
});
