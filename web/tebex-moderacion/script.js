const sampleOrders = [
  { id: 1, player: "HylandSteve", pack: "Rango Titan", amount: 19.99, payment: "paypal", risk: "medium", status: "pending" },
  { id: 2, player: "LunaPvP", pack: "Kit Legendario", amount: 7.5, payment: "card", risk: "low", status: "approved" },
  { id: 3, player: "MinerPro99", pack: "Llaves x20", amount: 12.0, payment: "crypto", risk: "high", status: "pending" },
  { id: 4, player: "AquaBuild", pack: "VIP+", amount: 4.99, payment: "paypal", risk: "low", status: "rejected" },
  { id: 5, player: "DarkRaid", pack: "Rango Elite", amount: 14.99, payment: "card", risk: "medium", status: "pending" }
];

const ordersTableBody = document.getElementById("ordersTableBody");
const activityLog = document.getElementById("activityLog");
const dialog = document.getElementById("moderationDialog");
const dialogTitle = document.getElementById("dialogTitle");
const dialogDetails = document.getElementById("dialogDetails");
const reasonInput = document.getElementById("reasonInput");
const moderationForm = document.getElementById("moderationForm");
const panicBtn = document.getElementById("panicBtn");

const metrics = {
  pendingOrders: document.getElementById("pendingOrders"),
  fraudCases: document.getElementById("fraudCases"),
  refundsToday: document.getElementById("refundsToday"),
  dailyRevenue: document.getElementById("dailyRevenue")
};

let currentAction = null;
let currentOrderId = null;

const filters = {
  status: document.getElementById("statusFilter"),
  payment: document.getElementById("paymentFilter"),
  search: document.getElementById("searchFilter")
};

function paymentLabel(type) {
  return { paypal: "PayPal", card: "Tarjeta", crypto: "Crypto" }[type] || type;
}

function formatUSD(value) {
  return `$${value.toFixed(2)}`;
}

function getVisibleOrders() {
  const statusValue = filters.status.value;
  const paymentValue = filters.payment.value;
  const searchValue = filters.search.value.trim().toLowerCase();

  return sampleOrders.filter((order) => {
    const statusMatch = statusValue === "all" || order.status === statusValue;
    const paymentMatch = paymentValue === "all" || order.payment === paymentValue;
    const searchMatch = !searchValue || order.player.toLowerCase().includes(searchValue);
    return statusMatch && paymentMatch && searchMatch;
  });
}

function renderOrders() {
  const visibleOrders = getVisibleOrders();

  if (visibleOrders.length === 0) {
    ordersTableBody.innerHTML = `
      <tr>
        <td colspan="7">No hay pedidos con los filtros actuales.</td>
      </tr>
    `;
    return;
  }

  ordersTableBody.innerHTML = visibleOrders
    .map(
      (order) => `
      <tr>
        <td>${order.player}</td>
        <td>${order.pack}</td>
        <td>${formatUSD(order.amount)}</td>
        <td>${paymentLabel(order.payment)}</td>
        <td><span class="badge badge-risk-${order.risk}">${order.risk.toUpperCase()}</span></td>
        <td class="status-${order.status}">${order.status.toUpperCase()}</td>
        <td class="row">
          <button class="btn" data-action="approve" data-id="${order.id}">Aprobar</button>
          <button class="btn btn-danger" data-action="reject" data-id="${order.id}">Rechazar</button>
        </td>
      </tr>
    `
    )
    .join("");
}

function updateMetrics() {
  const pending = sampleOrders.filter((entry) => entry.status === "pending").length;
  const fraud = sampleOrders.filter((entry) => entry.risk === "high").length;
  const refunds = sampleOrders.filter((entry) => entry.status === "rejected").length;
  const revenue = sampleOrders.filter((entry) => entry.status !== "rejected").reduce((acc, entry) => acc + entry.amount, 0);

  metrics.pendingOrders.textContent = String(pending);
  metrics.fraudCases.textContent = String(fraud);
  metrics.refundsToday.textContent = String(refunds);
  metrics.dailyRevenue.textContent = formatUSD(revenue);
}

function addActivity(message) {
  const item = document.createElement("li");
  const now = new Date().toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" });
  item.textContent = `[${now}] ${message}`;
  activityLog.prepend(item);
  if (activityLog.children.length > 8) {
    activityLog.lastChild.remove();
  }
}

function moderateOrder(id, action, reason) {
  const order = sampleOrders.find((entry) => entry.id === id);
  if (!order) return;

  order.status = action === "approve" ? "approved" : "rejected";
  updateMetrics();
  renderOrders();

  const resultText = action === "approve" ? "Aprobado" : "Rechazado";
  addActivity(`${resultText} pedido #${id} (${order.player}): ${reason}`);
}

document.addEventListener("click", (event) => {
  const button = event.target.closest("button[data-action]");
  if (!button) return;

  currentAction = button.dataset.action;
  currentOrderId = Number(button.dataset.id);
  const order = sampleOrders.find((entry) => entry.id === currentOrderId);
  if (!order) return;

  dialogTitle.textContent = `${currentAction === "approve" ? "Aprobar" : "Rechazar"} pedido`;
  dialogDetails.textContent = `Jugador ${order.player} • ${order.pack} • ${formatUSD(order.amount)}`;
  reasonInput.value = "";
  dialog.showModal();
});

dialog.addEventListener("close", () => {
  if (dialog.returnValue !== "confirm" || !currentAction || !currentOrderId) return;

  const reason = reasonInput.value.trim();
  if (!reason) return;

  moderateOrder(currentOrderId, currentAction, reason);
  currentAction = null;
  currentOrderId = null;
});

moderationForm.addEventListener("submit", (event) => {
  event.preventDefault();
  if (!reasonInput.value.trim()) {
    reasonInput.focus();
    return;
  }
  dialog.close("confirm");
});

Object.values(filters).forEach((element) => {
  element.addEventListener("input", renderOrders);
  element.addEventListener("change", renderOrders);
});

document.getElementById("refreshBtn").addEventListener("click", () => {
  addActivity("Cola de pedidos actualizada manualmente.");
  updateMetrics();
  renderOrders();
});

panicBtn.addEventListener("click", () => {
  addActivity("⚠️ Ventas pausadas de forma temporal por moderación.");
  panicBtn.textContent = "Ventas pausadas";
  panicBtn.disabled = true;
});

addActivity("Panel inicializado y conectado al webhook de Tebex.");
updateMetrics();
renderOrders();
