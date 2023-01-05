import { S as SvelteComponent, i as init, s as safe_not_equal, e as element, t as text, b as attr, f as insert, g as append, h as set_data, n as detach, p as create_slot, a as space, d as toggle_class, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, j as transition_in, k as transition_out, a1 as getContext, Q as component_subscribe, J as onDestroy, c as create_component, m as mount_component, o as destroy_component } from "./main.js";
import { a as CAROUSEL } from "./CarouselItem.svelte_svelte_type_style_lang.js";
function create_if_block(ctx) {
  let div;
  let t;
  return {
    c() {
      div = element("div");
      t = text(ctx[0]);
      attr(div, "class", "absolute left-0 top-0 py-1 px-2 rounded-br-lg shadow-sm text-xs text-gray-500 flex items-center pointer-events-none bg-white z-20 dark:bg-gray-800");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t);
    },
    p(ctx2, dirty) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_fragment$1(ctx) {
  let div;
  let t;
  let current;
  let if_block = ctx[0] && create_if_block(ctx);
  const default_slot_template = ctx[5].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[4], null);
  return {
    c() {
      div = element("div");
      if (if_block)
        if_block.c();
      t = space();
      if (default_slot)
        default_slot.c();
      attr(div, "class", "carousel-item hidden component min-h-[200px] border rounded-lg overflow-hidden relative svelte-89gglt");
      toggle_class(div, "!block", ctx[1] === ctx[3]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if (if_block)
        if_block.m(div, null);
      append(div, t);
      if (default_slot) {
        default_slot.m(div, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      if (ctx2[0]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block(ctx2);
          if_block.c();
          if_block.m(div, t);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 16)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[4], !current ? get_all_dirty_from_scope(ctx2[4]) : get_slot_changes(default_slot_template, ctx2[4], dirty, null), null);
        }
      }
      if (dirty & 10) {
        toggle_class(div, "!block", ctx2[1] === ctx2[3]);
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
      if (if_block)
        if_block.d();
      if (default_slot)
        default_slot.d(detaching);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let $current;
  let { $$slots: slots = {}, $$scope } = $$props;
  let { label = void 0 } = $$props;
  const { register, unregister, current } = getContext(CAROUSEL);
  component_subscribe($$self, current, (value) => $$invalidate(1, $current = value));
  let id = register();
  onDestroy(() => unregister(id));
  $$self.$$set = ($$props2) => {
    if ("label" in $$props2)
      $$invalidate(0, label = $$props2.label);
    if ("$$scope" in $$props2)
      $$invalidate(4, $$scope = $$props2.$$scope);
  };
  return [label, $current, current, id, $$scope, slots];
}
class CarouselItem extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { label: 0 });
  }
}
function create_default_slot(ctx) {
  let current;
  const default_slot_template = ctx[0].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[1], null);
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
        if (default_slot.p && (!current || dirty & 2)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[1], !current ? get_all_dirty_from_scope(ctx2[1]) : get_slot_changes(default_slot_template, ctx2[1], dirty, null), null);
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
  let carouselitem;
  let current;
  carouselitem = new CarouselItem({
    props: {
      $$slots: { default: [create_default_slot] },
      $$scope: { ctx }
    }
  });
  return {
    c() {
      create_component(carouselitem.$$.fragment);
    },
    m(target, anchor) {
      mount_component(carouselitem, target, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const carouselitem_changes = {};
      if (dirty & 2) {
        carouselitem_changes.$$scope = { dirty, ctx: ctx2 };
      }
      carouselitem.$set(carouselitem_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(carouselitem.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(carouselitem.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(carouselitem, detaching);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { $$slots: slots = {}, $$scope } = $$props;
  $$self.$$set = ($$props2) => {
    if ("$$scope" in $$props2)
      $$invalidate(1, $$scope = $$props2.$$scope);
  };
  return [slots, $$scope];
}
class CarouselItem_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {});
  }
}
var CarouselItem_1$1 = CarouselItem_1;
const modes = ["static"];
export { CarouselItem_1$1 as Component, modes };
//# sourceMappingURL=index6.js.map
