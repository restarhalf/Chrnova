// 4 个 Worker 共用的 HTTP 辅助函数：常量时间比较、管理员鉴权、客户端 IP、内存限流。

export function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

export function isAdmin(c: any): boolean {
  const expected = c.env.ADMIN_TOKEN;
  if (!expected) return false;
  const token = c.req.header('Authorization')?.replace(/^Bearer\s+/i, '')?.trim();
  if (!token) return false;
  return timingSafeEqual(token, expected);
}

export function clientIp(c: any): string {
  return c.req.header('CF-Connecting-IP') || c.req.header('X-Forwarded-For') || 'unknown';
}

const rateBuckets = new Map<string, { count: number; reset: number }>();
export function rateLimit(bucket: string, limit: number, windowSec: number): boolean {
  const now = Date.now();
  const b = rateBuckets.get(bucket);
  if (!b || now > b.reset) {
    rateBuckets.set(bucket, { count: 1, reset: now + windowSec * 1000 });
    return true;
  }
  if (b.count >= limit) return false;
  b.count++;
  return true;
}

export const CORS_ORIGINS = [
  'https://chrnova.restarhalf.dpdns.org',
  'http://localhost:5173',
  'http://127.0.0.1:5173',
];
