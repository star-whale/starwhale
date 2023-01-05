import { S as SvelteComponent, i as init, s as safe_not_equal, w as svg_element, b as attr, f as insert, g as append, x as noop, n as detach, e as element, a as space, t as text, c as create_component, M as src_url_equal, ad as add_render_callback, d as toggle_class, Y as set_style, m as mount_component, l as listen, z as prevent_default, W as stop_propagation, D as group_outros, k as transition_out, E as check_outros, j as transition_in, h as set_data, o as destroy_component, A as run_all, ar as globals, as as raf, a9 as tick, K as bubble, I as binding_callbacks, B as empty, F as createEventDispatcher, O as bind, L as add_flush_callback, P as Block, Q as component_subscribe, X, R as assign, T as StatusTracker, U as get_spread_update, V as get_spread_object } from "./main.js";
import { n as normalise_file } from "./utils.js";
import { U as Upload } from "./Upload.js";
import { M as ModifyUpload } from "./ModifyUpload.js";
import { B as BlockLabel } from "./BlockLabel.js";
import { U as Undo, W as Webcam } from "./Webcam.js";
function create_fragment$7(ctx) {
  let svg;
  let path;
  return {
    c() {
      svg = svg_element("svg");
      path = svg_element("path");
      attr(path, "d", "M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "viewBox", "0 0 24 24");
      attr(svg, "fill", "none");
      attr(svg, "stroke", "currentColor");
      attr(svg, "stroke-width", "1.5");
      attr(svg, "stroke-linecap", "round");
      attr(svg, "stroke-linejoin", "round");
    },
    m(target, anchor) {
      insert(target, svg, anchor);
      append(svg, path);
    },
    p: noop,
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(svg);
    }
  };
}
class Maximise extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$7, safe_not_equal, {});
  }
}
function create_fragment$6(ctx) {
  let svg;
  let rect0;
  let rect1;
  return {
    c() {
      svg = svg_element("svg");
      rect0 = svg_element("rect");
      rect1 = svg_element("rect");
      attr(rect0, "x", "6");
      attr(rect0, "y", "4");
      attr(rect0, "width", "4");
      attr(rect0, "height", "16");
      attr(rect1, "x", "14");
      attr(rect1, "y", "4");
      attr(rect1, "width", "4");
      attr(rect1, "height", "16");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "viewBox", "0 0 24 24");
      attr(svg, "fill", "none");
      attr(svg, "stroke", "currentColor");
      attr(svg, "stroke-width", "1.5");
      attr(svg, "stroke-linecap", "round");
      attr(svg, "stroke-linejoin", "round");
    },
    m(target, anchor) {
      insert(target, svg, anchor);
      append(svg, rect0);
      append(svg, rect1);
    },
    p: noop,
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(svg);
    }
  };
}
class Pause extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$6, safe_not_equal, {});
  }
}
function create_fragment$5(ctx) {
  let svg;
  let polygon;
  return {
    c() {
      svg = svg_element("svg");
      polygon = svg_element("polygon");
      attr(polygon, "points", "5 3 19 12 5 21 5 3");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "viewBox", "0 0 24 24");
      attr(svg, "fill", "none");
      attr(svg, "stroke", "currentColor");
      attr(svg, "stroke-width", "1.5");
      attr(svg, "stroke-linecap", "round");
      attr(svg, "stroke-linejoin", "round");
    },
    m(target, anchor) {
      insert(target, svg, anchor);
      append(svg, polygon);
    },
    p: noop,
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(svg);
    }
  };
}
class Play extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$5, safe_not_equal, {});
  }
}
function create_fragment$4(ctx) {
  let svg;
  let polygon;
  let rect;
  return {
    c() {
      svg = svg_element("svg");
      polygon = svg_element("polygon");
      rect = svg_element("rect");
      attr(polygon, "points", "23 7 16 12 23 17 23 7");
      attr(rect, "x", "1");
      attr(rect, "y", "5");
      attr(rect, "width", "15");
      attr(rect, "height", "14");
      attr(rect, "rx", "2");
      attr(rect, "ry", "2");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(svg, "width", "100%");
      attr(svg, "height", "100%");
      attr(svg, "viewBox", "0 0 24 24");
      attr(svg, "fill", "none");
      attr(svg, "stroke", "currentColor");
      attr(svg, "stroke-width", "1.5");
      attr(svg, "stroke-linecap", "round");
      attr(svg, "stroke-linejoin", "round");
      attr(svg, "class", "feather feather-video");
    },
    m(target, anchor) {
      insert(target, svg, anchor);
      append(svg, polygon);
      append(svg, rect);
    },
    p: noop,
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(svg);
    }
  };
}
class Video extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, null, create_fragment$4, safe_not_equal, {});
  }
}
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
const playable = () => {
  return true;
};
var Player_svelte_svelte_type_style_lang = "";
const { isNaN: isNaN_1 } = globals;
function create_else_block$3(ctx) {
  let pause;
  let current;
  pause = new Pause({});
  return {
    c() {
      create_component(pause.$$.fragment);
    },
    m(target, anchor) {
      mount_component(pause, target, anchor);
      current = true;
    },
    i(local) {
      if (current)
        return;
      transition_in(pause.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(pause.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(pause, detaching);
    }
  };
}
function create_if_block_1$1(ctx) {
  let play;
  let current;
  play = new Play({});
  return {
    c() {
      create_component(play.$$.fragment);
    },
    m(target, anchor) {
      mount_component(play, target, anchor);
      current = true;
    },
    i(local) {
      if (current)
        return;
      transition_in(play.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(play.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(play, detaching);
    }
  };
}
function create_if_block$3(ctx) {
  let undo;
  let current;
  undo = new Undo({});
  return {
    c() {
      create_component(undo.$$.fragment);
    },
    m(target, anchor) {
      mount_component(undo, target, anchor);
      current = true;
    },
    i(local) {
      if (current)
        return;
      transition_in(undo.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(undo.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(undo, detaching);
    }
  };
}
function create_fragment$3(ctx) {
  let div3;
  let video_1;
  let track;
  let video_1_src_value;
  let video_1_updating = false;
  let video_1_animationframe;
  let video_1_is_paused = true;
  let t0;
  let div2;
  let div1;
  let span0;
  let current_block_type_index;
  let if_block;
  let t1;
  let span1;
  let t2_value = format(ctx[2]) + "";
  let t2;
  let t3;
  let t4_value = format(ctx[3]) + "";
  let t4;
  let t5;
  let progress;
  let progress_value_value;
  let t6;
  let div0;
  let maximise;
  let current;
  let mounted;
  let dispose;
  function video_1_timeupdate_handler() {
    cancelAnimationFrame(video_1_animationframe);
    if (!video_1.paused) {
      video_1_animationframe = raf(video_1_timeupdate_handler);
      video_1_updating = true;
    }
    ctx[14].call(video_1);
  }
  const if_block_creators = [create_if_block$3, create_if_block_1$1, create_else_block$3];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[2] === ctx2[3])
      return 0;
    if (ctx2[4])
      return 1;
    return 2;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  maximise = new Maximise({});
  return {
    c() {
      div3 = element("div");
      video_1 = element("video");
      track = element("track");
      t0 = space();
      div2 = element("div");
      div1 = element("div");
      span0 = element("span");
      if_block.c();
      t1 = space();
      span1 = element("span");
      t2 = text(t2_value);
      t3 = text(" / ");
      t4 = text(t4_value);
      t5 = space();
      progress = element("progress");
      t6 = space();
      div0 = element("div");
      create_component(maximise.$$.fragment);
      attr(track, "kind", "captions");
      if (!src_url_equal(video_1.src, video_1_src_value = ctx[0]))
        attr(video_1, "src", video_1_src_value);
      attr(video_1, "preload", "auto");
      attr(video_1, "class", "w-full h-full object-contain bg-black svelte-1cgkd5k");
      if (ctx[3] === void 0)
        add_render_callback(() => ctx[15].call(video_1));
      toggle_class(video_1, "mirror", ctx[1]);
      attr(span0, "class", "w-6 cursor-pointer text-white flex justify-center svelte-1cgkd5k");
      attr(span1, "class", "font-mono shrink-0 text-xs mx-3 text-white svelte-1cgkd5k");
      progress.value = progress_value_value = ctx[2] / ctx[3] || 0;
      attr(progress, "class", "rounded h-2 w-full mx-3 svelte-1cgkd5k");
      attr(div0, "class", "w-6 cursor-pointer text-white");
      attr(div1, "class", "flex w-full justify-space h-full items-center px-1.5 ");
      attr(div2, "class", "wrap absolute bottom-0 transition duration-500 m-1.5 bg-slate-800 px-1 py-2.5 rounded-md svelte-1cgkd5k");
      set_style(div2, "opacity", ctx[3] && ctx[6] ? 1 : 0);
    },
    m(target, anchor) {
      insert(target, div3, anchor);
      append(div3, video_1);
      append(video_1, track);
      ctx[17](video_1);
      append(div3, t0);
      append(div3, div2);
      append(div2, div1);
      append(div1, span0);
      if_blocks[current_block_type_index].m(span0, null);
      append(div1, t1);
      append(div1, span1);
      append(span1, t2);
      append(span1, t3);
      append(span1, t4);
      append(div1, t5);
      append(div1, progress);
      append(div1, t6);
      append(div1, div0);
      mount_component(maximise, div0, null);
      current = true;
      if (!mounted) {
        dispose = [
          listen(video_1, "mousemove", ctx[7]),
          listen(video_1, "click", ctx[9]),
          listen(video_1, "play", ctx[11]),
          listen(video_1, "pause", ctx[12]),
          listen(video_1, "ended", ctx[13]),
          listen(video_1, "timeupdate", video_1_timeupdate_handler),
          listen(video_1, "durationchange", ctx[15]),
          listen(video_1, "play", ctx[16]),
          listen(video_1, "pause", ctx[16]),
          listen(span0, "click", ctx[9]),
          listen(progress, "mousemove", ctx[8]),
          listen(progress, "touchmove", prevent_default(ctx[8])),
          listen(progress, "click", stop_propagation(prevent_default(ctx[10]))),
          listen(div0, "click", ctx[18]),
          listen(div2, "mousemove", ctx[7])
        ];
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (!current || dirty & 1 && !src_url_equal(video_1.src, video_1_src_value = ctx2[0])) {
        attr(video_1, "src", video_1_src_value);
      }
      if (!video_1_updating && dirty & 4 && !isNaN_1(ctx2[2])) {
        video_1.currentTime = ctx2[2];
      }
      video_1_updating = false;
      if (dirty & 16 && video_1_is_paused !== (video_1_is_paused = ctx2[4])) {
        video_1[video_1_is_paused ? "pause" : "play"]();
      }
      if (dirty & 2) {
        toggle_class(video_1, "mirror", ctx2[1]);
      }
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type(ctx2);
      if (current_block_type_index !== previous_block_index) {
        group_outros();
        transition_out(if_blocks[previous_block_index], 1, 1, () => {
          if_blocks[previous_block_index] = null;
        });
        check_outros();
        if_block = if_blocks[current_block_type_index];
        if (!if_block) {
          if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
          if_block.c();
        }
        transition_in(if_block, 1);
        if_block.m(span0, null);
      }
      if ((!current || dirty & 4) && t2_value !== (t2_value = format(ctx2[2]) + ""))
        set_data(t2, t2_value);
      if ((!current || dirty & 8) && t4_value !== (t4_value = format(ctx2[3]) + ""))
        set_data(t4, t4_value);
      if (!current || dirty & 12 && progress_value_value !== (progress_value_value = ctx2[2] / ctx2[3] || 0)) {
        progress.value = progress_value_value;
      }
      if (!current || dirty & 72) {
        set_style(div2, "opacity", ctx2[3] && ctx2[6] ? 1 : 0);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block);
      transition_in(maximise.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      transition_out(maximise.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div3);
      ctx[17](null);
      if_blocks[current_block_type_index].d();
      destroy_component(maximise);
      mounted = false;
      run_all(dispose);
    }
  };
}
function format(seconds) {
  if (isNaN(seconds) || !isFinite(seconds))
    return "...";
  const minutes = Math.floor(seconds / 60);
  let _seconds = Math.floor(seconds % 60);
  if (seconds < 10)
    _seconds = `0${_seconds}`;
  return `${minutes}:${_seconds}`;
}
function instance$3($$self, $$props, $$invalidate) {
  let { src } = $$props;
  let { mirror } = $$props;
  let time = 0;
  let duration;
  let paused = true;
  let video;
  let show_controls = true;
  let show_controls_timeout;
  function video_move() {
    clearTimeout(show_controls_timeout);
    show_controls_timeout = setTimeout(() => $$invalidate(6, show_controls = false), 2500);
    $$invalidate(6, show_controls = true);
  }
  function handleMove(e) {
    if (!duration)
      return;
    if (e.type === "click") {
      handle_click(e);
      return;
    }
    if (e.type !== "touchmove" && !(e.buttons & 1))
      return;
    const clientX = e.type === "touchmove" ? e.touches[0].clientX : e.clientX;
    const { left, right } = e.currentTarget.getBoundingClientRect();
    $$invalidate(2, time = duration * (clientX - left) / (right - left));
  }
  function play_pause() {
    if (paused)
      video.play();
    else
      video.pause();
  }
  function handle_click(e) {
    const { left, right } = e.currentTarget.getBoundingClientRect();
    $$invalidate(2, time = duration * (e.clientX - left) / (right - left));
  }
  async function _load() {
    await tick();
    $$invalidate(5, video.currentTime = 9999, video);
    setTimeout(async () => {
      $$invalidate(5, video.currentTime = 0, video);
    }, 50);
  }
  function play_handler(event) {
    bubble.call(this, $$self, event);
  }
  function pause_handler(event) {
    bubble.call(this, $$self, event);
  }
  function ended_handler(event) {
    bubble.call(this, $$self, event);
  }
  function video_1_timeupdate_handler() {
    time = this.currentTime;
    $$invalidate(2, time);
  }
  function video_1_durationchange_handler() {
    duration = this.duration;
    $$invalidate(3, duration);
  }
  function video_1_play_pause_handler() {
    paused = this.paused;
    $$invalidate(4, paused);
  }
  function video_1_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      video = $$value;
      $$invalidate(5, video);
    });
  }
  const click_handler = () => video.requestFullscreen();
  $$self.$$set = ($$props2) => {
    if ("src" in $$props2)
      $$invalidate(0, src = $$props2.src);
    if ("mirror" in $$props2)
      $$invalidate(1, mirror = $$props2.mirror);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      src && _load();
    }
  };
  return [
    src,
    mirror,
    time,
    duration,
    paused,
    video,
    show_controls,
    video_move,
    handleMove,
    play_pause,
    handle_click,
    play_handler,
    pause_handler,
    ended_handler,
    video_1_timeupdate_handler,
    video_1_durationchange_handler,
    video_1_play_pause_handler,
    video_1_binding,
    click_handler
  ];
}
class Player extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$3, create_fragment$3, safe_not_equal, { src: 0, mirror: 1 });
  }
}
function create_else_block$2(ctx) {
  let modifyupload;
  let t;
  let show_if;
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  modifyupload = new ModifyUpload({});
  modifyupload.$on("clear", ctx[12]);
  const if_block_creators = [create_if_block_3, create_if_block_4];
  const if_blocks = [];
  function select_block_type_2(ctx2, dirty) {
    if (show_if == null)
      show_if = !!playable();
    if (show_if)
      return 0;
    if (ctx2[0].size)
      return 1;
    return -1;
  }
  if (~(current_block_type_index = select_block_type_2(ctx))) {
    if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  }
  return {
    c() {
      create_component(modifyupload.$$.fragment);
      t = space();
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      mount_component(modifyupload, target, anchor);
      insert(target, t, anchor);
      if (~current_block_type_index) {
        if_blocks[current_block_type_index].m(target, anchor);
      }
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type_2(ctx2);
      if (current_block_type_index === previous_block_index) {
        if (~current_block_type_index) {
          if_blocks[current_block_type_index].p(ctx2, dirty);
        }
      } else {
        if (if_block) {
          group_outros();
          transition_out(if_blocks[previous_block_index], 1, 1, () => {
            if_blocks[previous_block_index] = null;
          });
          check_outros();
        }
        if (~current_block_type_index) {
          if_block = if_blocks[current_block_type_index];
          if (!if_block) {
            if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
            if_block.c();
          } else {
            if_block.p(ctx2, dirty);
          }
          transition_in(if_block, 1);
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        } else {
          if_block = null;
        }
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(modifyupload.$$.fragment, local);
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(modifyupload.$$.fragment, local);
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      destroy_component(modifyupload, detaching);
      if (detaching)
        detach(t);
      if (~current_block_type_index) {
        if_blocks[current_block_type_index].d(detaching);
      }
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block$2(ctx) {
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  const if_block_creators = [create_if_block_1, create_if_block_2];
  const if_blocks = [];
  function select_block_type_1(ctx2, dirty) {
    if (ctx2[1] === "upload")
      return 0;
    if (ctx2[1] === "webcam")
      return 1;
    return -1;
  }
  if (~(current_block_type_index = select_block_type_1(ctx))) {
    if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  }
  return {
    c() {
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if (~current_block_type_index) {
        if_blocks[current_block_type_index].m(target, anchor);
      }
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type_1(ctx2);
      if (current_block_type_index === previous_block_index) {
        if (~current_block_type_index) {
          if_blocks[current_block_type_index].p(ctx2, dirty);
        }
      } else {
        if (if_block) {
          group_outros();
          transition_out(if_blocks[previous_block_index], 1, 1, () => {
            if_blocks[previous_block_index] = null;
          });
          check_outros();
        }
        if (~current_block_type_index) {
          if_block = if_blocks[current_block_type_index];
          if (!if_block) {
            if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx2);
            if_block.c();
          } else {
            if_block.p(ctx2, dirty);
          }
          transition_in(if_block, 1);
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        } else {
          if_block = null;
        }
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      current = false;
    },
    d(detaching) {
      if (~current_block_type_index) {
        if_blocks[current_block_type_index].d(detaching);
      }
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block_4(ctx) {
  let div0;
  let t0_value = ctx[0].name + "";
  let t0;
  let t1;
  let div1;
  let t2_value = prettyBytes(ctx[0].size) + "";
  let t2;
  return {
    c() {
      div0 = element("div");
      t0 = text(t0_value);
      t1 = space();
      div1 = element("div");
      t2 = text(t2_value);
      attr(div0, "class", "file-name text-4xl p-6 break-all");
      attr(div1, "class", "file-size text-2xl p-2");
    },
    m(target, anchor) {
      insert(target, div0, anchor);
      append(div0, t0);
      insert(target, t1, anchor);
      insert(target, div1, anchor);
      append(div1, t2);
    },
    p(ctx2, dirty) {
      if (dirty & 1 && t0_value !== (t0_value = ctx2[0].name + ""))
        set_data(t0, t0_value);
      if (dirty & 1 && t2_value !== (t2_value = prettyBytes(ctx2[0].size) + ""))
        set_data(t2, t2_value);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div0);
      if (detaching)
        detach(t1);
      if (detaching)
        detach(div1);
    }
  };
}
function create_if_block_3(ctx) {
  let player;
  let current;
  player = new Player({
    props: {
      src: ctx[0].data,
      mirror: ctx[4] && ctx[1] === "webcam"
    }
  });
  player.$on("play", ctx[16]);
  player.$on("pause", ctx[17]);
  player.$on("ended", ctx[18]);
  return {
    c() {
      create_component(player.$$.fragment);
    },
    m(target, anchor) {
      mount_component(player, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const player_changes = {};
      if (dirty & 1)
        player_changes.src = ctx2[0].data;
      if (dirty & 18)
        player_changes.mirror = ctx2[4] && ctx2[1] === "webcam";
      player.$set(player_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(player.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(player.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(player, detaching);
    }
  };
}
function create_if_block_2(ctx) {
  let webcam;
  let current;
  webcam = new Webcam({
    props: {
      mirror_webcam: ctx[4],
      include_audio: ctx[5],
      mode: "video"
    }
  });
  webcam.$on("error", ctx[14]);
  webcam.$on("capture", ctx[15]);
  return {
    c() {
      create_component(webcam.$$.fragment);
    },
    m(target, anchor) {
      mount_component(webcam, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const webcam_changes = {};
      if (dirty & 16)
        webcam_changes.mirror_webcam = ctx2[4];
      if (dirty & 32)
        webcam_changes.include_audio = ctx2[5];
      webcam.$set(webcam_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(webcam.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(webcam.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(webcam, detaching);
    }
  };
}
function create_if_block_1(ctx) {
  let upload;
  let updating_dragging;
  let current;
  function upload_dragging_binding(value) {
    ctx[13](value);
  }
  let upload_props = {
    filetype: "video/mp4,video/x-m4v,video/*",
    $$slots: { default: [create_default_slot$1] },
    $$scope: { ctx }
  };
  if (ctx[9] !== void 0) {
    upload_props.dragging = ctx[9];
  }
  upload = new Upload({ props: upload_props });
  binding_callbacks.push(() => bind(upload, "dragging", upload_dragging_binding));
  upload.$on("load", ctx[11]);
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
      if (dirty & 524736) {
        upload_changes.$$scope = { dirty, ctx: ctx2 };
      }
      if (!updating_dragging && dirty & 512) {
        updating_dragging = true;
        upload_changes.dragging = ctx2[9];
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
function create_default_slot$1(ctx) {
  let div;
  let t0;
  let t1;
  let span;
  let t2;
  let t3;
  let t4;
  let t5;
  let t6;
  return {
    c() {
      div = element("div");
      t0 = text(ctx[6]);
      t1 = space();
      span = element("span");
      t2 = text("- ");
      t3 = text(ctx[7]);
      t4 = text(" -");
      t5 = space();
      t6 = text(ctx[8]);
      attr(span, "class", "text-gray-300");
      attr(div, "class", "flex flex-col");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, t0);
      append(div, t1);
      append(div, span);
      append(span, t2);
      append(span, t3);
      append(span, t4);
      append(div, t5);
      append(div, t6);
    },
    p(ctx2, dirty) {
      if (dirty & 64)
        set_data(t0, ctx2[6]);
      if (dirty & 128)
        set_data(t3, ctx2[7]);
      if (dirty & 256)
        set_data(t6, ctx2[8]);
    },
    d(detaching) {
      if (detaching)
        detach(div);
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
      show_label: ctx[3],
      Icon: Video,
      label: ctx[2] || "Video"
    }
  });
  const if_block_creators = [create_if_block$2, create_else_block$2];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[0] === null)
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
      if (dirty & 8)
        blocklabel_changes.show_label = ctx2[3];
      if (dirty & 4)
        blocklabel_changes.label = ctx2[2] || "Video";
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
  let { value = null } = $$props;
  let { source } = $$props;
  let { label = void 0 } = $$props;
  let { show_label } = $$props;
  let { mirror_webcam } = $$props;
  let { include_audio } = $$props;
  let { drop_text = "Drop a video file" } = $$props;
  let { or_text = "or" } = $$props;
  let { upload_text = "click to upload" } = $$props;
  const dispatch = createEventDispatcher();
  function handle_load({ detail }) {
    dispatch("change", detail);
    dispatch("upload", detail);
    $$invalidate(0, value = detail);
  }
  function handle_clear({ detail }) {
    $$invalidate(0, value = null);
    dispatch("change", detail);
    dispatch("clear");
  }
  let dragging = false;
  function upload_dragging_binding(value2) {
    dragging = value2;
    $$invalidate(9, dragging);
  }
  function error_handler(event) {
    bubble.call(this, $$self, event);
  }
  const capture_handler = ({ detail }) => dispatch("change", detail);
  function play_handler(event) {
    bubble.call(this, $$self, event);
  }
  function pause_handler(event) {
    bubble.call(this, $$self, event);
  }
  function ended_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("source" in $$props2)
      $$invalidate(1, source = $$props2.source);
    if ("label" in $$props2)
      $$invalidate(2, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(3, show_label = $$props2.show_label);
    if ("mirror_webcam" in $$props2)
      $$invalidate(4, mirror_webcam = $$props2.mirror_webcam);
    if ("include_audio" in $$props2)
      $$invalidate(5, include_audio = $$props2.include_audio);
    if ("drop_text" in $$props2)
      $$invalidate(6, drop_text = $$props2.drop_text);
    if ("or_text" in $$props2)
      $$invalidate(7, or_text = $$props2.or_text);
    if ("upload_text" in $$props2)
      $$invalidate(8, upload_text = $$props2.upload_text);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 512) {
      dispatch("drag", dragging);
    }
  };
  return [
    value,
    source,
    label,
    show_label,
    mirror_webcam,
    include_audio,
    drop_text,
    or_text,
    upload_text,
    dragging,
    dispatch,
    handle_load,
    handle_clear,
    upload_dragging_binding,
    error_handler,
    capture_handler,
    play_handler,
    pause_handler,
    ended_handler
  ];
}
class Video_1$2 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$2, create_fragment$2, safe_not_equal, {
      value: 0,
      source: 1,
      label: 2,
      show_label: 3,
      mirror_webcam: 4,
      include_audio: 5,
      drop_text: 6,
      or_text: 7,
      upload_text: 8
    });
  }
}
function create_else_block$1(ctx) {
  let player;
  let current;
  player = new Player({
    props: {
      src: ctx[0].data,
      mirror: false
    }
  });
  player.$on("play", ctx[3]);
  player.$on("pause", ctx[4]);
  player.$on("ended", ctx[5]);
  return {
    c() {
      create_component(player.$$.fragment);
    },
    m(target, anchor) {
      mount_component(player, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const player_changes = {};
      if (dirty & 1)
        player_changes.src = ctx2[0].data;
      player.$set(player_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(player.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(player.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(player, detaching);
    }
  };
}
function create_if_block$1(ctx) {
  let div1;
  let div0;
  let video;
  let current;
  video = new Video({});
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      create_component(video.$$.fragment);
      attr(div0, "class", "h-5 dark:text-white opacity-50");
      attr(div1, "class", "h-full min-h-[15rem] flex justify-center items-center");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      mount_component(video, div0, null);
      current = true;
    },
    p: noop,
    i(local) {
      if (current)
        return;
      transition_in(video.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(video.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      destroy_component(video);
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
      show_label: ctx[2],
      Icon: Video,
      label: ctx[1] || "Video"
    }
  });
  const if_block_creators = [create_if_block$1, create_else_block$1];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[0] === null)
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
        blocklabel_changes.label = ctx2[1] || "Video";
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
  let { value = null } = $$props;
  let { label = void 0 } = $$props;
  let { show_label } = $$props;
  const dispatch = createEventDispatcher();
  function play_handler(event) {
    bubble.call(this, $$self, event);
  }
  function pause_handler(event) {
    bubble.call(this, $$self, event);
  }
  function ended_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("label" in $$props2)
      $$invalidate(1, label = $$props2.label);
    if ("show_label" in $$props2)
      $$invalidate(2, show_label = $$props2.show_label);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 1) {
      value && dispatch("change", value);
    }
  };
  return [value, label, show_label, play_handler, pause_handler, ended_handler];
}
class StaticVideo extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, { value: 0, label: 1, show_label: 2 });
  }
}
function create_else_block(ctx) {
  let video;
  let current;
  video = new Video_1$2({
    props: {
      value: ctx[11],
      label: ctx[4],
      show_label: ctx[6],
      source: ctx[5],
      drop_text: ctx[13]("interface.drop_video"),
      or_text: ctx[13]("or"),
      upload_text: ctx[13]("interface.click_to_upload"),
      mirror_webcam: ctx[8],
      include_audio: ctx[9]
    }
  });
  video.$on("change", ctx[16]);
  video.$on("drag", ctx[17]);
  video.$on("error", ctx[18]);
  video.$on("change", ctx[19]);
  video.$on("clear", ctx[20]);
  video.$on("play", ctx[21]);
  video.$on("pause", ctx[22]);
  video.$on("upload", ctx[23]);
  return {
    c() {
      create_component(video.$$.fragment);
    },
    m(target, anchor) {
      mount_component(video, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const video_changes = {};
      if (dirty & 2048)
        video_changes.value = ctx2[11];
      if (dirty & 16)
        video_changes.label = ctx2[4];
      if (dirty & 64)
        video_changes.show_label = ctx2[6];
      if (dirty & 32)
        video_changes.source = ctx2[5];
      if (dirty & 8192)
        video_changes.drop_text = ctx2[13]("interface.drop_video");
      if (dirty & 8192)
        video_changes.or_text = ctx2[13]("or");
      if (dirty & 8192)
        video_changes.upload_text = ctx2[13]("interface.click_to_upload");
      if (dirty & 256)
        video_changes.mirror_webcam = ctx2[8];
      if (dirty & 512)
        video_changes.include_audio = ctx2[9];
      video.$set(video_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(video.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(video.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(video, detaching);
    }
  };
}
function create_if_block(ctx) {
  let staticvideo;
  let current;
  staticvideo = new StaticVideo({
    props: {
      value: ctx[11],
      label: ctx[4],
      show_label: ctx[6]
    }
  });
  return {
    c() {
      create_component(staticvideo.$$.fragment);
    },
    m(target, anchor) {
      mount_component(staticvideo, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const staticvideo_changes = {};
      if (dirty & 2048)
        staticvideo_changes.value = ctx2[11];
      if (dirty & 16)
        staticvideo_changes.label = ctx2[4];
      if (dirty & 64)
        staticvideo_changes.show_label = ctx2[6];
      staticvideo.$set(staticvideo_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(staticvideo.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(staticvideo.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(staticvideo, detaching);
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
  const statustracker_spread_levels = [ctx[1]];
  let statustracker_props = {};
  for (let i = 0; i < statustracker_spread_levels.length; i += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  const if_block_creators = [create_if_block, create_else_block];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[10] === "static")
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
      const statustracker_changes = dirty & 2 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[1])]) : {};
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
      visible: ctx[3],
      variant: ctx[10] === "dynamic" && ctx[0] === null && ctx[5] === "upload" ? "dashed" : "solid",
      color: ctx[12] ? "green" : "grey",
      padding: false,
      elem_id: ctx[2],
      style: {
        height: ctx[7].height,
        width: ctx[7].width
      },
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
      if (dirty & 1057)
        block_changes.variant = ctx2[10] === "dynamic" && ctx2[0] === null && ctx2[5] === "upload" ? "dashed" : "solid";
      if (dirty & 4096)
        block_changes.color = ctx2[12] ? "green" : "grey";
      if (dirty & 4)
        block_changes.elem_id = ctx2[2];
      if (dirty & 128)
        block_changes.style = {
          height: ctx2[7].height,
          width: ctx2[7].width
        };
      if (dirty & 16793459) {
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
  component_subscribe($$self, X, ($$value) => $$invalidate(13, $_ = $$value));
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = null } = $$props;
  let { label } = $$props;
  let { source } = $$props;
  let { root } = $$props;
  let { root_url } = $$props;
  let { show_label } = $$props;
  let { loading_status } = $$props;
  let { style = {} } = $$props;
  let { mirror_webcam } = $$props;
  let { include_audio } = $$props;
  let { mode } = $$props;
  let _value;
  let dragging = false;
  const change_handler_1 = ({ detail }) => $$invalidate(0, value = detail);
  const drag_handler = ({ detail }) => $$invalidate(12, dragging = detail);
  const error_handler = ({ detail }) => {
    $$invalidate(1, loading_status = loading_status || {});
    $$invalidate(1, loading_status.status = "error", loading_status);
    $$invalidate(1, loading_status.message = detail, loading_status);
  };
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  function clear_handler(event) {
    bubble.call(this, $$self, event);
  }
  function play_handler(event) {
    bubble.call(this, $$self, event);
  }
  function pause_handler(event) {
    bubble.call(this, $$self, event);
  }
  function upload_handler(event) {
    bubble.call(this, $$self, event);
  }
  $$self.$$set = ($$props2) => {
    if ("elem_id" in $$props2)
      $$invalidate(2, elem_id = $$props2.elem_id);
    if ("visible" in $$props2)
      $$invalidate(3, visible = $$props2.visible);
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("label" in $$props2)
      $$invalidate(4, label = $$props2.label);
    if ("source" in $$props2)
      $$invalidate(5, source = $$props2.source);
    if ("root" in $$props2)
      $$invalidate(14, root = $$props2.root);
    if ("root_url" in $$props2)
      $$invalidate(15, root_url = $$props2.root_url);
    if ("show_label" in $$props2)
      $$invalidate(6, show_label = $$props2.show_label);
    if ("loading_status" in $$props2)
      $$invalidate(1, loading_status = $$props2.loading_status);
    if ("style" in $$props2)
      $$invalidate(7, style = $$props2.style);
    if ("mirror_webcam" in $$props2)
      $$invalidate(8, mirror_webcam = $$props2.mirror_webcam);
    if ("include_audio" in $$props2)
      $$invalidate(9, include_audio = $$props2.include_audio);
    if ("mode" in $$props2)
      $$invalidate(10, mode = $$props2.mode);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 49153) {
      $$invalidate(11, _value = normalise_file(value, root_url != null ? root_url : root));
    }
  };
  return [
    value,
    loading_status,
    elem_id,
    visible,
    label,
    source,
    show_label,
    style,
    mirror_webcam,
    include_audio,
    mode,
    _value,
    dragging,
    $_,
    root,
    root_url,
    change_handler_1,
    drag_handler,
    error_handler,
    change_handler,
    clear_handler,
    play_handler,
    pause_handler,
    upload_handler
  ];
}
class Video_1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      elem_id: 2,
      visible: 3,
      value: 0,
      label: 4,
      source: 5,
      root: 14,
      root_url: 15,
      show_label: 6,
      loading_status: 1,
      style: 7,
      mirror_webcam: 8,
      include_audio: 9,
      mode: 10
    });
  }
}
var Video_1$1 = Video_1;
const modes = ["static", "dynamic"];
const document = (config) => ({
  type: "{ name: string; data: string }",
  description: "file name and base64 data of video file"
});
export { Video_1$1 as Component, document, modes };
//# sourceMappingURL=index39.js.map
