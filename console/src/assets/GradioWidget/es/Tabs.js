import { S as SvelteComponent, i as init, s as safe_not_equal, B as empty, f as insert, n as detach, p as create_slot, e as element, a as space, b as attr, d as toggle_class, g as append, aa as update_keyed_each, u as update_slot_base, q as get_all_dirty_from_scope, r as get_slot_changes, j as transition_in, k as transition_out, Q as component_subscribe, F as createEventDispatcher, _ as setContext, ap as destroy_block, a0 as writable, $ as set_store_value, t as text, l as listen, h as set_data } from "./main.js";
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[11] = list[i];
  return child_ctx;
}
function create_else_block(ctx) {
  let button;
  let t0_value = ctx[11].name + "";
  let t0;
  let t1;
  let mounted;
  let dispose;
  function click_handler() {
    return ctx[9](ctx[11]);
  }
  return {
    c() {
      button = element("button");
      t0 = text(t0_value);
      t1 = space();
      attr(button, "class", "px-4 pb-2 pt-1.5 border-transparent text-gray-400 hover:text-gray-700 -mb-[2px] border-2 border-b-0");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, t0);
      append(button, t1);
      if (!mounted) {
        dispose = listen(button, "click", click_handler);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty & 4 && t0_value !== (t0_value = ctx[11].name + ""))
        set_data(t0, t0_value);
    },
    d(detaching) {
      if (detaching)
        detach(button);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block(ctx) {
  let button;
  let t0_value = ctx[11].name + "";
  let t0;
  let t1;
  return {
    c() {
      button = element("button");
      t0 = text(t0_value);
      t1 = space();
      attr(button, "class", "bg-white px-4 pb-2 pt-1.5 rounded-t-lg border-gray-200 -mb-[2px] border-2 border-b-0");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, t0);
      append(button, t1);
    },
    p(ctx2, dirty) {
      if (dirty & 4 && t0_value !== (t0_value = ctx2[11].name + ""))
        set_data(t0, t0_value);
    },
    d(detaching) {
      if (detaching)
        detach(button);
    }
  };
}
function create_each_block(key_1, ctx) {
  let first;
  let if_block_anchor;
  function select_block_type(ctx2, dirty) {
    if (ctx2[11].id === ctx2[3])
      return create_if_block;
    return create_else_block;
  }
  let current_block_type = select_block_type(ctx);
  let if_block = current_block_type(ctx);
  return {
    key: key_1,
    first: null,
    c() {
      first = empty();
      if_block.c();
      if_block_anchor = empty();
      this.first = first;
    },
    m(target, anchor) {
      insert(target, first, anchor);
      if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (current_block_type === (current_block_type = select_block_type(ctx)) && if_block) {
        if_block.p(ctx, dirty);
      } else {
        if_block.d(1);
        if_block = current_block_type(ctx);
        if (if_block) {
          if_block.c();
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        }
      }
    },
    d(detaching) {
      if (detaching)
        detach(first);
      if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_fragment(ctx) {
  let div1;
  let div0;
  let each_blocks = [];
  let each_1_lookup = /* @__PURE__ */ new Map();
  let t;
  let current;
  let each_value = ctx[2];
  const get_key = (ctx2) => ctx2[11].id;
  for (let i = 0; i < each_value.length; i += 1) {
    let child_ctx = get_each_context(ctx, each_value, i);
    let key = get_key(child_ctx);
    each_1_lookup.set(key, each_blocks[i] = create_each_block(key, child_ctx));
  }
  const default_slot_template = ctx[8].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[7], null);
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      t = space();
      if (default_slot)
        default_slot.c();
      attr(div0, "class", "flex border-b-2 flex-wrap dark:border-gray-700");
      attr(div1, "class", "tabs flex flex-col my-4");
      attr(div1, "id", ctx[1]);
      toggle_class(div1, "hidden", !ctx[0]);
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div0, null);
      }
      append(div1, t);
      if (default_slot) {
        default_slot.m(div1, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      if (dirty & 44) {
        each_value = ctx2[2];
        each_blocks = update_keyed_each(each_blocks, dirty, get_key, 1, ctx2, each_value, each_1_lookup, div0, destroy_block, create_each_block, null, get_each_context);
      }
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 128)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[7], !current ? get_all_dirty_from_scope(ctx2[7]) : get_slot_changes(default_slot_template, ctx2[7], dirty, null), null);
        }
      }
      if (!current || dirty & 2) {
        attr(div1, "id", ctx2[1]);
      }
      if (dirty & 1) {
        toggle_class(div1, "hidden", !ctx2[0]);
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
        detach(div1);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].d();
      }
      if (default_slot)
        default_slot.d(detaching);
    }
  };
}
const TABS = {};
function instance($$self, $$props, $$invalidate) {
  let $selected_tab;
  let { $$slots: slots = {}, $$scope } = $$props;
  let { visible = true } = $$props;
  let { elem_id } = $$props;
  let { selected } = $$props;
  let tabs = [];
  const selected_tab = writable(false);
  component_subscribe($$self, selected_tab, (value) => $$invalidate(3, $selected_tab = value));
  const dispatch = createEventDispatcher();
  setContext(TABS, {
    register_tab: (tab) => {
      tabs.push({ name: tab.name, id: tab.id });
      selected_tab.update((current) => current != null ? current : tab.id);
      $$invalidate(2, tabs);
    },
    unregister_tab: (tab) => {
      const i = tabs.findIndex((t) => t.id === tab.id);
      tabs.splice(i, 1);
      selected_tab.update((current) => {
        var _a, _b;
        return current === tab.id ? ((_a = tabs[i]) == null ? void 0 : _a.id) || ((_b = tabs[tabs.length - 1]) == null ? void 0 : _b.id) : current;
      });
    },
    selected_tab
  });
  function change_tab(id) {
    set_store_value(selected_tab, $selected_tab = id, $selected_tab);
    dispatch("change");
  }
  const click_handler = (t) => change_tab(t.id);
  $$self.$$set = ($$props2) => {
    if ("visible" in $$props2)
      $$invalidate(0, visible = $$props2.visible);
    if ("elem_id" in $$props2)
      $$invalidate(1, elem_id = $$props2.elem_id);
    if ("selected" in $$props2)
      $$invalidate(6, selected = $$props2.selected);
    if ("$$scope" in $$props2)
      $$invalidate(7, $$scope = $$props2.$$scope);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 64) {
      selected !== null && change_tab(selected);
    }
  };
  return [
    visible,
    elem_id,
    tabs,
    $selected_tab,
    selected_tab,
    change_tab,
    selected,
    $$scope,
    slots,
    click_handler
  ];
}
class Tabs extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, { visible: 0, elem_id: 1, selected: 6 });
  }
}
export { Tabs as T, TABS as a };
//# sourceMappingURL=Tabs.js.map
