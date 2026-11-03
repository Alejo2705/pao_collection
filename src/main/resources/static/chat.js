(() => {
  const log = document.getElementById('chatLog');
  const form = document.getElementById('chatForm');
  const input = document.getElementById('chatMensaje');
  const status = document.getElementById('chatStatus');
  const quote = document.getElementById('chatPropuesta');
  const contact = document.getElementById('chatConfirmacion');
  let busy = false, state = null;
  const money = n => new Intl.NumberFormat('es-PE', {style:'currency', currency:'PEN'}).format(n);
  function line(role, text) {
    const box = document.createElement('div'); box.className = 'chat-line ' + role;
    const label = document.createElement('strong'); label.textContent = role === 'cliente' ? 'Tú' : 'Pao · Asistente IA';
    const p = document.createElement('p'); p.textContent = text; box.append(label,p); log.append(box);
  }
  function render(s) {
    state = s; log.replaceChildren();
    if (!s.mensajes.length) line('asistente','¡Hola! 😊 Soy el asistente de Pao Collection. ¿Buscas algo para ti o un regalo? Cuéntame qué accesorio te gusta y te ayudo con modelos, precios y disponibilidad.');
    s.mensajes.forEach(m => line(m.rol,m.texto));
    quote.replaceChildren();
    if(s.puedeConfirmar) {
      const title = document.createElement('h3'); title.textContent = 'Revisa tu pedido'; quote.append(title);
      let total = 0;
      s.propuesta.forEach(i => {const p=document.createElement('p');p.textContent=`${i.cantidad} × ${i.nombre} · ${money(i.subtotal)}`;quote.append(p);total+=Number(i.subtotal);});
      const p=document.createElement('strong');p.textContent=`Total: ${money(total)}`;quote.append(p);
      const note=document.createElement('p');note.className='micro';note.textContent='Resumen válido por 15 minutos. Precio y stock se verifican al confirmar. No incluye envío ni implica un pago.';quote.append(note);
    }
    quote.hidden=!s.puedeConfirmar;contact.hidden=!s.puedeConfirmar;
    input.disabled=!!s.pedidoId; document.getElementById('chatEnviar').disabled=!!s.pedidoId;
    log.scrollTop=log.scrollHeight;
  }
  async function request(path,body) {
    const options={credentials:'same-origin',headers:{Accept:'application/json'}};
    if(body!==undefined) {
      const csrfResponse=await fetch('/auth/csrf',{credentials:'same-origin',cache:'no-store'});
      if(!csrfResponse.ok)throw new Error('No pudimos preparar la sesión. Recarga la página.');
      const csrf=await csrfResponse.json();options.method='POST';options.headers['Content-Type']='application/json';options.headers[csrf.headerName]=csrf.token;options.body=JSON.stringify(body);
    }
    const r=await fetch('/api/chat/'+path,options);
    if(!r.ok) {
      if(r.status===429)throw new Error('Vamos un poquito más despacio 😊 Espera un minuto antes de continuar.');
      if(r.status===403)throw new Error('La sesión cambió. Recarga la página para continuar.');
      if(r.status===503)throw new Error('El asistente no está disponible en este momento. Intenta de nuevo en unos minutos; tu pedido no se ha registrado.');
      const e=await r.json().catch(()=>({}));throw new Error(e.detail || e.message || e.mensaje || 'No pudimos completar la solicitud. Inténtalo de nuevo.');
    }
    return r.json();
  }
  async function run(action) {
    if(busy)return;busy=true;status.textContent='Un momento, por favor…';log.setAttribute('aria-busy','true');
    form.querySelector('button').disabled=true;contact.querySelector('button').disabled=true;document.getElementById('chatNueva').disabled=true;
    try{await action();status.textContent='';}catch(e){status.textContent=e.message;}
    finally{busy=false;log.setAttribute('aria-busy','false');form.querySelector('button').disabled=!!state?.pedidoId;contact.querySelector('button').disabled=false;document.getElementById('chatNueva').disabled=false;}
  }
  form.addEventListener('submit',e=>{e.preventDefault();const text=input.value.trim();if(!text)return;run(async()=>{const s=await request('mensajes',{mensaje:text});input.value='';render(s);});});
  contact.addEventListener('submit',e=>{e.preventDefault();run(async()=>render(await request('confirmar',{nombre:document.getElementById('chatNombre').value,telefono:document.getElementById('chatTelefono').value,consentimiento:document.getElementById('chatConsentimiento').checked})) );});
  document.getElementById('chatNueva').addEventListener('click',()=>run(async()=>{render(await request('nueva',{}));contact.reset();input.value='';}));
  document.querySelectorAll('[data-chat-example]').forEach(b=>b.addEventListener('click',()=>{input.value=b.dataset.chatExample;input.focus();}));
  run(async()=>render(await request('sesion')));
})();
