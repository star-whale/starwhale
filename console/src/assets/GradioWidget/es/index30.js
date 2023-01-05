import { S as SvelteComponent, i as init, s as safe_not_equal, p as create_slot, e as element, b as attr, d as toggle_class, f as insert, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, j as transition_in, k as transition_out, n as detach } from "./main.js";
function create_fragment(ctx) {
  let div;
  let current;
  const default_slot_template = ctx[5].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[4], null);
  return {
    c() {
      div = element("div");
      if (default_slot)
        default_slot.c();
      attr(div, "class", "flex row w-full flex-wrap gap-4");
      attr(div, "id", ctx[1]);
      toggle_class(div, "gr-compact", ctx[3] === "compact");
      toggle_class(div, "gr-panel", ctx[3] === "panel");
      toggle_class(div, "unequal-height", ctx[0].equal_height === false);
      toggle_class(div, "items-stretch", ctx[0].equal_height);
      toggle_class(div, "!hidden", !ctx[2]);
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
        if (default_slot.p && (!current || dirty & 16)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[4], !current ? get_all_dirty_from_scope(ctx2[4]) : get_slot_changes(default_slot_template, ctx2[4], dirty, null), null);
        }
      }
      if (!current || dirty & 2) {
        attr(div, "id", ctx2[1]);
      }
      if (dirty & 8) {
        toggle_class(div, "gr-compact", ctx2[3] === "compact");
      }
      if (dirty & 8) {
        toggle_class(div, "gr-panel", ctx2[3] === "panel");
      }
      if (dirty & 1) {
        toggle_class(div, "unequal-height", ctx2[0].equal_height === false);
      }
      if (dirty & 1) {
        toggle_class(div, "items-stretch", ctx2[0].equal_height);
      }
      if (dirty & 4) {
        toggle_class(div, "!hidden", !ctx2[2]);
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
  let { style = {} } = $$props;
  let { elem_id } = $$props;
  let { visible = true } = $$props;
  let { variant = "default" } = $$props;
  $$self.$$set = ($$props2) => {
    if ("style" in $$props2)
      $$invalidate(0, style = $$props2.style);
    if ("elem_id" in $$props2)
      $$invalidate(1, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(2, visible = $$props2.visible);
    if ("variant" in $$props2)
      $$invalidate(3, variant = $$props2.variant);
    if ("$$scope" in $$props2)
      $$invalidate(4, $$scope = $$props2.$$scope);
  };
  return [style, elem_id, visible, variant, $$scope, slots];
}
class Row extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      style: 0,
      elem_id: 1,
      visible: 2,
      variant: 3
    });
  }
}
var Row$1 = Row;
const modes = ["static"];
export { Row$1 as Component, modes };
//# sourceMappingURL=index30.js.map
