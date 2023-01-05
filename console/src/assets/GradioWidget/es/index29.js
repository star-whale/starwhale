import { S as SvelteComponent, i as init, s as safe_not_equal, a6 as BlockTitle, e as element, a as space, t as text, b as attr, d as toggle_class, f as insert, g as append, l as listen, h as set_data, n as detach, c as create_component, m as mount_component, aa as update_keyed_each, ap as destroy_block, j as transition_in, k as transition_out, o as destroy_component, F as createEventDispatcher, Z as get_styles, P as Block, R as assign, T as StatusTracker, I as binding_callbacks, O as bind, U as get_spread_update, V as get_spread_object, L as add_flush_callback, K as bubble } from "./main.js";
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[11] = list[i];
  child_ctx[13] = i;
  return child_ctx;
}
function create_default_slot$1(ctx) {
  let t;
  return {
    c() {
      t = text(ctx[3]);
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 8)
        set_data(t, ctx2[3]);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_each_block(key_1, ctx) {
  let label_1;
  let input;
  let input_name_value;
  let input_value_value;
  let t0;
  let span;
  let t1_value = ctx[11] + "";
  let t1;
  let label_1_class_value;
  let mounted;
  let dispose;
  return {
    key: key_1,
    first: null,
    c() {
      label_1 = element("label");
      input = element("input");
      t0 = space();
      span = element("span");
      t1 = text(t1_value);
      input.disabled = ctx[2];
      attr(input, "type", "radio");
      attr(input, "name", input_name_value = "radio-" + ctx[5]);
      attr(input, "class", "gr-check-radio gr-radio");
      input.__value = input_value_value = ctx[11];
      input.value = input.__value;
      ctx[9][0].push(input);
      attr(span, "class", "ml-2");
      attr(label_1, "class", label_1_class_value = "gr-input-label flex items-center text-gray-700 text-sm space-x-2 border py-1.5 px-3 rounded-lg cursor-pointer bg-white shadow-sm checked:shadow-inner " + ctx[6]);
      toggle_class(label_1, "!cursor-not-allowed", ctx[2]);
      this.first = label_1;
    },
    m(target, anchor) {
      insert(target, label_1, anchor);
      append(label_1, input);
      input.checked = input.__value === ctx[0];
      append(label_1, t0);
      append(label_1, span);
      append(span, t1);
      if (!mounted) {
        dispose = listen(input, "change", ctx[8]);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty & 4) {
        input.disabled = ctx[2];
      }
      if (dirty & 32 && input_name_value !== (input_name_value = "radio-" + ctx[5])) {
        attr(input, "name", input_name_value);
      }
      if (dirty & 2 && input_value_value !== (input_value_value = ctx[11])) {
        input.__value = input_value_value;
        input.value = input.__value;
      }
      if (dirty & 1) {
        input.checked = input.__value === ctx[0];
      }
      if (dirty & 2 && t1_value !== (t1_value = ctx[11] + ""))
        set_data(t1, t1_value);
      if (dirty & 64 && label_1_class_value !== (label_1_class_value = "gr-input-label flex items-center text-gray-700 text-sm space-x-2 border py-1.5 px-3 rounded-lg cursor-pointer bg-white shadow-sm checked:shadow-inner " + ctx[6])) {
        attr(label_1, "class", label_1_class_value);
      }
      if (dirty & 68) {
        toggle_class(label_1, "!cursor-not-allowed", ctx[2]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(label_1);
      ctx[9][0].splice(ctx[9][0].indexOf(input), 1);
      mounted = false;
      dispose();
    }
  };
}
function create_fragment$1(ctx) {
  let blocktitle;
  let t;
  let div;
  let each_blocks = [];
  let each_1_lookup = /* @__PURE__ */ new Map();
  let current;
  blocktitle = new BlockTitle({
    props: {
      show_label: ctx[4],
      $$slots: { default: [create_default_slot$1] },
      $$scope: { ctx }
    }
  });
  let each_value = ctx[1];
  const get_key = (ctx2) => ctx2[13];
  for (let i = 0; i < each_value.length; i += 1) {
    let child_ctx = get_each_context(ctx, each_value, i);
    let key = get_key(child_ctx);
    each_1_lookup.set(key, each_blocks[i] = create_each_block(key, child_ctx));
  }
  return {
    c() {
      create_component(blocktitle.$$.fragment);
      t = space();
      div = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", "flex flex-wrap gap-2");
    },
    m(target, anchor) {
      mount_component(blocktitle, target, anchor);
      insert(target, t, anchor);
      insert(target, div, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 16)
        blocktitle_changes.show_label = ctx2[4];
      if (dirty & 16392) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (dirty & 103) {
        each_value = ctx2[1];
        each_blocks = update_keyed_each(each_blocks, dirty, get_key, 1, ctx2, each_value, each_1_lookup, div, destroy_block, create_each_block, null, get_each_context);
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
      destroy_component(blocktitle, detaching);
      if (detaching)
        detach(t);
      if (detaching)
        detach(div);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].d();
      }
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let item_container;
  let { value } = $$props;
  let { style = {} } = $$props;
  let { choices } = $$props;
  let { disabled = false } = $$props;
  let { label } = $$props;
  let { show_label } = $$props;
  let { elem_id } = $$props;
  const dispatch = createEventDispatcher();
  const $$binding_groups = [[]];
  function input_change_handler() {
    value = this.__value;
    $$invalidate(0, value);
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("style" in $$props2)
      $$invalidate(7, style = $$props2.style);
    if ("choices" in $$props2)
      $$invalidate(1, choices = $$props2.choices);
    if ("disabled" in $$props2)
      $$invalidate(2, disabled = $$props2.disabled);
    if ("label" in $$props2)
      $$invalidate(3, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(4, show_label = $$props2.show_label);
    if ("elem_id" in $$props2)
      $$invalidate(5, elem_id = $$props2.elem_id);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      dispatch("change", value);
    }
    if ($$self.$$.dirty & 128) {
      $$invalidate(6, { item_container } = get_styles(style, ["item_container"]), item_container);
    }
  };
  return [
    value,
    choices,
    disabled,
    label,
    show_label,
    elem_id,
    item_container,
    style,
    input_change_handler,
    $$binding_groups
  ];
}
class Radio extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      value: 0,
      style: 7,
      choices: 1,
      disabled: 2,
      label: 3,
      show_label: 4,
      elem_id: 5
    });
  }
}
function create_default_slot(ctx) {
  let statustracker;
  let t;
  let radio;
  let updating_value;
  let current;
  const statustracker_spread_levels = [ctx[8]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  function radio_value_binding(value) {
    ctx[9](value);
  }
  let radio_props = {
    label: ctx[1],
    elem_id: ctx[2],
    show_label: ctx[6],
    choices: ctx[4],
    style: ctx[7],
    disabled: ctx[5] === "static"
  };
  if (ctx[0] !== void 0) {
    radio_props.value = ctx[0];
  }
  radio = new Radio({ props: radio_props });
  binding_callbacks.push(() => bind(radio, "value", radio_value_binding));
  radio.$on("change", ctx[10]);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t = space();
      create_component(radio.$$.fragment);
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      mount_component(radio, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 256 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[8])]) : {};
      statustracker.$set(statustracker_changes);
      const radio_changes = {};
      if (dirty & 2)
        radio_changes.label = ctx2[1];
      if (dirty & 4)
        radio_changes.elem_id = ctx2[2];
      if (dirty & 64)
        radio_changes.show_label = ctx2[6];
      if (dirty & 16)
        radio_changes.choices = ctx2[4];
      if (dirty & 128)
        radio_changes.style = ctx2[7];
      if (dirty & 32)
        radio_changes.disabled = ctx2[5] === "static";
      if (!updating_value && dirty & 1) {
        updating_value = true;
        radio_changes.value = ctx2[0];
        add_flush_callback(() => updating_value = false);
      }
      radio.$set(radio_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(radio.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(radio.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      destroy_component(radio, detaching);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      visible: ctx[3],
      type: "fieldset",
      elem_id: ctx[2],
      disable: typeof ctx[7].container === "boolean" && !ctx[7].container,
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
      if (dirty & 128)
        block_changes.disable = typeof ctx2[7].container === "boolean" && !ctx2[7].container;
      if (dirty & 2551) {
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
  let { label = "Radio" } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = "" } = $$props;
  let { choices = [] } = $$props;
  let { mode } = $$props;
  let { show_label } = $$props;
  let { style = {} } = $$props;
  let { loading_status } = $$props;
  function radio_value_binding(value$1) {
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
    if ("mode" in $$props2)
      $$invalidate(5, mode = $$props2.mode);
    if ("show_label" in $$props2)
      $$invalidate(6, show_label = $$props2.show_label);
    if ("style" in $$props2)
      $$invalidate(7, style = $$props2.style);
    if ("loading_status" in $$props2)
      $$invalidate(8, loading_status = $$props2.loading_status);
  };
  return [
    value,
    label,
    elem_id,
    visible,
    choices,
    mode,
    show_label,
    style,
    loading_status,
    radio_value_binding,
    change_handler
  ];
}
class Radio_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      label: 1,
      elem_id: 2,
      visible: 3,
      value: 0,
      choices: 4,
      mode: 5,
      show_label: 6,
      style: 7,
      loading_status: 8
    });
  }
}
var Radio_1$1 = Radio_1;
const modes = ["static", "dynamic"];
const document = (config) => ({
  type: "string",
  description: "selected choice",
  example_data: config.choices.length > 1 ? config.choices[0] : ""
});
export { Radio_1$1 as Component, document, modes };
//# sourceMappingURL=index29.js.map
