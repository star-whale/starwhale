import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, t as text, a as space, c as create_component, b as attr, d as toggle_class, f as insert, g as append, m as mount_component, l as listen, h as set_data, j as transition_in, k as transition_out, n as detach, o as destroy_component, p as create_slot, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes } from "./main.js";
import { C as Column } from "./Column.js";
function create_default_slot(ctx) {
  let current;
  const default_slot_template = ctx[6].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[7], null);
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
        if (default_slot.p && (!current || dirty & 128)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[7], !current ? get_all_dirty_from_scope(ctx2[7]) : get_slot_changes(default_slot_template, ctx2[7], dirty, null), null);
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
  let div1;
  let div0;
  let span0;
  let t0;
  let t1;
  let span1;
  let t3;
  let column;
  let current;
  let mounted;
  let dispose;
  column = new Column({
    props: {
      visible: ctx[3],
      $$slots: { default: [create_default_slot] },
      $$scope: { ctx }
    }
  });
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      span0 = element("span");
      t0 = text(ctx[0]);
      t1 = space();
      span1 = element("span");
      span1.textContent = "\u25BC";
      t3 = space();
      create_component(column.$$.fragment);
      attr(span1, "class", "transition");
      toggle_class(span1, "rotate-90", !ctx[3]);
      attr(div0, "class", "w-full flex justify-between cursor-pointer");
      attr(div1, "id", ctx[1]);
      attr(div1, "class", "p-3 border border-gray-200 dark:border-gray-700 rounded-lg flex flex-col gap-3 hover:border-gray-300 dark:hover:border-gray-600 transition");
      toggle_class(div1, "hidden", !ctx[2]);
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      append(div0, span0);
      append(span0, t0);
      append(div0, t1);
      append(div0, span1);
      append(div1, t3);
      mount_component(column, div1, null);
      current = true;
      if (!mounted) {
        dispose = listen(div0, "click", ctx[4]);
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (!current || dirty & 1)
        set_data(t0, ctx2[0]);
      if (dirty & 8) {
        toggle_class(span1, "rotate-90", !ctx2[3]);
      }
      const column_changes = {};
      if (dirty & 8)
        column_changes.visible = ctx2[3];
      if (dirty & 128) {
        column_changes.$$scope = { dirty, ctx: ctx2 };
      }
      column.$set(column_changes);
      if (!current || dirty & 2) {
        attr(div1, "id", ctx2[1]);
      }
      if (dirty & 4) {
        toggle_class(div1, "hidden", !ctx2[2]);
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
        detach(div1);
      destroy_component(column);
      mounted = false;
      dispose();
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let _open;
  let { $$slots: slots = {}, $$scope } = $$props;
  let { label } = $$props;
  let { elem_id } = $$props;
  let { visible = true } = $$props;
  let { open = true } = $$props;
  const toggle = () => {
    $$invalidate(3, _open = !_open);
  };
  $$self.$$set = ($$props2) => {
    if ("label" in $$props2)
      $$invalidate(0, label = $$props2.label);
    if ("elem_id" in $$props2)
      $$invalidate(1, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(2, visible = $$props2.visible);
    if ("open" in $$props2)
      $$invalidate(5, open = $$props2.open);
    if ("$$scope" in $$props2)
      $$invalidate(7, $$scope = $$props2.$$scope);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 32) {
      $$invalidate(3, _open = open);
    }
  };
  return [label, elem_id, visible, _open, toggle, open, slots, $$scope];
}
class Accordion extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      label: 0,
      elem_id: 1,
      visible: 2,
      open: 5
    });
  }
}
var Accordion$1 = Accordion;
const modes = ["static"];
export { Accordion$1 as Component, modes };
//# sourceMappingURL=index.js.map
