const API = {
  clientes: "/api/clientes",
  productos: "/api/productos",
  pedidos: "/api/pedidos",
  ia: "/api/ia/interpretar",
  instagramStatus: "/api/instagram/status",
  instagramCuenta: "/api/instagram/cuenta",
  instagramContactos: "/api/instagram/contactos",
  instagramConversaciones: "/api/instagram/conversaciones"
};

let clientes = [];
let productos = [];
let pedidos = [];
let ultimaInterpretacion = null;

document.addEventListener("DOMContentLoaded", async () => {
  configurarNavegacion();
  configurarModales();
  configurarFormularios();
  configurarSimuladorIa();
  configurarInstagram();

  document.getElementById("currentDate").textContent =
    new Intl.DateTimeFormat("es-PE", {
      dateStyle: "full"
    }).format(new Date());

  await cargarTodo();
  await cargarInstagram();
});

async function cargarTodo() {
  try {
    [clientes, productos, pedidos] = await Promise.all([
      apiFetch(API.clientes),
      apiFetch(API.productos),
      apiFetch(API.pedidos)
    ]);

    renderClientes();
    renderProductos();
    renderPedidos();
    renderDashboard();
    cargarClientesIa();
  } catch (error) {
    mostrarToast(error.message, true);
  }
}

function configurarNavegacion() {
  document.querySelectorAll(".nav-item").forEach(btn => {
    btn.addEventListener("click", () => mostrarSeccion(btn.dataset.section));
  });

  document.querySelectorAll("[data-go]").forEach(btn => {
    btn.addEventListener("click", () => mostrarSeccion(btn.dataset.go));
  });
}

function mostrarSeccion(id) {
  document.querySelectorAll(".page-section").forEach(section => {
    section.classList.toggle("active", section.id === id);
  });

  document.querySelectorAll(".nav-item").forEach(btn => {
    btn.classList.toggle("active", btn.dataset.section === id);
  });

  const titulos = {
    dashboard: "Dashboard",
    clientes: "Clientes",
    productos: "Productos",
    pedidos: "Pedidos",
    simulador: "Simulador IA"
  };

  document.getElementById("pageTitle").textContent = titulos[id] || "Pao Collection";
}

function configurarModales() {
  document.getElementById("btnNuevoCliente").addEventListener("click", () => abrirCliente());
  document.getElementById("btnNuevoProducto").addEventListener("click", () => abrirProducto());
  document.getElementById("btnNuevoPedido").addEventListener("click", abrirPedido);

  document.querySelectorAll("[data-close]").forEach(btn => {
    btn.addEventListener("click", () => cerrarModal(btn.dataset.close));
  });

  document.querySelectorAll(".modal-backdrop").forEach(backdrop => {
    backdrop.addEventListener("click", event => {
      if (event.target === backdrop) {
        backdrop.classList.add("hidden");
      }
    });
  });
}

function configurarFormularios() {
  document.getElementById("clienteForm").addEventListener("submit", guardarCliente);
  document.getElementById("productoForm").addEventListener("submit", guardarProducto);
  document.getElementById("pedidoForm").addEventListener("submit", guardarPedido);
  document.getElementById("btnAgregarItem").addEventListener("click", () => agregarFilaPedido());
}

function abrirModal(id) {
  document.getElementById(id).classList.remove("hidden");
}

function cerrarModal(id) {
  document.getElementById(id).classList.add("hidden");
}

function abrirCliente(cliente = null) {
  document.getElementById("clienteModalTitle").textContent =
    cliente ? "Editar cliente" : "Nuevo cliente";

  document.getElementById("clienteId").value = cliente?.id ?? "";
  document.getElementById("clienteNombres").value = cliente?.nombres ?? "";
  document.getElementById("clienteDocumento").value = cliente?.documento ?? "";
  document.getElementById("clienteTelefono").value = cliente?.telefono ?? "";
  document.getElementById("clienteEmail").value = cliente?.email ?? "";

  abrirModal("clienteModal");
}

