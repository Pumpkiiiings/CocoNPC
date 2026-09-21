const DEFAULT_BODY_LIMIT = 32 * 1024;
const MAX_BODY_LIMIT = 256 * 1024;

export function requestBodyLimit(): number {
  const configured = Number.parseInt(process.env.CHAT_MAX_BODY_BYTES ?? '', 10);
  return Number.isFinite(configured) && configured >= 1024 && configured <= MAX_BODY_LIMIT
    ? configured
    : DEFAULT_BODY_LIMIT;
}

export function requestClientKey(request: Request): string {
  const forwarded = request.headers.get('x-forwarded-for')?.split(',')[0]?.trim();
  const realIp = request.headers.get('x-real-ip')?.trim();
  return forwarded || realIp || 'unknown';
}

export function isSameOriginRequest(request: Request): boolean {
  const origin = request.headers.get('origin');
  if (!origin) return false;

  const allowed = new Set<string>([new URL(request.url).origin]);
  const configured = process.env.NEXT_PUBLIC_SITE_URL;
  if (configured) {
    try {
      allowed.add(new URL(configured).origin);
    } catch {
      // Invalid deployment configuration must not broaden access.
    }
  }

  return allowed.has(origin);
}
