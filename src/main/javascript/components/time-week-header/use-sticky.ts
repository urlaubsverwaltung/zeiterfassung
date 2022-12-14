export function useSticky(element: HTMLElement) {
  let listeners = [];

  const div = document.createElement("div");
  div.style.display = "hidden";
  element.before(div);

  const { height } = element.getBoundingClientRect();

  const observer = new IntersectionObserver(
    ([entry]) => {
      // element is outside the viewport, intercepts the bottom.
      // as we're only handling sticky-top elements -> this element is not be sticky anymore
      const outOfScreen =
        entry.target.getBoundingClientRect().bottom > window.innerHeight;

      for (const listener of listeners) {
        listener({
          element: entry.target,
          sticky: entry.intersectionRatio < 1 && !outOfScreen,
        });
      }
    },
    {
      threshold: [0, 1],
      rootMargin: `-${height + 1}px 0px 0px 0px`,
    },
  );

  return {
    subscribe(callback) {
      listeners.push(callback);
      if (listeners.length === 1) {
        observer.observe(element);
      }

      return () => {
        listeners = listeners.filter((listener) => listener !== callback);
        if (listeners.length === 0) {
          observer.disconnect();
        }
      };
    },
  };
}
