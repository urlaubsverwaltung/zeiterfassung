import "../components/checkbox-all-option";
import "../components/report-graph";
import "../components/report-breakdown-section";
import "../components/report-user-select";
import { initOvertimeSankeyView } from "../components/report-graph/overtime-sankey-view";

initOvertimeSankeyView({
  target: document.body.querySelector("#overtime-sankey"),
  props: {},
});
