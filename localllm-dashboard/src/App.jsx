import { useState, useEffect } from "react";
import { createClient } from "@supabase/supabase-js";

const supabase = createClient(
  import.meta.env.VITE_SUPABASE_URL,
  import.meta.env.VITE_SUPABASE_ANON_KEY
);

function generateKey() {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  return "llm_" + Array.from({ length: 48 }, () =>
    chars[Math.floor(Math.random() * chars.length)]
  ).join("");
}

export default function App() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [apiKey, setApiKey] = useState(null);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState("");
  const [stats, setStats] = useState({ total: 0, active: 0 });

  useEffect(() => { fetchStats(); }, []);

  async function fetchStats() {
    const { data } = await supabase.from("api_keys").select("is_active");
    if (data) setStats({ total: data.length, active: data.filter(k => k.is_active).length });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!name.trim() || !email.trim()) return;
    setLoading(true); setError("");
    try {
      const key = generateKey();
      const { error: err } = await supabase.from("api_keys").insert({
        key, name: name.trim(), email: email.trim(),
        is_active: true, usage_count: 0
      });
      if (err) throw err;
      setApiKey(key);
    } catch (err) {
      setError(err.message || "Failed to generate key. Try again.");
    } finally { setLoading(false); }
  }

  async function copyKey() {
    await navigator.clipboard.writeText(apiKey);
    setCopied(true); setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div style={styles.root}>
      <div style={styles.container}>
        {/* Header */}
        <div style={styles.header}>
          <div style={styles.logo}>🤖 LocalLLM</div>
          <p style={styles.tagline}>Local AI coding assistant for Android</p>
          <div style={styles.badges}>
            <span style={styles.badge}>⚡ Fully Offline</span>
            <span style={styles.badge}>🔒 100% Private</span>
            <span style={styles.badge}>🆓 Free API</span>
          </div>
        </div>

        {/* Stats */}
        <div style={styles.statsRow}>
          <Stat label="API Keys Issued" value={stats.total} />
          <Stat label="Active Keys" value={stats.active} />
          <Stat label="Rate Limit" value="Unlimited" />
        </div>

        {/* Main card */}
        {!apiKey ? (
          <div style={styles.card}>
            <h2 style={styles.cardTitle}>Get Your Free API Key</h2>
            <p style={styles.cardDesc}>
              Integrate LocalLLM into your own Android, web, or backend app.
              Your key authenticates requests to the LocalLLM server running on any device.
            </p>
            {error && <div style={styles.error}>{error}</div>}
            <form onSubmit={handleSubmit}>
              <label style={styles.label}>Your Name</label>
              <input style={styles.input} value={name} onChange={e => setName(e.target.value)}
                placeholder="LaiserDev" required />
              <label style={styles.label}>Email</label>
              <input style={styles.input} type="email" value={email}
                onChange={e => setEmail(e.target.value)} placeholder="you@example.com" required />
              <button type="submit" style={loading ? styles.btnDisabled : styles.btn} disabled={loading}>
                {loading ? "Generating..." : "Generate API Key →"}
              </button>
            </form>
          </div>
        ) : (
          <div style={styles.card}>
            <div style={styles.successIcon}>✅</div>
            <h2 style={{ ...styles.cardTitle, color: "#3FB950" }}>API Key Created!</h2>
            <p style={styles.cardDesc}>Save this key — it won't be shown again.</p>
            <div style={styles.keyBox}>
              <code style={styles.keyText}>{apiKey}</code>
              <button style={styles.copyBtn} onClick={copyKey}>
                {copied ? "Copied!" : "Copy"}
              </button>
            </div>
            <div style={styles.infoBox}>
              <p style={styles.infoTitle}>How to use:</p>
              <pre style={styles.code}>{`// Add to request headers
'X-API-Key': '${apiKey.slice(0, 20)}...'

// Endpoint (device must have LocalLLM open)
POST http://DEVICE_IP:8080/v1/chat
POST http://DEVICE_IP:8080/v1/code`}</pre>
            </div>
            <button style={styles.btnSecondary} onClick={() => { setApiKey(null); setName(""); setEmail(""); }}>
              Generate Another Key
            </button>
          </div>
        )}

        {/* Features */}
        <div style={styles.features}>
          {[
            { icon: "💻", title: "Code Generation", desc: "POST /v1/code — generates production-ready code in any language" },
            { icon: "💬", title: "Chat Completions", desc: "POST /v1/chat — conversational AI for any use case" },
            { icon: "📱", title: "On-Device Inference", desc: "All AI runs on the Android device — no cloud, no latency, no cost" },
            { icon: "🔑", title: "Simple Auth", desc: "Pass your key via X-API-Key header — that's it" }
          ].map(f => (
            <div key={f.title} style={styles.featureCard}>
              <span style={styles.featureIcon}>{f.icon}</span>
              <h3 style={styles.featureTitle}>{f.title}</h3>
              <p style={styles.featureDesc}>{f.desc}</p>
            </div>
          ))}
        </div>

        <p style={styles.footer}>
          Built by <a href="https://github.com/LaiserDev" style={styles.link}>LaiserDev</a> •{" "}
          <a href="https://github.com/LaiserDev/LocalLLM" style={styles.link}>GitHub</a>
        </p>
      </div>
    </div>
  );
}

