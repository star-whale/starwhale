import { S as SvelteComponent, i as init, s as safe_not_equal, p as create_slot, e as element, a as space, t as text, b as attr, d as toggle_class, f as insert, g as append, l as listen, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, h as set_data, j as transition_in, k as transition_out, n as detach, A as run_all, F as createEventDispatcher, Q as component_subscribe, _ as setContext, $ as set_store_value, a0 as writable } from "./main.js";
function create_fragment(ctx) {
  let div2;
  let t0;
  let div1;
  let button0;
  let t1;
  let div0;
  let t2_value = ctx[2] + 1 + "";
  let t2;
  let t3;
  let t4_value = ctx[3].length + "";
  let t4;
  let t5;
  let button1;
  let current;
  let mounted;
  let dispose;
  const default_slot_template = ctx[9].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[8], null);
  return {
    c() {
      div2 = element("div");
      if (default_slot)
        default_slot.c();
      t0 = space();
      div1 = element("div");
      button0 = element("button");
      button0.innerHTML = `<svg class="caret text-xs fill-current" width=".7em" height=".7em" viewBox="0 0 9.1457395 15.999842"><path d="M 0.32506616,7.2360106 7.1796187,0.33129769 c 0.4360247,-0.439451 1.1455702,-0.442056 1.5845974,-0.0058 0.4390612,0.435849 0.441666,1.14535901 0.00582,1.58438501 l -6.064985,6.1096644 6.10968,6.0646309 c 0.4390618,0.436026 0.4416664,1.145465 0.00582,1.584526 -0.4358485,0.439239 -1.1453586,0.441843 -1.5845975,0.0058 L 0.33088256,8.8203249 C 0.11135166,8.6022941 0.00105996,8.3161928 7.554975e-6,8.0295489 -0.00104244,7.7427633 0.10735446,7.4556467 0.32524356,7.2361162"></path></svg>`;
      t1 = space();
      div0 = element("div");
      t2 = text(t2_value);
      t3 = text(" / ");
      t4 = text(t4_value);
      t5 = space();
      button1 = element("button");
      button1.innerHTML = `<svg class="caret text-xs fill-current scale-x-[-1]" width=".7em" height=".7em" viewBox="0 0 9.1457395 15.999842"><path d="M 0.32506616,7.2360106 7.1796187,0.33129769 c 0.4360247,-0.439451 1.1455702,-0.442056 1.5845974,-0.0058 0.4390612,0.435849 0.441666,1.14535901 0.00582,1.58438501 l -6.064985,6.1096644 6.10968,6.0646309 c 0.4390618,0.436026 0.4416664,1.145465 0.00582,1.584526 -0.4358485,0.439239 -1.1453586,0.441843 -1.5845975,0.0058 L 0.33088256,8.8203249 C 0.11135166,8.6022941 0.00105996,8.3161928 7.554975e-6,8.0295489 -0.00104244,7.7427633 0.10735446,7.4556467 0.32524356,7.2361162"></path></svg>`;
      attr(button0, "class", "flex items-center justify-center h-6 w-6 hover:text-orange-500");
      attr(div0, "class", "carousel_index text-center font-semibold");
      attr(button1, "class", "flex items-center justify-center h-6 w-6 hover:text-orange-500");
      attr(div1, "class", "carousel-control flex gap-4 justify-center items-center pt-2 text-sm");
      attr(div2, "class", "output-carousel flex flex-col relative");
      attr(div2, "id", ctx[0]);
      toggle_class(div2, "!hidden", !ctx[1]);
    },
    m(target, anchor) {
      insert(target, div2, anchor);
      if (default_slot) {
        default_slot.m(div2, null);
      }
      append(div2, t0);
      append(div2, div1);
      append(div1, button0);
      append(div1, t1);
      append(div1, div0);
      append(div0, t2);
      append(div0, t3);
      append(div0, t4);
      append(div1, t5);
      append(div1, button1);
      current = true;
      if (!mounted) {
        dispose = [
          listen(button0, "click", ctx[7]),
          listen(button1, "click", ctx[6])
        ];
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 256)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[8], !current ? get_all_dirty_from_scope(ctx2[8]) : get_slot_changes(default_slot_template, ctx2[8], dirty, null), null);
        }
      }
      if ((!current || dirty & 4) && t2_value !== (t2_value = ctx2[2] + 1 + ""))
        set_data(t2, t2_value);
      if ((!current || dirty & 8) && t4_value !== (t4_value = ctx2[3].length + ""))
        set_data(t4, t4_value);
      if (!current || dirty & 1) {
        attr(div2, "id", ctx2[0]);
      }
      if (dirty & 2) {
        toggle_class(div2, "!hidden", !ctx2[1]);
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
        detach(div2);
      if (default_slot)
        default_slot.d(detaching);
      mounted = false;
      run_all(dispose);
    }
  };
}
const CAROUSEL = {};
function instance($$self, $$props, $$invalidate) {
  let $items;
  let $current;
  let { $$slots: slots = {}, $$scope } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  const dispatch = createEventDispatcher();
  const items = writable([]);
  component_subscribe($$self, items, (value) => $$invalidate(3, $items = value));
  const current = writable();
  component_subscribe($$self, current, (value) => $$invalidate(11, $current = value));
  let id = -1;
  setContext(CAROUSEL, {
    register: () => {
      $items.push(++id);
      items.set($items);
      return id;
    },
    unregister: (id2) => {
      const i = $items.findIndex((_id) => _id === id2);
      $items.slice(i, 1);
      items.set($items);
    },
    current
  });
  let carousel_index = 0;
  const next = () => {
    $$invalidate(2, carousel_index = (carousel_index + 1) % $items.length);
    dispatch("change");
  };
  const prev = () => {
    $$invalidate(2, carousel_index = (carousel_index - 1 + $items.length) % $items.length);
    dispatch("change");
  };
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(1, visible = $$props2.visible);
    if ("$$scope" in $$props2)
      $$invalidate(8, $$scope = $$props2.$$scope);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 12) {
      set_store_value(current, $current = $items[carousel_index] || 0, $current);
    }
  };
  return [
    elem_id,
    visible,
    carousel_index,
    $items,
    items,
    current,
    next,
    prev,
    $$scope,
    slots
  ];
}
class Carousel extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, { elem_id: 0, visible: 1 });
  }
}
var CarouselItem_svelte_svelte_type_style_lang = "";
export { Carousel as C, CAROUSEL as a };
//# sourceMappingURL=CarouselItem.svelte_svelte_type_style_lang.js.map
