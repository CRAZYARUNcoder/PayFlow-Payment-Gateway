
const API_BASE_URL = "http://localhost:8081";

export async function apiRequest(path, options = {}) {
  const token = localStorage.getItem("token");

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  });

  const text = await response.text();
  let data;

  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }

  if (!response.ok) {
    throw new Error(
      typeof data === "string"
        ? data
        : data?.message || `Request failed: ${response.status}`
    );
  }

  return data;
}