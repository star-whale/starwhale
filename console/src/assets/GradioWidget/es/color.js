import { ai as ordered_colors } from "./main.js";
const get_next_color = (index) => {
  return ordered_colors[index % ordered_colors.length];
};
export { get_next_color as g };
//# sourceMappingURL=color.js.map
