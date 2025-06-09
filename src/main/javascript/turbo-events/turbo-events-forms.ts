type TurboFetchRequest = {
  body: FormData | URLSearchParams;
  enctype: "";
  fetchOptions: RequestInit;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  headers: Headers | Record<string, any>;
  method: "get" | "post"; // we're only using GET and POST
  params: URLSearchParams;
  target: HTMLFormElement | HTMLAnchorElement | null;
  url: URL;
};

type TurboFormSubmission = {
  action: string;
  body: FormData | URLSearchParams;
  enctype: "";
  fetchRequest: TurboFetchRequest;
  formElement: HTMLFormElement;
  isSafe: boolean;
  location: URL;
  method: "get" | "post"; // we're only using GET and POST
  submitter: HTMLButtonElement | HTMLInputElement | undefined;
};

type SubmitStartEventDetail = {
  formSubmission: TurboFormSubmission;
};

export function onTurboSubmitStart(
  callback: (event: CustomEvent<SubmitStartEventDetail>) => void,
  options?: AddEventListenerOptions,
) {
  document.addEventListener("turbo:submit-start", callback, options);
}
