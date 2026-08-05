const currency = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

const number = new Intl.NumberFormat("en-US");

let currentState = null;
let lastDecision = null;

async function request(path, options = {}) {
    const response = await fetch(path, {
        headers: {"Content-Type": "application/json", ...(options.headers || {})},
        ...options
    });
    const body = await response.json();
    if (!response.ok) {
        throw new Error(body.message || "Request failed");
    }
    return body;
}

function tag(element, text, kind = "neutral") {
    element.textContent = text;
    element.className = `tag ${kind}`;
}

function status(element, text, kind = "neutral") {
    element.textContent = text;
    element.className = `status-pill ${kind}`;
}

function money(value) {
    return currency.format(Number(value || 0));
}

function micros(value) {
    return `${number.format(Number(value || 0))} us`;
}

function row(cells) {
    return `<tr>${cells.map(cell => `<td>${cell}</td>`).join("")}</tr>`;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function setScenarioOutput(value, label = "Updated") {
    document.querySelector("#scenarioOutput").textContent = JSON.stringify(value, null, 2);
    tag(document.querySelector("#scenarioTag"), label, "pass");
}

function decisionKind(decision) {
    if (!decision) {
        return "neutral";
    }
    return decision.status === "ACCEPTED" ? "pass" : "fail";
}

function renderDecision(decision) {
    lastDecision = decision;
    const kind = decisionKind(decision);
    tag(document.querySelector("#decisionTag"), decision ? `${decision.status}: ${decision.symbol}` : "No order", kind);
    if (decision) {
        status(document.querySelector("#latencyStatus"), micros(decision.totalLatencyMicros), decision.totalLatencyMicros < 1000 ? "pass" : "warn");
    }

    const checks = decision?.checks || [];
    document.querySelector("#checksBody").innerHTML = checks.length
        ? checks.map(check => row([
            escapeHtml(check.name),
            `<span class="tag ${check.passed ? "pass" : "fail"}">${check.passed ? "PASS" : "FAIL"}</span>`,
            `<span class="num">${micros(check.latencyMicros)}</span>`,
            escapeHtml(check.detail)
        ])).join("")
        : row(["No checks yet", "", "", ""]);

    document.querySelectorAll(".flow-step").forEach(step => step.classList.remove("active", "failed"));
    if (!decision) {
        return;
    }
    document.querySelector('[data-step="FIX Parser"]').classList.add("active");
    document.querySelector('[data-step="Risk Pipeline"]').classList.add(kind === "fail" ? "failed" : "active");
    document.querySelector('[data-step="Atomic Reserve"]').classList.add(kind === "fail" ? "failed" : "active");
    document.querySelector('[data-step="Audit and PnL"]').classList.add("active");
}

function renderState(state) {
    currentState = state;
    const activeKills = Object.entries(state.killSwitches || {}).filter(([, enabled]) => enabled);
    status(document.querySelector("#circuitStatus"), state.circuitBreakerOpen ? "Circuit open" : "Circuit closed", state.circuitBreakerOpen ? "fail" : "pass");
    status(document.querySelector("#killStatus"), activeKills.length ? `${activeKills.length} kill switch` : "No kill switch", activeKills.length ? "warn" : "pass");

    const positions = state.positions || [];
    tag(document.querySelector("#positionCount"), String(positions.length), positions.length ? "pass" : "neutral");
    document.querySelector("#positionsBody").innerHTML = positions.length
        ? positions.map(position => row([
            escapeHtml(position.account),
            escapeHtml(position.symbol),
            `<span class="num">${number.format(position.netQuantity)}</span>`,
            `<span class="num">${number.format(position.openBuyQuantity)} / ${number.format(position.openSellQuantity)}</span>`,
            `<span class="num">${money(position.reservedNotional)}</span>`,
            `<span class="num">${money(position.averageCost)}</span>`,
            `<span class="num">${money(position.unrealizedPnl)}</span>`
        ])).join("")
        : row(["No positions", "", "", "", "", "", ""]);

    renderPnl(state);
    renderAudit(state.auditTrail || []);
}

function renderPnl(state) {
    const accounts = Object.values(state.accounts || {});
    const total = accounts.reduce((sum, account) => sum + Number(account.realizedPnl || 0) + Number(account.unrealizedPnl || 0), 0);
    tag(document.querySelector("#pnlTag"), money(total), total >= 0 ? "pass" : "fail");

    const maxAbs = Math.max(1, ...accounts.map(account => Math.abs(Number(account.realizedPnl || 0) + Number(account.unrealizedPnl || 0))));
    document.querySelector("#pnlBars").innerHTML = accounts.length
        ? accounts.map(account => {
            const value = Number(account.realizedPnl || 0) + Number(account.unrealizedPnl || 0);
            const width = Math.max(4, Math.round(Math.abs(value) / maxAbs * 100));
            return `
                <div class="bar-row">
                    <strong>${escapeHtml(account.account)}</strong>
                    <div class="bar-track"><div class="bar-fill ${value < 0 ? "negative" : ""}" style="width:${width}%"></div></div>
                    <span class="num">${money(value)}</span>
                </div>
            `;
        }).join("")
        : "<div>No accounts</div>";
}

function renderAudit(events) {
    tag(document.querySelector("#auditCount"), String(events.length), events.length ? "pass" : "neutral");
    document.querySelector("#auditList").innerHTML = events.length
        ? events.slice(0, 30).map(event => {
            const lower = event.type.toLowerCase();
            const klass = lower.includes("rejected") ? "rejected" : lower.includes("accepted") || lower.includes("fill") ? "accepted" : lower.includes("switch") || lower.includes("breaker") ? "audit-switch" : "";
            return `
                <li class="${klass}">
                    <strong>#${event.sequence} ${escapeHtml(event.type)}</strong>
                    <span>${escapeHtml(event.aggregateKey)} | ${new Date(event.occurredAt).toLocaleTimeString()}</span>
                    <div>${escapeHtml(event.message)}</div>
                </li>
            `;
        }).join("")
        : "<li>No events</li>";
}

async function refresh() {
    const state = await request("/api/state");
    renderState(state);
    if (!lastDecision && state.recentDecisions?.length) {
        renderDecision(state.recentDecisions[0]);
    }
}

document.querySelector("#jsonOrderForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget).entries());
    const payload = {
        clOrdId: data.clOrdId,
        account: data.account,
        symbol: data.symbol,
        side: data.side,
        quantity: Number(data.quantity),
        price: Number(data.price),
        autoFill: event.currentTarget.elements.autoFill.checked
    };
    try {
        const decision = await request("/api/orders", {method: "POST", body: JSON.stringify(payload)});
        renderDecision(decision);
        tag(document.querySelector("#jsonResult"), decision.status, decisionKind(decision));
        await refresh();
        setScenarioOutput(decision, "JSON order");
    } catch (error) {
        tag(document.querySelector("#jsonResult"), error.message, "fail");
    }
});

