import "../components/avatar";
import "../components/feedback-form";
import "../components/form";
import "../components/navigation";
import "../components/time-clock";
import { initFeedbackHeartView } from "../components/feedback-heart";

const showFeedbackKudo =
  (window as any).zeiterfassung?.feedback?.showFeedbackKudo ?? false;

initFeedbackHeartView({
  target: document.body,
  props: {
    showFeedbackKudo: showFeedbackKudo,
  },
});
