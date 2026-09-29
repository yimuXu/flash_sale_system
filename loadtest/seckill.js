import http from 'k6/http';
import { Counter } from 'k6/metrics';

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

export default function () {
  const res = http.post(`http://host.docker.internal:8081/seckill/1/${__VU}`);

  if (res.status === 0) networkError.add(1);
  else if (res.status === 201) success.add(1);
  else if (res.status >= 500) serverError.add(1);
  else if (res.body.includes('out of stock')) soldOut.add(1);
  else if (res.body.includes('Duplicate')) duplicate.add(1);
  else if (res.body.includes('not started')) notStarted.add(1);
  else if (res.status === 409) lockFail.add(1);
  else otherFail.add(1);

}
