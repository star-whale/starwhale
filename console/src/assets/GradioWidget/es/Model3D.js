import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, t as text, b as attr, f as insert, g as append, h as set_data, x as noop, n as detach } from "./main.js";
function create_fragment(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[0]);
      attr(div, "class", "gr-sample-3d");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, [dirty]) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { value } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
  };
  return [value];
}
class Model3D extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, { value: 0 });
  }
}
var ExampleModel3D = Model3D;
export { ExampleModel3D as E };
//# sourceMappingURL=Model3D.js.map
