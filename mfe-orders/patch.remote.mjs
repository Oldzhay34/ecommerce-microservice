import { readFileSync, writeFileSync } from 'fs';

const path = 'dist/assets/remoteEntry.js';
let code = readFileSync(path, 'utf8');

// __v__css__ içeren dynamicLoadingCss çağrılarını sil.
code = code.replace(/[a-zA-Z]\(`__v__css__[^`]*`[^)]*\),/g, '');

writeFileSync(path, code);
console.log('remoteEntry.js patched: __v__css__ çağrıları temizlendi.');