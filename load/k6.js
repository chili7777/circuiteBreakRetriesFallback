import http from 'k6/http';
import { check } from 'k6';

const scenario = __ENV.SCENARIO || 'smoke';
const profiles = {
 smoke:    { executor: 'constant-vus', vus: 1, duration: '30s' },
 baseline: { executor: 'constant-arrival-rate', rate: 20, timeUnit: '1s', duration: '2m', preAllocatedVUs: 20, maxVUs: 80 },
 load:     { executor: 'constant-arrival-rate', rate: 80, timeUnit: '1s', duration: '3m', preAllocatedVUs: 80, maxVUs: 300 },
 stress:   { executor: 'ramping-arrival-rate', startRate: 20, timeUnit: '1s', preAllocatedVUs: 100, maxVUs: 500, stages: [{target:80,duration:'1m'},{target:150,duration:'2m'},{target:200,duration:'1m'}] },
 spike:    { executor: 'ramping-arrival-rate', startRate: 20, timeUnit: '1s', preAllocatedVUs: 100, maxVUs: 500, stages: [{target:20,duration:'30s'},{target:150,duration:'10s'},{target:150,duration:'50s'},{target:20,duration:'10s'},{target:20,duration:'2m'}] },
 soak:     { executor: 'constant-arrival-rate', rate: 20, timeUnit: '1s', duration: __ENV.SOAK_DURATION || '15m', preAllocatedVUs: 30, maxVUs: 100 },
 outage:   { executor: 'constant-arrival-rate', rate: 20, timeUnit: '1s', duration: '2m', preAllocatedVUs: 30, maxVUs: 150 }
};
export const options = {
 scenarios: { selected: profiles[scenario] },
 thresholds: {
   http_req_duration: ['p(95)<500', 'p(99)<1000'],
   http_req_failed: ['rate<0.01'],
   checks: ['rate>0.99'],
   dropped_iterations: ['count==0']
 }
};
const base = __ENV.BASE_URL || 'http://localhost:8080';
export default function () {
 const id = `${__VU}-${__ITER}-${Date.now()}`;
 const res = http.post(`${base}/api/orders`, JSON.stringify({orderId:id, amount:10.25}), {headers:{'Content-Type':'application/json'}});
 let body = {}; try { body = res.json(); } catch (_) {}
 check(res, {
   'transport is 200': r => r.status === 200,
   'business operation confirmed': () => body.status === 'CONFIRMED'
 });
}
