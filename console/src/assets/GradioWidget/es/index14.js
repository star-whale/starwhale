import { S as SvelteComponent, i as init, s as safe_not_equal, a6 as BlockTitle, e as element, t as text, f as insert, g as append, h as set_data, n as detach, c as create_component, a as space, b as attr, ad as add_render_callback, m as mount_component, ae as select_option, l as listen, j as transition_in, k as transition_out, o as destroy_component, C as destroy_each, F as createEventDispatcher, af as select_value, P as Block, R as assign, T as StatusTracker, I as binding_callbacks, O as bind, U as get_spread_update, V as get_spread_object, L as add_flush_callback, K as bubble } from "./main.js";
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[7] = list[i];
  return child_ctx;
}
function create_default_slot$1(ctx) {
  let t;
  return {
    c() {
      t = text(ctx[1]);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 2)
        set_data(t, ctx2[1]);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_each_block(ctx) {
  let option;
  let t_value = ctx[7] + "";
  let t;
  let option_value_value;
  return {
    c() {
      option = element("option");
      t = text(t_value);
      option.__value = option_value_value = ctx[7];
      option.value = option.__value;
    },
    m(target, anchor) {
      insert(target, option, anchor);
      append(option, t);
    },
    p(ctx2, dirty) {
      if (dirty & 4 && t_value !== (t_value = ctx2[7] + ""))
        set_data(t, t_value);
      if (dirty & 4 && option_value_value !== (option_value_value = ctx2[7])) {
        option.__value = option_value_value;
        option.value = option.__value;
      }
    },
    d(detaching) {
      if (detaching)
        detach(option);
    }
  };
}
function create_fragment$1(ctx) {
  let label_1;
  let blocktitle;
  let t;
  let select;
  let current;
  let mounted;
  let dispose;
  blocktitle = new BlockTitle({
    props: {
      show_label: ctx[4],
      $$slots: { default: [create_default_slot$1] },
      $$scope: { ctx }
    }
  });
  let each_value = ctx[2];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  return {
    c() {
      label_1 = element("label");
      create_component(blocktitle.$$.fragment);
      t = space();
      select = element("select");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(select, "class", "gr-box gr-input w-full disabled:cursor-not-allowed");
      select.disabled = ctx[3];
      if (ctx[0] === void 0)
        add_render_callback(() => ctx[5].call(select));
    },
    m(target, anchor) {
      insert(target, label_1, anchor);
      mount_component(blocktitle, label_1, null);
      append(label_1, t);
      append(label_1, select);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(select, null);
      }
      select_option(select, ctx[0]);
      current = true;
      if (!mounted) {
        dispose = listen(select, "change", ctx[5]);
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 16)
        blocktitle_changes.show_label = ctx2[4];
      if (dirty & 1026) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (dirty & 4) {
        each_value = ctx2[2];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(select, null);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
      if (!current || dirty & 8) {
        select.disabled = ctx2[3];
      }
      if (dirty & 5) {
        select_option(select, ctx2[0]);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(blocktitle.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocktitle.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(label_1);
      destroy_component(blocktitle);
      destroy_each(each_blocks, detaching);
      mounted = false;
      dispose();
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { label } = $$props;
  let { value = void 0 } = $$props;
  let { choices } = $$props;
  let { disabled = false } = $$props;
  let { show_label } = $$props;
  const dispatch = createEventDispatcher();
  function select_change_handler() {
    value = select_value(this);
    $$invalidate(0, value);
    $$invalidate(2, choices);
  }
  $$self.$$set = ($$props2) => {
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("choices" in $$props2)
      $$invalidate(2, choices = $$props2.choices);
    if ("disabled" in $$props2)
      $$invalidate(3, disabled = $$props2.disabled);
    if ("show_label" in $$props2)
      $$invalidate(4, show_label = $$props2.show_label);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      dispatch("change", value);
    }
  };
  return [value, label, choices, disabled, show_label, select_change_handler];
}
class Dropdown extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      label: 1,
      value: 0,
      choices: 2,
      disabled: 3,
      show_label: 4
    });
  }
}
function create_default_slot(ctx) {
  let statustracker;
  let t;
  let dropdown;
  let updating_value;
  let current;
  const statustracker_spread_levels = [ctx[7]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  function dropdown_value_binding(value) {
    ctx[9](value);
  }
  let dropdown_props = {
    choices: ctx[4],
    label: ctx[1],
    show_label: ctx[5],
    disabled: ctx[8] === "static"
  };
  if (ctx[0] !== void 0) {
    dropdown_props.value = ctx[0];
  }
  dropdown = new Dropdown({ props: dropdown_props });
  binding_callbacks.push(() => bind(dropdown, "value", dropdown_value_binding));
  dropdown.$on("change", ctx[10]);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t = space();
      create_component(dropdown.$$.fragment);
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      mount_component(dropdown, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 128 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[7])]) : {};
      statustracker.$set(statustracker_changes);
      const dropdown_changes = {};
      if (dirty & 16)
        dropdown_changes.choices = ctx2[4];
      if (dirty & 2)
        dropdown_changes.label = ctx2[1];
      if (dirty & 32)
        dropdown_changes.show_label = ctx2[5];
      if (dirty & 256)
        dropdown_changes.disabled = ctx2[8] === "static";
      if (!updating_value && dirty & 1) {
        updating_value = true;
        dropdown_changes.value = ctx2[0];
        add_flush_callback(() => updating_value = false);
      }
      dropdown.$set(dropdown_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(dropdown.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(dropdown.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      destroy_component(dropdown, detaching);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      visible: ctx[3],
      elem_id: ctx[2],
      disable: typeof ctx[6].container === "boolean" && !ctx[6].container,
      $$slots: { default: [create_default_slot] },
      $$scope: { ctx }
    }
  });
  return {
    c() {
      create_component(block.$$.fragment);
    },
    m(target, anchor) {
      mount_component(block, target, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const block_changes = {};
      if (dirty & 8)
        block_changes.visible = ctx2[3];
      if (dirty & 4)
        block_changes.elem_id = ctx2[2];
      if (dirty & 64)
        block_changes.disable = typeof ctx2[6].container === "boolean" && !ctx2[6].container;
      if (dirty & 2483) {
        block_changes.$$scope = { dirty, ctx: ctx2 };
      }
      block.$set(block_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(block.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(block.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(block, detaching);
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { label = "Dropdown" } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = "" } = $$props;
  let { choices } = $$props;
  let { show_label } = $$props;
  let { style = {} } = $$props;
  let { loading_status } = $$props;
  let { mode } = $$props;
  function dropdown_value_binding(value$1) {
    value = value$1;
    $$invalidate(0, value);
  }
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
    if ("elem_id" in $$props2)
      $$invalidate(2, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(3, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("choices" in $$props2)
      $$invalidate(4, choices = $$props2.choices);
    if ("show_label" in $$props2)
      $$invalidate(5, show_label = $$props2.show_label);
    if ("style" in $$props2)
      $$invalidate(6, style = $$props2.style);
    if ("loading_status" in $$props2)
      $$invalidate(7, loading_status = $$props2.loading_status);
    if ("mode" in $$props2)
      $$invalidate(8, mode = $$props2.mode);
  };
  return [
    value,
    label,
    elem_id,
    visible,
    choices,
    show_label,
    style,
    loading_status,
    mode,
    dropdown_value_binding,
    change_handler
  ];
}
class Dropdown_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      label: 1,
      elem_id: 2,
      visible: 3,
      value: 0,
      choices: 4,
      show_label: 5,
      style: 6,
      loading_status: 7,
      mode: 8
    });
  }
}
var Dropdown_1$1 = Dropdown_1;
const modes = ["static", "dynamic"];
const document = (config) => ({
  type: "string",
  description: "selected choice",
  example_data: config.choices.length ? config.choices[0] : ""
});
export { Dropdown_1$1 as Component, document, modes };
//# sourceMappingURL=index14.js.map
