import http from 'k6/http';
import { Counter } from 'k6/metrics';

// 1000 virtual users, each buy 1 time (simulate 1000 people buy 100 items simultaneously)
export const options = {
  scenarios: { rush: { executor: 'per-vu-iterations', vus: 1000, iterations: 1 } },
};

const success = new Counter('seckill_success');
const lockFail = new Counter('seckill_lock_fail');
const soldOut = new Counter('seckill_sold_out');
const serverError = new Counter('seckill_5xx');

export default function () {
  const userId = __VU;   // virtual user ID 1..1000, corresponding to the user ID inserted above
  const res = http.post(`http://host.docker.internal:8081/seckill/1/${userId}`);

  if (res.status === 201) success.add(1);
  else if (res.status >= 500) serverError.add(1);
  else if (res.body.includes('out of stock')) soldOut.add(1);
  else if (res.status === 409) lockFail.add(1);
}
