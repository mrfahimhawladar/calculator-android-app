(() => {
  'use strict';
  const Core = window.CalculatorCore;
  const $ = id => document.getElementById(id);
  const app = $('app'), resultEl = $('result'), expressionEl = $('expression'), angleBtn = $('angleMode');
  const historyList = $('historyList'), drawer = $('drawer'), toastEl = $('toast'), memoryIndicator = $('memoryIndicator');
  const STORAGE = { history: 'calculator.history.v2', memory: 'calculator.memory.v2', angle: 'calculator.angle.v2', last: 'calculator.last.v2' };
  const readJSON = (key, fallback) => { try { const v = localStorage.getItem(key); return v === null ? fallback : JSON.parse(v); } catch { return fallback; } };
  let input = '0', justEvaluated = false, expression = '', history = readJSON(STORAGE.history, []), memory = readJSON(STORAGE.memory, null), angleMode = readJSON(STORAGE.angle, 'DEG'), lastResult = String(readJSON(STORAGE.last, '0')), toastTimer = null;
  if (!Array.isArray(history)) history = [];
  if (!['DEG', 'RAD'].includes(angleMode)) angleMode = 'DEG';
  if (typeof memory !== 'number' || !Number.isFinite(memory)) memory = null;
  const isNumeric = value => typeof value === 'string' && value.trim() !== '' && value !== 'Error' && Number.isFinite(Number(value));
  const evaluate = value => Core.evaluate(value, { angleMode });
  const fmt = Core.format;
  function save(key, value) { try { localStorage.setItem(key, JSON.stringify(value)); } catch { toast('Storage is unavailable in this session'); } }
  function toast(message) { toastEl.textContent = message; toastEl.classList.add('show'); if (toastTimer) clearTimeout(toastTimer); toastTimer = setTimeout(() => { toastEl.classList.remove('show'); toastTimer = null; }, 1800); }
  function pretty(value) { return value.replace(/\*/g, '×').replace(/\//g, '÷').replace(/-/g, '−'); }
  function render() {
    resultEl.textContent = input;
    resultEl.classList.toggle('small', input.length > 10);
    resultEl.setAttribute('aria-label', 'Current value: ' + input + '. Activate to copy');
    let preview = '';
    if (!justEvaluated && input !== 'Error' && /[+−×÷^()%!()πe]/.test(input)) {
      try { preview = '≈ ' + fmt(evaluate(input)); } catch { preview = ''; }
    }
    expressionEl.textContent = expression || preview;
    memoryIndicator.classList.toggle('active', memory !== null);
    memoryIndicator.setAttribute('aria-hidden', memory === null ? 'true' : 'false');
    angleBtn.textContent = angleMode;
    angleBtn.setAttribute('aria-label', 'Angle mode: ' + (angleMode === 'DEG' ? 'degrees' : 'radians') + '. Activate to switch');
  }
  function persistHistory() { save(STORAGE.history, history.slice(0, 100)); }
  function addHistory(source, result) {
    history.unshift({ source, result, time: Date.now() });
    history = history.slice(0, 100);
    persistHistory(); renderHistory();
  }
  function renderHistory() {
    historyList.replaceChildren();
    if (!history.length) {
      const empty = document.createElement('div'); empty.className = 'history-empty'; empty.textContent = 'No calculations yet. Your completed calculations will appear here.'; historyList.appendChild(empty); return;
    }
    for (const item of history) {
      if (!item || typeof item.source !== 'string' || typeof item.result !== 'string') continue;
      const button = document.createElement('button'); button.className = 'history-item'; button.type = 'button'; button.setAttribute('aria-label', item.source + ' equals ' + item.result);
      const expr = document.createElement('div'); expr.className = 'history-expr'; expr.textContent = item.source + ' =';
      const result = document.createElement('div'); result.className = 'history-result'; result.textContent = item.result;
      button.append(expr, result); button.addEventListener('click', () => { input = item.result; expression = item.source + ' ='; justEvaluated = true; closeDrawer(); render(); }); historyList.appendChild(button);
    }
  }
  function openDrawer() { app.classList.add('history-open'); drawer.inert = false; drawer.setAttribute('aria-hidden', 'false'); $('closeHistory').focus(); }
  function closeDrawer() { app.classList.remove('history-open'); drawer.setAttribute('aria-hidden', 'true'); drawer.inert = true; app.focus({ preventScroll: true }); }
  function appendDigit(digit) { if (input === 'Error') input = '0'; if (justEvaluated) { input = digit; expression = ''; justEvaluated = false; } else if (input === '0') input = digit; else if (input.length < 100) input += digit; render(); }
  function decimal() {
    if (justEvaluated) { input = '0.'; expression = ''; justEvaluated = false; }
    else {
      if (input === 'Error') input = '0';
      if (/[πe)%!]$/.test(input)) input += '×0.';
      else { const segment = input.split(/[+\-−×÷^()%!()]/).pop(); if (!segment.includes('.')) { if (/[+\-−×÷^(]$/.test(input)) input += '0.'; else input += '.'; } }
    }
    render();
  }
  function insertOperator(op) {
    if (input === 'Error') return;
    if (justEvaluated) { justEvaluated = false; expression = ''; }
    const last = input.slice(-1);
    if ('+−-×÷^'.includes(last)) input = input.slice(0, -1) + op;
    else if (input.length < 120) input += op;
    render();
  }
  function insertParen(which) {
    if (which === 'open-paren') {
      if (justEvaluated || input === '0' || input === 'Error') { input = '('; expression = ''; justEvaluated = false; }
      else if (/[0-9πe)]$/.test(input)) input += '×(';
      else input += '(';
    } else {
      const opens = (input.match(/\(/g) || []).length, closes = (input.match(/\)/g) || []).length;
      if (opens > closes && /[0-9πe)%!]$/.test(input)) input += ')'; else toast('No matching opening parenthesis');
    }
    render();
  }
  function calculate() {
    if (input === 'Error') { toast('Enter a valid expression'); return; }
    const source = input;
    try { const result = fmt(evaluate(source)); expression = pretty(source) + ' ='; input = result; lastResult = result; save(STORAGE.last, lastResult); addHistory(pretty(source), result); justEvaluated = true; }
    catch (err) { toast(err.message || 'Invalid expression'); }
    render();
  }
  function backspace() { if (justEvaluated || input === 'Error') { input = '0'; expression = ''; justEvaluated = false; } else { input = input.length > 1 ? input.slice(0, -1) : '0'; if (input === '-' || input === '') input = '0'; } render(); }
  function unary(label, fn) {
    if (input === 'Error') { toast('Enter a valid expression'); return; }
    try { const source = input, value = evaluate(source), result = fmt(fn(value)); input = result; expression = label + '(' + pretty(source) + ') ='; lastResult = result; save(STORAGE.last, lastResult); addHistory(expression.slice(0, -2), result); justEvaluated = true; }
    catch (err) { toast(err.message || 'Invalid expression'); }
    render();
  }
  function percent() {
    if (input === 'Error') return;
    try {
      input = Core.applyPercent(input, { angleMode });
      expression = ''; justEvaluated = false; render();
    } catch (err) { toast(err.message || 'Invalid percentage'); }
  }
  function factorial() { unary('factorial', value => Core.factorial(value)); }
  function sign() {
    if (input === 'Error') { input = '0'; expression = ''; justEvaluated = false; render(); return; }
    try {
      if (justEvaluated) { input = fmt(-Number(input)); expression = ''; justEvaluated = false; }
      else { const toggled = Core.toggleTrailingNumberSign(input); if (toggled !== null) input = toggled; else if (/[+\-−×÷^(]$/.test(input)) input += '−'; else input = fmt(-evaluate(input)); }
    } catch { input = '0'; expression = ''; justEvaluated = false; toast('Enter a valid number'); }
    render();
  }
  function constant(value) { if (justEvaluated || input === 'Error') { input = value; expression = ''; justEvaluated = false; } else if (input === '0') input = value; else if (/[0-9πe)%!]$/.test(input)) input += '×' + value; else input += value; render(); }
  function memoryAction(action) {
    try {
      if (action === 'memory-clear') { memory = null; save(STORAGE.memory, memory); render(); toast('Memory cleared'); return; }
      if (action === 'memory-recall') { if (memory === null) { toast('Memory is empty'); return; } input = fmt(memory); expression = 'Memory recall'; justEvaluated = true; render(); return; }
      if (input === 'Error') throw new Error('Enter a valid expression');
      const value = evaluate(input); memory = (memory === null ? 0 : memory) + (action === 'memory-add' ? value : -value);
      if (!Number.isFinite(memory)) throw new Error('Memory overflow'); save(STORAGE.memory, memory); render(); toast('Memory updated');
    } catch (err) { toast(err.message || 'Memory operation failed'); }
  }
  async function copyResult() {
    if (!isNumeric(input)) { toast('No valid result to copy'); return; }
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) await navigator.clipboard.writeText(input);
      else { const field = document.createElement('textarea'); field.value = input; field.readOnly = true; field.style.position = 'fixed'; field.style.opacity = '0'; document.body.appendChild(field); field.select(); const ok = document.execCommand('copy'); field.remove(); if (!ok) throw new Error('Clipboard unavailable'); }
      toast('Copied');
    } catch { toast('Unable to copy'); }
  }
  function action(name) {
    switch (name) {
      case 'clear': input = '0'; expression = ''; justEvaluated = false; render(); break;
      case 'sign': sign(); break;
      case 'percent': percent(); break;
      case 'decimal': decimal(); break;
      case 'equals': calculate(); break;
      case 'sqrt': unary('√', Math.sqrt); break;
      case 'abs': unary('abs', Math.abs); break;
      case 'square': unary('square', v => v * v); break;
      case 'sin': unary('sin', v => Math.sin(angleMode === 'DEG' ? v * Math.PI / 180 : v)); break;
      case 'cos': unary('cos', v => Math.cos(angleMode === 'DEG' ? v * Math.PI / 180 : v)); break;
      case 'tan': unary('tan', v => { const a = angleMode === 'DEG' ? v * Math.PI / 180 : v; if (Math.abs(Math.cos(a)) < 1e-12) throw new Error('Tangent is undefined at this angle'); return Math.tan(a); }); break;
      case 'log': unary('log', v => { if (v <= 0) throw new Error('log needs a positive number'); return Math.log10(v); }); break;
      case 'ln': unary('ln', v => { if (v <= 0) throw new Error('ln needs a positive number'); return Math.log(v); }); break;
      case 'factorial': factorial(); break;
      case 'reciprocal': unary('1/', v => { if (v === 0) throw new Error('Cannot divide by zero'); return 1 / v; }); break;
      case 'pi': constant('π'); break;
      case 'euler': constant('e'); break;
      case 'open-paren': case 'close-paren': insertParen(name); break;
      case 'backspace': backspace(); break;
      case 'memory-add': case 'memory-subtract': case 'memory-recall': case 'memory-clear': memoryAction(name); break;
      case 'ans': constant(lastResult); break;
    }
  }
  document.querySelector('.keys').addEventListener('click', event => { const button = event.target.closest('button'); if (!button) return; if (button.dataset.num !== undefined) appendDigit(button.dataset.num); else if (button.dataset.op) insertOperator(button.dataset.op); else if (button.dataset.action) action(button.dataset.action); });
  document.querySelector('.science-grid').addEventListener('click', event => { const button = event.target.closest('button'); if (!button) return; if (button.dataset.op) insertOperator(button.dataset.op); else if (button.dataset.action) action(button.dataset.action); });
  angleBtn.addEventListener('click', () => { angleMode = angleMode === 'DEG' ? 'RAD' : 'DEG'; save(STORAGE.angle, angleMode); render(); toast('Angle mode: ' + angleMode); });
  resultEl.addEventListener('click', copyResult);
  resultEl.addEventListener('keydown', event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); copyResult(); } });
  $('historyBtn').addEventListener('click', openDrawer); $('closeHistory').addEventListener('click', closeDrawer); $('scrim').addEventListener('click', closeDrawer);
  $('clearHistory').addEventListener('click', () => { history = []; persistHistory(); renderHistory(); toast('History cleared'); });
  $('useLatest').addEventListener('click', () => { if (!isNumeric(lastResult)) { toast('No result available'); return; } input = lastResult; expression = 'Last answer'; justEvaluated = true; closeDrawer(); render(); });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') { if (app.classList.contains('history-open')) { event.preventDefault(); closeDrawer(); } return; }
    const target = event.target, isControl = target instanceof HTMLElement && (target.tagName === 'BUTTON' || target.id === 'result');
    if (isControl && (event.key === ' ' || event.key === 'Enter')) return;
    if (/^[0-9]$/.test(event.key)) { event.preventDefault(); appendDigit(event.key); }
    else if (event.key === '.') { event.preventDefault(); decimal(); }
    else if (event.key === '+') { event.preventDefault(); insertOperator('+'); }
    else if (event.key === '-') { event.preventDefault(); insertOperator('−'); }
    else if (event.key === '*') { event.preventDefault(); insertOperator('×'); }
    else if (event.key === '/') { event.preventDefault(); insertOperator('÷'); }
    else if (event.key === '^') { event.preventDefault(); insertOperator('^'); }
    else if (event.key === '(') { event.preventDefault(); insertParen('open-paren'); }
    else if (event.key === ')') { event.preventDefault(); insertParen('close-paren'); }
    else if (event.key === 'Enter' || event.key === '=') { event.preventDefault(); calculate(); }
    else if (event.key === 'Backspace') { event.preventDefault(); backspace(); }
    else if (event.key === '%') { event.preventDefault(); percent(); }
  });
  render(); renderHistory();
})();
