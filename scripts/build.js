const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const source = path.join(root, 'web');
const target = path.join(root, 'www');
fs.rmSync(target, { recursive: true, force: true });
fs.cpSync(source, target, { recursive: true });
console.log(`Built web assets: ${path.relative(root, source)} -> ${path.relative(root, target)}`);
