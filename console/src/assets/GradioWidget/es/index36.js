export { aq as Component } from "./main.js";
const modes = ["static", "dynamic"];
const document = (config) => ({
  type: "string",
  description: "text string",
  example_data: config.value || "hello world"
});
export { document, modes };
//# sourceMappingURL=index36.js.map
