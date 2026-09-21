type RateLimitEntry = {
  count: number;
  resetAt: number;
};

export type RateLimitResult = {
  allowed: boolean;
  limit: number;
  remaining: number;
  resetAt: number;
};

const entries = new Map<string, RateLimitEntry>();

export function consumeRateLimit(
  key: string,
  options: { limit: number; windowMs: number; now?: number },
): RateLimitResult {
  const now = options.now ?? Date.now();
  const existing = entries.get(key);
  const entry = !existing || existing.resetAt <= now
    ? { count: 0, resetAt: now + options.windowMs }
    : existing;

  entry.count += 1;
  entries.set(key, entry);

  // Keep the process-local fallback bounded on long-lived Node processes.
  if (entries.size > 10_000) {
    for (const [candidate, value] of entries) {
      if (value.resetAt <= now) entries.delete(candidate);
    }
  }

  return {
    allowed: entry.count <= options.limit,
    limit: options.limit,
    remaining: Math.max(0, options.limit - entry.count),
    resetAt: entry.resetAt,
  };
}

export function resetRateLimitsForTests() {
  entries.clear();
}