async function guardarCliente(event) {
  event.preventDefault();

  const id = document.getElementById("clienteId").value;

  const body = {
    nombres: document.getElementById("clienteNombres").value.trim(),
    documento: document.getElementById("clienteDocumento").value.trim(),
    telefono: document.getElementById("clienteTelefono").value.trim(),
    email: document.getElementById("clienteEmail").value.trim()
  };

  try {
    if (id) {
      await apiFetch(`${API.clientes}/${id}`, {
        method: "PUT",
        body: JSON.stringify(body)
      });
      mostrarToast("Cliente actualizado correctamente.");
    } else {
      await apiFetch(API.clientes, {
        method: "POST",
        body: JSON.stringify(body)
      });
      mostrarToast("Cliente registrado correctamente.");
    }

    cerrarModal("clienteModal");
    clientes = await apiFetch(API.clientes);
    renderClientes();
    renderDashboard();
    cargarClientesIa();
  } catch (error) {
    mostrarToast(error.message, true);
  }
}

function abrirProducto(producto = null) {
  document.getElementById("productoModalTitle").textContent =
    producto ? "Editar producto" : "Nuevo producto";

  document.getElementById("productoId").value = producto?.id ?? "";
  document.getElementById("productoNombre").value = producto?.nombre ?? "";
  document.getElementById("productoDescripcion").value = producto?.descripcion ?? "";
  document.getElementById("productoCategoria").value = producto?.categoria ?? "";
  document.getElementById("productoMaterial").value = producto?.material ?? "";
  document.getElementById("productoColor").value = producto?.color ?? "";
  document.getElementById("productoImagenUrl").value = producto?.imagenUrl ?? "";
  document.getElementById("productoPrecio").value = producto?.precio ?? "";
  document.getElementById("productoStock").value = producto?.stock ?? "";

  abrirModal("productoModal");
}

async function guardarProducto(event) {
  event.preventDefault();

  const id = document.getElementById("productoId").value;

  const body = {
    nombre: document.getElementById("productoNombre").value.trim(),
    descripcion: document.getElementById("productoDescripcion").value.trim(),
    categoria: document.getElementById("productoCategoria").value,
    material: document.getElementById("productoMaterial").value.trim(),
    color: document.getElementById("productoColor").value.trim(),
    imagenUrl: document.getElementById("productoImagenUrl").value.trim(),
    precio: Number(document.getElementById("productoPrecio").value),
    stock: Number(document.getElementById("productoStock").value)
  };

  try {
    if (id) {
      await apiFetch(`${API.productos}/${id}`, {
        method: "PUT",
        body: JSON.stringify(body)
      });
      mostrarToast("Producto actualizado correctamente.");
    } else {
      await apiFetch(API.productos, {
        method: "POST",
        body: JSON.stringify(body)
      });
      mostrarToast("Producto registrado correctamente.");
    }

    cerrarModal("productoModal");
    productos = await apiFetch(API.productos);
    renderProductos();
    renderDashboard();
    cargarClientesIa();
  } catch (error) {
    mostrarToast(error.message, true);
  }
}

function abrirPedido() {
  if (!clientes.length) {
    mostrarToast("Registra al menos un cliente antes de crear pedidos.", true);
    return;
  }

  if (!productos.length) {
    mostrarToast("Registra al menos un producto antes de crear pedidos.", true);
    return;
  }

  const selectCliente = document.getElementById("pedidoCliente");
  selectCliente.innerHTML =
    `<option value="">Seleccione un cliente</option>` +
    clientes
      .filter(c => c.activo !== false)
      .map(c => `<option value="${c.id}">${escapeHtml(c.nombres)} · ${escapeHtml(c.documento)}</option>`)
      .join("");

  document.getElementById("pedidoItems").innerHTML = "";
  agregarFilaPedido();
  recalcularPedido();
  abrirModal("pedidoModal");
}

