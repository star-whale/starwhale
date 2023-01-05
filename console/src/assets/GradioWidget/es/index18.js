import { S as SvelteComponent, i as init, s as safe_not_equal, p as create_slot, e as element, b as attr, d as toggle_class, f as insert, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, j as transition_in, k as transition_out, n as detach } from "./main.js";
var Group_svelte_svelte_type_style_lang = "";
function create_fragment(ctx) {
  let div;
  let current;
  const default_slot_template = ctx[3].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[2], null);
  return {
    c() {
      div = element("div");
      if (default_slot)
        default_slot.c();
      attr(div, "id", ctx[0]);
      attr(div, "class", "svelte-10ogue4");
      toggle_class(div, "hidden", !ctx[1]);
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
        if (default_slot.p && (!current || dirty & 4)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[2], !current ? get_all_dirty_from_scope(ctx2[2]) : get_slot_changes(default_slot_template, ctx2[2], dirty, null), null);
        }
      }
      if (!current || dirty & 1) {
        attr(div, "id", ctx2[0]);
      }
      if (dirty & 2) {
        toggle_class(div, "hidden", !ctx2[1]);
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
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(1, visible = $$props2.visible);
    if ("$$scope" in $$props2)
      $$invalidate(2, $$scope = $$props2.$$scope);
  };
  return [elem_id, visible, $$scope, slots];
}
class Group extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, { elem_id: 0, visible: 1 });
  }
}
var Group$1 = Group;
const modes = ["static"];
export { Group$1 as Component, modes };
//# sourceMappingURL=index18.js.map
