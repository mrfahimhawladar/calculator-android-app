const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const Core = require('../web/src/calculator-core.js');

class FakeClassList {
  constructor() { this.values = new Set(); }
  add(...names) { for (const name of names) this.values.add(name); }
  remove(...names) { for (const name of names) this.values.delete(name); }
  contains(name) { return this.values.has(name); }
  toggle(name, force) {
    const shouldAdd = force === undefined ? !this.values.has(name) : Boolean(force);
    if (shouldAdd) this.values.add(name);
    else this.values.delete(name);
    return shouldAdd;
  }
}

class FakeElement {
  constructor(tagName = 'div') {
    this.tagName = String(tagName).toUpperCase();
    this.dataset = {};
    this.className = '';
    this.textContent = '';
    this.inert = false;
    this.children = [];
    this.attributes = {};
    this.listeners = new Map();
    this.classList = new FakeClassList();
  }
  addEventListener(type, callback) {
    const callbacks = this.listeners.get(type) || [];
    callbacks.push(callback);
    this.listeners.set(type, callbacks);
  }
  setAttribute(name, value) { this.attributes[name] = String(value); }
  appendChild(child) { this.children.push(child); return child; }
  append(...children) { this.children.push(...children); }
  replaceChildren(...children) { this.children = [...children]; }
  focus() {}
  closest() { return this; }
}

function createAppHarness() {
  const nodes = new Map();
  const getNode = key => {
    if (!nodes.has(key)) nodes.set(key, new FakeElement());
    return nodes.get(key);
  };
  const document = {
    getElementById: id => getNode('id:' + id),
    querySelector: selector => getNode('selector:' + selector),
    createElement: tagName => new FakeElement(tagName),
    addEventListener(type, callback) { getNode('document').addEventListener(type, callback); }
  };
  const stored = new Map();
  const localStorage = {
    getItem(key) { return stored.has(key) ? stored.get(key) : null; },
    setItem(key, value) { stored.set(key, String(value)); }
  };
  const appPath = path.join(__dirname, '..', 'web', 'src', 'app.js');
  const appSource = fs.readFileSync(appPath, 'utf8');
  const context = {
    window: { CalculatorCore: Core },
    document,
    localStorage,
    HTMLElement: FakeElement,
    setTimeout: () => 1,
    clearTimeout: () => {}
  };
  vm.runInNewContext(appSource, context, { filename: appPath });
  function click(rootSelector, dataset) {
    const target = new FakeElement('button');
    target.dataset = dataset;
    const root = document.querySelector(rootSelector);
    const handler = (root.listeners.get('click') || [])[0];
    assert.equal(typeof handler, 'function', 'expected delegated click handler');
    handler({ target });
  }
  return { click, result: document.getElementById('result'), localStorage };
}

test('M+ overflow is rejected and MR can still recall the last valid memory', () => {
  const app = createAppHarness();

  // Calculate 10^308, a very large but finite IEEE-754 number.
  app.click('.keys', { num: '1' });
  app.click('.keys', { num: '0' });
  app.click('.keys', { op: '^' });
  app.click('.keys', { num: '3' });
  app.click('.keys', { num: '0' });
  app.click('.keys', { num: '8' });
  app.click('.keys', { action: 'equals' });
  assert.equal(app.result.textContent, '1e+308');

  // First M+ stores the value; a second M+ must fail without poisoning memory.
  app.click('.science-grid', { action: 'memory-add' });
  app.click('.science-grid', { action: 'memory-add' });

  // Change the display before MR, so a failed recall cannot pass accidentally.
  app.click('.keys', { action: 'clear' });
  assert.equal(app.result.textContent, '0');
  app.click('.science-grid', { action: 'memory-recall' });
  assert.equal(app.result.textContent, '1e+308');
  assert.equal(JSON.parse(app.localStorage.getItem('calculator.memory.v2')), 1e308);
});
