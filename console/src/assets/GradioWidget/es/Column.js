import { S as SvelteComponent, i as init, s as safe_not_equal, p as create_slot, e as element, b as attr, v as create_classes, d as toggle_class, f as insert, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, j as transition_in, k as transition_out, n as detach } from "./main.js";
function create_fragment(ctx) {
  let div;
  let div_class_value;
  let div_style_value;
  let current;
  const default_slot_template = ctx[7].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[6], null);
  return {
    c() {
      div = element("div");
      if (default_slot)
        default_slot.c();
      attr(div, "id", ctx[2]);
      attr(div, "class", div_class_value = "overflow-hidden flex flex-col relative col " + create_classes(ctx[5]));
      attr(div, "style", div_style_value = `min-width: min(${ctx[1]}px, 100%); flex-grow: ${ctx[0]}`);
      toggle_class(div, "gap-4", ctx[5].gap !== false);
      toggle_class(div, "gr-compact", ctx[4] === "compact");
      toggle_class(div, "gr-panel", ctx[4] === "panel");
      toggle_class(div, "!hidden", !ctx[3]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if (default_slot) {
        default_slot.m(div, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 64)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[6], !current ? get_all_dirty_from_scope(ctx2[6]) : get_slot_changes(default_slot_template, ctx2[6], dirty, null), null);
        }
      }
      if (!current || dirty & 4) {
        attr(div, "id", ctx2[2]);
      }
      if (!current || dirty & 32 && div_class_value !== (div_class_value = "overflow-hidden flex flex-col relative col " + create_classes(ctx2[5]))) {
        attr(div, "class", div_class_value);
      }
      if (!current || dirty & 3 && div_style_value !== (div_style_value = `min-width: min(${ctx2[1]}px, 100%); flex-grow: ${ctx2[0]}`)) {
        attr(div, "style", div_style_value);
      }
      if (dirty & 32) {
        toggle_class(div, "gap-4", ctx2[5].gap !== false);
      }
      if (dirty & 48) {
        toggle_class(div, "gr-compact", ctx2[4] === "compact");
      }
      if (dirty & 48) {
        toggle_class(div, "gr-panel", ctx2[4] === "panel");
      }
      if (dirty & 40) {
        toggle_class(div, "!hidden", !ctx2[3]);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(default_slot, local);
      current = true;
    },
    o(local) {
      transition_out(default_slot, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      if (default_slot)
        default_slot.d(detaching);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { $$slots: slots = {}, $$scope } = $$props;
  let { scale = 1 } = $$props;
  let { min_width = 0 } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { variant = "default" } = $$props;
  let { style = {} } = $$props;
  $$self.$$set = ($$props2) => {
    if ("scale" in $$props2)
      $$invalidate(0, scale = $$props2.scale);
    if ("min_width" in $$props2)
      $$invalidate(1, min_width = $$props2.min_width);
    if ("elem_id" in $$props2)
      $$invalidate(2, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(3, visible = $$props2.visible);
    if ("variant" in $$props2)
      $$invalidate(4, variant = $$props2.variant);
    if ("style" in $$props2)
      $$invalidate(5, style = $$props2.style);
    if ("$$scope" in $$props2)
      $$invalidate(6, $$scope = $$props2.$$scope);
  };
  return [scale, min_width, elem_id, visible, variant, style, $$scope, slots];
}
class Column extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      scale: 0,
      min_width: 1,
      elem_id: 2,
      visible: 3,
      variant: 4,
      style: 5
    });
  }
}
var Column$1 = Column;
export { Column$1 as C };
//# sourceMappingURL=Column.js.map
