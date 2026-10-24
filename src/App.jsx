
import { useEffect, useMemo, useState } from "react";
import { apiRequest } from "./api";
import "./App.css";

const navItems = [
  { id: "dashboard", icon: "▦", label: "Dashboard" },
  { id: "payments", icon: "＋", label: "Payments" },
  { id: "transactions", icon: "⇄", label: "Transactions" },
  { id: "refunds", icon: "↶", label: "Refunds" },
  { id: "settings", icon: "⚙", label: "Settings" },
];

function getRows(result) {
  if (Array.isArray(result)) return result;
  return result?.content || result?.payments || result?.data || [];
}

function getStatus(payment) {
  return String(payment?.status || "UNKNOWN").toUpperCase();
}

function getMethod(payment) {
  return payment?.paymentMethod || payment?.method || "—";
}

function getDate(payment) {
  const value =
    payment?.createdAt ||
    payment?.created_at ||
    payment?.createdDate ||
    payment?.timestamp;

  if (!value) return null;

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function money(value) {
  return Number(value || 0).toLocaleString("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  });
}

// Export the currently filtered payment records as CSV.
function exportPaymentsCsv(rows) {
  if (!rows.length) {
    alert("No transactions available to export.");
    return;
  }

  const headers = [
    "Payment ID",
    "Transaction ID",
    "Amount (INR)",
    "Payment Method",
    "Status",
    "Currency",
    "Creation Date",
  ];

  const escapeCsv = (value) => {
    const text = String(value ?? "");
    return `"${text.replace(/"/g, '""')}"`;
  };

  const csvRows = rows.map((payment) => {
    const createdAt = getDate(payment);

    return [
      payment.id ?? "",
      payment.transactionId || payment.transaction_id || "",
      payment.amount ?? "",
      getMethod(payment),
      getStatus(payment),
      payment.currency || "INR",
      createdAt ? createdAt.toISOString() : "",
    ];
  });

  const csvContent = [headers, ...csvRows]
    .map((row) => row.map(escapeCsv).join(","))
    .join("\r\n");

  const blob = new Blob(["\uFEFF" + csvContent], {
    type: "text/csv;charset=utf-8;",
  });

  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = "payflow-transactions.csv";

  document.body.appendChild(link);
  link.click();
  link.remove();

  URL.revokeObjectURL(url);
}

function App() {
  const [mode, setMode] = useState("login");
  const [token, setToken] = useState("");
  const [userEmail, setUserEmail] = useState("");
  const [activePage, setActivePage] = useState("dashboard");
  const [selectedPayment, setSelectedPayment] = useState(null);

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    phone: "",
  });

  const [paymentForm, setPaymentForm] = useState({
    amount: "",
    paymentMethod: "UPI",
  });

  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("info");
  const [loading, setLoading] = useState(false);
  const [payments, setPayments] = useState([]);
  const [paymentsLoaded, setPaymentsLoaded] = useState(false);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [methodFilter, setMethodFilter] = useState("ALL");

  function notify(text, type = "info") {
    setMessage(text);
    setMessageType(type);
  }

  function updateField(e) {
    setForm((previous) => ({
      ...previous,
      [e.target.name]: e.target.value,
    }));
  }

  function updatePaymentField(e) {
    setPaymentForm((previous) => ({
      ...previous,
      [e.target.name]: e.target.value,
    }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage("");
    setLoading(true);

    try {
      if (mode === "register") {
        await apiRequest("/api/users/register", {
          method: "POST",
          body: JSON.stringify({
            name: form.name,
            email: form.email,
            password: form.password,
            phone: form.phone,
          }),
        });

        setMode("login");
        setForm((previous) => ({
          ...previous,
          name: "",
          password: "",
          phone: "",
        }));

        notify("Registration successful! Please log in.", "success");
      } else {
        const result = await apiRequest("/api/users/login", {
          method: "POST",
          body: JSON.stringify({
            email: form.email,
            password: form.password,
          }),
        });

        const receivedToken =
          typeof result === "string"
            ? result
            : result?.token || result?.accessToken || result?.jwt;

        if (!receivedToken) {
          notify("Login response did not contain a token.", "error");
        } else {
          localStorage.setItem("token", receivedToken);
          setToken(receivedToken);
          setUserEmail(form.email);
          setActivePage("dashboard");
          setSelectedPayment(null);
          setMessage("");
        }
      }
    } catch (error) {
      notify(error.message || "Request failed.", "error");
    } finally {
      setLoading(false);
    }
  }

  async function loadPayments(showMessage = true) {
    setLoading(true);
    if (showMessage) setMessage("");

    try {
      const result = await apiRequest("/api/payments", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const rows = getRows(result);
      setPayments(rows);
      setPaymentsLoaded(true);

      if (showMessage) {
        notify(`Loaded ${rows.length} payment(s).`, "success");
      }

      return rows;
    } catch (error) {
      notify(`Could not load payments: ${error.message}`, "error");
      return null;
    } finally {
      setLoading(false);
    }
  }

  async function createPayment(e) {
    e.preventDefault();
    setMessage("");

    const amount = Number(paymentForm.amount);

    if (!Number.isFinite(amount) || amount < 1) {
      notify("Please enter an amount of at least ₹1.", "error");
      return;
    }

    setLoading(true);

    try {
      const idempotencyKey =
        globalThis.crypto?.randomUUID?.() ||
        `PAY-${Date.now()}-${Math.random().toString(16).slice(2)}`;

      await apiRequest("/api/payments", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          amount,
          paymentMethod: paymentForm.paymentMethod,
          idempotencyKey,
        }),
      });

      const result = await apiRequest("/api/payments", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      setPayments(getRows(result));
      setPaymentsLoaded(true);
      setPaymentForm({ amount: "", paymentMethod: "UPI" });
      setSelectedPayment(null);

      notify("Payment created successfully! Dashboard updated.", "success");
      setActivePage("payments");
    } catch (error) {
      notify(`Could not create payment: ${error.message}`, "error");
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    setToken("");
    setUserEmail("");
    setPayments([]);
    setPaymentsLoaded(false);
    setSelectedPayment(null);
    setMessage("");
    setMessageType("info");
    setActivePage("dashboard");
    setMode("login");
    setForm({ name: "", email: "", password: "", phone: "" });
    setPaymentForm({ amount: "", paymentMethod: "UPI" });
    setSearch("");
    setStatusFilter("ALL");
    setMethodFilter("ALL");
  }

  const successfulPayments = payments.filter(
    (payment) => getStatus(payment) === "SUCCESS"
  );

  const pendingPayments = payments.filter(
    (payment) => getStatus(payment) === "PENDING"
  );

  const failedPayments = payments.filter(
    (payment) => getStatus(payment) === "FAILED"
  );

  const totalAmount = successfulPayments.reduce(
    (sum, payment) => sum + Number(payment.amount || 0),
    0
  );

  const filteredPayments = useMemo(() => {
    const query = search.trim().toLowerCase();

    return payments.filter((payment) => {
      const transactionId = String(
        payment.transactionId || payment.transaction_id || ""
      ).toLowerCase();

      const id = String(payment.id ?? "").toLowerCase();
      const status = getStatus(payment);
      const method = String(getMethod(payment)).toUpperCase();

      const matchesSearch =
        !query || transactionId.includes(query) || id.includes(query);

      const matchesStatus =
        statusFilter === "ALL" || status === statusFilter;

      const matchesMethod =
        methodFilter === "ALL" || method === methodFilter;

      return matchesSearch && matchesStatus && matchesMethod;
    });
  }, [payments, search, statusFilter, methodFilter]);

  const activity = useMemo(() => {
    const today = new Date();
    const days = [];

    for (let offset = 6; offset >= 0; offset--) {
      const date = new Date(today);
      date.setHours(0, 0, 0, 0);
      date.setDate(date.getDate() - offset);

      const nextDate = new Date(date);
      nextDate.setDate(nextDate.getDate() + 1);

      const count = payments.filter((payment) => {
        const created = getDate(payment);
        return created && created >= date && created < nextDate;
      }).length;

      days.push({
        label: date.toLocaleDateString("en-IN", {
          weekday: "short",
        }),
        dateLabel: date.toLocaleDateString("en-IN", {
          day: "numeric",
          month: "short",
        }),
        count,
      });
    }

    return days;
  }, [payments]);

  const hasDatedPayments = payments.some((payment) => getDate(payment));
  const maxActivity = Math.max(1, ...activity.map((day) => day.count));

  function PaymentTable({ rows, limit }) {
    const displayedRows = limit ? rows.slice(0, limit) : rows;

    if (!paymentsLoaded) {
      return (
        <div className="empty-state">
          Load your payments to see real transaction records.
        </div>
      );
    }

    if (displayedRows.length === 0) {
      return <div className="empty-state">No matching payments found.</div>;
    }

    return (
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Transaction ID</th>
              <th>Amount</th>
              <th>Method</th>
              <th>Status</th>
            </tr>
          </thead>

          <tbody>
            {displayedRows.map((payment, index) => {
              const status = getStatus(payment);

              return (
                <tr key={payment.id ?? payment.transactionId ?? index}>
                  <td>{payment.id ?? "—"}</td>

                  <td className="transaction-code">
                    <button
                      type="button"
                      className="transaction-link"
                      onClick={() => {
                        setSelectedPayment(payment);
                        setActivePage("details");
                        setMessage("");
                      }}
                    >
                      {payment.transactionId ||
                        payment.transaction_id ||
                        "View details"}
                    </button>
                  </td>

                  <td className="amount-cell">{money(payment.amount)}</td>

                  <td>{String(getMethod(payment)).replaceAll("_", " ")}</td>

                  <td>
                    <span
                      className={`status-pill status-${status.toLowerCase()}`}
                    >
                      <span className="status-dot" />
                      {status}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    );
  }

  


function PaymentForm() {
  const methods = [
    {
      id: "UPI",
      title: "UPI",
      description: "Pay using UPI apps",
      logos: [
        { name: "Google Pay", src: "https://cdn.simpleicons.org/googlepay/4285F4" },
        { name: "PhonePe", src: "https://cdn.simpleicons.org/phonepe/5F259F" },
        { name: "Paytm", src: "https://cdn.simpleicons.org/paytm/00BAF2" },
      ],
    },
    {
      id: "CARD",
      title: "Credit / Debit Card",
      description: "Visa, Mastercard and RuPay",
      logos: [],
    },
    {
      id: "NET_BANKING",
      title: "Net Banking",
      description: "Pay directly through your bank",
      logos: [],
    },
    {
      id: "WALLET",
      title: "Wallet",
      description: "Pay using your digital wallet",
      logos: [
        { name: "Paytm", src: "https://cdn.simpleicons.org/paytm/00BAF2" },
        { name: "Google Pay", src: "https://cdn.simpleicons.org/googlepay/4285F4" },
      ],
    },
  ];

  const selectedMethod = methods.find(
    (method) => method.id === paymentForm.paymentMethod
  );

  return (
    <div className="checkout-panel">
      <div className="checkout-header">
        <div>
          <span className="checkout-eyebrow">SECURE CHECKOUT</span>
          <h2>Make a Payment</h2>
          <p>Choose your preferred payment method.</p>
        </div>

        <div className="checkout-lock">
          <svg viewBox="0 0 24 24" width="22" height="22"
            fill="none" stroke="currentColor" strokeWidth="1.8"
            strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <rect x="4" y="10" width="16" height="11" rx="2" />
            <path d="M8 10V7a4 4 0 0 1 8 0v3" />
            <path d="M12 14v3" />
          </svg>
        </div>
      </div>

      <div className="checkout-amount-field">
        <label htmlFor="checkout-amount">Payment amount</label>
        <div className="checkout-amount-input">
          <span>₹</span>
          <input
            id="checkout-amount"
            type="number"
            min="1"
            step="0.01"
            placeholder="Enter amount"
            value={paymentForm.amount}
            onChange={(e) =>
              setPaymentForm({ ...paymentForm, amount: e.target.value })
            }
          />
          <span className="checkout-currency">INR</span>
        </div>
      </div>

      <div className="checkout-method-heading">
        <h3>Payment method</h3>
        <span>4 options available</span>
      </div>

      <div className="payment-method-grid">
        {methods.map((method) => {
          const selected = paymentForm.paymentMethod === method.id;

          return (
            <button
              key={method.id}
              type="button"
              className={`payment-method-card ${
                selected ? "payment-method-selected" : ""
              }`}
              onClick={() =>
                setPaymentForm({
                  ...paymentForm,
                  paymentMethod: method.id,
                })
              }
              aria-pressed={selected}
            >
              <div className="method-card-top">
                <span className={`method-radio ${selected ? "active" : ""}`}>
                  {selected && <span />}
                </span>
                {selected && (
                  <span className="method-selected-label">Selected</span>
                )}
              </div>

              <div className="method-card-content">
                <div className="method-card-title">{method.title}</div>
                <div className="method-card-description">
                  {method.description}
                </div>
              </div>

              {method.id === "CARD" ? (
                <div className="method-brand-logos card-brand-icons">
                  <svg
                    className="premium-card-icon"
                    viewBox="0 0 180 116"
                    xmlns="http://www.w3.org/2000/svg"
                    role="img"
                    aria-label="Credit or debit card"
                  >
                    <defs>
                      <linearGradient id="cardBody" x1="0" y1="0" x2="1" y2="1">
                        <stop offset="0%" stopColor="#3730A3" />
                        <stop offset="52%" stopColor="#4F46E5" />
                        <stop offset="100%" stopColor="#171B45" />
                      </linearGradient>
                      <linearGradient id="cardShine" x1="0" y1="0" x2="1" y2="1">
                        <stop offset="0%" stopColor="#FFFFFF" stopOpacity=".25" />
                        <stop offset="100%" stopColor="#FFFFFF" stopOpacity="0" />
                      </linearGradient>
                      <linearGradient id="chipGold" x1="0" y1="0" x2="1" y2="1">
                        <stop offset="0%" stopColor="#FFF1B8" />
                        <stop offset="50%" stopColor="#D9B86C" />
                        <stop offset="100%" stopColor="#A67C32" />
                      </linearGradient>
                    </defs>

                    <rect x="3" y="3" width="174" height="110" rx="15"
                      fill="url(#cardBody)" stroke="#818CF8" strokeWidth="1.5" />

                    <path d="M18 4H160C169 4 176 11 176 20V36L91 92Z"
                      fill="url(#cardShine)" />

                    <circle cx="150" cy="29" r="17" fill="#FFFFFF" fillOpacity=".09" />
                    <circle cx="163" cy="29" r="17" fill="#FFFFFF" fillOpacity=".06" />

                    <rect x="18" y="40" width="31" height="24" rx="5"
                      fill="url(#chipGold)" stroke="#FDE68A" strokeWidth=".7" />

                    <path d="M28 40V64M39 40V64M18 48H49M18 56H49"
                      stroke="#9A793B" strokeWidth="1" fill="none" />

                    <g transform="translate(59 44)" fill="none" stroke="#DDE4FF"
                      strokeWidth="2.2" strokeLinecap="round">
                      <path d="M0 4Q10 14 0 24" />
                      <path d="M6 0Q20 14 6 28" />
                      <path d="M12 -4Q30 14 12 32" />
                    </g>

                    <g fill="#FFFFFF" fillOpacity=".8">
                      <rect x="19" y="77" width="25" height="3" rx="1.5" />
                      <rect x="19" y="84" width="39" height="3" rx="1.5" />
                    </g>

                    <text x="160" y="94" fill="#FFFFFF" fontSize="13"
                      fontWeight="700" textAnchor="end" letterSpacing="1.2">
                      PAYFLOW
                    </text>
                  </svg>

                  <div className="card-network-logos">
                    <span className="card-brand-name visa-brand">VISA</span>
                    <span className="mastercard-brand" aria-label="Mastercard"
                      title="Mastercard">
                      <i />
                      <i />
                    </span>
                    <span className="card-brand-name rupay-brand">RuPay</span>
                  </div>
                </div>
              ) : method.id === "NET_BANKING" ? (
                <div className="method-brand-logos">
                  <span className="bank-method-icon">
                    <svg viewBox="0 0 24 24" width="32" height="32"
                      fill="none" stroke="currentColor" strokeWidth="1.8"
                      strokeLinecap="round" strokeLinejoin="round"
                      aria-hidden="true">
                      <path d="M3 9h18L12 3 3 9Z" />
                      <path d="M5 10v8M9 10v8M15 10v8M19 10v8" />
                      <path d="M3 20h18M2 18h20" />
                    </svg>
                  </span>
                  <span className="bank-logo-caption">BANK TRANSFER</span>
                </div>
              ) : (
                <div className="method-brand-logos">
                  {method.logos.map((logo) => (
                    <img
                      key={logo.name}
                      src={logo.src}
                      alt={logo.name}
                      title={logo.name}
                      className="method-brand-logo"
                      onError={(e) => {
                        e.currentTarget.style.display = "none";
                      }}
                    />
                  ))}
                </div>
              )}
            </button>
          );
        })}
      </div>

      <div className="selected-method-summary">
        <span className="summary-check">✓</span>
        <span>
          Selected: <strong>{selectedMethod?.title || "UPI"}</strong>
        </span>
      </div>

      <button
        type="button"
        className="checkout-pay-button"
        disabled={!paymentForm.amount || Number(paymentForm.amount) <= 0}
        onClick={createPayment}
      >
        <span>Continue to Payment</span>
        <span className="checkout-button-arrow">→</span>
      </button>

      <p className="checkout-disclaimer">
        <span>🔒</span> Demo checkout · No real money will be transferred.
      </p>
    </div>
  );
}




  function TransactionFilters() {
    return (
      <div className="filter-bar">
        <label className="search-box">
          <span aria-hidden="true">⌕</span>
          <input
            aria-label="Search by transaction ID or payment ID"
            placeholder="Search transaction ID..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          {search && (
            <button
              type="button"
              onClick={() => setSearch("")}
              aria-label="Clear search"
            >
              ×
            </button>
          )}
        </label>

        <select
          aria-label="Filter by status"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">All statuses</option>
          <option value="SUCCESS">Success</option>
          <option value="PENDING">Pending</option>
          <option value="FAILED">Failed</option>
        </select>

        <select
          aria-label="Filter by payment method"
          value={methodFilter}
          onChange={(e) => setMethodFilter(e.target.value)}
        >
          <option value="ALL">All methods</option>
          <option value="UPI">UPI</option>
          <option value="CARD">Card</option>
          <option value="NET_BANKING">Net Banking</option>
          <option value="WALLET">Wallet</option>
        </select>

        <button
          className="secondary-button filter-reset"
          type="button"
          onClick={() => {
            setSearch("");
            setStatusFilter("ALL");
            setMethodFilter("ALL");
          }}
        >
          Reset
        </button>
      </div>
    );
  }

  function DashboardPage() {
    return (
      <>
        <div className="page-heading">
          <div>
            <p className="eyebrow">OVERVIEW</p>
            <h1>Good to see you again.</h1>
            <p className="muted">
              Here’s what’s happening with your payments.
            </p>
          </div>

          <button
            className="primary-button"
            onClick={() => loadPayments()}
            disabled={loading}
          >
            {loading ? "Loading..." : "↻ Refresh data"}
          </button>
        </div>

        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-top">
              <span>Total payments</span>
              <span className="stat-icon purple">▦</span>
            </div>
            <h2>{paymentsLoaded ? payments.length : "—"}</h2>
            <p className="stat-foot">
              {paymentsLoaded ? "Loaded records" : "Load data to begin"}
            </p>
          </div>

          <div className="stat-card">
            <div className="stat-top">
              <span>Successful</span>
              <span className="stat-icon green">✓</span>
            </div>
            <h2>{paymentsLoaded ? successfulPayments.length : "—"}</h2>
            <p className="stat-foot">Completed payment records</p>
          </div>

          <div className="stat-card">
            <div className="stat-top">
              <span>Successful volume</span>
              <span className="stat-icon blue">₹</span>
            </div>
            <h2 className="money-stat">
              {paymentsLoaded ? money(totalAmount) : "—"}
            </h2>
            <p className="stat-foot">Sum of successful payments</p>
          </div>

          <div className="stat-card">
            <div className="stat-top">
              <span>Pending / Failed</span>
              <span className="stat-icon yellow">◷</span>
            </div>
            <h2>
              {paymentsLoaded
                ? `${pendingPayments.length} / ${failedPayments.length}`
                : "—"}
            </h2>
            <p className="stat-foot">Awaiting or unsuccessful records</p>
          </div>
        </div>

        <section className="panel activity-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">LAST 7 DAYS</p>
              <h2>Payment activity</h2>
              <p className="muted">
                Payments grouped by their recorded creation date.
              </p>
            </div>
            <span className="panel-icon">↗</span>
          </div>

          {!paymentsLoaded ? (
            <div className="empty-state">Load payments to display activity.</div>
          ) : !hasDatedPayments ? (
            <div className="empty-state">
              Your payment records do not include a readable creation date, so
              the activity chart cannot be calculated yet.
            </div>
          ) : (
            <div className="activity-chart">
              {activity.map((day) => (
                <div className="chart-column" key={day.dateLabel}>
                  <span className="chart-count">{day.count}</span>
                  <div className="chart-track">
                    <div
                      className="chart-bar"
                      style={{
                        height: `${Math.max(
                          day.count > 0 ? 8 : 2,
                          (day.count / maxActivity) * 100
                        )}%`,
                      }}
                    />
                  </div>
                  <span className="chart-day">{day.label}</span>
                  <span className="chart-date">{day.dateLabel}</span>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">LATEST RECORDS</p>
              <h2>Recent transactions</h2>
              <p className="muted">
                Your most recently returned payment records.
              </p>
            </div>

            <button
              className="text-button"
              onClick={() => setActivePage("transactions")}
            >
              View all →
            </button>
          </div>

          <PaymentTable rows={[...payments].reverse()} limit={5} />
        </section>
      </>
    );
  }

  function PaymentsPage() {
    return (
      <>
        <div className="page-heading">
          <div>
            <p className="eyebrow">PAYMENT MANAGEMENT</p>
            <h1>Payments</h1>
            <p className="muted">Create and review payment records.</p>
          </div>

          <button
            className="primary-button"
            onClick={() => loadPayments()}
            disabled={loading}
          >
            {loading ? "Loading..." : "↻ Refresh"}
          </button>
        </div>

        <PaymentForm />

        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>All payments</h2>
              <p className="muted">
                {paymentsLoaded
                  ? `${payments.length} records loaded`
                  : "Load records from your backend."}
              </p>
            </div>
          </div>

          <PaymentTable rows={[...payments].reverse()} />
        </section>
      </>
    );
  }

  function TransactionsPage() {
    return (
      <>
        <div className="page-heading">
          <div>
            <p className="eyebrow">PAYMENT ACTIVITY</p>
            <h1>Transactions</h1>
            <p className="muted">
              Search and filter your payment records.
            </p>
          </div>

          <button
            className="primary-button"
            onClick={() => loadPayments()}
            disabled={loading}
          >
            {loading ? "Loading..." : "↻ Refresh"}
          </button>
        </div>

        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>Transaction history</h2>
              <p className="muted">
                {paymentsLoaded
                  ? `${filteredPayments.length} of ${payments.length} records shown`
                  : "Load payments to start filtering."}
              </p>
            </div>

            <button
              type="button"
              className="primary-button"
              onClick={() => exportPaymentsCsv(filteredPayments)}
              disabled={!paymentsLoaded || filteredPayments.length === 0}
            >
              ↓ Export CSV
            </button>
          </div>

          <TransactionFilters />
          <PaymentTable rows={filteredPayments} />
        </section>
      </>
    );
  }

  function PaymentDetailsPage() {
    if (!selectedPayment) {
      return (
        <section className="panel">
          <h2>No payment selected</h2>
          <p className="muted">
            Select a transaction from the payments table to view its details.
          </p>
          <button
            className="secondary-button"
            onClick={() => setActivePage("transactions")}
          >
            Back to transactions
          </button>
        </section>
      );
    }

    const payment = selectedPayment;
    const status = getStatus(payment);
    const createdAt = getDate(payment);

    const details = [
      ["Payment ID", payment.id ?? "—"],
      [
        "Transaction ID",
        payment.transactionId || payment.transaction_id || "—",
      ],
      ["Amount", money(payment.amount)],
      ["Payment method", String(getMethod(payment)).replaceAll("_", " ")],
      ["Status", status],
      [
        "Creation date",
        createdAt ? createdAt.toLocaleString("en-IN") : "Not available",
      ],
      ["Currency", payment.currency || "INR"],
      ["Description", payment.description || "Not available"],
    ];

    return (
      <>
        <div className="page-heading">
          <div>
            <p className="eyebrow">PAYMENT INFORMATION</p>
            <h1>Payment details</h1>
            <p className="muted">
              View the information returned for this payment.
            </p>
          </div>

          <button
            className="secondary-button"
            onClick={() => setActivePage("transactions")}
          >
            ← Back to transactions
          </button>
        </div>

        <section className="panel payment-details-panel">
          <div className="details-summary">
            <div>
              <p className="eyebrow">PAYMENT AMOUNT</p>
              <h2>{money(payment.amount)}</h2>
            </div>

            <span className={`status-pill status-${status.toLowerCase()}`}>
              <span className="status-dot" />
              {status}
            </span>
          </div>

          <div className="details-grid">
            {details.map(([label, value]) => (
              <div className="detail-item" key={label}>
                <span>{label}</span>
                <strong>{String(value)}</strong>
              </div>
            ))}
          </div>

          <div className="details-footer">
            These details reflect the payment object returned by your backend.
            Fields not returned by the API may show as unavailable.
          </div>
        </section>
      </>
    );
  }

  
function RefundsPage() {
  const [refunds, setRefunds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadRefunds() {
    setLoading(true);
    setError("");

    try {
      const data = await apiRequest("/api/refunds");
      setRefunds(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Unable to load refunds.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadRefunds();
  }, []);

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">PAYMENT REVERSALS</p>
          <h1>Refunds</h1>
          <p className="muted">
            View refund records for your payments.
          </p>
        </div>

        <button
          className="secondary-button"
          onClick={loadRefunds}
          disabled={loading}
        >
          {loading ? "Loading..." : "Refresh"}
        </button>
      </div>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h2>Refund history</h2>
            <p className="muted">
              Total refunds: {refunds.length}
            </p>
          </div>
        </div>

        {loading ? (
          <p className="muted">Loading refund records...</p>
        ) : error ? (
          <div className="refund-placeholder">
            <p>{error}</p>
            <button
              className="secondary-button"
              onClick={loadRefunds}
            >
              Try again
            </button>
          </div>
        ) : refunds.length === 0 ? (
          <div className="refund-placeholder">
            <span className="refund-icon">↶</span>
            <h2>No refunds yet</h2>
            <p className="muted">
              Your refund records will appear here.
            </p>
          </div>
        ) : (
         <div className="table-wrapper refunds-table-wrapper">
           <table className="refunds-table">
              <thead>
                <tr>
                  <th>Refund ID</th>
                  <th>Payment ID</th>
                  <th>Transaction ID</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Reason</th>
                  <th>Date</th>
                </tr>
              </thead>

              <tbody>
                {refunds.map((refund) => (
                  <tr key={refund.refundId}>
                    <td>{refund.refundId}</td>
                    <td>{refund.paymentId}</td>
                    <td>{refund.transactionId}</td>
                    <td>
                      ₹{Number(refund.refundAmount).toFixed(2)}
                    </td>
                    <td>
                      <span className="status-badge">
                        {refund.status}
                      </span>
                    </td>
                    <td>{refund.reason}</td>
                    <td>
                      {refund.createdAt
                        ? new Date(refund.createdAt).toLocaleString()
                        : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}

  function SettingsPage() {
    return (
      <>
        <div className="page-heading">
          <div>
            <p className="eyebrow">PREFERENCES</p>
            <h1>Settings</h1>
            <p className="muted">Your current account and session.</p>
          </div>
        </div>

        <section className="panel settings-panel">
          <div className="settings-avatar">
            {(userEmail[0] || "U").toUpperCase()}
          </div>

          <div className="settings-account">
            <h2>Account details</h2>
            <p className="muted">Signed in to PayFlow</p>
          </div>

          <div className="settings-row">
            <span>Email address</span>
            <strong>{userEmail}</strong>
          </div>

          <div className="settings-row">
            <span>Session status</span>
            <span className="status-pill status-success">
              <span className="status-dot" />
              Authenticated
            </span>
          </div>

          <div className="settings-row">
            <span>Backend connection</span>
            <strong>Configured through api.js</strong>
          </div>

          <button
            className="secondary-button settings-logout"
            onClick={logout}
          >
            Log out
          </button>
        </section>
      </>
    );
  }

  const pageTitles = {
    dashboard: "Dashboard",
    payments: "Payments",
    transactions: "Transactions",
    details: "Payment Details",
    refunds: "Refunds",
    settings: "Settings",
  };

  if (!token) {
    return (
      <div className="app-shell auth-shell">
        <main className="auth-card">
          <div className="brand auth-brand">
            <span className="brand-icon">P</span>
            PayFlow
          </div>

          <p className="eyebrow">SECURE PAYMENT PLATFORM</p>
          <h1>
            {mode === "login" ? "Welcome back" : "Create your account"}
          </h1>

          <p className="muted">
            {mode === "login"
              ? "Log in to continue to your payment dashboard."
              : "Register to get started with PayFlow."}
          </p>

          <div className="auth-tabs">
            <button
              type="button"
              className={mode === "login" ? "auth-tab selected" : "auth-tab"}
              onClick={() => {
                setMode("login");
                setMessage("");
              }}
            >
              Login
            </button>

            <button
              type="button"
              className={
                mode === "register" ? "auth-tab selected" : "auth-tab"
              }
              onClick={() => {
                setMode("register");
                setMessage("");
              }}
            >
              Register
            </button>
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            {mode === "register" && (
              <>
                <label htmlFor="name">Full name</label>
                <input
                  id="name"
                  name="name"
                  value={form.name}
                  onChange={updateField}
                  placeholder="Enter your name"
                  autoComplete="name"
                  required
                />

                <label htmlFor="phone">Phone number</label>
                <input
                  id="phone"
                  name="phone"
                  type="tel"
                  value={form.phone}
                  onChange={updateField}
                  placeholder="Enter your phone number"
                  autoComplete="tel"
                  required
                />
              </>
            )}

            <label htmlFor="email">Email address</label>
            <input
              id="email"
              name="email"
              type="email"
              value={form.email}
              onChange={updateField}
              placeholder="you@example.com"
              autoComplete="email"
              required
            />

            <label htmlFor="password">Password</label>
            <input
              id="password"
              name="password"
              type="password"
              value={form.password}
              onChange={updateField}
              placeholder="Enter your password"
              autoComplete={
                mode === "login" ? "current-password" : "new-password"
              }
              required
            />

            <button
              type="submit"
              className="primary-button full-button"
              disabled={loading}
            >
              {loading
                ? "Please wait..."
                : mode === "login"
                  ? "Login to PayFlow →"
                  : "Create account →"}
            </button>
          </form>

          {message && (
            <p
              className={`auth-message ${
                messageType === "success" ? "success-message" : ""
              }`}
              role="status"
            >
              {message}
            </p>
          )}

          <p className="form-footnote">
            🔒 Requests are sent to your local backend.
          </p>
        </main>
      </div>
    );
  }

  return (
    <div className="app-shell workspace">
      <aside className="sidebar">
       <button
  type="button"
  className="brand sidebar-brand"
  onClick={() => {
    setActivePage("dashboard");
    setSelectedPayment(null);
    setMessage("");
  }}
  style={{
    border: "none",
    cursor: "pointer",
    width: "100%",
    textAlign: "left",
  }}
>
  <span className="brand-icon">P</span>
  PayFlow
</button>

        <div className="sidebar-label">WORKSPACE</div>

        <nav className="side-nav" aria-label="Main navigation">
          {navItems.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${
                activePage === item.id ? "active" : ""
              }`}
              onClick={() => {
                setActivePage(item.id);
                setMessage("");
              }}
              aria-current={activePage === item.id ? "page" : undefined}
            >
              <span className="nav-icon">{item.icon}</span>
              <span>{item.label}</span>

              {item.id === "transactions" && paymentsLoaded && (
                <span className="nav-count">{payments.length}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="secure-note">
            <span>●</span>
            <div>
              <strong>Secure session</strong>
              <small>Authenticated access</small>
            </div>
          </div>

          <button className="nav-item logout-nav" onClick={logout}>
            <span className="nav-icon">↪</span>
            <span>Log out</span>
          </button>
        </div>
      </aside>

      <div className="main-content">
        <header className="topbar">
          <div className="mobile-brand brand">
            <span className="brand-icon">P</span>
            PayFlow
          </div>

          <div className="breadcrumb">
            <span>Workspace</span>
            <span className="breadcrumb-separator">/</span>
            <strong>{pageTitles[activePage]}</strong>
          </div>

          <div className="topbar-right">
            <span className="connection-indicator">
              <span />
              Connected
            </span>

            

           <button
  type="button"
  className="user-profile-button"
  onClick={logout}
  title="Switch account"
>
  <span className="user-avatar">
    {(userEmail[0] || "U").toUpperCase()}
  </span>
  <span className="user-info">
    <strong>{userEmail.split("@")[0]}</strong>
    <span>{userEmail}</span>
  </span>
</button>
          </div>
        </header>

        <main className="dashboard">
          {message && (
            <div className={`notice notice-${messageType}`} role="status">
              <span>
                {messageType === "error"
                  ? "!"
                  : messageType === "success"
                    ? "✓"
                    : "i"}
              </span>

              <p>{message}</p>

              <button
                type="button"
                aria-label="Dismiss message"
                onClick={() => setMessage("")}
              >
                ×
              </button>
            </div>
          )}

          {activePage === "dashboard" && <DashboardPage />}
          {activePage === "payments" && <PaymentsPage />}
          {activePage === "transactions" && <TransactionsPage />}
          {activePage === "details" && <PaymentDetailsPage />}
          {activePage === "refunds" && <RefundsPage />}
          {activePage === "settings" && <SettingsPage />}

          <footer className="app-footer">
            <span>PayFlow · Payment management workspace</span>
            <span>Secure authenticated session</span>
          </footer>
        </main>
      </div>
    </div>
  );
}

export default App;