function agregarFilaPedido() {
  const container = document.getElementById("pedidoItems");
  const row = document.createElement("div");
  row.className = "item-row";

  const opciones = productos
    .filter(p => p.activo !== false && Number(p.stock) > 0)
    .map(p =>
      `<option value="${p.id}" data-precio="${p.precio}">
        ${escapeHtml(p.nombre)} · Stock ${p.stock} · S/ ${Number(p.precio).toFixed(2)}
      </option>`
    )
    .join("");

  row.innerHTML = `
    <select class="item-producto" required>
      <option value="">Seleccione producto</option>
      ${opciones}
    </select>
    <input class="item-cantidad" type="number" min="1" value="1" required>
    <span class="item-subtotal">S/ 0.00</span>
    <button type="button" class="remove-item" title="Quitar">×</button>
  `;

  row.querySelector(".item-producto").addEventListener("change", recalcularPedido);
  row.querySelector(".item-cantidad").addEventListener("input", recalcularPedido);
  row.querySelector(".remove-item").addEventListener("click", () => {
    row.remove();
    recalcularPedido();
  });

  container.appendChild(row);
}

function recalcularPedido() {
  let total = 0;

  document.querySelectorAll("#pedidoItems .item-row").forEach(row => {
    const productoId = Number(row.querySelector(".item-producto").value);
    const cantidad = Number(row.querySelector(".item-cantidad").value || 0);
    const producto = productos.find(p => Number(p.id) === productoId);
    const subtotal = producto ? Number(producto.precio) * cantidad : 0;

    row.querySelector(".item-subtotal").textContent = moneda(subtotal);
    total += subtotal;
  });

  document.getElementById("pedidoTotalEstimado").textContent = moneda(total);
}

async function guardarPedido(event) {
  event.preventDefault();

  const clienteId = Number(document.getElementById("pedidoCliente").value);

  const items = [...document.querySelectorAll("#pedidoItems .item-row")]
    .map(row => ({
      productoId: Number(row.querySelector(".item-producto").value),
      cantidad: Number(row.querySelector(".item-cantidad").value)
    }))
    .filter(item => item.productoId && item.cantidad > 0);

  if (!clienteId) {
    mostrarToast("Selecciona un cliente.", true);
    return;
  }

  if (!items.length) {
    mostrarToast("Agrega al menos un producto.", true);
    return;
  }

  try {
    await apiFetch(API.pedidos, {
      method: "POST",
      body: JSON.stringify({ clienteId, items })
    });

    mostrarToast("Pedido registrado correctamente.");
    cerrarModal("pedidoModal");

    [productos, pedidos] = await Promise.all([
      apiFetch(API.productos),
      apiFetch(API.pedidos)
    ]);

    renderProductos();
    renderPedidos();
    renderDashboard();
    cargarClientesIa();
  } catch (error) {
    mostrarToast(error.message, true);
  }
}

function renderDashboard() {
  const totalVentas = pedidos
    .filter(p => p.estado === "COMPLETADO")
    .reduce((sum, p) => sum + Number(p.total || 0), 0);
  const stockBajo = productos.filter(p => Number(p.stock) <= 5).length;

  document.getElementById("statPedidos").textContent = pedidos.length;
  document.getElementById("statVentas").textContent = moneda(totalVentas);
  document.getElementById("statClientes").textContent = clientes.length;
  document.getElementById("statStockBajo").textContent = stockBajo;

  const body = document.getElementById("dashboardPedidosBody");
  const ultimos = pedidos.slice(0, 5);

  body.innerHTML = ultimos.length
    ? ultimos.map(p => `
      <tr>
        <td>#${p.id}</td>
        <td>${escapeHtml(p.cliente?.nombres ?? "-")}</td>
        <td>${formatearFecha(p.fecha)}</td>
        <td>${badgeEstado(p.estado)}</td>
        <td><strong>${moneda(p.total)}</strong></td>
      </tr>
    `).join("")
    : filaVacia(5, "Aún no hay pedidos registrados.");
}

