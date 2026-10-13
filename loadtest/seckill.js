import http from 'k6/http';
import { Counter } from 'k6/metrics';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const SECRET = __ENV.JWT_SECRET || 'dev-only-secret-do-not-use-in-production-0123456789';
const HEADER = encoding.b64encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }), 'rawurl');

function tokenFor(userId) {
  const exp = Math.floor(Date.now() / 1000) + 600;
  const payload = encoding.b64encode(JSON.stringify({ sub: String(userId), role: 'USER', exp }), 'rawurl');
  const sig = crypto.hmac('sha256', SECRET, `${HEADER}.${payload}`, 'base64rawurl');
  return `${HEADER}.${payload}.${sig}`;
}

export const options = {
  scenarios: { rush: { executor: 'per-vu-iterations', vus: 1000, iterations: 1 } },
};

const success = new Counter('seckill_success');
const soldOut = new Counter('seckill_sold_out');
const duplicate = new Counter('seckill_duplicate');
const notStarted = new Counter('seckill_not_started');
const serverError = new Counter('seckill_5xx');
const otherFail = new Counter('seckill_other');
const networkError = new Counter('seckill_network');
const lockFail = new Counter('seckill_lock');
const unauthorized = new Counter('seckill_unauthorized');   // 401/403: token rejected
const limited = new Counter('seckill_limited');             // 429: rejected by the rate limit

export default function () {
  // each virtual user is a different user, identified by its token
  const res = http.post('http://host.docker.internal:8081/seckill/1', null,
    { headers: { Authorization: `Bearer ${tokenFor(__VU)}` } });

  if (res.status === 0) networkError.add(1);
  else if (res.status === 202 || res.status === 201) success.add(1);
  else if (res.status === 401 || res.status === 403) unauthorized.add(1);
  else if (res.status === 429) limited.add(1);
  else if (res.status >= 500) serverError.add(1);
  else if (res.body.includes('out of stock')) soldOut.add(1);
  else if (res.body.includes('Duplicate')) duplicate.add(1);
  else if (res.body.includes('not started')) notStarted.add(1);
  else if (res.status === 409) lockFail.add(1);
  else otherFail.add(1);

}
