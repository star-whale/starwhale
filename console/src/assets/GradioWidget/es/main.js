var __defProp = Object.defineProperty;
var __defProps = Object.defineProperties;
var __getOwnPropDescs = Object.getOwnPropertyDescriptors;
var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __spreadValues = (a2, b2) => {
  for (var prop in b2 || (b2 = {}))
    if (__hasOwnProp.call(b2, prop))
      __defNormalProp(a2, prop, b2[prop]);
  if (__getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(b2)) {
      if (__propIsEnum.call(b2, prop))
        __defNormalProp(a2, prop, b2[prop]);
    }
  return a2;
};
var __spreadProps = (a2, b2) => __defProps(a2, __getOwnPropDescs(b2));
var __objRest = (source, exclude) => {
  var target = {};
  for (var prop in source)
    if (__hasOwnProp.call(source, prop) && exclude.indexOf(prop) < 0)
      target[prop] = source[prop];
  if (source != null && __getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(source)) {
      if (exclude.indexOf(prop) < 0 && __propIsEnum.call(source, prop))
        target[prop] = source[prop];
    }
  return target;
};
function noop() {
}
const identity = (x2) => x2;
function assign(tar, src) {
  for (const k2 in src)
    tar[k2] = src[k2];
  return tar;
}
function run(fn2) {
  return fn2();
}
function blank_object() {
  return /* @__PURE__ */ Object.create(null);
}
function run_all(fns) {
  fns.forEach(run);
}
function is_function(thing) {
  return typeof thing === "function";
}
function safe_not_equal(a2, b2) {
  return a2 != a2 ? b2 == b2 : a2 !== b2 || (a2 && typeof a2 === "object" || typeof a2 === "function");
}
let src_url_equal_anchor;
function src_url_equal(element_src, url) {
  if (!src_url_equal_anchor) {
    src_url_equal_anchor = document.createElement("a");
  }
  src_url_equal_anchor.href = url;
  return element_src === src_url_equal_anchor.href;
}
function is_empty(obj) {
  return Object.keys(obj).length === 0;
}
function subscribe(store, ...callbacks) {
  if (store == null) {
    return noop;
  }
  const unsub = store.subscribe(...callbacks);
  return unsub.unsubscribe ? () => unsub.unsubscribe() : unsub;
}
function get_store_value(store) {
  let value;
  subscribe(store, (_2) => value = _2)();
  return value;
}
function component_subscribe(component, store, callback) {
  component.$$.on_destroy.push(subscribe(store, callback));
}
function create_slot(definition, ctx, $$scope, fn2) {
  if (definition) {
    const slot_ctx = get_slot_context(definition, ctx, $$scope, fn2);
    return definition[0](slot_ctx);
  }
}
function get_slot_context(definition, ctx, $$scope, fn2) {
  return definition[1] && fn2 ? assign($$scope.ctx.slice(), definition[1](fn2(ctx))) : $$scope.ctx;
}
function get_slot_changes(definition, $$scope, dirty, fn2) {
  if (definition[2] && fn2) {
    const lets = definition[2](fn2(dirty));
    if ($$scope.dirty === void 0) {
      return lets;
    }
    if (typeof lets === "object") {
      const merged = [];
      const len = Math.max($$scope.dirty.length, lets.length);
      for (let i2 = 0; i2 < len; i2 += 1) {
        merged[i2] = $$scope.dirty[i2] | lets[i2];
      }
      return merged;
    }
    return $$scope.dirty | lets;
  }
  return $$scope.dirty;
}
function update_slot_base(slot, slot_definition, ctx, $$scope, slot_changes, get_slot_context_fn) {
  if (slot_changes) {
    const slot_context = get_slot_context(slot_definition, ctx, $$scope, get_slot_context_fn);
    slot.p(slot_context, slot_changes);
  }
}
function get_all_dirty_from_scope($$scope) {
  if ($$scope.ctx.length > 32) {
    const dirty = [];
    const length = $$scope.ctx.length / 32;
    for (let i2 = 0; i2 < length; i2++) {
      dirty[i2] = -1;
    }
    return dirty;
  }
  return -1;
}
function set_store_value(store, ret, value) {
  store.set(value);
  return ret;
}
function action_destroyer(action_result) {
  return action_result && is_function(action_result.destroy) ? action_result.destroy : noop;
}
const is_client = typeof window !== "undefined";
let now = is_client ? () => window.performance.now() : () => Date.now();
let raf = is_client ? (cb) => requestAnimationFrame(cb) : noop;
const tasks = /* @__PURE__ */ new Set();
function run_tasks(now2) {
  tasks.forEach((task) => {
    if (!task.c(now2)) {
      tasks.delete(task);
      task.f();
    }
  });
  if (tasks.size !== 0)
    raf(run_tasks);
}
function loop(callback) {
  let task;
  if (tasks.size === 0)
    raf(run_tasks);
  return {
    promise: new Promise((fulfill) => {
      tasks.add(task = { c: callback, f: fulfill });
    }),
    abort() {
      tasks.delete(task);
    }
  };
}
function append(target, node) {
  target.appendChild(node);
}
function get_root_for_style(node) {
  if (!node)
    return document;
  const root = node.getRootNode ? node.getRootNode() : node.ownerDocument;
  if (root && root.host) {
    return root;
  }
  return node.ownerDocument;
}
function append_empty_stylesheet(node) {
  const style_element = element("style");
  append_stylesheet(get_root_for_style(node), style_element);
  return style_element.sheet;
}
function append_stylesheet(node, style) {
  append(node.head || node, style);
}
function insert(target, node, anchor) {
  target.insertBefore(node, anchor || null);
}
function detach(node) {
  node.parentNode.removeChild(node);
}
function destroy_each(iterations, detaching) {
  for (let i2 = 0; i2 < iterations.length; i2 += 1) {
    if (iterations[i2])
      iterations[i2].d(detaching);
  }
}
function element(name) {
  return document.createElement(name);
}
function svg_element(name) {
  return document.createElementNS("http://www.w3.org/2000/svg", name);
}
function text(data) {
  return document.createTextNode(data);
}
function space() {
  return text(" ");
}
function empty() {
  return text("");
}
function listen(node, event, handler, options) {
  node.addEventListener(event, handler, options);
  return () => node.removeEventListener(event, handler, options);
}
function prevent_default(fn2) {
  return function(event) {
    event.preventDefault();
    return fn2.call(this, event);
  };
}
function stop_propagation(fn2) {
  return function(event) {
    event.stopPropagation();
    return fn2.call(this, event);
  };
}
function attr(node, attribute, value) {
  if (value == null)
    node.removeAttribute(attribute);
  else if (node.getAttribute(attribute) !== value)
    node.setAttribute(attribute, value);
}
function set_attributes(node, attributes) {
  const descriptors = Object.getOwnPropertyDescriptors(node.__proto__);
  for (const key in attributes) {
    if (attributes[key] == null) {
      node.removeAttribute(key);
    } else if (key === "style") {
      node.style.cssText = attributes[key];
    } else if (key === "__value") {
      node.value = node[key] = attributes[key];
    } else if (descriptors[key] && descriptors[key].set) {
      node[key] = attributes[key];
    } else {
      attr(node, key, attributes[key]);
    }
  }
}
function to_number(value) {
  return value === "" ? null : +value;
}
function children(element2) {
  return Array.from(element2.childNodes);
}
function set_data(text2, data) {
  data = "" + data;
  if (text2.wholeText !== data)
    text2.data = data;
}
function set_input_value(input, value) {
  input.value = value == null ? "" : value;
}
function set_style(node, key, value, important) {
  if (value === null) {
    node.style.removeProperty(key);
  } else {
    node.style.setProperty(key, value, important ? "important" : "");
  }
}
function select_option(select, value) {
  for (let i2 = 0; i2 < select.options.length; i2 += 1) {
    const option = select.options[i2];
    if (option.__value === value) {
      option.selected = true;
      return;
    }
  }
  select.selectedIndex = -1;
}
function select_value(select) {
  const selected_option = select.querySelector(":checked") || select.options[0];
  return selected_option && selected_option.__value;
}
let crossorigin;
function is_crossorigin() {
  if (crossorigin === void 0) {
    crossorigin = false;
    try {
      if (typeof window !== "undefined" && window.parent) {
        void window.parent.document;
      }
    } catch (error) {
      crossorigin = true;
    }
  }
  return crossorigin;
}
function add_resize_listener(node, fn2) {
  const computed_style = getComputedStyle(node);
  if (computed_style.position === "static") {
    node.style.position = "relative";
  }
  const iframe = element("iframe");
  iframe.setAttribute("style", "display: block; position: absolute; top: 0; left: 0; width: 100%; height: 100%; overflow: hidden; border: 0; opacity: 0; pointer-events: none; z-index: -1;");
  iframe.setAttribute("aria-hidden", "true");
  iframe.tabIndex = -1;
  const crossorigin2 = is_crossorigin();
  let unsubscribe;
  if (crossorigin2) {
    iframe.src = "data:text/html,<script>onresize=function(){parent.postMessage(0,'*')}<\/script>";
    unsubscribe = listen(window, "message", (event) => {
      if (event.source === iframe.contentWindow)
        fn2();
    });
  } else {
    iframe.src = "about:blank";
    iframe.onload = () => {
      unsubscribe = listen(iframe.contentWindow, "resize", fn2);
    };
  }
  append(node, iframe);
  return () => {
    if (crossorigin2) {
      unsubscribe();
    } else if (unsubscribe && iframe.contentWindow) {
      unsubscribe();
    }
    detach(iframe);
  };
}
function toggle_class(element2, name, toggle) {
  element2.classList[toggle ? "add" : "remove"](name);
}
function custom_event(type, detail, { bubbles = false, cancelable = false } = {}) {
  const e = document.createEvent("CustomEvent");
  e.initCustomEvent(type, bubbles, cancelable, detail);
  return e;
}
class HtmlTag {
  constructor(is_svg = false) {
    this.is_svg = false;
    this.is_svg = is_svg;
    this.e = this.n = null;
  }
  c(html) {
    this.h(html);
  }
  m(html, target, anchor = null) {
    if (!this.e) {
      if (this.is_svg)
        this.e = svg_element(target.nodeName);
      else
        this.e = element(target.nodeName);
      this.t = target;
      this.c(html);
    }
    this.i(anchor);
  }
  h(html) {
    this.e.innerHTML = html;
    this.n = Array.from(this.e.childNodes);
  }
  i(anchor) {
    for (let i2 = 0; i2 < this.n.length; i2 += 1) {
      insert(this.t, this.n[i2], anchor);
    }
  }
  p(html) {
    this.d();
    this.h(html);
    this.i(this.a);
  }
  d() {
    this.n.forEach(detach);
  }
}
const managed_styles = /* @__PURE__ */ new Map();
let active = 0;
function hash(str) {
  let hash2 = 5381;
  let i2 = str.length;
  while (i2--)
    hash2 = (hash2 << 5) - hash2 ^ str.charCodeAt(i2);
  return hash2 >>> 0;
}
function create_style_information(doc, node) {
  const info = { stylesheet: append_empty_stylesheet(node), rules: {} };
  managed_styles.set(doc, info);
  return info;
}
function create_rule(node, a2, b2, duration, delay, ease, fn2, uid = 0) {
  const step = 16.666 / duration;
  let keyframes = "{\n";
  for (let p2 = 0; p2 <= 1; p2 += step) {
    const t = a2 + (b2 - a2) * ease(p2);
    keyframes += p2 * 100 + `%{${fn2(t, 1 - t)}}
`;
  }
  const rule = keyframes + `100% {${fn2(b2, 1 - b2)}}
}`;
  const name = `__svelte_${hash(rule)}_${uid}`;
  const doc = get_root_for_style(node);
  const { stylesheet, rules } = managed_styles.get(doc) || create_style_information(doc, node);
  if (!rules[name]) {
    rules[name] = true;
    stylesheet.insertRule(`@keyframes ${name} ${rule}`, stylesheet.cssRules.length);
  }
  const animation = node.style.animation || "";
  node.style.animation = `${animation ? `${animation}, ` : ""}${name} ${duration}ms linear ${delay}ms 1 both`;
  active += 1;
  return name;
}
function delete_rule(node, name) {
  const previous = (node.style.animation || "").split(", ");
  const next = previous.filter(name ? (anim) => anim.indexOf(name) < 0 : (anim) => anim.indexOf("__svelte") === -1);
  const deleted = previous.length - next.length;
  if (deleted) {
    node.style.animation = next.join(", ");
    active -= deleted;
    if (!active)
      clear_rules();
  }
}
function clear_rules() {
  raf(() => {
    if (active)
      return;
    managed_styles.forEach((info) => {
      const { stylesheet } = info;
      let i2 = stylesheet.cssRules.length;
      while (i2--)
        stylesheet.deleteRule(i2);
      info.rules = {};
    });
    managed_styles.clear();
  });
}
let current_component;
function set_current_component(component) {
  current_component = component;
}
function get_current_component() {
  if (!current_component)
    throw new Error("Function called outside component initialization");
  return current_component;
}
function beforeUpdate(fn2) {
  get_current_component().$$.before_update.push(fn2);
}
function onMount(fn2) {
  get_current_component().$$.on_mount.push(fn2);
}
function afterUpdate(fn2) {
  get_current_component().$$.after_update.push(fn2);
}
function onDestroy(fn2) {
  get_current_component().$$.on_destroy.push(fn2);
}
function createEventDispatcher() {
  const component = get_current_component();
  return (type, detail, { cancelable = false } = {}) => {
    const callbacks = component.$$.callbacks[type];
    if (callbacks) {
      const event = custom_event(type, detail, { cancelable });
      callbacks.slice().forEach((fn2) => {
        fn2.call(component, event);
      });
      return !event.defaultPrevented;
    }
    return true;
  };
}
function setContext(key, context) {
  get_current_component().$$.context.set(key, context);
  return context;
}
function getContext(key) {
  return get_current_component().$$.context.get(key);
}
function bubble(component, event) {
  const callbacks = component.$$.callbacks[event.type];
  if (callbacks) {
    callbacks.slice().forEach((fn2) => fn2.call(this, event));
  }
}
const dirty_components = [];
const binding_callbacks = [];
const render_callbacks = [];
const flush_callbacks = [];
const resolved_promise = Promise.resolve();
let update_scheduled = false;
function schedule_update() {
  if (!update_scheduled) {
    update_scheduled = true;
    resolved_promise.then(flush);
  }
}
function tick() {
  schedule_update();
  return resolved_promise;
}
function add_render_callback(fn2) {
  render_callbacks.push(fn2);
}
function add_flush_callback(fn2) {
  flush_callbacks.push(fn2);
}
const seen_callbacks = /* @__PURE__ */ new Set();
let flushidx = 0;
function flush() {
  const saved_component = current_component;
  do {
    while (flushidx < dirty_components.length) {
      const component = dirty_components[flushidx];
      flushidx++;
      set_current_component(component);
      update(component.$$);
    }
    set_current_component(null);
    dirty_components.length = 0;
    flushidx = 0;
    while (binding_callbacks.length)
      binding_callbacks.pop()();
    for (let i2 = 0; i2 < render_callbacks.length; i2 += 1) {
      const callback = render_callbacks[i2];
      if (!seen_callbacks.has(callback)) {
        seen_callbacks.add(callback);
        callback();
      }
    }
    render_callbacks.length = 0;
  } while (dirty_components.length);
  while (flush_callbacks.length) {
    flush_callbacks.pop()();
  }
  update_scheduled = false;
  seen_callbacks.clear();
  set_current_component(saved_component);
}
function update($$) {
  if ($$.fragment !== null) {
    $$.update();
    run_all($$.before_update);
    const dirty = $$.dirty;
    $$.dirty = [-1];
    $$.fragment && $$.fragment.p($$.ctx, dirty);
    $$.after_update.forEach(add_render_callback);
  }
}
let promise;
function wait() {
  if (!promise) {
    promise = Promise.resolve();
    promise.then(() => {
      promise = null;
    });
  }
  return promise;
}
function dispatch(node, direction, kind) {
  node.dispatchEvent(custom_event(`${direction ? "intro" : "outro"}${kind}`));
}
const outroing = /* @__PURE__ */ new Set();
let outros;
function group_outros() {
  outros = {
    r: 0,
    c: [],
    p: outros
  };
}
function check_outros() {
  if (!outros.r) {
    run_all(outros.c);
  }
  outros = outros.p;
}
function transition_in(block, local) {
  if (block && block.i) {
    outroing.delete(block);
    block.i(local);
  }
}
function transition_out(block, local, detach2, callback) {
  if (block && block.o) {
    if (outroing.has(block))
      return;
    outroing.add(block);
    outros.c.push(() => {
      outroing.delete(block);
      if (callback) {
        if (detach2)
          block.d(1);
        callback();
      }
    });
    block.o(local);
  } else if (callback) {
    callback();
  }
}
const null_transition = { duration: 0 };
function create_in_transition(node, fn2, params) {
  let config = fn2(node, params);
  let running = false;
  let animation_name;
  let task;
  let uid = 0;
  function cleanup() {
    if (animation_name)
      delete_rule(node, animation_name);
  }
  function go() {
    const { delay = 0, duration = 300, easing = identity, tick: tick2 = noop, css } = config || null_transition;
    if (css)
      animation_name = create_rule(node, 0, 1, duration, delay, easing, css, uid++);
    tick2(0, 1);
    const start_time = now() + delay;
    const end_time = start_time + duration;
    if (task)
      task.abort();
    running = true;
    add_render_callback(() => dispatch(node, true, "start"));
    task = loop((now2) => {
      if (running) {
        if (now2 >= end_time) {
          tick2(1, 0);
          dispatch(node, true, "end");
          cleanup();
          return running = false;
        }
        if (now2 >= start_time) {
          const t = easing((now2 - start_time) / duration);
          tick2(t, 1 - t);
        }
      }
      return running;
    });
  }
  let started = false;
  return {
    start() {
      if (started)
        return;
      started = true;
      delete_rule(node);
      if (is_function(config)) {
        config = config();
        wait().then(go);
      } else {
        go();
      }
    },
    invalidate() {
      started = false;
    },
    end() {
      if (running) {
        cleanup();
        running = false;
      }
    }
  };
}
function create_out_transition(node, fn2, params) {
  let config = fn2(node, params);
  let running = true;
  let animation_name;
  const group = outros;
  group.r += 1;
  function go() {
    const { delay = 0, duration = 300, easing = identity, tick: tick2 = noop, css } = config || null_transition;
    if (css)
      animation_name = create_rule(node, 1, 0, duration, delay, easing, css);
    const start_time = now() + delay;
    const end_time = start_time + duration;
    add_render_callback(() => dispatch(node, false, "start"));
    loop((now2) => {
      if (running) {
        if (now2 >= end_time) {
          tick2(0, 1);
          dispatch(node, false, "end");
          if (!--group.r) {
            run_all(group.c);
          }
          return false;
        }
        if (now2 >= start_time) {
          const t = easing((now2 - start_time) / duration);
          tick2(1 - t, t);
        }
      }
      return running;
    });
  }
  if (is_function(config)) {
    wait().then(() => {
      config = config();
      go();
    });
  } else {
    go();
  }
  return {
    end(reset) {
      if (reset && config.tick) {
        config.tick(1, 0);
      }
      if (running) {
        if (animation_name)
          delete_rule(node, animation_name);
        running = false;
      }
    }
  };
}
function create_bidirectional_transition(node, fn2, params, intro) {
  let config = fn2(node, params);
  let t = intro ? 0 : 1;
  let running_program = null;
  let pending_program = null;
  let animation_name = null;
  function clear_animation() {
    if (animation_name)
      delete_rule(node, animation_name);
  }
  function init2(program, duration) {
    const d2 = program.b - t;
    duration *= Math.abs(d2);
    return {
      a: t,
      b: program.b,
      d: d2,
      duration,
      start: program.start,
      end: program.start + duration,
      group: program.group
    };
  }
  function go(b2) {
    const { delay = 0, duration = 300, easing = identity, tick: tick2 = noop, css } = config || null_transition;
    const program = {
      start: now() + delay,
      b: b2
    };
    if (!b2) {
      program.group = outros;
      outros.r += 1;
    }
    if (running_program || pending_program) {
      pending_program = program;
    } else {
      if (css) {
        clear_animation();
        animation_name = create_rule(node, t, b2, duration, delay, easing, css);
      }
      if (b2)
        tick2(0, 1);
      running_program = init2(program, duration);
      add_render_callback(() => dispatch(node, b2, "start"));
      loop((now2) => {
        if (pending_program && now2 > pending_program.start) {
          running_program = init2(pending_program, duration);
          pending_program = null;
          dispatch(node, running_program.b, "start");
          if (css) {
            clear_animation();
            animation_name = create_rule(node, t, running_program.b, running_program.duration, 0, easing, config.css);
          }
        }
        if (running_program) {
          if (now2 >= running_program.end) {
            tick2(t = running_program.b, 1 - t);
            dispatch(node, running_program.b, "end");
            if (!pending_program) {
              if (running_program.b) {
                clear_animation();
              } else {
                if (!--running_program.group.r)
                  run_all(running_program.group.c);
              }
            }
            running_program = null;
          } else if (now2 >= running_program.start) {
            const p2 = now2 - running_program.start;
            t = running_program.a + running_program.d * easing(p2 / running_program.duration);
            tick2(t, 1 - t);
          }
        }
        return !!(running_program || pending_program);
      });
    }
  }
  return {
    run(b2) {
      if (is_function(config)) {
        wait().then(() => {
          config = config();
          go(b2);
        });
      } else {
        go(b2);
      }
    },
    end() {
      clear_animation();
      running_program = pending_program = null;
    }
  };
}
const globals = typeof window !== "undefined" ? window : typeof globalThis !== "undefined" ? globalThis : global;
function destroy_block(block, lookup) {
  block.d(1);
  lookup.delete(block.key);
}
function outro_and_destroy_block(block, lookup) {
  transition_out(block, 1, 1, () => {
    lookup.delete(block.key);
  });
}
function update_keyed_each(old_blocks, dirty, get_key, dynamic, ctx, list, lookup, node, destroy, create_each_block2, next, get_context) {
  let o2 = old_blocks.length;
  let n = list.length;
  let i2 = o2;
  const old_indexes = {};
  while (i2--)
    old_indexes[old_blocks[i2].key] = i2;
  const new_blocks = [];
  const new_lookup = /* @__PURE__ */ new Map();
  const deltas = /* @__PURE__ */ new Map();
  i2 = n;
  while (i2--) {
    const child_ctx = get_context(ctx, list, i2);
    const key = get_key(child_ctx);
    let block = lookup.get(key);
    if (!block) {
      block = create_each_block2(key, child_ctx);
      block.c();
    } else if (dynamic) {
      block.p(child_ctx, dirty);
    }
    new_lookup.set(key, new_blocks[i2] = block);
    if (key in old_indexes)
      deltas.set(key, Math.abs(i2 - old_indexes[key]));
  }
  const will_move = /* @__PURE__ */ new Set();
  const did_move = /* @__PURE__ */ new Set();
  function insert2(block) {
    transition_in(block, 1);
    block.m(node, next);
    lookup.set(block.key, block);
    next = block.first;
    n--;
  }
  while (o2 && n) {
    const new_block = new_blocks[n - 1];
    const old_block = old_blocks[o2 - 1];
    const new_key = new_block.key;
    const old_key = old_block.key;
    if (new_block === old_block) {
      next = new_block.first;
      o2--;
      n--;
    } else if (!new_lookup.has(old_key)) {
      destroy(old_block, lookup);
      o2--;
    } else if (!lookup.has(new_key) || will_move.has(new_key)) {
      insert2(new_block);
    } else if (did_move.has(old_key)) {
      o2--;
    } else if (deltas.get(new_key) > deltas.get(old_key)) {
      did_move.add(new_key);
      insert2(new_block);
    } else {
      will_move.add(old_key);
      o2--;
    }
  }
  while (o2--) {
    const old_block = old_blocks[o2];
    if (!new_lookup.has(old_block.key))
      destroy(old_block, lookup);
  }
  while (n)
    insert2(new_blocks[n - 1]);
  return new_blocks;
}
function get_spread_update(levels, updates) {
  const update2 = {};
  const to_null_out = {};
  const accounted_for = { $$scope: 1 };
  let i2 = levels.length;
  while (i2--) {
    const o2 = levels[i2];
    const n = updates[i2];
    if (n) {
      for (const key in o2) {
        if (!(key in n))
          to_null_out[key] = 1;
      }
      for (const key in n) {
        if (!accounted_for[key]) {
          update2[key] = n[key];
          accounted_for[key] = 1;
        }
      }
      levels[i2] = n;
    } else {
      for (const key in o2) {
        accounted_for[key] = 1;
      }
    }
  }
  for (const key in to_null_out) {
    if (!(key in update2))
      update2[key] = void 0;
  }
  return update2;
}
function get_spread_object(spread_props) {
  return typeof spread_props === "object" && spread_props !== null ? spread_props : {};
}
function bind(component, name, callback) {
  const index = component.$$.props[name];
  if (index !== void 0) {
    component.$$.bound[index] = callback;
    callback(component.$$.ctx[index]);
  }
}
function create_component(block) {
  block && block.c();
}
function mount_component(component, target, anchor, customElement) {
  const { fragment, on_mount, on_destroy, after_update } = component.$$;
  fragment && fragment.m(target, anchor);
  if (!customElement) {
    add_render_callback(() => {
      const new_on_destroy = on_mount.map(run).filter(is_function);
      if (on_destroy) {
        on_destroy.push(...new_on_destroy);
      } else {
        run_all(new_on_destroy);
      }
      component.$$.on_mount = [];
    });
  }
  after_update.forEach(add_render_callback);
}
function destroy_component(component, detaching) {
  const $$ = component.$$;
  if ($$.fragment !== null) {
    run_all($$.on_destroy);
    $$.fragment && $$.fragment.d(detaching);
    $$.on_destroy = $$.fragment = null;
    $$.ctx = [];
  }
}
function make_dirty(component, i2) {
  if (component.$$.dirty[0] === -1) {
    dirty_components.push(component);
    schedule_update();
    component.$$.dirty.fill(0);
  }
  component.$$.dirty[i2 / 31 | 0] |= 1 << i2 % 31;
}
function init(component, options, instance2, create_fragment2, not_equal, props, append_styles, dirty = [-1]) {
  const parent_component = current_component;
  set_current_component(component);
  const $$ = component.$$ = {
    fragment: null,
    ctx: null,
    props,
    update: noop,
    not_equal,
    bound: blank_object(),
    on_mount: [],
    on_destroy: [],
    on_disconnect: [],
    before_update: [],
    after_update: [],
    context: new Map(options.context || (parent_component ? parent_component.$$.context : [])),
    callbacks: blank_object(),
    dirty,
    skip_bound: false,
    root: options.target || parent_component.$$.root
  };
  append_styles && append_styles($$.root);
  let ready = false;
  $$.ctx = instance2 ? instance2(component, options.props || {}, (i2, ret, ...rest) => {
    const value = rest.length ? rest[0] : ret;
    if ($$.ctx && not_equal($$.ctx[i2], $$.ctx[i2] = value)) {
      if (!$$.skip_bound && $$.bound[i2])
        $$.bound[i2](value);
      if (ready)
        make_dirty(component, i2);
    }
    return ret;
  }) : [];
  $$.update();
  ready = true;
  run_all($$.before_update);
  $$.fragment = create_fragment2 ? create_fragment2($$.ctx) : false;
  if (options.target) {
    if (options.hydrate) {
      const nodes = children(options.target);
      $$.fragment && $$.fragment.l(nodes);
      nodes.forEach(detach);
    } else {
      $$.fragment && $$.fragment.c();
    }
    if (options.intro)
      transition_in(component.$$.fragment);
    mount_component(component, options.target, options.anchor, options.customElement);
    flush();
  }
  set_current_component(parent_component);
}
class SvelteComponent {
  $destroy() {
    destroy_component(this, 1);
    this.$destroy = noop;
  }
  $on(type, callback) {
    const callbacks = this.$$.callbacks[type] || (this.$$.callbacks[type] = []);
    callbacks.push(callback);
    return () => {
      const index = callbacks.indexOf(callback);
      if (index !== -1)
        callbacks.splice(index, 1);
    };
  }
  $set($$props) {
    if (this.$$set && !is_empty($$props)) {
      this.$$.skip_bound = true;
      this.$$set($$props);
      this.$$.skip_bound = false;
    }
  }
}
const subscriber_queue = [];
function readable(value, start) {
  return {
    subscribe: writable(value, start).subscribe
  };
}
function writable(value, start = noop) {
  let stop;
  const subscribers = /* @__PURE__ */ new Set();
  function set(new_value) {
    if (safe_not_equal(value, new_value)) {
      value = new_value;
      if (stop) {
        const run_queue = !subscriber_queue.length;
        for (const subscriber of subscribers) {
          subscriber[1]();
          subscriber_queue.push(subscriber, value);
        }
        if (run_queue) {
          for (let i2 = 0; i2 < subscriber_queue.length; i2 += 2) {
            subscriber_queue[i2][0](subscriber_queue[i2 + 1]);
          }
          subscriber_queue.length = 0;
        }
      }
    }
  }
  function update2(fn2) {
    set(fn2(value));
  }
  function subscribe2(run2, invalidate = noop) {
    const subscriber = [run2, invalidate];
    subscribers.add(subscriber);
    if (subscribers.size === 1) {
      stop = start(set) || noop;
    }
    run2(value);
    return () => {
      subscribers.delete(subscriber);
      if (subscribers.size === 0) {
        stop();
        stop = null;
      }
    };
  }
  return { set, update: update2, subscribe: subscribe2 };
}
function derived(stores, fn2, initial_value) {
  const single = !Array.isArray(stores);
  const stores_array = single ? [stores] : stores;
  const auto = fn2.length < 2;
  return readable(initial_value, (set) => {
    let inited = false;
    const values = [];
    let pending = 0;
    let cleanup = noop;
    const sync = () => {
      if (pending) {
        return;
      }
      cleanup();
      const result = fn2(single ? values[0] : values, set);
      if (auto) {
        set(result);
      } else {
        cleanup = is_function(result) ? result : noop;
      }
    };
    const unsubscribers = stores_array.map((store, i2) => subscribe(store, (value) => {
      values[i2] = value;
      pending &= ~(1 << i2);
      if (inited) {
        sync();
      }
    }, () => {
      pending |= 1 << i2;
    }));
    inited = true;
    sync();
    return function stop() {
      run_all(unsubscribers);
      cleanup();
    };
  });
}
var isMergeableObject = function isMergeableObject2(value) {
  return isNonNullObject(value) && !isSpecial(value);
};
function isNonNullObject(value) {
  return !!value && typeof value === "object";
}
function isSpecial(value) {
  var stringValue = Object.prototype.toString.call(value);
  return stringValue === "[object RegExp]" || stringValue === "[object Date]" || isReactElement(value);
}
var canUseSymbol = typeof Symbol === "function" && Symbol.for;
var REACT_ELEMENT_TYPE = canUseSymbol ? Symbol.for("react.element") : 60103;
function isReactElement(value) {
  return value.$$typeof === REACT_ELEMENT_TYPE;
}
function emptyTarget(val) {
  return Array.isArray(val) ? [] : {};
}
function cloneUnlessOtherwiseSpecified(value, options) {
  return options.clone !== false && options.isMergeableObject(value) ? deepmerge(emptyTarget(value), value, options) : value;
}
function defaultArrayMerge(target, source, options) {
  return target.concat(source).map(function(element2) {
    return cloneUnlessOtherwiseSpecified(element2, options);
  });
}
function getMergeFunction(key, options) {
  if (!options.customMerge) {
    return deepmerge;
  }
  var customMerge = options.customMerge(key);
  return typeof customMerge === "function" ? customMerge : deepmerge;
}
function getEnumerableOwnPropertySymbols(target) {
  return Object.getOwnPropertySymbols ? Object.getOwnPropertySymbols(target).filter(function(symbol) {
    return target.propertyIsEnumerable(symbol);
  }) : [];
}
function getKeys(target) {
  return Object.keys(target).concat(getEnumerableOwnPropertySymbols(target));
}
function propertyIsOnObject(object, property) {
  try {
    return property in object;
  } catch (_2) {
    return false;
  }
}
function propertyIsUnsafe(target, key) {
  return propertyIsOnObject(target, key) && !(Object.hasOwnProperty.call(target, key) && Object.propertyIsEnumerable.call(target, key));
}
function mergeObject(target, source, options) {
  var destination = {};
  if (options.isMergeableObject(target)) {
    getKeys(target).forEach(function(key) {
      destination[key] = cloneUnlessOtherwiseSpecified(target[key], options);
    });
  }
  getKeys(source).forEach(function(key) {
    if (propertyIsUnsafe(target, key)) {
      return;
    }
    if (propertyIsOnObject(target, key) && options.isMergeableObject(source[key])) {
      destination[key] = getMergeFunction(key, options)(target[key], source[key], options);
    } else {
      destination[key] = cloneUnlessOtherwiseSpecified(source[key], options);
    }
  });
  return destination;
}
function deepmerge(target, source, options) {
  options = options || {};
  options.arrayMerge = options.arrayMerge || defaultArrayMerge;
  options.isMergeableObject = options.isMergeableObject || isMergeableObject;
  options.cloneUnlessOtherwiseSpecified = cloneUnlessOtherwiseSpecified;
  var sourceIsArray = Array.isArray(source);
  var targetIsArray = Array.isArray(target);
  var sourceAndTargetTypesMatch = sourceIsArray === targetIsArray;
  if (!sourceAndTargetTypesMatch) {
    return cloneUnlessOtherwiseSpecified(source, options);
  } else if (sourceIsArray) {
    return options.arrayMerge(target, source, options);
  } else {
    return mergeObject(target, source, options);
  }
}
deepmerge.all = function deepmergeAll(array, options) {
  if (!Array.isArray(array)) {
    throw new Error("first argument should be an array");
  }
  return array.reduce(function(prev, next) {
    return deepmerge(prev, next, options);
  }, {});
};
var deepmerge_1 = deepmerge;
var cjs = deepmerge_1;
var extendStatics = function(d2, b2) {
  extendStatics = Object.setPrototypeOf || { __proto__: [] } instanceof Array && function(d3, b3) {
    d3.__proto__ = b3;
  } || function(d3, b3) {
    for (var p2 in b3)
      if (Object.prototype.hasOwnProperty.call(b3, p2))
        d3[p2] = b3[p2];
  };
  return extendStatics(d2, b2);
};
function __extends(d2, b2) {
  if (typeof b2 !== "function" && b2 !== null)
    throw new TypeError("Class extends value " + String(b2) + " is not a constructor or null");
  extendStatics(d2, b2);
  function __() {
    this.constructor = d2;
  }
  d2.prototype = b2 === null ? Object.create(b2) : (__.prototype = b2.prototype, new __());
}
var __assign = function() {
  __assign = Object.assign || function __assign2(t) {
    for (var s2, i2 = 1, n = arguments.length; i2 < n; i2++) {
      s2 = arguments[i2];
      for (var p2 in s2)
        if (Object.prototype.hasOwnProperty.call(s2, p2))
          t[p2] = s2[p2];
    }
    return t;
  };
  return __assign.apply(this, arguments);
};
function __spreadArray(to, from, pack) {
  if (pack || arguments.length === 2)
    for (var i2 = 0, l2 = from.length, ar2; i2 < l2; i2++) {
      if (ar2 || !(i2 in from)) {
        if (!ar2)
          ar2 = Array.prototype.slice.call(from, 0, i2);
        ar2[i2] = from[i2];
      }
    }
  return to.concat(ar2 || Array.prototype.slice.call(from));
}
var ErrorKind;
(function(ErrorKind2) {
  ErrorKind2[ErrorKind2["EXPECT_ARGUMENT_CLOSING_BRACE"] = 1] = "EXPECT_ARGUMENT_CLOSING_BRACE";
  ErrorKind2[ErrorKind2["EMPTY_ARGUMENT"] = 2] = "EMPTY_ARGUMENT";
  ErrorKind2[ErrorKind2["MALFORMED_ARGUMENT"] = 3] = "MALFORMED_ARGUMENT";
  ErrorKind2[ErrorKind2["EXPECT_ARGUMENT_TYPE"] = 4] = "EXPECT_ARGUMENT_TYPE";
  ErrorKind2[ErrorKind2["INVALID_ARGUMENT_TYPE"] = 5] = "INVALID_ARGUMENT_TYPE";
  ErrorKind2[ErrorKind2["EXPECT_ARGUMENT_STYLE"] = 6] = "EXPECT_ARGUMENT_STYLE";
  ErrorKind2[ErrorKind2["INVALID_NUMBER_SKELETON"] = 7] = "INVALID_NUMBER_SKELETON";
  ErrorKind2[ErrorKind2["INVALID_DATE_TIME_SKELETON"] = 8] = "INVALID_DATE_TIME_SKELETON";
  ErrorKind2[ErrorKind2["EXPECT_NUMBER_SKELETON"] = 9] = "EXPECT_NUMBER_SKELETON";
  ErrorKind2[ErrorKind2["EXPECT_DATE_TIME_SKELETON"] = 10] = "EXPECT_DATE_TIME_SKELETON";
  ErrorKind2[ErrorKind2["UNCLOSED_QUOTE_IN_ARGUMENT_STYLE"] = 11] = "UNCLOSED_QUOTE_IN_ARGUMENT_STYLE";
  ErrorKind2[ErrorKind2["EXPECT_SELECT_ARGUMENT_OPTIONS"] = 12] = "EXPECT_SELECT_ARGUMENT_OPTIONS";
  ErrorKind2[ErrorKind2["EXPECT_PLURAL_ARGUMENT_OFFSET_VALUE"] = 13] = "EXPECT_PLURAL_ARGUMENT_OFFSET_VALUE";
  ErrorKind2[ErrorKind2["INVALID_PLURAL_ARGUMENT_OFFSET_VALUE"] = 14] = "INVALID_PLURAL_ARGUMENT_OFFSET_VALUE";
  ErrorKind2[ErrorKind2["EXPECT_SELECT_ARGUMENT_SELECTOR"] = 15] = "EXPECT_SELECT_ARGUMENT_SELECTOR";
  ErrorKind2[ErrorKind2["EXPECT_PLURAL_ARGUMENT_SELECTOR"] = 16] = "EXPECT_PLURAL_ARGUMENT_SELECTOR";
  ErrorKind2[ErrorKind2["EXPECT_SELECT_ARGUMENT_SELECTOR_FRAGMENT"] = 17] = "EXPECT_SELECT_ARGUMENT_SELECTOR_FRAGMENT";
  ErrorKind2[ErrorKind2["EXPECT_PLURAL_ARGUMENT_SELECTOR_FRAGMENT"] = 18] = "EXPECT_PLURAL_ARGUMENT_SELECTOR_FRAGMENT";
  ErrorKind2[ErrorKind2["INVALID_PLURAL_ARGUMENT_SELECTOR"] = 19] = "INVALID_PLURAL_ARGUMENT_SELECTOR";
  ErrorKind2[ErrorKind2["DUPLICATE_PLURAL_ARGUMENT_SELECTOR"] = 20] = "DUPLICATE_PLURAL_ARGUMENT_SELECTOR";
  ErrorKind2[ErrorKind2["DUPLICATE_SELECT_ARGUMENT_SELECTOR"] = 21] = "DUPLICATE_SELECT_ARGUMENT_SELECTOR";
  ErrorKind2[ErrorKind2["MISSING_OTHER_CLAUSE"] = 22] = "MISSING_OTHER_CLAUSE";
  ErrorKind2[ErrorKind2["INVALID_TAG"] = 23] = "INVALID_TAG";
  ErrorKind2[ErrorKind2["INVALID_TAG_NAME"] = 25] = "INVALID_TAG_NAME";
  ErrorKind2[ErrorKind2["UNMATCHED_CLOSING_TAG"] = 26] = "UNMATCHED_CLOSING_TAG";
  ErrorKind2[ErrorKind2["UNCLOSED_TAG"] = 27] = "UNCLOSED_TAG";
})(ErrorKind || (ErrorKind = {}));
var TYPE;
(function(TYPE2) {
  TYPE2[TYPE2["literal"] = 0] = "literal";
  TYPE2[TYPE2["argument"] = 1] = "argument";
  TYPE2[TYPE2["number"] = 2] = "number";
  TYPE2[TYPE2["date"] = 3] = "date";
  TYPE2[TYPE2["time"] = 4] = "time";
  TYPE2[TYPE2["select"] = 5] = "select";
  TYPE2[TYPE2["plural"] = 6] = "plural";
  TYPE2[TYPE2["pound"] = 7] = "pound";
  TYPE2[TYPE2["tag"] = 8] = "tag";
})(TYPE || (TYPE = {}));
var SKELETON_TYPE;
(function(SKELETON_TYPE2) {
  SKELETON_TYPE2[SKELETON_TYPE2["number"] = 0] = "number";
  SKELETON_TYPE2[SKELETON_TYPE2["dateTime"] = 1] = "dateTime";
})(SKELETON_TYPE || (SKELETON_TYPE = {}));
function isLiteralElement(el) {
  return el.type === TYPE.literal;
}
function isArgumentElement(el) {
  return el.type === TYPE.argument;
}
function isNumberElement(el) {
  return el.type === TYPE.number;
}
function isDateElement(el) {
  return el.type === TYPE.date;
}
function isTimeElement(el) {
  return el.type === TYPE.time;
}
function isSelectElement(el) {
  return el.type === TYPE.select;
}
function isPluralElement(el) {
  return el.type === TYPE.plural;
}
function isPoundElement(el) {
  return el.type === TYPE.pound;
}
function isTagElement(el) {
  return el.type === TYPE.tag;
}
function isNumberSkeleton(el) {
  return !!(el && typeof el === "object" && el.type === SKELETON_TYPE.number);
}
function isDateTimeSkeleton(el) {
  return !!(el && typeof el === "object" && el.type === SKELETON_TYPE.dateTime);
}
var SPACE_SEPARATOR_REGEX = /[ \xA0\u1680\u2000-\u200A\u202F\u205F\u3000]/;
var DATE_TIME_REGEX = /(?:[Eec]{1,6}|G{1,5}|[Qq]{1,5}|(?:[yYur]+|U{1,5})|[ML]{1,5}|d{1,2}|D{1,3}|F{1}|[abB]{1,5}|[hkHK]{1,2}|w{1,2}|W{1}|m{1,2}|s{1,2}|[zZOvVxX]{1,4})(?=([^']*'[^']*')*[^']*$)/g;
function parseDateTimeSkeleton(skeleton) {
  var result = {};
  skeleton.replace(DATE_TIME_REGEX, function(match) {
    var len = match.length;
    switch (match[0]) {
      case "G":
        result.era = len === 4 ? "long" : len === 5 ? "narrow" : "short";
        break;
      case "y":
        result.year = len === 2 ? "2-digit" : "numeric";
        break;
      case "Y":
      case "u":
      case "U":
      case "r":
        throw new RangeError("`Y/u/U/r` (year) patterns are not supported, use `y` instead");
      case "q":
      case "Q":
        throw new RangeError("`q/Q` (quarter) patterns are not supported");
      case "M":
      case "L":
        result.month = ["numeric", "2-digit", "short", "long", "narrow"][len - 1];
        break;
      case "w":
      case "W":
        throw new RangeError("`w/W` (week) patterns are not supported");
      case "d":
        result.day = ["numeric", "2-digit"][len - 1];
        break;
      case "D":
      case "F":
      case "g":
        throw new RangeError("`D/F/g` (day) patterns are not supported, use `d` instead");
      case "E":
        result.weekday = len === 4 ? "short" : len === 5 ? "narrow" : "short";
        break;
      case "e":
        if (len < 4) {
          throw new RangeError("`e..eee` (weekday) patterns are not supported");
        }
        result.weekday = ["short", "long", "narrow", "short"][len - 4];
        break;
      case "c":
        if (len < 4) {
          throw new RangeError("`c..ccc` (weekday) patterns are not supported");
        }
        result.weekday = ["short", "long", "narrow", "short"][len - 4];
        break;
      case "a":
        result.hour12 = true;
        break;
      case "b":
      case "B":
        throw new RangeError("`b/B` (period) patterns are not supported, use `a` instead");
      case "h":
        result.hourCycle = "h12";
        result.hour = ["numeric", "2-digit"][len - 1];
        break;
      case "H":
        result.hourCycle = "h23";
        result.hour = ["numeric", "2-digit"][len - 1];
        break;
      case "K":
        result.hourCycle = "h11";
        result.hour = ["numeric", "2-digit"][len - 1];
        break;
      case "k":
        result.hourCycle = "h24";
        result.hour = ["numeric", "2-digit"][len - 1];
        break;
      case "j":
      case "J":
      case "C":
        throw new RangeError("`j/J/C` (hour) patterns are not supported, use `h/H/K/k` instead");
      case "m":
        result.minute = ["numeric", "2-digit"][len - 1];
        break;
      case "s":
        result.second = ["numeric", "2-digit"][len - 1];
        break;
      case "S":
      case "A":
        throw new RangeError("`S/A` (second) patterns are not supported, use `s` instead");
      case "z":
        result.timeZoneName = len < 4 ? "short" : "long";
        break;
      case "Z":
      case "O":
      case "v":
      case "V":
      case "X":
      case "x":
        throw new RangeError("`Z/O/v/V/X/x` (timeZone) patterns are not supported, use `z` instead");
    }
    return "";
  });
  return result;
}
var WHITE_SPACE_REGEX = /[\t-\r \x85\u200E\u200F\u2028\u2029]/i;
function parseNumberSkeletonFromString(skeleton) {
  if (skeleton.length === 0) {
    throw new Error("Number skeleton cannot be empty");
  }
  var stringTokens = skeleton.split(WHITE_SPACE_REGEX).filter(function(x2) {
    return x2.length > 0;
  });
  var tokens2 = [];
  for (var _i = 0, stringTokens_1 = stringTokens; _i < stringTokens_1.length; _i++) {
    var stringToken = stringTokens_1[_i];
    var stemAndOptions = stringToken.split("/");
    if (stemAndOptions.length === 0) {
      throw new Error("Invalid number skeleton");
    }
    var stem = stemAndOptions[0], options = stemAndOptions.slice(1);
    for (var _a2 = 0, options_1 = options; _a2 < options_1.length; _a2++) {
      var option = options_1[_a2];
      if (option.length === 0) {
        throw new Error("Invalid number skeleton");
      }
    }
    tokens2.push({ stem, options });
  }
  return tokens2;
}
function icuUnitToEcma(unit) {
  return unit.replace(/^(.*?)-/, "");
}
var FRACTION_PRECISION_REGEX = /^\.(?:(0+)(\*)?|(#+)|(0+)(#+))$/g;
var SIGNIFICANT_PRECISION_REGEX = /^(@+)?(\+|#+)?[rs]?$/g;
var INTEGER_WIDTH_REGEX = /(\*)(0+)|(#+)(0+)|(0+)/g;
var CONCISE_INTEGER_WIDTH_REGEX = /^(0+)$/;
function parseSignificantPrecision(str) {
  var result = {};
  if (str[str.length - 1] === "r") {
    result.roundingPriority = "morePrecision";
  } else if (str[str.length - 1] === "s") {
    result.roundingPriority = "lessPrecision";
  }
  str.replace(SIGNIFICANT_PRECISION_REGEX, function(_2, g1, g2) {
    if (typeof g2 !== "string") {
      result.minimumSignificantDigits = g1.length;
      result.maximumSignificantDigits = g1.length;
    } else if (g2 === "+") {
      result.minimumSignificantDigits = g1.length;
    } else if (g1[0] === "#") {
      result.maximumSignificantDigits = g1.length;
    } else {
      result.minimumSignificantDigits = g1.length;
      result.maximumSignificantDigits = g1.length + (typeof g2 === "string" ? g2.length : 0);
    }
    return "";
  });
  return result;
}
function parseSign(str) {
  switch (str) {
    case "sign-auto":
      return {
        signDisplay: "auto"
      };
    case "sign-accounting":
    case "()":
      return {
        currencySign: "accounting"
      };
    case "sign-always":
    case "+!":
      return {
        signDisplay: "always"
      };
    case "sign-accounting-always":
    case "()!":
      return {
        signDisplay: "always",
        currencySign: "accounting"
      };
    case "sign-except-zero":
    case "+?":
      return {
        signDisplay: "exceptZero"
      };
    case "sign-accounting-except-zero":
    case "()?":
      return {
        signDisplay: "exceptZero",
        currencySign: "accounting"
      };
    case "sign-never":
    case "+_":
      return {
        signDisplay: "never"
      };
  }
}
function parseConciseScientificAndEngineeringStem(stem) {
  var result;
  if (stem[0] === "E" && stem[1] === "E") {
    result = {
      notation: "engineering"
    };
    stem = stem.slice(2);
  } else if (stem[0] === "E") {
    result = {
      notation: "scientific"
    };
    stem = stem.slice(1);
  }
  if (result) {
    var signDisplay = stem.slice(0, 2);
    if (signDisplay === "+!") {
      result.signDisplay = "always";
      stem = stem.slice(2);
    } else if (signDisplay === "+?") {
      result.signDisplay = "exceptZero";
      stem = stem.slice(2);
    }
    if (!CONCISE_INTEGER_WIDTH_REGEX.test(stem)) {
      throw new Error("Malformed concise eng/scientific notation");
    }
    result.minimumIntegerDigits = stem.length;
  }
  return result;
}
function parseNotationOptions(opt) {
  var result = {};
  var signOpts = parseSign(opt);
  if (signOpts) {
    return signOpts;
  }
  return result;
}
function parseNumberSkeleton(tokens2) {
  var result = {};
  for (var _i = 0, tokens_1 = tokens2; _i < tokens_1.length; _i++) {
    var token = tokens_1[_i];
    switch (token.stem) {
      case "percent":
      case "%":
        result.style = "percent";
        continue;
      case "%x100":
        result.style = "percent";
        result.scale = 100;
        continue;
      case "currency":
        result.style = "currency";
        result.currency = token.options[0];
        continue;
      case "group-off":
      case ",_":
        result.useGrouping = false;
        continue;
      case "precision-integer":
      case ".":
        result.maximumFractionDigits = 0;
        continue;
      case "measure-unit":
      case "unit":
        result.style = "unit";
        result.unit = icuUnitToEcma(token.options[0]);
        continue;
      case "compact-short":
      case "K":
        result.notation = "compact";
        result.compactDisplay = "short";
        continue;
      case "compact-long":
      case "KK":
        result.notation = "compact";
        result.compactDisplay = "long";
        continue;
      case "scientific":
        result = __assign(__assign(__assign({}, result), { notation: "scientific" }), token.options.reduce(function(all, opt2) {
          return __assign(__assign({}, all), parseNotationOptions(opt2));
        }, {}));
        continue;
      case "engineering":
        result = __assign(__assign(__assign({}, result), { notation: "engineering" }), token.options.reduce(function(all, opt2) {
          return __assign(__assign({}, all), parseNotationOptions(opt2));
        }, {}));
        continue;
      case "notation-simple":
        result.notation = "standard";
        continue;
      case "unit-width-narrow":
        result.currencyDisplay = "narrowSymbol";
        result.unitDisplay = "narrow";
        continue;
      case "unit-width-short":
        result.currencyDisplay = "code";
        result.unitDisplay = "short";
        continue;
      case "unit-width-full-name":
        result.currencyDisplay = "name";
        result.unitDisplay = "long";
        continue;
      case "unit-width-iso-code":
        result.currencyDisplay = "symbol";
        continue;
      case "scale":
        result.scale = parseFloat(token.options[0]);
        continue;
      case "integer-width":
        if (token.options.length > 1) {
          throw new RangeError("integer-width stems only accept a single optional option");
        }
        token.options[0].replace(INTEGER_WIDTH_REGEX, function(_2, g1, g2, g3, g4, g5) {
          if (g1) {
            result.minimumIntegerDigits = g2.length;
          } else if (g3 && g4) {
            throw new Error("We currently do not support maximum integer digits");
          } else if (g5) {
            throw new Error("We currently do not support exact integer digits");
          }
          return "";
        });
        continue;
    }
    if (CONCISE_INTEGER_WIDTH_REGEX.test(token.stem)) {
      result.minimumIntegerDigits = token.stem.length;
      continue;
    }
    if (FRACTION_PRECISION_REGEX.test(token.stem)) {
      if (token.options.length > 1) {
        throw new RangeError("Fraction-precision stems only accept a single optional option");
      }
      token.stem.replace(FRACTION_PRECISION_REGEX, function(_2, g1, g2, g3, g4, g5) {
        if (g2 === "*") {
          result.minimumFractionDigits = g1.length;
        } else if (g3 && g3[0] === "#") {
          result.maximumFractionDigits = g3.length;
        } else if (g4 && g5) {
          result.minimumFractionDigits = g4.length;
          result.maximumFractionDigits = g4.length + g5.length;
        } else {
          result.minimumFractionDigits = g1.length;
          result.maximumFractionDigits = g1.length;
        }
        return "";
      });
      var opt = token.options[0];
      if (opt === "w") {
        result = __assign(__assign({}, result), { trailingZeroDisplay: "stripIfInteger" });
      } else if (opt) {
        result = __assign(__assign({}, result), parseSignificantPrecision(opt));
      }
      continue;
    }
    if (SIGNIFICANT_PRECISION_REGEX.test(token.stem)) {
      result = __assign(__assign({}, result), parseSignificantPrecision(token.stem));
      continue;
    }
    var signOpts = parseSign(token.stem);
    if (signOpts) {
      result = __assign(__assign({}, result), signOpts);
    }
    var conciseScientificAndEngineeringOpts = parseConciseScientificAndEngineeringStem(token.stem);
    if (conciseScientificAndEngineeringOpts) {
      result = __assign(__assign({}, result), conciseScientificAndEngineeringOpts);
    }
  }
  return result;
}
var _a;
var SPACE_SEPARATOR_START_REGEX = new RegExp("^".concat(SPACE_SEPARATOR_REGEX.source, "*"));
var SPACE_SEPARATOR_END_REGEX = new RegExp("".concat(SPACE_SEPARATOR_REGEX.source, "*$"));
function createLocation(start, end) {
  return { start, end };
}
var hasNativeStartsWith = !!String.prototype.startsWith;
var hasNativeFromCodePoint = !!String.fromCodePoint;
var hasNativeFromEntries = !!Object.fromEntries;
var hasNativeCodePointAt = !!String.prototype.codePointAt;
var hasTrimStart = !!String.prototype.trimStart;
var hasTrimEnd = !!String.prototype.trimEnd;
var hasNativeIsSafeInteger = !!Number.isSafeInteger;
var isSafeInteger = hasNativeIsSafeInteger ? Number.isSafeInteger : function(n) {
  return typeof n === "number" && isFinite(n) && Math.floor(n) === n && Math.abs(n) <= 9007199254740991;
};
var REGEX_SUPPORTS_U_AND_Y = true;
try {
  var re = RE("([^\\p{White_Space}\\p{Pattern_Syntax}]*)", "yu");
  REGEX_SUPPORTS_U_AND_Y = ((_a = re.exec("a")) === null || _a === void 0 ? void 0 : _a[0]) === "a";
} catch (_2) {
  REGEX_SUPPORTS_U_AND_Y = false;
}
var startsWith = hasNativeStartsWith ? function startsWith2(s2, search, position) {
  return s2.startsWith(search, position);
} : function startsWith3(s2, search, position) {
  return s2.slice(position, position + search.length) === search;
};
var fromCodePoint = hasNativeFromCodePoint ? String.fromCodePoint : function fromCodePoint2() {
  var codePoints = [];
  for (var _i = 0; _i < arguments.length; _i++) {
    codePoints[_i] = arguments[_i];
  }
  var elements = "";
  var length = codePoints.length;
  var i2 = 0;
  var code;
  while (length > i2) {
    code = codePoints[i2++];
    if (code > 1114111)
      throw RangeError(code + " is not a valid code point");
    elements += code < 65536 ? String.fromCharCode(code) : String.fromCharCode(((code -= 65536) >> 10) + 55296, code % 1024 + 56320);
  }
  return elements;
};
var fromEntries = hasNativeFromEntries ? Object.fromEntries : function fromEntries2(entries) {
  var obj = {};
  for (var _i = 0, entries_1 = entries; _i < entries_1.length; _i++) {
    var _a2 = entries_1[_i], k2 = _a2[0], v2 = _a2[1];
    obj[k2] = v2;
  }
  return obj;
};
var codePointAt = hasNativeCodePointAt ? function codePointAt2(s2, index) {
  return s2.codePointAt(index);
} : function codePointAt3(s2, index) {
  var size = s2.length;
  if (index < 0 || index >= size) {
    return void 0;
  }
  var first = s2.charCodeAt(index);
  var second;
  return first < 55296 || first > 56319 || index + 1 === size || (second = s2.charCodeAt(index + 1)) < 56320 || second > 57343 ? first : (first - 55296 << 10) + (second - 56320) + 65536;
};
var trimStart = hasTrimStart ? function trimStart2(s2) {
  return s2.trimStart();
} : function trimStart3(s2) {
  return s2.replace(SPACE_SEPARATOR_START_REGEX, "");
};
var trimEnd = hasTrimEnd ? function trimEnd2(s2) {
  return s2.trimEnd();
} : function trimEnd3(s2) {
  return s2.replace(SPACE_SEPARATOR_END_REGEX, "");
};
function RE(s2, flag) {
  return new RegExp(s2, flag);
}
var matchIdentifierAtIndex;
if (REGEX_SUPPORTS_U_AND_Y) {
  var IDENTIFIER_PREFIX_RE_1 = RE("([^\\p{White_Space}\\p{Pattern_Syntax}]*)", "yu");
  matchIdentifierAtIndex = function matchIdentifierAtIndex2(s2, index) {
    var _a2;
    IDENTIFIER_PREFIX_RE_1.lastIndex = index;
    var match = IDENTIFIER_PREFIX_RE_1.exec(s2);
    return (_a2 = match[1]) !== null && _a2 !== void 0 ? _a2 : "";
  };
} else {
  matchIdentifierAtIndex = function matchIdentifierAtIndex2(s2, index) {
    var match = [];
    while (true) {
      var c2 = codePointAt(s2, index);
      if (c2 === void 0 || _isWhiteSpace(c2) || _isPatternSyntax(c2)) {
        break;
      }
      match.push(c2);
      index += c2 >= 65536 ? 2 : 1;
    }
    return fromCodePoint.apply(void 0, match);
  };
}
var Parser = function() {
  function Parser2(message, options) {
    if (options === void 0) {
      options = {};
    }
    this.message = message;
    this.position = { offset: 0, line: 1, column: 1 };
    this.ignoreTag = !!options.ignoreTag;
    this.requiresOtherClause = !!options.requiresOtherClause;
    this.shouldParseSkeletons = !!options.shouldParseSkeletons;
  }
  Parser2.prototype.parse = function() {
    if (this.offset() !== 0) {
      throw Error("parser can only be used once");
    }
    return this.parseMessage(0, "", false);
  };
  Parser2.prototype.parseMessage = function(nestingLevel, parentArgType, expectingCloseTag) {
    var elements = [];
    while (!this.isEOF()) {
      var char = this.char();
      if (char === 123) {
        var result = this.parseArgument(nestingLevel, expectingCloseTag);
        if (result.err) {
          return result;
        }
        elements.push(result.val);
      } else if (char === 125 && nestingLevel > 0) {
        break;
      } else if (char === 35 && (parentArgType === "plural" || parentArgType === "selectordinal")) {
        var position = this.clonePosition();
        this.bump();
        elements.push({
          type: TYPE.pound,
          location: createLocation(position, this.clonePosition())
        });
      } else if (char === 60 && !this.ignoreTag && this.peek() === 47) {
        if (expectingCloseTag) {
          break;
        } else {
          return this.error(ErrorKind.UNMATCHED_CLOSING_TAG, createLocation(this.clonePosition(), this.clonePosition()));
        }
      } else if (char === 60 && !this.ignoreTag && _isAlpha(this.peek() || 0)) {
        var result = this.parseTag(nestingLevel, parentArgType);
        if (result.err) {
          return result;
        }
        elements.push(result.val);
      } else {
        var result = this.parseLiteral(nestingLevel, parentArgType);
        if (result.err) {
          return result;
        }
        elements.push(result.val);
      }
    }
    return { val: elements, err: null };
  };
  Parser2.prototype.parseTag = function(nestingLevel, parentArgType) {
    var startPosition = this.clonePosition();
    this.bump();
    var tagName = this.parseTagName();
    this.bumpSpace();
    if (this.bumpIf("/>")) {
      return {
        val: {
          type: TYPE.literal,
          value: "<".concat(tagName, "/>"),
          location: createLocation(startPosition, this.clonePosition())
        },
        err: null
      };
    } else if (this.bumpIf(">")) {
      var childrenResult = this.parseMessage(nestingLevel + 1, parentArgType, true);
      if (childrenResult.err) {
        return childrenResult;
      }
      var children2 = childrenResult.val;
      var endTagStartPosition = this.clonePosition();
      if (this.bumpIf("</")) {
        if (this.isEOF() || !_isAlpha(this.char())) {
          return this.error(ErrorKind.INVALID_TAG, createLocation(endTagStartPosition, this.clonePosition()));
        }
        var closingTagNameStartPosition = this.clonePosition();
        var closingTagName = this.parseTagName();
        if (tagName !== closingTagName) {
          return this.error(ErrorKind.UNMATCHED_CLOSING_TAG, createLocation(closingTagNameStartPosition, this.clonePosition()));
        }
        this.bumpSpace();
        if (!this.bumpIf(">")) {
          return this.error(ErrorKind.INVALID_TAG, createLocation(endTagStartPosition, this.clonePosition()));
        }
        return {
          val: {
            type: TYPE.tag,
            value: tagName,
            children: children2,
            location: createLocation(startPosition, this.clonePosition())
          },
          err: null
        };
      } else {
        return this.error(ErrorKind.UNCLOSED_TAG, createLocation(startPosition, this.clonePosition()));
      }
    } else {
      return this.error(ErrorKind.INVALID_TAG, createLocation(startPosition, this.clonePosition()));
    }
  };
  Parser2.prototype.parseTagName = function() {
    var startOffset = this.offset();
    this.bump();
    while (!this.isEOF() && _isPotentialElementNameChar(this.char())) {
      this.bump();
    }
    return this.message.slice(startOffset, this.offset());
  };
  Parser2.prototype.parseLiteral = function(nestingLevel, parentArgType) {
    var start = this.clonePosition();
    var value = "";
    while (true) {
      var parseQuoteResult = this.tryParseQuote(parentArgType);
      if (parseQuoteResult) {
        value += parseQuoteResult;
        continue;
      }
      var parseUnquotedResult = this.tryParseUnquoted(nestingLevel, parentArgType);
      if (parseUnquotedResult) {
        value += parseUnquotedResult;
        continue;
      }
      var parseLeftAngleResult = this.tryParseLeftAngleBracket();
      if (parseLeftAngleResult) {
        value += parseLeftAngleResult;
        continue;
      }
      break;
    }
    var location2 = createLocation(start, this.clonePosition());
    return {
      val: { type: TYPE.literal, value, location: location2 },
      err: null
    };
  };
  Parser2.prototype.tryParseLeftAngleBracket = function() {
    if (!this.isEOF() && this.char() === 60 && (this.ignoreTag || !_isAlphaOrSlash(this.peek() || 0))) {
      this.bump();
      return "<";
    }
    return null;
  };
  Parser2.prototype.tryParseQuote = function(parentArgType) {
    if (this.isEOF() || this.char() !== 39) {
      return null;
    }
    switch (this.peek()) {
      case 39:
        this.bump();
        this.bump();
        return "'";
      case 123:
      case 60:
      case 62:
      case 125:
        break;
      case 35:
        if (parentArgType === "plural" || parentArgType === "selectordinal") {
          break;
        }
        return null;
      default:
        return null;
    }
    this.bump();
    var codePoints = [this.char()];
    this.bump();
    while (!this.isEOF()) {
      var ch = this.char();
      if (ch === 39) {
        if (this.peek() === 39) {
          codePoints.push(39);
          this.bump();
        } else {
          this.bump();
          break;
        }
      } else {
        codePoints.push(ch);
      }
      this.bump();
    }
    return fromCodePoint.apply(void 0, codePoints);
  };
  Parser2.prototype.tryParseUnquoted = function(nestingLevel, parentArgType) {
    if (this.isEOF()) {
      return null;
    }
    var ch = this.char();
    if (ch === 60 || ch === 123 || ch === 35 && (parentArgType === "plural" || parentArgType === "selectordinal") || ch === 125 && nestingLevel > 0) {
      return null;
    } else {
      this.bump();
      return fromCodePoint(ch);
    }
  };
  Parser2.prototype.parseArgument = function(nestingLevel, expectingCloseTag) {
    var openingBracePosition = this.clonePosition();
    this.bump();
    this.bumpSpace();
    if (this.isEOF()) {
      return this.error(ErrorKind.EXPECT_ARGUMENT_CLOSING_BRACE, createLocation(openingBracePosition, this.clonePosition()));
    }
    if (this.char() === 125) {
      this.bump();
      return this.error(ErrorKind.EMPTY_ARGUMENT, createLocation(openingBracePosition, this.clonePosition()));
    }
    var value = this.parseIdentifierIfPossible().value;
    if (!value) {
      return this.error(ErrorKind.MALFORMED_ARGUMENT, createLocation(openingBracePosition, this.clonePosition()));
    }
    this.bumpSpace();
    if (this.isEOF()) {
      return this.error(ErrorKind.EXPECT_ARGUMENT_CLOSING_BRACE, createLocation(openingBracePosition, this.clonePosition()));
    }
    switch (this.char()) {
      case 125: {
        this.bump();
        return {
          val: {
            type: TYPE.argument,
            value,
            location: createLocation(openingBracePosition, this.clonePosition())
          },
          err: null
        };
      }
      case 44: {
        this.bump();
        this.bumpSpace();
        if (this.isEOF()) {
          return this.error(ErrorKind.EXPECT_ARGUMENT_CLOSING_BRACE, createLocation(openingBracePosition, this.clonePosition()));
        }
        return this.parseArgumentOptions(nestingLevel, expectingCloseTag, value, openingBracePosition);
      }
      default:
        return this.error(ErrorKind.MALFORMED_ARGUMENT, createLocation(openingBracePosition, this.clonePosition()));
    }
  };
  Parser2.prototype.parseIdentifierIfPossible = function() {
    var startingPosition = this.clonePosition();
    var startOffset = this.offset();
    var value = matchIdentifierAtIndex(this.message, startOffset);
    var endOffset = startOffset + value.length;
    this.bumpTo(endOffset);
    var endPosition = this.clonePosition();
    var location2 = createLocation(startingPosition, endPosition);
    return { value, location: location2 };
  };
  Parser2.prototype.parseArgumentOptions = function(nestingLevel, expectingCloseTag, value, openingBracePosition) {
    var _a2;
    var typeStartPosition = this.clonePosition();
    var argType = this.parseIdentifierIfPossible().value;
    var typeEndPosition = this.clonePosition();
    switch (argType) {
      case "":
        return this.error(ErrorKind.EXPECT_ARGUMENT_TYPE, createLocation(typeStartPosition, typeEndPosition));
      case "number":
      case "date":
      case "time": {
        this.bumpSpace();
        var styleAndLocation = null;
        if (this.bumpIf(",")) {
          this.bumpSpace();
          var styleStartPosition = this.clonePosition();
          var result = this.parseSimpleArgStyleIfPossible();
          if (result.err) {
            return result;
          }
          var style = trimEnd(result.val);
          if (style.length === 0) {
            return this.error(ErrorKind.EXPECT_ARGUMENT_STYLE, createLocation(this.clonePosition(), this.clonePosition()));
          }
          var styleLocation = createLocation(styleStartPosition, this.clonePosition());
          styleAndLocation = { style, styleLocation };
        }
        var argCloseResult = this.tryParseArgumentClose(openingBracePosition);
        if (argCloseResult.err) {
          return argCloseResult;
        }
        var location_1 = createLocation(openingBracePosition, this.clonePosition());
        if (styleAndLocation && startsWith(styleAndLocation === null || styleAndLocation === void 0 ? void 0 : styleAndLocation.style, "::", 0)) {
          var skeleton = trimStart(styleAndLocation.style.slice(2));
          if (argType === "number") {
            var result = this.parseNumberSkeletonFromString(skeleton, styleAndLocation.styleLocation);
            if (result.err) {
              return result;
            }
            return {
              val: { type: TYPE.number, value, location: location_1, style: result.val },
              err: null
            };
          } else {
            if (skeleton.length === 0) {
              return this.error(ErrorKind.EXPECT_DATE_TIME_SKELETON, location_1);
            }
            var style = {
              type: SKELETON_TYPE.dateTime,
              pattern: skeleton,
              location: styleAndLocation.styleLocation,
              parsedOptions: this.shouldParseSkeletons ? parseDateTimeSkeleton(skeleton) : {}
            };
            var type = argType === "date" ? TYPE.date : TYPE.time;
            return {
              val: { type, value, location: location_1, style },
              err: null
            };
          }
        }
        return {
          val: {
            type: argType === "number" ? TYPE.number : argType === "date" ? TYPE.date : TYPE.time,
            value,
            location: location_1,
            style: (_a2 = styleAndLocation === null || styleAndLocation === void 0 ? void 0 : styleAndLocation.style) !== null && _a2 !== void 0 ? _a2 : null
          },
          err: null
        };
      }
      case "plural":
      case "selectordinal":
      case "select": {
        var typeEndPosition_1 = this.clonePosition();
        this.bumpSpace();
        if (!this.bumpIf(",")) {
          return this.error(ErrorKind.EXPECT_SELECT_ARGUMENT_OPTIONS, createLocation(typeEndPosition_1, __assign({}, typeEndPosition_1)));
        }
        this.bumpSpace();
        var identifierAndLocation = this.parseIdentifierIfPossible();
        var pluralOffset = 0;
        if (argType !== "select" && identifierAndLocation.value === "offset") {
          if (!this.bumpIf(":")) {
            return this.error(ErrorKind.EXPECT_PLURAL_ARGUMENT_OFFSET_VALUE, createLocation(this.clonePosition(), this.clonePosition()));
          }
          this.bumpSpace();
          var result = this.tryParseDecimalInteger(ErrorKind.EXPECT_PLURAL_ARGUMENT_OFFSET_VALUE, ErrorKind.INVALID_PLURAL_ARGUMENT_OFFSET_VALUE);
          if (result.err) {
            return result;
          }
          this.bumpSpace();
          identifierAndLocation = this.parseIdentifierIfPossible();
          pluralOffset = result.val;
        }
        var optionsResult = this.tryParsePluralOrSelectOptions(nestingLevel, argType, expectingCloseTag, identifierAndLocation);
        if (optionsResult.err) {
          return optionsResult;
        }
        var argCloseResult = this.tryParseArgumentClose(openingBracePosition);
        if (argCloseResult.err) {
          return argCloseResult;
        }
        var location_2 = createLocation(openingBracePosition, this.clonePosition());
        if (argType === "select") {
          return {
            val: {
              type: TYPE.select,
              value,
              options: fromEntries(optionsResult.val),
              location: location_2
            },
            err: null
          };
        } else {
          return {
            val: {
              type: TYPE.plural,
              value,
              options: fromEntries(optionsResult.val),
              offset: pluralOffset,
              pluralType: argType === "plural" ? "cardinal" : "ordinal",
              location: location_2
            },
            err: null
          };
        }
      }
      default:
        return this.error(ErrorKind.INVALID_ARGUMENT_TYPE, createLocation(typeStartPosition, typeEndPosition));
    }
  };
  Parser2.prototype.tryParseArgumentClose = function(openingBracePosition) {
    if (this.isEOF() || this.char() !== 125) {
      return this.error(ErrorKind.EXPECT_ARGUMENT_CLOSING_BRACE, createLocation(openingBracePosition, this.clonePosition()));
    }
    this.bump();
    return { val: true, err: null };
  };
  Parser2.prototype.parseSimpleArgStyleIfPossible = function() {
    var nestedBraces = 0;
    var startPosition = this.clonePosition();
    while (!this.isEOF()) {
      var ch = this.char();
      switch (ch) {
        case 39: {
          this.bump();
          var apostrophePosition = this.clonePosition();
          if (!this.bumpUntil("'")) {
            return this.error(ErrorKind.UNCLOSED_QUOTE_IN_ARGUMENT_STYLE, createLocation(apostrophePosition, this.clonePosition()));
          }
          this.bump();
          break;
        }
        case 123: {
          nestedBraces += 1;
          this.bump();
          break;
        }
        case 125: {
          if (nestedBraces > 0) {
            nestedBraces -= 1;
          } else {
            return {
              val: this.message.slice(startPosition.offset, this.offset()),
              err: null
            };
          }
          break;
        }
        default:
          this.bump();
          break;
      }
    }
    return {
      val: this.message.slice(startPosition.offset, this.offset()),
      err: null
    };
  };
  Parser2.prototype.parseNumberSkeletonFromString = function(skeleton, location2) {
    var tokens2 = [];
    try {
      tokens2 = parseNumberSkeletonFromString(skeleton);
    } catch (e) {
      return this.error(ErrorKind.INVALID_NUMBER_SKELETON, location2);
    }
    return {
      val: {
        type: SKELETON_TYPE.number,
        tokens: tokens2,
        location: location2,
        parsedOptions: this.shouldParseSkeletons ? parseNumberSkeleton(tokens2) : {}
      },
      err: null
    };
  };
  Parser2.prototype.tryParsePluralOrSelectOptions = function(nestingLevel, parentArgType, expectCloseTag, parsedFirstIdentifier) {
    var _a2;
    var hasOtherClause = false;
    var options = [];
    var parsedSelectors = /* @__PURE__ */ new Set();
    var selector = parsedFirstIdentifier.value, selectorLocation = parsedFirstIdentifier.location;
    while (true) {
      if (selector.length === 0) {
        var startPosition = this.clonePosition();
        if (parentArgType !== "select" && this.bumpIf("=")) {
          var result = this.tryParseDecimalInteger(ErrorKind.EXPECT_PLURAL_ARGUMENT_SELECTOR, ErrorKind.INVALID_PLURAL_ARGUMENT_SELECTOR);
          if (result.err) {
            return result;
          }
          selectorLocation = createLocation(startPosition, this.clonePosition());
          selector = this.message.slice(startPosition.offset, this.offset());
        } else {
          break;
        }
      }
      if (parsedSelectors.has(selector)) {
        return this.error(parentArgType === "select" ? ErrorKind.DUPLICATE_SELECT_ARGUMENT_SELECTOR : ErrorKind.DUPLICATE_PLURAL_ARGUMENT_SELECTOR, selectorLocation);
      }
      if (selector === "other") {
        hasOtherClause = true;
      }
      this.bumpSpace();
      var openingBracePosition = this.clonePosition();
      if (!this.bumpIf("{")) {
        return this.error(parentArgType === "select" ? ErrorKind.EXPECT_SELECT_ARGUMENT_SELECTOR_FRAGMENT : ErrorKind.EXPECT_PLURAL_ARGUMENT_SELECTOR_FRAGMENT, createLocation(this.clonePosition(), this.clonePosition()));
      }
      var fragmentResult = this.parseMessage(nestingLevel + 1, parentArgType, expectCloseTag);
      if (fragmentResult.err) {
        return fragmentResult;
      }
      var argCloseResult = this.tryParseArgumentClose(openingBracePosition);
      if (argCloseResult.err) {
        return argCloseResult;
      }
      options.push([
        selector,
        {
          value: fragmentResult.val,
          location: createLocation(openingBracePosition, this.clonePosition())
        }
      ]);
      parsedSelectors.add(selector);
      this.bumpSpace();
      _a2 = this.parseIdentifierIfPossible(), selector = _a2.value, selectorLocation = _a2.location;
    }
    if (options.length === 0) {
      return this.error(parentArgType === "select" ? ErrorKind.EXPECT_SELECT_ARGUMENT_SELECTOR : ErrorKind.EXPECT_PLURAL_ARGUMENT_SELECTOR, createLocation(this.clonePosition(), this.clonePosition()));
    }
    if (this.requiresOtherClause && !hasOtherClause) {
      return this.error(ErrorKind.MISSING_OTHER_CLAUSE, createLocation(this.clonePosition(), this.clonePosition()));
    }
    return { val: options, err: null };
  };
  Parser2.prototype.tryParseDecimalInteger = function(expectNumberError, invalidNumberError) {
    var sign = 1;
    var startingPosition = this.clonePosition();
    if (this.bumpIf("+"))
      ;
    else if (this.bumpIf("-")) {
      sign = -1;
    }
    var hasDigits = false;
    var decimal = 0;
    while (!this.isEOF()) {
      var ch = this.char();
      if (ch >= 48 && ch <= 57) {
        hasDigits = true;
        decimal = decimal * 10 + (ch - 48);
        this.bump();
      } else {
        break;
      }
    }
    var location2 = createLocation(startingPosition, this.clonePosition());
    if (!hasDigits) {
      return this.error(expectNumberError, location2);
    }
    decimal *= sign;
    if (!isSafeInteger(decimal)) {
      return this.error(invalidNumberError, location2);
    }
    return { val: decimal, err: null };
  };
  Parser2.prototype.offset = function() {
    return this.position.offset;
  };
  Parser2.prototype.isEOF = function() {
    return this.offset() === this.message.length;
  };
  Parser2.prototype.clonePosition = function() {
    return {
      offset: this.position.offset,
      line: this.position.line,
      column: this.position.column
    };
  };
  Parser2.prototype.char = function() {
    var offset = this.position.offset;
    if (offset >= this.message.length) {
      throw Error("out of bound");
    }
    var code = codePointAt(this.message, offset);
    if (code === void 0) {
      throw Error("Offset ".concat(offset, " is at invalid UTF-16 code unit boundary"));
    }
    return code;
  };
  Parser2.prototype.error = function(kind, location2) {
    return {
      val: null,
      err: {
        kind,
        message: this.message,
        location: location2
      }
    };
  };
  Parser2.prototype.bump = function() {
    if (this.isEOF()) {
      return;
    }
    var code = this.char();
    if (code === 10) {
      this.position.line += 1;
      this.position.column = 1;
      this.position.offset += 1;
    } else {
      this.position.column += 1;
      this.position.offset += code < 65536 ? 1 : 2;
    }
  };
  Parser2.prototype.bumpIf = function(prefix) {
    if (startsWith(this.message, prefix, this.offset())) {
      for (var i2 = 0; i2 < prefix.length; i2++) {
        this.bump();
      }
      return true;
    }
    return false;
  };
  Parser2.prototype.bumpUntil = function(pattern) {
    var currentOffset = this.offset();
    var index = this.message.indexOf(pattern, currentOffset);
    if (index >= 0) {
      this.bumpTo(index);
      return true;
    } else {
      this.bumpTo(this.message.length);
      return false;
    }
  };
  Parser2.prototype.bumpTo = function(targetOffset) {
    if (this.offset() > targetOffset) {
      throw Error("targetOffset ".concat(targetOffset, " must be greater than or equal to the current offset ").concat(this.offset()));
    }
    targetOffset = Math.min(targetOffset, this.message.length);
    while (true) {
      var offset = this.offset();
      if (offset === targetOffset) {
        break;
      }
      if (offset > targetOffset) {
        throw Error("targetOffset ".concat(targetOffset, " is at invalid UTF-16 code unit boundary"));
      }
      this.bump();
      if (this.isEOF()) {
        break;
      }
    }
  };
  Parser2.prototype.bumpSpace = function() {
    while (!this.isEOF() && _isWhiteSpace(this.char())) {
      this.bump();
    }
  };
  Parser2.prototype.peek = function() {
    if (this.isEOF()) {
      return null;
    }
    var code = this.char();
    var offset = this.offset();
    var nextCode = this.message.charCodeAt(offset + (code >= 65536 ? 2 : 1));
    return nextCode !== null && nextCode !== void 0 ? nextCode : null;
  };
  return Parser2;
}();
function _isAlpha(codepoint) {
  return codepoint >= 97 && codepoint <= 122 || codepoint >= 65 && codepoint <= 90;
}
function _isAlphaOrSlash(codepoint) {
  return _isAlpha(codepoint) || codepoint === 47;
}
function _isPotentialElementNameChar(c2) {
  return c2 === 45 || c2 === 46 || c2 >= 48 && c2 <= 57 || c2 === 95 || c2 >= 97 && c2 <= 122 || c2 >= 65 && c2 <= 90 || c2 == 183 || c2 >= 192 && c2 <= 214 || c2 >= 216 && c2 <= 246 || c2 >= 248 && c2 <= 893 || c2 >= 895 && c2 <= 8191 || c2 >= 8204 && c2 <= 8205 || c2 >= 8255 && c2 <= 8256 || c2 >= 8304 && c2 <= 8591 || c2 >= 11264 && c2 <= 12271 || c2 >= 12289 && c2 <= 55295 || c2 >= 63744 && c2 <= 64975 || c2 >= 65008 && c2 <= 65533 || c2 >= 65536 && c2 <= 983039;
}
function _isWhiteSpace(c2) {
  return c2 >= 9 && c2 <= 13 || c2 === 32 || c2 === 133 || c2 >= 8206 && c2 <= 8207 || c2 === 8232 || c2 === 8233;
}
function _isPatternSyntax(c2) {
  return c2 >= 33 && c2 <= 35 || c2 === 36 || c2 >= 37 && c2 <= 39 || c2 === 40 || c2 === 41 || c2 === 42 || c2 === 43 || c2 === 44 || c2 === 45 || c2 >= 46 && c2 <= 47 || c2 >= 58 && c2 <= 59 || c2 >= 60 && c2 <= 62 || c2 >= 63 && c2 <= 64 || c2 === 91 || c2 === 92 || c2 === 93 || c2 === 94 || c2 === 96 || c2 === 123 || c2 === 124 || c2 === 125 || c2 === 126 || c2 === 161 || c2 >= 162 && c2 <= 165 || c2 === 166 || c2 === 167 || c2 === 169 || c2 === 171 || c2 === 172 || c2 === 174 || c2 === 176 || c2 === 177 || c2 === 182 || c2 === 187 || c2 === 191 || c2 === 215 || c2 === 247 || c2 >= 8208 && c2 <= 8213 || c2 >= 8214 && c2 <= 8215 || c2 === 8216 || c2 === 8217 || c2 === 8218 || c2 >= 8219 && c2 <= 8220 || c2 === 8221 || c2 === 8222 || c2 === 8223 || c2 >= 8224 && c2 <= 8231 || c2 >= 8240 && c2 <= 8248 || c2 === 8249 || c2 === 8250 || c2 >= 8251 && c2 <= 8254 || c2 >= 8257 && c2 <= 8259 || c2 === 8260 || c2 === 8261 || c2 === 8262 || c2 >= 8263 && c2 <= 8273 || c2 === 8274 || c2 === 8275 || c2 >= 8277 && c2 <= 8286 || c2 >= 8592 && c2 <= 8596 || c2 >= 8597 && c2 <= 8601 || c2 >= 8602 && c2 <= 8603 || c2 >= 8604 && c2 <= 8607 || c2 === 8608 || c2 >= 8609 && c2 <= 8610 || c2 === 8611 || c2 >= 8612 && c2 <= 8613 || c2 === 8614 || c2 >= 8615 && c2 <= 8621 || c2 === 8622 || c2 >= 8623 && c2 <= 8653 || c2 >= 8654 && c2 <= 8655 || c2 >= 8656 && c2 <= 8657 || c2 === 8658 || c2 === 8659 || c2 === 8660 || c2 >= 8661 && c2 <= 8691 || c2 >= 8692 && c2 <= 8959 || c2 >= 8960 && c2 <= 8967 || c2 === 8968 || c2 === 8969 || c2 === 8970 || c2 === 8971 || c2 >= 8972 && c2 <= 8991 || c2 >= 8992 && c2 <= 8993 || c2 >= 8994 && c2 <= 9e3 || c2 === 9001 || c2 === 9002 || c2 >= 9003 && c2 <= 9083 || c2 === 9084 || c2 >= 9085 && c2 <= 9114 || c2 >= 9115 && c2 <= 9139 || c2 >= 9140 && c2 <= 9179 || c2 >= 9180 && c2 <= 9185 || c2 >= 9186 && c2 <= 9254 || c2 >= 9255 && c2 <= 9279 || c2 >= 9280 && c2 <= 9290 || c2 >= 9291 && c2 <= 9311 || c2 >= 9472 && c2 <= 9654 || c2 === 9655 || c2 >= 9656 && c2 <= 9664 || c2 === 9665 || c2 >= 9666 && c2 <= 9719 || c2 >= 9720 && c2 <= 9727 || c2 >= 9728 && c2 <= 9838 || c2 === 9839 || c2 >= 9840 && c2 <= 10087 || c2 === 10088 || c2 === 10089 || c2 === 10090 || c2 === 10091 || c2 === 10092 || c2 === 10093 || c2 === 10094 || c2 === 10095 || c2 === 10096 || c2 === 10097 || c2 === 10098 || c2 === 10099 || c2 === 10100 || c2 === 10101 || c2 >= 10132 && c2 <= 10175 || c2 >= 10176 && c2 <= 10180 || c2 === 10181 || c2 === 10182 || c2 >= 10183 && c2 <= 10213 || c2 === 10214 || c2 === 10215 || c2 === 10216 || c2 === 10217 || c2 === 10218 || c2 === 10219 || c2 === 10220 || c2 === 10221 || c2 === 10222 || c2 === 10223 || c2 >= 10224 && c2 <= 10239 || c2 >= 10240 && c2 <= 10495 || c2 >= 10496 && c2 <= 10626 || c2 === 10627 || c2 === 10628 || c2 === 10629 || c2 === 10630 || c2 === 10631 || c2 === 10632 || c2 === 10633 || c2 === 10634 || c2 === 10635 || c2 === 10636 || c2 === 10637 || c2 === 10638 || c2 === 10639 || c2 === 10640 || c2 === 10641 || c2 === 10642 || c2 === 10643 || c2 === 10644 || c2 === 10645 || c2 === 10646 || c2 === 10647 || c2 === 10648 || c2 >= 10649 && c2 <= 10711 || c2 === 10712 || c2 === 10713 || c2 === 10714 || c2 === 10715 || c2 >= 10716 && c2 <= 10747 || c2 === 10748 || c2 === 10749 || c2 >= 10750 && c2 <= 11007 || c2 >= 11008 && c2 <= 11055 || c2 >= 11056 && c2 <= 11076 || c2 >= 11077 && c2 <= 11078 || c2 >= 11079 && c2 <= 11084 || c2 >= 11085 && c2 <= 11123 || c2 >= 11124 && c2 <= 11125 || c2 >= 11126 && c2 <= 11157 || c2 === 11158 || c2 >= 11159 && c2 <= 11263 || c2 >= 11776 && c2 <= 11777 || c2 === 11778 || c2 === 11779 || c2 === 11780 || c2 === 11781 || c2 >= 11782 && c2 <= 11784 || c2 === 11785 || c2 === 11786 || c2 === 11787 || c2 === 11788 || c2 === 11789 || c2 >= 11790 && c2 <= 11798 || c2 === 11799 || c2 >= 11800 && c2 <= 11801 || c2 === 11802 || c2 === 11803 || c2 === 11804 || c2 === 11805 || c2 >= 11806 && c2 <= 11807 || c2 === 11808 || c2 === 11809 || c2 === 11810 || c2 === 11811 || c2 === 11812 || c2 === 11813 || c2 === 11814 || c2 === 11815 || c2 === 11816 || c2 === 11817 || c2 >= 11818 && c2 <= 11822 || c2 === 11823 || c2 >= 11824 && c2 <= 11833 || c2 >= 11834 && c2 <= 11835 || c2 >= 11836 && c2 <= 11839 || c2 === 11840 || c2 === 11841 || c2 === 11842 || c2 >= 11843 && c2 <= 11855 || c2 >= 11856 && c2 <= 11857 || c2 === 11858 || c2 >= 11859 && c2 <= 11903 || c2 >= 12289 && c2 <= 12291 || c2 === 12296 || c2 === 12297 || c2 === 12298 || c2 === 12299 || c2 === 12300 || c2 === 12301 || c2 === 12302 || c2 === 12303 || c2 === 12304 || c2 === 12305 || c2 >= 12306 && c2 <= 12307 || c2 === 12308 || c2 === 12309 || c2 === 12310 || c2 === 12311 || c2 === 12312 || c2 === 12313 || c2 === 12314 || c2 === 12315 || c2 === 12316 || c2 === 12317 || c2 >= 12318 && c2 <= 12319 || c2 === 12320 || c2 === 12336 || c2 === 64830 || c2 === 64831 || c2 >= 65093 && c2 <= 65094;
}
function pruneLocation(els) {
  els.forEach(function(el) {
    delete el.location;
    if (isSelectElement(el) || isPluralElement(el)) {
      for (var k2 in el.options) {
        delete el.options[k2].location;
        pruneLocation(el.options[k2].value);
      }
    } else if (isNumberElement(el) && isNumberSkeleton(el.style)) {
      delete el.style.location;
    } else if ((isDateElement(el) || isTimeElement(el)) && isDateTimeSkeleton(el.style)) {
      delete el.style.location;
    } else if (isTagElement(el)) {
      pruneLocation(el.children);
    }
  });
}
function parse(message, opts) {
  if (opts === void 0) {
    opts = {};
  }
  opts = __assign({ shouldParseSkeletons: true, requiresOtherClause: true }, opts);
  var result = new Parser(message, opts).parse();
  if (result.err) {
    var error = SyntaxError(ErrorKind[result.err.kind]);
    error.location = result.err.location;
    error.originalMessage = result.err.message;
    throw error;
  }
  if (!(opts === null || opts === void 0 ? void 0 : opts.captureLocation)) {
    pruneLocation(result.val);
  }
  return result.val;
}
function memoize(fn2, options) {
  var cache = options && options.cache ? options.cache : cacheDefault;
  var serializer = options && options.serializer ? options.serializer : serializerDefault;
  var strategy = options && options.strategy ? options.strategy : strategyDefault;
  return strategy(fn2, {
    cache,
    serializer
  });
}
function isPrimitive(value) {
  return value == null || typeof value === "number" || typeof value === "boolean";
}
function monadic(fn2, cache, serializer, arg) {
  var cacheKey = isPrimitive(arg) ? arg : serializer(arg);
  var computedValue = cache.get(cacheKey);
  if (typeof computedValue === "undefined") {
    computedValue = fn2.call(this, arg);
    cache.set(cacheKey, computedValue);
  }
  return computedValue;
}
function variadic(fn2, cache, serializer) {
  var args = Array.prototype.slice.call(arguments, 3);
  var cacheKey = serializer(args);
  var computedValue = cache.get(cacheKey);
  if (typeof computedValue === "undefined") {
    computedValue = fn2.apply(this, args);
    cache.set(cacheKey, computedValue);
  }
  return computedValue;
}
function assemble(fn2, context, strategy, cache, serialize) {
  return strategy.bind(context, fn2, cache, serialize);
}
function strategyDefault(fn2, options) {
  var strategy = fn2.length === 1 ? monadic : variadic;
  return assemble(fn2, this, strategy, options.cache.create(), options.serializer);
}
function strategyVariadic(fn2, options) {
  return assemble(fn2, this, variadic, options.cache.create(), options.serializer);
}
function strategyMonadic(fn2, options) {
  return assemble(fn2, this, monadic, options.cache.create(), options.serializer);
}
var serializerDefault = function() {
  return JSON.stringify(arguments);
};
function ObjectWithoutPrototypeCache() {
  this.cache = /* @__PURE__ */ Object.create(null);
}
ObjectWithoutPrototypeCache.prototype.get = function(key) {
  return this.cache[key];
};
ObjectWithoutPrototypeCache.prototype.set = function(key, value) {
  this.cache[key] = value;
};
var cacheDefault = {
  create: function create() {
    return new ObjectWithoutPrototypeCache();
  }
};
var strategies = {
  variadic: strategyVariadic,
  monadic: strategyMonadic
};
var ErrorCode;
(function(ErrorCode2) {
  ErrorCode2["MISSING_VALUE"] = "MISSING_VALUE";
  ErrorCode2["INVALID_VALUE"] = "INVALID_VALUE";
  ErrorCode2["MISSING_INTL_API"] = "MISSING_INTL_API";
})(ErrorCode || (ErrorCode = {}));
var FormatError = function(_super) {
  __extends(FormatError2, _super);
  function FormatError2(msg, code, originalMessage) {
    var _this = _super.call(this, msg) || this;
    _this.code = code;
    _this.originalMessage = originalMessage;
    return _this;
  }
  FormatError2.prototype.toString = function() {
    return "[formatjs Error: ".concat(this.code, "] ").concat(this.message);
  };
  return FormatError2;
}(Error);
var InvalidValueError = function(_super) {
  __extends(InvalidValueError2, _super);
  function InvalidValueError2(variableId, value, options, originalMessage) {
    return _super.call(this, 'Invalid values for "'.concat(variableId, '": "').concat(value, '". Options are "').concat(Object.keys(options).join('", "'), '"'), ErrorCode.INVALID_VALUE, originalMessage) || this;
  }
  return InvalidValueError2;
}(FormatError);
var InvalidValueTypeError = function(_super) {
  __extends(InvalidValueTypeError2, _super);
  function InvalidValueTypeError2(value, type, originalMessage) {
    return _super.call(this, 'Value for "'.concat(value, '" must be of type ').concat(type), ErrorCode.INVALID_VALUE, originalMessage) || this;
  }
  return InvalidValueTypeError2;
}(FormatError);
var MissingValueError = function(_super) {
  __extends(MissingValueError2, _super);
  function MissingValueError2(variableId, originalMessage) {
    return _super.call(this, 'The intl string context variable "'.concat(variableId, '" was not provided to the string "').concat(originalMessage, '"'), ErrorCode.MISSING_VALUE, originalMessage) || this;
  }
  return MissingValueError2;
}(FormatError);
var PART_TYPE;
(function(PART_TYPE2) {
  PART_TYPE2[PART_TYPE2["literal"] = 0] = "literal";
  PART_TYPE2[PART_TYPE2["object"] = 1] = "object";
})(PART_TYPE || (PART_TYPE = {}));
function mergeLiteral(parts) {
  if (parts.length < 2) {
    return parts;
  }
  return parts.reduce(function(all, part) {
    var lastPart = all[all.length - 1];
    if (!lastPart || lastPart.type !== PART_TYPE.literal || part.type !== PART_TYPE.literal) {
      all.push(part);
    } else {
      lastPart.value += part.value;
    }
    return all;
  }, []);
}
function isFormatXMLElementFn(el) {
  return typeof el === "function";
}
function formatToParts(els, locales, formatters, formats, values, currentPluralValue, originalMessage) {
  if (els.length === 1 && isLiteralElement(els[0])) {
    return [
      {
        type: PART_TYPE.literal,
        value: els[0].value
      }
    ];
  }
  var result = [];
  for (var _i = 0, els_1 = els; _i < els_1.length; _i++) {
    var el = els_1[_i];
    if (isLiteralElement(el)) {
      result.push({
        type: PART_TYPE.literal,
        value: el.value
      });
      continue;
    }
    if (isPoundElement(el)) {
      if (typeof currentPluralValue === "number") {
        result.push({
          type: PART_TYPE.literal,
          value: formatters.getNumberFormat(locales).format(currentPluralValue)
        });
      }
      continue;
    }
    var varName = el.value;
    if (!(values && varName in values)) {
      throw new MissingValueError(varName, originalMessage);
    }
    var value = values[varName];
    if (isArgumentElement(el)) {
      if (!value || typeof value === "string" || typeof value === "number") {
        value = typeof value === "string" || typeof value === "number" ? String(value) : "";
      }
      result.push({
        type: typeof value === "string" ? PART_TYPE.literal : PART_TYPE.object,
        value
      });
      continue;
    }
    if (isDateElement(el)) {
      var style = typeof el.style === "string" ? formats.date[el.style] : isDateTimeSkeleton(el.style) ? el.style.parsedOptions : void 0;
      result.push({
        type: PART_TYPE.literal,
        value: formatters.getDateTimeFormat(locales, style).format(value)
      });
      continue;
    }
    if (isTimeElement(el)) {
      var style = typeof el.style === "string" ? formats.time[el.style] : isDateTimeSkeleton(el.style) ? el.style.parsedOptions : void 0;
      result.push({
        type: PART_TYPE.literal,
        value: formatters.getDateTimeFormat(locales, style).format(value)
      });
      continue;
    }
    if (isNumberElement(el)) {
      var style = typeof el.style === "string" ? formats.number[el.style] : isNumberSkeleton(el.style) ? el.style.parsedOptions : void 0;
      if (style && style.scale) {
        value = value * (style.scale || 1);
      }
      result.push({
        type: PART_TYPE.literal,
        value: formatters.getNumberFormat(locales, style).format(value)
      });
      continue;
    }
    if (isTagElement(el)) {
      var children2 = el.children, value_1 = el.value;
      var formatFn = values[value_1];
      if (!isFormatXMLElementFn(formatFn)) {
        throw new InvalidValueTypeError(value_1, "function", originalMessage);
      }
      var parts = formatToParts(children2, locales, formatters, formats, values, currentPluralValue);
      var chunks = formatFn(parts.map(function(p2) {
        return p2.value;
      }));
      if (!Array.isArray(chunks)) {
        chunks = [chunks];
      }
      result.push.apply(result, chunks.map(function(c2) {
        return {
          type: typeof c2 === "string" ? PART_TYPE.literal : PART_TYPE.object,
          value: c2
        };
      }));
    }
    if (isSelectElement(el)) {
      var opt = el.options[value] || el.options.other;
      if (!opt) {
        throw new InvalidValueError(el.value, value, Object.keys(el.options), originalMessage);
      }
      result.push.apply(result, formatToParts(opt.value, locales, formatters, formats, values));
      continue;
    }
    if (isPluralElement(el)) {
      var opt = el.options["=".concat(value)];
      if (!opt) {
        if (!Intl.PluralRules) {
          throw new FormatError('Intl.PluralRules is not available in this environment.\nTry polyfilling it using "@formatjs/intl-pluralrules"\n', ErrorCode.MISSING_INTL_API, originalMessage);
        }
        var rule = formatters.getPluralRules(locales, { type: el.pluralType }).select(value - (el.offset || 0));
        opt = el.options[rule] || el.options.other;
      }
      if (!opt) {
        throw new InvalidValueError(el.value, value, Object.keys(el.options), originalMessage);
      }
      result.push.apply(result, formatToParts(opt.value, locales, formatters, formats, values, value - (el.offset || 0)));
      continue;
    }
  }
  return mergeLiteral(result);
}
function mergeConfig(c1, c2) {
  if (!c2) {
    return c1;
  }
  return __assign(__assign(__assign({}, c1 || {}), c2 || {}), Object.keys(c1).reduce(function(all, k2) {
    all[k2] = __assign(__assign({}, c1[k2]), c2[k2] || {});
    return all;
  }, {}));
}
function mergeConfigs(defaultConfig, configs) {
  if (!configs) {
    return defaultConfig;
  }
  return Object.keys(defaultConfig).reduce(function(all, k2) {
    all[k2] = mergeConfig(defaultConfig[k2], configs[k2]);
    return all;
  }, __assign({}, defaultConfig));
}
function createFastMemoizeCache(store) {
  return {
    create: function() {
      return {
        get: function(key) {
          return store[key];
        },
        set: function(key, value) {
          store[key] = value;
        }
      };
    }
  };
}
function createDefaultFormatters(cache) {
  if (cache === void 0) {
    cache = {
      number: {},
      dateTime: {},
      pluralRules: {}
    };
  }
  return {
    getNumberFormat: memoize(function() {
      var _a2;
      var args = [];
      for (var _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
      }
      return new ((_a2 = Intl.NumberFormat).bind.apply(_a2, __spreadArray([void 0], args, false)))();
    }, {
      cache: createFastMemoizeCache(cache.number),
      strategy: strategies.variadic
    }),
    getDateTimeFormat: memoize(function() {
      var _a2;
      var args = [];
      for (var _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
      }
      return new ((_a2 = Intl.DateTimeFormat).bind.apply(_a2, __spreadArray([void 0], args, false)))();
    }, {
      cache: createFastMemoizeCache(cache.dateTime),
      strategy: strategies.variadic
    }),
    getPluralRules: memoize(function() {
      var _a2;
      var args = [];
      for (var _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
      }
      return new ((_a2 = Intl.PluralRules).bind.apply(_a2, __spreadArray([void 0], args, false)))();
    }, {
      cache: createFastMemoizeCache(cache.pluralRules),
      strategy: strategies.variadic
    })
  };
}
var IntlMessageFormat = function() {
  function IntlMessageFormat2(message, locales, overrideFormats, opts) {
    var _this = this;
    if (locales === void 0) {
      locales = IntlMessageFormat2.defaultLocale;
    }
    this.formatterCache = {
      number: {},
      dateTime: {},
      pluralRules: {}
    };
    this.format = function(values) {
      var parts = _this.formatToParts(values);
      if (parts.length === 1) {
        return parts[0].value;
      }
      var result = parts.reduce(function(all, part) {
        if (!all.length || part.type !== PART_TYPE.literal || typeof all[all.length - 1] !== "string") {
          all.push(part.value);
        } else {
          all[all.length - 1] += part.value;
        }
        return all;
      }, []);
      if (result.length <= 1) {
        return result[0] || "";
      }
      return result;
    };
    this.formatToParts = function(values) {
      return formatToParts(_this.ast, _this.locales, _this.formatters, _this.formats, values, void 0, _this.message);
    };
    this.resolvedOptions = function() {
      return {
        locale: Intl.NumberFormat.supportedLocalesOf(_this.locales)[0]
      };
    };
    this.getAst = function() {
      return _this.ast;
    };
    if (typeof message === "string") {
      this.message = message;
      if (!IntlMessageFormat2.__parse) {
        throw new TypeError("IntlMessageFormat.__parse must be set to process `message` of type `string`");
      }
      this.ast = IntlMessageFormat2.__parse(message, {
        ignoreTag: opts === null || opts === void 0 ? void 0 : opts.ignoreTag
      });
    } else {
      this.ast = message;
    }
    if (!Array.isArray(this.ast)) {
      throw new TypeError("A message must be provided as a String or AST.");
    }
    this.formats = mergeConfigs(IntlMessageFormat2.formats, overrideFormats);
    this.locales = locales;
    this.formatters = opts && opts.formatters || createDefaultFormatters(this.formatterCache);
  }
  Object.defineProperty(IntlMessageFormat2, "defaultLocale", {
    get: function() {
      if (!IntlMessageFormat2.memoizedDefaultLocale) {
        IntlMessageFormat2.memoizedDefaultLocale = new Intl.NumberFormat().resolvedOptions().locale;
      }
      return IntlMessageFormat2.memoizedDefaultLocale;
    },
    enumerable: false,
    configurable: true
  });
  IntlMessageFormat2.memoizedDefaultLocale = null;
  IntlMessageFormat2.__parse = parse;
  IntlMessageFormat2.formats = {
    number: {
      integer: {
        maximumFractionDigits: 0
      },
      currency: {
        style: "currency"
      },
      percent: {
        style: "percent"
      }
    },
    date: {
      short: {
        month: "numeric",
        day: "numeric",
        year: "2-digit"
      },
      medium: {
        month: "short",
        day: "numeric",
        year: "numeric"
      },
      long: {
        month: "long",
        day: "numeric",
        year: "numeric"
      },
      full: {
        weekday: "long",
        month: "long",
        day: "numeric",
        year: "numeric"
      }
    },
    time: {
      short: {
        hour: "numeric",
        minute: "numeric"
      },
      medium: {
        hour: "numeric",
        minute: "numeric",
        second: "numeric"
      },
      long: {
        hour: "numeric",
        minute: "numeric",
        second: "numeric",
        timeZoneName: "short"
      },
      full: {
        hour: "numeric",
        minute: "numeric",
        second: "numeric",
        timeZoneName: "short"
      }
    }
  };
  return IntlMessageFormat2;
}();
var o = IntlMessageFormat;
const r = {}, i = (e, n, t) => t ? (n in r || (r[n] = {}), e in r[n] || (r[n][e] = t), t) : t, l = (e, n) => {
  if (n == null)
    return;
  if (n in r && e in r[n])
    return r[n][e];
  const t = E(n);
  for (let o2 = 0; o2 < t.length; o2++) {
    const r2 = c(t[o2], e);
    if (r2)
      return i(e, n, r2);
  }
};
let a;
const s = writable({});
function u(e) {
  return e in a;
}
function c(e, n) {
  if (!u(e))
    return null;
  return function(e2, n2) {
    if (n2 == null)
      return;
    if (n2 in e2)
      return e2[n2];
    const t = n2.split(".");
    let o2 = e2;
    for (let e3 = 0; e3 < t.length; e3++)
      if (typeof o2 == "object") {
        if (e3 > 0) {
          const n3 = t.slice(e3, t.length).join(".");
          if (n3 in o2) {
            o2 = o2[n3];
            break;
          }
        }
        o2 = o2[t[e3]];
      } else
        o2 = void 0;
    return o2;
  }(function(e2) {
    return a[e2] || null;
  }(e), n);
}
function m(e, ...n) {
  delete r[e], s.update((o2) => (o2[e] = cjs.all([o2[e] || {}, ...n]), o2));
}
derived([s], ([e]) => Object.keys(e));
s.subscribe((e) => a = e);
const d = {};
function g(e) {
  return d[e];
}
function w(e) {
  return e != null && E(e).some((e2) => {
    var n;
    return (n = g(e2)) === null || n === void 0 ? void 0 : n.size;
  });
}
function h(e, n) {
  return Promise.all(n.map((n2) => (function(e2, n3) {
    d[e2].delete(n3), d[e2].size === 0 && delete d[e2];
  }(e, n2), n2().then((e2) => e2.default || e2)))).then((n2) => m(e, ...n2));
}
const p = {};
function b(e) {
  if (!w(e))
    return e in p ? p[e] : Promise.resolve();
  const n = function(e2) {
    return E(e2).map((e3) => {
      const n2 = g(e3);
      return [e3, n2 ? [...n2] : []];
    }).filter(([, e3]) => e3.length > 0);
  }(e);
  return p[e] = Promise.all(n.map(([e2, n2]) => h(e2, n2))).then(() => {
    if (w(e))
      return b(e);
    delete p[e];
  }), p[e];
}
/*! *****************************************************************************
Copyright (c) Microsoft Corporation.

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.
***************************************************************************** */
function v(e, n) {
  var t = {};
  for (var o2 in e)
    Object.prototype.hasOwnProperty.call(e, o2) && n.indexOf(o2) < 0 && (t[o2] = e[o2]);
  if (e != null && typeof Object.getOwnPropertySymbols == "function") {
    var r2 = 0;
    for (o2 = Object.getOwnPropertySymbols(e); r2 < o2.length; r2++)
      n.indexOf(o2[r2]) < 0 && Object.prototype.propertyIsEnumerable.call(e, o2[r2]) && (t[o2[r2]] = e[o2[r2]]);
  }
  return t;
}
const O = { fallbackLocale: null, loadingDelay: 200, formats: { number: { scientific: { notation: "scientific" }, engineering: { notation: "engineering" }, compactLong: { notation: "compact", compactDisplay: "long" }, compactShort: { notation: "compact", compactDisplay: "short" } }, date: { short: { month: "numeric", day: "numeric", year: "2-digit" }, medium: { month: "short", day: "numeric", year: "numeric" }, long: { month: "long", day: "numeric", year: "numeric" }, full: { weekday: "long", month: "long", day: "numeric", year: "numeric" } }, time: { short: { hour: "numeric", minute: "numeric" }, medium: { hour: "numeric", minute: "numeric", second: "numeric" }, long: { hour: "numeric", minute: "numeric", second: "numeric", timeZoneName: "short" }, full: { hour: "numeric", minute: "numeric", second: "numeric", timeZoneName: "short" } } }, warnOnMissingMessages: true, ignoreTag: true };
function j() {
  return O;
}
function $(e) {
  const { formats: n } = e, t = v(e, ["formats"]), o2 = e.initialLocale || e.fallbackLocale;
  return Object.assign(O, t, { initialLocale: o2 }), n && ("number" in n && Object.assign(O.formats.number, n.number), "date" in n && Object.assign(O.formats.date, n.date), "time" in n && Object.assign(O.formats.time, n.time)), M.set(o2);
}
const k = writable(false);
let L;
const T = writable(null);
function x$1(e) {
  return e.split("-").map((e2, n, t) => t.slice(0, n + 1).join("-")).reverse();
}
function E(e, n = j().fallbackLocale) {
  const t = x$1(e);
  return n ? [.../* @__PURE__ */ new Set([...t, ...x$1(n)])] : t;
}
function D() {
  return L != null ? L : void 0;
}
T.subscribe((e) => {
  L = e != null ? e : void 0, typeof window != "undefined" && e != null && document.documentElement.setAttribute("lang", e);
});
const M = Object.assign(Object.assign({}, T), { set: (e) => {
  if (e && function(e2) {
    if (e2 == null)
      return;
    const n = E(e2);
    for (let e3 = 0; e3 < n.length; e3++) {
      const t = n[e3];
      if (u(t))
        return t;
    }
  }(e) && w(e)) {
    const { loadingDelay: n } = j();
    let t;
    return typeof window != "undefined" && D() != null && n ? t = window.setTimeout(() => k.set(true), n) : k.set(true), b(e).then(() => {
      T.set(e);
    }).finally(() => {
      clearTimeout(t), k.set(false);
    });
  }
  return T.set(e);
} }), I = () => typeof window == "undefined" ? null : window.navigator.language || window.navigator.languages[0], Z = (e) => {
  const n = /* @__PURE__ */ Object.create(null);
  return (t) => {
    const o2 = JSON.stringify(t);
    return o2 in n ? n[o2] : n[o2] = e(t);
  };
}, C = (e, n) => {
  const { formats: t } = j();
  if (e in t && n in t[e])
    return t[e][n];
  throw new Error(`[svelte-i18n] Unknown "${n}" ${e} format.`);
}, G = Z((e) => {
  var { locale: n, format: t } = e, o2 = v(e, ["locale", "format"]);
  if (n == null)
    throw new Error('[svelte-i18n] A "locale" must be set to format numbers');
  return t && (o2 = C("number", t)), new Intl.NumberFormat(n, o2);
}), J = Z((e) => {
  var { locale: n, format: t } = e, o2 = v(e, ["locale", "format"]);
  if (n == null)
    throw new Error('[svelte-i18n] A "locale" must be set to format dates');
  return t ? o2 = C("date", t) : Object.keys(o2).length === 0 && (o2 = C("date", "short")), new Intl.DateTimeFormat(n, o2);
}), U = Z((e) => {
  var { locale: n, format: t } = e, o2 = v(e, ["locale", "format"]);
  if (n == null)
    throw new Error('[svelte-i18n] A "locale" must be set to format time values');
  return t ? o2 = C("time", t) : Object.keys(o2).length === 0 && (o2 = C("time", "short")), new Intl.DateTimeFormat(n, o2);
}), _ = (e = {}) => {
  var { locale: n = D() } = e, t = v(e, ["locale"]);
  return G(Object.assign({ locale: n }, t));
}, q = (e = {}) => {
  var { locale: n = D() } = e, t = v(e, ["locale"]);
  return J(Object.assign({ locale: n }, t));
}, B = (e = {}) => {
  var { locale: n = D() } = e, t = v(e, ["locale"]);
  return U(Object.assign({ locale: n }, t));
}, H = Z((e, n = D()) => new o(e, n, j().formats, { ignoreTag: j().ignoreTag })), K = (e, n = {}) => {
  let t = n;
  typeof e == "object" && (t = e, e = t.id);
  const { values: o2, locale: r2 = D(), default: i2 } = t;
  if (r2 == null)
    throw new Error("[svelte-i18n] Cannot format a message without first setting the initial locale.");
  let a2 = l(e, r2);
  if (a2) {
    if (typeof a2 != "string")
      return console.warn(`[svelte-i18n] Message with id "${e}" must be of type "string", found: "${typeof a2}". Gettin its value through the "$format" method is deprecated; use the "json" method instead.`), a2;
  } else
    j().warnOnMissingMessages && console.warn(`[svelte-i18n] The message "${e}" was not found in "${E(r2).join('", "')}".${w(D()) ? "\n\nNote: there are at least one loader still registered to this locale that wasn't executed." : ""}`), a2 = i2 != null ? i2 : e;
  if (!o2)
    return a2;
  let s2 = a2;
  try {
    s2 = H(a2, r2).format(o2);
  } catch (n2) {
    console.warn(`[svelte-i18n] Message "${e}" has syntax error:`, n2.message);
  }
  return s2;
}, Q = (e, n) => B(n).format(e), R = (e, n) => q(n).format(e), V = (e, n) => _(n).format(e), W = (e, n = D()) => l(e, n), X = derived([M, s], () => K);
derived([M], () => Q);
derived([M], () => R);
derived([M], () => V);
derived([M, s], () => W);
const component_map = {
  accordion: () => import("./index.js"),
  audio: () => import("./index2.js"),
  box: () => import("./index3.js"),
  button: () => import("./index4.js"),
  carousel: () => import("./index5.js"),
  carouselitem: () => import("./index6.js"),
  chatbot: () => import("./index7.js"),
  checkbox: () => import("./index8.js"),
  checkboxgroup: () => import("./index9.js"),
  colorpicker: () => import("./index10.js"),
  column: () => import("./index11.js"),
  dataframe: () => import("./index12.js"),
  dataset: () => import("./index13.js"),
  dropdown: () => import("./index14.js"),
  file: () => import("./index15.js"),
  form: () => import("./index16.js"),
  gallery: () => import("./index17.js"),
  group: () => import("./index18.js"),
  highlightedtext: () => import("./index19.js"),
  html: () => import("./index20.js"),
  image: () => import("./index21.js"),
  interpretation: () => import("./index22.js"),
  json: () => import("./index23.js"),
  label: () => import("./index24.js"),
  markdown: () => import("./index25.js"),
  model3d: () => import("./index26.js"),
  number: () => import("./index27.js"),
  plot: () => import("./index28.js"),
  radio: () => import("./index29.js"),
  row: () => import("./index30.js"),
  slider: () => import("./index31.js"),
  state: () => import("./index32.js"),
  statustracker: () => import("./index33.js"),
  tabs: () => import("./index34.js"),
  tabitem: () => import("./index35.js"),
  textbox: () => import("./index36.js"),
  timeseries: () => import("./index37.js"),
  uploadbutton: () => import("./index38.js"),
  video: () => import("./index39.js")
};
function create_loading_status_store() {
  const store = writable({});
  const fn_inputs = [];
  const fn_outputs = [];
  const pending_outputs = /* @__PURE__ */ new Map();
  const pending_inputs = /* @__PURE__ */ new Map();
  const inputs_to_update = /* @__PURE__ */ new Map();
  const fn_status = [];
  function update2(fn_index, status, queue, size, position, eta, message) {
    const outputs = fn_outputs[fn_index];
    const inputs = fn_inputs[fn_index];
    const last_status = fn_status[fn_index];
    const outputs_to_update = outputs.map((id2) => {
      let new_status;
      const pending_count = pending_outputs.get(id2) || 0;
      if (last_status === "pending" && status !== "pending") {
        let new_count = pending_count - 1;
        pending_outputs.set(id2, new_count < 0 ? 0 : new_count);
        new_status = new_count > 0 ? "pending" : status;
      } else if (last_status === "pending" && status === "pending") {
        new_status = "pending";
      } else if (last_status !== "pending" && status === "pending") {
        new_status = "pending";
        pending_outputs.set(id2, pending_count + 1);
      } else {
        new_status = status;
      }
      return {
        id: id2,
        queue_position: position,
        queue_size: size,
        eta,
        status: new_status,
        message
      };
    });
    inputs.map((id2) => {
      const pending_count = pending_inputs.get(id2) || 0;
      if (last_status === "pending" && status !== "pending") {
        let new_count = pending_count - 1;
        pending_inputs.set(id2, new_count < 0 ? 0 : new_count);
        inputs_to_update.set(id2, status);
      } else if (last_status !== "pending" && status === "pending") {
        pending_inputs.set(id2, pending_count + 1);
        inputs_to_update.set(id2, status);
      } else {
        inputs_to_update.delete(id2);
      }
    });
    store.update((outputs2) => {
      outputs_to_update.forEach(({ id: id2, queue_position, queue_size, eta: eta2, status: status2, message: message2 }) => {
        outputs2[id2] = {
          queue,
          queue_size,
          queue_position,
          eta: eta2,
          message: message2,
          status: status2,
          fn_index
        };
      });
      return outputs2;
    });
    fn_status[fn_index] = status;
  }
  function register(index, inputs, outputs) {
    fn_inputs[index] = inputs;
    fn_outputs[index] = outputs;
  }
  return {
    update: update2,
    register,
    subscribe: store.subscribe,
    get_status_for_fn(i2) {
      return fn_status[i2];
    },
    get_inputs_to_update() {
      return inputs_to_update;
    }
  };
}
const app_state = writable({ autoscroll: false });
const Submit$l = "\u0623\u0631\u0633\u0644";
const Clear$l = "\u0623\u0645\u0633\u062D";
const Interpret$l = "\u0641\u0633\u0650\u0651\u0631";
const Flag$l = "\u0628\u0644\u0650\u0651\u063A";
const Examples$l = "\u0623\u0645\u062B\u0644\u0629";
const or$l = "\u0623\u0648";
var ar = {
  "interface": {
    drop_image: "\u0623\u0633\u0642\u0637 \u0627\u0644\u0635\u0648\u0631\u0629 \u0647\u0646\u0627",
    drop_video: "\u0623\u0633\u0642\u0637 \u0627\u0644\u0641\u064A\u062F\u064A\u0648 \u0647\u0646\u0627",
    drop_audio: "\u0623\u0633\u0642\u0637 \u0627\u0644\u0645\u0644\u0641 \u0627\u0644\u0635\u0648\u062A\u064A \u0647\u0646\u0627",
    drop_file: "\u0623\u0633\u0642\u0637 \u0627\u0644\u0645\u0644\u0641 \u0647\u0646\u0627",
    drop_csv: "\u0623\u0633\u0642\u0637 \u0645\u0644\u0641 \u0627\u0644\u0628\u064A\u0627\u0646\u0627\u062A \u0647\u0646\u0627",
    click_to_upload: "\u0625\u0636\u063A\u0637 \u0644\u0644\u062A\u062D\u0645\u064A\u0644",
    view_api: "\u0625\u0633\u062A\u062E\u062F\u0645 \u0648\u0627\u062C\u0647\u0629 \u0627\u0644\u0628\u0631\u0645\u062C\u0629",
    built_with_Gradio: "\u062A\u0645 \u0627\u0644\u0625\u0646\u0634\u0627\u0621 \u0628\u0625\u0633\u062A\u062E\u062F\u0627\u0645 Gradio"
  },
  Submit: Submit$l,
  Clear: Clear$l,
  Interpret: Interpret$l,
  Flag: Flag$l,
  Examples: Examples$l,
  or: or$l
};
var __glob_1_0 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$l,
  Clear: Clear$l,
  Interpret: Interpret$l,
  Flag: Flag$l,
  Examples: Examples$l,
  or: or$l,
  "default": ar
});
const Submit$k = "Absenden";
const Clear$k = "L\xF6schen";
const Interpret$k = "Ersteller";
const Flag$k = "Flag";
const Examples$k = "Beispiele";
const or$k = "oder";
var de = {
  "interface": {
    drop_image: "Bild hier ablegen",
    drop_video: "Video hier ablegen",
    drop_audio: "Audio hier ablegen",
    drop_file: "Datei hier ablegen",
    drop_csv: "CSV Datei hier ablegen",
    click_to_upload: "Hochladen",
    view_api: "API anschauen",
    built_with_Gradio: "Mit Gradio erstellt"
  },
  Submit: Submit$k,
  Clear: Clear$k,
  Interpret: Interpret$k,
  Flag: Flag$k,
  Examples: Examples$k,
  or: or$k
};
var __glob_1_1 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$k,
  Clear: Clear$k,
  Interpret: Interpret$k,
  Flag: Flag$k,
  Examples: Examples$k,
  or: or$k,
  "default": de
});
const Submit$j = "Submit";
const Clear$j = "Clear";
const Interpret$j = "Interpret";
const Flag$j = "Flag";
const Examples$j = "Examples";
const or$j = "or";
var en = {
  "interface": {
    drop_image: "Drop Image Here",
    drop_video: "Drop Video Here",
    drop_audio: "Drop Audio Here",
    drop_file: "Drop File Here",
    drop_csv: "Drop CSV Here",
    click_to_upload: "Click to Upload",
    view_api: "view the api",
    built_with_Gradio: "Built with gradio",
    copy_to_clipboard: "copy to clipboard",
    loading: "Loading",
    error: "ERROR",
    empty: "Empty"
  },
  Submit: Submit$j,
  Clear: Clear$j,
  Interpret: Interpret$j,
  Flag: Flag$j,
  Examples: Examples$j,
  or: or$j
};
var __glob_1_2 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$j,
  Clear: Clear$j,
  Interpret: Interpret$j,
  Flag: Flag$j,
  Examples: Examples$j,
  or: or$j,
  "default": en
});
const Submit$i = "Enviar";
const Clear$i = "Limpiar";
const Interpret$i = "Interpretar";
const Flag$i = "Avisar";
const Examples$i = "Ejemplos";
const or$i = "o";
var es = {
  "interface": {
    drop_image: "Coloque la imagen aqu\xED",
    drop_video: "Coloque el video aqu\xED",
    drop_audio: "Coloque el audio aqu\xED",
    drop_file: "Coloque el archivo aqu\xED",
    drop_csv: "Coloque el CSV aqu\xED",
    click_to_upload: "Haga click para cargar",
    view_api: "Ver la API",
    built_with_Gradio: "Construido con Gradio"
  },
  Submit: Submit$i,
  Clear: Clear$i,
  Interpret: Interpret$i,
  Flag: Flag$i,
  Examples: Examples$i,
  or: or$i
};
var __glob_1_3 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$i,
  Clear: Clear$i,
  Interpret: Interpret$i,
  Flag: Flag$i,
  Examples: Examples$i,
  or: or$i,
  "default": es
});
const Submit$h = "\u0627\u0631\u0633\u0627\u0644";
const Clear$h = "\u062D\u0630\u0641";
const Interpret$h = "\u062A\u0641\u0633\u06CC\u0631";
const Flag$h = "\u067E\u0631\u0686\u0645";
const Examples$h = "\u0645\u062B\u0627\u0644 \u0647\u0627";
const or$h = "\u06CC\u0627";
var fa = {
  "interface": {
    drop_image: "\u062A\u0635\u0648\u06CC\u0631 \u0631\u0627 \u0627\u06CC\u0646\u062C\u0627 \u0631\u0647\u0627 \u06A9\u0646\u06CC\u062F",
    drop_video: "\u0648\u06CC\u062F\u06CC\u0648 \u0631\u0627 \u0627\u06CC\u0646\u062C\u0627 \u0631\u0647\u0627 \u06A9\u0646\u06CC\u062F",
    drop_audio: "\u0635\u0648\u062A \u0631\u0627 \u0627\u06CC\u0646\u062C\u0627 \u0631\u0647\u0627 \u06A9\u0646\u06CC\u062F",
    drop_file: "\u0641\u0627\u06CC\u0644 \u0631\u0627 \u0627\u06CC\u0646\u062C\u0627 \u0631\u0647\u0627 \u06A9\u0646\u06CC\u062F",
    drop_csv: "\u0641\u0627\u06CC\u0644 csv \u0631\u0627  \u0627\u06CC\u0646\u062C\u0627 \u0631\u0647\u0627 \u06A9\u0646\u06CC\u062F",
    click_to_upload: "\u0628\u0631\u0627\u06CC \u0622\u067E\u0644\u0648\u062F \u06A9\u0644\u06CC\u06A9 \u06A9\u0646\u06CC\u062F",
    view_api: "api \u0631\u0627 \u0645\u0634\u0627\u0647\u062F\u0647 \u06A9\u0646\u06CC\u062F",
    built_with_Gradio: "\u0633\u0627\u062E\u062A\u0647 \u0634\u062F\u0647 \u0628\u0627 gradio"
  },
  Submit: Submit$h,
  Clear: Clear$h,
  Interpret: Interpret$h,
  Flag: Flag$h,
  Examples: Examples$h,
  or: or$h
};
var __glob_1_4 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$h,
  Clear: Clear$h,
  Interpret: Interpret$h,
  Flag: Flag$h,
  Examples: Examples$h,
  or: or$h,
  "default": fa
});
const Submit$g = "Soumettre";
const Clear$g = "Nettoyer";
const Interpret$g = "Interpr\xE9ter";
const Flag$g = "Signaler";
const Examples$g = "Exemples";
const or$g = "ou";
var fr = {
  "interface": {
    drop_image: "D\xE9poser l'Image Ici",
    drop_video: "D\xE9poser la Vid\xE9o Ici",
    drop_audio: "D\xE9poser l'Audio Ici",
    drop_file: "D\xE9poser le Fichier Ici",
    drop_csv: "D\xE9poser le CSV Ici",
    click_to_upload: "Cliquer pour T\xE9l\xE9charger",
    view_api: "Voir l'API",
    built_with_Gradio: "Con\xE7u avec Gradio"
  },
  Submit: Submit$g,
  Clear: Clear$g,
  Interpret: Interpret$g,
  Flag: Flag$g,
  Examples: Examples$g,
  or: or$g
};
var __glob_1_5 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$g,
  Clear: Clear$g,
  Interpret: Interpret$g,
  Flag: Flag$g,
  Examples: Examples$g,
  or: or$g,
  "default": fr
});
const Submit$f = "\u05E9\u05DC\u05D7";
const Clear$f = "\u05E0\u05E7\u05D4";
const Interpret$f = "\u05DC\u05E4\u05E8\u05E9";
const Flag$f = "\u05E1\u05DE\u05DF";
const Examples$f = "\u05D3\u05D5\u05D2\u05DE\u05D5\u05EA";
const or$f = "\u05D0\u05D5";
var he = {
  "interface": {
    drop_image: "\u05D2\u05E8\u05D5\u05E8 \u05E7\u05D5\u05D1\u05E5 \u05EA\u05DE\u05D5\u05E0\u05D4 \u05DC\u05DB\u05D0\u05DF",
    drop_video: "\u05D2\u05E8\u05D5\u05E8 \u05E7\u05D5\u05D1\u05E5 \u05E1\u05E8\u05D8\u05D5\u05DF \u05DC\u05DB\u05D0\u05DF",
    drop_audio: "\u05D2\u05E8\u05D5\u05E8 \u05DC\u05DB\u05D0\u05DF \u05E7\u05D5\u05D1\u05E5 \u05E9\u05DE\u05E2",
    drop_file: "\u05D2\u05E8\u05D5\u05E8 \u05E7\u05D5\u05D1\u05E5 \u05DC\u05DB\u05D0\u05DF",
    drop_csv: "\u05D2\u05E8\u05D5\u05E8 csv \u05E7\u05D5\u05D1\u05E5 \u05DC\u05DB\u05D0\u05DF",
    click_to_upload: "\u05DC\u05D7\u05E5 \u05DB\u05D3\u05D9 \u05DC\u05D4\u05E2\u05DC\u05D5\u05EA",
    view_api: "\u05E6\u05E4\u05D4 \u05D1 API",
    built_with_Gradio: "\u05D1\u05E0\u05D5\u05D9 \u05E2\u05DD \u05D2\u05E8\u05D3\u05D9\u05D5"
  },
  Submit: Submit$f,
  Clear: Clear$f,
  Interpret: Interpret$f,
  Flag: Flag$f,
  Examples: Examples$f,
  or: or$f
};
var __glob_1_6 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$f,
  Clear: Clear$f,
  Interpret: Interpret$f,
  Flag: Flag$f,
  Examples: Examples$f,
  or: or$f,
  "default": he
});
const Submit$e = "\u0938\u092C\u092E\u093F\u091F \u0915\u0930\u0947";
const Clear$e = "\u0939\u091F\u093E\u092F\u0947";
const Interpret$e = "\u0935\u094D\u092F\u093E\u0916\u094D\u092F\u093E \u0915\u0930\u0947";
const Flag$e = "\u091A\u093F\u0939\u094D\u0928\u093F\u0924 \u0915\u0930\u0947";
const Examples$e = "\u0909\u0926\u093E\u0939\u0930\u0923";
const or$e = "\u092F\u093E";
var hi = {
  "interface": {
    drop_image: "\u092F\u0939\u093E\u0901 \u0907\u092E\u0947\u091C \u0921\u094D\u0930\u0949\u092A \u0915\u0930\u0947\u0902",
    drop_video: "\u092F\u0939\u093E\u0901 \u0935\u0940\u0921\u093F\u092F\u094B \u0921\u094D\u0930\u0949\u092A \u0915\u0930\u0947\u0902",
    drop_audio: "\u092F\u0939\u093E\u0901 \u0911\u0921\u093F\u092F\u094B \u0921\u094D\u0930\u0949\u092A \u0915\u0930\u0947\u0902",
    drop_file: "\u092F\u0939\u093E\u0901 File \u0921\u094D\u0930\u0949\u092A \u0915\u0930\u0947\u0902",
    drop_csv: "\u092F\u0939\u093E\u0901 CSV \u0921\u094D\u0930\u0949\u092A \u0915\u0930\u0947\u0902",
    click_to_upload: "\u0905\u092A\u0932\u094B\u0921 \u0915\u0947 \u0932\u093F\u090F \u092C\u091F\u0928 \u0926\u092C\u093E\u092F\u0947\u0902",
    view_api: "API \u0915\u094B \u0926\u0947\u0916\u0947",
    built_with_Gradio: "Gradio \u0938\u0947 \u092C\u0928\u093E"
  },
  Submit: Submit$e,
  Clear: Clear$e,
  Interpret: Interpret$e,
  Flag: Flag$e,
  Examples: Examples$e,
  or: or$e
};
var __glob_1_7 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$e,
  Clear: Clear$e,
  Interpret: Interpret$e,
  Flag: Flag$e,
  Examples: Examples$e,
  or: or$e,
  "default": hi
});
const Submit$d = "\u9001\u4FE1";
const Clear$d = "\u30AF\u30EA\u30A2";
const Interpret$d = "\u89E3\u91C8";
const Flag$d = "\u30D5\u30E9\u30B0\u3059\u308B";
const Examples$d = "\u5165\u529B\u4F8B";
const or$d = "\u307E\u305F\u306F";
var ja = {
  "interface": {
    drop_image: "\u3053\u3053\u306B\u753B\u50CF\u3092\u30C9\u30ED\u30C3\u30D7",
    drop_video: "\u3053\u3053\u306B\u52D5\u753B\u3092\u30C9\u30ED\u30C3\u30D7",
    drop_audio: "\u3053\u3053\u306B\u97F3\u58F0\u3092\u30C9\u30ED\u30C3\u30D7",
    drop_file: "\u3053\u3053\u306B\u30D5\u30A1\u30A4\u30EB\u3092\u30C9\u30ED\u30C3\u30D7",
    drop_csv: "\u3053\u3053\u306BCSV\u3092\u30C9\u30ED\u30C3\u30D7",
    click_to_upload: "\u30AF\u30EA\u30C3\u30AF\u3057\u3066\u30A2\u30C3\u30D7\u30ED\u30FC\u30C9",
    view_api: "API\u3092\u898B\u308B",
    built_with_Gradio: "gradio\u3067\u4F5C\u308D\u3046"
  },
  Submit: Submit$d,
  Clear: Clear$d,
  Interpret: Interpret$d,
  Flag: Flag$d,
  Examples: Examples$d,
  or: or$d
};
var __glob_1_8 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$d,
  Clear: Clear$d,
  Interpret: Interpret$d,
  Flag: Flag$d,
  Examples: Examples$d,
  or: or$d,
  "default": ja
});
const Submit$c = "\uC81C\uCD9C\uD558\uAE30";
const Clear$c = "\uD074\uB9AC\uC5B4";
const Interpret$c = "\uC124\uBA85\uD558\uAE30";
const Flag$c = "\uD50C\uB798\uADF8";
const Examples$c = "\uC608\uC2DC";
const or$c = "\uB610\uB294";
var ko = {
  "interface": {
    drop_image: "\uC774\uBBF8\uC9C0\uB97C \uB04C\uC5B4 \uB193\uC73C\uC138\uC694",
    drop_video: "\uBE44\uB514\uC624\uB97C \uB04C\uC5B4 \uB193\uC73C\uC138\uC694",
    drop_audio: "\uC624\uB514\uC624\uB97C \uB04C\uC5B4 \uB193\uC73C\uC138\uC694",
    drop_file: "\uD30C\uC77C\uC744 \uB04C\uC5B4 \uB193\uC73C\uC138\uC694",
    drop_csv: "CSV\uD30C\uC77C\uC744 \uB04C\uC5B4 \uB193\uC73C\uC138\uC694",
    click_to_upload: "\uD074\uB9AD\uD574\uC11C \uC5C5\uB85C\uB4DC\uD558\uAE30",
    view_api: "API \uBCF4\uAE30",
    built_with_Gradio: "gradio\uB85C \uC81C\uC791\uB418\uC5C8\uC2B5\uB2C8\uB2E4"
  },
  Submit: Submit$c,
  Clear: Clear$c,
  Interpret: Interpret$c,
  Flag: Flag$c,
  Examples: Examples$c,
  or: or$c
};
var __glob_1_9 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$c,
  Clear: Clear$c,
  Interpret: Interpret$c,
  Flag: Flag$c,
  Examples: Examples$c,
  or: or$c,
  "default": ko
});
const Submit$b = "Pateikti";
const Clear$b = "Trinti";
const Interpret$b = "Interpretuoti";
const Flag$b = "Pa\u017Eym\u0117ti";
const Examples$b = "Pavyzd\u017Eiai";
const or$b = "arba";
var lt = {
  "interface": {
    drop_image: "\u012Ekelkite paveiksl\u0117l\u012F \u010Dia",
    drop_video: "\u012Ekelkite vaizdo \u012Fra\u0161\u0105 \u010Dia",
    drop_audio: "\u012Ekelkite garso \u012Fra\u0161\u0105 \u010Dia",
    drop_file: "\u012Ekelkite byl\u0105 \u010Dia",
    drop_csv: "\u012Ekelkite CSV \u010Dia",
    click_to_upload: "Spustel\u0117kite nor\u0117dami \u012Fkelti",
    view_api: "per\u017Ei\u016Br\u0117ti api",
    built_with_Gradio: "sukurta su gradio"
  },
  Submit: Submit$b,
  Clear: Clear$b,
  Interpret: Interpret$b,
  Flag: Flag$b,
  Examples: Examples$b,
  or: or$b
};
var __glob_1_10 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$b,
  Clear: Clear$b,
  Interpret: Interpret$b,
  Flag: Flag$b,
  Examples: Examples$b,
  or: or$b,
  "default": lt
});
const Submit$a = "Zend in";
const Clear$a = "Wis";
const Interpret$a = "Interpreteer";
const Flag$a = "Vlag";
const Examples$a = "Voorbeelden";
const or$a = "of";
var nl = {
  "interface": {
    drop_image: "Sleep een Afbeelding hier",
    drop_video: "Sleep een Video hier",
    drop_audio: "Sleep een Geluidsbestand hier",
    drop_file: "Sleep een Document hier",
    drop_csv: "Sleep een CSV hier",
    click_to_upload: "Klik om the Uploaden",
    view_api: "zie de api",
    built_with_Gradio: "gemaakt met gradio"
  },
  Submit: Submit$a,
  Clear: Clear$a,
  Interpret: Interpret$a,
  Flag: Flag$a,
  Examples: Examples$a,
  or: or$a
};
var __glob_1_11 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$a,
  Clear: Clear$a,
  Interpret: Interpret$a,
  Flag: Flag$a,
  Examples: Examples$a,
  or: or$a,
  "default": nl
});
const Submit$9 = "Zatwierd\u017A";
const Clear$9 = "Wyczy\u015B\u0107";
const Interpret$9 = "Interpretuj";
const Flag$9 = "Oznacz";
const Examples$9 = "Przyk\u0142ady";
const or$9 = "lub";
var pl = {
  "interface": {
    drop_image: "Przeci\u0105gnij tutaj zdj\u0119cie",
    drop_video: "Przeci\u0105gnij tutaj video",
    drop_audio: "Przeci\u0105gnij tutaj audio",
    drop_file: "Przeci\u0105gnij tutaj plik",
    drop_csv: "Przeci\u0105gnij tutaj CSV",
    click_to_upload: "Kliknij, aby przes\u0142a\u0107",
    view_api: "zobacz api",
    built_with_Gradio: "utworzone z gradio"
  },
  Submit: Submit$9,
  Clear: Clear$9,
  Interpret: Interpret$9,
  Flag: Flag$9,
  Examples: Examples$9,
  or: or$9
};
var __glob_1_12 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$9,
  Clear: Clear$9,
  Interpret: Interpret$9,
  Flag: Flag$9,
  Examples: Examples$9,
  or: or$9,
  "default": pl
});
const Submit$8 = "Enviar";
const Clear$8 = "Limpar";
const Interpret$8 = "Interpretar";
const Flag$8 = "Marcar";
const Examples$8 = "Exemplos";
const or$8 = "ou";
var ptBR = {
  "interface": {
    drop_image: "Solte a Imagem Aqui",
    drop_video: "Solte o V\xEDdeo Aqui",
    drop_audio: "Solte o \xC1udio Aqui",
    drop_file: "Solte o Arquivo Aqui",
    drop_csv: "Solte o CSV Aqui",
    click_to_upload: "Clique para o Upload",
    view_api: "Veja a API",
    built_with_Gradio: "Constru\xEDdo com gradio",
    copy_to_clipboard: "copiar para o clipboard",
    loading: "Carregando",
    error: "ERRO",
    empty: "Vazio"
  },
  Submit: Submit$8,
  Clear: Clear$8,
  Interpret: Interpret$8,
  Flag: Flag$8,
  Examples: Examples$8,
  or: or$8
};
var __glob_1_13 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$8,
  Clear: Clear$8,
  Interpret: Interpret$8,
  Flag: Flag$8,
  Examples: Examples$8,
  or: or$8,
  "default": ptBR
});
const Submit$7 = "\u0418\u0441\u043F\u043E\u043B\u043D\u0438\u0442\u044C";
const Clear$7 = "\u041E\u0447\u0438\u0441\u0442\u0438\u0442\u044C";
const Interpret$7 = "\u0418\u043D\u0442\u0435\u0440\u043F\u0440\u0435\u0442\u0438\u0440\u043E\u0432\u0430\u0442\u044C";
const Flag$7 = "\u041F\u043E\u043C\u0435\u0442\u0438\u0442\u044C";
const Examples$7 = "\u041F\u0440\u0438\u043C\u0435\u0440\u044B";
const or$7 = "\u0438\u043B\u0438";
var ru = {
  "interface": {
    drop_image: "\u041F\u043E\u043C\u0435\u0441\u0442\u0438\u0442\u0435 \u0418\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u0417\u0434\u0435\u0441\u044C",
    drop_video: "\u041F\u043E\u043C\u0435\u0441\u0442\u0438\u0442\u0435 \u0412\u0438\u0434\u0435\u043E \u0417\u0434\u0435\u0441\u044C",
    drop_audio: "\u041F\u043E\u043C\u0435\u0441\u0442\u0438\u0442\u0435 \u0410\u0443\u0434\u0438\u043E \u0417\u0434\u0435\u0441\u044C",
    drop_file: "\u041F\u043E\u043C\u0435\u0441\u0442\u0438\u0442\u0435 \u0414\u043E\u043A\u0443\u043C\u0435\u043D\u0442 \u0417\u0434\u0435\u0441\u044C",
    drop_csv: "\u041F\u043E\u043C\u0435\u0441\u0442\u0438\u0442\u0435 CSV \u0417\u0434\u0435\u0441\u044C",
    click_to_upload: "\u041D\u0430\u0436\u043C\u0438\u0442\u0435, \u0447\u0442\u043E\u0431\u044B \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C",
    view_api: "\u043F\u0440\u043E\u0441\u043C\u043E\u0442\u0440 api",
    built_with_Gradio: "\u0441\u0434\u0435\u043B\u0430\u043D\u043E \u0441 \u043F\u043E\u043C\u043E\u0449\u044C\u044E gradio"
  },
  Submit: Submit$7,
  Clear: Clear$7,
  Interpret: Interpret$7,
  Flag: Flag$7,
  Examples: Examples$7,
  or: or$7
};
var __glob_1_14 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$7,
  Clear: Clear$7,
  Interpret: Interpret$7,
  Flag: Flag$7,
  Examples: Examples$7,
  or: or$7,
  "default": ru
});
const Submit$6 = "\u0B9A\u0BAE\u0BB0\u0BCD\u0BAA\u0BCD\u0BAA\u0BBF";
const Clear$6 = "\u0B85\u0BB4\u0BBF";
const Interpret$6 = "\u0B89\u0B9F\u0BCD\u0BAA\u0BCA\u0BB0\u0BC1\u0BB3\u0BCD";
const Flag$6 = "\u0B95\u0BCA\u0B9F\u0BBF\u0BAF\u0BBF\u0B9F\u0BC1";
const Examples$6 = "\u0B8E\u0B9F\u0BC1\u0BA4\u0BCD\u0BA4\u0BC1\u0B95\u0BCD\u0B95\u0BBE\u0B9F\u0BCD\u0B9F\u0BC1\u0B95\u0BB3\u0BCD";
const or$6 = "\u0B85\u0BB2\u0BCD\u0BB2\u0BA4\u0BC1";
var ta = {
  "interface": {
    drop_image: "\u0BAA\u0B9F\u0BA4\u0BCD\u0BA4\u0BC8 \u0BB5\u0BC8",
    drop_video: "\u0BB5\u0BC0\u0B9F\u0BBF\u0BAF\u0BCB\u0BB5\u0BC8 \u0BB5\u0BC8",
    drop_audio: "\u0B86\u0B9F\u0BBF\u0BAF\u0BCB\u0BB5\u0BC8 \u0BB5\u0BC8",
    drop_file: "\u0B95\u0BCB\u0BAA\u0BCD\u0BAA\u0BC8 \u0BB5\u0BC8",
    drop_csv: "\u0B9A\u0BBF\u0B8E\u0BB8\u0BCD\u0BB5\u0BBF \u0BB5\u0BC8",
    click_to_upload: "\u0BAA\u0BA4\u0BBF\u0BB5\u0BC7\u0BB1\u0BCD\u0BB1 \u0B95\u0BBF\u0BB3\u0BBF\u0B95\u0BCD \u0B9A\u0BC6\u0BAF\u0BCD",
    view_api: "\u0B85\u0BAA\u0BBF\u0BAF\u0BC8 \u0B95\u0BBE\u0BA3\u0BCD",
    built_with_Gradio: "\u0B95\u0BCD\u0BB0\u0BC7\u0B9F\u0BBF\u0BAF\u0BCB-\u0BB5\u0BC1\u0B9F\u0BA9\u0BCD \u0B95\u0B9F\u0BCD\u0B9F\u0BAA\u0BCD\u0BAA\u0B9F\u0BCD\u0B9F\u0BA4\u0BC1"
  },
  Submit: Submit$6,
  Clear: Clear$6,
  Interpret: Interpret$6,
  Flag: Flag$6,
  Examples: Examples$6,
  or: or$6
};
var __glob_1_15 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$6,
  Clear: Clear$6,
  Interpret: Interpret$6,
  Flag: Flag$6,
  Examples: Examples$6,
  or: or$6,
  "default": ta
});
const Submit$5 = "Y\xFCkle";
const Clear$5 = "Temizle";
const Interpret$5 = "Yorumla";
const Flag$5 = "Etiketle";
const Examples$5 = "\xF6rnekler";
const or$5 = "veya";
var tr = {
  "interface": {
    drop_image: "Resmi Buraya S\xFCr\xFCkle",
    drop_video: "Videoyu Buraya S\xFCr\xFCkle",
    drop_audio: "Kayd\u0131 Buraya S\xFCr\xFCkle",
    drop_file: "Dosyay\u0131 Buraya S\xFCr\xFCkle",
    drop_csv: "CSV'yi Buraya S\xFCr\xFCkle",
    click_to_upload: "Y\xFCklemek i\xE7in T\u0131kla",
    view_api: "api'yi g\xF6r\xFCnt\xFCle",
    built_with_Gradio: "Gradio ile olu\u015Fturulmu\u015Ftur"
  },
  Submit: Submit$5,
  Clear: Clear$5,
  Interpret: Interpret$5,
  Flag: Flag$5,
  Examples: Examples$5,
  or: or$5
};
var __glob_1_16 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$5,
  Clear: Clear$5,
  Interpret: Interpret$5,
  Flag: Flag$5,
  Examples: Examples$5,
  or: or$5,
  "default": tr
});
const Submit$4 = "\u041D\u0430\u0434\u0456\u0441\u043B\u0430\u0442\u0438";
const Clear$4 = "\u041E\u0447\u0438\u0441\u0442\u0438\u0442\u0438";
const Interpret$4 = "\u041F\u043E\u044F\u0441\u043D\u0438\u0442\u0438 \u0440\u0435\u0437\u0443\u043B\u044C\u0442\u0430\u0442";
const Flag$4 = "\u041F\u043E\u0437\u043D\u0430\u0447\u0438\u0442\u0438";
const Examples$4 = "\u041F\u0440\u0438\u043A\u043B\u0430\u0434\u0438";
const or$4 = "\u0430\u0431\u043E";
var uk = {
  "interface": {
    drop_image: "\u041F\u0435\u0440\u0435\u0442\u044F\u0433\u043D\u0456\u0442\u044C \u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u043D\u044F \u0441\u044E\u0434\u0438",
    drop_video: "\u041F\u0435\u0440\u0435\u0442\u044F\u0433\u043D\u0456\u0442\u044C \u0432\u0456\u0434\u0435\u043E \u0441\u044E\u0434\u0438",
    drop_audio: "\u041F\u0435\u0440\u0435\u0442\u044F\u0433\u043D\u0456\u0442\u044C \u0430\u0443\u0434\u0456\u043E \u0441\u044E\u0434\u0438",
    drop_file: "\u041F\u0435\u0440\u0435\u0442\u044F\u0433\u043D\u0456\u0442\u044C \u0444\u0430\u0439\u043B \u0441\u044E\u0434\u0438",
    drop_csv: "\u041F\u0435\u0440\u0435\u0442\u044F\u0433\u043D\u0456\u0442\u044C CSV-\u0444\u0430\u0439\u043B \u0441\u044E\u0434\u0438",
    click_to_upload: "\u041D\u0430\u0442\u0438\u0441\u043D\u0456\u0442\u044C \u0449\u043E\u0431 \u0437\u0430\u0432\u0430\u043D\u0442\u0430\u0436\u0438\u0442\u0438",
    view_api: "\u041F\u0435\u0440\u0435\u0433\u043B\u044F\u043D\u0443\u0442\u0438 API",
    built_with_Gradio: "\u0417\u0440\u043E\u0431\u043B\u0435\u043D\u043E \u043D\u0430 \u043E\u0441\u043D\u043E\u0432\u0456 gradio"
  },
  Submit: Submit$4,
  Clear: Clear$4,
  Interpret: Interpret$4,
  Flag: Flag$4,
  Examples: Examples$4,
  or: or$4
};
var __glob_1_17 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$4,
  Clear: Clear$4,
  Interpret: Interpret$4,
  Flag: Flag$4,
  Examples: Examples$4,
  or: or$4,
  "default": uk
});
const Submit$3 = "\u062C\u0645\u0639 \u06A9\u0631\u06CC\u06BA";
const Clear$3 = "\u06C1\u0679\u0627 \u062F\u06CC\u06BA";
const Interpret$3 = "\u062A\u0634\u0631\u06CC\u062D \u06A9\u0631\u06CC\u06BA";
const Flag$3 = "\u0646\u0634\u0627\u0646 \u0644\u06AF\u0627\u0626\u06CC\u06BA";
const Examples$3 = "\u0645\u062B\u0627\u0644\u06CC\u06BA";
const or$3 = "\u06CC\u0627";
var ur = {
  "interface": {
    drop_image: "\u06CC\u06C1\u0627\u06BA \u062A\u0635\u0648\u06CC\u0631 \u0688\u0631\u0627\u067E \u06A9\u0631\u06CC\u06BA",
    drop_video: "\u06CC\u06C1\u0627\u06BA \u0648\u06CC\u0688\u06CC\u0648 \u0688\u0631\u0627\u067E \u06A9\u0631\u06CC\u06BA",
    drop_audio: "\u06CC\u06C1\u0627\u06BA \u0622\u0688\u06CC\u0648 \u0688\u0631\u0627\u067E \u06A9\u0631\u06CC\u06BA",
    drop_file: "\u06CC\u06C1\u0627\u06BA \u0641\u0627\u0626\u0644 \u0688\u0631\u0627\u067E \u06A9\u0631\u06CC\u06BA",
    drop_csv: "\u06CC\u06C1\u0627\u06BA \u0641\u0627\u0626\u0644 \u0688\u0631\u0627\u067E \u06A9\u0631\u06CC\u06BA",
    click_to_upload: "\u0627\u067E \u0644\u0648\u0688 \u06A9\u06D2 \u0644\u06CC\u06D2 \u06A9\u0644\u06A9 \u06A9\u0631\u06CC\u06BA",
    view_api: "API \u062F\u06CC\u06A9\u06BE\u06CC\u06BA",
    built_with_Gradio: "\u06A9\u06D2 \u0633\u0627\u062A\u06BE \u0628\u0646\u0627\u06CC\u0627 \u06AF\u06CC\u0627 Gradio"
  },
  Submit: Submit$3,
  Clear: Clear$3,
  Interpret: Interpret$3,
  Flag: Flag$3,
  Examples: Examples$3,
  or: or$3
};
var __glob_1_18 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$3,
  Clear: Clear$3,
  Interpret: Interpret$3,
  Flag: Flag$3,
  Examples: Examples$3,
  or: or$3,
  "default": ur
});
const Submit$2 = "Yubor";
const Clear$2 = "Tozalash";
const Interpret$2 = "Tushuntirish";
const Flag$2 = "Bayroq";
const Examples$2 = "Namunalar";
const or$2 = "\u6216";
var uz = {
  "interface": {
    drop_image: "Rasmni Shu Yerga Tashlang",
    drop_video: "Videoni Shu Yerga Tashlang",
    drop_audio: "Audioni Shu Yerga Tashlang",
    drop_file: "Faylni Shu Yerga Tashlang",
    drop_csv: "CSVni Shu Yerga Tashlang",
    click_to_upload: "Yuklash uchun Bosing",
    view_api: "apini ko'ring",
    built_with_Gradio: "gradio bilan qilingan"
  },
  Submit: Submit$2,
  Clear: Clear$2,
  Interpret: Interpret$2,
  Flag: Flag$2,
  Examples: Examples$2,
  or: or$2
};
var __glob_1_19 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$2,
  Clear: Clear$2,
  Interpret: Interpret$2,
  Flag: Flag$2,
  Examples: Examples$2,
  or: or$2,
  "default": uz
});
const Submit$1 = "\u63D0\u4EA4";
const Clear$1 = "\u6E05\u9664";
const Interpret$1 = "\u89E3\u91CA";
const Flag$1 = "\u6807\u8BB0";
const Examples$1 = "\u793A\u4F8B";
const or$1 = "\u6216";
var zhCn = {
  "interface": {
    drop_image: "\u62D6\u653E\u56FE\u7247\u81F3\u6B64\u5904",
    drop_video: "\u62D6\u653E\u89C6\u9891\u81F3\u6B64\u5904",
    drop_audio: "\u62D6\u653E\u97F3\u9891\u81F3\u6B64\u5904",
    drop_file: "\u62D6\u653E\u6587\u4EF6\u81F3\u6B64\u5904",
    drop_csv: "\u62D6\u653ECSV\u81F3\u6B64\u5904",
    click_to_upload: "\u70B9\u51FB\u4E0A\u4F20",
    view_api: "\u67E5\u770BAPI",
    built_with_Gradio: "\u4F7F\u7528Gradio\u6784\u5EFA"
  },
  Submit: Submit$1,
  Clear: Clear$1,
  Interpret: Interpret$1,
  Flag: Flag$1,
  Examples: Examples$1,
  or: or$1
};
var __glob_1_20 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit: Submit$1,
  Clear: Clear$1,
  Interpret: Interpret$1,
  Flag: Flag$1,
  Examples: Examples$1,
  or: or$1,
  "default": zhCn
});
const Submit = "\u63D0\u4EA4";
const Clear = "\u6E05\u9664";
const Interpret = "\u89E3\u91CB";
const Flag = "Flag";
const Examples = "\u7BC4\u4F8B";
const or = "\u6216";
var zhTw = {
  "interface": {
    drop_image: "\u522A\u9664\u5716\u7247",
    drop_video: "\u522A\u9664\u5F71\u7247",
    drop_audio: "\u522A\u9664\u97F3\u983B",
    drop_file: "\u522A\u9664\u6A94\u6848",
    drop_csv: "\u522A\u9664CSV",
    click_to_upload: "\u9EDE\u64CA\u4E0A\u50B3",
    view_api: "\u67E5\u770BAPI",
    built_with_Gradio: "\u4F7F\u7528Gradio\u69CB\u5EFA"
  },
  Submit,
  Clear,
  Interpret,
  Flag,
  Examples,
  or
};
var __glob_1_21 = /* @__PURE__ */ Object.freeze({
  __proto__: null,
  [Symbol.toStringTag]: "Module",
  Submit,
  Clear,
  Interpret,
  Flag,
  Examples,
  or,
  "default": zhTw
});
const langs = { "./lang/ar.json": __glob_1_0, "./lang/de.json": __glob_1_1, "./lang/en.json": __glob_1_2, "./lang/es.json": __glob_1_3, "./lang/fa.json": __glob_1_4, "./lang/fr.json": __glob_1_5, "./lang/he.json": __glob_1_6, "./lang/hi.json": __glob_1_7, "./lang/ja.json": __glob_1_8, "./lang/ko.json": __glob_1_9, "./lang/lt.json": __glob_1_10, "./lang/nl.json": __glob_1_11, "./lang/pl.json": __glob_1_12, "./lang/pt-BR.json": __glob_1_13, "./lang/ru.json": __glob_1_14, "./lang/ta.json": __glob_1_15, "./lang/tr.json": __glob_1_16, "./lang/uk.json": __glob_1_17, "./lang/ur.json": __glob_1_18, "./lang/uz.json": __glob_1_19, "./lang/zh-cn.json": __glob_1_20, "./lang/zh-tw.json": __glob_1_21 };
function process_langs() {
  let _langs = {};
  for (const lang in langs) {
    const code = lang.split("/").pop().split(".").shift();
    _langs[code] = langs[lang].default;
  }
  return _langs;
}
const processed_langs = process_langs();
for (const lang in processed_langs) {
  m(lang, processed_langs[lang]);
}
function setupi18n() {
  $({
    fallbackLocale: "en",
    initialLocale: I()
  });
}
function get_each_context$1(ctx, list, i2) {
  const child_ctx = ctx.slice();
  child_ctx[8] = list[i2].component;
  child_ctx[17] = list[i2].id;
  child_ctx[2] = list[i2].props;
  child_ctx[18] = list[i2].children;
  child_ctx[9] = list[i2].has_modes;
  return child_ctx;
}
function create_if_block$6(ctx) {
  let each_blocks = [];
  let each_1_lookup = /* @__PURE__ */ new Map();
  let each_1_anchor;
  let current;
  let each_value = ctx[1];
  const get_key = (ctx2) => ctx2[17];
  for (let i2 = 0; i2 < each_value.length; i2 += 1) {
    let child_ctx = get_each_context$1(ctx, each_value, i2);
    let key = get_key(child_ctx);
    each_1_lookup.set(key, each_blocks[i2] = create_each_block$1(key, child_ctx));
  }
  return {
    c() {
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        each_blocks[i2].c();
      }
      each_1_anchor = empty();
    },
    m(target, anchor) {
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        each_blocks[i2].m(target, anchor);
      }
      insert(target, each_1_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (dirty & 235) {
        each_value = ctx2[1];
        group_outros();
        each_blocks = update_keyed_each(each_blocks, dirty, get_key, 1, ctx2, each_value, each_1_lookup, each_1_anchor.parentNode, outro_and_destroy_block, create_each_block$1, each_1_anchor, get_each_context$1);
        check_outros();
      }
    },
    i(local) {
      if (current)
        return;
      for (let i2 = 0; i2 < each_value.length; i2 += 1) {
        transition_in(each_blocks[i2]);
      }
      current = true;
    },
    o(local) {
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        transition_out(each_blocks[i2]);
      }
      current = false;
    },
    d(detaching) {
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        each_blocks[i2].d(detaching);
      }
      if (detaching)
        detach(each_1_anchor);
    }
  };
}
function create_each_block$1(key_1, ctx) {
  let first;
  let render;
  let current;
  render = new Render({
    props: {
      component: ctx[8],
      target: ctx[6],
      id: ctx[17],
      props: ctx[2],
      root: ctx[3],
      instance_map: ctx[0],
      children: ctx[18],
      dynamic_ids: ctx[5],
      has_modes: ctx[9],
      theme: ctx[7]
    }
  });
  render.$on("destroy", ctx[12]);
  render.$on("mount", ctx[13]);
  return {
    key: key_1,
    first: null,
    c() {
      first = empty();
      create_component(render.$$.fragment);
      this.first = first;
    },
    m(target, anchor) {
      insert(target, first, anchor);
      mount_component(render, target, anchor);
      current = true;
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      const render_changes = {};
      if (dirty & 2)
        render_changes.component = ctx[8];
      if (dirty & 64)
        render_changes.target = ctx[6];
      if (dirty & 2)
        render_changes.id = ctx[17];
      if (dirty & 2)
        render_changes.props = ctx[2];
      if (dirty & 8)
        render_changes.root = ctx[3];
      if (dirty & 1)
        render_changes.instance_map = ctx[0];
      if (dirty & 2)
        render_changes.children = ctx[18];
      if (dirty & 32)
        render_changes.dynamic_ids = ctx[5];
      if (dirty & 2)
        render_changes.has_modes = ctx[9];
      if (dirty & 128)
        render_changes.theme = ctx[7];
      render.$set(render_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(render.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(render.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(first);
      destroy_component(render, detaching);
    }
  };
}
function create_default_slot$3(ctx) {
  let if_block_anchor;
  let current;
  let if_block = ctx[1] && ctx[1].length && create_if_block$6(ctx);
  return {
    c() {
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if (if_block)
        if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[1] && ctx2[1].length) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty & 2) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block$6(ctx2);
          if_block.c();
          transition_in(if_block, 1);
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        }
      } else if (if_block) {
        group_outros();
        transition_out(if_block, 1, 1, () => {
          if_block = null;
        });
        check_outros();
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
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_fragment$a(ctx) {
  let switch_instance;
  let updating_value;
  let switch_instance_anchor;
  let current;
  const switch_instance_spread_levels = [
    {
      elem_id: "elem_id" in ctx[2] && ctx[2].elem_id || `component-${ctx[4]}`
    },
    { target: ctx[6] },
    ctx[2],
    { theme: ctx[7] },
    { root: ctx[3] }
  ];
  function switch_instance_value_binding(value) {
    ctx[15](value);
  }
  var switch_value = ctx[8];
  function switch_props(ctx2) {
    let switch_instance_props = {
      $$slots: { default: [create_default_slot$3] },
      $$scope: { ctx: ctx2 }
    };
    for (let i2 = 0; i2 < switch_instance_spread_levels.length; i2 += 1) {
      switch_instance_props = assign(switch_instance_props, switch_instance_spread_levels[i2]);
    }
    if (ctx2[0][ctx2[4]].props.value !== void 0) {
      switch_instance_props.value = ctx2[0][ctx2[4]].props.value;
    }
    return { props: switch_instance_props };
  }
  if (switch_value) {
    switch_instance = new switch_value(switch_props(ctx));
    ctx[14](switch_instance);
    binding_callbacks.push(() => bind(switch_instance, "value", switch_instance_value_binding));
    switch_instance.$on("prop_change", ctx[10]);
  }
  return {
    c() {
      if (switch_instance)
        create_component(switch_instance.$$.fragment);
      switch_instance_anchor = empty();
    },
    m(target, anchor) {
      if (switch_instance) {
        mount_component(switch_instance, target, anchor);
      }
      insert(target, switch_instance_anchor, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      const switch_instance_changes = dirty & 220 ? get_spread_update(switch_instance_spread_levels, [
        dirty & 20 && {
          elem_id: "elem_id" in ctx2[2] && ctx2[2].elem_id || `component-${ctx2[4]}`
        },
        dirty & 64 && { target: ctx2[6] },
        dirty & 4 && get_spread_object(ctx2[2]),
        dirty & 128 && { theme: ctx2[7] },
        dirty & 8 && { root: ctx2[3] }
      ]) : {};
      if (dirty & 2097387) {
        switch_instance_changes.$$scope = { dirty, ctx: ctx2 };
      }
      if (!updating_value && dirty & 17) {
        updating_value = true;
        switch_instance_changes.value = ctx2[0][ctx2[4]].props.value;
        add_flush_callback(() => updating_value = false);
      }
      if (switch_value !== (switch_value = ctx2[8])) {
        if (switch_instance) {
          group_outros();
          const old_component = switch_instance;
          transition_out(old_component.$$.fragment, 1, 0, () => {
            destroy_component(old_component, 1);
          });
          check_outros();
        }
        if (switch_value) {
          switch_instance = new switch_value(switch_props(ctx2));
          ctx2[14](switch_instance);
          binding_callbacks.push(() => bind(switch_instance, "value", switch_instance_value_binding));
          switch_instance.$on("prop_change", ctx2[10]);
          create_component(switch_instance.$$.fragment);
          transition_in(switch_instance.$$.fragment, 1);
          mount_component(switch_instance, switch_instance_anchor.parentNode, switch_instance_anchor);
        } else {
          switch_instance = null;
        }
      } else if (switch_value) {
        switch_instance.$set(switch_instance_changes);
      }
    },
    i(local) {
      if (current)
        return;
      if (switch_instance)
        transition_in(switch_instance.$$.fragment, local);
      current = true;
    },
    o(local) {
      if (switch_instance)
        transition_out(switch_instance.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      ctx[14](null);
      if (detaching)
        detach(switch_instance_anchor);
      if (switch_instance)
        destroy_component(switch_instance, detaching);
    }
  };
}
function instance$a($$self, $$props, $$invalidate) {
  let { root } = $$props;
  let { component } = $$props;
  let { instance_map } = $$props;
  let { id: id2 } = $$props;
  let { props } = $$props;
  let { children: children2 } = $$props;
  let { dynamic_ids } = $$props;
  let { has_modes } = $$props;
  let { parent = null } = $$props;
  let { target } = $$props;
  let { theme } = $$props;
  const dispatch2 = createEventDispatcher();
  if (has_modes) {
    if (props.interactive === false) {
      props.mode = "static";
    } else if (props.interactive === true) {
      props.mode = "dynamic";
    } else if (dynamic_ids.has(id2)) {
      props.mode = "dynamic";
    } else {
      props.mode = "static";
    }
  }
  onMount(() => {
    dispatch2("mount", id2);
    return () => dispatch2("destroy", id2);
  });
  setContext("BLOCK_KEY", parent);
  function handle_prop_change(e) {
    for (const k2 in e.detail) {
      $$invalidate(0, instance_map[id2].props[k2] = e.detail[k2], instance_map);
    }
  }
  function destroy_handler(event) {
    bubble.call(this, $$self, event);
  }
  function mount_handler(event) {
    bubble.call(this, $$self, event);
  }
  function switch_instance_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      instance_map[id2].instance = $$value;
      $$invalidate(0, instance_map);
    });
  }
  function switch_instance_value_binding(value) {
    if ($$self.$$.not_equal(instance_map[id2].props.value, value)) {
      instance_map[id2].props.value = value;
      $$invalidate(0, instance_map);
    }
  }
  $$self.$$set = ($$props2) => {
    if ("root" in $$props2)
      $$invalidate(3, root = $$props2.root);
    if ("component" in $$props2)
      $$invalidate(8, component = $$props2.component);
    if ("instance_map" in $$props2)
      $$invalidate(0, instance_map = $$props2.instance_map);
    if ("id" in $$props2)
      $$invalidate(4, id2 = $$props2.id);
    if ("props" in $$props2)
      $$invalidate(2, props = $$props2.props);
    if ("children" in $$props2)
      $$invalidate(1, children2 = $$props2.children);
    if ("dynamic_ids" in $$props2)
      $$invalidate(5, dynamic_ids = $$props2.dynamic_ids);
    if ("has_modes" in $$props2)
      $$invalidate(9, has_modes = $$props2.has_modes);
    if ("parent" in $$props2)
      $$invalidate(11, parent = $$props2.parent);
    if ("target" in $$props2)
      $$invalidate(6, target = $$props2.target);
    if ("theme" in $$props2)
      $$invalidate(7, theme = $$props2.theme);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 3) {
      $$invalidate(1, children2 = children2 && children2.filter((v2) => instance_map[v2.id].type !== "statustracker"));
    }
  };
  return [
    instance_map,
    children2,
    props,
    root,
    id2,
    dynamic_ids,
    target,
    theme,
    component,
    has_modes,
    handle_prop_change,
    parent,
    destroy_handler,
    mount_handler,
    switch_instance_binding,
    switch_instance_value_binding
  ];
}
class Render extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$a, create_fragment$a, safe_not_equal, {
      root: 3,
      component: 8,
      instance_map: 0,
      id: 4,
      props: 2,
      children: 1,
      dynamic_ids: 5,
      has_modes: 9,
      parent: 11,
      target: 6,
      theme: 7
    });
  }
}
const QUEUE_FULL_MSG = "This application is too busy. Keep trying!";
const BROKEN_CONNECTION_MSG = "Connection errored out.";
async function post_data(url, body) {
  try {
    var response = await fetch(url, {
      method: "POST",
      body: JSON.stringify(body),
      headers: { "Content-Type": "application/json" }
    });
  } catch (e) {
    return [{ error: BROKEN_CONNECTION_MSG }, 500];
  }
  const output = await response.json();
  return [output, response.status];
}
const ws_map = /* @__PURE__ */ new Map();
const fn = (session_hash, api_endpoint, is_space, is_embed) => async ({
  action,
  payload,
  queue,
  backend_fn,
  frontend_fn,
  output_data,
  queue_callback,
  loading_status,
  cancels
}) => {
  const fn_index = payload.fn_index;
  payload.session_hash = session_hash;
  if (frontend_fn !== void 0) {
    payload.data = await frontend_fn(payload.data.concat(output_data));
  }
  if (backend_fn == false) {
    return payload;
  }
  const api_endpoint2 = window.gradio_config.root;
  console.log("root", window.gradio_config.root);
  if (queue && ["predict", "interpret"].includes(action)) {
    let send_message = function(fn2, data) {
      var _a2;
      (_a2 = ws_map.get(fn2)) == null ? void 0 : _a2.send(JSON.stringify(data));
    };
    loading_status.update(fn_index, "pending", queue, null, null, null, null);
    let WS_ENDPOINT = "";
    if (is_embed) {
      WS_ENDPOINT = `wss://${new URL(api_endpoint2).host}/queue/join`;
    } else {
      var ws_endpoint = api_endpoint2 === "run/" ? location.href : api_endpoint2;
      var ws_protocol = ws_endpoint.startsWith("https") ? "wss:" : "ws:";
      var ws_path = location.pathname === "/" ? "/" : location.pathname;
      var ws_host = location.origin === "http://localhost:3000" ? "".replace("http://", "").slice(0, -1) : location.host;
      WS_ENDPOINT = `${ws_protocol}//${ws_host}${ws_path}queue/join`;
    }
    var websocket = new WebSocket(WS_ENDPOINT);
    ws_map.set(fn_index, websocket);
    websocket.onclose = (evt) => {
      if (!evt.wasClean) {
        loading_status.update(fn_index, "error", queue, null, null, null, BROKEN_CONNECTION_MSG);
      }
    };
    websocket.onmessage = async function(event) {
      var _a2, _b;
      const data = JSON.parse(event.data);
      switch (data.msg) {
        case "send_data":
          send_message(fn_index, payload);
          break;
        case "send_hash":
          (_a2 = ws_map.get(fn_index)) == null ? void 0 : _a2.send(JSON.stringify({
            session_hash,
            fn_index
          }));
          break;
        case "queue_full":
          loading_status.update(fn_index, "error", queue, null, null, null, QUEUE_FULL_MSG);
          websocket.close();
          return;
        case "estimation":
          loading_status.update(fn_index, ((_b = get_store_value(loading_status)[data.fn_index]) == null ? void 0 : _b.status) || "pending", queue, data.queue_size, data.rank, data.rank_eta, null);
          break;
        case "process_generating":
          loading_status.update(fn_index, data.success ? "generating" : "error", queue, null, null, data.output.average_duration, !data.success ? data.output.error : null);
          if (data.success) {
            queue_callback(data.output);
          }
          break;
        case "process_completed":
          loading_status.update(fn_index, data.success ? "complete" : "error", queue, null, null, data.output.average_duration, !data.success ? data.output.error : null);
          if (data.success) {
            queue_callback(data.output);
          }
          websocket.close();
          return;
        case "process_starts":
          loading_status.update(fn_index, "pending", queue, data.rank, 0, null, null);
          break;
      }
    };
  } else {
    loading_status.update(fn_index, "pending", queue, null, null, null, null);
    var [output, status_code] = await post_data(api_endpoint2 + action + "/", __spreadProps(__spreadValues({}, payload), {
      session_hash
    }));
    if (status_code == 200) {
      loading_status.update(fn_index, "complete", queue, null, null, output.average_duration, null);
      if (cancels.length > 0) {
        cancels.forEach((fn_index2) => {
          var _a2;
          loading_status.update(fn_index2, "complete", queue, null, null, null, null);
          (_a2 = ws_map.get(fn_index2)) == null ? void 0 : _a2.close();
        });
      }
    } else {
      loading_status.update(fn_index, "error", queue, null, null, null, output.error);
      throw output.error || "API Error";
    }
    return output;
  }
};
function is_date(obj) {
  return Object.prototype.toString.call(obj) === "[object Date]";
}
function tick_spring(ctx, last_value, current_value, target_value) {
  if (typeof current_value === "number" || is_date(current_value)) {
    const delta = target_value - current_value;
    const velocity = (current_value - last_value) / (ctx.dt || 1 / 60);
    const spring2 = ctx.opts.stiffness * delta;
    const damper = ctx.opts.damping * velocity;
    const acceleration = (spring2 - damper) * ctx.inv_mass;
    const d2 = (velocity + acceleration) * ctx.dt;
    if (Math.abs(d2) < ctx.opts.precision && Math.abs(delta) < ctx.opts.precision) {
      return target_value;
    } else {
      ctx.settled = false;
      return is_date(current_value) ? new Date(current_value.getTime() + d2) : current_value + d2;
    }
  } else if (Array.isArray(current_value)) {
    return current_value.map((_2, i2) => tick_spring(ctx, last_value[i2], current_value[i2], target_value[i2]));
  } else if (typeof current_value === "object") {
    const next_value = {};
    for (const k2 in current_value) {
      next_value[k2] = tick_spring(ctx, last_value[k2], current_value[k2], target_value[k2]);
    }
    return next_value;
  } else {
    throw new Error(`Cannot spring ${typeof current_value} values`);
  }
}
function spring(value, opts = {}) {
  const store = writable(value);
  const { stiffness = 0.15, damping = 0.8, precision = 0.01 } = opts;
  let last_time;
  let task;
  let current_token;
  let last_value = value;
  let target_value = value;
  let inv_mass = 1;
  let inv_mass_recovery_rate = 0;
  let cancel_task = false;
  function set(new_value, opts2 = {}) {
    target_value = new_value;
    const token = current_token = {};
    if (value == null || opts2.hard || spring2.stiffness >= 1 && spring2.damping >= 1) {
      cancel_task = true;
      last_time = now();
      last_value = new_value;
      store.set(value = target_value);
      return Promise.resolve();
    } else if (opts2.soft) {
      const rate = opts2.soft === true ? 0.5 : +opts2.soft;
      inv_mass_recovery_rate = 1 / (rate * 60);
      inv_mass = 0;
    }
    if (!task) {
      last_time = now();
      cancel_task = false;
      task = loop((now2) => {
        if (cancel_task) {
          cancel_task = false;
          task = null;
          return false;
        }
        inv_mass = Math.min(inv_mass + inv_mass_recovery_rate, 1);
        const ctx = {
          inv_mass,
          opts: spring2,
          settled: true,
          dt: (now2 - last_time) * 60 / 1e3
        };
        const next_value = tick_spring(ctx, last_value, value, target_value);
        last_time = now2;
        last_value = value;
        store.set(value = next_value);
        if (ctx.settled) {
          task = null;
        }
        return !ctx.settled;
      });
    }
    return new Promise((fulfil) => {
      task.promise.then(() => {
        if (token === current_token)
          fulfil();
      });
    });
  }
  const spring2 = {
    set,
    update: (fn2, opts2) => set(fn2(target_value, value), opts2),
    subscribe: store.subscribe,
    stiffness,
    damping,
    precision
  };
  return spring2;
}
function create_fragment$9(ctx) {
  let div;
  let svg;
  let g0;
  let path0;
  let path1;
  let path2;
  let path3;
  let g1;
  let path4;
  let path5;
  let path6;
  let path7;
  return {
    c() {
      div = element("div");
      svg = svg_element("svg");
      g0 = svg_element("g");
      path0 = svg_element("path");
      path1 = svg_element("path");
      path2 = svg_element("path");
      path3 = svg_element("path");
      g1 = svg_element("g");
      path4 = svg_element("path");
      path5 = svg_element("path");
      path6 = svg_element("path");
      path7 = svg_element("path");
      attr(path0, "d", "M255.926 0.754768L509.702 139.936V221.027L255.926 81.8465V0.754768Z");
      attr(path0, "fill", "#FF7C00");
      attr(path0, "fill-opacity", "0.4");
      attr(path1, "d", "M509.69 139.936L254.981 279.641V361.255L509.69 221.55V139.936Z");
      attr(path1, "fill", "#FF7C00");
      attr(path2, "d", "M0.250138 139.937L254.981 279.641V361.255L0.250138 221.55V139.937Z");
      attr(path2, "fill", "#FF7C00");
      attr(path2, "fill-opacity", "0.4");
      attr(path3, "d", "M255.923 0.232622L0.236328 139.936V221.55L255.923 81.8469V0.232622Z");
      attr(path3, "fill", "#FF7C00");
      set_style(g0, "transform", "translate(" + ctx[1][0] + "px, " + ctx[1][1] + "px)");
      attr(path4, "d", "M255.926 141.5L509.702 280.681V361.773L255.926 222.592V141.5Z");
      attr(path4, "fill", "#FF7C00");
      attr(path4, "fill-opacity", "0.4");
      attr(path5, "d", "M509.69 280.679L254.981 420.384V501.998L509.69 362.293V280.679Z");
      attr(path5, "fill", "#FF7C00");
      attr(path6, "d", "M0.250138 280.681L254.981 420.386V502L0.250138 362.295V280.681Z");
      attr(path6, "fill", "#FF7C00");
      attr(path6, "fill-opacity", "0.4");
      attr(path7, "d", "M255.923 140.977L0.236328 280.68V362.294L255.923 222.591V140.977Z");
      attr(path7, "fill", "#FF7C00");
      set_style(g1, "transform", "translate(" + ctx[2][0] + "px, " + ctx[2][1] + "px)");
      attr(svg, "class", "text-xl");
      attr(svg, "width", "5em");
      attr(svg, "height", "5em");
      attr(svg, "viewBox", "-1200 -1200 3000 3000");
      attr(svg, "fill", "none");
      attr(svg, "xmlns", "http://www.w3.org/2000/svg");
      attr(div, "class", "z-20");
      toggle_class(div, "m-12", ctx[0]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      append(div, svg);
      append(svg, g0);
      append(g0, path0);
      append(g0, path1);
      append(g0, path2);
      append(g0, path3);
      append(svg, g1);
      append(g1, path4);
      append(g1, path5);
      append(g1, path6);
      append(g1, path7);
    },
    p(ctx2, [dirty]) {
      if (dirty & 2) {
        set_style(g0, "transform", "translate(" + ctx2[1][0] + "px, " + ctx2[1][1] + "px)");
      }
      if (dirty & 4) {
        set_style(g1, "transform", "translate(" + ctx2[2][0] + "px, " + ctx2[2][1] + "px)");
      }
      if (dirty & 1) {
        toggle_class(div, "m-12", ctx2[0]);
      }
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function instance$9($$self, $$props, $$invalidate) {
  let $top;
  let $bottom;
  let { margin = true } = $$props;
  const top = spring([0, 0]);
  component_subscribe($$self, top, (value) => $$invalidate(1, $top = value));
  const bottom = spring([0, 0]);
  component_subscribe($$self, bottom, (value) => $$invalidate(2, $bottom = value));
  let dismounted;
  function animate() {
    return new Promise(async (res) => {
      await Promise.all([top.set([125, 140]), bottom.set([-125, -140])]);
      await Promise.all([top.set([-125, 140]), bottom.set([125, -140])]);
      await Promise.all([top.set([-125, 0]), bottom.set([125, -0])]);
      await Promise.all([top.set([125, 0]), bottom.set([-125, 0])]);
      res();
    });
  }
  async function run2() {
    await animate();
    if (!dismounted)
      run2();
  }
  async function loading() {
    await Promise.all([top.set([125, 0]), bottom.set([-125, 0])]);
    run2();
  }
  onMount(() => {
    loading();
    return () => dismounted = true;
  });
  $$self.$$set = ($$props2) => {
    if ("margin" in $$props2)
      $$invalidate(0, margin = $$props2.margin);
  };
  return [margin, $top, $bottom, top, bottom];
}
class Loader extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$9, create_fragment$9, safe_not_equal, { margin: 0 });
  }
}
var api_logo = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjgiIGhlaWdodD0iMjgiIHZpZXdCb3g9IjAgMCAyOCAyOCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZmlsbC1ydWxlPSJldmVub2RkIiBjbGlwLXJ1bGU9ImV2ZW5vZGQiIGQ9Ik0yNi45NDI1IDIuOTQyNjVDMjcuNDYzMiAyLjQyMTk1IDI3LjQ2MzIgMS41Nzc3MyAyNi45NDI1IDEuMDU3MDNDMjYuNDIxOCAwLjUzNjMyOSAyNS41Nzc2IDAuNTM2MzI5IDI1LjA1NjkgMS4wNTcwM0wyMi41NzEzIDMuNTQyNTZDMjEuMTIxMyAyLjU5MzMzIDE5LjUzNjcgMi40MzM3OCAxOC4xNzUzIDIuNjQwMDZDMTYuNTQ5NSAyLjg4NjM4IDE1LjExMjcgMy42NjgzOCAxNC4zOTA1IDQuMzkwNTNMMTIuMzkwNSA2LjM5MDUzQzEyLjE0MDUgNi42NDA1OCAxMiA2Ljk3OTcyIDEyIDcuMzMzMzRDMTIgNy42ODY5NyAxMi4xNDA1IDguMDI2MSAxMi4zOTA1IDguMjc2MTVMMTkuNzIzOSAxNS42MDk1QzIwLjI0NDYgMTYuMTMwMiAyMS4wODg4IDE2LjEzMDIgMjEuNjA5NSAxNS42MDk1TDIzLjYwOTUgMTMuNjA5NUMyNC4zMzE2IDEyLjg4NzMgMjUuMTEzNiAxMS40NTA1IDI1LjM2IDkuODI0NzVDMjUuNTY2MyA4LjQ2MzEyIDI1LjQwNjYgNi44NzgyNyAyNC40NTcxIDUuNDI4MDdMMjYuOTQyNSAyLjk0MjY1WiIgZmlsbD0iIzNjNDU1NSIvPgo8cGF0aCBmaWxsLXJ1bGU9ImV2ZW5vZGQiIGNsaXAtcnVsZT0iZXZlbm9kZCIgZD0iTTEyLjI3NiAxMi45NDI2QzEyLjc5NjcgMTIuNDIxOSAxMi43OTY3IDExLjU3NzcgMTIuMjc2IDExLjA1N0MxMS43NTUzIDEwLjUzNjMgMTAuOTExMSAxMC41MzYzIDEwLjM5MDQgMTEuMDU3TDguNjY2NTEgMTIuNzgwOUw4LjI3NjE1IDEyLjM5MDVDOC4wMjYxIDEyLjE0MDUgNy42ODY5NyAxMiA3LjMzMzM0IDEyQzYuOTc5NzIgMTIgNi42NDA1OCAxMi4xNDA1IDYuMzkwNTMgMTIuMzkwNUw0LjM5MDUzIDE0LjM5MDVDMy42NjgzOCAxNS4xMTI3IDIuODg2MzggMTYuNTQ5NSAyLjY0MDA2IDE4LjE3NTNDMi40MzM3NyAxOS41MzY3IDIuNTkzMzMgMjEuMTIxNCAzLjU0MjYyIDIyLjU3MTRMMS4wNTcwMyAyNS4wNTdDMC41MzYzMjkgMjUuNTc3NyAwLjUzNjMyOSAyNi40MjE5IDEuMDU3MDMgMjYuOTQyNkMxLjU3NzczIDI3LjQ2MzMgMi40MjE5NSAyNy40NjMzIDIuOTQyNjUgMjYuOTQyNkw1LjQyODE3IDI0LjQ1NzFDNi44NzgzNSAyNS40MDY2IDguNDYzMTUgMjUuNTY2MyA5LjgyNDc1IDI1LjM2QzExLjQ1MDUgMjUuMTEzNiAxMi44ODczIDI0LjMzMTYgMTMuNjA5NSAyMy42MDk1TDE1LjYwOTUgMjEuNjA5NUMxNi4xMzAyIDIxLjA4ODggMTYuMTMwMiAyMC4yNDQ2IDE1LjYwOTUgMTkuNzIzOUwxNS4yMTg4IDE5LjMzMzJMMTYuOTQyNiAxNy42MDkzQzE3LjQ2MzMgMTcuMDg4NiAxNy40NjMzIDE2LjI0NDQgMTYuOTQyNiAxNS43MjM3QzE2LjQyMTkgMTUuMjAzIDE1LjU3NzcgMTUuMjAzIDE1LjA1NyAxNS43MjM3TDEzLjMzMzIgMTcuNDQ3NUwxMC41NTIxIDE0LjY2NjVMMTIuMjc2IDEyLjk0MjZaIiBmaWxsPSIjRkY3QzAwIi8+Cjwvc3ZnPgo=";
var clear = "data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjwhLS0gQ3JlYXRlZCB3aXRoIElua3NjYXBlIChodHRwOi8vd3d3Lmlua3NjYXBlLm9yZy8pIC0tPgoKPHN2ZwogICB3aWR0aD0iNS45NDAzOTQ5bW0iCiAgIGhlaWdodD0iNS45NDAzOTQ5bW0iCiAgIHZpZXdCb3g9IjAgMCA1Ljk0MDM5NDkgNS45NDAzOTQ5IgogICB2ZXJzaW9uPSIxLjEiCiAgIGlkPSJzdmc1IgogICBpbmtzY2FwZTp2ZXJzaW9uPSIxLjEgKGM2OGUyMmMzODcsIDIwMjEtMDUtMjMpIgogICBzb2RpcG9kaTpkb2NuYW1lPSJjbGVhci5zdmciCiAgIHhtbG5zOmlua3NjYXBlPSJodHRwOi8vd3d3Lmlua3NjYXBlLm9yZy9uYW1lc3BhY2VzL2lua3NjYXBlIgogICB4bWxuczpzb2RpcG9kaT0iaHR0cDovL3NvZGlwb2RpLnNvdXJjZWZvcmdlLm5ldC9EVEQvc29kaXBvZGktMC5kdGQiCiAgIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIKICAgeG1sbnM6c3ZnPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgPHNvZGlwb2RpOm5hbWVkdmlldwogICAgIGlkPSJuYW1lZHZpZXc3IgogICAgIHBhZ2Vjb2xvcj0iI2ZmZmZmZiIKICAgICBib3JkZXJjb2xvcj0iIzY2NjY2NiIKICAgICBib3JkZXJvcGFjaXR5PSIxLjAiCiAgICAgaW5rc2NhcGU6cGFnZXNoYWRvdz0iMiIKICAgICBpbmtzY2FwZTpwYWdlb3BhY2l0eT0iMC4wIgogICAgIGlua3NjYXBlOnBhZ2VjaGVja2VyYm9hcmQ9IjAiCiAgICAgaW5rc2NhcGU6ZG9jdW1lbnQtdW5pdHM9Im1tIgogICAgIHNob3dncmlkPSJmYWxzZSIKICAgICBpbmtzY2FwZTp6b29tPSIxMC45MjU0NzQiCiAgICAgaW5rc2NhcGU6Y3g9IjQuMTE4ODE0MyIKICAgICBpbmtzY2FwZTpjeT0iMTUuNTU5OTY1IgogICAgIGlua3NjYXBlOndpbmRvdy13aWR0aD0iMTI0OCIKICAgICBpbmtzY2FwZTp3aW5kb3ctaGVpZ2h0PSI3NzAiCiAgICAgaW5rc2NhcGU6d2luZG93LXg9Ii02IgogICAgIGlua3NjYXBlOndpbmRvdy15PSItNiIKICAgICBpbmtzY2FwZTp3aW5kb3ctbWF4aW1pemVkPSIxIgogICAgIGlua3NjYXBlOmN1cnJlbnQtbGF5ZXI9ImxheWVyMSIgLz4KICA8ZGVmcwogICAgIGlkPSJkZWZzMiIgLz4KICA8ZwogICAgIGlua3NjYXBlOmxhYmVsPSJMYXllciAxIgogICAgIGlua3NjYXBlOmdyb3VwbW9kZT0ibGF5ZXIiCiAgICAgaWQ9ImxheWVyMSIKICAgICB0cmFuc2Zvcm09InRyYW5zbGF0ZSgtMTE1LjEwOTQyLC0xMTkuMjIzNTMpIj4KICAgIDxnCiAgICAgICBpZD0iZzIzOSIKICAgICAgIHRyYW5zZm9ybT0ibWF0cml4KDAuMDUxMzg5ODYsMC4wNTEzODk4NiwtMC4wNTEzODk4NiwwLjA1MTM4OTg2LDExNy4wODY5LDExMi43NTMxNykiPgogICAgICA8cmVjdAogICAgICAgICBzdHlsZT0iZmlsbDojMDAwMDAwO3N0cm9rZS13aWR0aDowLjI5NTI4NyIKICAgICAgICAgaWQ9InJlY3QzMSIKICAgICAgICAgd2lkdGg9IjIwIgogICAgICAgICBoZWlnaHQ9IjgwIgogICAgICAgICB4PSItMTExLjUxMTA3IgogICAgICAgICB5PSI0Mi4xOTM3MjYiCiAgICAgICAgIHJ4PSIyLjk0MzQxMjgiCiAgICAgICAgIHJ5PSIyLjY0NDgwNTciCiAgICAgICAgIHRyYW5zZm9ybT0ic2NhbGUoLTEsMSkiIC8+CiAgICAgIDxyZWN0CiAgICAgICAgIHN0eWxlPSJmaWxsOiMwMDAwMDA7c3Ryb2tlLXdpZHRoOjAuMjk1Mjg3IgogICAgICAgICBpZD0icmVjdDMxLTMiCiAgICAgICAgIHdpZHRoPSIyMCIKICAgICAgICAgaGVpZ2h0PSI4MCIKICAgICAgICAgeD0iLTkyLjE5MzcyNiIKICAgICAgICAgeT0iLTE0MS41MTEwNiIKICAgICAgICAgcng9IjIuOTQzNDEyOCIKICAgICAgICAgcnk9IjIuNjQ0ODA1NyIKICAgICAgICAgdHJhbnNmb3JtPSJtYXRyaXgoMCwtMSwtMSwwLDAsMCkiIC8+CiAgICA8L2c+CiAgPC9nPgo8L3N2Zz4K";
var python = "data:image/svg+xml;base64,PHN2ZwoJeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIgoJeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiCglhcmlhLWhpZGRlbj0idHJ1ZSIKCWZvY3VzYWJsZT0iZmFsc2UiCglyb2xlPSJpbWciCgl3aWR0aD0iMWVtIgoJaGVpZ2h0PSIxZW0iCglwcmVzZXJ2ZUFzcGVjdFJhdGlvPSJ4TWlkWU1pZCBtZWV0IgoJdmlld0JveD0iMCAwIDMyIDMyIgo+Cgk8cGF0aAoJCWQ9Ik0xNS44NC41YTE2LjQsMTYuNCwwLDAsMC0zLjU3LjMyQzkuMSwxLjM5LDguNTMsMi41Myw4LjUzLDQuNjRWNy40OEgxNnYxSDUuNzdhNC43Myw0LjczLDAsMCwwLTQuNywzLjc0LDE0LjgyLDE0LjgyLDAsMCwwLDAsNy41NGMuNTcsMi4yOCwxLjg2LDMuODIsNCwzLjgyaDIuNlYyMC4xNGE0LjczLDQuNzMsMCwwLDEsNC42My00LjYzaDcuMzhhMy43MiwzLjcyLDAsMCwwLDMuNzMtMy43M1Y0LjY0QTQuMTYsNC4xNiwwLDAsMCwxOS42NS44MiwyMC40OSwyMC40OSwwLDAsMCwxNS44NC41Wk0xMS43OCwyLjc3YTEuMzksMS4zOSwwLDAsMSwxLjM4LDEuNDYsMS4zNywxLjM3LDAsMCwxLTEuMzgsMS4zOEExLjQyLDEuNDIsMCwwLDEsMTAuNCw0LjIzLDEuNDQsMS40NCwwLDAsMSwxMS43OCwyLjc3WiIKCQlmaWxsPSIjNWE5ZmQ0IgoJPjwvcGF0aD4KCTxwYXRoCgkJZD0iTTE2LjE2LDMxLjVhMTYuNCwxNi40LDAsMCwwLDMuNTctLjMyYzMuMTctLjU3LDMuNzQtMS43MSwzLjc0LTMuODJWMjQuNTJIMTZ2LTFIMjYuMjNhNC43Myw0LjczLDAsMCwwLDQuNy0zLjc0LDE0LjgyLDE0LjgyLDAsMCwwLDAtNy41NGMtLjU3LTIuMjgtMS44Ni0zLjgyLTQtMy44MmgtMi42djMuNDFhNC43Myw0LjczLDAsMCwxLTQuNjMsNC42M0gxMi4zNWEzLjcyLDMuNzIsMCwwLDAtMy43MywzLjczdjcuMTRhNC4xNiw0LjE2LDAsMCwwLDMuNzMsMy44MkEyMC40OSwyMC40OSwwLDAsMCwxNi4xNiwzMS41Wm00LjA2LTIuMjdhMS4zOSwxLjM5LDAsMCwxLTEuMzgtMS40NiwxLjM3LDEuMzcsMCwwLDEsMS4zOC0xLjM4LDEuNDIsMS40MiwwLDAsMSwxLjM4LDEuMzhBMS40NCwxLjQ0LDAsMCwxLDIwLjIyLDI5LjIzWiIKCQlmaWxsPSIjZmZkNDNiIgoJPjwvcGF0aD4KPC9zdmc+Cg==";
var javascript = "data:image/svg+xml;base64,PHN2ZwoJeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIgoJeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiCglhcmlhLWhpZGRlbj0idHJ1ZSIKCWZvY3VzYWJsZT0iZmFsc2UiCglyb2xlPSJpbWciCgl3aWR0aD0iMWVtIgoJaGVpZ2h0PSIxZW0iCglwcmVzZXJ2ZUFzcGVjdFJhdGlvPSJ4TWlkWU1pZCBtZWV0IgoJdmlld0JveD0iMCAwIDMyIDMyIgo+Cgk8cmVjdCB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIGZpbGw9IiNmN2RmMWUiPjwvcmVjdD4KCTxwYXRoCgkJZD0iTTIxLjUsMjVhMy4yNywzLjI3LDAsMCwwLDMsMS44M2MxLjI1LDAsMi0uNjMsMi0xLjQ5LDAtMS0uODEtMS4zOS0yLjE5LTJMMjMuNTYsMjNDMjEuMzksMjIuMSwyMCwyMC45NCwyMCwxOC40OWMwLTIuMjUsMS43Mi00LDQuNDEtNGE0LjQ0LDQuNDQsMCwwLDEsNC4yNywyLjQxbC0yLjM0LDEuNWEyLDIsMCwwLDAtMS45My0xLjI5LDEuMzEsMS4zMSwwLDAsMC0xLjQ0LDEuMjljMCwuOS41NiwxLjI3LDEuODUsMS44M2wuNzUuMzJjMi41NSwxLjEsNCwyLjIxLDQsNC43MiwwLDIuNzEtMi4xMiw0LjE5LTUsNC4xOWE1Ljc4LDUuNzgsMCwwLDEtNS40OC0zLjA3Wm0tMTAuNjMuMjZjLjQ4Ljg0LjkxLDEuNTUsMS45NCwxLjU1czEuNjEtLjM5LDEuNjEtMS44OVYxNC42OWgzVjI1YzAsMy4xMS0xLjgzLDQuNTMtNC40OSw0LjUzYTQuNjYsNC42NiwwLDAsMS00LjUxLTIuNzVaIgoJPjwvcGF0aD4KPC9zdmc+Cg==";
function get_each_context(ctx, list, i2) {
  const child_ctx = ctx.slice();
  child_ctx[19] = list[i2];
  child_ctx[20] = list;
  child_ctx[21] = i2;
  return child_ctx;
}
function get_each_context_2(ctx, list, i2) {
  const child_ctx = ctx.slice();
  child_ctx[22] = list[i2];
  child_ctx[24] = i2;
  return child_ctx;
}
function get_each_context_1(ctx, list, i2) {
  const child_ctx = ctx.slice();
  child_ctx[22] = list[i2];
  child_ctx[24] = i2;
  return child_ctx;
}
function get_each_context_3(ctx, list, i2) {
  const child_ctx = ctx.slice();
  child_ctx[26] = list[i2][0];
  child_ctx[27] = list[i2][1];
  return child_ctx;
}
function get_each_context_4(ctx, list, i2) {
  const child_ctx = ctx.slice();
  child_ctx[30] = list[i2];
  child_ctx[31] = list;
  child_ctx[24] = i2;
  return child_ctx;
}
function get_each_context_5(ctx, list, i2) {
  const child_ctx = ctx.slice();
  child_ctx[30] = list[i2];
  child_ctx[32] = list;
  child_ctx[24] = i2;
  return child_ctx;
}
function create_else_block_1(ctx) {
  let div1;
  let h2;
  let t0;
  let span0;
  let t1;
  let t2;
  let button;
  let img;
  let img_src_value;
  let t3;
  let div0;
  let mounted;
  let dispose;
  return {
    c() {
      div1 = element("div");
      h2 = element("h2");
      t0 = text("No named API Routes found for\n			");
      span0 = element("span");
      t1 = text(ctx[0]);
      t2 = space();
      button = element("button");
      img = element("img");
      t3 = space();
      div0 = element("div");
      div0.innerHTML = `To expose an API endpoint of your app in this page, set the <span class="text-gray-800 text-sm bg-gray-200/80 dark:bg-gray-600 px-1 rounded font-mono">api_name</span>
			parameter of the event listener.<br/> For more information, visit the
			<a href="https://gradio.app/sharing_your_app/#api-page" target="_blank" class="text-orange-500 hover:text-orange-600 underline">API Page guide</a>. To hide the API documentation button and this page, set
			<span class="text-gray-800 text-sm bg-gray-200/80 dark:bg-gray-600 px-1 rounded font-mono">show_api=False</span>
			in the
			<span class="text-gray-800 text-sm bg-gray-200/80 dark:bg-gray-600 px-1 rounded font-mono">Blocks.launch()</span> method.`;
      attr(span0, "class", "italic text-orange-500");
      if (!src_url_equal(img.src, img_src_value = clear))
        attr(img, "src", img_src_value);
      attr(img, "alt", "");
      attr(img, "class", "w-3 dark:invert");
      attr(button, "class", "absolute right-6 top-5 md:top-6");
      attr(h2, "class", "text-lg mb-4 font-semibold");
      attr(div1, "class", "p-6");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, h2);
      append(h2, t0);
      append(h2, span0);
      append(span0, t1);
      append(h2, t2);
      append(h2, button);
      append(button, img);
      append(div1, t3);
      append(div1, div0);
      if (!mounted) {
        dispose = listen(button, "click", ctx[18]);
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty[0] & 1)
        set_data(t1, ctx2[0]);
    },
    i: noop,
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div1);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block$5(ctx) {
  let div1;
  let h2;
  let img0;
  let img0_src_value;
  let t0;
  let div0;
  let t1;
  let t2;
  let button;
  let img1;
  let img1_src_value;
  let t3;
  let t4;
  let div2;
  let current;
  let mounted;
  let dispose;
  let if_block = ctx[10] > 1 && create_if_block_9(ctx);
  let each_value = ctx[2];
  let each_blocks = [];
  for (let i2 = 0; i2 < each_value.length; i2 += 1) {
    each_blocks[i2] = create_each_block(get_each_context(ctx, each_value, i2));
  }
  const out = (i2) => transition_out(each_blocks[i2], 1, 1, () => {
    each_blocks[i2] = null;
  });
  return {
    c() {
      div1 = element("div");
      h2 = element("h2");
      img0 = element("img");
      t0 = text("\n			API documentation for\xA0\n			");
      div0 = element("div");
      t1 = text(ctx[0]);
      t2 = space();
      button = element("button");
      img1 = element("img");
      t3 = space();
      if (if_block)
        if_block.c();
      t4 = space();
      div2 = element("div");
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        each_blocks[i2].c();
      }
      if (!src_url_equal(img0.src, img0_src_value = api_logo))
        attr(img0, "src", img0_src_value);
      attr(img0, "alt", "");
      attr(img0, "class", "w-3.5 md:w-4 mr-1 md:mr-2");
      attr(div0, "class", "text-orange-500");
      if (!src_url_equal(img1.src, img1_src_value = clear))
        attr(img1, "src", img1_src_value);
      attr(img1, "alt", "");
      attr(img1, "class", "w-3 dark:invert");
      attr(button, "class", "absolute right-6 top-5 md:top-6");
      attr(h2, "class", "font-semibold flex");
      attr(div1, "class", "px-6 py-4 border-b border-gray-100 dark:border-gray-900 relative text-sm md:text-lg");
      attr(div2, "class", "flex flex-col gap-6");
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, h2);
      append(h2, img0);
      append(h2, t0);
      append(h2, div0);
      append(div0, t1);
      append(h2, t2);
      append(h2, button);
      append(button, img1);
      append(div1, t3);
      if (if_block)
        if_block.m(div1, null);
      insert(target, t4, anchor);
      insert(target, div2, anchor);
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        each_blocks[i2].m(div2, null);
      }
      current = true;
      if (!mounted) {
        dispose = listen(button, "click", ctx[13]);
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (!current || dirty[0] & 1)
        set_data(t1, ctx2[0]);
      if (ctx2[10] > 1)
        if_block.p(ctx2, dirty);
      if (dirty[0] & 6655) {
        each_value = ctx2[2];
        let i2;
        for (i2 = 0; i2 < each_value.length; i2 += 1) {
          const child_ctx = get_each_context(ctx2, each_value, i2);
          if (each_blocks[i2]) {
            each_blocks[i2].p(child_ctx, dirty);
            transition_in(each_blocks[i2], 1);
          } else {
            each_blocks[i2] = create_each_block(child_ctx);
            each_blocks[i2].c();
            transition_in(each_blocks[i2], 1);
            each_blocks[i2].m(div2, null);
          }
        }
        group_outros();
        for (i2 = each_value.length; i2 < each_blocks.length; i2 += 1) {
          out(i2);
        }
        check_outros();
      }
    },
    i(local) {
      if (current)
        return;
      for (let i2 = 0; i2 < each_value.length; i2 += 1) {
        transition_in(each_blocks[i2]);
      }
      current = true;
    },
    o(local) {
      each_blocks = each_blocks.filter(Boolean);
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        transition_out(each_blocks[i2]);
      }
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      if (if_block)
        if_block.d();
      if (detaching)
        detach(t4);
      if (detaching)
        detach(div2);
      destroy_each(each_blocks, detaching);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block_9(ctx) {
  let div;
  return {
    c() {
      div = element("div");
      div.textContent = `${ctx[10]} API endpoints:`;
    },
    m(target, anchor) {
      insert(target, div, anchor);
    },
    p: noop,
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_if_block_1$4(ctx) {
  let div9;
  let h3;
  let span0;
  let t1;
  let t2_value = ctx[19].api_name + "";
  let t2;
  let t3;
  let div0;
  let t4;
  let span1;
  let t5;
  let t6;
  let t7_value = ctx[19].api_name + "";
  let t7;
  let t8;
  let button0;
  let t9;
  let h40;
  let t11;
  let div3;
  let t12;
  let br0;
  let t13;
  let br1;
  let t14;
  let t15;
  let br2;
  let t16;
  let t17;
  let button1;
  let t19;
  let h41;
  let t21;
  let div7;
  let div6;
  let t22;
  let br3;
  let t23;
  let br4;
  let t24;
  let t25;
  let br5;
  let t26;
  let span2;
  let br6;
  let t28;
  let div6_class_value;
  let t29;
  let t30;
  let h42;
  let t32;
  let div8;
  let t33;
  let code;
  let t34;
  let current;
  let mounted;
  let dispose;
  function select_block_type_1(ctx2, dirty) {
    if (ctx2[4] === ctx2[21])
      return create_if_block_8;
    return create_else_block$1;
  }
  let current_block_type = select_block_type_1(ctx);
  let if_block0 = current_block_type(ctx);
  function click_handler_1() {
    return ctx[14](ctx[19], ctx[21]);
  }
  let each_value_5 = ctx[19].inputs;
  let each_blocks_2 = [];
  for (let i2 = 0; i2 < each_value_5.length; i2 += 1) {
    each_blocks_2[i2] = create_each_block_5(get_each_context_5(ctx, each_value_5, i2));
  }
  let each_value_4 = ctx[19].outputs;
  let each_blocks_1 = [];
  for (let i2 = 0; i2 < each_value_4.length; i2 += 1) {
    each_blocks_1[i2] = create_each_block_4(get_each_context_4(ctx, each_value_4, i2));
  }
  let if_block1 = ctx[5] && create_if_block_5$1();
  let each_value_3 = [["python", python], ["javascript", javascript]];
  let each_blocks = [];
  for (let i2 = 0; i2 < 2; i2 += 1) {
    each_blocks[i2] = create_each_block_3(get_each_context_3(ctx, each_value_3, i2));
  }
  function select_block_type_2(ctx2, dirty) {
    if (ctx2[3] === "python")
      return create_if_block_2$3;
    if (ctx2[3] === "javascript")
      return create_if_block_3$3;
    if (ctx2[3] === "gradio client")
      return create_if_block_4$1;
  }
  let current_block_type_1 = select_block_type_2(ctx);
  let if_block2 = current_block_type_1 && current_block_type_1(ctx);
  return {
    c() {
      div9 = element("div");
      h3 = element("h3");
      span0 = element("span");
      span0.textContent = "POST";
      t1 = text("\n						/run/");
      t2 = text(t2_value);
      t3 = space();
      div0 = element("div");
      t4 = text("Endpoint: ");
      span1 = element("span");
      t5 = text(ctx[0]);
      t6 = text("run/");
      t7 = text(t7_value);
      t8 = space();
      button0 = element("button");
      if_block0.c();
      t9 = space();
      h40 = element("h4");
      h40.innerHTML = `<div class="flex items-center h-1 w-3 bg-gray-300 dark:bg-gray-500 rounded-full mr-2"><div class="rounded-full h-1.5 w-1.5 bg-gray-700 dark:bg-gray-400"></div></div>
						Input Payload`;
      t11 = space();
      div3 = element("div");
      t12 = text("{");
      br0 = element("br");
      t13 = text('\n						\xA0\xA0"data": [');
      br1 = element("br");
      t14 = space();
      for (let i2 = 0; i2 < each_blocks_2.length; i2 += 1) {
        each_blocks_2[i2].c();
      }
      t15 = text("\n						\xA0\xA0]");
      br2 = element("br");
      t16 = text("\n						}");
      t17 = space();
      button1 = element("button");
      button1.textContent = "Try It Out";
      t19 = space();
      h41 = element("h4");
      h41.innerHTML = `<div class="flex items-center h-1 w-3 bg-gray-300 dark:bg-gray-500 rounded-full mr-2"><div class="rounded-full h-1.5 w-1.5 bg-gray-700 dark:bg-gray-400 ml-auto"></div></div>
						Response Object`;
      t21 = space();
      div7 = element("div");
      div6 = element("div");
      t22 = text("{");
      br3 = element("br");
      t23 = text('\n							\xA0\xA0"data": [');
      br4 = element("br");
      t24 = space();
      for (let i2 = 0; i2 < each_blocks_1.length; i2 += 1) {
        each_blocks_1[i2].c();
      }
      t25 = text("\n							\xA0\xA0\xA0\xA0],");
      br5 = element("br");
      t26 = text('\n							\xA0\xA0\xA0\xA0"duration": (float)\n							');
      span2 = element("span");
      span2.textContent = "// number of seconds to run function call";
      br6 = element("br");
      t28 = text("\n							}");
      t29 = space();
      if (if_block1)
        if_block1.c();
      t30 = space();
      h42 = element("h4");
      h42.innerHTML = `<svg width="1em" height="1em" viewBox="0 0 24 24" class="mr-1.5"><path fill="currentColor" d="m8 18l-6-6l6-6l1.425 1.425l-4.6 4.6L9.4 16.6Zm8 0l-1.425-1.425l4.6-4.6L14.6 7.4L16 6l6 6Z"></path></svg>
						Code snippets`;
      t32 = space();
      div8 = element("div");
      for (let i2 = 0; i2 < 2; i2 += 1) {
        each_blocks[i2].c();
      }
      t33 = space();
      code = element("code");
      if (if_block2)
        if_block2.c();
      t34 = space();
      attr(span0, "class", "bg-orange-100 px-1 rounded text-sm border border-orange-200 mr-2 font-semibold text-orange-600 dark:bg-orange-400 dark:text-orange-900 dark:border-orange-600");
      attr(h3, "class", "text-lg font-bold mb-1.5");
      attr(span1, "class", "underline");
      attr(button0, "class", "gr-button ml-2 !py-0");
      attr(div0, "class", "text-sm md:text-base mb-6 text-gray-500");
      attr(h40, "class", "font-bold mt-6 mb-3 flex items-center");
      attr(div3, "class", "block bg-white border dark:bg-gray-800 p-4 font-mono text-sm rounded-lg");
      attr(button1, "class", "gr-button gr-button-lg gr-button-primary w-full mt-4");
      attr(h41, "class", "font-bold mt-6 mb-3 flex items-center");
      attr(span2, "class", "text-gray-400");
      attr(div6, "class", div6_class_value = ctx[5] ? "hidden" : "");
      attr(div7, "class", "bg-white border dark:bg-gray-800 p-4 font-mono text-sm rounded-lg flex flex-col");
      attr(h42, "class", "font-bold mt-8 mb-3 flex items-center");
      attr(div8, "class", "flex space-x-2 items-center mb-3");
      attr(code, "class", "bg-white border dark:bg-gray-800 p-4 font-mono text-sm rounded-lg flex flex-col overflow-x-auto");
      attr(div9, "class", "bg-gradient-to-b dark:from-orange-200/5 from-orange-200/20 via-transparent to-transparent p-6 rounded");
    },
    m(target, anchor) {
      insert(target, div9, anchor);
      append(div9, h3);
      append(h3, span0);
      append(h3, t1);
      append(h3, t2);
      append(div9, t3);
      append(div9, div0);
      append(div0, t4);
      append(div0, span1);
      append(span1, t5);
      append(span1, t6);
      append(span1, t7);
      append(div0, t8);
      append(div0, button0);
      if_block0.m(button0, null);
      append(div9, t9);
      append(div9, h40);
      append(div9, t11);
      append(div9, div3);
      append(div3, t12);
      append(div3, br0);
      append(div3, t13);
      append(div3, br1);
      append(div3, t14);
      for (let i2 = 0; i2 < each_blocks_2.length; i2 += 1) {
        each_blocks_2[i2].m(div3, null);
      }
      append(div3, t15);
      append(div3, br2);
      append(div3, t16);
      append(div9, t17);
      append(div9, button1);
      append(div9, t19);
      append(div9, h41);
      append(div9, t21);
      append(div9, div7);
      append(div7, div6);
      append(div6, t22);
      append(div6, br3);
      append(div6, t23);
      append(div6, br4);
      append(div6, t24);
      for (let i2 = 0; i2 < each_blocks_1.length; i2 += 1) {
        each_blocks_1[i2].m(div6, null);
      }
      append(div6, t25);
      append(div6, br5);
      append(div6, t26);
      append(div6, span2);
      append(div6, br6);
      append(div6, t28);
      append(div7, t29);
      if (if_block1)
        if_block1.m(div7, null);
      append(div9, t30);
      append(div9, h42);
      append(div9, t32);
      append(div9, div8);
      for (let i2 = 0; i2 < 2; i2 += 1) {
        each_blocks[i2].m(div8, null);
      }
      append(div9, t33);
      append(div9, code);
      if (if_block2)
        if_block2.m(code, null);
      append(div9, t34);
      current = true;
      if (!mounted) {
        dispose = [
          listen(button0, "click", click_handler_1),
          listen(button1, "click", ctx[11].bind(null, ctx[21]))
        ];
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if ((!current || dirty[0] & 4) && t2_value !== (t2_value = ctx[19].api_name + ""))
        set_data(t2, t2_value);
      if (!current || dirty[0] & 1)
        set_data(t5, ctx[0]);
      if ((!current || dirty[0] & 4) && t7_value !== (t7_value = ctx[19].api_name + ""))
        set_data(t7, t7_value);
      if (current_block_type !== (current_block_type = select_block_type_1(ctx))) {
        if_block0.d(1);
        if_block0 = current_block_type(ctx);
        if (if_block0) {
          if_block0.c();
          if_block0.m(button0, null);
        }
      }
      if (dirty[0] & 326) {
        each_value_5 = ctx[19].inputs;
        let i2;
        for (i2 = 0; i2 < each_value_5.length; i2 += 1) {
          const child_ctx = get_each_context_5(ctx, each_value_5, i2);
          if (each_blocks_2[i2]) {
            each_blocks_2[i2].p(child_ctx, dirty);
          } else {
            each_blocks_2[i2] = create_each_block_5(child_ctx);
            each_blocks_2[i2].c();
            each_blocks_2[i2].m(div3, t15);
          }
        }
        for (; i2 < each_blocks_2.length; i2 += 1) {
          each_blocks_2[i2].d(1);
        }
        each_blocks_2.length = each_value_5.length;
      }
      if (dirty[0] & 134) {
        each_value_4 = ctx[19].outputs;
        let i2;
        for (i2 = 0; i2 < each_value_4.length; i2 += 1) {
          const child_ctx = get_each_context_4(ctx, each_value_4, i2);
          if (each_blocks_1[i2]) {
            each_blocks_1[i2].p(child_ctx, dirty);
          } else {
            each_blocks_1[i2] = create_each_block_4(child_ctx);
            each_blocks_1[i2].c();
            each_blocks_1[i2].m(div6, t25);
          }
        }
        for (; i2 < each_blocks_1.length; i2 += 1) {
          each_blocks_1[i2].d(1);
        }
        each_blocks_1.length = each_value_4.length;
      }
      if (!current || dirty[0] & 32 && div6_class_value !== (div6_class_value = ctx[5] ? "hidden" : "")) {
        attr(div6, "class", div6_class_value);
      }
      if (ctx[5]) {
        if (if_block1) {
          if (dirty[0] & 32) {
            transition_in(if_block1, 1);
          }
        } else {
          if_block1 = create_if_block_5$1();
          if_block1.c();
          transition_in(if_block1, 1);
          if_block1.m(div7, null);
        }
      } else if (if_block1) {
        group_outros();
        transition_out(if_block1, 1, 1, () => {
          if_block1 = null;
        });
        check_outros();
      }
      if (dirty[0] & 8) {
        each_value_3 = [["python", python], ["javascript", javascript]];
        let i2;
        for (i2 = 0; i2 < 2; i2 += 1) {
          const child_ctx = get_each_context_3(ctx, each_value_3, i2);
          if (each_blocks[i2]) {
            each_blocks[i2].p(child_ctx, dirty);
          } else {
            each_blocks[i2] = create_each_block_3(child_ctx);
            each_blocks[i2].c();
            each_blocks[i2].m(div8, null);
          }
        }
        for (; i2 < 2; i2 += 1) {
          each_blocks[i2].d(1);
        }
      }
      if (current_block_type_1 === (current_block_type_1 = select_block_type_2(ctx)) && if_block2) {
        if_block2.p(ctx, dirty);
      } else {
        if (if_block2)
          if_block2.d(1);
        if_block2 = current_block_type_1 && current_block_type_1(ctx);
        if (if_block2) {
          if_block2.c();
          if_block2.m(code, null);
        }
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block1);
      current = true;
    },
    o(local) {
      transition_out(if_block1);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div9);
      if_block0.d();
      destroy_each(each_blocks_2, detaching);
      destroy_each(each_blocks_1, detaching);
      if (if_block1)
        if_block1.d();
      destroy_each(each_blocks, detaching);
      if (if_block2) {
        if_block2.d();
      }
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_else_block$1(ctx) {
  let t;
  return {
    c() {
      t = text("copy");
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_if_block_8(ctx) {
  let t;
  return {
    c() {
      t = text("copied!");
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_if_block_7$1(ctx) {
  let span;
  return {
    c() {
      span = element("span");
      span.textContent = "ERROR";
      attr(span, "class", "text-red-600");
    },
    m(target, anchor) {
      insert(target, span, anchor);
    },
    d(detaching) {
      if (detaching)
        detach(span);
    }
  };
}
function create_each_block_5(ctx) {
  var _a2, _b;
  let t0;
  let input;
  let t1;
  let t2;
  let span0;
  let t3;
  let t4_value = ((_a2 = ctx[1][ctx[30]].documentation) == null ? void 0 : _a2.type) + "";
  let t4;
  let t5;
  let t6;
  let span2;
  let t7;
  let t8_value = ((_b = ctx[1][ctx[30]].documentation) == null ? void 0 : _b.description) + "";
  let t8;
  let t9;
  let t10_value = func(ctx[1][ctx[30]].props.label) + "";
  let t10;
  let t11;
  let span1;
  let t12_value = ctx[1][ctx[30]].props.name + "";
  let t12;
  let t13;
  let t14;
  let br;
  let mounted;
  let dispose;
  function input_input_handler() {
    ctx[15].call(input, ctx[21], ctx[24]);
  }
  let if_block = ctx[8][ctx[21]][ctx[24]] && create_if_block_7$1();
  return {
    c() {
      t0 = text("\xA0\xA0\xA0\xA0");
      input = element("input");
      t1 = space();
      if (if_block)
        if_block.c();
      t2 = space();
      span0 = element("span");
      t3 = text(": ");
      t4 = text(t4_value);
      t5 = text(",");
      t6 = space();
      span2 = element("span");
      t7 = text("// represents ");
      t8 = text(t8_value);
      t9 = text(" of\n								");
      t10 = text(t10_value);
      t11 = space();
      span1 = element("span");
      t12 = text(t12_value);
      t13 = text(" component");
      t14 = space();
      br = element("br");
      attr(input, "class", "bg-gray-100 dark:bg-gray-600 border-none w-40 px-1 py-0.5 my-0.5 text-sm rounded ring-1 ring-gray-300 dark:ring-gray-500");
      attr(input, "type", "text");
      attr(span0, "class", "text-gray-500");
      attr(span1, "class", "capitalize");
      attr(span2, "class", "text-gray-400");
    },
    m(target, anchor) {
      insert(target, t0, anchor);
      insert(target, input, anchor);
      set_input_value(input, ctx[6][ctx[21]][ctx[24]]);
      insert(target, t1, anchor);
      if (if_block)
        if_block.m(target, anchor);
      insert(target, t2, anchor);
      insert(target, span0, anchor);
      append(span0, t3);
      append(span0, t4);
      append(span0, t5);
      insert(target, t6, anchor);
      insert(target, span2, anchor);
      append(span2, t7);
      append(span2, t8);
      append(span2, t9);
      append(span2, t10);
      append(span2, t11);
      append(span2, span1);
      append(span1, t12);
      append(span2, t13);
      insert(target, t14, anchor);
      insert(target, br, anchor);
      if (!mounted) {
        dispose = listen(input, "input", input_input_handler);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      var _a3, _b2;
      ctx = new_ctx;
      if (dirty[0] & 64 && input.value !== ctx[6][ctx[21]][ctx[24]]) {
        set_input_value(input, ctx[6][ctx[21]][ctx[24]]);
      }
      if (ctx[8][ctx[21]][ctx[24]]) {
        if (if_block)
          ;
        else {
          if_block = create_if_block_7$1();
          if_block.c();
          if_block.m(t2.parentNode, t2);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty[0] & 6 && t4_value !== (t4_value = ((_a3 = ctx[1][ctx[30]].documentation) == null ? void 0 : _a3.type) + ""))
        set_data(t4, t4_value);
      if (dirty[0] & 6 && t8_value !== (t8_value = ((_b2 = ctx[1][ctx[30]].documentation) == null ? void 0 : _b2.description) + ""))
        set_data(t8, t8_value);
      if (dirty[0] & 6 && t10_value !== (t10_value = func(ctx[1][ctx[30]].props.label) + ""))
        set_data(t10, t10_value);
      if (dirty[0] & 6 && t12_value !== (t12_value = ctx[1][ctx[30]].props.name + ""))
        set_data(t12, t12_value);
    },
    d(detaching) {
      if (detaching)
        detach(t0);
      if (detaching)
        detach(input);
      if (detaching)
        detach(t1);
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(t2);
      if (detaching)
        detach(span0);
      if (detaching)
        detach(t6);
      if (detaching)
        detach(span2);
      if (detaching)
        detach(t14);
      if (detaching)
        detach(br);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block_6$1(ctx) {
  let input;
  let t;
  let mounted;
  let dispose;
  function input_input_handler_1() {
    ctx[16].call(input, ctx[21], ctx[24]);
  }
  return {
    c() {
      input = element("input");
      t = text(" :");
      input.disabled = true;
      attr(input, "class", "bg-gray-100 dark:bg-gray-600 border-none w-40 px-1 py-0.5 my-0.5 text-sm rounded ring-1 ring-gray-300 dark:ring-gray-500");
      attr(input, "type", "text");
    },
    m(target, anchor) {
      insert(target, input, anchor);
      set_input_value(input, ctx[7][ctx[21]][ctx[24]]);
      insert(target, t, anchor);
      if (!mounted) {
        dispose = listen(input, "input", input_input_handler_1);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty[0] & 128 && input.value !== ctx[7][ctx[21]][ctx[24]]) {
        set_input_value(input, ctx[7][ctx[21]][ctx[24]]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(input);
      if (detaching)
        detach(t);
      mounted = false;
      dispose();
    }
  };
}
function create_each_block_4(ctx) {
  var _a2, _b;
  let t0;
  let t1;
  let span0;
  let t2_value = ((_a2 = ctx[1][ctx[30]].documentation) == null ? void 0 : _a2.type) + "";
  let t2;
  let t3;
  let t4;
  let span2;
  let t5;
  let t6_value = ((_b = ctx[1][ctx[30]].documentation) == null ? void 0 : _b.description) + "";
  let t6;
  let t7;
  let t8_value = func_1(ctx[1][ctx[30]].props.label) + "";
  let t8;
  let t9;
  let span1;
  let t10_value = ctx[1][ctx[30]].props.name + "";
  let t10;
  let t11;
  let t12;
  let br;
  let if_block = ctx[7][ctx[21]][ctx[24]] !== void 0 && create_if_block_6$1(ctx);
  return {
    c() {
      t0 = text("\xA0\xA0\xA0\xA0");
      if (if_block)
        if_block.c();
      t1 = space();
      span0 = element("span");
      t2 = text(t2_value);
      t3 = text(",");
      t4 = space();
      span2 = element("span");
      t5 = text("// represents ");
      t6 = text(t6_value);
      t7 = text(" of\n									");
      t8 = text(t8_value);
      t9 = space();
      span1 = element("span");
      t10 = text(t10_value);
      t11 = text(" component");
      t12 = space();
      br = element("br");
      attr(span0, "class", "text-gray-500");
      attr(span1, "class", "capitalize");
      attr(span2, "class", "text-gray-400");
    },
    m(target, anchor) {
      insert(target, t0, anchor);
      if (if_block)
        if_block.m(target, anchor);
      insert(target, t1, anchor);
      insert(target, span0, anchor);
      append(span0, t2);
      append(span0, t3);
      insert(target, t4, anchor);
      insert(target, span2, anchor);
      append(span2, t5);
      append(span2, t6);
      append(span2, t7);
      append(span2, t8);
      append(span2, t9);
      append(span2, span1);
      append(span1, t10);
      append(span2, t11);
      insert(target, t12, anchor);
      insert(target, br, anchor);
    },
    p(ctx2, dirty) {
      var _a3, _b2;
      if (ctx2[7][ctx2[21]][ctx2[24]] !== void 0) {
        if (if_block) {
          if_block.p(ctx2, dirty);
        } else {
          if_block = create_if_block_6$1(ctx2);
          if_block.c();
          if_block.m(t1.parentNode, t1);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
      if (dirty[0] & 6 && t2_value !== (t2_value = ((_a3 = ctx2[1][ctx2[30]].documentation) == null ? void 0 : _a3.type) + ""))
        set_data(t2, t2_value);
      if (dirty[0] & 6 && t6_value !== (t6_value = ((_b2 = ctx2[1][ctx2[30]].documentation) == null ? void 0 : _b2.description) + ""))
        set_data(t6, t6_value);
      if (dirty[0] & 6 && t8_value !== (t8_value = func_1(ctx2[1][ctx2[30]].props.label) + ""))
        set_data(t8, t8_value);
      if (dirty[0] & 6 && t10_value !== (t10_value = ctx2[1][ctx2[30]].props.name + ""))
        set_data(t10, t10_value);
    },
    d(detaching) {
      if (detaching)
        detach(t0);
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(t1);
      if (detaching)
        detach(span0);
      if (detaching)
        detach(t4);
      if (detaching)
        detach(span2);
      if (detaching)
        detach(t12);
      if (detaching)
        detach(br);
    }
  };
}
function create_if_block_5$1(ctx) {
  let div;
  let loader;
  let current;
  loader = new Loader({ props: { margin: false } });
  return {
    c() {
      div = element("div");
      create_component(loader.$$.fragment);
      attr(div, "class", "self-center justify-self-center");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      mount_component(loader, div, null);
      current = true;
    },
    i(local) {
      if (current)
        return;
      transition_in(loader.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(loader.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div);
      destroy_component(loader);
    }
  };
}
function create_each_block_3(ctx) {
  let li;
  let img;
  let img_src_value;
  let t0;
  let t1_value = ctx[26] + "";
  let t1;
  let t2;
  let li_class_value;
  let mounted;
  let dispose;
  function click_handler_2() {
    return ctx[17](ctx[26]);
  }
  return {
    c() {
      li = element("li");
      img = element("img");
      t0 = space();
      t1 = text(t1_value);
      t2 = space();
      if (!src_url_equal(img.src, img_src_value = ctx[27]))
        attr(img, "src", img_src_value);
      attr(img, "class", "mr-1.5 w-3");
      attr(img, "alt", "");
      attr(li, "class", li_class_value = "flex items-center border rounded-lg px-1.5 py-1 leading-none select-none text-smd capitalize " + (ctx[3] === ctx[26] ? "border-gray-400 text-gray-800 dark:bg-gray-700" : "text-gray-400 cursor-pointer hover:text-gray-700 dark:hover:text-gray-200 hover:shadow-sm"));
    },
    m(target, anchor) {
      insert(target, li, anchor);
      append(li, img);
      append(li, t0);
      append(li, t1);
      append(li, t2);
      if (!mounted) {
        dispose = listen(li, "click", click_handler_2);
        mounted = true;
      }
    },
    p(new_ctx, dirty) {
      ctx = new_ctx;
      if (dirty[0] & 8 && li_class_value !== (li_class_value = "flex items-center border rounded-lg px-1.5 py-1 leading-none select-none text-smd capitalize " + (ctx[3] === ctx[26] ? "border-gray-400 text-gray-800 dark:bg-gray-700" : "text-gray-400 cursor-pointer hover:text-gray-700 dark:hover:text-gray-200 hover:shadow-sm"))) {
        attr(li, "class", li_class_value);
      }
    },
    d(detaching) {
      if (detaching)
        detach(li);
      mounted = false;
      dispose();
    }
  };
}
function create_if_block_4$1(ctx) {
  let pre;
  return {
    c() {
      pre = element("pre");
      pre.textContent = "Hello World";
      attr(pre, "class", "break-words whitespace-pre-wrap");
    },
    m(target, anchor) {
      insert(target, pre, anchor);
    },
    p: noop,
    d(detaching) {
      if (detaching)
        detach(pre);
    }
  };
}
function create_if_block_3$3(ctx) {
  let pre;
  let t0;
  let t1_value = ctx[0] + "run/" + ctx[19].api_name;
  let t1;
  let t2;
  let t3;
  let each_value_2 = ctx[6][ctx[21]];
  let each_blocks = [];
  for (let i2 = 0; i2 < each_value_2.length; i2 += 1) {
    each_blocks[i2] = create_each_block_2(get_each_context_2(ctx, each_value_2, i2));
  }
  return {
    c() {
      pre = element("pre");
      t0 = text('fetch("');
      t1 = text(t1_value);
      t2 = text('", {\n  method: "POST",\n  headers: { "Content-Type": "application/json" },\n  body: JSON.stringify({\n    data: [');
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        each_blocks[i2].c();
      }
      t3 = text("\n	]\n  })})\n.then(r => r.json())\n.then(\n  r => {\n    let data = r.data;\n  }\n)");
    },
    m(target, anchor) {
      insert(target, pre, anchor);
      append(pre, t0);
      append(pre, t1);
      append(pre, t2);
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        each_blocks[i2].m(pre, null);
      }
      append(pre, t3);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 5 && t1_value !== (t1_value = ctx2[0] + "run/" + ctx2[19].api_name))
        set_data(t1, t1_value);
      if (dirty[0] & 4166) {
        each_value_2 = ctx2[6][ctx2[21]];
        let i2;
        for (i2 = 0; i2 < each_value_2.length; i2 += 1) {
          const child_ctx = get_each_context_2(ctx2, each_value_2, i2);
          if (each_blocks[i2]) {
            each_blocks[i2].p(child_ctx, dirty);
          } else {
            each_blocks[i2] = create_each_block_2(child_ctx);
            each_blocks[i2].c();
            each_blocks[i2].m(pre, t3);
          }
        }
        for (; i2 < each_blocks.length; i2 += 1) {
          each_blocks[i2].d(1);
        }
        each_blocks.length = each_value_2.length;
      }
    },
    d(detaching) {
      if (detaching)
        detach(pre);
      destroy_each(each_blocks, detaching);
    }
  };
}
function create_if_block_2$3(ctx) {
  let pre;
  let t0;
  let t1_value = ctx[0] + "run/" + ctx[19].api_name;
  let t1;
  let t2;
  let t3;
  let each_value_1 = ctx[6][ctx[21]];
  let each_blocks = [];
  for (let i2 = 0; i2 < each_value_1.length; i2 += 1) {
    each_blocks[i2] = create_each_block_1(get_each_context_1(ctx, each_value_1, i2));
  }
  return {
    c() {
      pre = element("pre");
      t0 = text('import requests\n\nresponse = requests.post("');
      t1 = text(t1_value);
      t2 = text('", json={\n  "data": [');
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        each_blocks[i2].c();
      }
      t3 = text('\n]}).json()\n\ndata = response["data"]');
    },
    m(target, anchor) {
      insert(target, pre, anchor);
      append(pre, t0);
      append(pre, t1);
      append(pre, t2);
      for (let i2 = 0; i2 < each_blocks.length; i2 += 1) {
        each_blocks[i2].m(pre, null);
      }
      append(pre, t3);
    },
    p(ctx2, dirty) {
      if (dirty[0] & 5 && t1_value !== (t1_value = ctx2[0] + "run/" + ctx2[19].api_name))
        set_data(t1, t1_value);
      if (dirty[0] & 4166) {
        each_value_1 = ctx2[6][ctx2[21]];
        let i2;
        for (i2 = 0; i2 < each_value_1.length; i2 += 1) {
          const child_ctx = get_each_context_1(ctx2, each_value_1, i2);
          if (each_blocks[i2]) {
            each_blocks[i2].p(child_ctx, dirty);
          } else {
            each_blocks[i2] = create_each_block_1(child_ctx);
            each_blocks[i2].c();
            each_blocks[i2].m(pre, t3);
          }
        }
        for (; i2 < each_blocks.length; i2 += 1) {
          each_blocks[i2].d(1);
        }
        each_blocks.length = each_value_1.length;
      }
    },
    d(detaching) {
      if (detaching)
        detach(pre);
      destroy_each(each_blocks, detaching);
    }
  };
}
function create_each_block_2(ctx) {
  var _a2;
  let br;
  let t0;
  let t1_value = ctx[12](ctx[22], (_a2 = ctx[1][ctx[2][ctx[21]].inputs[ctx[24]]].documentation) == null ? void 0 : _a2.type, "js") + "";
  let t1;
  let t2;
  return {
    c() {
      br = element("br");
      t0 = text("      ");
      t1 = text(t1_value);
      t2 = text(",");
    },
    m(target, anchor) {
      insert(target, br, anchor);
      insert(target, t0, anchor);
      insert(target, t1, anchor);
      insert(target, t2, anchor);
    },
    p(ctx2, dirty) {
      var _a3;
      if (dirty[0] & 70 && t1_value !== (t1_value = ctx2[12](ctx2[22], (_a3 = ctx2[1][ctx2[2][ctx2[21]].inputs[ctx2[24]]].documentation) == null ? void 0 : _a3.type, "js") + ""))
        set_data(t1, t1_value);
    },
    d(detaching) {
      if (detaching)
        detach(br);
      if (detaching)
        detach(t0);
      if (detaching)
        detach(t1);
      if (detaching)
        detach(t2);
    }
  };
}
function create_each_block_1(ctx) {
  var _a2;
  let br;
  let t0;
  let t1_value = ctx[12](ctx[22], (_a2 = ctx[1][ctx[2][ctx[21]].inputs[ctx[24]]].documentation) == null ? void 0 : _a2.type, "py") + "";
  let t1;
  let t2;
  return {
    c() {
      br = element("br");
      t0 = text("    ");
      t1 = text(t1_value);
      t2 = text(",");
    },
    m(target, anchor) {
      insert(target, br, anchor);
      insert(target, t0, anchor);
      insert(target, t1, anchor);
      insert(target, t2, anchor);
    },
    p(ctx2, dirty) {
      var _a3;
      if (dirty[0] & 70 && t1_value !== (t1_value = ctx2[12](ctx2[22], (_a3 = ctx2[1][ctx2[2][ctx2[21]].inputs[ctx2[24]]].documentation) == null ? void 0 : _a3.type, "py") + ""))
        set_data(t1, t1_value);
    },
    d(detaching) {
      if (detaching)
        detach(br);
      if (detaching)
        detach(t0);
      if (detaching)
        detach(t1);
      if (detaching)
        detach(t2);
    }
  };
}
function create_each_block(ctx) {
  let if_block_anchor;
  let current;
  let if_block = ctx[19].api_name && create_if_block_1$4(ctx);
  return {
    c() {
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if (if_block)
        if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[19].api_name) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty[0] & 4) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block_1$4(ctx2);
          if_block.c();
          transition_in(if_block, 1);
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        }
      } else if (if_block) {
        group_outros();
        transition_out(if_block, 1, 1, () => {
          if_block = null;
        });
        check_outros();
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
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_fragment$8(ctx) {
  let current_block_type_index;
  let if_block;
  let if_block_anchor;
  let current;
  const if_block_creators = [create_if_block$5, create_else_block_1];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[10])
      return 0;
    return 1;
  }
  current_block_type_index = select_block_type(ctx);
  if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  return {
    c() {
      if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if_blocks[current_block_type_index].m(target, anchor);
      insert(target, if_block_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if_block.p(ctx2, dirty);
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
      if_blocks[current_block_type_index].d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
const func = (label) => {
  return label ? "'" + label + "'" : "the";
};
const func_1 = (label) => {
  return label ? "'" + label + "'" : "the";
};
function instance$8($$self, $$props, $$invalidate) {
  const dispatch2 = createEventDispatcher();
  onMount(() => {
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = "auto";
    };
  });
  let { instance_map } = $$props;
  let { dependencies } = $$props;
  let { root } = $$props;
  if (root === "") {
    root = location.protocol + "//" + location.host + location.pathname;
  }
  if (!root.endsWith("/")) {
    root += "/";
  }
  let current_language = "python";
  let just_copied = -1;
  let isRunning = false;
  let dependency_inputs = dependencies.map((dependency) => dependency.inputs.map((_id) => {
    var _a2;
    let default_data = (_a2 = instance_map[_id].documentation) == null ? void 0 : _a2.example_data;
    if (default_data === void 0) {
      default_data = "";
    } else if (typeof default_data === "object") {
      default_data = JSON.stringify(default_data);
    }
    return default_data;
  }));
  let dependency_outputs = dependencies.map((dependency) => new Array(dependency.outputs.length));
  let dependency_failures = dependencies.map((dependency) => new Array(dependency.inputs.length).fill(false));
  let active_api_count = dependencies.filter((d2) => d2.api_name).length;
  const run2 = async (index) => {
    $$invalidate(5, isRunning = true);
    let dependency = dependencies[index];
    let attempted_component_index = 0;
    try {
      var inputs = dependency_inputs[index].map((input_val, i2) => {
        var _a2;
        attempted_component_index = i2;
        let component = instance_map[dependency.inputs[i2]];
        input_val = represent_value(input_val, (_a2 = component.documentation) == null ? void 0 : _a2.type);
        $$invalidate(8, dependency_failures[index][attempted_component_index] = false, dependency_failures);
        return input_val;
      });
    } catch (err) {
      $$invalidate(8, dependency_failures[index][attempted_component_index] = true, dependency_failures);
      $$invalidate(5, isRunning = false);
      return;
    }
    let [response, status_code] = await post_data(`${root}run/${dependency.api_name}`, { data: inputs });
    $$invalidate(5, isRunning = false);
    if (status_code == 200) {
      $$invalidate(7, dependency_outputs[index] = response.data.map((output_val, i2) => {
        var _a2, _b, _c;
        let component = instance_map[dependency.outputs[i2]];
        console.log((_a2 = component.documentation) == null ? void 0 : _a2.type, output_val, represent_value(output_val, (_b = component.documentation) == null ? void 0 : _b.type, "js"));
        return represent_value(output_val, (_c = component.documentation) == null ? void 0 : _c.type, "js");
      }), dependency_outputs);
    } else {
      $$invalidate(8, dependency_failures[index] = new Array(dependency_failures[index].length).fill(true), dependency_failures);
    }
  };
  const represent_value = (value, type, lang = null) => {
    if (type === void 0) {
      return lang === "py" ? "None" : null;
    }
    if (type === "string") {
      return lang === null ? value : '"' + value + '"';
    } else if (type === "number") {
      return lang === null ? parseFloat(value) : value;
    } else if (type === "boolean") {
      if (lang === "py") {
        return value === "true" ? "True" : "False";
      } else if (lang === "js") {
        return value;
      } else {
        return value === "true";
      }
    } else {
      if (lang === null) {
        return value === "" ? null : JSON.parse(value);
      } else if (typeof value === "string") {
        if (value === "") {
          return lang === "py" ? "None" : "null";
        }
        return value;
      } else {
        return JSON.stringify(value);
      }
    }
  };
  const click_handler = () => dispatch2("close");
  const click_handler_1 = (dependency, dependency_index) => {
    navigator.clipboard.writeText(root + "run/" + dependency.api_name);
    $$invalidate(4, just_copied = dependency_index);
    setTimeout(() => {
      $$invalidate(4, just_copied = -1);
    }, 500);
  };
  function input_input_handler(dependency_index, component_index) {
    dependency_inputs[dependency_index][component_index] = this.value;
    $$invalidate(6, dependency_inputs);
  }
  function input_input_handler_1(dependency_index, component_index) {
    dependency_outputs[dependency_index][component_index] = this.value;
    $$invalidate(7, dependency_outputs);
  }
  const click_handler_2 = (language) => $$invalidate(3, current_language = language);
  const click_handler_3 = () => dispatch2("close");
  $$self.$$set = ($$props2) => {
    if ("instance_map" in $$props2)
      $$invalidate(1, instance_map = $$props2.instance_map);
    if ("dependencies" in $$props2)
      $$invalidate(2, dependencies = $$props2.dependencies);
    if ("root" in $$props2)
      $$invalidate(0, root = $$props2.root);
  };
  return [
    root,
    instance_map,
    dependencies,
    current_language,
    just_copied,
    isRunning,
    dependency_inputs,
    dependency_outputs,
    dependency_failures,
    dispatch2,
    active_api_count,
    run2,
    represent_value,
    click_handler,
    click_handler_1,
    input_input_handler,
    input_input_handler_1,
    click_handler_2,
    click_handler_3
  ];
}
class ApiDocs extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$8, create_fragment$8, safe_not_equal, {
      instance_map: 1,
      dependencies: 2,
      root: 0
    }, null, [-1, -1]);
  }
}
function create_if_block_3$2(ctx) {
  document.title = ctx[2];
  return { c: noop, m: noop, d: noop };
}
function create_if_block_2$2(ctx) {
  let script;
  let script_src_value;
  return {
    c() {
      script = element("script");
      script.async = true;
      script.defer = true;
      if (!src_url_equal(script.src, script_src_value = "https://www.googletagmanager.com/gtag/js?id=UA-156449732-1"))
        attr(script, "src", script_src_value);
    },
    m(target, anchor) {
      insert(target, script, anchor);
    },
    d(detaching) {
      if (detaching)
        detach(script);
    }
  };
}
function create_if_block_1$3(ctx) {
  let render;
  let current;
  render = new Render({
    props: {
      has_modes: ctx[8].has_modes,
      component: ctx[8].component,
      id: ctx[8].id,
      props: ctx[8].props,
      children: ctx[8].children,
      dynamic_ids: ctx[14],
      instance_map: ctx[10],
      root: ctx[0],
      target: ctx[4],
      theme: ctx[7]
    }
  });
  render.$on("mount", ctx[15]);
  render.$on("destroy", ctx[25]);
  return {
    c() {
      create_component(render.$$.fragment);
    },
    m(target, anchor) {
      mount_component(render, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const render_changes = {};
      if (dirty[0] & 256)
        render_changes.has_modes = ctx2[8].has_modes;
      if (dirty[0] & 256)
        render_changes.component = ctx2[8].component;
      if (dirty[0] & 256)
        render_changes.id = ctx2[8].id;
      if (dirty[0] & 256)
        render_changes.props = ctx2[8].props;
      if (dirty[0] & 256)
        render_changes.children = ctx2[8].children;
      if (dirty[0] & 1024)
        render_changes.instance_map = ctx2[10];
      if (dirty[0] & 1)
        render_changes.root = ctx2[0];
      if (dirty[0] & 16)
        render_changes.target = ctx2[4];
      if (dirty[0] & 128)
        render_changes.theme = ctx2[7];
      render.$set(render_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(render.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(render.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(render, detaching);
    }
  };
}
function create_if_block$4(ctx) {
  let div2;
  let div0;
  let t;
  let div1;
  let apidocs;
  let current;
  let mounted;
  let dispose;
  apidocs = new ApiDocs({
    props: {
      instance_map: ctx[10],
      dependencies: ctx[1],
      root: ctx[0]
    }
  });
  apidocs.$on("close", ctx[27]);
  return {
    c() {
      div2 = element("div");
      div0 = element("div");
      t = space();
      div1 = element("div");
      create_component(apidocs.$$.fragment);
      attr(div0, "class", "flex-1 backdrop-blur-sm");
      attr(div1, "class", "md:w-[950px] 2xl:w-[1150px] bg-white md:rounded-l-xl shadow-2xl overflow-hidden overflow-y-auto");
      attr(div2, "class", "h-screen w-screen fixed z-50 bg-black/50 flex top-0");
    },
    m(target, anchor) {
      insert(target, div2, anchor);
      append(div2, div0);
      append(div2, t);
      append(div2, div1);
      mount_component(apidocs, div1, null);
      current = true;
      if (!mounted) {
        dispose = listen(div0, "click", ctx[26]);
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      const apidocs_changes = {};
      if (dirty[0] & 1024)
        apidocs_changes.instance_map = ctx2[10];
      if (dirty[0] & 2)
        apidocs_changes.dependencies = ctx2[1];
      if (dirty[0] & 1)
        apidocs_changes.root = ctx2[0];
      apidocs.$set(apidocs_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(apidocs.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(apidocs.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div2);
      destroy_component(apidocs);
      mounted = false;
      dispose();
    }
  };
}
function create_fragment$7(ctx) {
  let if_block0_anchor;
  let if_block1_anchor;
  let t0;
  let div1;
  let div0;
  let t1;
  let footer;
  let t2;
  let if_block3_anchor;
  let current;
  let if_block0 = ctx[5] && create_if_block_3$2(ctx);
  let if_block1 = ctx[3] && create_if_block_2$2();
  let if_block2 = ctx[11] && create_if_block_1$3(ctx);
  let if_block3 = ctx[9] && ctx[11] && create_if_block$4(ctx);
  return {
    c() {
      if (if_block0)
        if_block0.c();
      if_block0_anchor = empty();
      if (if_block1)
        if_block1.c();
      if_block1_anchor = empty();
      t0 = space();
      div1 = element("div");
      div0 = element("div");
      if (if_block2)
        if_block2.c();
      t1 = space();
      footer = element("footer");
      t2 = space();
      if (if_block3)
        if_block3.c();
      if_block3_anchor = empty();
      attr(div0, "class", "mx-auto container px-4 py-6 dark:bg-gray-950");
      toggle_class(div0, "flex-grow", ctx[6]);
      attr(footer, "class", "flex justify-center pb-6 text-gray-400 space-x-2 text-sm md:text-base");
      attr(div1, "class", "w-full flex flex-col");
      toggle_class(div1, "min-h-screen", ctx[6]);
    },
    m(target, anchor) {
      if (if_block0)
        if_block0.m(document.head, null);
      append(document.head, if_block0_anchor);
      if (if_block1)
        if_block1.m(document.head, null);
      append(document.head, if_block1_anchor);
      insert(target, t0, anchor);
      insert(target, div1, anchor);
      append(div1, div0);
      if (if_block2)
        if_block2.m(div0, null);
      append(div1, t1);
      append(div1, footer);
      insert(target, t2, anchor);
      if (if_block3)
        if_block3.m(target, anchor);
      insert(target, if_block3_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[5]) {
        if (if_block0)
          ;
        else {
          if_block0 = create_if_block_3$2(ctx2);
          if_block0.c();
          if_block0.m(if_block0_anchor.parentNode, if_block0_anchor);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (ctx2[3]) {
        if (if_block1)
          ;
        else {
          if_block1 = create_if_block_2$2();
          if_block1.c();
          if_block1.m(if_block1_anchor.parentNode, if_block1_anchor);
        }
      } else if (if_block1) {
        if_block1.d(1);
        if_block1 = null;
      }
      if (ctx2[11]) {
        if (if_block2) {
          if_block2.p(ctx2, dirty);
          if (dirty[0] & 2048) {
            transition_in(if_block2, 1);
          }
        } else {
          if_block2 = create_if_block_1$3(ctx2);
          if_block2.c();
          transition_in(if_block2, 1);
          if_block2.m(div0, null);
        }
      } else if (if_block2) {
        group_outros();
        transition_out(if_block2, 1, 1, () => {
          if_block2 = null;
        });
        check_outros();
      }
      if (dirty[0] & 64) {
        toggle_class(div0, "flex-grow", ctx2[6]);
      }
      if (dirty[0] & 64) {
        toggle_class(div1, "min-h-screen", ctx2[6]);
      }
      if (ctx2[9] && ctx2[11]) {
        if (if_block3) {
          if_block3.p(ctx2, dirty);
          if (dirty[0] & 2560) {
            transition_in(if_block3, 1);
          }
        } else {
          if_block3 = create_if_block$4(ctx2);
          if_block3.c();
          transition_in(if_block3, 1);
          if_block3.m(if_block3_anchor.parentNode, if_block3_anchor);
        }
      } else if (if_block3) {
        group_outros();
        transition_out(if_block3, 1, 1, () => {
          if_block3 = null;
        });
        check_outros();
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block2);
      transition_in(if_block3);
      current = true;
    },
    o(local) {
      transition_out(if_block2);
      transition_out(if_block3);
      current = false;
    },
    d(detaching) {
      if (if_block0)
        if_block0.d(detaching);
      detach(if_block0_anchor);
      if (if_block1)
        if_block1.d(detaching);
      detach(if_block1_anchor);
      if (detaching)
        detach(t0);
      if (detaching)
        detach(div1);
      if (if_block2)
        if_block2.d();
      if (detaching)
        detach(t2);
      if (if_block3)
        if_block3.d(detaching);
      if (detaching)
        detach(if_block3_anchor);
    }
  };
}
function is_dep(id2, type, deps) {
  let dep_index = 0;
  for (; ; ) {
    const dep = deps[dep_index];
    if (dep === void 0)
      break;
    let dep_item_index = 0;
    for (; ; ) {
      const dep_item = dep[type][dep_item_index];
      if (dep_item === void 0)
        break;
      if (dep_item === id2)
        return true;
      dep_item_index++;
    }
    dep_index++;
  }
  return false;
}
function has_no_default_value(value) {
  return Array.isArray(value) && value.length === 0 || value === "" || value === 0 || !value;
}
function instance$7($$self, $$props, $$invalidate) {
  let $loading_status;
  setupi18n();
  let { root } = $$props;
  let { fn: fn2 } = $$props;
  let { components = [] } = $$props;
  let { layout = { id: 0, children: [] } } = $$props;
  let { dependencies = [] } = $$props;
  let { enable_queue } = $$props;
  let { title = "Gradio" } = $$props;
  let { analytics_enabled = false } = $$props;
  let { target } = $$props;
  let { id: id2 = 0 } = $$props;
  let { autoscroll = false } = $$props;
  let { show_api = true } = $$props;
  let { control_page_title = false } = $$props;
  let { app_mode: app_mode2 } = $$props;
  let { theme } = $$props;
  let loading_status = create_loading_status_store();
  component_subscribe($$self, loading_status, (value) => $$invalidate(24, $loading_status = value));
  let rootNode = {
    id: layout.id,
    type: "column",
    props: {},
    has_modes: false,
    instance: {},
    component: {}
  };
  components.push(rootNode);
  const AsyncFunction = Object.getPrototypeOf(async function() {
  }).constructor;
  dependencies.forEach((d2) => {
    if (d2.js) {
      try {
        d2.frontend_fn = new AsyncFunction("__fn_args", `let result = await (${d2.js})(...__fn_args);
					return ${d2.outputs.length} === 1 ? [result] : result;`);
      } catch (e) {
        console.error("Could not parse custom js method.");
        console.error(e);
      }
    }
  });
  let params = new URLSearchParams(window.location.search);
  let api_docs_visible = params.get("view") === "api";
  const set_api_docs_visible = (visible) => {
    $$invalidate(9, api_docs_visible = visible);
    let params2 = new URLSearchParams(window.location.search);
    if (visible) {
      params2.set("view", "api");
    } else {
      params2.delete("view");
    }
    history.replaceState(null, "", "?" + params2.toString());
  };
  const dynamic_ids = components.reduce((acc, { id: id3, props }) => {
    const is_input = is_dep(id3, "inputs", dependencies);
    const is_output = is_dep(id3, "outputs", dependencies);
    if (!is_input && !is_output && has_no_default_value(props == null ? void 0 : props.value))
      acc.add(id3);
    if (is_input)
      acc.add(id3);
    return acc;
  }, /* @__PURE__ */ new Set());
  let instance_map = components.reduce((acc, next) => {
    acc[next.id] = next;
    return acc;
  }, {});
  function load_component(name) {
    return new Promise(async (res, rej) => {
      try {
        const c2 = await component_map[name]();
        res({ name, component: c2 });
      } catch (e) {
        console.error("failed to load: " + name);
        console.error(e);
        rej(e);
      }
    });
  }
  const component_set = /* @__PURE__ */ new Set();
  const _component_map = /* @__PURE__ */ new Map();
  async function walk_layout(node) {
    let instance2 = instance_map[node.id];
    const _component = (await _component_map.get(instance2.type)).component;
    instance2.component = _component.Component;
    if (_component.document) {
      instance2.documentation = _component.document(instance2.props);
    }
    if (_component.modes && _component.modes.length > 1) {
      instance2.has_modes = true;
    }
    if (node.children) {
      instance2.children = node.children.map((v2) => instance_map[v2.id]);
      await Promise.all(node.children.map((v2) => walk_layout(v2)));
    }
  }
  components.forEach(async (c2) => {
    const _c = load_component(c2.type);
    component_set.add(_c);
    _component_map.set(c2.type, _c);
  });
  let ready = false;
  Promise.all(Array.from(component_set)).then(() => {
    walk_layout(layout).then(async () => {
      $$invalidate(11, ready = true);
      await tick();
      window.__gradio_loader__[id2].$set({ status: "complete" });
    }).catch((e) => {
      console.error(e);
      window.__gradio_loader__[id2].$set({ status: "error" });
    });
  });
  function set_prop(obj, prop, val) {
    if (!(obj == null ? void 0 : obj.props)) {
      obj.props = {};
    }
    obj.props[prop] = val;
    $$invalidate(8, rootNode);
  }
  let handled_dependencies = [];
  async function handle_mount() {
    await tick();
    var a2 = target.getElementsByTagName("a");
    for (var i2 = 0; i2 < a2.length; i2++) {
      const _target = a2[i2].getAttribute("target");
      if (_target !== "_blank")
        a2[i2].setAttribute("target", "_blank");
    }
    dependencies.forEach((_a2, i3) => {
      var _b = _a2, { targets, trigger, inputs, outputs, queue, backend_fn, frontend_fn, cancels } = _b, rest = __objRest(_b, ["targets", "trigger", "inputs", "outputs", "queue", "backend_fn", "frontend_fn", "cancels"]);
      var _a3;
      const target_instances = targets.map((t) => [t, instance_map[t]]);
      if (targets.length === 0 && !((_a3 = handled_dependencies[i3]) == null ? void 0 : _a3.includes(-1)) && trigger === "load" && outputs.every((v2) => instance_map == null ? void 0 : instance_map[v2].instance) && inputs.every((v2) => instance_map == null ? void 0 : instance_map[v2].instance)) {
        let handle_update = function(output) {
          output.data.forEach((value, i4) => {
            if (typeof value === "object" && value !== null && value.__type__ === "update") {
              for (const [update_key, update_value] of Object.entries(value)) {
                if (update_key === "__type__") {
                  continue;
                } else {
                  $$invalidate(10, instance_map[outputs[i4]].props[update_key] = update_value, instance_map);
                }
              }
              $$invalidate(8, rootNode);
            } else {
              $$invalidate(10, instance_map[outputs[i4]].props.value = value, instance_map);
            }
          });
        };
        const req = fn2({
          action: "predict",
          backend_fn,
          frontend_fn,
          payload: {
            fn_index: i3,
            data: inputs.map((id3) => instance_map[id3].props.value)
          },
          queue: queue === null ? enable_queue : queue,
          queue_callback: handle_update,
          loading_status,
          cancels
        });
        if (!(queue === null ? enable_queue : queue)) {
          req.then(handle_update);
        }
        handled_dependencies[i3] = [-1];
      }
      target_instances.filter((v2) => !!v2 && !!v2[1]).forEach(([id3, { instance: instance2 }]) => {
        var _a4;
        if (((_a4 = handled_dependencies[i3]) == null ? void 0 : _a4.includes(id3)) || !instance2)
          return;
        instance2 == null ? void 0 : instance2.$on(trigger, () => {
          if (loading_status.get_status_for_fn(i3) === "pending") {
            return;
          }
          console.log("trigger", instance2, trigger);
          const req = fn2({
            action: "predict",
            backend_fn,
            frontend_fn,
            payload: {
              fn_index: i3,
              data: inputs.map((id4) => instance_map[id4].props.value)
            },
            output_data: outputs.map((id4) => instance_map[id4].props.value),
            queue: queue === null ? enable_queue : queue,
            queue_callback: handle_update,
            loading_status,
            cancels
          });
          if (!(queue === null ? enable_queue : queue)) {
            req.then(handle_update);
          }
        });
        function handle_update(output) {
          output.data.forEach((value, i4) => {
            if (typeof value === "object" && value !== null && value.__type__ === "update") {
              for (const [update_key, update_value] of Object.entries(value)) {
                if (update_key === "__type__") {
                  continue;
                } else {
                  $$invalidate(10, instance_map[outputs[i4]].props[update_key] = update_value, instance_map);
                }
              }
              $$invalidate(8, rootNode);
            } else {
              $$invalidate(10, instance_map[outputs[i4]].props.value = value, instance_map);
            }
          });
        }
        if (!handled_dependencies[i3])
          handled_dependencies[i3] = [];
        handled_dependencies[i3].push(id3);
      });
    });
  }
  function handle_destroy(id3) {
    handled_dependencies = handled_dependencies.map((dep) => {
      return dep.filter((_id) => _id !== id3);
    });
  }
  dependencies.forEach((v2, i2) => {
    loading_status.register(i2, v2.inputs, v2.outputs);
  });
  function set_status(statuses) {
    for (const id3 in statuses) {
      let loading_status2 = statuses[id3];
      let dependency = dependencies[loading_status2.fn_index];
      loading_status2.scroll_to_output = dependency.scroll_to_output;
      loading_status2.visible = dependency.show_progress;
      set_prop(instance_map[id3], "loading_status", loading_status2);
    }
    const inputs_to_update = loading_status.get_inputs_to_update();
    for (const [id3, pending_status] of inputs_to_update) {
      set_prop(instance_map[id3], "pending", pending_status === "pending");
    }
  }
  const destroy_handler = ({ detail }) => handle_destroy(detail);
  const click_handler = () => {
    set_api_docs_visible(false);
  };
  const close_handler = () => {
    set_api_docs_visible(false);
  };
  $$self.$$set = ($$props2) => {
    if ("root" in $$props2)
      $$invalidate(0, root = $$props2.root);
    if ("fn" in $$props2)
      $$invalidate(17, fn2 = $$props2.fn);
    if ("components" in $$props2)
      $$invalidate(18, components = $$props2.components);
    if ("layout" in $$props2)
      $$invalidate(19, layout = $$props2.layout);
    if ("dependencies" in $$props2)
      $$invalidate(1, dependencies = $$props2.dependencies);
    if ("enable_queue" in $$props2)
      $$invalidate(20, enable_queue = $$props2.enable_queue);
    if ("title" in $$props2)
      $$invalidate(2, title = $$props2.title);
    if ("analytics_enabled" in $$props2)
      $$invalidate(3, analytics_enabled = $$props2.analytics_enabled);
    if ("target" in $$props2)
      $$invalidate(4, target = $$props2.target);
    if ("id" in $$props2)
      $$invalidate(21, id2 = $$props2.id);
    if ("autoscroll" in $$props2)
      $$invalidate(22, autoscroll = $$props2.autoscroll);
    if ("show_api" in $$props2)
      $$invalidate(23, show_api = $$props2.show_api);
    if ("control_page_title" in $$props2)
      $$invalidate(5, control_page_title = $$props2.control_page_title);
    if ("app_mode" in $$props2)
      $$invalidate(6, app_mode2 = $$props2.app_mode);
    if ("theme" in $$props2)
      $$invalidate(7, theme = $$props2.theme);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty[0] & 4194304) {
      app_state.update((s2) => __spreadProps(__spreadValues({}, s2), { autoscroll }));
    }
    if ($$self.$$.dirty[0] & 16777216) {
      set_status($loading_status);
    }
  };
  return [
    root,
    dependencies,
    title,
    analytics_enabled,
    target,
    control_page_title,
    app_mode2,
    theme,
    rootNode,
    api_docs_visible,
    instance_map,
    ready,
    loading_status,
    set_api_docs_visible,
    dynamic_ids,
    handle_mount,
    handle_destroy,
    fn2,
    components,
    layout,
    enable_queue,
    id2,
    autoscroll,
    show_api,
    $loading_status,
    destroy_handler,
    click_handler,
    close_handler
  ];
}
class Blocks extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$7, create_fragment$7, safe_not_equal, {
      root: 0,
      fn: 17,
      components: 18,
      layout: 19,
      dependencies: 1,
      enable_queue: 20,
      title: 2,
      analytics_enabled: 3,
      target: 4,
      id: 21,
      autoscroll: 22,
      show_api: 23,
      control_page_title: 5,
      app_mode: 6,
      theme: 7
    }, null, [-1, -1]);
  }
}
function create_fragment$6(ctx) {
  let div;
  let current;
  const default_slot_template = ctx[1].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[0], null);
  return {
    c() {
      div = element("div");
      if (default_slot)
        default_slot.c();
      attr(div, "class", "gr-form overflow-hidden flex border-solid border bg-gray-200 dark:bg-gray-700 gap-px rounded-lg flex-wrap");
      set_style(div, "flex-direction", "inherit");
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if (default_slot) {
        default_slot.m(div, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 1)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[0], !current ? get_all_dirty_from_scope(ctx2[0]) : get_slot_changes(default_slot_template, ctx2[0], dirty, null), null);
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
      if (detaching)
        detach(div);
      if (default_slot)
        default_slot.d(detaching);
    }
  };
}
function instance$6($$self, $$props, $$invalidate) {
  let { $$slots: slots = {}, $$scope } = $$props;
  $$self.$$set = ($$props2) => {
    if ("$$scope" in $$props2)
      $$invalidate(0, $$scope = $$props2.$$scope);
  };
  return [$$scope, slots];
}
class Form extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$6, create_fragment$6, safe_not_equal, {});
  }
}
var global$1 = `*,:before,:after{box-sizing:border-box;border-width:0;border-style:solid;border-color:#e5e7eb}:before,:after{--tw-content: ""}html{line-height:1.5;-webkit-text-size-adjust:100%;-moz-tab-size:4;tab-size:4;font-family:Source Sans Pro,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica Neue,Arial,Noto Sans,sans-serif,"Apple Color Emoji","Segoe UI Emoji",Segoe UI Symbol,"Noto Color Emoji"}body{margin:0;line-height:inherit}hr{height:0;color:inherit;border-top-width:1px}abbr:where([title]){text-decoration:underline dotted}h1,h2,h3,h4,h5,h6{font-size:inherit;font-weight:inherit}a{color:inherit;text-decoration:inherit}b,strong{font-weight:bolder}code,kbd,samp,pre{font-family:IBM Plex Mono,ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,Liberation Mono,Courier New,monospace;font-size:1em}small{font-size:80%}sub,sup{font-size:75%;line-height:0;position:relative;vertical-align:baseline}sub{bottom:-.25em}sup{top:-.5em}table{text-indent:0;border-color:inherit;border-collapse:collapse}button,input,optgroup,select,textarea{font-family:inherit;font-size:100%;font-weight:inherit;line-height:inherit;color:inherit;margin:0;padding:0}button,select{text-transform:none}button,[type=button],[type=reset],[type=submit]{-webkit-appearance:button;background-color:transparent;background-image:none}:-moz-focusring{outline:auto}:-moz-ui-invalid{box-shadow:none}progress{vertical-align:baseline}::-webkit-inner-spin-button,::-webkit-outer-spin-button{height:auto}[type=search]{-webkit-appearance:textfield;outline-offset:-2px}::-webkit-search-decoration{-webkit-appearance:none}::-webkit-file-upload-button{-webkit-appearance:button;font:inherit}summary{display:list-item}blockquote,dl,dd,h1,h2,h3,h4,h5,h6,hr,figure,p,pre{margin:0}fieldset{margin:0;padding:0}legend{padding:0}ol,ul,menu{list-style:none;margin:0;padding:0}textarea{resize:vertical}input::placeholder,textarea::placeholder{opacity:1;color:#9ca3af}button,[role=button]{cursor:pointer}:disabled{cursor:default}img,svg,video,canvas,audio,iframe,embed,object{display:block;vertical-align:middle}img,video{max-width:100%;height:auto}[type=text],[type=email],[type=url],[type=password],[type=number],[type=date],[type=datetime-local],[type=month],[type=search],[type=tel],[type=time],[type=week],[multiple],textarea,select{appearance:none;background-color:#fff;border-color:#6b7280;border-width:1px;border-radius:0;padding:.5rem .75rem;font-size:1rem;line-height:1.5rem;--tw-shadow: 0 0 #0000}[type=text]:focus,[type=email]:focus,[type=url]:focus,[type=password]:focus,[type=number]:focus,[type=date]:focus,[type=datetime-local]:focus,[type=month]:focus,[type=search]:focus,[type=tel]:focus,[type=time]:focus,[type=week]:focus,[multiple]:focus,textarea:focus,select:focus{outline:2px solid transparent;outline-offset:2px;--tw-ring-inset: var(--tw-empty, );--tw-ring-offset-width: 0px;--tw-ring-offset-color: #fff;--tw-ring-color: #2563eb;--tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);--tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(1px + var(--tw-ring-offset-width)) var(--tw-ring-color);box-shadow:var(--tw-ring-offset-shadow),var(--tw-ring-shadow),var(--tw-shadow);border-color:#2563eb}input::placeholder,textarea::placeholder{color:#6b7280;opacity:1}::-webkit-datetime-edit-fields-wrapper{padding:0}::-webkit-date-and-time-value{min-height:1.5em}::-webkit-datetime-edit,::-webkit-datetime-edit-year-field,::-webkit-datetime-edit-month-field,::-webkit-datetime-edit-day-field,::-webkit-datetime-edit-hour-field,::-webkit-datetime-edit-minute-field,::-webkit-datetime-edit-second-field,::-webkit-datetime-edit-millisecond-field,::-webkit-datetime-edit-meridiem-field{padding-top:0;padding-bottom:0}select{background-image:url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e");background-position:right .5rem center;background-repeat:no-repeat;background-size:1.5em 1.5em;padding-right:2.5rem;color-adjust:exact}[multiple]{background-image:initial;background-position:initial;background-repeat:unset;background-size:initial;padding-right:.75rem;color-adjust:unset}[type=checkbox],[type=radio]{appearance:none;padding:0;color-adjust:exact;display:inline-block;vertical-align:middle;background-origin:border-box;user-select:none;flex-shrink:0;height:1rem;width:1rem;color:#2563eb;background-color:#fff;border-color:#6b7280;border-width:1px;--tw-shadow: 0 0 #0000}[type=checkbox]{border-radius:0}[type=radio]{border-radius:100%}[type=checkbox]:focus,[type=radio]:focus{outline:2px solid transparent;outline-offset:2px;--tw-ring-inset: var(--tw-empty, );--tw-ring-offset-width: 2px;--tw-ring-offset-color: #fff;--tw-ring-color: #2563eb;--tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);--tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(2px + var(--tw-ring-offset-width)) var(--tw-ring-color);box-shadow:var(--tw-ring-offset-shadow),var(--tw-ring-shadow),var(--tw-shadow)}[type=checkbox]:checked,[type=radio]:checked{border-color:transparent;background-color:currentColor;background-size:100% 100%;background-position:center;background-repeat:no-repeat}[type=checkbox]:checked{background-image:url("data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='white' xmlns='http://www.w3.org/2000/svg'%3e%3cpath d='M12.207 4.793a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0l-2-2a1 1 0 011.414-1.414L6.5 9.086l4.293-4.293a1 1 0 011.414 0z'/%3e%3c/svg%3e")}[type=radio]:checked{background-image:url("data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='white' xmlns='http://www.w3.org/2000/svg'%3e%3ccircle cx='8' cy='8' r='3'/%3e%3c/svg%3e")}[type=checkbox]:checked:hover,[type=checkbox]:checked:focus,[type=radio]:checked:hover,[type=radio]:checked:focus{border-color:transparent;background-color:currentColor}[type=checkbox]:indeterminate{background-image:url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 16 16'%3e%3cpath stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M4 8h8'/%3e%3c/svg%3e");border-color:transparent;background-color:currentColor;background-size:100% 100%;background-position:center;background-repeat:no-repeat}[type=checkbox]:indeterminate:hover,[type=checkbox]:indeterminate:focus{border-color:transparent;background-color:currentColor}[type=file]{background:unset;border-color:inherit;border-width:0;border-radius:0;padding:0;font-size:unset;line-height:inherit}[type=file]:focus{outline:1px auto -webkit-focus-ring-color}.bg-gray-950{background-color:#0b0f19}.dark{background-color:#0b0f19;--tw-bg-opacity: 1;background-color:rgb(11 15 25 / var(--tw-bg-opacity));--tw-text-opacity: 1;color:rgb(209 213 219 / var(--tw-text-opacity))}.dark .text-gray-500,.dark .text-gray-600,.dark .\\!text-gray-500{--tw-text-opacity: 1;color:rgb(209 213 219 / var(--tw-text-opacity))}.dark .text-gray-700,.dark .text-gray-800,.dark .text-gray-900,.dark .\\!text-gray-700,.dark .\\!text-gray-800{--tw-text-opacity: 1;color:rgb(229 231 235 / var(--tw-text-opacity))}.dark .border,.dark .border-gray-100,.dark .border-gray-200,.dark .border-gray-300,.dark .\\!border,.dark .\\!border-gray-300{--tw-border-opacity: 1;border-color:rgb(55 65 81 / var(--tw-border-opacity))}.dark .bg-white{background-color:#0b0f19;--tw-bg-opacity: 1;background-color:rgb(11 15 25 / var(--tw-bg-opacity))}.dark .bg-gray-50{--tw-bg-opacity: 1;background-color:rgb(17 24 39 / var(--tw-bg-opacity))}.dark .bg-gray-200,.dark .\\!bg-gray-200{--tw-bg-opacity: 1;background-color:rgb(31 41 55 / var(--tw-bg-opacity))}.unequal-height{align-items:flex-start}*,:before,:after{--tw-border-spacing-x: 0;--tw-border-spacing-y: 0;--tw-translate-x: 0;--tw-translate-y: 0;--tw-rotate: 0;--tw-skew-x: 0;--tw-skew-y: 0;--tw-scale-x: 1;--tw-scale-y: 1;--tw-pan-x: ;--tw-pan-y: ;--tw-pinch-zoom: ;--tw-scroll-snap-strictness: proximity;--tw-ordinal: ;--tw-slashed-zero: ;--tw-numeric-figure: ;--tw-numeric-spacing: ;--tw-numeric-fraction: ;--tw-ring-inset: ;--tw-ring-offset-width: 0px;--tw-ring-offset-color: #fff;--tw-ring-color: rgb(59 130 246 / .5);--tw-ring-offset-shadow: 0 0 #0000;--tw-ring-shadow: 0 0 #0000;--tw-shadow: 0 0 #0000;--tw-shadow-colored: 0 0 #0000;--tw-blur: ;--tw-brightness: ;--tw-contrast: ;--tw-grayscale: ;--tw-hue-rotate: ;--tw-invert: ;--tw-saturate: ;--tw-sepia: ;--tw-drop-shadow: ;--tw-backdrop-blur: ;--tw-backdrop-brightness: ;--tw-backdrop-contrast: ;--tw-backdrop-grayscale: ;--tw-backdrop-hue-rotate: ;--tw-backdrop-invert: ;--tw-backdrop-opacity: ;--tw-backdrop-saturate: ;--tw-backdrop-sepia: }::backdrop{--tw-border-spacing-x: 0;--tw-border-spacing-y: 0;--tw-translate-x: 0;--tw-translate-y: 0;--tw-rotate: 0;--tw-skew-x: 0;--tw-skew-y: 0;--tw-scale-x: 1;--tw-scale-y: 1;--tw-pan-x: ;--tw-pan-y: ;--tw-pinch-zoom: ;--tw-scroll-snap-strictness: proximity;--tw-ordinal: ;--tw-slashed-zero: ;--tw-numeric-figure: ;--tw-numeric-spacing: ;--tw-numeric-fraction: ;--tw-ring-inset: ;--tw-ring-offset-width: 0px;--tw-ring-offset-color: #fff;--tw-ring-color: rgb(59 130 246 / .5);--tw-ring-offset-shadow: 0 0 #0000;--tw-ring-shadow: 0 0 #0000;--tw-shadow: 0 0 #0000;--tw-shadow-colored: 0 0 #0000;--tw-blur: ;--tw-brightness: ;--tw-contrast: ;--tw-grayscale: ;--tw-hue-rotate: ;--tw-invert: ;--tw-saturate: ;--tw-sepia: ;--tw-drop-shadow: ;--tw-backdrop-blur: ;--tw-backdrop-brightness: ;--tw-backdrop-contrast: ;--tw-backdrop-grayscale: ;--tw-backdrop-hue-rotate: ;--tw-backdrop-invert: ;--tw-backdrop-opacity: ;--tw-backdrop-saturate: ;--tw-backdrop-sepia: }.container{width:100%}.\\!container{width:100%!important}@media (min-width: 640px){.container{max-width:640px}.\\!container{max-width:640px!important}}@media (min-width: 768px){.container{max-width:768px}.\\!container{max-width:768px!important}}@media (min-width: 1024px){.container{max-width:1024px}.\\!container{max-width:1024px!important}}@media (min-width: 1280px){.container{max-width:1280px}.\\!container{max-width:1280px!important}}@media (min-width: 1536px){.container{max-width:1536px}.\\!container{max-width:1536px!important}}.gr-form>.gr-block{border-radius:0;border-width:0px;--tw-shadow: 0 0 #0000 !important;--tw-shadow-colored: 0 0 #0000 !important;box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)!important}.row>*,.row>.gr-form>*{min-width:min(160px,100%)}.\\!row>*,.\\!row>.gr-form>*{min-width:min(160px,100%)!important}.row>*,.row>.gr-form>*{flex:1 1 0%}.\\!row>*,.\\!row>.gr-form>*{flex:1 1 0%}.col>*,.col>.gr-form>*{width:100%}.gr-compact>*,.gr-compact .gr-box{border-radius:0!important;border-width:0px!important}.scroll-hide{-ms-overflow-style:none;scrollbar-width:none}.scroll-hide::-webkit-scrollbar{display:none}.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border-width:0}.pointer-events-none{pointer-events:none}.visible{visibility:visible}.\\!visible{visibility:visible!important}.invisible{visibility:hidden}.static{position:static}.fixed{position:fixed}.absolute{position:absolute}.\\!absolute{position:absolute!important}.relative{position:relative}.sticky{position:sticky}.inset-0{top:0px;right:0px;bottom:0px;left:0px}.inset-2{top:.5rem;right:.5rem;bottom:.5rem;left:.5rem}.inset-x-0{left:0px;right:0px}.left-0{left:0px}.top-0{top:0px}.top-2{top:.5rem}.right-2{right:.5rem}.top-10{top:2.5rem}.right-0{right:0px}.top-\\[2px\\]{top:2px}.right-6{right:1.5rem}.top-6{top:1.5rem}.top-\\[-3px\\]{top:-3px}.bottom-2{bottom:.5rem}.bottom-0{bottom:0px}.isolate{isolation:isolate}.z-20{z-index:20}.z-40{z-index:40}.z-10{z-index:10}.z-50{z-index:50}.\\!m-0{margin:0!important}.m-auto{margin:auto}.m-1{margin:.25rem}.m-1\\.5{margin:.375rem}.m-0{margin:0}.mx-2{margin-left:.5rem;margin-right:.5rem}.my-4{margin-top:1rem;margin-bottom:1rem}.mx-3{margin-left:.75rem;margin-right:.75rem}.\\!mt-0{margin-top:0!important}.\\!mb-0{margin-bottom:0!important}.\\!ml-0{margin-left:0!important}.\\!mr-0{margin-right:0!important}.mr-2{margin-right:.5rem}.mb-2{margin-bottom:.5rem}.mt-6{margin-top:1.5rem}.mt-7{margin-top:1.75rem}.mt-3{margin-top:.75rem}.mb-7{margin-bottom:1.75rem}.ml-2{margin-left:.5rem}.mr-\\[-4px\\]{margin-right:-4px}.mt-\\[0\\.05rem\\]{margin-top:.05rem}.mb-3{margin-bottom:.75rem}.mr-0\\.5{margin-right:.125rem}.mr-0{margin-right:0}.mt-10{margin-top:2.5rem}.mb-1{margin-bottom:.25rem}.ml-auto{margin-left:auto}.mr-1{margin-right:.25rem}.-mb-\\[2px\\]{margin-bottom:-2px}.box-border{box-sizing:border-box}.block{display:block}.\\!block{display:block!important}.inline-block{display:inline-block}.inline{display:inline}.flex{display:flex}.inline-flex{display:inline-flex}.table{display:table}.grid{display:grid}.contents{display:contents}.\\!hidden{display:none!important}.hidden{display:none}.h-0{height:0px}.h-\\[12px\\]{height:12px}.h-5{height:1.25rem}.h-\\[60\\%\\]{height:60%}.h-1\\.5{height:.375rem}.h-1{height:.25rem}.h-full{height:100%}.h-14{height:3.5rem}.h-6{height:1.5rem}.h-\\[40vh\\]{height:40vh}.h-60{height:15rem}.h-10{height:2.5rem}.h-2\\/4{height:50%}.h-20{height:5rem}.h-2{height:.5rem}.h-3{height:.75rem}.max-h-\\[55vh\\]{max-height:55vh}.max-h-96{max-height:24rem}.max-h-60{max-height:15rem}.max-h-\\[30rem\\]{max-height:30rem}.max-h-\\[15rem\\]{max-height:15rem}.min-h-\\[350px\\]{min-height:350px}.min-h-\\[8rem\\]{min-height:8rem}.min-h-\\[200px\\]{min-height:200px}.min-h-\\[15rem\\]{min-height:15rem}.min-h-\\[6rem\\]{min-height:6rem}.min-h-\\[16rem\\]{min-height:16rem}.min-h-\\[2\\.3rem\\]{min-height:2.3rem}.min-h-\\[10rem\\]{min-height:10rem}.w-full{width:100%}.w-\\[12px\\]{width:12px}.w-5{width:1.25rem}.w-\\[60\\%\\]{width:60%}.w-1\\.5{width:.375rem}.w-1{width:.25rem}.w-6{width:1.5rem}.w-3\\/12{width:25%}.w-5\\/12{width:41.666667%}.w-10{width:2.5rem}.w-2\\/4{width:50%}.w-0{width:0px}.w-2{width:.5rem}.w-3{width:.75rem}.w-4{width:1rem}.max-w-full{max-width:100%}.max-w-none{max-width:none}.flex-1{flex:1 1 0%}.flex-none{flex:none}.\\!flex-none{flex:none!important}.shrink-0{flex-shrink:0}.shrink{flex-shrink:1}.flex-grow,.grow{flex-grow:1}.grow-0{flex-grow:0}.\\!grow-0{flex-grow:0!important}.table-auto{table-layout:auto}.border-collapse{border-collapse:collapse}.translate-x-px{--tw-translate-x: 1px;transform:translate(var(--tw-translate-x),var(--tw-translate-y)) rotate(var(--tw-rotate)) skew(var(--tw-skew-x)) skewY(var(--tw-skew-y)) scaleX(var(--tw-scale-x)) scaleY(var(--tw-scale-y))}.scale-x-\\[-1\\]{--tw-scale-x: -1;transform:translate(var(--tw-translate-x),var(--tw-translate-y)) rotate(var(--tw-rotate)) skew(var(--tw-skew-x)) skewY(var(--tw-skew-y)) scaleX(var(--tw-scale-x)) scaleY(var(--tw-scale-y))}.-scale-y-\\[1\\]{--tw-scale-y: -1;transform:translate(var(--tw-translate-x),var(--tw-translate-y)) rotate(var(--tw-rotate)) skew(var(--tw-skew-x)) skewY(var(--tw-skew-y)) scaleX(var(--tw-scale-x)) scaleY(var(--tw-scale-y))}.transform{transform:translate(var(--tw-translate-x),var(--tw-translate-y)) rotate(var(--tw-rotate)) skew(var(--tw-skew-x)) skewY(var(--tw-skew-y)) scaleX(var(--tw-scale-x)) scaleY(var(--tw-scale-y))}.\\!transform{transform:translate(var(--tw-translate-x),var(--tw-translate-y)) rotate(var(--tw-rotate)) skew(var(--tw-skew-x)) skewY(var(--tw-skew-y)) scaleX(var(--tw-scale-x)) scaleY(var(--tw-scale-y))!important}@keyframes ping{75%,to{transform:scale(2);opacity:0}}.animate-ping{animation:ping 1s cubic-bezier(0,0,.2,1) infinite}.\\!cursor-not-allowed{cursor:not-allowed!important}.cursor-pointer{cursor:pointer}.cursor-default{cursor:default}.cursor-crosshair{cursor:crosshair}.cursor-move{cursor:move}.cursor-col-resize{cursor:col-resize}.cursor-row-resize{cursor:row-resize}.cursor-ns-resize{cursor:ns-resize}.cursor-ew-resize{cursor:ew-resize}.cursor-sw-resize{cursor:sw-resize}.cursor-s-resize{cursor:s-resize}.cursor-se-resize{cursor:se-resize}.cursor-w-resize{cursor:w-resize}.cursor-e-resize{cursor:e-resize}.cursor-nw-resize{cursor:nw-resize}.cursor-n-resize{cursor:n-resize}.cursor-ne-resize{cursor:ne-resize}.cursor-grab{cursor:grab}.touch-none{touch-action:none}.resize{resize:both}.grid-cols-1{grid-template-columns:repeat(1,minmax(0,1fr))}.grid-cols-2{grid-template-columns:repeat(2,minmax(0,1fr))}.grid-cols-3{grid-template-columns:repeat(3,minmax(0,1fr))}.grid-cols-4{grid-template-columns:repeat(4,minmax(0,1fr))}.grid-cols-5{grid-template-columns:repeat(5,minmax(0,1fr))}.grid-cols-6{grid-template-columns:repeat(6,minmax(0,1fr))}.grid-cols-7{grid-template-columns:repeat(7,minmax(0,1fr))}.grid-cols-8{grid-template-columns:repeat(8,minmax(0,1fr))}.grid-cols-9{grid-template-columns:repeat(9,minmax(0,1fr))}.grid-cols-10{grid-template-columns:repeat(10,minmax(0,1fr))}.grid-cols-11{grid-template-columns:repeat(11,minmax(0,1fr))}.grid-cols-12{grid-template-columns:repeat(12,minmax(0,1fr))}.flex-row{flex-direction:row}.flex-col{flex-direction:column}.flex-wrap{flex-wrap:wrap}.items-start{align-items:flex-start}.items-end{align-items:flex-end}.items-center{align-items:center}.items-baseline{align-items:baseline}.items-stretch{align-items:stretch}.justify-end{justify-content:flex-end}.justify-center{justify-content:center}.justify-between{justify-content:space-between}.gap-4{gap:1rem}.gap-1{gap:.25rem}.gap-2{gap:.5rem}.gap-0{gap:0px}.space-y-2>:not([hidden])~:not([hidden]){--tw-space-y-reverse: 0;margin-top:calc(.5rem * calc(1 - var(--tw-space-y-reverse)));margin-bottom:calc(.5rem * var(--tw-space-y-reverse))}.space-y-4>:not([hidden])~:not([hidden]){--tw-space-y-reverse: 0;margin-top:calc(1rem * calc(1 - var(--tw-space-y-reverse)));margin-bottom:calc(1rem * var(--tw-space-y-reverse))}.space-x-2>:not([hidden])~:not([hidden]){--tw-space-x-reverse: 0;margin-right:calc(.5rem * var(--tw-space-x-reverse));margin-left:calc(.5rem * calc(1 - var(--tw-space-x-reverse)))}.space-x-4>:not([hidden])~:not([hidden]){--tw-space-x-reverse: 0;margin-right:calc(1rem * var(--tw-space-x-reverse));margin-left:calc(1rem * calc(1 - var(--tw-space-x-reverse)))}.space-x-1>:not([hidden])~:not([hidden]){--tw-space-x-reverse: 0;margin-right:calc(.25rem * var(--tw-space-x-reverse));margin-left:calc(.25rem * calc(1 - var(--tw-space-x-reverse)))}.divide-x>:not([hidden])~:not([hidden]){--tw-divide-x-reverse: 0;border-right-width:calc(1px * var(--tw-divide-x-reverse));border-left-width:calc(1px * calc(1 - var(--tw-divide-x-reverse)))}.place-self-start{place-self:start}.overflow-hidden{overflow:hidden}.overflow-clip{overflow:clip}.\\!overflow-visible{overflow:visible!important}.overflow-y-auto{overflow-y:auto}.overflow-x-scroll{overflow-x:scroll}.overflow-y-scroll{overflow-y:scroll}.truncate{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.whitespace-nowrap{white-space:nowrap}.break-all{word-break:break-all}.rounded{border-radius:.25rem}.rounded-xl{border-radius:.75rem}.rounded-full{border-radius:9999px}.rounded-lg{border-radius:.5rem}.rounded-sm{border-radius:.125rem}.rounded-\\[22px\\]{border-radius:22px}.rounded-none{border-radius:0}.\\!rounded-none{border-radius:0!important}.\\!rounded-lg{border-radius:.5rem!important}.rounded-md{border-radius:.375rem}.rounded-t-lg{border-top-left-radius:.5rem;border-top-right-radius:.5rem}.\\!rounded-br-none{border-bottom-right-radius:0!important}.\\!rounded-br-lg{border-bottom-right-radius:.5rem!important}.\\!rounded-bl-none{border-bottom-left-radius:0!important}.\\!rounded-bl-lg{border-bottom-left-radius:.5rem!important}.\\!rounded-tr-none{border-top-right-radius:0!important}.\\!rounded-tr-lg{border-top-right-radius:.5rem!important}.\\!rounded-tl-none{border-top-left-radius:0!important}.\\!rounded-tl-lg{border-top-left-radius:.5rem!important}.rounded-br-lg{border-bottom-right-radius:.5rem}.rounded-br-none{border-bottom-right-radius:0}.rounded-bl-none{border-bottom-left-radius:0}.rounded-bl-lg{border-bottom-left-radius:.5rem}.rounded-tl-lg{border-top-left-radius:.5rem}.rounded-tr-lg{border-top-right-radius:.5rem}.border{border-width:1px}.\\!border-0{border-width:0px!important}.border-0{border-width:0px}.border-2{border-width:2px}.\\!border{border-width:1px!important}.\\!border-t-0{border-top-width:0px!important}.\\!border-b-0{border-bottom-width:0px!important}.\\!border-l-0{border-left-width:0px!important}.\\!border-r-0{border-right-width:0px!important}.border-b{border-bottom-width:1px}.border-r{border-right-width:1px}.border-l{border-left-width:1px}.border-t-0{border-top-width:0px}.border-b-2{border-bottom-width:2px}.border-b-0{border-bottom-width:0px}.border-solid{border-style:solid}.border-dashed{border-style:dashed}.\\!border-none{border-style:none!important}.border-gray-300{--tw-border-opacity: 1;border-color:rgb(209 213 219 / var(--tw-border-opacity))}.border-gray-200{--tw-border-opacity: 1;border-color:rgb(229 231 235 / var(--tw-border-opacity))}.border-green-400{--tw-border-opacity: 1;border-color:rgb(74 222 128 / var(--tw-border-opacity))}.border-gray-100{--tw-border-opacity: 1;border-color:rgb(243 244 246 / var(--tw-border-opacity))}.border-gray-200\\/60{border-color:#e5e7eb99}.border-transparent{border-color:transparent}.border-orange-200{--tw-border-opacity: 1;border-color:rgb(255 216 180 / var(--tw-border-opacity))}.border-red-200{--tw-border-opacity: 1;border-color:rgb(254 202 202 / var(--tw-border-opacity))}.\\!border-red-300{--tw-border-opacity: 1 !important;border-color:rgb(252 165 165 / var(--tw-border-opacity))!important}.\\!border-yellow-300{--tw-border-opacity: 1 !important;border-color:rgb(253 224 71 / var(--tw-border-opacity))!important}.\\!border-green-300{--tw-border-opacity: 1 !important;border-color:rgb(134 239 172 / var(--tw-border-opacity))!important}.\\!border-blue-300{--tw-border-opacity: 1 !important;border-color:rgb(147 197 253 / var(--tw-border-opacity))!important}.\\!border-purple-300{--tw-border-opacity: 1 !important;border-color:rgb(216 180 254 / var(--tw-border-opacity))!important}.\\!border-gray-300{--tw-border-opacity: 1 !important;border-color:rgb(209 213 219 / var(--tw-border-opacity))!important}.\\!border-pink-300{--tw-border-opacity: 1 !important;border-color:rgb(249 168 212 / var(--tw-border-opacity))!important}.\\!bg-red-200{--tw-bg-opacity: 1 !important;background-color:rgb(254 202 202 / var(--tw-bg-opacity))!important}.\\!bg-green-200{--tw-bg-opacity: 1 !important;background-color:rgb(187 247 208 / var(--tw-bg-opacity))!important}.\\!bg-blue-200{--tw-bg-opacity: 1 !important;background-color:rgb(191 219 254 / var(--tw-bg-opacity))!important}.\\!bg-yellow-200{--tw-bg-opacity: 1 !important;background-color:rgb(254 240 138 / var(--tw-bg-opacity))!important}.\\!bg-purple-200{--tw-bg-opacity: 1 !important;background-color:rgb(233 213 255 / var(--tw-bg-opacity))!important}.\\!bg-teal-200{--tw-bg-opacity: 1 !important;background-color:rgb(153 246 228 / var(--tw-bg-opacity))!important}.\\!bg-orange-200{--tw-bg-opacity: 1 !important;background-color:rgb(255 216 180 / var(--tw-bg-opacity))!important}.\\!bg-cyan-200{--tw-bg-opacity: 1 !important;background-color:rgb(165 243 252 / var(--tw-bg-opacity))!important}.\\!bg-lime-200{--tw-bg-opacity: 1 !important;background-color:rgb(217 249 157 / var(--tw-bg-opacity))!important}.\\!bg-pink-200{--tw-bg-opacity: 1 !important;background-color:rgb(251 207 232 / var(--tw-bg-opacity))!important}.\\!bg-gray-200{--tw-bg-opacity: 1 !important;background-color:rgb(229 231 235 / var(--tw-bg-opacity))!important}.bg-gray-950{--tw-bg-opacity: 1;background-color:rgb(11 15 25 / var(--tw-bg-opacity))}.bg-white{--tw-bg-opacity: 1;background-color:rgb(255 255 255 / var(--tw-bg-opacity))}.bg-white\\/90{background-color:#ffffffe6}.bg-gray-50{--tw-bg-opacity: 1;background-color:rgb(249 250 251 / var(--tw-bg-opacity))}.\\!bg-red-500\\/10{background-color:#ef44441a!important}.bg-red-400{--tw-bg-opacity: 1;background-color:rgb(248 113 113 / var(--tw-bg-opacity))}.bg-red-500{--tw-bg-opacity: 1;background-color:rgb(239 68 68 / var(--tw-bg-opacity))}.bg-gray-200{--tw-bg-opacity: 1;background-color:rgb(229 231 235 / var(--tw-bg-opacity))}.bg-black\\/90{background-color:#000000e6}.bg-transparent{background-color:transparent}.bg-orange-50{--tw-bg-opacity: 1;background-color:rgb(255 242 229 / var(--tw-bg-opacity))}.\\!bg-transparent{background-color:transparent!important}.\\!bg-red-100{--tw-bg-opacity: 1 !important;background-color:rgb(254 226 226 / var(--tw-bg-opacity))!important}.\\!bg-yellow-100{--tw-bg-opacity: 1 !important;background-color:rgb(254 249 195 / var(--tw-bg-opacity))!important}.\\!bg-green-100{--tw-bg-opacity: 1 !important;background-color:rgb(220 252 231 / var(--tw-bg-opacity))!important}.\\!bg-blue-100{--tw-bg-opacity: 1 !important;background-color:rgb(219 234 254 / var(--tw-bg-opacity))!important}.\\!bg-purple-100{--tw-bg-opacity: 1 !important;background-color:rgb(243 232 255 / var(--tw-bg-opacity))!important}.\\!bg-gray-100{--tw-bg-opacity: 1 !important;background-color:rgb(243 244 246 / var(--tw-bg-opacity))!important}.\\!bg-pink-100{--tw-bg-opacity: 1 !important;background-color:rgb(252 231 243 / var(--tw-bg-opacity))!important}.bg-black{--tw-bg-opacity: 1;background-color:rgb(0 0 0 / var(--tw-bg-opacity))}.bg-slate-800{--tw-bg-opacity: 1;background-color:rgb(30 41 59 / var(--tw-bg-opacity))}.bg-opacity-20{--tw-bg-opacity: .2}.bg-opacity-80{--tw-bg-opacity: .8}.bg-gradient-to-r{background-image:linear-gradient(to right,var(--tw-gradient-stops))}.bg-gradient-to-t{background-image:linear-gradient(to top,var(--tw-gradient-stops))}.bg-gradient-to-br{background-image:linear-gradient(to bottom right,var(--tw-gradient-stops))}.from-orange-400{--tw-gradient-from: #FF9633;--tw-gradient-to: rgb(255 150 51 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.from-gray-50{--tw-gradient-from: #f9fafb;--tw-gradient-to: rgb(249 250 251 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.from-orange-200\\/70{--tw-gradient-from: rgb(255 216 180 / .7);--tw-gradient-to: rgb(255 216 180 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.from-gray-100\\/70{--tw-gradient-from: rgb(243 244 246 / .7);--tw-gradient-to: rgb(243 244 246 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.from-red-200\\/70{--tw-gradient-from: rgb(254 202 202 / .7);--tw-gradient-to: rgb(254 202 202 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.\\!from-red-100{--tw-gradient-from: #fee2e2 !important;--tw-gradient-to: rgb(254 226 226 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.\\!from-yellow-100{--tw-gradient-from: #fef9c3 !important;--tw-gradient-to: rgb(254 249 195 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.\\!from-green-100{--tw-gradient-from: #dcfce7 !important;--tw-gradient-to: rgb(220 252 231 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.\\!from-blue-100{--tw-gradient-from: #dbeafe !important;--tw-gradient-to: rgb(219 234 254 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.\\!from-purple-100{--tw-gradient-from: #f3e8ff !important;--tw-gradient-to: rgb(243 232 255 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.\\!from-gray-100{--tw-gradient-from: #f3f4f6 !important;--tw-gradient-to: rgb(243 244 246 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.\\!from-pink-100{--tw-gradient-from: #fce7f3 !important;--tw-gradient-to: rgb(252 231 243 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.to-orange-200{--tw-gradient-to: #FFD8B4}.to-white{--tw-gradient-to: #fff}.to-orange-300\\/80{--tw-gradient-to: rgb(255 176 102 / .8)}.to-gray-200\\/80{--tw-gradient-to: rgb(229 231 235 / .8)}.to-red-300\\/80{--tw-gradient-to: rgb(252 165 165 / .8)}.\\!to-red-200{--tw-gradient-to: #fecaca !important}.\\!to-yellow-200{--tw-gradient-to: #fef08a !important}.\\!to-green-200{--tw-gradient-to: #bbf7d0 !important}.\\!to-blue-200{--tw-gradient-to: #bfdbfe !important}.\\!to-purple-200{--tw-gradient-to: #e9d5ff !important}.\\!to-gray-200{--tw-gradient-to: #e5e7eb !important}.\\!to-pink-200{--tw-gradient-to: #fbcfe8 !important}.fill-current{fill:currentColor}.object-contain{object-fit:contain}.object-cover{object-fit:cover}.p-4{padding:1rem}.p-2{padding:.5rem}.p-3{padding:.75rem}.p-1{padding:.25rem}.p-0{padding:0}.p-2\\.5{padding:.625rem}.\\!p-0{padding:0!important}.p-6{padding:1.5rem}.py-1{padding-top:.25rem;padding-bottom:.25rem}.px-2{padding-left:.5rem;padding-right:.5rem}.px-3{padding-left:.75rem;padding-right:.75rem}.py-2{padding-top:.5rem;padding-bottom:.5rem}.py-1\\.5{padding-top:.375rem;padding-bottom:.375rem}.px-1{padding-left:.25rem;padding-right:.25rem}.px-\\[0\\.325rem\\]{padding-left:.325rem;padding-right:.325rem}.py-\\[0\\.05rem\\]{padding-top:.05rem;padding-bottom:.05rem}.py-6{padding-top:1.5rem;padding-bottom:1.5rem}.px-4{padding-left:1rem;padding-right:1rem}.py-2\\.5{padding-top:.625rem;padding-bottom:.625rem}.py-0\\.5{padding-top:.125rem;padding-bottom:.125rem}.py-0{padding-top:0;padding-bottom:0}.px-1\\.5{padding-left:.375rem;padding-right:.375rem}.px-\\[0\\.4rem\\]{padding-left:.4rem;padding-right:.4rem}.pt-2{padding-top:.5rem}.pb-\\[0\\.225rem\\]{padding-bottom:.225rem}.pt-\\[0\\.15rem\\]{padding-top:.15rem}.pl-4{padding-left:1rem}.pb-1\\.5{padding-bottom:.375rem}.pb-1{padding-bottom:.25rem}.pb-2{padding-bottom:.5rem}.pt-1\\.5{padding-top:.375rem}.pt-1{padding-top:.25rem}.text-left{text-align:left}.text-center{text-align:center}.text-right{text-align:right}.text-justify{text-align:justify}.align-middle{vertical-align:middle}.font-mono{font-family:IBM Plex Mono,ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,Liberation Mono,Courier New,monospace}.font-sans{font-family:Source Sans Pro,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica Neue,Arial,Noto Sans,sans-serif,"Apple Color Emoji","Segoe UI Emoji",Segoe UI Symbol,"Noto Color Emoji"}.text-xs{font-size:.75rem;line-height:1rem}.text-\\[0\\.855rem\\]{font-size:.855rem}.text-sm{font-size:.875rem;line-height:1.25rem}.text-2xl{font-size:1.5rem;line-height:2rem}.text-\\[10px\\]{font-size:10px}.text-4xl{font-size:2.25rem;line-height:2.5rem}.font-semibold{font-weight:600}.font-bold{font-weight:700}.uppercase{text-transform:uppercase}.lowercase{text-transform:lowercase}.capitalize{text-transform:capitalize}.italic{font-style:italic}.ordinal{--tw-ordinal: ordinal;font-variant-numeric:var(--tw-ordinal) var(--tw-slashed-zero) var(--tw-numeric-figure) var(--tw-numeric-spacing) var(--tw-numeric-fraction)}.leading-7{line-height:1.75rem}.leading-tight{line-height:1.25}.leading-none{line-height:1}.leading-snug{line-height:1.375}.\\!text-red-500{--tw-text-opacity: 1 !important;color:rgb(239 68 68 / var(--tw-text-opacity))!important}.\\!text-green-500{--tw-text-opacity: 1 !important;color:rgb(34 197 94 / var(--tw-text-opacity))!important}.\\!text-blue-500{--tw-text-opacity: 1 !important;color:rgb(59 130 246 / var(--tw-text-opacity))!important}.\\!text-yellow-500{--tw-text-opacity: 1 !important;color:rgb(234 179 8 / var(--tw-text-opacity))!important}.\\!text-purple-500{--tw-text-opacity: 1 !important;color:rgb(168 85 247 / var(--tw-text-opacity))!important}.\\!text-teal-500{--tw-text-opacity: 1 !important;color:rgb(20 184 166 / var(--tw-text-opacity))!important}.\\!text-orange-500{--tw-text-opacity: 1 !important;color:rgb(255 124 0 / var(--tw-text-opacity))!important}.\\!text-cyan-500{--tw-text-opacity: 1 !important;color:rgb(6 182 212 / var(--tw-text-opacity))!important}.\\!text-lime-500{--tw-text-opacity: 1 !important;color:rgb(132 204 22 / var(--tw-text-opacity))!important}.\\!text-pink-500{--tw-text-opacity: 1 !important;color:rgb(236 72 153 / var(--tw-text-opacity))!important}.\\!text-gray-500{--tw-text-opacity: 1 !important;color:rgb(107 114 128 / var(--tw-text-opacity))!important}.\\!text-gray-700{--tw-text-opacity: 1 !important;color:rgb(55 65 81 / var(--tw-text-opacity))!important}.text-gray-500{--tw-text-opacity: 1;color:rgb(107 114 128 / var(--tw-text-opacity))}.text-red-500{--tw-text-opacity: 1;color:rgb(239 68 68 / var(--tw-text-opacity))}.text-gray-800{--tw-text-opacity: 1;color:rgb(31 41 55 / var(--tw-text-opacity))}.text-gray-300{--tw-text-opacity: 1;color:rgb(209 213 219 / var(--tw-text-opacity))}.text-white{--tw-text-opacity: 1;color:rgb(255 255 255 / var(--tw-text-opacity))}.text-indigo-600{--tw-text-opacity: 1;color:rgb(79 70 229 / var(--tw-text-opacity))}.text-gray-700{--tw-text-opacity: 1;color:rgb(55 65 81 / var(--tw-text-opacity))}.text-black{--tw-text-opacity: 1;color:rgb(0 0 0 / var(--tw-text-opacity))}.text-gray-400{--tw-text-opacity: 1;color:rgb(156 163 175 / var(--tw-text-opacity))}.text-green-600{--tw-text-opacity: 1;color:rgb(22 163 74 / var(--tw-text-opacity))}.text-green-500{--tw-text-opacity: 1;color:rgb(34 197 94 / var(--tw-text-opacity))}.text-blue-500{--tw-text-opacity: 1;color:rgb(59 130 246 / var(--tw-text-opacity))}.text-gray-600{--tw-text-opacity: 1;color:rgb(75 85 99 / var(--tw-text-opacity))}.text-gray-900{--tw-text-opacity: 1;color:rgb(17 24 39 / var(--tw-text-opacity))}.text-gray-200{--tw-text-opacity: 1;color:rgb(229 231 235 / var(--tw-text-opacity))}.text-orange-500{--tw-text-opacity: 1;color:rgb(255 124 0 / var(--tw-text-opacity))}.text-blue-600{--tw-text-opacity: 1;color:rgb(37 99 235 / var(--tw-text-opacity))}.text-orange-600{--tw-text-opacity: 1;color:rgb(238 116 0 / var(--tw-text-opacity))}.text-red-600{--tw-text-opacity: 1;color:rgb(220 38 38 / var(--tw-text-opacity))}.\\!text-gray-800{--tw-text-opacity: 1 !important;color:rgb(31 41 55 / var(--tw-text-opacity))!important}.underline{text-decoration-line:underline}.opacity-80{opacity:.8}.opacity-75{opacity:.75}.opacity-20{opacity:.2}.opacity-50{opacity:.5}.opacity-0{opacity:0}.opacity-40{opacity:.4}.shadow-sm{--tw-shadow: 0 1px 2px 0 rgb(0 0 0 / .05);--tw-shadow-colored: 0 1px 2px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.shadow-inner{--tw-shadow: inset 0 2px 4px 0 rgb(0 0 0 / .05);--tw-shadow-colored: inset 0 2px 4px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.\\!shadow-none{--tw-shadow: 0 0 #0000 !important;--tw-shadow-colored: 0 0 #0000 !important;box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)!important}.shadow{--tw-shadow: 0 1px 3px 0 rgb(0 0 0 / .1), 0 1px 2px -1px rgb(0 0 0 / .1);--tw-shadow-colored: 0 1px 3px 0 var(--tw-shadow-color), 0 1px 2px -1px var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.outline-none{outline:2px solid transparent;outline-offset:2px}.outline{outline-style:solid}.ring{--tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);--tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(3px + var(--tw-ring-offset-width)) var(--tw-ring-color);box-shadow:var(--tw-ring-offset-shadow),var(--tw-ring-shadow),var(--tw-shadow, 0 0 #0000)}.ring-inset{--tw-ring-inset: inset}.ring-gray-200{--tw-ring-opacity: 1;--tw-ring-color: rgb(229 231 235 / var(--tw-ring-opacity))}.ring-orange-500{--tw-ring-opacity: 1;--tw-ring-color: rgb(255 124 0 / var(--tw-ring-opacity))}.blur{--tw-blur: blur(8px);filter:var(--tw-blur) var(--tw-brightness) var(--tw-contrast) var(--tw-grayscale) var(--tw-hue-rotate) var(--tw-invert) var(--tw-saturate) var(--tw-sepia) var(--tw-drop-shadow)}.drop-shadow-lg{--tw-drop-shadow: drop-shadow(0 10px 8px rgb(0 0 0 / .04)) drop-shadow(0 4px 3px rgb(0 0 0 / .1));filter:var(--tw-blur) var(--tw-brightness) var(--tw-contrast) var(--tw-grayscale) var(--tw-hue-rotate) var(--tw-invert) var(--tw-saturate) var(--tw-sepia) var(--tw-drop-shadow)}.invert{--tw-invert: invert(100%);filter:var(--tw-blur) var(--tw-brightness) var(--tw-contrast) var(--tw-grayscale) var(--tw-hue-rotate) var(--tw-invert) var(--tw-saturate) var(--tw-sepia) var(--tw-drop-shadow)}.sepia{--tw-sepia: sepia(100%);filter:var(--tw-blur) var(--tw-brightness) var(--tw-contrast) var(--tw-grayscale) var(--tw-hue-rotate) var(--tw-invert) var(--tw-saturate) var(--tw-sepia) var(--tw-drop-shadow)}.filter{filter:var(--tw-blur) var(--tw-brightness) var(--tw-contrast) var(--tw-grayscale) var(--tw-hue-rotate) var(--tw-invert) var(--tw-saturate) var(--tw-sepia) var(--tw-drop-shadow)}.\\!filter{filter:var(--tw-blur) var(--tw-brightness) var(--tw-contrast) var(--tw-grayscale) var(--tw-hue-rotate) var(--tw-invert) var(--tw-saturate) var(--tw-sepia) var(--tw-drop-shadow)!important}.transition-colors{transition-property:color,background-color,border-color,text-decoration-color,fill,stroke;transition-timing-function:cubic-bezier(.4,0,.2,1);transition-duration:.15s}.transition-opacity{transition-property:opacity;transition-timing-function:cubic-bezier(.4,0,.2,1);transition-duration:.15s}.transition-all{transition-property:all;transition-timing-function:cubic-bezier(.4,0,.2,1);transition-duration:.15s}.transition{transition-property:color,background-color,border-color,text-decoration-color,fill,stroke,opacity,box-shadow,transform,filter,backdrop-filter;transition-timing-function:cubic-bezier(.4,0,.2,1);transition-duration:.15s}.duration-500{transition-duration:.5s}.ease-out{transition-timing-function:cubic-bezier(0,0,.2,1)}.gradio-container{line-height:1.5;-webkit-text-size-adjust:100%;-moz-tab-size:4;tab-size:4;font-family:Source Sans Pro,ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica Neue,Arial,Noto Sans,sans-serif,"Apple Color Emoji","Segoe UI Emoji",Segoe UI Symbol,"Noto Color Emoji"}.cropper-container{direction:ltr;font-size:0;line-height:0;position:relative;-ms-touch-action:none;touch-action:none;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none}.cropper-container img{display:block;height:100%;image-orientation:0deg;max-height:none!important;max-width:none!important;min-height:0!important;min-width:0!important;width:100%}.cropper-wrap-box,.cropper-canvas,.cropper-drag-box,.cropper-crop-box,.cropper-modal{bottom:0;left:0;position:absolute;right:0;top:0}.cropper-wrap-box,.cropper-canvas{overflow:hidden}.cropper-drag-box{background-color:#fff;opacity:0}.cropper-modal{background-color:#000;opacity:.5}.cropper-view-box{display:block;height:100%;outline:1px solid #39f;outline-color:#3399ffbf;overflow:hidden;width:100%}.cropper-dashed{border:0 dashed #eee;display:block;opacity:.5;position:absolute}.cropper-dashed.dashed-h{border-bottom-width:1px;border-top-width:1px;height:calc(100% / 3);left:0;top:calc(100% / 3);width:100%}.cropper-dashed.dashed-v{border-left-width:1px;border-right-width:1px;height:100%;left:calc(100% / 3);top:0;width:calc(100% / 3)}.cropper-center{display:block;height:0;left:50%;opacity:.75;position:absolute;top:50%;width:0}.cropper-center:before,.cropper-center:after{background-color:#eee;content:" ";display:block;position:absolute}.cropper-center:before{height:1px;left:-3px;top:0;width:7px}.cropper-center:after{height:7px;left:0;top:-3px;width:1px}.cropper-face,.cropper-line,.cropper-point{display:block;height:100%;opacity:.1;position:absolute;width:100%}.cropper-face{background-color:#fff;left:0;top:0}.cropper-line{background-color:#39f}.cropper-line.line-e{cursor:ew-resize;right:-3px;top:0;width:5px}.cropper-line.line-n{cursor:ns-resize;height:5px;left:0;top:-3px}.cropper-line.line-w{cursor:ew-resize;left:-3px;top:0;width:5px}.cropper-line.line-s{bottom:-3px;cursor:ns-resize;height:5px;left:0}.cropper-point{background-color:#39f;height:5px;opacity:.75;width:5px}.cropper-point.point-e{cursor:ew-resize;margin-top:-3px;right:-3px;top:50%}.cropper-point.point-n{cursor:ns-resize;left:50%;margin-left:-3px;top:-3px}.cropper-point.point-w{cursor:ew-resize;left:-3px;margin-top:-3px;top:50%}.cropper-point.point-s{bottom:-3px;cursor:s-resize;left:50%;margin-left:-3px}.cropper-point.point-ne{cursor:nesw-resize;right:-3px;top:-3px}.cropper-point.point-nw{cursor:nwse-resize;left:-3px;top:-3px}.cropper-point.point-sw{bottom:-3px;cursor:nesw-resize;left:-3px}.cropper-point.point-se{bottom:-3px;cursor:nwse-resize;height:20px;opacity:1;right:-3px;width:20px}@media (min-width: 768px){.cropper-point.point-se{height:15px;width:15px}}@media (min-width: 992px){.cropper-point.point-se{height:10px;width:10px}}@media (min-width: 1200px){.cropper-point.point-se{height:5px;opacity:.75;width:5px}}.cropper-point.point-se:before{background-color:#39f;bottom:-50%;content:" ";display:block;height:200%;opacity:0;position:absolute;right:-50%;width:200%}.cropper-invisible{opacity:0}.cropper-bg{background-image:url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAQMAAAAlPW0iAAAAA3NCSVQICAjb4U/gAAAABlBMVEXMzMz////TjRV2AAAACXBIWXMAAArrAAAK6wGCiw1aAAAAHHRFWHRTb2Z0d2FyZQBBZG9iZSBGaXJld29ya3MgQ1M26LyyjAAAABFJREFUCJlj+M/AgBVhF/0PAH6/D/HkDxOGAAAAAElFTkSuQmCC)}.cropper-hide{display:block;height:0;position:absolute;width:0}.cropper-hidden{display:none!important}.cropper-move{cursor:move}.cropper-crop{cursor:crosshair}.cropper-disabled .cropper-drag-box,.cropper-disabled .cropper-face,.cropper-disabled .cropper-line,.cropper-disabled .cropper-point{cursor:not-allowed}.placeholder\\:text-gray-400::placeholder{--tw-text-opacity: 1;color:rgb(156 163 175 / var(--tw-text-opacity))}.last\\:mb-0:last-child{margin-bottom:0}.last\\:border-none:last-child{border-style:none}.dark .odd\\:bg-gray-50:nth-child(odd){--tw-bg-opacity: 1;background-color:rgb(17 24 39 / var(--tw-bg-opacity))}.odd\\:bg-gray-50:nth-child(odd){--tw-bg-opacity: 1;background-color:rgb(249 250 251 / var(--tw-bg-opacity))}.checked\\:shadow-inner:checked{--tw-shadow: inset 0 2px 4px 0 rgb(0 0 0 / .05);--tw-shadow-colored: inset 0 2px 4px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.focus-within\\:bg-orange-50:focus-within{--tw-bg-opacity: 1;background-color:rgb(255 242 229 / var(--tw-bg-opacity))}.focus-within\\:ring-1:focus-within{--tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);--tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(1px + var(--tw-ring-offset-width)) var(--tw-ring-color);box-shadow:var(--tw-ring-offset-shadow),var(--tw-ring-shadow),var(--tw-shadow, 0 0 #0000)}.dark .hover\\:text-gray-500:hover{--tw-text-opacity: 1;color:rgb(209 213 219 / var(--tw-text-opacity))}.dark .hover\\:text-gray-700:hover{--tw-text-opacity: 1;color:rgb(229 231 235 / var(--tw-text-opacity))}.dark .hover\\:bg-gray-50:hover{--tw-bg-opacity: 1;background-color:rgb(17 24 39 / var(--tw-bg-opacity))}.hover\\:cursor-none:hover{cursor:none}.hover\\:border-orange-400:hover{--tw-border-opacity: 1;border-color:rgb(255 150 51 / var(--tw-border-opacity))}.hover\\:bg-gray-100:hover{--tw-bg-opacity: 1;background-color:rgb(243 244 246 / var(--tw-bg-opacity))}.hover\\:bg-gray-50:hover{--tw-bg-opacity: 1;background-color:rgb(249 250 251 / var(--tw-bg-opacity))}.hover\\:from-gray-100:hover{--tw-gradient-from: #f3f4f6;--tw-gradient-to: rgb(243 244 246 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.hover\\:to-orange-200\\/90:hover{--tw-gradient-to: rgb(255 216 180 / .9)}.hover\\:to-gray-100\\/90:hover{--tw-gradient-to: rgb(243 244 246 / .9)}.hover\\:to-red-200\\/90:hover{--tw-gradient-to: rgb(254 202 202 / .9)}.hover\\:text-orange-500:hover{--tw-text-opacity: 1;color:rgb(255 124 0 / var(--tw-text-opacity))}.hover\\:text-gray-500:hover{--tw-text-opacity: 1;color:rgb(107 114 128 / var(--tw-text-opacity))}.hover\\:text-gray-700:hover{--tw-text-opacity: 1;color:rgb(55 65 81 / var(--tw-text-opacity))}.hover\\:underline:hover{text-decoration-line:underline}.hover\\:shadow-xl:hover{--tw-shadow: 0 20px 25px -5px rgb(0 0 0 / .1), 0 8px 10px -6px rgb(0 0 0 / .1);--tw-shadow-colored: 0 20px 25px -5px var(--tw-shadow-color), 0 8px 10px -6px var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.hover\\:shadow:hover{--tw-shadow: 0 1px 3px 0 rgb(0 0 0 / .1), 0 1px 2px -1px rgb(0 0 0 / .1);--tw-shadow-colored: 0 1px 3px 0 var(--tw-shadow-color), 0 1px 2px -1px var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.hover\\:ring-1:hover{--tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);--tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(1px + var(--tw-ring-offset-width)) var(--tw-ring-color);box-shadow:var(--tw-ring-offset-shadow),var(--tw-ring-shadow),var(--tw-shadow, 0 0 #0000)}.focus\\:border-blue-300:focus{--tw-border-opacity: 1;border-color:rgb(147 197 253 / var(--tw-border-opacity))}.focus\\:bg-gradient-to-b:focus{background-image:linear-gradient(to bottom,var(--tw-gradient-stops))}.focus\\:from-blue-100:focus{--tw-gradient-from: #dbeafe;--tw-gradient-to: rgb(219 234 254 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.focus\\:to-blue-50:focus{--tw-gradient-to: #eff6ff}.focus\\:ring:focus{--tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);--tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(3px + var(--tw-ring-offset-width)) var(--tw-ring-color);box-shadow:var(--tw-ring-offset-shadow),var(--tw-ring-shadow),var(--tw-shadow, 0 0 #0000)}.focus\\:ring-blue-200:focus{--tw-ring-opacity: 1;--tw-ring-color: rgb(191 219 254 / var(--tw-ring-opacity))}.focus\\:ring-opacity-50:focus{--tw-ring-opacity: .5}.focus\\:ring-offset-0:focus{--tw-ring-offset-width: 0px}.dark .focus\\:odd\\:bg-white:nth-child(odd):focus{background-color:#0b0f19;--tw-bg-opacity: 1;background-color:rgb(11 15 25 / var(--tw-bg-opacity))}.focus\\:odd\\:bg-white:nth-child(odd):focus{--tw-bg-opacity: 1;background-color:rgb(255 255 255 / var(--tw-bg-opacity))}.active\\:shadow-inner:active{--tw-shadow: inset 0 2px 4px 0 rgb(0 0 0 / .05);--tw-shadow-colored: inset 0 2px 4px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.disabled\\:cursor-not-allowed:disabled{cursor:not-allowed}.disabled\\:\\!cursor-not-allowed:disabled{cursor:not-allowed!important}.disabled\\:text-gray-400:disabled{--tw-text-opacity: 1;color:rgb(156 163 175 / var(--tw-text-opacity))}.group:last-child .group-last\\:first\\:rounded-bl-lg:first-child{border-bottom-left-radius:.5rem}.group:last-child .group-last\\:last\\:rounded-br-lg:last-child{border-bottom-right-radius:.5rem}.group:hover .group-hover\\:border-orange-400{--tw-border-opacity: 1;border-color:rgb(255 150 51 / var(--tw-border-opacity))}.group:hover .group-hover\\:from-orange-500{--tw-gradient-from: #FF7C00;--tw-gradient-to: rgb(255 124 0 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.group:hover .group-hover\\:text-orange-500{--tw-text-opacity: 1;color:rgb(255 124 0 / var(--tw-text-opacity))}.dark .dark\\:bg-gray-950{background-color:#0b0f19}.dark .dark\\:divide-gray-700>:not([hidden])~:not([hidden]){--tw-divide-opacity: 1;border-color:rgb(55 65 81 / var(--tw-divide-opacity))}.dark .dark\\:border-gray-800{--tw-border-opacity: 1;border-color:rgb(31 41 55 / var(--tw-border-opacity))}.dark .dark\\:border-gray-700{--tw-border-opacity: 1;border-color:rgb(55 65 81 / var(--tw-border-opacity))}.dark .dark\\:border-gray-600{--tw-border-opacity: 1;border-color:rgb(75 85 99 / var(--tw-border-opacity))}.dark .dark\\:\\!border-red-900{--tw-border-opacity: 1 !important;border-color:rgb(127 29 29 / var(--tw-border-opacity))!important}.dark .dark\\:\\!border-yellow-900{--tw-border-opacity: 1 !important;border-color:rgb(113 63 18 / var(--tw-border-opacity))!important}.dark .dark\\:\\!border-green-900{--tw-border-opacity: 1 !important;border-color:rgb(20 83 45 / var(--tw-border-opacity))!important}.dark .dark\\:\\!border-blue-900{--tw-border-opacity: 1 !important;border-color:rgb(30 58 138 / var(--tw-border-opacity))!important}.dark .dark\\:\\!border-purple-900{--tw-border-opacity: 1 !important;border-color:rgb(88 28 135 / var(--tw-border-opacity))!important}.dark .dark\\:\\!border-gray-900{--tw-border-opacity: 1 !important;border-color:rgb(17 24 39 / var(--tw-border-opacity))!important}.dark .dark\\:\\!border-pink-900{--tw-border-opacity: 1 !important;border-color:rgb(131 24 67 / var(--tw-border-opacity))!important}.dark .dark\\:\\!bg-red-700{--tw-bg-opacity: 1 !important;background-color:rgb(185 28 28 / var(--tw-bg-opacity))!important}.dark .dark\\:\\!bg-green-700{--tw-bg-opacity: 1 !important;background-color:rgb(21 128 61 / var(--tw-bg-opacity))!important}.dark .dark\\:\\!bg-blue-700{--tw-bg-opacity: 1 !important;background-color:rgb(29 78 216 / var(--tw-bg-opacity))!important}.dark .dark\\:\\!bg-yellow-700{--tw-bg-opacity: 1 !important;background-color:rgb(161 98 7 / var(--tw-bg-opacity))!important}.dark .dark\\:\\!bg-purple-700{--tw-bg-opacity: 1 !important;background-color:rgb(126 34 206 / var(--tw-bg-opacity))!important}.dark .dark\\:\\!bg-teal-700{--tw-bg-opacity: 1 !important;background-color:rgb(15 118 110 / var(--tw-bg-opacity))!important}.dark .dark\\:\\!bg-orange-700{--tw-bg-opacity: 1 !important;background-color:rgb(206 100 0 / var(--tw-bg-opacity))!important}.dark .dark\\:\\!bg-cyan-700{--tw-bg-opacity: 1 !important;background-color:rgb(14 116 144 / var(--tw-bg-opacity))!important}.dark .dark\\:\\!bg-lime-700{--tw-bg-opacity: 1 !important;background-color:rgb(77 124 15 / var(--tw-bg-opacity))!important}.dark .dark\\:\\!bg-pink-700{--tw-bg-opacity: 1 !important;background-color:rgb(190 24 93 / var(--tw-bg-opacity))!important}.dark .dark\\:\\!bg-gray-700{--tw-bg-opacity: 1 !important;background-color:rgb(55 65 81 / var(--tw-bg-opacity))!important}.dark .dark\\:bg-gray-900{--tw-bg-opacity: 1;background-color:rgb(17 24 39 / var(--tw-bg-opacity))}.dark .dark\\:bg-gray-800{--tw-bg-opacity: 1;background-color:rgb(31 41 55 / var(--tw-bg-opacity))}.dark .dark\\:bg-transparent{background-color:transparent}.dark .dark\\:bg-gray-600{--tw-bg-opacity: 1;background-color:rgb(75 85 99 / var(--tw-bg-opacity))}.dark .dark\\:bg-gray-950{--tw-bg-opacity: 1;background-color:rgb(11 15 25 / var(--tw-bg-opacity))}.dark .dark\\:bg-gray-700{--tw-bg-opacity: 1;background-color:rgb(55 65 81 / var(--tw-bg-opacity))}.dark .dark\\:bg-opacity-80{--tw-bg-opacity: .8}.dark .dark\\:from-orange-400{--tw-gradient-from: #FF9633;--tw-gradient-to: rgb(255 150 51 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.dark .dark\\:from-gray-900{--tw-gradient-from: #111827;--tw-gradient-to: rgb(17 24 39 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.dark .dark\\:from-orange-700{--tw-gradient-from: #CE6400;--tw-gradient-to: rgb(206 100 0 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.dark .dark\\:from-gray-600{--tw-gradient-from: #4b5563;--tw-gradient-to: rgb(75 85 99 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.dark .dark\\:from-red-700{--tw-gradient-from: #b91c1c;--tw-gradient-to: rgb(185 28 28 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.dark .dark\\:\\!from-red-700{--tw-gradient-from: #b91c1c !important;--tw-gradient-to: rgb(185 28 28 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.dark .dark\\:\\!from-yellow-700{--tw-gradient-from: #a16207 !important;--tw-gradient-to: rgb(161 98 7 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.dark .dark\\:\\!from-green-700{--tw-gradient-from: #15803d !important;--tw-gradient-to: rgb(21 128 61 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.dark .dark\\:\\!from-blue-700{--tw-gradient-from: #1d4ed8 !important;--tw-gradient-to: rgb(29 78 216 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.dark .dark\\:\\!from-purple-700{--tw-gradient-from: #7e22ce !important;--tw-gradient-to: rgb(126 34 206 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.dark .dark\\:\\!from-gray-700{--tw-gradient-from: #374151 !important;--tw-gradient-to: rgb(55 65 81 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.dark .dark\\:\\!from-pink-700{--tw-gradient-from: #be185d !important;--tw-gradient-to: rgb(190 24 93 / 0) !important;--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) !important}.dark .dark\\:to-orange-600{--tw-gradient-to: #EE7400}.dark .dark\\:to-gray-800{--tw-gradient-to: #1f2937}.dark .dark\\:to-orange-700{--tw-gradient-to: #CE6400}.dark .dark\\:to-gray-700{--tw-gradient-to: #374151}.dark .dark\\:to-red-700{--tw-gradient-to: #b91c1c}.dark .dark\\:\\!to-red-800{--tw-gradient-to: #991b1b !important}.dark .dark\\:\\!to-yellow-800{--tw-gradient-to: #854d0e !important}.dark .dark\\:\\!to-green-800{--tw-gradient-to: #166534 !important}.dark .dark\\:\\!to-blue-800{--tw-gradient-to: #1e40af !important}.dark .dark\\:\\!to-purple-800{--tw-gradient-to: #6b21a8 !important}.dark .dark\\:\\!to-gray-800{--tw-gradient-to: #1f2937 !important}.dark .dark\\:\\!to-pink-800{--tw-gradient-to: #9d174d !important}.dark .dark\\:fill-slate-200{fill:#e2e8f0}.dark .dark\\:\\!text-red-300{--tw-text-opacity: 1 !important;color:rgb(252 165 165 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-green-300{--tw-text-opacity: 1 !important;color:rgb(134 239 172 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-blue-300{--tw-text-opacity: 1 !important;color:rgb(147 197 253 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-yellow-300{--tw-text-opacity: 1 !important;color:rgb(253 224 71 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-purple-300{--tw-text-opacity: 1 !important;color:rgb(216 180 254 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-teal-300{--tw-text-opacity: 1 !important;color:rgb(94 234 212 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-orange-300{--tw-text-opacity: 1 !important;color:rgb(255 176 102 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-cyan-300{--tw-text-opacity: 1 !important;color:rgb(103 232 249 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-lime-300{--tw-text-opacity: 1 !important;color:rgb(190 242 100 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-pink-300{--tw-text-opacity: 1 !important;color:rgb(249 168 212 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-gray-300{--tw-text-opacity: 1 !important;color:rgb(209 213 219 / var(--tw-text-opacity))!important}.dark .dark\\:\\!text-gray-50{--tw-text-opacity: 1 !important;color:rgb(249 250 251 / var(--tw-text-opacity))!important}.dark .dark\\:text-gray-200{--tw-text-opacity: 1;color:rgb(229 231 235 / var(--tw-text-opacity))}.dark .dark\\:text-white{--tw-text-opacity: 1;color:rgb(255 255 255 / var(--tw-text-opacity))}.dark .dark\\:text-slate-200{--tw-text-opacity: 1;color:rgb(226 232 240 / var(--tw-text-opacity))}.dark .dark\\:text-indigo-300{--tw-text-opacity: 1;color:rgb(165 180 252 / var(--tw-text-opacity))}.dark .dark\\:text-green-400{--tw-text-opacity: 1;color:rgb(74 222 128 / var(--tw-text-opacity))}.dark .dark\\:text-gray-400{--tw-text-opacity: 1;color:rgb(156 163 175 / var(--tw-text-opacity))}.dark .dark\\:text-slate-300{--tw-text-opacity: 1;color:rgb(203 213 225 / var(--tw-text-opacity))}.dark .dark\\:text-red-100{--tw-text-opacity: 1;color:rgb(254 226 226 / var(--tw-text-opacity))}.dark .dark\\:text-yellow-100{--tw-text-opacity: 1;color:rgb(254 249 195 / var(--tw-text-opacity))}.dark .dark\\:text-green-100{--tw-text-opacity: 1;color:rgb(220 252 231 / var(--tw-text-opacity))}.dark .dark\\:text-blue-100{--tw-text-opacity: 1;color:rgb(219 234 254 / var(--tw-text-opacity))}.dark .dark\\:text-purple-100{--tw-text-opacity: 1;color:rgb(243 232 255 / var(--tw-text-opacity))}.dark .dark\\:text-gray-50{--tw-text-opacity: 1;color:rgb(249 250 251 / var(--tw-text-opacity))}.dark .dark\\:ring-gray-600{--tw-ring-opacity: 1;--tw-ring-color: rgb(75 85 99 / var(--tw-ring-opacity))}.dark .dark\\:invert{--tw-invert: invert(100%);filter:var(--tw-blur) var(--tw-brightness) var(--tw-contrast) var(--tw-grayscale) var(--tw-hue-rotate) var(--tw-invert) var(--tw-saturate) var(--tw-sepia) var(--tw-drop-shadow)}.dark .dark .dark\\:placeholder\\:text-gray-500::placeholder{--tw-text-opacity: 1;color:rgb(209 213 219 / var(--tw-text-opacity))}.dark .dark\\:placeholder\\:text-gray-500::placeholder{--tw-text-opacity: 1;color:rgb(107 114 128 / var(--tw-text-opacity))}.dark .dark\\:odd\\:bg-gray-900:nth-child(odd){--tw-bg-opacity: 1;background-color:rgb(17 24 39 / var(--tw-bg-opacity))}.dark .dark\\:focus-within\\:bg-gray-800:focus-within{--tw-bg-opacity: 1;background-color:rgb(31 41 55 / var(--tw-bg-opacity))}.dark .dark\\:hover\\:border-orange-700:hover{--tw-border-opacity: 1;border-color:rgb(206 100 0 / var(--tw-border-opacity))}.dark .dark\\:hover\\:bg-gray-800:hover{--tw-bg-opacity: 1;background-color:rgb(31 41 55 / var(--tw-bg-opacity))}.dark .dark\\:hover\\:to-orange-500:hover{--tw-gradient-to: #FF7C00}.dark .dark\\:hover\\:to-gray-600:hover{--tw-gradient-to: #4b5563}.dark .dark\\:hover\\:to-red-500:hover{--tw-gradient-to: #ef4444}.dark .dark\\:focus\\:border-gray-600:focus{--tw-border-opacity: 1;border-color:rgb(75 85 99 / var(--tw-border-opacity))}.dark .dark\\:focus\\:from-blue-900:focus{--tw-gradient-from: #1e3a8a;--tw-gradient-to: rgb(30 58 138 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to)}.dark .dark\\:focus\\:to-gray-900:focus{--tw-gradient-to: #111827}.dark .dark\\:focus\\:ring-0:focus{--tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);--tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(0px + var(--tw-ring-offset-width)) var(--tw-ring-color);box-shadow:var(--tw-ring-offset-shadow),var(--tw-ring-shadow),var(--tw-shadow, 0 0 #0000)}.dark .dark\\:focus\\:ring-gray-700:focus{--tw-ring-opacity: 1;--tw-ring-color: rgb(55 65 81 / var(--tw-ring-opacity))}@media (min-width: 640px){.sm\\:grid-cols-1{grid-template-columns:repeat(1,minmax(0,1fr))}.sm\\:grid-cols-2{grid-template-columns:repeat(2,minmax(0,1fr))}.sm\\:grid-cols-3{grid-template-columns:repeat(3,minmax(0,1fr))}.sm\\:grid-cols-4{grid-template-columns:repeat(4,minmax(0,1fr))}.sm\\:grid-cols-5{grid-template-columns:repeat(5,minmax(0,1fr))}.sm\\:grid-cols-6{grid-template-columns:repeat(6,minmax(0,1fr))}.sm\\:grid-cols-7{grid-template-columns:repeat(7,minmax(0,1fr))}.sm\\:grid-cols-8{grid-template-columns:repeat(8,minmax(0,1fr))}.sm\\:grid-cols-9{grid-template-columns:repeat(9,minmax(0,1fr))}.sm\\:grid-cols-10{grid-template-columns:repeat(10,minmax(0,1fr))}.sm\\:grid-cols-11{grid-template-columns:repeat(11,minmax(0,1fr))}.sm\\:grid-cols-12{grid-template-columns:repeat(12,minmax(0,1fr))}}@media (min-width: 768px){.md\\:bottom-4{bottom:1rem}.md\\:min-h-\\[15rem\\]{min-height:15rem}.md\\:grid-cols-1{grid-template-columns:repeat(1,minmax(0,1fr))}.md\\:grid-cols-2{grid-template-columns:repeat(2,minmax(0,1fr))}.md\\:grid-cols-3{grid-template-columns:repeat(3,minmax(0,1fr))}.md\\:grid-cols-4{grid-template-columns:repeat(4,minmax(0,1fr))}.md\\:grid-cols-5{grid-template-columns:repeat(5,minmax(0,1fr))}.md\\:grid-cols-6{grid-template-columns:repeat(6,minmax(0,1fr))}.md\\:grid-cols-7{grid-template-columns:repeat(7,minmax(0,1fr))}.md\\:grid-cols-8{grid-template-columns:repeat(8,minmax(0,1fr))}.md\\:grid-cols-9{grid-template-columns:repeat(9,minmax(0,1fr))}.md\\:grid-cols-10{grid-template-columns:repeat(10,minmax(0,1fr))}.md\\:grid-cols-11{grid-template-columns:repeat(11,minmax(0,1fr))}.md\\:grid-cols-12{grid-template-columns:repeat(12,minmax(0,1fr))}.md\\:text-xl{font-size:1.25rem;line-height:1.75rem}}@media (min-width: 1024px){.lg\\:grid-cols-1{grid-template-columns:repeat(1,minmax(0,1fr))}.lg\\:grid-cols-2{grid-template-columns:repeat(2,minmax(0,1fr))}.lg\\:grid-cols-3{grid-template-columns:repeat(3,minmax(0,1fr))}.lg\\:grid-cols-4{grid-template-columns:repeat(4,minmax(0,1fr))}.lg\\:grid-cols-5{grid-template-columns:repeat(5,minmax(0,1fr))}.lg\\:grid-cols-6{grid-template-columns:repeat(6,minmax(0,1fr))}.lg\\:grid-cols-7{grid-template-columns:repeat(7,minmax(0,1fr))}.lg\\:grid-cols-8{grid-template-columns:repeat(8,minmax(0,1fr))}.lg\\:grid-cols-9{grid-template-columns:repeat(9,minmax(0,1fr))}.lg\\:grid-cols-10{grid-template-columns:repeat(10,minmax(0,1fr))}.lg\\:grid-cols-11{grid-template-columns:repeat(11,minmax(0,1fr))}.lg\\:grid-cols-12{grid-template-columns:repeat(12,minmax(0,1fr))}}@media (min-width: 1280px){.xl\\:bottom-8{bottom:2rem}.xl\\:max-h-\\[18rem\\]{max-height:18rem}.xl\\:min-h-\\[450px\\]{min-height:450px}.xl\\:grid-cols-1{grid-template-columns:repeat(1,minmax(0,1fr))}.xl\\:grid-cols-2{grid-template-columns:repeat(2,minmax(0,1fr))}.xl\\:grid-cols-3{grid-template-columns:repeat(3,minmax(0,1fr))}.xl\\:grid-cols-4{grid-template-columns:repeat(4,minmax(0,1fr))}.xl\\:grid-cols-5{grid-template-columns:repeat(5,minmax(0,1fr))}.xl\\:grid-cols-6{grid-template-columns:repeat(6,minmax(0,1fr))}.xl\\:grid-cols-7{grid-template-columns:repeat(7,minmax(0,1fr))}.xl\\:grid-cols-8{grid-template-columns:repeat(8,minmax(0,1fr))}.xl\\:grid-cols-9{grid-template-columns:repeat(9,minmax(0,1fr))}.xl\\:grid-cols-10{grid-template-columns:repeat(10,minmax(0,1fr))}.xl\\:grid-cols-11{grid-template-columns:repeat(11,minmax(0,1fr))}.xl\\:grid-cols-12{grid-template-columns:repeat(12,minmax(0,1fr))}}@media (min-width: 1536px){.\\32xl\\:max-h-\\[20rem\\]{max-height:20rem}.\\32xl\\:grid-cols-1{grid-template-columns:repeat(1,minmax(0,1fr))}.\\32xl\\:grid-cols-2{grid-template-columns:repeat(2,minmax(0,1fr))}.\\32xl\\:grid-cols-3{grid-template-columns:repeat(3,minmax(0,1fr))}.\\32xl\\:grid-cols-4{grid-template-columns:repeat(4,minmax(0,1fr))}.\\32xl\\:grid-cols-5{grid-template-columns:repeat(5,minmax(0,1fr))}.\\32xl\\:grid-cols-6{grid-template-columns:repeat(6,minmax(0,1fr))}.\\32xl\\:grid-cols-7{grid-template-columns:repeat(7,minmax(0,1fr))}.\\32xl\\:grid-cols-8{grid-template-columns:repeat(8,minmax(0,1fr))}.\\32xl\\:grid-cols-9{grid-template-columns:repeat(9,minmax(0,1fr))}.\\32xl\\:grid-cols-10{grid-template-columns:repeat(10,minmax(0,1fr))}.\\32xl\\:grid-cols-11{grid-template-columns:repeat(11,minmax(0,1fr))}.\\32xl\\:grid-cols-12{grid-template-columns:repeat(12,minmax(0,1fr))}}
`;
var tokens = ".gr-box{position:relative;border-radius:.5rem;--tw-bg-opacity: 1;background-color:rgb(255 255 255 / var(--tw-bg-opacity));font-size:.875rem;line-height:1.25rem;--tw-text-opacity: 1;color:rgb(55 65 81 / var(--tw-text-opacity));--tw-shadow: 0 1px 2px 0 rgb(0 0 0 / .05);--tw-shadow-colored: 0 1px 2px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.dark .gr-box{--tw-bg-opacity: 1;background-color:rgb(31 41 55 / var(--tw-bg-opacity))}.gr-box-unrounded{position:relative;--tw-bg-opacity: 1;background-color:rgb(255 255 255 / var(--tw-bg-opacity));font-size:.875rem;line-height:1.25rem;--tw-text-opacity: 1;color:rgb(55 65 81 / var(--tw-text-opacity));--tw-shadow: 0 1px 2px 0 rgb(0 0 0 / .05);--tw-shadow-colored: 0 1px 2px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.dark .gr-box-unrounded{--tw-bg-opacity: 1;background-color:rgb(31 41 55 / var(--tw-bg-opacity))}.gr-input{--tw-border-opacity: 1;border-color:rgb(229 231 235 / var(--tw-border-opacity))}.gr-input::placeholder{--tw-text-opacity: 1;color:rgb(156 163 175 / var(--tw-text-opacity))}.gr-input:checked{--tw-shadow: inset 0 2px 4px 0 rgb(0 0 0 / .05);--tw-shadow-colored: inset 0 2px 4px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.gr-input:focus{--tw-border-opacity: 1;border-color:rgb(147 197 253 / var(--tw-border-opacity));--tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);--tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(3px + var(--tw-ring-offset-width)) var(--tw-ring-color);box-shadow:var(--tw-ring-offset-shadow),var(--tw-ring-shadow),var(--tw-shadow, 0 0 #0000);--tw-ring-color: rgb(191 219 254 / var(--tw-ring-opacity));--tw-ring-opacity: .5 }.dark .gr-input{--tw-border-opacity: 1;border-color:rgb(55 65 81 / var(--tw-border-opacity));--tw-text-opacity: 1;color:rgb(229 231 235 / var(--tw-text-opacity))}.dark .gr-input::placeholder{--tw-text-opacity: 1;color:rgb(107 114 128 / var(--tw-text-opacity))}.dark .gr-input:focus{--tw-border-opacity: 1;border-color:rgb(75 85 99 / var(--tw-border-opacity));--tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);--tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(0px + var(--tw-ring-offset-width)) var(--tw-ring-color);box-shadow:var(--tw-ring-offset-shadow),var(--tw-ring-shadow),var(--tw-shadow, 0 0 #0000)}.gr-label{margin-bottom:.5rem;display:block;font-size:.875rem;line-height:1.25rem;--tw-text-opacity: 1;color:rgb(75 85 99 / var(--tw-text-opacity))}.gr-padded{padding:.625rem .75rem}.gr-panel{border-radius:.5rem;--tw-bg-opacity: 1;background-color:rgb(249 250 251 / var(--tw-bg-opacity));padding:.5rem}.dark .gr-panel{--tw-bg-opacity: 1;background-color:rgb(17 24 39 / var(--tw-bg-opacity))}.gr-box-sm>:not([hidden])~:not([hidden]){--tw-space-x-reverse: 0;margin-right:calc(.5rem * var(--tw-space-x-reverse));margin-left:calc(.5rem * calc(1 - var(--tw-space-x-reverse)))}.gr-box-sm{border-width:1px;padding:.375rem .75rem}.gr-compact{align-items:stretch;gap:0px;overflow:clip;border-radius:.5rem!important;border-width:1px;--tw-border-opacity: 1;border-color:rgb(229 231 235 / var(--tw-border-opacity))}.dark .gr-compact{--tw-border-opacity: 1;border-color:rgb(55 65 81 / var(--tw-border-opacity))}.gr-text-input{padding:.625rem;--tw-shadow: inset 0 2px 4px 0 rgb(0 0 0 / .05);--tw-shadow-colored: inset 0 2px 4px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.gr-text-input:disabled{--tw-shadow: 0 0 #0000;--tw-shadow-colored: 0 0 #0000;box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.gr-check-radio{--tw-border-opacity: 1;border-color:rgb(209 213 219 / var(--tw-border-opacity));--tw-text-opacity: 1;color:rgb(37 99 235 / var(--tw-text-opacity));--tw-shadow: 0 1px 2px 0 rgb(0 0 0 / .05);--tw-shadow-colored: 0 1px 2px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.gr-check-radio:focus{--tw-border-opacity: 1;border-color:rgb(147 197 253 / var(--tw-border-opacity));--tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);--tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(3px + var(--tw-ring-offset-width)) var(--tw-ring-color);box-shadow:var(--tw-ring-offset-shadow),var(--tw-ring-shadow),var(--tw-shadow, 0 0 #0000);--tw-ring-color: rgb(191 219 254 / var(--tw-ring-opacity));--tw-ring-opacity: .5;--tw-ring-offset-width: 0px }.gr-check-radio:disabled{cursor:not-allowed!important;--tw-text-opacity: 1;color:rgb(156 163 175 / var(--tw-text-opacity))}.dark .gr-check-radio{--tw-border-opacity: 1;border-color:rgb(55 65 81 / var(--tw-border-opacity));--tw-bg-opacity: 1;background-color:rgb(17 24 39 / var(--tw-bg-opacity))}.dark .gr-check-radio:checked{--tw-bg-opacity: 1;background-color:rgb(37 99 235 / var(--tw-bg-opacity))}.dark .gr-check-radio:focus{--tw-ring-opacity: 1;--tw-ring-color: rgb(55 65 81 / var(--tw-ring-opacity)) }.gr-checkbox{border-radius:.25rem}.gr-input-label{background-image:linear-gradient(to top,var(--tw-gradient-stops));--tw-gradient-from: #f9fafb;--tw-gradient-to: rgb(249 250 251 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to);--tw-gradient-to: #fff;transition-property:color,background-color,border-color,text-decoration-color,fill,stroke,opacity,box-shadow,transform,filter,backdrop-filter;transition-timing-function:cubic-bezier(.4,0,.2,1);transition-duration:.15s}.gr-input-label:hover{--tw-gradient-from: #f3f4f6;--tw-gradient-to: rgb(243 244 246 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to) }.dark .gr-input-label{--tw-gradient-from: #111827;--tw-gradient-to: rgb(17 24 39 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to);--tw-gradient-to: #1f2937 }.gr-radio{border-radius:9999px}.gr-button{display:inline-flex;align-items:center;justify-content:center;border-radius:.25rem;border-width:1px;--tw-bg-opacity: 1;background-color:rgb(255 255 255 / var(--tw-bg-opacity));background-image:linear-gradient(to bottom right,var(--tw-gradient-stops));padding:.125rem .5rem;text-align:center;font-size:.875rem;line-height:1.25rem;--tw-shadow: 0 1px 2px 0 rgb(0 0 0 / .05);--tw-shadow-colored: 0 1px 2px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.gr-button:hover{--tw-shadow: 0 1px 3px 0 rgb(0 0 0 / .1), 0 1px 2px -1px rgb(0 0 0 / .1);--tw-shadow-colored: 0 1px 3px 0 var(--tw-shadow-color), 0 1px 2px -1px var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.gr-button:active{--tw-shadow: inset 0 2px 4px 0 rgb(0 0 0 / .05);--tw-shadow-colored: inset 0 2px 4px 0 var(--tw-shadow-color);box-shadow:var(--tw-ring-offset-shadow, 0 0 #0000),var(--tw-ring-shadow, 0 0 #0000),var(--tw-shadow)}.dark .gr-button{--tw-border-opacity: 1;border-color:rgb(75 85 99 / var(--tw-border-opacity));--tw-bg-opacity: 1;background-color:rgb(55 65 81 / var(--tw-bg-opacity))}.gr-button-primary{--tw-border-opacity: 1;border-color:rgb(255 216 180 / var(--tw-border-opacity));--tw-gradient-from: rgb(255 216 180 / .7);--tw-gradient-to: rgb(255 216 180 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to);--tw-gradient-to: rgb(255 176 102 / .8);--tw-text-opacity: 1;color:rgb(238 116 0 / var(--tw-text-opacity))}.gr-button-primary:hover{--tw-gradient-to: rgb(255 216 180 / .9) }.dark .gr-button-primary{--tw-border-opacity: 1;border-color:rgb(238 116 0 / var(--tw-border-opacity));--tw-gradient-from: #CE6400;--tw-gradient-to: rgb(206 100 0 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to);--tw-gradient-to: #CE6400;--tw-text-opacity: 1;color:rgb(255 255 255 / var(--tw-text-opacity))}.dark .gr-button-primary:hover{--tw-gradient-to: #FF7C00 }.gr-button-secondary{--tw-border-opacity: 1;border-color:rgb(229 231 235 / var(--tw-border-opacity));--tw-gradient-from: rgb(243 244 246 / .7);--tw-gradient-to: rgb(243 244 246 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to);--tw-gradient-to: rgb(229 231 235 / .8);--tw-text-opacity: 1;color:rgb(55 65 81 / var(--tw-text-opacity))}.gr-button-secondary:hover{--tw-gradient-to: rgb(243 244 246 / .9) }.dark .gr-button-secondary{--tw-border-opacity: 1;border-color:rgb(75 85 99 / var(--tw-border-opacity));--tw-gradient-from: #4b5563;--tw-gradient-to: rgb(75 85 99 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to);--tw-gradient-to: #374151;--tw-text-opacity: 1;color:rgb(255 255 255 / var(--tw-text-opacity))}.dark .gr-button-secondary:hover{--tw-gradient-to: #4b5563 }.gr-button-stop{--tw-border-opacity: 1;border-color:rgb(254 202 202 / var(--tw-border-opacity));--tw-gradient-from: rgb(254 202 202 / .7);--tw-gradient-to: rgb(254 202 202 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to);--tw-gradient-to: rgb(252 165 165 / .8);--tw-text-opacity: 1;color:rgb(220 38 38 / var(--tw-text-opacity))}.gr-button-stop:hover{--tw-gradient-to: rgb(254 202 202 / .9) }.dark .gr-button-stop{--tw-border-opacity: 1;border-color:rgb(220 38 38 / var(--tw-border-opacity));--tw-gradient-from: #b91c1c;--tw-gradient-to: rgb(185 28 28 / 0);--tw-gradient-stops: var(--tw-gradient-from), var(--tw-gradient-to);--tw-gradient-to: #b91c1c;--tw-text-opacity: 1;color:rgb(255 255 255 / var(--tw-text-opacity))}.dark .gr-button-stop:hover{--tw-gradient-to: #ef4444 }.gr-button-sm{border-radius:.375rem;padding:.25rem .75rem;font-size:.875rem;line-height:1.25rem}.gr-button-lg{border-radius:.5rem;padding:.5rem 1rem;font-size:1rem;line-height:1.5rem;font-weight:600}.gr-samples-table{width:100%}.gr-samples-table img.gr-sample-image,.gr-samples-table video.gr-sample-video{height:5rem;width:5rem;object-fit:cover}.gr-samples-gallery{display:flex;flex-wrap:wrap;gap:.5rem}.gr-samples-gallery img.gr-sample-image,.gr-samples-gallery video.gr-sample-video{max-height:5rem;object-fit:cover}.gr-samples-gallery .gr-sample-textbox,.gr-samples-gallery .gr-sample-markdown,.gr-samples-gallery .gr-sample-html,.gr-samples-gallery .gr-sample-slider,.gr-samples-gallery .gr-sample-checkbox,.gr-samples-gallery .gr-sample-checkboxgroup,.gr-samples-gallery .gr-sample-file,.gr-samples-gallery .gr-sample-number,.gr-samples-gallery .gr-sample-audio,.gr-samples-gallery .gr-sample-3d{display:flex;cursor:pointer;align-items:center;border-radius:.5rem;border-width:1px;--tw-bg-opacity: 1;background-color:rgb(255 255 255 / var(--tw-bg-opacity));padding:.375rem .5rem;text-align:left;font-size:.875rem;line-height:1.25rem}.gr-samples-gallery .gr-sample-textbox:hover,.gr-samples-gallery .gr-sample-markdown:hover,.gr-samples-gallery .gr-sample-html:hover,.gr-samples-gallery .gr-sample-slider:hover,.gr-samples-gallery .gr-sample-checkbox:hover,.gr-samples-gallery .gr-sample-checkboxgroup:hover,.gr-samples-gallery .gr-sample-file:hover,.gr-samples-gallery .gr-sample-number:hover,.gr-samples-gallery .gr-sample-audio:hover,.gr-samples-gallery .gr-sample-3d:hover{--tw-bg-opacity: 1;background-color:rgb(249 250 251 / var(--tw-bg-opacity))}.dark .gr-samples-gallery .gr-sample-textbox,.dark .gr-samples-gallery .gr-sample-markdown,.dark .gr-samples-gallery .gr-sample-html,.dark .gr-samples-gallery .gr-sample-slider,.dark .gr-samples-gallery .gr-sample-checkbox,.dark .gr-samples-gallery .gr-sample-checkboxgroup,.dark .gr-samples-gallery .gr-sample-file,.dark .gr-samples-gallery .gr-sample-number,.dark .gr-samples-gallery .gr-sample-audio,.dark .gr-samples-gallery .gr-sample-3d{background-color:transparent}.dark .gr-samples-gallery .gr-sample-textbox:hover,.dark .gr-samples-gallery .gr-sample-markdown:hover,.dark .gr-samples-gallery .gr-sample-html:hover,.dark .gr-samples-gallery .gr-sample-slider:hover,.dark .gr-samples-gallery .gr-sample-checkbox:hover,.dark .gr-samples-gallery .gr-sample-checkboxgroup:hover,.dark .gr-samples-gallery .gr-sample-file:hover,.dark .gr-samples-gallery .gr-sample-number:hover,.dark .gr-samples-gallery .gr-sample-audio:hover,.dark .gr-samples-gallery .gr-sample-3d:hover{--tw-bg-opacity: 1;background-color:rgb(31 41 55 / var(--tw-bg-opacity))}.gr-samples-gallery .gr-sample-dataframe{border-collapse:collapse;cursor:pointer;align-items:center;overflow:hidden;border-radius:.5rem;border-width:1px;--tw-bg-opacity: 1;background-color:rgb(255 255 255 / var(--tw-bg-opacity));padding:.375rem .5rem;text-align:left;font-size:.875rem;line-height:1.25rem}.gr-samples-gallery .gr-sample-dataframe:hover{--tw-bg-opacity: 1;background-color:rgb(249 250 251 / var(--tw-bg-opacity))}.dark .gr-samples-gallery .gr-sample-dataframe{background-color:transparent}.dark .gr-samples-gallery .gr-sample-dataframe:hover{--tw-bg-opacity: 1;background-color:rgb(31 41 55 / var(--tw-bg-opacity))}img.gr-sample-image,video.gr-sample-video{max-width:none;flex:none;border-radius:.5rem;border-width:2px;--tw-border-opacity: 1;border-color:rgb(229 231 235 / var(--tw-border-opacity))}img.gr-sample-image:hover,video.gr-sample-video:hover{--tw-border-opacity: 1;border-color:rgb(255 150 51 / var(--tw-border-opacity))}.group:hover img.gr-sample-image,.group:hover video.gr-sample-video{--tw-border-opacity: 1;border-color:rgb(255 150 51 / var(--tw-border-opacity))}.dark img.gr-sample-image,.dark video.gr-sample-video{--tw-border-opacity: 1;border-color:rgb(55 65 81 / var(--tw-border-opacity))}.dark img.gr-sample-image:hover,.dark video.gr-sample-video:hover{--tw-border-opacity: 1;border-color:rgb(206 100 0 / var(--tw-border-opacity))}.dark .group:hover img.gr-sample-image,.dark .group:hover video.gr-sample-video{--tw-border-opacity: 1;border-color:rgb(206 100 0 / var(--tw-border-opacity))}\n";
var colors$2 = {};
var log$1 = {};
var picocolors_browser = { exports: {} };
var x = String;
var create2 = function() {
  return { isColorSupported: false, reset: x, bold: x, dim: x, italic: x, underline: x, inverse: x, hidden: x, strikethrough: x, black: x, red: x, green: x, yellow: x, blue: x, magenta: x, cyan: x, white: x, gray: x, bgBlack: x, bgRed: x, bgGreen: x, bgYellow: x, bgBlue: x, bgMagenta: x, bgCyan: x, bgWhite: x };
};
picocolors_browser.exports = create2();
picocolors_browser.exports.createColors = create2;
Object.defineProperty(log$1, "__esModule", {
  value: true
});
log$1.dim = dim;
log$1.default = void 0;
var _picocolors = _interopRequireDefault$1(picocolors_browser.exports);
function _interopRequireDefault$1(obj) {
  return obj && obj.__esModule ? obj : {
    default: obj
  };
}
let alreadyShown = /* @__PURE__ */ new Set();
function log(type, messages, key) {
  if (typeof process !== "undefined" && {}.JEST_WORKER_ID)
    return;
  if (key && alreadyShown.has(key))
    return;
  if (key)
    alreadyShown.add(key);
  console.warn("");
  messages.forEach((message) => console.warn(type, "-", message));
}
function dim(input) {
  return _picocolors.default.dim(input);
}
var _default$1 = {
  info(key, messages) {
    log(_picocolors.default.bold(_picocolors.default.cyan("info")), ...Array.isArray(key) ? [
      key
    ] : [
      messages,
      key
    ]);
  },
  warn(key, messages) {
    log(_picocolors.default.bold(_picocolors.default.yellow("warn")), ...Array.isArray(key) ? [
      key
    ] : [
      messages,
      key
    ]);
  },
  risk(key, messages) {
    log(_picocolors.default.bold(_picocolors.default.magenta("risk")), ...Array.isArray(key) ? [
      key
    ] : [
      messages,
      key
    ]);
  }
};
log$1.default = _default$1;
Object.defineProperty(colors$2, "__esModule", {
  value: true
});
colors$2.default = void 0;
var _log = _interopRequireDefault(log$1);
function _interopRequireDefault(obj) {
  return obj && obj.__esModule ? obj : {
    default: obj
  };
}
function warn({ version, from, to }) {
  _log.default.warn(`${from}-color-renamed`, [
    `As of Tailwind CSS ${version}, \`${from}\` has been renamed to \`${to}\`.`,
    "Update your configuration file to silence this warning."
  ]);
}
var _default = {
  inherit: "inherit",
  current: "currentColor",
  transparent: "transparent",
  black: "#000",
  white: "#fff",
  slate: {
    50: "#f8fafc",
    100: "#f1f5f9",
    200: "#e2e8f0",
    300: "#cbd5e1",
    400: "#94a3b8",
    500: "#64748b",
    600: "#475569",
    700: "#334155",
    800: "#1e293b",
    900: "#0f172a"
  },
  gray: {
    50: "#f9fafb",
    100: "#f3f4f6",
    200: "#e5e7eb",
    300: "#d1d5db",
    400: "#9ca3af",
    500: "#6b7280",
    600: "#4b5563",
    700: "#374151",
    800: "#1f2937",
    900: "#111827"
  },
  zinc: {
    50: "#fafafa",
    100: "#f4f4f5",
    200: "#e4e4e7",
    300: "#d4d4d8",
    400: "#a1a1aa",
    500: "#71717a",
    600: "#52525b",
    700: "#3f3f46",
    800: "#27272a",
    900: "#18181b"
  },
  neutral: {
    50: "#fafafa",
    100: "#f5f5f5",
    200: "#e5e5e5",
    300: "#d4d4d4",
    400: "#a3a3a3",
    500: "#737373",
    600: "#525252",
    700: "#404040",
    800: "#262626",
    900: "#171717"
  },
  stone: {
    50: "#fafaf9",
    100: "#f5f5f4",
    200: "#e7e5e4",
    300: "#d6d3d1",
    400: "#a8a29e",
    500: "#78716c",
    600: "#57534e",
    700: "#44403c",
    800: "#292524",
    900: "#1c1917"
  },
  red: {
    50: "#fef2f2",
    100: "#fee2e2",
    200: "#fecaca",
    300: "#fca5a5",
    400: "#f87171",
    500: "#ef4444",
    600: "#dc2626",
    700: "#b91c1c",
    800: "#991b1b",
    900: "#7f1d1d"
  },
  orange: {
    50: "#fff7ed",
    100: "#ffedd5",
    200: "#fed7aa",
    300: "#fdba74",
    400: "#fb923c",
    500: "#f97316",
    600: "#ea580c",
    700: "#c2410c",
    800: "#9a3412",
    900: "#7c2d12"
  },
  amber: {
    50: "#fffbeb",
    100: "#fef3c7",
    200: "#fde68a",
    300: "#fcd34d",
    400: "#fbbf24",
    500: "#f59e0b",
    600: "#d97706",
    700: "#b45309",
    800: "#92400e",
    900: "#78350f"
  },
  yellow: {
    50: "#fefce8",
    100: "#fef9c3",
    200: "#fef08a",
    300: "#fde047",
    400: "#facc15",
    500: "#eab308",
    600: "#ca8a04",
    700: "#a16207",
    800: "#854d0e",
    900: "#713f12"
  },
  lime: {
    50: "#f7fee7",
    100: "#ecfccb",
    200: "#d9f99d",
    300: "#bef264",
    400: "#a3e635",
    500: "#84cc16",
    600: "#65a30d",
    700: "#4d7c0f",
    800: "#3f6212",
    900: "#365314"
  },
  green: {
    50: "#f0fdf4",
    100: "#dcfce7",
    200: "#bbf7d0",
    300: "#86efac",
    400: "#4ade80",
    500: "#22c55e",
    600: "#16a34a",
    700: "#15803d",
    800: "#166534",
    900: "#14532d"
  },
  emerald: {
    50: "#ecfdf5",
    100: "#d1fae5",
    200: "#a7f3d0",
    300: "#6ee7b7",
    400: "#34d399",
    500: "#10b981",
    600: "#059669",
    700: "#047857",
    800: "#065f46",
    900: "#064e3b"
  },
  teal: {
    50: "#f0fdfa",
    100: "#ccfbf1",
    200: "#99f6e4",
    300: "#5eead4",
    400: "#2dd4bf",
    500: "#14b8a6",
    600: "#0d9488",
    700: "#0f766e",
    800: "#115e59",
    900: "#134e4a"
  },
  cyan: {
    50: "#ecfeff",
    100: "#cffafe",
    200: "#a5f3fc",
    300: "#67e8f9",
    400: "#22d3ee",
    500: "#06b6d4",
    600: "#0891b2",
    700: "#0e7490",
    800: "#155e75",
    900: "#164e63"
  },
  sky: {
    50: "#f0f9ff",
    100: "#e0f2fe",
    200: "#bae6fd",
    300: "#7dd3fc",
    400: "#38bdf8",
    500: "#0ea5e9",
    600: "#0284c7",
    700: "#0369a1",
    800: "#075985",
    900: "#0c4a6e"
  },
  blue: {
    50: "#eff6ff",
    100: "#dbeafe",
    200: "#bfdbfe",
    300: "#93c5fd",
    400: "#60a5fa",
    500: "#3b82f6",
    600: "#2563eb",
    700: "#1d4ed8",
    800: "#1e40af",
    900: "#1e3a8a"
  },
  indigo: {
    50: "#eef2ff",
    100: "#e0e7ff",
    200: "#c7d2fe",
    300: "#a5b4fc",
    400: "#818cf8",
    500: "#6366f1",
    600: "#4f46e5",
    700: "#4338ca",
    800: "#3730a3",
    900: "#312e81"
  },
  violet: {
    50: "#f5f3ff",
    100: "#ede9fe",
    200: "#ddd6fe",
    300: "#c4b5fd",
    400: "#a78bfa",
    500: "#8b5cf6",
    600: "#7c3aed",
    700: "#6d28d9",
    800: "#5b21b6",
    900: "#4c1d95"
  },
  purple: {
    50: "#faf5ff",
    100: "#f3e8ff",
    200: "#e9d5ff",
    300: "#d8b4fe",
    400: "#c084fc",
    500: "#a855f7",
    600: "#9333ea",
    700: "#7e22ce",
    800: "#6b21a8",
    900: "#581c87"
  },
  fuchsia: {
    50: "#fdf4ff",
    100: "#fae8ff",
    200: "#f5d0fe",
    300: "#f0abfc",
    400: "#e879f9",
    500: "#d946ef",
    600: "#c026d3",
    700: "#a21caf",
    800: "#86198f",
    900: "#701a75"
  },
  pink: {
    50: "#fdf2f8",
    100: "#fce7f3",
    200: "#fbcfe8",
    300: "#f9a8d4",
    400: "#f472b6",
    500: "#ec4899",
    600: "#db2777",
    700: "#be185d",
    800: "#9d174d",
    900: "#831843"
  },
  rose: {
    50: "#fff1f2",
    100: "#ffe4e6",
    200: "#fecdd3",
    300: "#fda4af",
    400: "#fb7185",
    500: "#f43f5e",
    600: "#e11d48",
    700: "#be123c",
    800: "#9f1239",
    900: "#881337"
  },
  get lightBlue() {
    warn({
      version: "v2.2",
      from: "lightBlue",
      to: "sky"
    });
    return this.sky;
  },
  get warmGray() {
    warn({
      version: "v3.0",
      from: "warmGray",
      to: "stone"
    });
    return this.stone;
  },
  get trueGray() {
    warn({
      version: "v3.0",
      from: "trueGray",
      to: "neutral"
    });
    return this.neutral;
  },
  get coolGray() {
    warn({
      version: "v3.0",
      from: "coolGray",
      to: "gray"
    });
    return this.gray;
  },
  get blueGray() {
    warn({
      version: "v3.0",
      from: "blueGray",
      to: "slate"
    });
    return this.slate;
  }
};
colors$2.default = _default;
let colors$1 = colors$2;
var colors_1 = (colors$1.__esModule ? colors$1 : { default: colors$1 }).default;
const ordered_colors = [
  "red",
  "green",
  "blue",
  "yellow",
  "purple",
  "teal",
  "orange",
  "cyan",
  "lime",
  "pink"
];
const color_values = [
  { color: "red", primary: 600, secondary: 100 },
  { color: "green", primary: 600, secondary: 100 },
  { color: "blue", primary: 600, secondary: 100 },
  { color: "yellow", primary: 500, secondary: 100 },
  { color: "purple", primary: 600, secondary: 100 },
  { color: "teal", primary: 600, secondary: 100 },
  { color: "orange", primary: 600, secondary: 100 },
  { color: "cyan", primary: 600, secondary: 100 },
  { color: "lime", primary: 500, secondary: 100 },
  { color: "pink", primary: 600, secondary: 100 }
];
const colors = color_values.reduce((acc, { color, primary, secondary }) => __spreadProps(__spreadValues({}, acc), {
  [color]: {
    primary: colors_1[color][primary],
    secondary: colors_1[color][secondary]
  }
}), {});
const get_style = (styles, key) => {
  return style_handlers[key](styles[key]);
};
function get_styles(styles, allowed_styles) {
  const processed_styles = allowed_styles.reduce((acc, next) => {
    if (styles[next] === void 0 || !style_handlers[next])
      acc[next] = " ";
    else {
      acc[next] = ` ${get_style(styles, next)} `;
    }
    return acc;
  }, {});
  processed_styles.classes = ` ${Object.values(processed_styles).join(" ").replace(/\s+/g, " ").trim()} `;
  return processed_styles;
}
const style_handlers = {
  container(container_visible) {
    return container_visible ? "" : `!p-0 !m-0 !border-0 !shadow-none !overflow-visible !bg-transparent`;
  },
  label_container(visible) {
    return visible ? "" : `!border-0 !shadow-none !overflow-visible !bg-transparent`;
  },
  grid(grid) {
    let grid_map = ["", "sm:", "md:", "lg:", "xl:", "2xl:"];
    let _grid = Array.isArray(grid) ? grid : [grid];
    return [0, 0, 0, 0, 0, 0].map((_2, i2) => `${grid_map[i2]}grid-cols-${(_grid == null ? void 0 : _grid[i2]) || (_grid == null ? void 0 : _grid[(_grid == null ? void 0 : _grid.length) - 1])}`).join(" ");
  },
  height(height) {
    return height === "auto" ? "auto" : "";
  },
  full_width(full_width) {
    return full_width ? "w-full grow" : "grow-0";
  },
  equal_height(equal_height) {
    return equal_height ? "items-stretch" : "unequal-height";
  },
  visible(visible) {
    return visible ? "" : "!hidden";
  },
  item_container(visible) {
    return visible ? "" : "!border-none";
  }
};
const create_classes = (styles, prefix = "") => {
  let classes = [];
  let target_styles = {};
  if (prefix === "") {
    target_styles = styles;
  } else {
    for (const prop in styles) {
      if (prop.startsWith(prefix + "_")) {
        const propname = prop.substring(prop.indexOf("_") + 1);
        target_styles[propname] = styles[prop];
      }
    }
  }
  if (target_styles.hasOwnProperty("margin")) {
    if (!Array.isArray(target_styles.margin)) {
      target_styles.margin = !!target_styles.margin ? [true, true, true, true] : [false, false, false, false];
    }
    let margin_map = ["t", "r", "b", "l"];
    target_styles.margin.forEach((margin, i2) => {
      if (!margin) {
        classes.push(`!m${margin_map[i2]}-0`);
      }
    });
  }
  if (target_styles.hasOwnProperty("border")) {
    if (!Array.isArray(target_styles.border)) {
      target_styles.border = !!target_styles.border ? [true, true, true, true] : [false, false, false, false];
    }
    let border_map = ["t", "r", "b", "l"];
    target_styles.border.forEach((border, i2) => {
      if (!border) {
        classes.push(`!border-${border_map[i2]}-0`);
      }
    });
  }
  switch (target_styles.rounded) {
    case true:
      classes.push("!rounded-lg");
      break;
    case false:
      classes.push("!rounded-none");
      break;
  }
  switch (target_styles.full_width) {
    case true:
      classes.push("w-full");
      break;
    case false:
      classes.push("!grow-0");
      break;
  }
  switch (target_styles.text_color) {
    case "red":
      classes.push("!text-red-500", "dark:text-red-100");
      break;
    case "yellow":
      classes.push("!text-yellow-500", "dark:text-yellow-100");
      break;
    case "green":
      classes.push("!text-green-500", "dark:text-green-100");
      break;
    case "blue":
      classes.push("!text-blue-500", "dark:text-blue-100");
      break;
    case "purple":
      classes.push("!text-purple-500", "dark:text-purple-100");
      break;
    case "black":
      classes.push("!text-gray-700", "dark:text-gray-50");
      break;
  }
  switch (target_styles.bg_color) {
    case "red":
      classes.push("!bg-red-100 !from-red-100 !to-red-200 !border-red-300", "dark:!bg-red-700 dark:!from-red-700 dark:!to-red-800 dark:!border-red-900");
      break;
    case "yellow":
      classes.push("!bg-yellow-100 !from-yellow-100 !to-yellow-200 !border-yellow-300", "dark:!bg-yellow-700 dark:!from-yellow-700 dark:!to-yellow-800 dark:!border-yellow-900");
      break;
    case "green":
      classes.push("!bg-green-100 !from-green-100 !to-green-200 !border-green-300", "dark:!bg-green-700 dark:!from-green-700 dark:!to-green-800 dark:!border-green-900  !text-gray-800");
      break;
    case "blue":
      classes.push("!bg-blue-100 !from-blue-100 !to-blue-200 !border-blue-300", "dark:!bg-blue-700 dark:!from-blue-700 dark:!to-blue-800 dark:!border-blue-900");
      break;
    case "purple":
      classes.push("!bg-purple-100 !from-purple-100 !to-purple-200 !border-purple-300", "dark:!bg-purple-700 dark:!from-purple-700 dark:!to-purple-800 dark:!border-purple-900");
      break;
    case "black":
      classes.push("!bg-gray-100 !from-gray-100 !to-gray-200 !border-gray-300", "dark:!bg-gray-700 dark:!from-gray-700 dark:!to-gray-800 dark:!border-gray-900");
    case "pink":
      classes.push("!bg-pink-100 !from-pink-100 !to-pink-200 !border-pink-300", "dark:!bg-pink-700 dark:!from-pink-700 dark:!to-pink-800 dark:!border-pink-900 !text-gray-800");
      break;
  }
  return " " + classes.join(" ");
};
function create_dynamic_element(ctx) {
  let svelte_element;
  let svelte_element_class_value;
  let svelte_element_style_value;
  let current;
  const default_slot_template = ctx[15].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[14], null);
  let svelte_element_levels = [
    { "data-testid": ctx[4] },
    { id: ctx[0] },
    {
      class: svelte_element_class_value = "gr-block gr-box relative w-full overflow-hidden " + ctx[8][ctx[1]] + " " + ctx[8][ctx[2]] + " " + ctx[7]
    },
    {
      style: svelte_element_style_value = ctx[6] || null
    }
  ];
  let svelte_element_data = {};
  for (let i2 = 0; i2 < svelte_element_levels.length; i2 += 1) {
    svelte_element_data = assign(svelte_element_data, svelte_element_levels[i2]);
  }
  return {
    c() {
      svelte_element = element(ctx[9]);
      if (default_slot)
        default_slot.c();
      set_attributes(svelte_element, svelte_element_data);
      toggle_class(svelte_element, "!hidden", ctx[5] === false);
      toggle_class(svelte_element, "gr-padded", ctx[3]);
    },
    m(target, anchor) {
      insert(target, svelte_element, anchor);
      if (default_slot) {
        default_slot.m(svelte_element, null);
      }
      current = true;
    },
    p(ctx2, dirty) {
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 16384)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[14], !current ? get_all_dirty_from_scope(ctx2[14]) : get_slot_changes(default_slot_template, ctx2[14], dirty, null), null);
        }
      }
      set_attributes(svelte_element, svelte_element_data = get_spread_update(svelte_element_levels, [
        (!current || dirty & 16) && { "data-testid": ctx2[4] },
        (!current || dirty & 1) && { id: ctx2[0] },
        (!current || dirty & 134 && svelte_element_class_value !== (svelte_element_class_value = "gr-block gr-box relative w-full overflow-hidden " + ctx2[8][ctx2[1]] + " " + ctx2[8][ctx2[2]] + " " + ctx2[7])) && { class: svelte_element_class_value },
        (!current || dirty & 64 && svelte_element_style_value !== (svelte_element_style_value = ctx2[6] || null)) && { style: svelte_element_style_value }
      ]));
      toggle_class(svelte_element, "!hidden", ctx2[5] === false);
      toggle_class(svelte_element, "gr-padded", ctx2[3]);
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
        detach(svelte_element);
      if (default_slot)
        default_slot.d(detaching);
    }
  };
}
function create_fragment$5(ctx) {
  let previous_tag = ctx[9];
  let svelte_element_anchor;
  let current;
  let svelte_element = ctx[9] && create_dynamic_element(ctx);
  return {
    c() {
      if (svelte_element)
        svelte_element.c();
      svelte_element_anchor = empty();
    },
    m(target, anchor) {
      if (svelte_element)
        svelte_element.m(target, anchor);
      insert(target, svelte_element_anchor, anchor);
      current = true;
    },
    p(ctx2, [dirty]) {
      if (ctx2[9]) {
        if (!previous_tag) {
          svelte_element = create_dynamic_element(ctx2);
          svelte_element.c();
          svelte_element.m(svelte_element_anchor.parentNode, svelte_element_anchor);
        } else if (safe_not_equal(previous_tag, ctx2[9])) {
          svelte_element.d(1);
          svelte_element = create_dynamic_element(ctx2);
          svelte_element.c();
          svelte_element.m(svelte_element_anchor.parentNode, svelte_element_anchor);
        } else {
          svelte_element.p(ctx2, dirty);
        }
      } else if (previous_tag) {
        svelte_element.d(1);
        svelte_element = null;
      }
      previous_tag = ctx2[9];
    },
    i(local) {
      if (current)
        return;
      transition_in(svelte_element);
      current = true;
    },
    o(local) {
      transition_out(svelte_element);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(svelte_element_anchor);
      if (svelte_element)
        svelte_element.d(detaching);
    }
  };
}
function instance$5($$self, $$props, $$invalidate) {
  let classes;
  let size_style;
  let { $$slots: slots = {}, $$scope } = $$props;
  let { style = {} } = $$props;
  let { elem_id = "" } = $$props;
  let { variant = "solid" } = $$props;
  let { color = "grey" } = $$props;
  let { padding = true } = $$props;
  let { type = "normal" } = $$props;
  let { test_id = void 0 } = $$props;
  let { disable = false } = $$props;
  let { explicit_call = false } = $$props;
  let { visible = true } = $$props;
  const styles = {
    dashed: "border-dashed border border-gray-300",
    solid: "border-solid border",
    grey: "border-gray-200",
    green: "border-green-400",
    none: "!border-0"
  };
  let tag = type === "fieldset" ? "fieldset" : "div";
  getContext("BLOCK_KEY");
  $$self.$$set = ($$props2) => {
    if ("style" in $$props2)
      $$invalidate(10, style = $$props2.style);
    if ("elem_id" in $$props2)
      $$invalidate(0, elem_id = $$props2.elem_id);
    if ("variant" in $$props2)
      $$invalidate(1, variant = $$props2.variant);
    if ("color" in $$props2)
      $$invalidate(2, color = $$props2.color);
    if ("padding" in $$props2)
      $$invalidate(3, padding = $$props2.padding);
    if ("type" in $$props2)
      $$invalidate(11, type = $$props2.type);
    if ("test_id" in $$props2)
      $$invalidate(4, test_id = $$props2.test_id);
    if ("disable" in $$props2)
      $$invalidate(12, disable = $$props2.disable);
    if ("explicit_call" in $$props2)
      $$invalidate(13, explicit_call = $$props2.explicit_call);
    if ("visible" in $$props2)
      $$invalidate(5, visible = $$props2.visible);
    if ("$$scope" in $$props2)
      $$invalidate(14, $$scope = $$props2.$$scope);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 13312) {
      $$invalidate(7, { classes } = explicit_call ? get_styles(style, []) : disable ? get_styles({ container: false }, ["container"]) : { classes: "" }, classes);
    }
    if ($$self.$$.dirty & 1024) {
      $$invalidate(6, size_style = (typeof style.height === "number" ? `height: ${style.height}px; ` : "") + (typeof style.width === "number" ? `width: ${style.width}px;` : ""));
    }
  };
  return [
    elem_id,
    variant,
    color,
    padding,
    test_id,
    visible,
    size_style,
    classes,
    styles,
    tag,
    style,
    type,
    disable,
    explicit_call,
    $$scope,
    slots
  ];
}
class Block extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$5, create_fragment$5, safe_not_equal, {
      style: 10,
      elem_id: 0,
      variant: 1,
      color: 2,
      padding: 3,
      type: 11,
      test_id: 4,
      disable: 12,
      explicit_call: 13,
      visible: 5
    });
  }
}
function create_fragment$4(ctx) {
  let span;
  let current;
  const default_slot_template = ctx[2].default;
  const default_slot = create_slot(default_slot_template, ctx, ctx[1], null);
  return {
    c() {
      span = element("span");
      if (default_slot)
        default_slot.c();
      attr(span, "class", "text-gray-500 text-[0.855rem] mb-2 block dark:text-gray-200 relative z-40");
      toggle_class(span, "sr-only", !ctx[0]);
      toggle_class(span, "h-0", !ctx[0]);
      toggle_class(span, "!m-0", !ctx[0]);
    },
    m(target, anchor) {
      insert(target, span, anchor);
      if (default_slot) {
        default_slot.m(span, null);
      }
      current = true;
    },
    p(ctx2, [dirty]) {
      if (default_slot) {
        if (default_slot.p && (!current || dirty & 2)) {
          update_slot_base(default_slot, default_slot_template, ctx2, ctx2[1], !current ? get_all_dirty_from_scope(ctx2[1]) : get_slot_changes(default_slot_template, ctx2[1], dirty, null), null);
        }
      }
      if (dirty & 1) {
        toggle_class(span, "sr-only", !ctx2[0]);
      }
      if (dirty & 1) {
        toggle_class(span, "h-0", !ctx2[0]);
      }
      if (dirty & 1) {
        toggle_class(span, "!m-0", !ctx2[0]);
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
        detach(span);
      if (default_slot)
        default_slot.d(detaching);
    }
  };
}
function instance$4($$self, $$props, $$invalidate) {
  let { $$slots: slots = {}, $$scope } = $$props;
  let { show_label = true } = $$props;
  $$self.$$set = ($$props2) => {
    if ("show_label" in $$props2)
      $$invalidate(0, show_label = $$props2.show_label);
    if ("$$scope" in $$props2)
      $$invalidate(1, $$scope = $$props2.$$scope);
  };
  return [show_label, $$scope, slots];
}
class BlockTitle extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$4, create_fragment$4, safe_not_equal, { show_label: 0 });
  }
}
function create_default_slot$2(ctx) {
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
function create_else_block(ctx) {
  let textarea;
  let text_area_resize_action;
  let mounted;
  let dispose;
  return {
    c() {
      textarea = element("textarea");
      attr(textarea, "data-testid", "textbox");
      attr(textarea, "class", "scroll-hide block gr-box gr-input w-full gr-text-input");
      attr(textarea, "placeholder", ctx[2]);
      attr(textarea, "rows", ctx[1]);
      textarea.disabled = ctx[4];
    },
    m(target, anchor) {
      insert(target, textarea, anchor);
      set_input_value(textarea, ctx[0]);
      ctx[19](textarea);
      if (!mounted) {
        dispose = [
          action_destroyer(text_area_resize_action = ctx[11].call(null, textarea, ctx[0])),
          listen(textarea, "input", ctx[18]),
          listen(textarea, "keypress", ctx[10]),
          listen(textarea, "blur", ctx[9])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 4) {
        attr(textarea, "placeholder", ctx2[2]);
      }
      if (dirty & 2) {
        attr(textarea, "rows", ctx2[1]);
      }
      if (dirty & 16) {
        textarea.disabled = ctx2[4];
      }
      if (text_area_resize_action && is_function(text_area_resize_action.update) && dirty & 1)
        text_area_resize_action.update.call(null, ctx2[0]);
      if (dirty & 1) {
        set_input_value(textarea, ctx2[0]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(textarea);
      ctx[19](null);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block$3(ctx) {
  let if_block_anchor;
  function select_block_type_1(ctx2, dirty) {
    if (ctx2[7] === "text")
      return create_if_block_1$2;
    if (ctx2[7] === "password")
      return create_if_block_2$1;
    if (ctx2[7] === "email")
      return create_if_block_3$1;
  }
  let current_block_type = select_block_type_1(ctx);
  let if_block = current_block_type && current_block_type(ctx);
  return {
    c() {
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
    },
    m(target, anchor) {
      if (if_block)
        if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
    },
    p(ctx2, dirty) {
      if (current_block_type === (current_block_type = select_block_type_1(ctx2)) && if_block) {
        if_block.p(ctx2, dirty);
      } else {
        if (if_block)
          if_block.d(1);
        if_block = current_block_type && current_block_type(ctx2);
        if (if_block) {
          if_block.c();
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        }
      }
    },
    d(detaching) {
      if (if_block) {
        if_block.d(detaching);
      }
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block_3$1(ctx) {
  let input;
  let mounted;
  let dispose;
  return {
    c() {
      input = element("input");
      attr(input, "data-testid", "textbox");
      attr(input, "type", "email");
      attr(input, "class", "scroll-hide block gr-box gr-input w-full gr-text-input");
      attr(input, "placeholder", ctx[2]);
      input.disabled = ctx[4];
      attr(input, "autocomplete", "email");
    },
    m(target, anchor) {
      insert(target, input, anchor);
      set_input_value(input, ctx[0]);
      ctx[17](input);
      if (!mounted) {
        dispose = [
          listen(input, "input", ctx[16]),
          listen(input, "keypress", ctx[10]),
          listen(input, "blur", ctx[9])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 4) {
        attr(input, "placeholder", ctx2[2]);
      }
      if (dirty & 16) {
        input.disabled = ctx2[4];
      }
      if (dirty & 1 && input.value !== ctx2[0]) {
        set_input_value(input, ctx2[0]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(input);
      ctx[17](null);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block_2$1(ctx) {
  let input;
  let mounted;
  let dispose;
  return {
    c() {
      input = element("input");
      attr(input, "data-testid", "password");
      attr(input, "type", "password");
      attr(input, "class", "scroll-hide block gr-box gr-input w-full gr-text-input");
      attr(input, "placeholder", ctx[2]);
      input.disabled = ctx[4];
      attr(input, "autocomplete", "");
    },
    m(target, anchor) {
      insert(target, input, anchor);
      set_input_value(input, ctx[0]);
      ctx[15](input);
      if (!mounted) {
        dispose = [
          listen(input, "input", ctx[14]),
          listen(input, "keypress", ctx[10]),
          listen(input, "blur", ctx[9])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 4) {
        attr(input, "placeholder", ctx2[2]);
      }
      if (dirty & 16) {
        input.disabled = ctx2[4];
      }
      if (dirty & 1 && input.value !== ctx2[0]) {
        set_input_value(input, ctx2[0]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(input);
      ctx[15](null);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block_1$2(ctx) {
  let input;
  let mounted;
  let dispose;
  return {
    c() {
      input = element("input");
      attr(input, "data-testid", "textbox");
      attr(input, "type", "text");
      attr(input, "class", "scroll-hide block gr-box gr-input w-full gr-text-input");
      attr(input, "placeholder", ctx[2]);
      input.disabled = ctx[4];
    },
    m(target, anchor) {
      insert(target, input, anchor);
      set_input_value(input, ctx[0]);
      ctx[13](input);
      if (!mounted) {
        dispose = [
          listen(input, "input", ctx[12]),
          listen(input, "keypress", ctx[10]),
          listen(input, "blur", ctx[9])
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 4) {
        attr(input, "placeholder", ctx2[2]);
      }
      if (dirty & 16) {
        input.disabled = ctx2[4];
      }
      if (dirty & 1 && input.value !== ctx2[0]) {
        set_input_value(input, ctx2[0]);
      }
    },
    d(detaching) {
      if (detaching)
        detach(input);
      ctx[13](null);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_fragment$3(ctx) {
  let label_1;
  let blocktitle;
  let t;
  let current;
  blocktitle = new BlockTitle({
    props: {
      show_label: ctx[5],
      $$slots: { default: [create_default_slot$2] },
      $$scope: { ctx }
    }
  });
  function select_block_type(ctx2, dirty) {
    if (ctx2[1] === 1 && ctx2[6] === 1)
      return create_if_block$3;
    return create_else_block;
  }
  let current_block_type = select_block_type(ctx);
  let if_block = current_block_type(ctx);
  return {
    c() {
      label_1 = element("label");
      create_component(blocktitle.$$.fragment);
      t = space();
      if_block.c();
      attr(label_1, "class", "block w-full");
    },
    m(target, anchor) {
      insert(target, label_1, anchor);
      mount_component(blocktitle, label_1, null);
      append(label_1, t);
      if_block.m(label_1, null);
      current = true;
    },
    p(ctx2, [dirty]) {
      const blocktitle_changes = {};
      if (dirty & 32)
        blocktitle_changes.show_label = ctx2[5];
      if (dirty & 8388616) {
        blocktitle_changes.$$scope = { dirty, ctx: ctx2 };
      }
      blocktitle.$set(blocktitle_changes);
      if (current_block_type === (current_block_type = select_block_type(ctx2)) && if_block) {
        if_block.p(ctx2, dirty);
      } else {
        if_block.d(1);
        if_block = current_block_type(ctx2);
        if (if_block) {
          if_block.c();
          if_block.m(label_1, null);
        }
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
      if_block.d();
    }
  };
}
function instance$3($$self, $$props, $$invalidate) {
  let { value = "" } = $$props;
  let { lines = 1 } = $$props;
  let { placeholder = "Type here..." } = $$props;
  let { label } = $$props;
  let { disabled = false } = $$props;
  let { show_label = true } = $$props;
  let { max_lines } = $$props;
  let { type = "text" } = $$props;
  let el;
  const dispatch2 = createEventDispatcher();
  function handle_change(val) {
    dispatch2("change", val);
  }
  function handle_blur(e) {
    dispatch2("blur");
  }
  async function handle_keypress(e) {
    await tick();
    if (e.key === "Enter" && e.shiftKey && lines > 1) {
      e.preventDefault();
      dispatch2("submit");
    } else if (e.key === "Enter" && !e.shiftKey && lines === 1 && max_lines >= 1) {
      e.preventDefault();
      dispatch2("submit");
    }
  }
  async function resize(event) {
    await tick();
    if (lines === max_lines)
      return;
    let max = max_lines === false ? false : max_lines === void 0 ? 21 * 11 : 21 * (max_lines + 1);
    let min = 21 * (lines + 1);
    const target = event.target;
    target.style.height = "1px";
    let scroll_height;
    if (max && target.scrollHeight > max) {
      scroll_height = max;
    } else if (target.scrollHeight < min) {
      scroll_height = min;
    } else {
      scroll_height = target.scrollHeight;
    }
    target.style.height = `${scroll_height}px`;
  }
  function text_area_resize(el2, value2) {
    if (lines === max_lines)
      return;
    el2.style.overflowY = "scroll";
    el2.addEventListener("input", resize);
    if (!value2.trim())
      return;
    resize({ target: el2 });
    return {
      destroy: () => el2.removeEventListener("input", resize)
    };
  }
  function input_input_handler() {
    value = this.value;
    $$invalidate(0, value);
  }
  function input_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      el = $$value;
      $$invalidate(8, el);
    });
  }
  function input_input_handler_1() {
    value = this.value;
    $$invalidate(0, value);
  }
  function input_binding_1($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      el = $$value;
      $$invalidate(8, el);
    });
  }
  function input_input_handler_2() {
    value = this.value;
    $$invalidate(0, value);
  }
  function input_binding_2($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      el = $$value;
      $$invalidate(8, el);
    });
  }
  function textarea_input_handler() {
    value = this.value;
    $$invalidate(0, value);
  }
  function textarea_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      el = $$value;
      $$invalidate(8, el);
    });
  }
  $$self.$$set = ($$props2) => {
    if ("value" in $$props2)
      $$invalidate(0, value = $$props2.value);
    if ("lines" in $$props2)
      $$invalidate(1, lines = $$props2.lines);
    if ("placeholder" in $$props2)
      $$invalidate(2, placeholder = $$props2.placeholder);
    if ("label" in $$props2)
      $$invalidate(3, label = $$props2.label);
    if ("disabled" in $$props2)
      $$invalidate(4, disabled = $$props2.disabled);
    if ("show_label" in $$props2)
      $$invalidate(5, show_label = $$props2.show_label);
    if ("max_lines" in $$props2)
      $$invalidate(6, max_lines = $$props2.max_lines);
    if ("type" in $$props2)
      $$invalidate(7, type = $$props2.type);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 323) {
      el && lines !== max_lines && resize({ target: el });
    }
    if ($$self.$$.dirty & 1) {
      handle_change(value);
    }
  };
  return [
    value,
    lines,
    placeholder,
    label,
    disabled,
    show_label,
    max_lines,
    type,
    el,
    handle_blur,
    handle_keypress,
    text_area_resize,
    input_input_handler,
    input_binding,
    input_input_handler_1,
    input_binding_1,
    input_input_handler_2,
    input_binding_2,
    textarea_input_handler,
    textarea_binding
  ];
}
class Textbox$1 extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$3, create_fragment$3, safe_not_equal, {
      value: 0,
      lines: 1,
      placeholder: 2,
      label: 3,
      disabled: 4,
      show_label: 5,
      max_lines: 6,
      type: 7
    });
  }
}
function fade(node, { delay = 0, duration = 400, easing = identity } = {}) {
  const o2 = +getComputedStyle(node).opacity;
  return {
    delay,
    duration,
    easing,
    css: (t) => `opacity: ${t * o2}`
  };
}
var StatusTracker_svelte_svelte_type_style_lang = "";
function create_if_block_6(ctx) {
  let span;
  let t1;
  let if_block_anchor;
  let if_block = ctx[9] && create_if_block_7(ctx);
  return {
    c() {
      span = element("span");
      span.textContent = "Error";
      t1 = space();
      if (if_block)
        if_block.c();
      if_block_anchor = empty();
      attr(span, "class", "error svelte-85dhbz");
    },
    m(target, anchor) {
      insert(target, span, anchor);
      insert(target, t1, anchor);
      if (if_block)
        if_block.m(target, anchor);
      insert(target, if_block_anchor, anchor);
    },
    p(ctx2, dirty) {
      if (ctx2[9]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty & 512) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block_7(ctx2);
          if_block.c();
          transition_in(if_block, 1);
          if_block.m(if_block_anchor.parentNode, if_block_anchor);
        }
      } else if (if_block) {
        if_block.d(1);
        if_block = null;
      }
    },
    i(local) {
      transition_in(if_block);
    },
    o: noop,
    d(detaching) {
      if (detaching)
        detach(span);
      if (detaching)
        detach(t1);
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(if_block_anchor);
    }
  };
}
function create_if_block$2(ctx) {
  let t0;
  let div;
  let t1;
  let t2;
  let loader;
  let t3;
  let if_block3_anchor;
  let current;
  let if_block0 = ctx[7] === "default" && create_if_block_5(ctx);
  function select_block_type_1(ctx2, dirty) {
    if (ctx2[1] !== null && ctx2[2] !== void 0 && ctx2[1] >= 0)
      return create_if_block_3;
    if (ctx2[1] === 0)
      return create_if_block_4;
  }
  let current_block_type = select_block_type_1(ctx);
  let if_block1 = current_block_type && current_block_type(ctx);
  let if_block2 = ctx[4] && create_if_block_2(ctx);
  loader = new Loader({
    props: { margin: ctx[7] === "default" }
  });
  let if_block3 = !ctx[4] && create_if_block_1$1();
  return {
    c() {
      if (if_block0)
        if_block0.c();
      t0 = space();
      div = element("div");
      if (if_block1)
        if_block1.c();
      t1 = space();
      if (if_block2)
        if_block2.c();
      t2 = space();
      create_component(loader.$$.fragment);
      t3 = space();
      if (if_block3)
        if_block3.c();
      if_block3_anchor = empty();
      attr(div, "class", "dark:text-gray-400 svelte-85dhbz");
      toggle_class(div, "meta-text-center", ctx[7] === "center");
      toggle_class(div, "meta-text", ctx[7] === "default");
    },
    m(target, anchor) {
      if (if_block0)
        if_block0.m(target, anchor);
      insert(target, t0, anchor);
      insert(target, div, anchor);
      if (if_block1)
        if_block1.m(div, null);
      append(div, t1);
      if (if_block2)
        if_block2.m(div, null);
      insert(target, t2, anchor);
      mount_component(loader, target, anchor);
      insert(target, t3, anchor);
      if (if_block3)
        if_block3.m(target, anchor);
      insert(target, if_block3_anchor, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[7] === "default") {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_5(ctx2);
          if_block0.c();
          if_block0.m(t0.parentNode, t0);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (current_block_type === (current_block_type = select_block_type_1(ctx2)) && if_block1) {
        if_block1.p(ctx2, dirty);
      } else {
        if (if_block1)
          if_block1.d(1);
        if_block1 = current_block_type && current_block_type(ctx2);
        if (if_block1) {
          if_block1.c();
          if_block1.m(div, t1);
        }
      }
      if (ctx2[4]) {
        if (if_block2) {
          if_block2.p(ctx2, dirty);
        } else {
          if_block2 = create_if_block_2(ctx2);
          if_block2.c();
          if_block2.m(div, null);
        }
      } else if (if_block2) {
        if_block2.d(1);
        if_block2 = null;
      }
      if (dirty & 128) {
        toggle_class(div, "meta-text-center", ctx2[7] === "center");
      }
      if (dirty & 128) {
        toggle_class(div, "meta-text", ctx2[7] === "default");
      }
      const loader_changes = {};
      if (dirty & 128)
        loader_changes.margin = ctx2[7] === "default";
      loader.$set(loader_changes);
      if (!ctx2[4]) {
        if (if_block3)
          ;
        else {
          if_block3 = create_if_block_1$1();
          if_block3.c();
          if_block3.m(if_block3_anchor.parentNode, if_block3_anchor);
        }
      } else if (if_block3) {
        if_block3.d(1);
        if_block3 = null;
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(loader.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(loader.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (if_block0)
        if_block0.d(detaching);
      if (detaching)
        detach(t0);
      if (detaching)
        detach(div);
      if (if_block1) {
        if_block1.d();
      }
      if (if_block2)
        if_block2.d();
      if (detaching)
        detach(t2);
      destroy_component(loader, detaching);
      if (detaching)
        detach(t3);
      if (if_block3)
        if_block3.d(detaching);
      if (detaching)
        detach(if_block3_anchor);
    }
  };
}
function create_if_block_7(ctx) {
  let div4;
  let div0;
  let t0;
  let div3;
  let div1;
  let t1;
  let button;
  let t3;
  let div2;
  let t4;
  let div3_intro;
  let mounted;
  let dispose;
  return {
    c() {
      div4 = element("div");
      div0 = element("div");
      t0 = space();
      div3 = element("div");
      div1 = element("div");
      t1 = text("Error\n						");
      button = element("button");
      button.textContent = "\xD7";
      t3 = space();
      div2 = element("div");
      t4 = text(ctx[6]);
      attr(div0, "class", "absolute left-0 md:left-auto border-black right-0 top-0 h-96 md:w-1/2 bg-gradient-to-b md:bg-gradient-to-bl from-red-500/5 via-transparent to-transparent");
      attr(button, "class", "ml-auto text-gray-900 text-2xl pr-1");
      attr(div1, "class", "flex items-center bg-gradient-to-r from-red-500/10 to-red-200/10 px-3 py-1 text-lg font-bold text-red-500");
      attr(div2, "class", "px-3 py-3 text-base font-mono");
      attr(div3, "class", "absolute bg-white top-7 left-4 right-4 md:right-8 md:left-auto rounded-xl border border-gray-100 dark:border-gray-800 overflow-hidden shadow-2xl shadow-red-500/10 md:w-96 pointer-events-auto");
      attr(div4, "class", "fixed inset-0 z-[100]");
    },
    m(target, anchor) {
      insert(target, div4, anchor);
      append(div4, div0);
      append(div4, t0);
      append(div4, div3);
      append(div3, div1);
      append(div1, t1);
      append(div1, button);
      append(div3, t3);
      append(div3, div2);
      append(div2, t4);
      if (!mounted) {
        dispose = [
          listen(button, "click", ctx[13]),
          listen(div3, "click", stop_propagation(ctx[20]))
        ];
        mounted = true;
      }
    },
    p(ctx2, dirty) {
      if (dirty & 64)
        set_data(t4, ctx2[6]);
    },
    i(local) {
      if (!div3_intro) {
        add_render_callback(() => {
          div3_intro = create_in_transition(div3, fade, { duration: 100 });
          div3_intro.start();
        });
      }
    },
    o: noop,
    d(detaching) {
      if (detaching)
        detach(div4);
      mounted = false;
      run_all(dispose);
    }
  };
}
function create_if_block_5(ctx) {
  let div;
  let style_transform = `scaleX(${ctx[12] || 0})`;
  return {
    c() {
      div = element("div");
      attr(div, "class", "progress-bar svelte-85dhbz");
      set_style(div, "transform", style_transform, false);
    },
    m(target, anchor) {
      insert(target, div, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 4096 && style_transform !== (style_transform = `scaleX(${ctx2[12] || 0})`)) {
        set_style(div, "transform", style_transform, false);
      }
    },
    d(detaching) {
      if (detaching)
        detach(div);
    }
  };
}
function create_if_block_4(ctx) {
  let t;
  return {
    c() {
      t = text("processing |");
    },
    m(target, anchor) {
      insert(target, t, anchor);
    },
    p: noop,
    d(detaching) {
      if (detaching)
        detach(t);
    }
  };
}
function create_if_block_3(ctx) {
  let t0;
  let t1_value = ctx[1] + 1 + "";
  let t1;
  let t2;
  let t3;
  let t4;
  return {
    c() {
      t0 = text("queue: ");
      t1 = text(t1_value);
      t2 = text("/");
      t3 = text(ctx[2]);
      t4 = text(" |");
    },
    m(target, anchor) {
      insert(target, t0, anchor);
      insert(target, t1, anchor);
      insert(target, t2, anchor);
      insert(target, t3, anchor);
      insert(target, t4, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 2 && t1_value !== (t1_value = ctx2[1] + 1 + ""))
        set_data(t1, t1_value);
      if (dirty & 4)
        set_data(t3, ctx2[2]);
    },
    d(detaching) {
      if (detaching)
        detach(t0);
      if (detaching)
        detach(t1);
      if (detaching)
        detach(t2);
      if (detaching)
        detach(t3);
      if (detaching)
        detach(t4);
    }
  };
}
function create_if_block_2(ctx) {
  let t0;
  let t1_value = ctx[0] ? `/${ctx[10]}` : "";
  let t1;
  return {
    c() {
      t0 = text(ctx[11]);
      t1 = text(t1_value);
    },
    m(target, anchor) {
      insert(target, t0, anchor);
      insert(target, t1, anchor);
    },
    p(ctx2, dirty) {
      if (dirty & 2048)
        set_data(t0, ctx2[11]);
      if (dirty & 1025 && t1_value !== (t1_value = ctx2[0] ? `/${ctx2[10]}` : ""))
        set_data(t1, t1_value);
    },
    d(detaching) {
      if (detaching)
        detach(t0);
      if (detaching)
        detach(t1);
    }
  };
}
function create_if_block_1$1(ctx) {
  let p2;
  return {
    c() {
      p2 = element("p");
      p2.textContent = "Loading...";
      attr(p2, "class", "timer svelte-85dhbz");
    },
    m(target, anchor) {
      insert(target, p2, anchor);
    },
    d(detaching) {
      if (detaching)
        detach(p2);
    }
  };
}
function create_fragment$2(ctx) {
  let div;
  let current_block_type_index;
  let if_block;
  let current;
  const if_block_creators = [create_if_block$2, create_if_block_6];
  const if_blocks = [];
  function select_block_type(ctx2, dirty) {
    if (ctx2[3] === "pending")
      return 0;
    if (ctx2[3] === "error")
      return 1;
    return -1;
  }
  if (~(current_block_type_index = select_block_type(ctx))) {
    if_block = if_blocks[current_block_type_index] = if_block_creators[current_block_type_index](ctx);
  }
  return {
    c() {
      div = element("div");
      if (if_block)
        if_block.c();
      attr(div, "class", "wrap svelte-85dhbz");
      toggle_class(div, "inset-0", ctx[7] === "default");
      toggle_class(div, "inset-x-0", ctx[7] === "center");
      toggle_class(div, "top-0", ctx[7] === "center");
      toggle_class(div, "opacity-0", !ctx[3] || ctx[3] === "complete");
      toggle_class(div, "cover-bg", ctx[7] === "default" && (ctx[3] === "pending" || ctx[3] === "error"));
      toggle_class(div, "generating", ctx[3] === "generating");
      toggle_class(div, "!hidden", !ctx[5]);
    },
    m(target, anchor) {
      insert(target, div, anchor);
      if (~current_block_type_index) {
        if_blocks[current_block_type_index].m(div, null);
      }
      ctx[21](div);
      current = true;
    },
    p(ctx2, [dirty]) {
      let previous_block_index = current_block_type_index;
      current_block_type_index = select_block_type(ctx2);
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
          if_block.m(div, null);
        } else {
          if_block = null;
        }
      }
      if (dirty & 128) {
        toggle_class(div, "inset-0", ctx2[7] === "default");
      }
      if (dirty & 128) {
        toggle_class(div, "inset-x-0", ctx2[7] === "center");
      }
      if (dirty & 128) {
        toggle_class(div, "top-0", ctx2[7] === "center");
      }
      if (dirty & 8) {
        toggle_class(div, "opacity-0", !ctx2[3] || ctx2[3] === "complete");
      }
      if (dirty & 136) {
        toggle_class(div, "cover-bg", ctx2[7] === "default" && (ctx2[3] === "pending" || ctx2[3] === "error"));
      }
      if (dirty & 8) {
        toggle_class(div, "generating", ctx2[3] === "generating");
      }
      if (dirty & 32) {
        toggle_class(div, "!hidden", !ctx2[5]);
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
      if (detaching)
        detach(div);
      if (~current_block_type_index) {
        if_blocks[current_block_type_index].d();
      }
      ctx[21](null);
    }
  };
}
let items = [];
let called = false;
async function scroll_into_view(el, enable = true) {
  if (window.__gradio_mode__ === "website" || window.__gradio_mode__ !== "app" && enable !== true) {
    return;
  }
  items.push(el);
  if (!called)
    called = true;
  else
    return;
  await tick();
  requestAnimationFrame(() => {
    let min = [0, 0];
    for (let i2 = 0; i2 < items.length; i2++) {
      const element2 = items[i2];
      const box = element2.getBoundingClientRect();
      if (i2 === 0 || box.top + window.scrollY <= min[0]) {
        min[0] = box.top + window.scrollY;
        min[1] = i2;
      }
    }
    window.scrollTo({ top: min[0] - 20, behavior: "smooth" });
    called = false;
    items = [];
  });
}
function instance$2($$self, $$props, $$invalidate) {
  let progress;
  let formatted_timer;
  let $app_state;
  component_subscribe($$self, app_state, ($$value) => $$invalidate(19, $app_state = $$value));
  let { eta = null } = $$props;
  let { queue = false } = $$props;
  let { queue_position } = $$props;
  let { queue_size } = $$props;
  let { status } = $$props;
  let { scroll_to_output = false } = $$props;
  let { timer = true } = $$props;
  let { visible = true } = $$props;
  let { message = null } = $$props;
  let { variant = "default" } = $$props;
  let el;
  let _timer = false;
  let timer_start = 0;
  let timer_diff = 0;
  let old_eta = null;
  let message_visible = false;
  const start_timer = () => {
    $$invalidate(16, timer_start = performance.now());
    $$invalidate(17, timer_diff = 0);
    _timer = true;
    run2();
  };
  function run2() {
    requestAnimationFrame(() => {
      $$invalidate(17, timer_diff = (performance.now() - timer_start) / 1e3);
      if (_timer)
        run2();
    });
  }
  const stop_timer = () => {
    $$invalidate(17, timer_diff = 0);
    if (!_timer)
      return;
    _timer = false;
  };
  onDestroy(() => {
    if (_timer)
      stop_timer();
  });
  let formatted_eta = null;
  const close_message = () => {
    $$invalidate(9, message_visible = false);
  };
  function click_handler(event) {
    bubble.call(this, $$self, event);
  }
  function div_binding($$value) {
    binding_callbacks[$$value ? "unshift" : "push"](() => {
      el = $$value;
      $$invalidate(8, el);
    });
  }
  $$self.$$set = ($$props2) => {
    if ("eta" in $$props2)
      $$invalidate(0, eta = $$props2.eta);
    if ("queue" in $$props2)
      $$invalidate(14, queue = $$props2.queue);
    if ("queue_position" in $$props2)
      $$invalidate(1, queue_position = $$props2.queue_position);
    if ("queue_size" in $$props2)
      $$invalidate(2, queue_size = $$props2.queue_size);
    if ("status" in $$props2)
      $$invalidate(3, status = $$props2.status);
    if ("scroll_to_output" in $$props2)
      $$invalidate(15, scroll_to_output = $$props2.scroll_to_output);
    if ("timer" in $$props2)
      $$invalidate(4, timer = $$props2.timer);
    if ("visible" in $$props2)
      $$invalidate(5, visible = $$props2.visible);
    if ("message" in $$props2)
      $$invalidate(6, message = $$props2.message);
    if ("variant" in $$props2)
      $$invalidate(7, variant = $$props2.variant);
  };
  $$self.$$.update = () => {
    if ($$self.$$.dirty & 344065) {
      {
        if (eta === null) {
          $$invalidate(0, eta = old_eta);
        } else if (queue) {
          $$invalidate(0, eta = (performance.now() - timer_start) / 1e3 + eta);
        }
        if (eta != null) {
          $$invalidate(10, formatted_eta = eta.toFixed(1));
          $$invalidate(18, old_eta = eta);
        }
      }
    }
    if ($$self.$$.dirty & 131073) {
      $$invalidate(12, progress = eta === null || eta <= 0 || !timer_diff ? null : Math.min(timer_diff / eta, 1));
    }
    if ($$self.$$.dirty & 8) {
      {
        if (status === "pending") {
          start_timer();
        } else {
          stop_timer();
        }
      }
    }
    if ($$self.$$.dirty & 557320) {
      el && scroll_to_output && (status === "pending" || status === "complete") && scroll_into_view(el, $app_state.autoscroll);
    }
    if ($$self.$$.dirty & 72) {
      {
        close_message();
        if (status === "error" && message) {
          $$invalidate(9, message_visible = true);
        }
      }
    }
    if ($$self.$$.dirty & 131072) {
      $$invalidate(11, formatted_timer = timer_diff.toFixed(1));
    }
  };
  return [
    eta,
    queue_position,
    queue_size,
    status,
    timer,
    visible,
    message,
    variant,
    el,
    message_visible,
    formatted_eta,
    formatted_timer,
    progress,
    close_message,
    queue,
    scroll_to_output,
    timer_start,
    timer_diff,
    old_eta,
    $app_state,
    click_handler,
    div_binding
  ];
}
class StatusTracker extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$2, create_fragment$2, safe_not_equal, {
      eta: 0,
      queue: 14,
      queue_position: 1,
      queue_size: 2,
      status: 3,
      scroll_to_output: 15,
      timer: 4,
      visible: 5,
      message: 6,
      variant: 7
    });
  }
}
function create_if_block$1(ctx) {
  let statustracker;
  let current;
  const statustracker_spread_levels = [ctx[10]];
  let statustracker_props = {};
  for (let i2 = 0; i2 < statustracker_spread_levels.length; i2 += 1) {
    statustracker_props = assign(statustracker_props, statustracker_spread_levels[i2]);
  }
  statustracker = new StatusTracker({ props: statustracker_props });
  return {
    c() {
      create_component(statustracker.$$.fragment);
    },
    m(target, anchor) {
      mount_component(statustracker, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const statustracker_changes = dirty & 1024 ? get_spread_update(statustracker_spread_levels, [get_spread_object(ctx2[10])]) : {};
      statustracker.$set(statustracker_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(statustracker.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(statustracker.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(statustracker, detaching);
    }
  };
}
function create_default_slot$1(ctx) {
  let t;
  let textbox;
  let updating_value;
  let current;
  let if_block = ctx[10] && create_if_block$1(ctx);
  function textbox_value_binding(value) {
    ctx[12](value);
  }
  let textbox_props = {
    label: ctx[1],
    show_label: ctx[6],
    lines: ctx[4],
    type: ctx[8],
    max_lines: !ctx[7] && ctx[11] === "static" ? ctx[4] + 1 : ctx[7],
    placeholder: ctx[5],
    disabled: ctx[11] === "static"
  };
  if (ctx[0] !== void 0) {
    textbox_props.value = ctx[0];
  }
  textbox = new Textbox$1({ props: textbox_props });
  binding_callbacks.push(() => bind(textbox, "value", textbox_value_binding));
  textbox.$on("change", ctx[13]);
  textbox.$on("submit", ctx[14]);
  textbox.$on("blur", ctx[15]);
  return {
    c() {
      if (if_block)
        if_block.c();
      t = space();
      create_component(textbox.$$.fragment);
    },
    m(target, anchor) {
      if (if_block)
        if_block.m(target, anchor);
      insert(target, t, anchor);
      mount_component(textbox, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      if (ctx2[10]) {
        if (if_block) {
          if_block.p(ctx2, dirty);
          if (dirty & 1024) {
            transition_in(if_block, 1);
          }
        } else {
          if_block = create_if_block$1(ctx2);
          if_block.c();
          transition_in(if_block, 1);
          if_block.m(t.parentNode, t);
        }
      } else if (if_block) {
        group_outros();
        transition_out(if_block, 1, 1, () => {
          if_block = null;
        });
        check_outros();
      }
      const textbox_changes = {};
      if (dirty & 2)
        textbox_changes.label = ctx2[1];
      if (dirty & 64)
        textbox_changes.show_label = ctx2[6];
      if (dirty & 16)
        textbox_changes.lines = ctx2[4];
      if (dirty & 256)
        textbox_changes.type = ctx2[8];
      if (dirty & 2192)
        textbox_changes.max_lines = !ctx2[7] && ctx2[11] === "static" ? ctx2[4] + 1 : ctx2[7];
      if (dirty & 32)
        textbox_changes.placeholder = ctx2[5];
      if (dirty & 2048)
        textbox_changes.disabled = ctx2[11] === "static";
      if (!updating_value && dirty & 1) {
        updating_value = true;
        textbox_changes.value = ctx2[0];
        add_flush_callback(() => updating_value = false);
      }
      textbox.$set(textbox_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(if_block);
      transition_in(textbox.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(if_block);
      transition_out(textbox.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (if_block)
        if_block.d(detaching);
      if (detaching)
        detach(t);
      destroy_component(textbox, detaching);
    }
  };
}
function create_fragment$1(ctx) {
  let block;
  let current;
  block = new Block({
    props: {
      visible: ctx[3],
      elem_id: ctx[2],
      disable: typeof ctx[9].container === "boolean" && !ctx[9].container,
      $$slots: { default: [create_default_slot$1] },
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
      if (dirty & 512)
        block_changes.disable = typeof ctx2[9].container === "boolean" && !ctx2[9].container;
      if (dirty & 69107) {
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
function instance$1($$self, $$props, $$invalidate) {
  let { label = "Textbox" } = $$props;
  let { elem_id = "" } = $$props;
  let { visible = true } = $$props;
  let { value = "" } = $$props;
  let { lines } = $$props;
  let { placeholder = "" } = $$props;
  let { show_label } = $$props;
  let { max_lines } = $$props;
  let { type = "text" } = $$props;
  let { style = {} } = $$props;
  let { loading_status = void 0 } = $$props;
  let { mode } = $$props;
  function textbox_value_binding(value$1) {
    value = value$1;
    $$invalidate(0, value);
  }
  function change_handler(event) {
    bubble.call(this, $$self, event);
  }
  function submit_handler(event) {
    bubble.call(this, $$self, event);
  }
  function blur_handler(event) {
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
    if ("lines" in $$props2)
      $$invalidate(4, lines = $$props2.lines);
    if ("placeholder" in $$props2)
      $$invalidate(5, placeholder = $$props2.placeholder);
    if ("show_label" in $$props2)
      $$invalidate(6, show_label = $$props2.show_label);
    if ("max_lines" in $$props2)
      $$invalidate(7, max_lines = $$props2.max_lines);
    if ("type" in $$props2)
      $$invalidate(8, type = $$props2.type);
    if ("style" in $$props2)
      $$invalidate(9, style = $$props2.style);
    if ("loading_status" in $$props2)
      $$invalidate(10, loading_status = $$props2.loading_status);
    if ("mode" in $$props2)
      $$invalidate(11, mode = $$props2.mode);
  };
  return [
    value,
    label,
    elem_id,
    visible,
    lines,
    placeholder,
    show_label,
    max_lines,
    type,
    style,
    loading_status,
    mode,
    textbox_value_binding,
    change_handler,
    submit_handler,
    blur_handler
  ];
}
class Textbox extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance$1, create_fragment$1, safe_not_equal, {
      label: 1,
      elem_id: 2,
      visible: 3,
      value: 0,
      lines: 4,
      placeholder: 5,
      show_label: 6,
      max_lines: 7,
      type: 8,
      style: 9,
      loading_status: 10,
      mode: 11
    });
  }
  get label() {
    return this.$$.ctx[1];
  }
  set label(label) {
    this.$$set({ label });
    flush();
  }
  get elem_id() {
    return this.$$.ctx[2];
  }
  set elem_id(elem_id) {
    this.$$set({ elem_id });
    flush();
  }
  get visible() {
    return this.$$.ctx[3];
  }
  set visible(visible) {
    this.$$set({ visible });
    flush();
  }
  get value() {
    return this.$$.ctx[0];
  }
  set value(value) {
    this.$$set({ value });
    flush();
  }
  get lines() {
    return this.$$.ctx[4];
  }
  set lines(lines) {
    this.$$set({ lines });
    flush();
  }
  get placeholder() {
    return this.$$.ctx[5];
  }
  set placeholder(placeholder) {
    this.$$set({ placeholder });
    flush();
  }
  get show_label() {
    return this.$$.ctx[6];
  }
  set show_label(show_label) {
    this.$$set({ show_label });
    flush();
  }
  get max_lines() {
    return this.$$.ctx[7];
  }
  set max_lines(max_lines) {
    this.$$set({ max_lines });
    flush();
  }
  get type() {
    return this.$$.ctx[8];
  }
  set type(type) {
    this.$$set({ type });
    flush();
  }
  get style() {
    return this.$$.ctx[9];
  }
  set style(style) {
    this.$$set({ style });
    flush();
  }
  get loading_status() {
    return this.$$.ctx[10];
  }
  set loading_status(loading_status) {
    this.$$set({ loading_status });
    flush();
  }
  get mode() {
    return this.$$.ctx[11];
  }
  set mode(mode) {
    this.$$set({ mode });
    flush();
  }
}
function create_if_block_1(ctx) {
  let p2;
  let t;
  return {
    c() {
      p2 = element("p");
      t = text(ctx[0]);
      attr(p2, "class", "my-4");
    },
    m(target, anchor) {
      insert(target, p2, anchor);
      append(p2, t);
    },
    p(ctx2, dirty) {
      if (dirty & 1)
        set_data(t, ctx2[0]);
    },
    d(detaching) {
      if (detaching)
        detach(p2);
    }
  };
}
function create_if_block(ctx) {
  let p2;
  return {
    c() {
      p2 = element("p");
      p2.textContent = "Incorrect Credentials";
      attr(p2, "class", "my-4 text-red-600 font-semibold");
    },
    m(target, anchor) {
      insert(target, p2, anchor);
    },
    d(detaching) {
      if (detaching)
        detach(p2);
    }
  };
}
function create_default_slot(ctx) {
  let textbox0;
  let updating_value;
  let t;
  let textbox1;
  let updating_value_1;
  let current;
  function textbox0_value_binding(value) {
    ctx[8](value);
  }
  let textbox0_props = {
    label: "username",
    lines: 1,
    show_label: true,
    max_lines: 1,
    mode: "dynamic"
  };
  if (ctx[2] !== void 0) {
    textbox0_props.value = ctx[2];
  }
  textbox0 = new Textbox({ props: textbox0_props });
  binding_callbacks.push(() => bind(textbox0, "value", textbox0_value_binding));
  textbox0.$on("submit", ctx[5]);
  function textbox1_value_binding(value) {
    ctx[9](value);
  }
  let textbox1_props = {
    label: "password",
    lines: 1,
    show_label: true,
    max_lines: 1,
    mode: "dynamic",
    type: "password"
  };
  if (ctx[3] !== void 0) {
    textbox1_props.value = ctx[3];
  }
  textbox1 = new Textbox({ props: textbox1_props });
  binding_callbacks.push(() => bind(textbox1, "value", textbox1_value_binding));
  textbox1.$on("submit", ctx[5]);
  return {
    c() {
      create_component(textbox0.$$.fragment);
      t = space();
      create_component(textbox1.$$.fragment);
    },
    m(target, anchor) {
      mount_component(textbox0, target, anchor);
      insert(target, t, anchor);
      mount_component(textbox1, target, anchor);
      current = true;
    },
    p(ctx2, dirty) {
      const textbox0_changes = {};
      if (!updating_value && dirty & 4) {
        updating_value = true;
        textbox0_changes.value = ctx2[2];
        add_flush_callback(() => updating_value = false);
      }
      textbox0.$set(textbox0_changes);
      const textbox1_changes = {};
      if (!updating_value_1 && dirty & 8) {
        updating_value_1 = true;
        textbox1_changes.value = ctx2[3];
        add_flush_callback(() => updating_value_1 = false);
      }
      textbox1.$set(textbox1_changes);
    },
    i(local) {
      if (current)
        return;
      transition_in(textbox0.$$.fragment, local);
      transition_in(textbox1.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(textbox0.$$.fragment, local);
      transition_out(textbox1.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      destroy_component(textbox0, detaching);
      if (detaching)
        detach(t);
      destroy_component(textbox1, detaching);
    }
  };
}
function create_fragment(ctx) {
  let div1;
  let div0;
  let h2;
  let t1;
  let t2;
  let t3;
  let form;
  let t4;
  let button;
  let current;
  let mounted;
  let dispose;
  let if_block0 = ctx[0] && create_if_block_1(ctx);
  let if_block1 = ctx[4] && create_if_block();
  form = new Form({
    props: {
      $$slots: { default: [create_default_slot] },
      $$scope: { ctx }
    }
  });
  return {
    c() {
      div1 = element("div");
      div0 = element("div");
      h2 = element("h2");
      h2.textContent = "Login";
      t1 = space();
      if (if_block0)
        if_block0.c();
      t2 = space();
      if (if_block1)
        if_block1.c();
      t3 = space();
      create_component(form.$$.fragment);
      t4 = space();
      button = element("button");
      button.textContent = "Login";
      attr(h2, "class", "text-2xl font-semibold mb-6");
      attr(button, "class", "gr-button gr-button-lg gr-button-primary w-full mt-4");
      attr(div0, "class", "gr-panel !p-8");
      attr(div1, "class", "dark:bg-gray-950 w-full flex flex-col items-center justify-center");
      toggle_class(div1, "min-h-screen", ctx[1]);
    },
    m(target, anchor) {
      insert(target, div1, anchor);
      append(div1, div0);
      append(div0, h2);
      append(div0, t1);
      if (if_block0)
        if_block0.m(div0, null);
      append(div0, t2);
      if (if_block1)
        if_block1.m(div0, null);
      append(div0, t3);
      mount_component(form, div0, null);
      append(div0, t4);
      append(div0, button);
      current = true;
      if (!mounted) {
        dispose = listen(button, "click", ctx[5]);
        mounted = true;
      }
    },
    p(ctx2, [dirty]) {
      if (ctx2[0]) {
        if (if_block0) {
          if_block0.p(ctx2, dirty);
        } else {
          if_block0 = create_if_block_1(ctx2);
          if_block0.c();
          if_block0.m(div0, t2);
        }
      } else if (if_block0) {
        if_block0.d(1);
        if_block0 = null;
      }
      if (ctx2[4]) {
        if (if_block1)
          ;
        else {
          if_block1 = create_if_block();
          if_block1.c();
          if_block1.m(div0, t3);
        }
      } else if (if_block1) {
        if_block1.d(1);
        if_block1 = null;
      }
      const form_changes = {};
      if (dirty & 1036) {
        form_changes.$$scope = { dirty, ctx: ctx2 };
      }
      form.$set(form_changes);
      if (dirty & 2) {
        toggle_class(div1, "min-h-screen", ctx2[1]);
      }
    },
    i(local) {
      if (current)
        return;
      transition_in(form.$$.fragment, local);
      current = true;
    },
    o(local) {
      transition_out(form.$$.fragment, local);
      current = false;
    },
    d(detaching) {
      if (detaching)
        detach(div1);
      if (if_block0)
        if_block0.d();
      if (if_block1)
        if_block1.d();
      destroy_component(form);
      mounted = false;
      dispose();
    }
  };
}
function instance($$self, $$props, $$invalidate) {
  let { root } = $$props;
  let { id: id2 } = $$props;
  let { auth_message } = $$props;
  let { app_mode: app_mode2 } = $$props;
  window.__gradio_loader__[id2].$set({ status: "complete" });
  let username = "";
  let password = "";
  let incorrect_credentials = false;
  const submit = async () => {
    const formData = new FormData();
    formData.append("username", username);
    formData.append("password", password);
    let response = await fetch(root + "login", { method: "POST", body: formData });
    if (response.status === 400) {
      $$invalidate(4, incorrect_credentials = true);
      $$invalidate(2, username = "");
      $$invalidate(3, password = "");
    } else {
      location.reload();
    }
  };
  function textbox0_value_binding(value) {
    username = value;
    $$invalidate(2, username);
  }
  function textbox1_value_binding(value) {
    password = value;
    $$invalidate(3, password);
  }
  $$self.$$set = ($$props2) => {
    if ("root" in $$props2)
      $$invalidate(6, root = $$props2.root);
    if ("id" in $$props2)
      $$invalidate(7, id2 = $$props2.id);
    if ("auth_message" in $$props2)
      $$invalidate(0, auth_message = $$props2.auth_message);
    if ("app_mode" in $$props2)
      $$invalidate(1, app_mode2 = $$props2.app_mode);
  };
  return [
    auth_message,
    app_mode2,
    username,
    password,
    incorrect_credentials,
    submit,
    root,
    id2,
    textbox0_value_binding,
    textbox1_value_binding
  ];
}
class Login extends SvelteComponent {
  constructor(options) {
    super();
    init(this, options, instance, create_fragment, safe_not_equal, {
      root: 6,
      id: 7,
      auth_message: 0,
      app_mode: 1
    });
  }
}
let id = -1;
window.__gradio_loader__ = [];
const FONTS = "__FONTS_CSS__";
let app_id = null;
let app_mode = window.__gradio_mode__ === "app";
async function reload_check(root) {
  const result = await (await fetch(root + "app_id")).text();
  if (app_id === null) {
    app_id = result;
  } else if (app_id != result) {
    location.reload();
  }
}
async function get_config(source) {
  return window.gradio_config;
}
function mount_custom_css(target, css_string) {
  if (css_string) {
    let style = document.createElement("style");
    style.innerHTML = css_string;
    target.appendChild(style);
  }
}
function mount_css(url, target) {
  const link = document.createElement("link");
  link.rel = "stylesheet";
  link.href = url;
  target.appendChild(link);
  return new Promise((res, rej) => {
    link.addEventListener("load", () => res());
    link.addEventListener("error", () => rej(new Error(`Unable to preload CSS for ${url}`)));
  });
}
async function handle_config(target, source) {
  let config;
  try {
    let [_config] = await Promise.all([
      get_config(source)
    ]);
    config = _config;
  } catch (e) {
    console.error(e);
    return null;
  }
  if (config) {
    mount_custom_css(target, config.css);
    window.__is_colab__ = config.is_colab;
    if (config.root === void 0) {
      config.root = "";
    }
    if (config.dev_mode) {
      reload_check(config.root);
    }
    config.target = target;
  }
  return config;
}
function mount_app(config, target, wrapper, id2, autoscroll, is_embed = false) {
  if (config.detail === "Not authenticated" || config.auth_required) {
    new Login({
      target: wrapper,
      props: {
        auth_message: config.auth_message,
        root: config.root,
        id: id2,
        app_mode
      }
    });
  } else {
    let session_hash = Math.random().toString(36).substring(2);
    config.fn = fn(session_hash, config.root + "run/", config.is_space, is_embed);
    new Blocks({
      target: wrapper,
      props: __spreadProps(__spreadValues({}, config), {
        target: wrapper,
        id: id2,
        autoscroll,
        app_mode
      })
    });
  }
  if (target) {
    target.append(wrapper);
  }
}
function create_custom_element() {
  Array.isArray(FONTS) && FONTS.map((f) => mount_css(f, document.head));
  class GradioApp extends HTMLElement {
    constructor() {
      super();
      this._id = ++id;
      this.root = this.attachShadow({ mode: "open" });
      window.scoped_css_attach = (link) => {
        this.root.append(link);
      };
      this.wrapper = document.createElement("div");
      this.wrapper.classList.add("gradio-container");
      this.wrapper.style.position = "relative";
      this.wrapper.style.width = "100%";
      this.wrapper.style.minHeight = "100vh";
      this.theme = "light";
      window.__gradio_loader__[this._id] = new StatusTracker({
        target: this.wrapper,
        props: {
          status: "pending",
          timer: false,
          queue_position: null,
          queue_size: null
        }
      });
      this.root.append(this.wrapper);
      if (window.__gradio_mode__ !== "website") {
        this.theme = handle_darkmode(this.wrapper);
      }
    }
    async connectedCallback() {
      const event = new CustomEvent("domchange", {
        bubbles: true,
        cancelable: false,
        composed: true
      });
      var observer = new MutationObserver((mutations) => {
        this.dispatchEvent(event);
      });
      observer.observe(this.root, { childList: true });
      const host = this.getAttribute("host");
      const space2 = this.getAttribute("space");
      host ? `https://${host}` : space2 ? (await (await fetch(`https://huggingface.co/api/spaces/${space2}/host`)).json()).host : this.getAttribute("src");
      const control_page_title = this.getAttribute("control_page_title");
      const initial_height = this.getAttribute("initial_height");
      let autoscroll = this.getAttribute("autoscroll");
      const _autoscroll = autoscroll === "true" ? true : false;
      this.wrapper.style.minHeight = initial_height || "300px";
      const config = await handle_config(this.root, null);
      if (config === null) {
        this.wrapper.remove();
      } else {
        mount_app(__spreadProps(__spreadValues({}, config), {
          theme: this.theme,
          control_page_title: control_page_title && control_page_title === "true" ? true : false
        }), this.root, this.wrapper, this._id, _autoscroll, !!space2);
      }
    }
  }
  console.log("defining gradio-app");
  customElements.define("gradio-app", GradioApp);
}
async function unscoped_mount() {
  const target = document.querySelector("#online-eval");
  target.classList.add("gradio-container");
  if (window.__gradio_mode__ !== "website") {
    handle_darkmode(target);
  }
  window.__gradio_loader__[0] = new StatusTracker({
    target,
    props: {
      status: "pending",
      timer: false,
      queue_position: null,
      queue_size: null
    }
  });
  const source = target.getAttribute("data-config");
  console.log("unscoped_mount", source);
  const config = await handle_config(target, source);
  mount_app(__spreadProps(__spreadValues({}, config), { control_page_title: true }), false, target, 0);
}
function handle_darkmode(target) {
  let url = new URL(window.location.toString());
  let theme = "light";
  const color_mode = url.searchParams.get("__theme");
  if (color_mode !== null) {
    if (color_mode === "dark") {
      theme = darkmode(target);
    } else if (color_mode === "system") {
      theme = use_system_theme(target);
    }
  } else if (url.searchParams.get("__dark-theme") === "true") {
    theme = darkmode(target);
  } else {
    theme = use_system_theme(target);
  }
  return theme;
}
function use_system_theme(target) {
  var _a2;
  const theme = update_scheme();
  (_a2 = window == null ? void 0 : window.matchMedia("(prefers-color-scheme: dark)")) == null ? void 0 : _a2.addEventListener("change", update_scheme);
  function update_scheme() {
    var _a3, _b;
    let theme2 = "light";
    const is_dark = (_b = (_a3 = window == null ? void 0 : window.matchMedia) == null ? void 0 : _a3.call(window, "(prefers-color-scheme: dark)").matches) != null ? _b : null;
    if (is_dark) {
      theme2 = darkmode(target);
    }
    return theme2;
  }
  return theme;
}
function darkmode(target) {
  target.classList.add("dark");
  if (app_mode) {
    document.body.style.backgroundColor = "rgb(11, 15, 25)";
  }
  return "dark";
}
if (window.location !== window.parent.location) {
  window.scoped_css_attach = (link) => {
    document.head.append(link);
  };
  unscoped_mount();
} else {
  create_custom_element();
}
export { set_store_value as $, run_all as A, empty as B, destroy_each as C, group_outros as D, check_outros as E, createEventDispatcher as F, spring as G, subscribe as H, binding_callbacks as I, onDestroy as J, bubble as K, add_flush_callback as L, src_url_equal as M, action_destroyer as N, bind as O, Block as P, component_subscribe as Q, assign as R, SvelteComponent as S, StatusTracker as T, get_spread_update as U, get_spread_object as V, stop_propagation as W, X, set_style as Y, get_styles as Z, setContext as _, space as a, writable as a0, getContext as a1, HtmlTag as a2, beforeUpdate as a3, afterUpdate as a4, colors as a5, BlockTitle as a6, set_input_value as a7, flush as a8, tick as a9, update_keyed_each as aa, outro_and_destroy_block as ab, onMount as ac, add_render_callback as ad, select_option as ae, select_value as af, Form as ag, add_resize_listener as ah, ordered_colors as ai, create_bidirectional_transition as aj, fade as ak, to_number as al, create_in_transition as am, create_out_transition as an, colors_1 as ao, destroy_block as ap, Textbox as aq, globals as ar, raf as as, attr as b, create_component as c, toggle_class as d, element as e, insert as f, append as g, set_data as h, init as i, transition_in as j, transition_out as k, listen as l, mount_component as m, detach as n, destroy_component as o, create_slot as p, get_all_dirty_from_scope as q, get_slot_changes as r, safe_not_equal as s, text as t, update_slot_base as u, create_classes as v, svg_element as w, noop as x, is_function as y, prevent_default as z };
//# sourceMappingURL=main.js.map