function renderClientes() {
  const body = document.getElementById("clientesBody");

  body.innerHTML = clientes.length
    ? clientes.map(c => `
      <tr>
        <td>#${c.id}</td>
        <td><strong>${escapeHtml(c.nombres)}</strong></td>
        <td>${escapeHtml(c.documento)}</td>
        <td>${escapeHtml(c.telefono || "-")}</td>
        <td>${escapeHtml(c.email || "-")}</td>
        <td>${c.activo !== false
          ? `<span class="badge badge-success">Activo</span>`
          : `<span class="badge badge-neutral">Inactivo</span>`}
        </td>
        <td>
          <div class="actions">
            <button class="btn btn-secondary btn-small" onclick="editarCliente(${c.id})">Editar</button>
            <button class="btn btn-danger btn-small" onclick="eliminarCliente(${c.id})">Eliminar</button>
          </div>
        </td>
      </tr>
    `).join("")
    : filaVacia(7, "No hay clientes registrados.");
}

function renderProductos() {
  const body = document.getElementById("productosBody");

  body.innerHTML = productos.length
    ? productos.map(p => `
      <tr>
        <td>#${p.id}</td>
        <td>
          <div class="product-name-cell">
            ${p.imagenUrl
              ? `<img src="${escapeHtml(p.imagenUrl)}" alt="${escapeHtml(p.nombre)}" class="product-thumb"
                    onerror="this.style.display='none'">`
              : `<div class="product-thumb product-thumb-placeholder">◇</div>`}
            <div>
              <strong>${escapeHtml(p.nombre)}</strong><br>
              <small>${escapeHtml(p.descripcion || "")}</small>
            </div>
          </div>
        </td>
        <td>
          <span class="badge badge-neutral">${escapeHtml(p.categoria || "Sin categoría")}</span>
        </td>
        <td>
          ${escapeHtml(p.material || "-")}
          ${p.color ? `<br><small>${escapeHtml(p.color)}</small>` : ""}
        </td>
        <td>${moneda(p.precio)}</td>
        <td>
          <span class="badge ${Number(p.stock) <= 5 ? "badge-warning" : "badge-success"}">
            ${p.stock}
          </span>
        </td>
        <td>${p.activo !== false
          ? `<span class="badge badge-success">Activo</span>`
          : `<span class="badge badge-neutral">Inactivo</span>`}
        </td>
        <td>
          <div class="actions">
            <button class="btn btn-secondary btn-small" onclick="editarProducto(${p.id})">Editar</button>
            <button class="btn btn-danger btn-small" onclick="eliminarProducto(${p.id})">Eliminar</button>
          </div>
        </td>
      </tr>
    `).join("")
    : filaVacia(8, "No hay joyas registradas.");
}

function renderPedidos() {
  const body = document.getElementById("pedidosBody");

  body.innerHTML = pedidos.length
    ? pedidos.map(p => {
      const detalle = (p.detalles || [])
        .map(d => `${escapeHtml(d.producto?.nombre ?? "Producto")} x${d.cantidad}`)
        .join("<br>");

      return `
        <tr>
          <td>#${p.id}</td>
          <td>${escapeHtml(p.cliente?.nombres ?? "-")}</td>
          <td>${formatearFecha(p.fecha)}</td>
          <td>${badgeEstado(p.estado)}</td>
          <td>${detalle || "-"}</td>
          <td><strong>${moneda(p.total)}</strong></td>
          <td>
            <select class="estado-select" onchange="cambiarEstadoPedido(${p.id}, this.value)">
              ${["PENDIENTE", "CONFIRMADO", "EN_PROCESO", "COMPLETADO", "CANCELADO"]
                .map(e => `<option value="${e}" ${e === p.estado ? "selected" : ""}>${e.replace("_", " ")}</option>`)
                .join("")}
            </select>
          </td>
        </tr>
      `;
    }).join("")
    : filaVacia(7, "No hay pedidos registrados.");
}

window.editarCliente = id => {
  const cliente = clientes.find(c => Number(c.id) === Number(id));
  if (cliente) abrirCliente(cliente);
};

window.editarProducto = id => {
  const producto = productos.find(p => Number(p.id) === Number(id));
  if (producto) abrirProducto(producto);
};

