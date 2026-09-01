import { beforeEach, describe, expect, it } from 'vitest';
import { consumeRateLimit, resetRateLimitsForTests } from './rate-limit';

describe('consumeRateLimit', () => {
  beforeEach(resetRateLimitsForTests);

  it('rejects requests beyond the configured limit', () => {
    expect(consumeRateLimit('client', { limit: 2, windowMs: 1000, now: 0 }).allowed).toBe(true);
    expect(consumeRateLimit('client', { limit: 2, windowMs: 1000, now: 1 }).allowed).toBe(true);
    expect(consumeRateLimit('client', { limit: 2, windowMs: 1000, now: 2 }).allowed).toBe(false);
  });

  it('starts a fresh window after expiration', () => {
    consumeRateLimit('client', { limit: 1, windowMs: 1000, now: 0 });
    expect(consumeRateLimit('client', { limit: 1, windowMs: 1000, now: 1000 }).allowed).toBe(true);
  });
});
