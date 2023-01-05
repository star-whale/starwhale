import { S as SvelteComponent, i as init, s as safe_not_equal, ad as add_render_callback, P as Block, c as create_component, m as mount_component, l as listen, j as transition_in, k as transition_out, o as destroy_component, Z as get_styles, R as assign, T as StatusTracker, a as space, B as empty, f as insert, U as get_spread_update, V as get_spread_object, D as group_outros, E as check_outros, n as detach, a9 as tick, I as binding_callbacks, e as element, b as attr, M as src_url_equal, Y as set_style, d as toggle_class, g as append, t as text, h as set_data, C as destroy_each, A as run_all, ah as add_resize_listener, x as noop } from "./main.js";
import { B as BlockLabel } from "./BlockLabel.js";
import { M as ModifyUpload } from "./ModifyUpload.js";
import { n as normalise_file } from "./utils.js";
import { I as Image } from "./Image2.js";
var Gallery_svelte_svelte_type_style_lang = "";
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[30] = list[i][0];
  child_ctx[31] = list[i][1];
  child_ctx[33] = i;
  return child_ctx;
}
function get_each_context_1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[30] = list[i];
  child_ctx[34] = list;
  child_ctx[33] = i;
  return child_ctx;
}
function create_if_block_5(ctx) {
  let blocklabel;
  let current;
  blocklabel = new BlockLabel({
    props: {
      show_label: ctx[1],
      Icon: Image,
      label: ctx[2] || "Gallery",
      disable: typeof ctx[6].container === "boolean" && !ctx[6].container
    }
  });
  return {
    c() {
      create_component(blocklabel.$$.fragment);
    },
    m(target, anchor) {
      mount_component(blocklabel, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const blocklabel_changes = {};
      if (dirty[0] & 2)
        blocklabel_changes.show_label = ctx2[1];
      if (dirty[0] & 4)
        blocklabel_changes.label = ctx2[2] || "Gallery";
      if (dirty[0] & 64)
        blocklabel_changes.disable = typeof ctx2[6].container === "boolean" && !ctx2[6].container;
      blocklabel.$set(blocklabel_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(blocklabel.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(blocklabel.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(blocklabel, detaching);
    }
  };
}
function create_else_block(ctx) {
  let t;
  let div;
  let current_block_type_index;
  let if_block1;
  let div_resize_listener;
  let current;
  let if_block0 = ctx[7] !== null && create_if_block_3(ctx);
  const if_block_creators = [create_if_block_1, create_else_block_1];
  const if_blocks = [];
  function select_block_type_1(ctx2, dirty) {
    if (ctx2[10].length === 0)
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type_1(ctx);
  if_block1 = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      if (if_block0)
        if_block0.c();
      t = space();
      div = element("div");
      if_block1.c();
      attr(div, "class", "overflow-y-auto h-full p-2");
      add_render_callback(() => ctx[27].call(div));
      toggle_class(div, "min-h-[350px]", ctx[6].height !== "auto");
      toggle_class(div, "max-h-[55vh]", ctx[6].height !== "auto");
      toggle_class(div, "xl:min-h-[450px]", ctx[6].height !== "auto");
    },
    m(target, anchor) {
      if (if_block0)
        if_block0.m(target, anchor);
      insert(target, t, anchor);
      insert(target, div, anchor);
      if_blocks[current_block_type_index].m(div, null);
      div_resize_listener = add_resize_listener(div, ctx[27].bind(div));
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[7] !== null) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
          if (dirty[0] & 128) {
            transition_in(if_block0, 1);
          }
        } else {
          if_block0 = create_if_block_3(ctx2);
          if_block0.c();
          transition_in(if_block0, 1);
          if_block0.m(t.parentNode, t);
        }
      } else if (if_block0) {
        group_outros();
        transition_out(if_block0, 1, 1, () => {
          if_block0 = null;
        });
        check_outros();
      }
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type_1(ctx2);
      if (current_block_type_index === previous_block_index) {
        if_blocks[current_block_type_index].p(ctx2, dirty);
      } else {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block1 = if_blocks[current_block_type_index];
        if (!if_block1) {
          if_block1 = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block1.c();
        } else {
          if_block1.p(ctx2, dirty);
        }
        transition_in(if_block1, 1);
        if_block1.m(div, null);
      }
      if (dirty[0] & 64) {
        toggle_class(div, "min-h-[350px]", ctx2[6].height !== "auto");
      }
      if (dirty[0] & 64) {
        toggle_class(div, "max-h-[55vh]", ctx2[6].height !== "auto");
      }
      if (dirty[0] & 64) {
        toggle_class(div, "xl:min-h-[450px]", ctx2[6].height !== "auto");
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block0);
      transition_in(if_block1);
      current = true;
    },
    o(local) {
      transition_out(if_block0);
      transition_out(if_block1);
      current = false;
    },
    d(detaching) {
      if (if_block0)
        if_block0.d(detaching);
      if (detaching)
        detach(t);
      if (detaching)
        detach(div);
      if_blocks[current_block_type_index].d();
      div_resize_listener();
    }
  };
}
function create_if_block(ctx) {
  let div1;
  let div0;
  let image;
  let current;
  image = new Image({});
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      create_component(image.$$.fragment);
      attr(div0, "class", "h-5 dark:text-white opacity-50");
      attr(div1, "class", "h-full min-h-[15rem] flex justify-center items-center");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      mount_component(image, div0, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(image.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(image.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(image);
    }
  };
}
function create_if_block_3(ctx) {
  let div1;
  let modifyupload;
  let t0;
  let img;
  let img_src_value;
  let img_alt_value;
  let img_title_value;
  let t1;
  let t2;
  let div0;
  let current;
  let mounted;
  let dispose;
  modifyupload = new ModifyUpload({});
  modifyupload.$on("clear", ctx[21]);
  let if_block = ctx[10][ctx[7]][1] && create_if_block_4(ctx);
  let each_value_1 = ctx[10];
  let each_blocks = [];
  for (let i = 0; i < each_value_1.length; i += 1) {
    each_blocks[i] = create_each_block_1(get_each_context_1(ctx, each_value_1, i));
  }
  return {
    c() {
      div1 = element("div");
      create_component(modifyupload.$$.fragment);
      t0 = space();
      img = element("img");
      t1 = space();
      if (if_block)
        if_block.c();
      t2 = space();
      div0 = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(img, "class", "w-full object-contain h-[calc(100%-50px)");
      if (!src_url_equal(img.src, img_src_value = ctx[10][ctx[7]][0].data))
        attr(img, "src", img_src_value);
      attr(img, "alt", img_alt_value = ctx[10][ctx[7]][1] || "");
      attr(img, "title", img_title_value = ctx[10][ctx[7]][1] || null);
      set_style(img, "height", "calc(100% - " + (ctx[10][ctx[7]][1] ? "80px" : "60px") + ")");
      attr(div0, "class", "absolute h-[60px] overflow-x-scroll scroll-hide w-full bottom-0 flex gap-1.5 items-center py-2 text-sm px-3 justify-center");
      attr(div1, "class", "absolute group inset-0 z-10 flex flex-col bg-white/90 dark:bg-gray-900 backdrop-blur h-full");
      toggle_class(div1, "min-h-[350px]", ctx[6].height !== "auto");
      toggle_class(div1, "max-h-[55vh]", ctx[6].height !== "auto");
      toggle_class(div1, "xl:min-h-[450px]", ctx[6].height !== "auto");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      mount_component(modifyupload, div1, null);
      append(div1, t0);
      append(div1, img);
      append(div1, t1);
      if (if_block)
        if_block.m(div1, null);
      append(div1, t2);
      append(div1, div0);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div0, null);
      }
      ctx[25](div0);
      current = true;
      if (!mounted) {
        dispose = [
          listen(img, "click", ctx[22]),
          listen(div1, "keydown", ctx[16])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (!current || dirty[0] & 1152 && !src_url_equal(img.src, img_src_value = ctx2[10][ctx2[7]][0].data)) {
        attr(img, "src", img_src_value);
      }
      if (!current || dirty[0] & 1152 && img_alt_value !== (img_alt_value = ctx2[10][ctx2[7]][1] || "")) {
        attr(img, "alt", img_alt_value);
      }
      if (!current || dirty[0] & 1152 && img_title_value !== (img_title_value = ctx2[10][ctx2[7]][1] || null)) {
        attr(img, "title", img_title_value);
      }
      if (!current || dirty[0] & 1152) {
        set_style(img, "height", "calc(100% - " + (ctx2[10][ctx2[7]][1] ? "80px" : "60px") + ")");
      }
      if (ctx2[10][ctx2[7]][1]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block_4(ctx2);
          if_block.c();
          if_block.m(div1, t2);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty[0] & 3200) {
        each_value_1 = ctx2[10];
        let i;
        for (i = 0; i < each_value_1.length; i += 1) {
          const child_ctx = get_each_context_1(ctx2, each_value_1, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block_1(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(div0, null);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value_1.length;
      }
      if (dirty[0] & 64) {
        toggle_class(div1, "min-h-[350px]", ctx2[6].height !== "auto");
      }
      if (dirty[0] & 64) {
        toggle_class(div1, "max-h-[55vh]", ctx2[6].height !== "auto");
      }
      if (dirty[0] & 64) {
        toggle_class(div1, "xl:min-h-[450px]", ctx2[6].height !== "auto");
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(modifyupload.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(modifyupload.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(modifyupload);
      if (if_block)
        if_block.d();
      destroy_each(each_blocks, detaching);
      ctx[25](null);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block_4(ctx) {
  let div1;
  let div0;
  let t_value = ctx[10][ctx[7]][1] + "";
  let t;
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      t = text(t_value);
      attr(div0, "class", "dark:text-gray-200 font-semibold px-3 py-1 max-w-full truncate");
      attr(div1, "class", "bottom-[50px] absolute z-[5] flex justify-center w-full");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      append(div0, t);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 1152 && t_value !== (t_value = ctx2[10][ctx2[7]][1] + ""))
        set_data(t, t_value);
    },
    d(detaching) {
      if (detaching)
        detach(div1);
    }
  };
}
function create_each_block_1(ctx) {
  let button;
  let img;
  let img_src_value;
  let img_title_value;
  let img_alt_value;
  let t;
  let button_class_value;
  let i = ctx[33];
  let mounted;
  let dispose;
  const assign_button = () => ctx[23](button, i);
  const unassign_button = () => ctx[23](null, i);
  function click_handler_1() {
    return ctx[24](ctx[33]);
  }
  return {
    c() {
      button = element("button");
      img = element("img");
      t = space();
      attr(img, "class", "h-full w-full overflow-hidden object-contain");
      if (!src_url_equal(img.src, img_src_value = ctx[30][0].data))
        attr(img, "src", img_src_value);
      attr(img, "title", img_title_value = ctx[30][1] || null);
      attr(img, "alt", img_alt_value = ctx[30][1] || null);
      attr(button, "class", button_class_value = "gallery-item !flex-none !h-9 !w-9 transition-all duration-75 " + (ctx[7] === ctx[33] ? "!ring-2 !ring-orange-500 hover:!ring-orange-500" : "scale-90 transform") + " svelte-1g9btlg");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, img);
      append(button, t);
      assign_button();
      if (!mounted) {
        dispose = listen(button, "click", click_handler_1);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty[0] & 1024 && !src_url_equal(img.src, img_src_value = ctx[30][0].data)) {
        attr(img, "src", img_src_value);
      }
      if (dirty[0] & 1024 && img_title_value !== (img_title_value = ctx[30][1] || null)) {
        attr(img, "title", img_title_value);
      }
      if (dirty[0] & 1024 && img_alt_value !== (img_alt_value = ctx[30][1] || null)) {
        attr(img, "alt", img_alt_value);
      }
      if (dirty[0] & 128 && button_class_value !== (button_class_value = "gallery-item !flex-none !h-9 !w-9 transition-all duration-75 " + (ctx[7] === ctx[33] ? "!ring-2 !ring-orange-500 hover:!ring-orange-500" : "scale-90 transform") + " svelte-1g9btlg")) {
        attr(button, "class", button_class_value);
      }
      if (i !== ctx[33]) {
        unassign_button();
        i = ctx[33];
        assign_button();
      }
    },
    d(detaching) {
      if (detaching)
        detach(button);
      unassign_button();
      mounted = false;
      dispose();
    }
  };
}
function create_else_block_1(ctx) {
  let div;
  let div_class_value;
  let each_value = ctx[10];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  return {
    c() {
      div = element("div");
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      attr(div, "class", div_class_value = "grid gap-2 " + ctx[13] + " svelte-1g9btlg");
      toggle_class(div, "pt-6", ctx[1]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(div, null);
      }
    },
    p(ctx2, dirty) {
      if (dirty[0] & 17536) {
        each_value = ctx2[10];
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
      if (dirty[0] & 8192 && div_class_value !== (div_class_value = "grid gap-2 " + ctx2[13] + " svelte-1g9btlg")) {
        attr(div, "class", div_class_value);
      }
      if (dirty[0] & 8194) {
        toggle_class(div, "pt-6", ctx2[1]);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_each(each_blocks, detaching);
    }
  };
}
function create_if_block_1(ctx) {
  let div1;
  let div0;
  let image;
  let current;
  image = new Image({});
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      create_component(image.$$.fragment);
      attr(div0, "class", "h-5 dark:text-white opacity-50");
      attr(div1, "class", "h-full min-h-[15rem] flex justify-center items-center");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      mount_component(image, div0, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(image.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(image.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(image);
    }
  };
}
function create_if_block_2(ctx) {
  let div1;
  let div0;
  let t_value = ctx[31] + "";
  let t;
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      t = text(t_value);
      attr(div0, "class", "bg-gray-50 dark:bg-gray-700 dark:text-gray-200 text-xs border-t border-l dark:border-gray-600 font-semibold px-3 py-1 rounded-tl-lg group-hover:opacity-50 max-w-full truncate");
      attr(div1, "class", "bottom-0 absolute z-[5] flex justify-end w-full");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      append(div0, t);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 1024 && t_value !== (t_value = ctx2[31] + ""))
        set_data(t, t_value);
    },
    d(detaching) {
      if (detaching)
        detach(div1);
    }
  };
}
function create_each_block(ctx) {
  let button;
  let img;
  let img_alt_value;
  let img_src_value;
  let t0;
  let t1;
  let mounted;
  let dispose;
  let if_block = ctx[31] && create_if_block_2(ctx);
  function click_handler_2() {
    return ctx[26](ctx[33]);
  }
  return {
    c() {
      button = element("button");
      img = element("img");
      t0 = space();
      if (if_block)
        if_block.c();
      t1 = space();
      attr(img, "alt", img_alt_value = ctx[31] || "");
      attr(img, "class", "h-full w-full overflow-hidden object-contain");
      if (!src_url_equal(img.src, img_src_value = typeof ctx[30] === "string" ? ctx[30] : ctx[30].data))
        attr(img, "src", img_src_value);
      attr(button, "class", "gallery-item group svelte-1g9btlg");
    },
    m(target, anchor) {
      insert(target, button, anchor);
      append(button, img);
      append(button, t0);
      if (if_block)
        if_block.m(button, null);
      append(button, t1);
      if (!mounted) {
        dispose = listen(button, "click", click_handler_2);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty[0] & 1024 && img_alt_value !== (img_alt_value = ctx[31] || "")) {
        attr(img, "alt", img_alt_value);
      }
      if (dirty[0] & 1024 && !src_url_equal(img.src, img_src_value = typeof ctx[30] === "string" ? ctx[30] : ctx[30].data)) {
        attr(img, "src", img_src_value);
      }
      if (ctx[31]) {
        if (if_block) {
          if_block.p(ctx, dirty);
        } else {
          if_block = create_if_block_2(ctx);
          if_block.c();
          if_block.m(button, t1);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
    d(detaching) {
      if (detaching)
        detach(button);
      if (if_block)
        if_block.d();
      mounted = false;
      dispose();
    }
  };
}
function create_default_slot(ctx) {
  let statustracker;
  let t0;
  let t1;
  let current_block_type_index;
  let if_block1;
  let if_block1_anchor;
  let current;
  const statustracker_spread_levels = [ctx[0]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  let if_block0 = ctx[1] && create_if_block_5(ctx);
  const if_block_creators = [create_if_block, create_else_block];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[5] === null || ctx2[10] === null)
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block1 = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t0 = space();
      if (if_block0)
        if_block0.c();
      t1 = space();
      if_block1.c();
      if_block1_anchor = empty();
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t0, anchor);
      if (if_block0)
        if_block0.m(target, anchor);
      insert(target, t1, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block1_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty[0] & 1 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[0])]) : {};
      statustracker.$set(statustracker_changes);
      if (ctx2[1]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
          if (dirty[0] & 2) {
            transition_in(if_block0, 1);
          }
        } else {
          if_block0 = create_if_block_5(ctx2);
          if_block0.c();
          transition_in(if_block0, 1);
          if_block0.m(t1.parentNode, t1);
        }
      } else if (if_block0) {
        group_outros();
        transition_out(if_block0, 1, 1, () => {
          if_block0 = null;
        });
        check_outros();
      }
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type(ctx2);
      if (current_block_type_index === previous_block_index) {
        if_blocks[current_block_type_index].p(ctx2, dirty);
      } else {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block1 = if_blocks[current_block_type_index];
        if (!if_block1) {
          if_block1 = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block1.c();
        } else {
          if_block1.p(ctx2, dirty);
        }
        transition_in(if_block1, 1);
        if_block1.m(if_block1_anchor.parentNode, if_block1_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(if_block0);
      transition_in(if_block1);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(if_block0);
      transition_out(if_block1);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t0);
      if (if_block0)
        if_block0.d(detaching);
      if (detaching)
        detach(t1);
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block1_anchor);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  let mounted;
  let dispose;
  add_render_callback(ctx[20]);
  block = new Block({
    props: {
      visible: ctx[4],
      variant: "solid",
      color: "grey",
      padding: false,
      elem_id: ctx[3],
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
      if (!mounted) {
        dispose = listen(window, "resize", ctx[20]);
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      const block_changes = {};
      if (dirty[0] & 16)
        block_changes.visible = ctx2[4];
      if (dirty[0] & 8)
        block_changes.elem_id = ctx2[3];
      if (dirty[0] & 64)
        block_changes.disable = typeof ctx2[6].container === "boolean" && !ctx2[6].container;
      if (dirty[0] & 64999 | dirty[1] & 16) {
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
      mounted = false;
      dispose();
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let _value;
  let previous;
  let next;
  let can_zoom;
  let classes;
  let { loading_status } = $$props;
  let { show_label } = $$props;
  let { label } = $$props;
  let { root } = $$props;
  let { root_url } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = null } = $$props;
  let { style = {} } = $$props;
  let prevValue = null;
  let selected_image = null;
  function on_keydown(e) {
    switch (e.code) {
      case "Escape":
        e.preventDefault();
        $$invalidate(7, selected_image = null);
        break;
      case "ArrowLeft":
        e.preventDefault();
        $$invalidate(7, selected_image = previous);
        break;
      case "ArrowRight":
        e.preventDefault();
        $$invalidate(7, selected_image = next);
        break;
    }
  }
  let el = [];
  let container;
  async function scroll_to_img(index) {
    if (typeof index !== "number")
      return;
    await tick();
    el[index].focus();
    const { left: container_left, width: container_width } = container.getBoundingClientRect();
    const { left, width } = el[index].getBoundingClientRect();
    const relative_left = left - container_left;
    const pos = relative_left + width / 2 - container_width / 2 + container.scrollLeft;
    container.scrollTo({
      left: pos < 0 ? 0 : pos,
      behavior: "smooth"
    });
  }
  let height = 0;
  let window_height = 0;
  function onwindowresize() {
    $$invalidate(9, window_height = window.innerHeight);
  }
  const clear_handler = () => $$invalidate(7, selected_image = null);
  const click_handler = () => $$invalidate(7, selected_image = next);
  function button_binding($$value, i) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      el[i] = $$value;
      $$invalidate(11, el);
    });
  }
  const click_handler_1 = (i) => $$invalidate(7, selected_image = i);
  function div0_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      container = $$value;
      $$invalidate(12, container);
    });
  }
  const click_handler_2 = (i) => $$invalidate(7, selected_image = can_zoom ? i : selected_image);
  function div_elementresize_handler() {
    height = this.clientHeight;
    $$invalidate(8, height);
  }
  $$self.$$set = ($$props2) => {
    if ("loading_status" in $$props2)
      $$invalidate(0, loading_status = $$props2.loading_status);
    if ("show_label" in $$props2)
      $$invalidate(1, show_label = $$props2.show_label);
    if ("label" in $$props2)
      $$invalidate(2, label = $$props2.label);
    if ("root" in $$props2)
      $$invalidate(17, root = $$props2.root);
    if ("root_url" in $$props2)
      $$invalidate(18, root_url = $$props2.root_url);
    if ("elem_id" in $$props2)
      $$invalidate(3, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(4, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(5, value = $$props2.value);
    if ("style" in $$props2)
      $$invalidate(6, style = $$props2.style);
  };
  $$self.$$.update = () => {
    var _a, _b, _c;
    if ($$self.$$.dirty[0] & 393248) {
      $$invalidate(10, _value = value === null ? null : value.map((img) => Array.isArray(img) ? [normalise_file(img[0], root_url != null ? root_url : root), img[1]] : [normalise_file(img, root_url != null ? root_url : root), null]));
    }
    if ($$self.$$.dirty[0] & 524320) {
      if (prevValue !== value) {
        $$invalidate(7, selected_image = null);
        $$invalidate(19, prevValue = value);
      }
    }
    if ($$self.$$.dirty[0] & 1152) {
      previous = ((selected_image != null ? selected_image : 0) + ((_a = _value == null ? void 0 : _value.length) != null ? _a : 0) - 1) % ((_b = _value == null ? void 0 : _value.length) != null ? _b : 0);
    }
    if ($$self.$$.dirty[0] & 1152) {
      $$invalidate(15, next = ((selected_image != null ? selected_image : 0) + 1) % ((_c = _value == null ? void 0 : _value.length) != null ? _c : 0));
    }
    if ($$self.$$.dirty[0] & 128) {
      scroll_to_img(selected_image);
    }
    if ($$self.$$.dirty[0] & 768) {
      $$invalidate(14, can_zoom = window_height >= height);
    }
    if ($$self.$$.dirty[0] & 64) {
      $$invalidate(13, { classes } = get_styles(style, ["grid"]), classes);
    }
  };
  return [
    loading_status,
    show_label,
    label,
    elem_id,
    visible,
    value,
    style,
    selected_image,
    height,
    window_height,
    _value,
    el,
    container,
    classes,
    can_zoom,
    next,
    on_keydown,
    root,
    root_url,
    prevValue,
    onwindowresize,
    clear_handler,
    click_handler,
    button_binding,
    click_handler_1,
    div0_binding,
    click_handler_2,
    div_elementresize_handler
  ];
}
class Gallery extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      loading_status: 0,
      show_label: 1,
      label: 2,
      root: 17,
      root_url: 18,
      elem_id: 3,
      visible: 4,
      value: 5,
      style: 6
    }, null, [-1, -1]);
  }
}
var Gallery$1 = Gallery;
const modes = ["static"];
const document = (config) => ({
  type: "Array<{ name: string } | [{ name: string }, string]>",
  description: "list of objects with filename and optional caption"
});
export { Gallery$1 as Component, document, modes };
//# sourceMappingURL=index17.js.map