window.eliminarCliente = async id => {
  if (!confirm("¿Deseas eliminar este cliente?")) return;

  try {
    await apiFetch(`${API.clientes}/${id}`, { method: "DELETE" });
    clientes = await apiFetch(API.clientes);
    renderClientes();
    renderDashboard();
    mostrarToast("Cliente eliminado.");
  } catch (error) {
    mostrarToast(error.message, true);
  }
};

window.eliminarProducto = async id => {
  if (!confirm("¿Deseas eliminar este producto?")) return;

  try {
    await apiFetch(`${API.productos}/${id}`, { method: "DELETE" });
    productos = await apiFetch(API.productos);
    renderProductos();
    renderDashboard();
    mostrarToast("Producto eliminado.");
  } catch (error) {
    mostrarToast(error.message, true);
  }
};

window.cambiarEstadoPedido = async (id, estado) => {
  try {
    await apiFetch(`${API.pedidos}/${id}/estado?estado=${encodeURIComponent(estado)}`, {
      method: "PATCH"
    });

    pedidos = await apiFetch(API.pedidos);
    renderPedidos();
    renderDashboard();
    mostrarToast(`Pedido #${id} actualizado a ${estado.replace("_", " ")}.`);
  } catch (error) {
    mostrarToast(error.message, true);
    pedidos = await apiFetch(API.pedidos);
    renderPedidos();
  }
};


function configurarSimuladorIa() {
  document.getElementById("btnAnalizarIa").addEventListener("click", analizarMensajeIa);
  document.getElementById("btnLimpiarIa").addEventListener("click", limpiarSimuladorIa);
  document.getElementById("btnConfirmarIa").addEventListener("click", confirmarPedidoIa);

  document.querySelectorAll("[data-example]").forEach(btn => {
    btn.addEventListener("click", () => {
      document.getElementById("iaMensaje").value = btn.dataset.example;
    });
  });
}

function cargarClientesIa() {
  const select = document.getElementById("iaCliente");

  if (!select) return;

  select.innerHTML =
    `<option value="">Seleccione un cliente</option>` +
    clientes
      .filter(c => c.activo !== false)
      .map(c => `<option value="${c.id}">${escapeHtml(c.nombres)} · ${escapeHtml(c.documento)}</option>`)
      .join("");
}

async function analizarMensajeIa() {
  const clienteId = Number(document.getElementById("iaCliente").value);
  const mensaje = document.getElementById("iaMensaje").value.trim();

  if (!clienteId) {
    mostrarToast("Selecciona un cliente para analizar el mensaje.", true);
    return;
  }

  if (!mensaje) {
    mostrarToast("Escribe un mensaje antes de analizar.", true);
    return;
  }

  const boton = document.getElementById("btnAnalizarIa");
  const textoOriginal = boton.textContent;
  boton.disabled = true;
  boton.textContent = "Analizando...";

  try {
    ultimaInterpretacion = await apiFetch(API.ia, {
      method: "POST",
      body: JSON.stringify({ clienteId, mensaje })
    });

    renderInterpretacionIa(ultimaInterpretacion);
  } catch (error) {
    mostrarToast(error.message, true);
  } finally {
    boton.disabled = false;
    boton.textContent = textoOriginal;
  }
}

