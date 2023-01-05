import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, c as create_component, b as attr, Y as set_style, f as insert, m as mount_component, j as transition_in, k as transition_out, n as detach, o as destroy_component, F as createEventDispatcher, a1 as getContext, Q as component_subscribe, ac as onMount, a9 as tick, p as create_slot, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, K as bubble } from "./main.js";
import { a as TABS } from "./Tabs.js";
import { C as Column } from "./Column.js";
function create_default_slot$1(ctx) {
  let current;
  const default_slot_template = ctx[5].default;
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
function create_fragment$1(ctx) {
  let div;
  let column;
  let current;
  column = new Column({
    props: {
      $$slots: { default: [create_default_slot$1] },
      $$scope: { ctx }
    }
  });
  return {
    c() {
      div = element("div");
      create_component(column.$$.fragment);
      attr(div, "id", ctx[0]);
      attr(div, "class", "tabitem p-2 border-2 border-t-0 border-gray-200 relative flex");
      set_style(div, "display", ctx[2] === ctx[1] ? "block" : "none", false);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(column, div, null);
      current = true;
    },
    p(ctx2, [dirty]) {
      const column_changes = {};
      if (dirty & 64) {
        column_changes.$$scope = { dirty, ctx: ctx2 };
      }
      column.$set(column_changes);
      if (!current || dirty & 1) {
        attr(div, "id", ctx2[0]);
      }
      if (dirty & 6) {
        set_style(div, "display", ctx2[2] === ctx2[1] ? "block" : "none", false);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(column.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(column.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(column);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let $selected_tab;
  let { $$slots: slots = {}, $$scope } = $$props;
  let { elem_id = "" } = $$props;
  let { name } = $$props;
  let { id = {} } = $$props;
  const dispatch = createEventDispatcher();
  const { register_tab, unregister_tab, selected_tab } = getContext(TABS);
  component_subscribe($$self, selected_tab, (value) => $$invalidate(2, $selected_tab = value));
  register_tab({ name, id });
  onMount(() => {
    return () => unregister_tab({ name, id });
  });
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("name" in $$props2)
      $$invalidate(4, name = $$props2.name);
    if ("id" in $$props2)
      $$invalidate(1, id = $$props2.id);
    if ("$$scope" in $$props2)
      $$invalidate(6, $$scope = $$props2.$$scope);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 6) {
      $selected_tab === id && tick().then(() => dispatch("select"));
    }
  };
  return [elem_id, id, $selected_tab, selected_tab, name, slots, $$scope];
}
class TabItem extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { elem_id: 0, name: 4, id: 1 });
  }
}
function create_default_slot(ctx) {
  let current;
  const default_slot_template = ctx[3].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[5], null);
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
        if (default_slot.p && (!current || dirty & 32)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[5], !current ? get_all_dirty_from_scope(ctx2[5]) : get_slot_changes(default_slot_template, ctx2[5], dirty, null), null);
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
  let tabitem;
  let current;
  tabitem = new TabItem({
    props: {
      elem_id: ctx[0],
      name: ctx[1],
      id: ctx[2],
      $$slots: { default: [create_default_slot] },
      $$scope: { ctx }
    }
  });
  tabitem.$on("select", ctx[4]);
  return {
    c() {
      create_component(tabitem.$$.fragment);
    },
    m(target, anchor) {
      mount_component(tabitem, target, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const tabitem_changes = {};
      if (dirty & 1)
        tabitem_changes.elem_id = ctx2[0];
      if (dirty & 2)
        tabitem_changes.name = ctx2[1];
      if (dirty & 4)
        tabitem_changes.id = ctx2[2];
      if (dirty & 32) {
        tabitem_changes.$$scope = { dirty, ctx: ctx2 };
      }
      tabitem.$set(tabitem_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(tabitem.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(tabitem.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(tabitem, detaching);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { $$slots: slots = {}, $$scope } = $$props;
  let { elem_id = "" } = $$props;
  let { label } = $$props;
  let { id } = $$props;
  function select_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
    if ("id" in $$props2)
      $$invalidate(2, id = $$props2.id);
    if ("$$scope" in $$props2)
      $$invalidate(5, $$scope = $$props2.$$scope);
  };
  return [elem_id, label, id, slots, select_handler, $$scope];
}
class Tabs extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, { elem_id: 0, label: 1, id: 2 });
  }
}
var Tabs$1 = Tabs;
const modes = ["static"];
export { Tabs$1 as Component, modes };
//# sourceMappingURL=index35.js.map