function Stat({ label, value }) {
  return (
    <div style={styles.stat}>
      <div style={styles.statValue}>{value}</div>
      <div style={styles.statLabel}>{label}</div>
    </div>
  );
}

const styles = {
  root: { minHeight: "100vh", background: "#0D1117", fontFamily: "'Inter', system-ui, sans-serif", padding: "24px 16px" },
  container: { maxWidth: 640, margin: "0 auto" },
  header: { textAlign: "center", marginBottom: 32 },
  logo: { fontSize: 36, fontWeight: 800, color: "#E6EDF3", marginBottom: 8 },
  tagline: { color: "#8B949E", fontSize: 16, margin: "0 0 16px" },
  badges: { display: "flex", gap: 8, justifyContent: "center", flexWrap: "wrap" },
  badge: { background: "#161B22", border: "1px solid #30363D", borderRadius: 20, padding: "4px 12px", color: "#8B949E", fontSize: 12 },
  statsRow: { display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 10, marginBottom: 24 },
  stat: { background: "#161B22", border: "1px solid #30363D", borderRadius: 10, padding: "16px 12px", textAlign: "center" },
  statValue: { fontSize: 22, fontWeight: 700, color: "#3FB950" },
  statLabel: { fontSize: 11, color: "#8B949E", marginTop: 4 },
  card: { background: "#161B22", border: "1px solid #30363D", borderRadius: 12, padding: 24, marginBottom: 24 },
  cardTitle: { color: "#E6EDF3", fontSize: 20, fontWeight: 700, marginBottom: 8, marginTop: 0 },
  cardDesc: { color: "#8B949E", fontSize: 13, lineHeight: 1.6, marginBottom: 20 },
  label: { display: "block", color: "#8B949E", fontSize: 12, marginBottom: 6, marginTop: 12 },
  input: { width: "100%", background: "#21262D", border: "1px solid #30363D", borderRadius: 8, padding: "10px 12px", color: "#E6EDF3", fontSize: 14, boxSizing: "border-box", outline: "none" },
  btn: { width: "100%", background: "#3FB950", border: "none", borderRadius: 8, padding: "12px", color: "#0D1117", fontSize: 14, fontWeight: 700, cursor: "pointer", marginTop: 16 },
  btnDisabled: { width: "100%", background: "#30363D", border: "none", borderRadius: 8, padding: "12px", color: "#8B949E", fontSize: 14, cursor: "not-allowed", marginTop: 16 },
  btnSecondary: { width: "100%", background: "transparent", border: "1px solid #30363D", borderRadius: 8, padding: "10px", color: "#8B949E", fontSize: 13, cursor: "pointer", marginTop: 12 },
  error: { background: "#2D1B1B", border: "1px solid #F85149", borderRadius: 8, padding: "10px 12px", color: "#F85149", fontSize: 13, marginBottom: 16 },
  successIcon: { textAlign: "center", fontSize: 40, marginBottom: 8 },
  keyBox: { display: "flex", alignItems: "center", background: "#0D1117", border: "1px solid #3FB950", borderRadius: 8, padding: "12px", marginBottom: 16, gap: 8 },
  keyText: { flex: 1, color: "#3FB950", fontFamily: "monospace", fontSize: 12, wordBreak: "break-all" },
  copyBtn: { background: "#3FB950", border: "none", borderRadius: 6, padding: "6px 12px", color: "#0D1117", fontSize: 12, fontWeight: 700, cursor: "pointer", whiteSpace: "nowrap" },
  infoBox: { background: "#0D1117", borderRadius: 8, padding: 12, marginBottom: 8 },
  infoTitle: { color: "#8B949E", fontSize: 11, margin: "0 0 8px" },
  code: { color: "#58A6FF", fontFamily: "monospace", fontSize: 11, margin: 0, lineHeight: 1.6 },
  features: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 24 },
  featureCard: { background: "#161B22", border: "1px solid #30363D", borderRadius: 10, padding: 16 },
  featureIcon: { fontSize: 24 },
  featureTitle: { color: "#E6EDF3", fontSize: 13, fontWeight: 600, margin: "8px 0 4px" },
  featureDesc: { color: "#8B949E", fontSize: 11, lineHeight: 1.5, margin: 0 },
  footer: { textAlign: "center", color: "#484F58", fontSize: 12, paddingBottom: 24 },
  link: { color: "#58A6FF", textDecoration: "none" }
};
