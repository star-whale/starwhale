import { S as SvelteComponent, i as init, s as safe_not_equal, c as create_component, m as mount_component, j as transition_in, k as transition_out, o as destroy_component, R as assign, T as StatusTracker, p as create_slot, a as space, f as insert, U as get_spread_update, V as get_spread_object, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, n as detach, K as bubble } from "./main.js";
import { C as Carousel } from "./CarouselItem.svelte_svelte_type_style_lang.js";
function create_default_slot(ctx) {
  let statustracker;
  let t;
  let current;
  const statustracker_spread_levels = [ctx[2]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  const default_slot_template = ctx[3].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[5], null);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t = space();
      if (default_slot)
        default_slot.c();
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      if (default_slot) {
        default_slot.m(target, anchor);
      }
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 4 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[2])]) : {};
      statustracker.$set(statustracker_changes);
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 32)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[5], !current ? get_all_dirty_from_scope(ctx2[5]) : get_slot_changes(default_slot_template, ctx2[5], dirty, null), null);
        }
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(default_slot, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(default_slot, local);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      if (default_slot)
        default_slot.d(detaching);
    }
  };
}
function create_fragment(ctx) {
  let carousel;
  let current;
  carousel = new Carousel({
    props: {
      elem_id: ctx[0],
      visible: ctx[1],
      $$slots: { default: [create_default_slot] },
      $$scope: { ctx }
    }
  });
  carousel.$on("change", ctx[4]);
  return {
    c() {
      create_component(carousel.$$.fragment);
    },
    m(target, anchor) {
      mount_component(carousel, target, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const carousel_changes = {};
      if (dirty & 1)
        carousel_changes.elem_id = ctx2[0];
      if (dirty & 2)
        carousel_changes.visible = ctx2[1];
      if (dirty & 36) {
        carousel_changes.$$scope = { dirty, ctx: ctx2 };
      }
      carousel.$set(carousel_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(carousel.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(carousel.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(carousel, detaching);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { $$slots: slots = {}, $$scope } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { loading_status } = $$props;
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(1, visible = $$props2.visible);
    if ("loading_status" in $$props2)
      $$invalidate(2, loading_status = $$props2.loading_status);
    if ("$$scope" in $$props2)
      $$invalidate(5, $$scope = $$props2.$$scope);
  };
  return [elem_id, visible, loading_status, slots, change_handler, $$scope];
}
class Carousel_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 0,
      visible: 1,
      loading_status: 2
    });
  }
}
var Carousel_1$1 = Carousel_1;
const modes = ["static"];
export { Carousel_1$1 as Component, modes };
//# sourceMappingURL=index5.js.map
