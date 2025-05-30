import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  test,
} from "vitest";
import { initPreventDoubleClickSubmit } from "./prevent-double-click-submit";

describe("DoubleClickSubmitGuard", () => {
  beforeAll(() => {
    initPreventDoubleClickSubmit();
  });

  beforeEach(() => {
    // prevent HTMLFormElement.prototype.requestSubmit is not implemented log.
    globalThis._virtualConsole.emit = () => {};
  });

  afterEach(() => {
    while (document.body.firstChild) {
      document.body.firstChild.remove();
    }
  });

  test("disables submitter on form submit", async () => {
    document.body.innerHTML = `
      <form action="#">
        <button type="submit">Submit</button>
      </form>
    `;

    const button = document.querySelector("button");

    expect(button.getAttribute("disabled")).toBeNull();

    button.click();

    // attribute is added after end of event loop
    // to avoid not sending button attributes in form post
    await new Promise((resolve) => setTimeout(resolve));

    expect(button.getAttribute("disabled")).toBe("");
  });

  test("does not disable submitter on form submit when defaultPrevented", () => {
    document.body.innerHTML = `
      <form action="#">
        <button type="submit">Submit</button>
      </form>
    `;

    let called = false;

    document.querySelector("form").addEventListener("submit", function (event) {
      event.preventDefault();
      called = true;
    });

    const button = document.querySelector("button");

    button.click();

    expect(button.getAttribute("disabled")).toBeNull();
    expect(called).toBe(true);
  });
});
