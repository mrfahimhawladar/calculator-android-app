(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.CalculatorCore = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  'use strict';

  function tokenize(source) {
    const text = String(source).replace(/[×]/g, '*').replace(/[÷]/g, '/').replace(/[−]/g, '-').replace(/π/g, 'pi');
    const tokens = [];
    let i = 0;
    while (i < text.length) {
      const c = text[i];
      if (/\s/.test(c)) { i++; continue; }
      if (/[0-9.]/.test(c)) {
        const match = text.slice(i).match(/^(?:\d+\.?\d*|\.\d+)(?:[eE][+\-]?\d+)?/);
        if (!match) throw new Error('Invalid number');
        const raw = match[0];
        const value = Number(raw);
        if (!Number.isFinite(value)) throw new Error('Invalid number');
        tokens.push({ type: 'num', value });
        i += raw.length;
        continue;
      }
      if (/[a-zA-Z]/.test(c)) {
        const start = i;
        while (i < text.length && /[a-zA-Z]/.test(text[i])) i++;
        const name = text.slice(start, i).toLowerCase();
        if (!['pi', 'e', 'sin', 'cos', 'tan', 'log', 'ln', 'sqrt', 'abs'].includes(name)) throw new Error('Unknown function or constant');
        tokens.push({ type: ['pi', 'e'].includes(name) ? 'const' : 'func', value: name });
        continue;
      }
      if ('+-*/^()%!'.includes(c)) {
        const type = c === '(' ? 'lp' : c === ')' ? 'rp' : c === '%' || c === '!' ? 'postfix' : 'op';
        tokens.push({ type, value: c });
        i++;
        continue;
      }
      throw new Error('Invalid input');
    }
    const out = [];
    const endsValue = t => t && ['num', 'const', 'rp', 'postfix'].includes(t.type);
    const startsValue = t => t && ['num', 'const', 'lp', 'func'].includes(t.type);
    for (const token of tokens) {
      const prev = out[out.length - 1];
      if (endsValue(prev) && startsValue(token) && !(prev.type === 'num' && token.type === 'num')) out.push({ type: 'op', value: '*' });
      out.push(token);
    }
    return out;
  }

  function factorial(value) {
    if (!Number.isInteger(value) || value < 0) throw new Error('Factorial needs a non-negative integer');
    if (value > 170) throw new Error('Factorial value is too large');
    let result = 1;
    for (let i = 2; i <= value; i++) result *= i;
    return result;
  }

  function evaluate(source, options) {
    const opts = options || {};
    const angleMode = opts.angleMode === 'RAD' ? 'RAD' : 'DEG';
    const tokens = tokenize(source);
    let index = 0;
    const peek = () => tokens[index];
    const take = () => tokens[index++];
    const finite = n => { if (!Number.isFinite(n)) throw new Error('Math error'); return n; };

    function primary() {
      const token = take();
      if (!token) throw new Error('Incomplete expression');
      if (token.type === 'num') return token.value;
      if (token.type === 'const') return token.value === 'pi' ? Math.PI : Math.E;
      if (token.type === 'lp') {
        const value = addSub();
        if (!peek() || peek().type !== 'rp') throw new Error('Missing closing parenthesis');
        take();
        return value;
      }
      if (token.type === 'func') {
        if (!peek() || peek().type !== 'lp') throw new Error('Function needs parentheses');
        take();
        const value = addSub();
        if (!peek() || peek().type !== 'rp') throw new Error('Missing closing parenthesis');
        take();
        const angle = angleMode === 'DEG' ? value * Math.PI / 180 : value;
        switch (token.value) {
          case 'sin': return Math.sin(angle);
          case 'cos': return Math.cos(angle);
          case 'tan': if (Math.abs(Math.cos(angle)) < 1e-12) throw new Error('Tangent is undefined at this angle'); return Math.tan(angle);
          case 'log': if (value <= 0) throw new Error('log needs a positive number'); return Math.log10(value);
          case 'ln': if (value <= 0) throw new Error('ln needs a positive number'); return Math.log(value);
          case 'sqrt': if (value < 0) throw new Error('Square root needs a non-negative number'); return Math.sqrt(value);
          case 'abs': return Math.abs(value);
          default: throw new Error('Unknown function');
        }
      }
      throw new Error('Invalid expression');
    }
    function postfix() {
      let value = primary();
      while (peek() && peek().type === 'postfix') {
        const op = take().value;
        value = op === '%' ? value / 100 : factorial(value);
      }
      return finite(value);
    }
    function power() {
      const left = postfix();
      if (peek() && peek().type === 'op' && peek().value === '^') {
        take();
        return finite(Math.pow(left, unary()));
      }
      return left;
    }
    function unary() {
      if (peek() && peek().type === 'op' && (peek().value === '+' || peek().value === '-')) {
        const op = take().value;
        const value = unary();
        return op === '-' ? -value : value;
      }
      return power();
    }
    function mulDiv() {
      let value = unary();
      while (peek() && peek().type === 'op' && (peek().value === '*' || peek().value === '/')) {
        const op = take().value;
        const right = unary();
        if (op === '/' && right === 0) throw new Error('Cannot divide by zero');
        value = finite(op === '*' ? value * right : value / right);
      }
      return value;
    }
    function addSub() {
      let value = mulDiv();
      while (peek() && peek().type === 'op' && (peek().value === '+' || peek().value === '-')) {
        const op = take().value;
        const right = mulDiv();
        value = finite(op === '+' ? value + right : value - right);
      }
      return value;
    }
    if (!tokens.length) throw new Error('Enter an expression');
    const result = finite(addSub());
    if (index !== tokens.length) throw new Error('Check the expression or parentheses');
    return result;
  }

  function toggleTrailingNumberSign(source) {
    const text = String(source);
    const match = text.match(/(\d*\.?\d+(?:[eE][+\-]?\d+)?)$/);
    if (!match) return null;
    const before = text.slice(0, match.index);
    const trailingSign = before.slice(-1);
    if (trailingSign === '-' || trailingSign === '−') {
      const beforeSign = before.slice(0, -1);
      if (beforeSign === '' || /[+\-−×÷^(]$/.test(beforeSign)) return beforeSign + match[0];
      return before + '−' + match[0];
    }
    return before + '−' + match[0];
  }

  function applyPercent(source, options) {
    const text = String(source);
    const match = text.match(/(\d*\.?\d+(?:[eE][+\-]?\d+)?)$/);
    if (!match) throw new Error('Enter a number first');
    const value = Number(match[0]);
    if (!Number.isFinite(value)) throw new Error('Invalid percentage');
    const prefix = text.slice(0, match.index);
    const minus = prefix.match(/([\-−])\s*$/);
    if (minus) {
      const beforeSign = prefix.slice(0, minus.index);
      const unaryMinus = beforeSign === '' || /[+\-−×÷^(]$/.test(beforeSign);
      if (unaryMinus) return prefix + format(value / 100);
      const left = beforeSign.trim();
      if (!left) throw new Error('Enter a base value');
      return prefix + format(evaluate(left, options) * value / 100);
    }
    const plus = prefix.match(/\+\s*$/);
    if (plus) {
      const left = prefix.slice(0, plus.index).trim();
      if (!left) throw new Error('Enter a base value');
      return prefix + format(evaluate(left, options) * value / 100);
    }
    return prefix + format(value / 100);
  }

  function format(value) {
    if (!Number.isFinite(value)) throw new Error('Math error');
    if (Math.abs(value) < 1e-12) return '0';
    return String(Number(value.toPrecision(12)));
  }

  return { tokenize, evaluate, format, factorial, toggleTrailingNumberSign, applyPercent };
});
