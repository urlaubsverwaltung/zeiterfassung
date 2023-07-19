// kudos https://gist.github.com/jakearchibald/cb03f15670817001b1157e62a076fe95

export function animationInterval(
  ms: number,
  signal: AbortSignal,
  callback: (time: number) => void,
) {
  const start =
    (document?.timeline?.currentTime as number) || performance.now();

  function frame(time: number) {
    if (signal.aborted) return;
    callback(time);
    scheduleFrame(time);
  }

  function scheduleFrame(time: number) {
    const elapsed = time - start;
    const roundedElapsed = Math.round(elapsed / ms) * ms;
    const targetNext = start + roundedElapsed + ms;
    const delay = targetNext - performance.now();
    setTimeout(() => requestAnimationFrame(frame), delay);
  }

  scheduleFrame(start);
}
