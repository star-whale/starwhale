import { S as SvelteComponent, i as init, s as safe_not_equal, w as svg_element, b as attr, f as insert, g as append, x as noop, n as detach } from "./main.js";
function create_fragment(ctx) {
  let svg;
  let rect;
  let circle;
  let polyline;
  return {
    c() {
      svg = svg_element("svg");
      rect = svg_element("rect");
      circle = svg_element("circle");
      polyline = svg_element("polyline");
      attr(rect, "x", "3");
      attr(rect, "y", "3");
      attr(rect, "width", "18");
      attr(rect, "height", "18");
      attr(rect, "rx", "2");
      attr(rect, "ry", "2");
      attr(circle, "cx", "8.5");
      attr(circle, "cy", "8.5");
      attr(circle, "r", "1.5");
      attr(polyline, "points", "21 15 16 10 5 21");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "viewBox", "0 0 24 24");
      attr(svg, "fill", "none");
      attr(svg, "stroke", "currentColor");
      attr(svg, "stroke-width", "1.5");
      attr(svg, "stroke-linecap", "round");
      attr(svg, "stroke-linejoin", "round");
      attr(svg, "class", "feather feather-image");
    },
    m(target, anchor) {
      insert(target, svg, anchor);
      append(svg, rect);
      append(svg, circle);
      append(svg, polyline);
    },
    p: noop,
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(svg);
    }
  };
}
class Image extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment, safe_not_equal, {});
  }
}
export { Image as I };
//# sourceMappingURL=Image2.js.map
