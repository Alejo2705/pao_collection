const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const elements = new Map();
const getElement = id => {
  if (!elements.has(id)) elements.set(id, {
    hidden: false, textContent: '', attributes: {},
    setAttribute(k, v) { this.attributes[k] = v; },
    removeAttribute(k) { delete this[k]; }
  });
  return elements.get(id);
};
const context = vm.createContext({
  document: { getElementById: getElement, addEventListener() {} },
  window: {}, URL, AbortSignal
});
vm.runInContext(fs.readFileSync('src/main/resources/static/app.js', 'utf8'), context);
const profile = { id: '123456', username: '<img onerror=alert(1)>', name: 'Prueba', profile_picture_url: 'https://example.com/photo.jpg' };
const ok = data => ({ ok: true, json: async () => data });
(async () => {
  context.fetch = async () => ok(profile);
  await context.cargarCuentaInstagram();
  assert.equal(getElement('igCuentaEstado').textContent, 'Conectada');
  assert.equal(getElement('igCuentaUsername').textContent, '@' + profile.username);
  assert.equal(getElement('igCuentaDetalle').hidden, false);
  getElement('igCuentaFoto').onload();
  assert.equal(getElement('igCuentaAvatar').hidden, true);
  getElement('igCuentaFoto').onerror();
  assert.equal(getElement('igCuentaAvatar').hidden, false);
  context.fetch = async () => ({ ok: false, json: async () => { throw Error('no debe leer secretos'); } });
  await context.cargarCuentaInstagram();
  assert.equal(getElement('igCuentaEstado').textContent, 'No verificada');
  assert.equal(getElement('igCuentaDetalle').hidden, true);
  assert.equal(getElement('igCuenta').attributes['aria-busy'], 'false');
  context.fetch = async () => ok({ id: '123456' });
  await context.cargarCuentaInstagram();
  assert.equal(getElement('igCuentaEstado').textContent, 'No verificada');
  context.fetch = async () => ok({ ...profile, profile_picture_url: 'javascript:alert(1)' });
  await context.cargarCuentaInstagram();
  assert.equal(getElement('igCuentaFoto').src, undefined);
  let resolveOld;
  context.fetch = () => new Promise(resolve => { resolveOld = resolve; });
  const old = context.cargarCuentaInstagram();
  context.fetch = async () => ({ ok: false });
  await context.cargarCuentaInstagram();
  resolveOld(ok(profile));
  await old;
  assert.equal(getElement('igCuentaEstado').textContent, 'No verificada');
  console.log('UI OK: perfil, texto seguro, avatar, error, respuesta incompleta, URL y concurrencia.');
})().catch(error => { console.error(error); process.exitCode = 1; });
