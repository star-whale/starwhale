import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, b as attr, M as src_url_equal, f as insert, x as noop, n as detach } from "./main.js";
function create_fragment(ctx) {
  let img;
  let img_src_value;
  return {
    c() {
      img = element("img");
      attr(img, "class", "gr-sample-image object-contain h-20 w-20");
      if (!src_url_equal(img.src, img_src_value = ctx[1] + ctx[0]))
        attr(img, "src", img_src_value);
    },
    m(target, anchor) {
      insert(target, img, anchor);
    },
    p(ctx2, [dirty]) {
      if (dirty & 3 && !src_url_equal(img.src, img_src_value = ctx2[1] + ctx2[0])) {
        attr(img, "src", img_src_value);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(img);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { value } = $$props;
  let { samples_dir } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("samples_dir" in $$props2)
      $$invalidate(1, samples_dir = $$props2.samples_dir);
  };
  return [value, samples_dir];
}
class Image extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, { value: 0, samples_dir: 1 });
  }
}
var ExampleImage = Image;
export { ExampleImage as E };
//# sourceMappingURL=Image.js.map
