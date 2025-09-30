import "../components/details-dropdown";
import "../components/feedback-form";
import { enhanceModal } from "../components/modal";
import "../components/navigation";
import "../components/time-clock";
import "../components/tooltip";
import { initPreventDoubleClickSubmit } from "../components/form";
import { initFeedbackHeartView } from "../components/feedback-heart";

const showFeedbackKudo =
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (globalThis as any).zeiterfassung?.feedback?.showFeedbackKudo ?? false;

enhanceModal();

initFeedbackHeartView({
  target: document.body,
  props: {
    showFeedbackKudo: showFeedbackKudo,
  },
});

initPreventDoubleClickSubmit();
