document.addEventListener("DOMContentLoaded", async () => {
  const message = document.getElementById("loginMessage");
  const params = new URLSearchParams(window.location.search);
  if (params.has("error")) message.textContent = "Usuario o contraseña incorrectos.";
  if (params.has("logout")) message.textContent = "Sesión cerrada.";
  try {
    const response = await fetch("/auth/csrf", { cache: "no-store", credentials: "same-origin" });
    if (!response.ok) throw new Error();
    const csrf = await response.json();
    const field = document.getElementById("csrfToken");
    field.name = csrf.parameterName;
    field.value = csrf.token;
    document.getElementById("loginButton").disabled = false;
  } catch (_) {
    message.textContent = "No se pudo preparar el acceso seguro. Recarga la página.";
  }
});
