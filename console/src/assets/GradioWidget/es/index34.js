import { S as SvelteComponent, i as init, s as safe_not_equal, I as binding_callbacks, O as bind, c as create_component, m as mount_component, L as add_flush_callback, j as transition_in, k as transition_out, o as destroy_component, F as createEventDispatcher, p as create_slot, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, K as bubble } from "./main.js";
import { T as Tabs } from "./Tabs.js";
import "./Column.js";
function create_default_slot(ctx) {
  let current;
  const default_slot_template = ctx[3].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[6], null);
  return {
    c() {
      if (default_slot)
        default_slot.c();
    },
    m(target, anchor) {
      if (default_slot) {
        default_slot.m(target, anchor);
      }
      current = true;
    },
    p(ctx2, dirty) {
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 64)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[6], !current ? get_all_dirty_from_scope(ctx2[6]) : get_slot_changes(default_slot_template, ctx2[6], dirty, null), null);
        }
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
      if (default_slot)
        default_slot.d(detaching);
    }
  };
}
function create_fragment(ctx) {
  let tabs;
  let updating_selected;
  let current;
  function tabs_selected_binding(value) {
    ctx[4](value);
  }
  let tabs_props = {
    visible: ctx[1],
    elem_id: ctx[2],
    $$slots: { default: [create_default_slot] },
    $$scope: { ctx }
  };
  if (ctx[0] !== void 0) {
    tabs_props.selected = ctx[0];
  }
  tabs = new Tabs({ props: tabs_props });
  binding_callbacks.push(() => bind(tabs, "selected", tabs_selected_binding));
  tabs.$on("change", ctx[5]);
  return {
    c() {
      create_component(tabs.$$.fragment);
    },
    m(target, anchor) {
      mount_component(tabs, target, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const tabs_changes = {};
      if (dirty & 2)
        tabs_changes.visible = ctx2[1];
      if (dirty & 4)
        tabs_changes.elem_id = ctx2[2];
      if (dirty & 64) {
        tabs_changes.$$scope = { dirty, ctx: ctx2 };
      }
      if (!updating_selected && dirty & 1) {
        updating_selected = true;
        tabs_changes.selected = ctx2[0];
        add_flush_callback(() => updating_selected = false);
      }
      tabs.$set(tabs_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(tabs.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(tabs.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(tabs, detaching);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { $$slots: slots = {}, $$scope } = $$props;
  const dispatch = createEventDispatcher();
  let { visible = true } = $$props;
  let { elem_id = "" } = $$props;
  let { selected } = $$props;
  function tabs_selected_binding(value) {
    selected = value;
    $$invalidate(0, selected);
  }
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("visible" in $$props2)
      $$invalidate(1, visible = $$props2.visible);
    if ("elem_id" in $$props2)
      $$invalidate(2, elem_id = $$props2.elem_id);
    if ("selected" in $$props2)
      $$invalidate(0, selected = $$props2.selected);
    if ("$$scope" in $$props2)
      $$invalidate(6, $$scope = $$props2.$$scope);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      dispatch("prop_change", { selected });
    }
  };
  return [
    selected,
    visible,
    elem_id,
    slots,
    tabs_selected_binding,
    change_handler,
    $$scope
  ];
}
class Tabs_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, { visible: 1, elem_id: 2, selected: 0 });
  }
}
var Tabs_1$1 = Tabs_1;
const modes = ["static"];
export { Tabs_1$1 as Component, modes };
//# sourceMappingURL=index34.js.map
