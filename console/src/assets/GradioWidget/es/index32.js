import { S as SvelteComponent, i as init, s as safe_not_equal } from "./main.js";
class State extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, null, safe_not_equal, {});
  }
}
var State$1 = State;
const modes = ["static"];
const document = (config) => ({
  type: "Any",
  description: "stored state value",
  example_data: ""
});
export { State$1 as Component, document, modes };
//# sourceMappingURL=index32.js.map
