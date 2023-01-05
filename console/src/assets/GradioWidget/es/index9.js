import { S as SvelteComponent, i as init, s as safe_not_equal, a6 as BlockTitle, e as element, a as space, t as text, b as attr, d as toggle_class, f as insert, g as append, l as listen, h as set_data, n as detach, c as create_component, m as mount_component, j as transition_in, k as transition_out, o as destroy_component, C as destroy_each, F as createEventDispatcher, Z as get_styles, P as Block, R as assign, T as StatusTracker, I as binding_callbacks, O as bind, U as get_spread_update, V as get_spread_object, L as add_flush_callback, K as bubble } from "./main.js";
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[10] = list[i];
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
function create_each_block(ctx) {
  let label_1;
  let input;
  let input_checked_value;
  let t0;
  let span;
  let t1_value = ctx[10] + "";
  let t1;
  let label_1_class_value;
  let mounted;
  let dispose;
  function change_handler() {
    return ctx[8](ctx[10]);
  }
  return {
    c() {
      label_1 = element("label");
      input = element("input");
      t0 = space();
      span = element("span");
      t1 = text(t1_value);
      input.disabled = ctx[2];
      input.checked = input_checked_value = ctx[0].includes(ctx[10]);
      attr(input, "type", "checkbox");
      attr(input, "name", "test");
      attr(input, "class", "gr-check-radio gr-checkbox");
      attr(span, "class", "ml-2");
      attr(label_1, "class", label_1_class_value = "gr-input-label flex items-center text-gray-700 text-sm space-x-2 border py-1.5 px-3 rounded-lg cursor-pointer bg-white shadow-sm checked:shadow-inner " + ctx[5]);
      toggle_class(label_1, "!cursor-not-allowed", ctx[2]);
    },
    m(target, anchor) {
      insert(target, label_1, anchor);
      append(label_1, input);
      append(label_1, t0);
      append(label_1, span);
      append(span, t1);
      if (!mounted) {
        dispose = listen(input, "change", change_handler);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty & 4) {
        input.disabled = ctx[2];
      }
      if (dirty & 3 && input_checked_value !== (input_checked_value = ctx[0].includes(ctx[10]))) {
        input.checked = input_checked_value;
      }
      if (dirty & 2 && t1_value !== (t1_value = ctx[10] + ""))
        set_data(t1, t1_value);
      if (dirty & 32 && label_1_class_value !== (label_1_class_value = "gr-input-label flex items-center text-gray-700 text-sm space-x-2 border py-1.5 px-3 rounded-lg cursor-pointer bg-white shadow-sm checked:shadow-inner " + ctx[5])) {
        attr(label_1, "class", label_1_class_value);
      }
      if (dirty & 36) {
        toggle_class(label_1, "!cursor-not-allowed", ctx[2]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(label_1);
      mounted = false;
      dispose();
    }
  };
}
function create_fragment$1(ctx) {
  let blocktitle;
  let t;
  let div;
  let current;
  blocktitle = new BlockTitle({
    props: {
      show_label: ctx[4],
      $$slots: { default: [create_default_slot$1] },
      $$scope: { ctx }
    }
  });
  let each_value = ctx[1];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
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
      attr(div, "data-testid", "checkbox-group");
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
      if (dirty & 8200) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (dirty & 103) {
        each_value = ctx2[1];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(div, null);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
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
      destroy_each(each_blocks, detaching);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let item_container;
  let { value = [] } = $$props;
  let { style = {} } = $$props;
  let { choices } = $$props;
  let { disabled = false } = $$props;
  let { label } = $$props;
  let { show_label } = $$props;
  const dispatch = createEventDispatcher();
  const toggleChoice = (choice) => {
    if (value.includes(choice)) {
      value.splice(value.indexOf(choice), 1);
    } else {
      value.push(choice);
    }
    dispatch("change", value);
    $$invalidate(0, value);
  };
  const change_handler = (choice) => toggleChoice(choice);
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
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 128) {
      $$invalidate(5, { item_container } = get_styles(style, ["item_container"]), item_container);
    }
  };
  return [
    value,
    choices,
    disabled,
    label,
    show_label,
    item_container,
    toggleChoice,
    style,
    change_handler
  ];
}
class CheckboxGroup extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      value: 0,
      style: 7,
      choices: 1,
      disabled: 2,
      label: 3,
      show_label: 4
    });
  }
}
function create_default_slot(ctx) {
  let statustracker;
  let t;
  let checkboxgroup;
  let updating_value;
  let current;
  const statustracker_spread_levels = [ctx[8]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  function checkboxgroup_value_binding(value) {
    ctx[9](value);
  }
  let checkboxgroup_props = {
    choices: ctx[3],
    label: ctx[6],
    style: ctx[4],
    show_label: ctx[7],
    disabled: ctx[5] === "static"
  };
  if (ctx[0] !== void 0) {
    checkboxgroup_props.value = ctx[0];
  }
  checkboxgroup = new CheckboxGroup({ props: checkboxgroup_props });
  binding_callbacks.push(() => bind(checkboxgroup, "value", checkboxgroup_value_binding));
  checkboxgroup.$on("change", ctx[10]);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t = space();
      create_component(checkboxgroup.$$.fragment);
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      mount_component(checkboxgroup, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 256 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[8])]) : {};
      statustracker.$set(statustracker_changes);
      const checkboxgroup_changes = {};
      if (dirty & 8)
        checkboxgroup_changes.choices = ctx2[3];
      if (dirty & 64)
        checkboxgroup_changes.label = ctx2[6];
      if (dirty & 16)
        checkboxgroup_changes.style = ctx2[4];
      if (dirty & 128)
        checkboxgroup_changes.show_label = ctx2[7];
      if (dirty & 32)
        checkboxgroup_changes.disabled = ctx2[5] === "static";
      if (!updating_value && dirty & 1) {
        updating_value = true;
        checkboxgroup_changes.value = ctx2[0];
        add_flush_callback(() => updating_value = false);
      }
      checkboxgroup.$set(checkboxgroup_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(checkboxgroup.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(checkboxgroup.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      destroy_component(checkboxgroup, detaching);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      visible: ctx[2],
      elem_id: ctx[1],
      type: "fieldset",
      disable: typeof ctx[4].container === "boolean" && !ctx[4].container,
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
      if (dirty & 4)
        block_changes.visible = ctx2[2];
      if (dirty & 2)
        block_changes.elem_id = ctx2[1];
      if (dirty & 16)
        block_changes.disable = typeof ctx2[4].container === "boolean" && !ctx2[4].container;
      if (dirty & 2553) {
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
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = [] } = $$props;
  let { choices } = $$props;
  let { style = {} } = $$props;
  let { mode } = $$props;
  let { label = "Checkbox Group" } = $$props;
  let { show_label } = $$props;
  let { loading_status } = $$props;
  function checkboxgroup_value_binding(value$1) {
    value = value$1;
    $$invalidate(0, value);
  }
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(1, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(2, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("choices" in $$props2)
      $$invalidate(3, choices = $$props2.choices);
    if ("style" in $$props2)
      $$invalidate(4, style = $$props2.style);
    if ("mode" in $$props2)
      $$invalidate(5, mode = $$props2.mode);
    if ("label" in $$props2)
      $$invalidate(6, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(7, show_label = $$props2.show_label);
    if ("loading_status" in $$props2)
      $$invalidate(8, loading_status = $$props2.loading_status);
  };
  return [
    value,
    elem_id,
    visible,
    choices,
    style,
    mode,
    label,
    show_label,
    loading_status,
    checkboxgroup_value_binding,
    change_handler
  ];
}
class CheckboxGroup_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 1,
      visible: 2,
      value: 0,
      choices: 3,
      style: 4,
      mode: 5,
      label: 6,
      show_label: 7,
      loading_status: 8
    });
  }
}
var CheckboxGroup_1$1 = CheckboxGroup_1;
const modes = ["static", "dynamic"];
const document = (config) => ({
  type: "Array<string>",
  description: "list of selected choices",
  example_data: config.choices.length ? [config.choices[0]] : []
});
export { CheckboxGroup_1$1 as Component, document, modes };
//# sourceMappingURL=index9.js.map