document.querySelector("#fixOrderForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget).entries());
    try {
        const decision = await request("/api/fix/orders", {method: "POST", body: JSON.stringify({message: data.message})});
        renderDecision(decision);
        tag(document.querySelector("#fixResult"), decision.status, decisionKind(decision));
        await refresh();
        setScenarioOutput(decision, "FIX order");
    } catch (error) {
        tag(document.querySelector("#fixResult"), error.message, "fail");
    }
});

document.querySelector("#killForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget).entries());
    const state = await request("/api/kill-switch", {
        method: "POST",
        body: JSON.stringify({
            scope: data.scope,
            key: data.key,
            enabled: event.currentTarget.elements.enabled.checked,
            reason: "dashboard toggle"
        })
    });
    renderState(state);
    setScenarioOutput(state.killSwitches, "Kill switch");
});

document.querySelector("#marketForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget).entries());
    const state = await request("/api/market-data", {
        method: "POST",
        body: JSON.stringify({symbol: data.symbol, price: Number(data.price)})
    });
    renderState(state);
    tag(document.querySelector("#marketResult"), "Tick applied", "pass");
    setScenarioOutput(state.marketData, "Market data");
});

document.querySelector("#breakerForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget).entries());
    const state = await request("/api/circuit-breaker", {
        method: "POST",
        body: JSON.stringify({open: event.currentTarget.elements.open.checked, reason: data.reason})
    });
    renderState(state);
    setScenarioOutput({circuitBreakerOpen: state.circuitBreakerOpen}, "Circuit");
});

document.querySelectorAll("[data-scenario]").forEach(button => {
    button.addEventListener("click", async () => {
        const result = await request(`/api/scenarios/${button.dataset.scenario}`, {method: "POST"});
        const decision = result.decisions?.[0] || null;
        renderDecision(decision);
        renderState(result.state);
        setScenarioOutput(result, result.name);
    });
});

document.querySelector("#resetButton").addEventListener("click", async () => {
    const state = await request("/api/reset", {method: "POST"});
    lastDecision = null;
    renderDecision(null);
    renderState(state);
    setScenarioOutput(state, "Reset");
});

refresh().catch(error => {
    setScenarioOutput({error: error.message}, "Error");
});
