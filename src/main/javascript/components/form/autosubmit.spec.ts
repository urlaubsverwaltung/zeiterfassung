import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  test,
} from "vitest";
import { initAutosubmit } from "./autosubmit";

describe("autosubmit", () => {
  beforeAll(() => {
    initAutosubmit();
  });

  beforeEach(() => {
    // prevent HTMLFormElement.prototype.requestSubmit is not implemented log.
    //
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-expect-error
    window._virtualConsole.emit = () => {};
  });

  afterEach(() => {
    while (document.body.firstChild) {
      document.body.firstChild.remove();
    }
  });

  test("auto-submits text input", async () => {
    document.body.innerHTML = `
      <form action="#">
        <input type="text" data-auto-submit="submitter" />
        <button type="submit" id="submitter">Submit</button>
      </form>
    `;

    let submitter;

    document.querySelector("form").addEventListener("submit", function (event) {
      submitter = event.submitter;
    });

    const inputElement = document.querySelector("input");
    inputElement.value = "awesome text";
    inputElement.dispatchEvent(new InputEvent("input", { bubbles: true }));

    await wait();

    expect(submitter).toBe(document.querySelector("button"));
  });

  test("auto-submits text input with custom delay", async () => {
    document.body.innerHTML = `
      <form action="#">
        <input type="text" data-auto-submit="submitter" data-auto-submit-delay="100" />
        <button type="submit" id="submitter">Submit</button>
      </form>
    `;

    let submitter;

    document.querySelector("form").addEventListener("submit", function (event) {
      submitter = event.submitter;
    });

    const inputElement = document.querySelector("input");
    inputElement.value = "awesome text";
    inputElement.dispatchEvent(new InputEvent("input", { bubbles: true }));

    await wait();
    expect(submitter).toBeUndefined();

    await wait(100);
    expect(submitter).toBe(document.querySelector("button"));
  });
});

function wait(delay = 0) {
  return new Promise((resolve) => setTimeout(resolve, delay));
}
