import { afterEach, describe, expect, it } from 'vitest';
import { isSameOriginRequest, requestBodyLimit, requestClientKey } from './request-security';

const originalBodyLimit = process.env.CHAT_MAX_BODY_BYTES;

afterEach(() => {
  if (originalBodyLimit === undefined) delete process.env.CHAT_MAX_BODY_BYTES;
  else process.env.CHAT_MAX_BODY_BYTES = originalBodyLimit;
});

describe('request security', () => {
  it('accepts only matching browser origins', () => {
    const accepted = new Request('https://docs.example.com/api/chat', {
      headers: { origin: 'https://docs.example.com' },
    });
    const rejected = new Request('https://docs.example.com/api/chat', {
      headers: { origin: 'https://attacker.example' },
    });

    expect(isSameOriginRequest(accepted)).toBe(true);
    expect(isSameOriginRequest(rejected)).toBe(false);
  });

  it('uses a safe default for invalid body-limit configuration', () => {
    process.env.CHAT_MAX_BODY_BYTES = 'invalid';
    expect(requestBodyLimit()).toBe(32 * 1024);
    process.env.CHAT_MAX_BODY_BYTES = String(1024 * 1024);
    expect(requestBodyLimit()).toBe(32 * 1024);
  });

  it('uses the first forwarded client address', () => {
    const request = new Request('https://docs.example.com/api/chat', {
      headers: { 'x-forwarded-for': '203.0.113.10, 10.0.0.2' },
    });
    expect(requestClientKey(request)).toBe('203.0.113.10');
  });
});
