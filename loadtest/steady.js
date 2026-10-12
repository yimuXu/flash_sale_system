import http from 'k6/http';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';

const RATE = Number(__ENV.RATE) || 500;      // requests per second
const DURATION = __ENV.DURATION || '30s';    // duration

export const options = {
  scenarios: {
    steady: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.min(RATE, 1000),
      maxVUs: RATE * 2,
    },
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const success = new Counter('seckill_success');
const limited = new Counter('seckill_limited');        // 429 Too Many Requests
const soldOut = new Counter('seckill_sold_out');
const duplicate = new Counter('seckill_duplicate');
const notStarted = new Counter('seckill_not_started');
const serverError = new Counter('seckill_5xx');
const networkError = new Counter('seckill_network');
const otherFail = new Counter('seckill_other');

const tAccepted = new Trend('t_accepted', true);       // Accepted requests latency
const tLimited = new Trend('t_limited', true);         // Throttled requests latency

export default function () {
  // Each request uses a different user: the nth request in the entire test uses user n
  const userId = exec.scenario.iterationInTest + 1;
  const res = http.post(`http://host.docker.internal:8081/seckill/1/${userId}`);

  if (res.status === 0) networkError.add(1);
  else if (res.status === 201 || res.status === 202) { success.add(1); tAccepted.add(res.timings.duration); }
  else if (res.status === 429) { limited.add(1); tLimited.add(res.timings.duration); }
  else if (res.status >= 500) serverError.add(1);
  else if (res.body.includes('out of stock')) soldOut.add(1);
  else if (res.body.includes('Duplicate')) duplicate.add(1);
  else if (res.body.includes('not started')) notStarted.add(1);
  else otherFail.add(1);
}
