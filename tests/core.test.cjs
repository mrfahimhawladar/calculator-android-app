const test = require('node:test');
const assert = require('node:assert/strict');
const Core = require('../web/src/calculator-core.js');
const close = (actual, expected, epsilon = 1e-10) => assert.ok(Math.abs(actual - expected) <= epsilon, `${actual} is not close to ${expected}`);

test('respects multiplication/division precedence', () => assert.equal(Core.evaluate('2+3*4'), 14));
test('supports nested parentheses', () => assert.equal(Core.evaluate('(2+3)*(4-(1+1))'), 10));
test('supports decimal and scientific-notation numbers', () => { assert.equal(Core.evaluate('0.25+0.5'), 0.75); assert.equal(Core.evaluate('1e3'), 1000); });
test('supports unary operators and exponent precedence', () => { assert.equal(Core.evaluate('-(-3+1)'), 2); assert.equal(Core.evaluate('-2^2'), -4); assert.equal(Core.evaluate('2^-2'), 0.25); assert.equal(Core.evaluate('2^3^2'), 512); });
test('supports implicit multiplication and constants', () => { assert.equal(Core.evaluate('2(3+4)'), 14); assert.equal(Core.evaluate('(2+3)(4)'), 20); close(Core.evaluate('2pi'), 2 * Math.PI); });
test('supports visible operators and percent postfix', () => { assert.equal(Core.evaluate('8÷2×3−1'), 11); assert.equal(Core.evaluate('200*10%'), 20); assert.equal(Core.evaluate('50%'), 0.5); });
test('supports factorial and consistent formatting', () => { assert.equal(Core.evaluate('5!'), 120); assert.equal(Core.format(0.1 + 0.2), '0.3'); assert.equal(Core.format(1234567.8912), '1234567.8912'); });
test('supports trigonometry in degrees and radians', () => { close(Core.evaluate('sin(30)', { angleMode: 'DEG' }), 0.5); close(Core.evaluate('cos(0)', { angleMode: 'RAD' }), 1); });
test('supports square root, absolute value and logarithms', () => { assert.equal(Core.evaluate('sqrt(81)'), 9); assert.equal(Core.evaluate('abs(-13)'), 13); assert.equal(Core.evaluate('log(1000)'), 3); close(Core.evaluate('ln(e)'), 1); });
test('rejects division by zero', () => assert.throws(() => Core.evaluate('5/0'), /divide by zero/i));
test('rejects malformed numeric tokens', () => assert.throws(() => Core.evaluate('1..2')));
test('rejects incomplete expressions and missing parentheses', () => { assert.throws(() => Core.evaluate('3+'), /Incomplete expression/); assert.throws(() => Core.evaluate('(2+3'), /Missing closing parenthesis/); });
test('rejects unknown identifiers and unsafe JavaScript input', () => { assert.throws(() => Core.evaluate('2+alert(1)'), /Unknown function or constant/); assert.throws(() => Core.evaluate('process.exit()')); assert.throws(() => Core.evaluate('1+*2')); });
test('rejects function domain errors and tangent singularities', () => { assert.throws(() => Core.evaluate('log(0)')); assert.throws(() => Core.evaluate('sqrt(-1)')); assert.throws(() => Core.evaluate('tan(90)', { angleMode: 'DEG' })); });
test('enforces factorial bounds', () => { assert.throws(() => Core.factorial(-1)); assert.throws(() => Core.factorial(171)); });
test('calculator-style percent handles subtraction with the Unicode minus sign', () => { const expression = Core.applyPercent('200−10', { angleMode: 'DEG' }); assert.equal(expression, '200−20'); assert.equal(Core.evaluate(expression), 180); });
test('calculator-style percent uses the left base for addition/subtraction only', () => { assert.equal(Core.evaluate(Core.applyPercent('200+10')), 220); assert.equal(Core.evaluate(Core.applyPercent('200×10')), 20); assert.equal(Core.evaluate(Core.applyPercent('200÷10')), 2000); assert.equal(Core.applyPercent('12.5'), '0.125'); assert.equal(Core.applyPercent('−10'), '−0.1'); assert.equal(Core.evaluate(Core.applyPercent('2×−10')), -0.2); });
test('sign toggle preserves binary subtraction and handles unary signs', () => { assert.equal(Core.toggleTrailingNumberSign('2−3'), '2−−3'); assert.equal(Core.evaluate(Core.toggleTrailingNumberSign('2−3')), 5); assert.equal(Core.evaluate(Core.toggleTrailingNumberSign('2+3')), -1); assert.equal(Core.evaluate(Core.toggleTrailingNumberSign('2×−3')), 6); assert.equal(Core.evaluate(Core.toggleTrailingNumberSign('2−−3')), -1); assert.equal(Core.evaluate(Core.toggleTrailingNumberSign('1e+3')), -1000); assert.equal(Core.toggleTrailingNumberSign('π'), null); });

test('rejects exponentiation overflow instead of returning Infinity', () => { assert.throws(() => Core.evaluate('10^1000'), /Math error/); });