function renderInterpretacionIa(resultado) {
  document.getElementById("iaEmpty").classList.add("hidden");
  document.getElementById("iaResultado").classList.remove("hidden");

  document.getElementById("iaIntencion").textContent =
    String(resultado.intencion || "-").replaceAll("_", " ");

  document.getElementById("iaTotal").textContent =
    moneda(resultado.totalEstimado);

  document.getElementById("iaRevision").innerHTML =
    resultado.requiereRevision
      ? `<span class="badge badge-warning">Sí</span>`
      : `<span class="badge badge-success">No</span>`;

  const itemsContainer = document.getElementById("iaItems");

  itemsContainer.innerHTML = (resultado.items || []).length
    ? resultado.items.map(item => `
      <div class="ai-item">
        <div>
          <strong>${escapeHtml(item.nombreProducto)}</strong>
          <small>
            ${escapeHtml(item.categoria || "Joya")}
            ${item.material ? ` · ${escapeHtml(item.material)}` : ""}
            ${item.color ? ` · ${escapeHtml(item.color)}` : ""}
          </small>
          <small>
            Cantidad: ${item.cantidad} · Stock: ${item.stockDisponible}
          </small>
        </div>
        <div class="ai-item-right">
          <span>${moneda(item.subtotal)}</span>
          <small>${Math.round(Number(item.confianza || 0) * 100)}% coincidencia</small>
        </div>
      </div>
    `).join("")
    : `<div class="empty-state">No se identificaron productos.</div>`;

  const observaciones = resultado.observaciones || [];

  document.getElementById("iaObservaciones").innerHTML = observaciones.length
    ? observaciones.map(obs => `<li>${escapeHtml(obs)}</li>`).join("")
    : `<li>Sin observaciones.</li>`;

  document.getElementById("iaRespuesta").textContent =
    resultado.respuestaSugerida || "-";

  document.getElementById("btnConfirmarIa").disabled =
    resultado.requiereRevision || !(resultado.items || []).length;
}

async function confirmarPedidoIa() {
  if (!ultimaInterpretacion) return;

  const body = {
    clienteId: ultimaInterpretacion.clienteId,
    items: (ultimaInterpretacion.items || []).map(item => ({
      productoId: item.productoId,
      cantidad: item.cantidad
    }))
  };

  try {
    await apiFetch(API.pedidos, {
      method: "POST",
      body: JSON.stringify(body)
    });

    mostrarToast("Pedido creado desde el simulador.");

    [productos, pedidos] = await Promise.all([
      apiFetch(API.productos),
      apiFetch(API.pedidos)
    ]);

    renderProductos();
    renderPedidos();
    renderDashboard();
    limpiarSimuladorIa();
  } catch (error) {
    mostrarToast(error.message, true);
  }
}

function limpiarSimuladorIa() {
  ultimaInterpretacion = null;
  document.getElementById("iaMensaje").value = "";
  document.getElementById("iaEmpty").classList.remove("hidden");
  document.getElementById("iaResultado").classList.add("hidden");
  document.getElementById("btnConfirmarIa").disabled = true;
}



let consultaCuentaInstagram = 0;

async function cargarCuentaInstagram() {
  const tarjeta = document.getElementById("igCuenta");
  if (!tarjeta) return;
  const consulta = ++consultaCuentaInstagram;
  const estado = document.getElementById("igCuentaEstado");
  const detalle = document.getElementById("igCuentaDetalle");
  const foto = document.getElementById("igCuentaFoto");
  const avatar = document.getElementById("igCuentaAvatar");
  const mensaje = document.getElementById("igCuentaMensaje");
  tarjeta.setAttribute("aria-busy", "true");
  estado.textContent = "Verificando…";
  estado.className = "badge badge-neutral";
  detalle.hidden = true;
  foto.hidden = true;
  foto.removeAttribute("src");
  avatar.hidden = false;
  mensaje.textContent = "Consultando la cuenta profesional en Instagram…";
  try {
    const respuesta = await fetch(API.instagramCuenta, {
      cache: "no-store", signal: AbortSignal.timeout(15000)
    });
    if (consulta !== consultaCuentaInstagram) return;
    // No mostrar cuerpos de error externos ni mensajes de proxies.
    if (!respuesta.ok) throw new Error("perfil_no_disponible");
    const cuenta = await respuesta.json();
    if (consulta !== consultaCuentaInstagram) return;
    if (!cuenta || typeof cuenta.id !== "string" || !cuenta.id.trim()
        || typeof cuenta.username !== "string" || !cuenta.username.trim()) {
      throw new Error("perfil_incompleto");
    }
    document.getElementById("igCuentaUsername").textContent = `@${cuenta.username}`;
    document.getElementById("igCuentaNombre").textContent = cuenta.name || "Nombre no disponible";
    document.getElementById("igCuentaId").textContent = cuenta.id;
    if (cuenta.profile_picture_url) {
      try {
        const url = new URL(cuenta.profile_picture_url);
        if (url.protocol === "https:" && !url.username && !url.password) {
          foto.onload = () => {
            if (consulta !== consultaCuentaInstagram) return;
            foto.hidden = false;
            avatar.hidden = true;
          };
          foto.onerror = () => {
            if (consulta !== consultaCuentaInstagram) return;
            foto.hidden = true;
            avatar.hidden = false;
          };
          foto.src = url.href;
        }
      } catch (_) { /* Mantener el avatar si Meta no entrega una URL válida. */ }
    }
    detalle.hidden = false;
    estado.textContent = "Conectada";
    estado.className = "badge ig-connected";
    mensaje.textContent = "Información obtenida de Meta. Usa Actualizar para volver a verificar.";
  } catch (_) {
    if (consulta !== consultaCuentaInstagram) return;
    estado.textContent = "No verificada";
    estado.className = "badge badge-warning";
    mensaje.textContent = "No se pudo verificar la cuenta. Revisa la conexión, el token, el ID y el permiso instagram_business_basic. Luego pulsa Actualizar.";
  } finally {
    if (consulta === consultaCuentaInstagram) tarjeta.setAttribute("aria-busy", "false");
  }
}

