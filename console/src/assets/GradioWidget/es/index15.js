import { S as SvelteComponent, i as init, s as safe_not_equal, c as create_component, a as space, B as empty, m as mount_component, f as insert, D as group_outros, k as transition_out, E as check_outros, j as transition_in, o as destroy_component, n as detach, e as element, b as attr, g as append, x as noop, t as text, h as set_data, C as destroy_each, F as createEventDispatcher, a9 as tick, I as binding_callbacks, O as bind, L as add_flush_callback, P as Block, Q as component_subscribe, X, R as assign, T as StatusTracker, U as get_spread_update, V as get_spread_object, K as bubble } from "./main.js";
import { B as BlockLabel } from "./BlockLabel.js";
import { F as File } from "./File.js";
import { U as Upload } from "./Upload.js";
import { M as ModifyUpload } from "./ModifyUpload.js";
import { n as normalise_file } from "./utils.js";
const prettyBytes = (bytes) => {
  let units = ["B", "KB", "MB", "GB", "PB"];
  let i = 0;
  while (bytes > 1024) {
    bytes /= 1024;
    i++;
  }
  let unit = units[i];
  return bytes.toFixed(1) + " " + unit;
};
const display_file_name = (value) => {
  var str;
  str = value.orig_name || value.name;
  if (str.length > 30) {
    return `${str.substr(0, 30)}...`;
  } else
    return str;
};
const download_files = (value) => {
  return value.data;
};
const display_file_size = (value) => {
  var total_size = 0;
  if (Array.isArray(value)) {
    for (var file of value) {
      if (file.size !== void 0)
        total_size += file.size;
    }
  } else {
    total_size = value.size || 0;
  }
  return prettyBytes(total_size);
};
function get_each_context$1(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[3] = list[i];
  return child_ctx;
}
function create_else_block_1$1(ctx) {
  let div1;
  let div0;
  let file;
  let current;
  file = new File({});
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      create_component(file.$$.fragment);
      attr(div0, "class", "h-5 dark:text-white opacity-50");
      attr(div1, "class", "h-full min-h-[15rem] flex justify-center items-center");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      mount_component(file, div0, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(file.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(file.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(file);
    }
  };
}
function create_if_block$2(ctx) {
  let div;
  let show_if;
  function select_block_type_1(ctx2, dirty) {
    if (dirty & 1)
      show_if = null;
    if (show_if == null)
      show_if = !!Array.isArray(ctx2[0]);
    if (show_if)
      return create_if_block_1$1;
    return create_else_block$2;
  }
  let current_block_type = select_block_type_1(ctx, -1);
  let if_block = current_block_type(ctx);
  return {
    c() {
      div = element("div");
      if_block.c();
      attr(div, "class", "file-preview overflow-y-scroll w-full max-h-60 flex flex-col justify-between mt-7 mb-7 dark:text-slate-200");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if_block.m(div, null);
    },
    p(ctx2, dirty) {
      if (current_block_type === (current_block_type = select_block_type_1(ctx2, dirty)) && if_block) {
        if_block.p(ctx2, dirty);
      } else {
        if_block.d(1);
        if_block = current_block_type(ctx2);
        if (if_block) {
          if_block.c();
          if_block.m(div, null);
        }
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
      if_block.d();
    }
  };
}
function create_else_block$2(ctx) {
  let div3;
  let div0;
  let t0_value = display_file_name(ctx[0]) + "";
  let t0;
  let t1;
  let div1;
  let t2_value = display_file_size(ctx[0]) + "";
  let t2;
  let t3;
  let div2;
  let a;
  let t4;
  let a_href_value;
  let a_download_value;
  return {
    c() {
      div3 = element("div");
      div0 = element("div");
      t0 = text(t0_value);
      t1 = space();
      div1 = element("div");
      t2 = text(t2_value);
      t3 = space();
      div2 = element("div");
      a = element("a");
      t4 = text("Download");
      attr(div0, "class", "file-name w-5/12 p-2");
      attr(div1, "class", "file-size w-3/12 p-2");
      attr(a, "href", a_href_value = download_files(ctx[0]));
      attr(a, "target", window.__is_colab__ ? "_blank" : null);
      attr(a, "download", a_download_value = window.__is_colab__ ? null : display_file_name(ctx[0]));
      attr(a, "class", "text-indigo-600 hover:underline dark:text-indigo-300");
      attr(div2, "class", "file-size w-3/12 p-2 hover:underline");
      attr(div3, "class", "flex flex-row");
    },
    m(target, anchor) {
      insert(target, div3, anchor);
      append(div3, div0);
      append(div0, t0);
      append(div3, t1);
      append(div3, div1);
      append(div1, t2);
      append(div3, t3);
      append(div3, div2);
      append(div2, a);
      append(a, t4);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t0_value !== (t0_value = display_file_name(ctx2[0]) + ""))
        set_data(t0, t0_value);
      if (dirty & 1 && t2_value !== (t2_value = display_file_size(ctx2[0]) + ""))
        set_data(t2, t2_value);
      if (dirty & 1 && a_href_value !== (a_href_value = download_files(ctx2[0]))) {
        attr(a, "href", a_href_value);
      }
      if (dirty & 1 && a_download_value !== (a_download_value = window.__is_colab__ ? null : display_file_name(ctx2[0]))) {
        attr(a, "download", a_download_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(div3);
    }
  };
}
function create_if_block_1$1(ctx) {
  let each_1_anchor;
  let each_value = ctx[0];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block$1(get_each_context$1(ctx, each_value, i));
  }
  return {
    c() {
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      each_1_anchor = empty();
    },
    m(target, anchor) {
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(target, anchor);
      }
      insert(target, each_1_anchor, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 1) {
        each_value = ctx2[0];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context$1(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block$1(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(each_1_anchor.parentNode, each_1_anchor);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
    },
    d(detaching) {
      destroy_each(each_blocks, detaching);
      if (detaching)
        detach(each_1_anchor);
    }
  };
}
function create_each_block$1(ctx) {
  let div3;
  let div0;
  let t0_value = display_file_name(ctx[3]) + "";
  let t0;
  let t1;
  let div1;
  let t2_value = display_file_size(ctx[3]) + "";
  let t2;
  let t3;
  let div2;
  let a;
  let t4;
  let a_href_value;
  let a_target_value;
  let a_download_value;
  let t5;
  return {
    c() {
      div3 = element("div");
      div0 = element("div");
      t0 = text(t0_value);
      t1 = space();
      div1 = element("div");
      t2 = text(t2_value);
      t3 = space();
      div2 = element("div");
      a = element("a");
      t4 = text("Download");
      t5 = space();
      attr(div0, "class", "file-name p-2");
      attr(div1, "class", "file-size p-2");
      attr(a, "href", a_href_value = download_files(ctx[3]));
      attr(a, "target", a_target_value = window.__is_colab__ ? "_blank" : null);
      attr(a, "download", a_download_value = window.__is_colab__ ? null : display_file_name(ctx[3]));
      attr(a, "class", "text-indigo-600 hover:underline dark:text-indigo-300");
      attr(div2, "class", "file-size w-3/12 p-2 hover:underline");
      attr(div3, "class", "flex flex-row w-full justify-between");
    },
    m(target, anchor) {
      insert(target, div3, anchor);
      append(div3, div0);
      append(div0, t0);
      append(div3, t1);
      append(div3, div1);
      append(div1, t2);
      append(div3, t3);
      append(div3, div2);
      append(div2, a);
      append(a, t4);
      append(div3, t5);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t0_value !== (t0_value = display_file_name(ctx2[3]) + ""))
        set_data(t0, t0_value);
      if (dirty & 1 && t2_value !== (t2_value = display_file_size(ctx2[3]) + ""))
        set_data(t2, t2_value);
      if (dirty & 1 && a_href_value !== (a_href_value = download_files(ctx2[3]))) {
        attr(a, "href", a_href_value);
      }
      if (dirty & 1 && a_download_value !== (a_download_value = window.__is_colab__ ? null : display_file_name(ctx2[3]))) {
        attr(a, "download", a_download_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(div3);
    }
  };
}
function create_fragment$2(ctx) {
  let blocklabel;
  let t;
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  blocklabel = new BlockLabel({
    props: {
      show_label: ctx[2],
      Icon: File,
      label: ctx[1] || "File"
    }
  });
  const if_block_creators = [create_if_block$2, create_else_block_1$1];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[0])
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(blocklabel.$$.fragment);
      t = space();
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(blocklabel, target, anchor);
      insert(target, t, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocklabel_changes = {};
      if (dirty & 4)
        blocklabel_changes.show_label = ctx2[2];
      if (dirty & 2)
        blocklabel_changes.label = ctx2[1] || "File";
      blocklabel.$set(blocklabel_changes);
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
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        } else {
          if_block.p(ctx2, dirty);
        }
        transition_in(if_block, 1);
        if_block.m(if_block_anchor.parentNode, if_block_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(blocklabel.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(blocklabel.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(blocklabel, detaching);
      if (detaching)
        detach(t);
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function instance$2($$self, $$props, $$invalidate) {
  let { value } = $$props;
  let { label } = $$props;
  let { show_label } = $$props;
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(2, show_label = $$props2.show_label);
  };
  return [value, label, show_label];
}
class File_1$2 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$2, create_fragment$2, safe_not_equal, { value: 0, label: 1, show_label: 2 });
  }
}
function get_each_context(ctx, list, i) {
  const child_ctx = ctx.slice();
  child_ctx[15] = list[i];
  return child_ctx;
}
function create_else_block$1(ctx) {
  let div;
  let modifyupload;
  let t;
  let show_if;
  let current;
  modifyupload = new ModifyUpload({ props: { absolute: true } });
  modifyupload.$on("clear", ctx[10]);
  function select_block_type_1(ctx2, dirty) {
    if (dirty & 1)
      show_if = null;
    if (show_if == null)
      show_if = !!Array.isArray(ctx2[0]);
    if (show_if)
      return create_if_block_2;
    return create_else_block_1;
  }
  let current_block_type = select_block_type_1(ctx, -1);
  let if_block = current_block_type(ctx);
  return {
    c() {
      div = element("div");
      create_component(modifyupload.$$.fragment);
      t = space();
      if_block.c();
      attr(div, "class", "file-preview overflow-y-scroll w-full max-h-60 flex flex-col justify-between mt-7 mb-7 dark:text-slate-200");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(modifyupload, div, null);
      append(div, t);
      if_block.m(div, null);
      current = true;
    },
    p(ctx2, dirty) {
      if (current_block_type === (current_block_type = select_block_type_1(ctx2, dirty)) && if_block) {
        if_block.p(ctx2, dirty);
      } else {
        if_block.d(1);
        if_block = current_block_type(ctx2);
        if (if_block) {
          if_block.c();
          if_block.m(div, null);
        }
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
        detach(div);
      destroy_component(modifyupload);
      if_block.d();
    }
  };
}
function create_if_block_1(ctx) {
  let upload;
  let updating_dragging;
  let current;
  function upload_dragging_binding_1(value) {
    ctx[13](value);
  }
  let upload_props = {
    filetype: ctx[8],
    file_count: ctx[6],
    $$slots: { default: [create_default_slot_1] },
    $$scope: { ctx }
  };
  if (ctx[7] !== void 0) {
    upload_props.dragging = ctx[7];
  }
  upload = new Upload({ props: upload_props });
  binding_callbacks.push(() => bind(upload, "dragging", upload_dragging_binding_1));
  upload.$on("load", ctx[9]);
  return {
    c() {
      create_component(upload.$$.fragment);
    },
    m(target, anchor) {
      mount_component(upload, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const upload_changes = {};
      if (dirty & 256)
        upload_changes.filetype = ctx2[8];
      if (dirty & 64)
        upload_changes.file_count = ctx2[6];
      if (dirty & 262158) {
        upload_changes.$$scope = { dirty, ctx: ctx2 };
      }
      if (!updating_dragging && dirty & 128) {
        updating_dragging = true;
        upload_changes.dragging = ctx2[7];
        add_flush_callback(() => updating_dragging = false);
      }
      upload.$set(upload_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(upload.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(upload.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(upload, detaching);
    }
  };
}
function create_if_block$1(ctx) {
  let upload;
  let updating_dragging;
  let current;
  function upload_dragging_binding(value) {
    ctx[12](value);
  }
  let upload_props = {
    filetype: ctx[8],
    $$slots: { default: [create_default_slot$1] },
    $$scope: { ctx }
  };
  if (ctx[7] !== void 0) {
    upload_props.dragging = ctx[7];
  }
  upload = new Upload({ props: upload_props });
  binding_callbacks.push(() => bind(upload, "dragging", upload_dragging_binding));
  upload.$on("load", ctx[9]);
  return {
    c() {
      create_component(upload.$$.fragment);
    },
    m(target, anchor) {
      mount_component(upload, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const upload_changes = {};
      if (dirty & 256)
        upload_changes.filetype = ctx2[8];
      if (dirty & 262158) {
        upload_changes.$$scope = { dirty, ctx: ctx2 };
      }
      if (!updating_dragging && dirty & 128) {
        updating_dragging = true;
        upload_changes.dragging = ctx2[7];
        add_flush_callback(() => updating_dragging = false);
      }
      upload.$set(upload_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(upload.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(upload.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(upload, detaching);
    }
  };
}
function create_else_block_1(ctx) {
  let div3;
  let div0;
  let t0_value = display_file_name(ctx[0]) + "";
  let t0;
  let t1;
  let div1;
  let t2_value = display_file_size(ctx[0]) + "";
  let t2;
  let t3;
  let div2;
  let a;
  let t4;
  let a_href_value;
  let a_download_value;
  return {
    c() {
      div3 = element("div");
      div0 = element("div");
      t0 = text(t0_value);
      t1 = space();
      div1 = element("div");
      t2 = text(t2_value);
      t3 = space();
      div2 = element("div");
      a = element("a");
      t4 = text("Download");
      attr(div0, "class", "file-name p-2");
      attr(div1, "class", "file-size p-2");
      attr(a, "href", a_href_value = download_files(ctx[0]));
      attr(a, "download", a_download_value = display_file_name(ctx[0]));
      attr(a, "class", "text-indigo-600 hover:underline dark:text-indigo-300");
      attr(div2, "class", "file-size p-2 hover:underline");
      attr(div3, "class", "flex flex-row");
    },
    m(target, anchor) {
      insert(target, div3, anchor);
      append(div3, div0);
      append(div0, t0);
      append(div3, t1);
      append(div3, div1);
      append(div1, t2);
      append(div3, t3);
      append(div3, div2);
      append(div2, a);
      append(a, t4);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t0_value !== (t0_value = display_file_name(ctx2[0]) + ""))
        set_data(t0, t0_value);
      if (dirty & 1 && t2_value !== (t2_value = display_file_size(ctx2[0]) + ""))
        set_data(t2, t2_value);
      if (dirty & 1 && a_href_value !== (a_href_value = download_files(ctx2[0]))) {
        attr(a, "href", a_href_value);
      }
      if (dirty & 1 && a_download_value !== (a_download_value = display_file_name(ctx2[0]))) {
        attr(a, "download", a_download_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(div3);
    }
  };
}
function create_if_block_2(ctx) {
  let each_1_anchor;
  let each_value = ctx[0];
  let each_blocks = [];
  for (let i = 0; i < each_value.length; i += 1) {
    each_blocks[i] = create_each_block(get_each_context(ctx, each_value, i));
  }
  return {
    c() {
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].c();
      }
      each_1_anchor = empty();
    },
    m(target, anchor) {
      for (let i = 0; i < each_blocks.length; i += 1) {
        each_blocks[i].m(target, anchor);
      }
      insert(target, each_1_anchor, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 1) {
        each_value = ctx2[0];
        let i;
        for (i = 0; i < each_value.length; i += 1) {
          const child_ctx = get_each_context(ctx2, each_value, i);
          if (each_blocks[i]) {
            each_blocks[i].p(child_ctx, dirty);
          } else {
            each_blocks[i] = create_each_block(child_ctx);
            each_blocks[i].c();
            each_blocks[i].m(each_1_anchor.parentNode, each_1_anchor);
          }
        }
        for (; i < each_blocks.length; i += 1) {
          each_blocks[i].d(1);
        }
        each_blocks.length = each_value.length;
      }
    },
    d(detaching) {
      destroy_each(each_blocks, detaching);
      if (detaching)
        detach(each_1_anchor);
    }
  };
}
function create_each_block(ctx) {
  let div3;
  let div0;
  let t0_value = display_file_name(ctx[15]) + "";
  let t0;
  let t1;
  let div1;
  let t2_value = display_file_size(ctx[15]) + "";
  let t2;
  let t3;
  let div2;
  let a;
  let t4;
  let a_href_value;
  let a_download_value;
  let t5;
  return {
    c() {
      div3 = element("div");
      div0 = element("div");
      t0 = text(t0_value);
      t1 = space();
      div1 = element("div");
      t2 = text(t2_value);
      t3 = space();
      div2 = element("div");
      a = element("a");
      t4 = text("Download");
      t5 = space();
      attr(div0, "class", "file-name w-5/12 p-2");
      attr(div1, "class", "file-size w-3/12 p-2");
      attr(a, "href", a_href_value = download_files(ctx[15]));
      attr(a, "download", a_download_value = display_file_name(ctx[15]));
      attr(a, "class", "text-indigo-600 hover:underline dark:text-indigo-300");
      attr(div2, "class", "file-size w-3/12 p-2 hover:underline");
      attr(div3, "class", "flex flex-row w-full justify-between");
    },
    m(target, anchor) {
      insert(target, div3, anchor);
      append(div3, div0);
      append(div0, t0);
      append(div3, t1);
      append(div3, div1);
      append(div1, t2);
      append(div3, t3);
      append(div3, div2);
      append(div2, a);
      append(a, t4);
      append(div3, t5);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t0_value !== (t0_value = display_file_name(ctx2[15]) + ""))
        set_data(t0, t0_value);
      if (dirty & 1 && t2_value !== (t2_value = display_file_size(ctx2[15]) + ""))
        set_data(t2, t2_value);
      if (dirty & 1 && a_href_value !== (a_href_value = download_files(ctx2[15]))) {
        attr(a, "href", a_href_value);
      }
      if (dirty & 1 && a_download_value !== (a_download_value = display_file_name(ctx2[15]))) {
        attr(a, "download", a_download_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(div3);
    }
  };
}
function create_default_slot_1(ctx) {
  let t0;
  let t1;
  let br0;
  let t2;
  let t3;
  let t4;
  let br1;
  let t5;
  let t6;
  return {
    c() {
      t0 = text(ctx[1]);
      t1 = space();
      br0 = element("br");
      t2 = text("- ");
      t3 = text(ctx[2]);
      t4 = text(" -");
      br1 = element("br");
      t5 = space();
      t6 = text(ctx[3]);
    },
    m(target, anchor) {
      insert(target, t0, anchor);
      insert(target, t1, anchor);
      insert(target, br0, anchor);
      insert(target, t2, anchor);
      insert(target, t3, anchor);
      insert(target, t4, anchor);
      insert(target, br1, anchor);
      insert(target, t5, anchor);
      insert(target, t6, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 2)
        set_data(t0, ctx2[1]);
      if (dirty & 4)
        set_data(t3, ctx2[2]);
      if (dirty & 8)
        set_data(t6, ctx2[3]);
    },
    d(detaching) {
      if (detaching)
        detach(t0);
      if (detaching)
        detach(t1);
      if (detaching)
        detach(br0);
      if (detaching)
        detach(t2);
      if (detaching)
        detach(t3);
      if (detaching)
        detach(t4);
      if (detaching)
        detach(br1);
      if (detaching)
        detach(t5);
      if (detaching)
        detach(t6);
    }
  };
}
function create_default_slot$1(ctx) {
  let t0;
  let t1;
  let br0;
  let t2;
  let t3;
  let t4;
  let br1;
  let t5;
  let t6;
  return {
    c() {
      t0 = text(ctx[1]);
      t1 = space();
      br0 = element("br");
      t2 = text("- ");
      t3 = text(ctx[2]);
      t4 = text(" -");
      br1 = element("br");
      t5 = space();
      t6 = text(ctx[3]);
    },
    m(target, anchor) {
      insert(target, t0, anchor);
      insert(target, t1, anchor);
      insert(target, br0, anchor);
      insert(target, t2, anchor);
      insert(target, t3, anchor);
      insert(target, t4, anchor);
      insert(target, br1, anchor);
      insert(target, t5, anchor);
      insert(target, t6, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 2)
        set_data(t0, ctx2[1]);
      if (dirty & 4)
        set_data(t3, ctx2[2]);
      if (dirty & 8)
        set_data(t6, ctx2[3]);
    },
    d(detaching) {
      if (detaching)
        detach(t0);
      if (detaching)
        detach(t1);
      if (detaching)
        detach(br0);
      if (detaching)
        detach(t2);
      if (detaching)
        detach(t3);
      if (detaching)
        detach(t4);
      if (detaching)
        detach(br1);
      if (detaching)
        detach(t5);
      if (detaching)
        detach(t6);
    }
  };
}
function create_fragment$1(ctx) {
  let blocklabel;
  let t;
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  blocklabel = new BlockLabel({
    props: {
      show_label: ctx[5],
      Icon: File,
      label: ctx[4] || "File"
    }
  });
  const if_block_creators = [create_if_block$1, create_if_block_1, create_else_block$1];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[0] === null && ctx2[6] === "single")
      return 0;
    if (ctx2[0] === null)
      return 1;
    return 2;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(blocklabel.$$.fragment);
      t = space();
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(blocklabel, target, anchor);
      insert(target, t, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocklabel_changes = {};
      if (dirty & 32)
        blocklabel_changes.show_label = ctx2[5];
      if (dirty & 16)
        blocklabel_changes.label = ctx2[4] || "File";
      blocklabel.$set(blocklabel_changes);
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
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        } else {
          if_block.p(ctx2, dirty);
        }
        transition_in(if_block, 1);
        if_block.m(if_block_anchor.parentNode, if_block_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(blocklabel.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(blocklabel.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(blocklabel, detaching);
      if (detaching)
        detach(t);
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function instance$1($$self, $$props, $$invalidate) {
  let { value } = $$props;
  let { drop_text = "Drop a file" } = $$props;
  let { or_text = "or" } = $$props;
  let { upload_text = "click to upload" } = $$props;
  let { label = "" } = $$props;
  let { show_label } = $$props;
  let { file_count } = $$props;
  let { file_types = ["file"] } = $$props;
  async function handle_upload({ detail }) {
    $$invalidate(0, value = detail);
    await tick();
    dispatch("change", value);
    dispatch("upload", detail);
  }
  function handle_clear({ detail }) {
    $$invalidate(0, value = null);
    dispatch("change", value);
    dispatch("clear");
  }
  const dispatch = createEventDispatcher();
  let accept_file_types = "";
  try {
    file_types.forEach((type) => $$invalidate(8, accept_file_types += type + "/*, "));
  } catch (err) {
    if (err instanceof TypeError) {
      dispatch("error", "Please set file_types to a list.");
    } else {
      throw err;
    }
  }
  let dragging = false;
  function upload_dragging_binding(value2) {
    dragging = value2;
    $$invalidate(7, dragging);
  }
  function upload_dragging_binding_1(value2) {
    dragging = value2;
    $$invalidate(7, dragging);
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("drop_text" in $$props2)
      $$invalidate(1, drop_text = $$props2.drop_text);
    if ("or_text" in $$props2)
      $$invalidate(2, or_text = $$props2.or_text);
    if ("upload_text" in $$props2)
      $$invalidate(3, upload_text = $$props2.upload_text);
    if ("label" in $$props2)
      $$invalidate(4, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(5, show_label = $$props2.show_label);
    if ("file_count" in $$props2)
      $$invalidate(6, file_count = $$props2.file_count);
    if ("file_types" in $$props2)
      $$invalidate(11, file_types = $$props2.file_types);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 128) {
      dispatch("drag", dragging);
    }
  };
  return [
    value,
    drop_text,
    or_text,
    upload_text,
    label,
    show_label,
    file_count,
    dragging,
    accept_file_types,
    handle_upload,
    handle_clear,
    file_types,
    upload_dragging_binding,
    upload_dragging_binding_1
  ];
}
class FileUpload extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      value: 0,
      drop_text: 1,
      or_text: 2,
      upload_text: 3,
      label: 4,
      show_label: 5,
      file_count: 6,
      file_types: 11
    });
  }
}
function create_else_block(ctx) {
  let file;
  let current;
  file = new File_1$2({
    props: {
      value: ctx[9],
      label: ctx[4],
      show_label: ctx[5]
    }
  });
  return {
    c() {
      create_component(file.$$.fragment);
    },
    m(target, anchor) {
      mount_component(file, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const file_changes = {};
      if (dirty & 512)
        file_changes.value = ctx2[9];
      if (dirty & 16)
        file_changes.label = ctx2[4];
      if (dirty & 32)
        file_changes.show_label = ctx2[5];
      file.$set(file_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(file.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(file.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(file, detaching);
    }
  };
}
function create_if_block(ctx) {
  let fileupload;
  let current;
  fileupload = new FileUpload({
    props: {
      label: ctx[4],
      show_label: ctx[5],
      value: ctx[9],
      file_count: ctx[6],
      file_types: ctx[7],
      drop_text: ctx[11]("interface.drop_file"),
      or_text: ctx[11]("or"),
      upload_text: ctx[11]("interface.click_to_upload")
    }
  });
  fileupload.$on("change", ctx[14]);
  fileupload.$on("drag", ctx[15]);
  fileupload.$on("change", ctx[16]);
  fileupload.$on("clear", ctx[17]);
  fileupload.$on("upload", ctx[18]);
  return {
    c() {
      create_component(fileupload.$$.fragment);
    },
    m(target, anchor) {
      mount_component(fileupload, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const fileupload_changes = {};
      if (dirty & 16)
        fileupload_changes.label = ctx2[4];
      if (dirty & 32)
        fileupload_changes.show_label = ctx2[5];
      if (dirty & 512)
        fileupload_changes.value = ctx2[9];
      if (dirty & 64)
        fileupload_changes.file_count = ctx2[6];
      if (dirty & 128)
        fileupload_changes.file_types = ctx2[7];
      if (dirty & 2048)
        fileupload_changes.drop_text = ctx2[11]("interface.drop_file");
      if (dirty & 2048)
        fileupload_changes.or_text = ctx2[11]("or");
      if (dirty & 2048)
        fileupload_changes.upload_text = ctx2[11]("interface.click_to_upload");
      fileupload.$set(fileupload_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(fileupload.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(fileupload.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(fileupload, detaching);
    }
  };
}
function create_default_slot(ctx) {
  let statustracker;
  let t;
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  const statustracker_spread_levels = [ctx[8]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  const if_block_creators = [create_if_block, create_else_block];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[3] === "dynamic")
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      create_component(statustracker.$$.fragment);
      t = space();
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      insert(target, t, anchor);
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 256 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[8])]) : {};
      statustracker.$set(statustracker_changes);
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
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        } else {
          if_block.p(ctx2, dirty);
        }
        transition_in(if_block, 1);
        if_block.m(if_block_anchor.parentNode, if_block_anchor);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
      if (detaching)
        detach(t);
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_fragment(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      visible: ctx[2],
      variant: ctx[3] === "dynamic" && ctx[0] === null ? "dashed" : "solid",
      color: ctx[10] ? "green" : "grey",
      padding: false,
      elem_id: ctx[1],
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
      if (dirty & 9)
        block_changes.variant = ctx2[3] === "dynamic" && ctx2[0] === null ? "dashed" : "solid";
      if (dirty & 1024)
        block_changes.color = ctx2[10] ? "green" : "grey";
      if (dirty & 2)
        block_changes.elem_id = ctx2[1];
      if (dirty & 528377) {
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
  let $_;
  component_subscribe($$self, X, ($$value) => $$invalidate(11, $_ = $$value));
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = null } = $$props;
  let { mode } = $$props;
  let { root } = $$props;
  let { label } = $$props;
  let { show_label } = $$props;
  let { file_count } = $$props;
  let { file_types = ["file"] } = $$props;
  let { root_url } = $$props;
  let { loading_status } = $$props;
  let _value;
  let dragging = false;
  const change_handler_1 = ({ detail }) => $$invalidate(0, value = detail);
  const drag_handler = ({ detail }) => $$invalidate(10, dragging = detail);
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  function clear_handler(event) {
    bubble.call(this, $$self, event);
  }
  function upload_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(1, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(2, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("mode" in $$props2)
      $$invalidate(3, mode = $$props2.mode);
    if ("root" in $$props2)
      $$invalidate(12, root = $$props2.root);
    if ("label" in $$props2)
      $$invalidate(4, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(5, show_label = $$props2.show_label);
    if ("file_count" in $$props2)
      $$invalidate(6, file_count = $$props2.file_count);
    if ("file_types" in $$props2)
      $$invalidate(7, file_types = $$props2.file_types);
    if ("root_url" in $$props2)
      $$invalidate(13, root_url = $$props2.root_url);
    if ("loading_status" in $$props2)
      $$invalidate(8, loading_status = $$props2.loading_status);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 12289) {
      $$invalidate(9, _value = normalise_file(value, root_url != null ? root_url : root));
    }
  };
  return [
    value,
    elem_id,
    visible,
    mode,
    label,
    show_label,
    file_count,
    file_types,
    loading_status,
    _value,
    dragging,
    $_,
    root,
    root_url,
    change_handler_1,
    drag_handler,
    change_handler,
    clear_handler,
    upload_handler
  ];
}
class File_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 1,
      visible: 2,
      value: 0,
      mode: 3,
      root: 12,
      label: 4,
      show_label: 5,
      file_count: 6,
      file_types: 7,
      root_url: 13,
      loading_status: 8
    });
  }
}
var File_1$1 = File_1;
const modes = ["static", "dynamic"];
const document = (config) => ({
  type: "{ name: string; data: string }",
  description: "file name and base64 data as an object",
  example_data: {
    name: "zip.zip",
    data: "data:@file/octet-stream;base64,UEsFBgAAAAAAAAAAAAAAAAAAAAAAAA=="
  }
});
export { File_1$1 as Component, document, modes };
//# sourceMappingURL=index15.js.map
