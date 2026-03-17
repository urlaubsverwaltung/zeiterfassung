import "../components/details-dropdown";
import "../components/feedback-form";
import { enhanceModal } from "../components/modal";
import "../components/navigation";
import "../components/time-clock";
import "../components/tooltip";
import {
  initAutosubmit,
  initPreventDoubleClickSubmit,
} from "../components/form";
import { FeedbackHeart } from "../components/feedback-heart";

enhanceModal();
initPreventDoubleClickSubmit();
initAutosubmit();

const feedbackHeart = new FeedbackHeart();
document.querySelector("#fixed-bottom-bar")?.prepend(feedbackHeart);

if (globalThis.zeiterfassung.feedback.showFeedbackKudo) {
  feedbackHeart.unleash();
}