function configurarInstagram() {
  const btn = document.getElementById("btnActualizarInstagram");

  if (btn) {
    btn.addEventListener("click", cargarInstagram);
  }
}

async function cargarInstagram() {
  // La consulta del perfil es independiente de contactos y conversaciones.
  void cargarCuentaInstagram();
  const contactosBody = document.getElementById("instagramContactosBody");
  const conversacionesBody = document.getElementById("instagramConversacionesBody");

  if (!contactosBody || !conversacionesBody) return;

  try {
    const [status, contactos, conversaciones] = await Promise.all([
      apiFetch(API.instagramStatus),
      apiFetch(API.instagramContactos),
      apiFetch(API.instagramConversaciones)
    ]);

    document.getElementById("igConfigurado").textContent =
      status.configurado ? "Listo" : "Pendiente";

    document.getElementById("igConfiguradoDetalle").textContent =
      status.configurado
        ? "Variables locales configuradas"
        : "Faltan datos de Meta";

    document.getElementById("igGraphVersion").textContent =
      status.graphVersion || "-";

    document.getElementById("igContactosCount").textContent =
      contactos.length;

    document.getElementById("igConversacionesCount").textContent =
      conversaciones.length;

    const badge = document.getElementById("igBadge");

    if (status.configurado) {
      badge.textContent = "Backend listo";
      badge.className = "badge badge-success";
    } else {
      badge.textContent = "Configuración incompleta";
      badge.className = "badge badge-warning";
    }

    contactosBody.innerHTML = contactos.length
      ? contactos.map(c => {
          const options = clientes
            .map(cliente =>
              `<option value="${cliente.id}" ${Number(c.clienteId) === Number(cliente.id) ? "selected" : ""}>
                ${escapeHtml(cliente.nombres)} · ${escapeHtml(cliente.documento)}
              </option>`
            )
            .join("");

          return `
            <tr>
              <td><code>${escapeHtml(c.instagramScopedId)}</code></td>
              <td>${escapeHtml(c.nombreReferencia || "-")}</td>
              <td>
                <select class="ig-client-select" id="ig-cliente-${c.id}">
                  <option value="">Sin vincular</option>
                  ${options}
                </select>
              </td>
              <td>
                <div class="actions">
                  <button class="btn btn-primary btn-small"
                    onclick="vincularInstagram(${c.id})">
                    Guardar vínculo
                  </button>
                  ${c.clienteId ? `
                    <button class="btn btn-danger btn-small"
                      onclick="desvincularInstagram(${c.id})">
                      Quitar
                    </button>
                  ` : ""}
                </div>
              </td>
            </tr>
          `;
        }).join("")
      : filaVacia(4, "Aún no se han recibido contactos desde Instagram.");

    conversacionesBody.innerHTML = conversaciones.length
      ? conversaciones.map(c => `
        <tr>
          <td>${formatearFecha(c.fecha)}</td>
          <td>
            <strong>${escapeHtml(c.clienteNombre || "Sin vincular")}</strong>
            <br><small>${escapeHtml(c.instagramScopedId)}</small>
          </td>
          <td class="ig-message-cell">${escapeHtml(c.mensajeEntrada || "-")}</td>
          <td class="ig-message-cell">${escapeHtml(c.respuestaSistema || "-")}</td>
          <td>${badgeInstagramEstado(c.estado)}</td>
          <td>${c.pedidoId ? `#${c.pedidoId}` : "-"}</td>
        </tr>
      `).join("")
      : filaVacia(6, "Aún no hay mensajes recibidos desde Instagram.");

  } catch (error) {
    contactosBody.innerHTML = filaVacia(4, "No se pudo cargar Instagram.");
    conversacionesBody.innerHTML = filaVacia(6, "No se pudo cargar Instagram.");
  }
}

window.vincularInstagram = async contactoId => {
  const select = document.getElementById(`ig-cliente-${contactoId}`);
  const clienteId = Number(select.value);

  if (!clienteId) {
    mostrarToast("Selecciona un cliente para vincular.", true);
    return;
  }

  try {
    await apiFetch(
      `/api/instagram/contactos/${contactoId}/vincular?clienteId=${clienteId}`,
      { method: "PATCH" }
    );

    mostrarToast("Contacto de Instagram vinculado.");
    await cargarInstagram();

  } catch (error) {
    mostrarToast(error.message, true);
  }
};

window.desvincularInstagram = async contactoId => {
  try {
    await apiFetch(
      `/api/instagram/contactos/${contactoId}/vinculo`,
      { method: "DELETE" }
    );

    mostrarToast("Vínculo eliminado.");
    await cargarInstagram();

  } catch (error) {
    mostrarToast(error.message, true);
  }
};

function badgeInstagramEstado(estado) {
  const clases = {
    CONFIRMADO: "badge-success",
    PENDIENTE_CONFIRMACION: "badge-warning",
    REQUIERE_VINCULACION: "badge-warning",
    REQUIERE_REVISION: "badge-warning",
    CANCELADO: "badge-danger",
    ERROR: "badge-danger",
    RECIBIDO: "badge-neutral",
    IGNORADO: "badge-neutral"
  };

  const clase = clases[estado] || "badge-neutral";

  return `<span class="badge ${clase}">${String(estado || "-").replaceAll("_", " ")}</span>`;
}


async function apiFetch(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  if (!response.ok) {
    let mensaje = `Error HTTP ${response.status}`;

    try {
      const error = await response.json();
      mensaje = error.mensaje || error.message || mensaje;
    } catch (_) {}

    throw new Error(mensaje);
  }

  if (response.status === 204) return null;

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

function badgeEstado(estado) {
  const clase = {
    PENDIENTE: "badge-warning",
    CONFIRMADO: "badge-neutral",
    EN_PROCESO: "badge-neutral",
    COMPLETADO: "badge-success",
    CANCELADO: "badge-danger"
  }[estado] || "badge-neutral";

  return `<span class="badge ${clase}">${String(estado || "-").replace("_", " ")}</span>`;
}

function moneda(valor) {
  return new Intl.NumberFormat("es-PE", {
    style: "currency",
    currency: "PEN"
  }).format(Number(valor || 0));
}

function formatearFecha(fecha) {
  if (!fecha) return "-";

  return new Intl.DateTimeFormat("es-PE", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(fecha));
}

function filaVacia(columnas, texto) {
  return `<tr><td colspan="${columnas}" class="empty-state">${texto}</td></tr>`;
}

function mostrarToast(mensaje, error = false) {
  const toast = document.getElementById("toast");
  toast.textContent = mensaje;
  toast.classList.remove("hidden", "error");

  if (error) toast.classList.add("error");

  clearTimeout(window.toastTimer);
  window.toastTimer = setTimeout(() => {
    toast.classList.add("hidden");
  }, 3500);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
