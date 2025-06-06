export type TurboFetchRequest = {
  body: FormData | URLSearchParams;
  enctype:
    | "application/x-www-form-urlencoded"
    | "multipart/form-date"
    | "text/plain";
  fetchOptions: RequestInit;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  headers: Headers | Record<string, any>;
  method: "get" | "post"; // we're using GET and POST only
  params: URLSearchParams;
  target: HTMLFormElement | HTMLAnchorElement | null;
  url: URL;
};

export type TurboFetchResponse = {
  clientError: boolean;
  contentType: string | null;
  failed: boolean;
  isHTML: boolean;
  location: URL;
  redirected: boolean;
  responseHTML: Promise<string>;
  responseText: Promise<string>;
  response: Response;
  serverError: boolean;
  statusCode: number;
  succeeded: boolean;
};

type FetchRequestEventDetail = {
  request: TurboFetchRequest;
  error: Error;
};

export function onTurboFetchRequestError(
  callback: (event: CustomEvent<FetchRequestEventDetail>) => void,
  options?: AddEventListenerOptions,
) {
  document.addEventListener("turbo:fetch-request-error", callback, options);
}
