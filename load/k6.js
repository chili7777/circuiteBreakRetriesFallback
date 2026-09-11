import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Rate } from 'k6/metrics';

const scenario = __ENV.SCENARIO || 'smoke';
const base = __ENV.BASE_URL || 'http://localhost:8080';
const businessSuccess = new Rate('business_success');
const controlledResponse = new Rate('controlled_response');
const validContract = new Rate('valid_contract');
const profiles = {
 smoke:    { executor: 'constant-arrival-rate', rate: 1, timeUnit: '1s', duration: '30s', preAllocatedVUs: 2, maxVUs: 10 },
 baseline: { executor: 'constant-arrival-rate', rate: 20, timeUnit: '1s', duration: '2m', preAllocatedVUs: 20, maxVUs: 80 },
 load:     { executor: 'constant-arrival-rate', rate: 80, timeUnit: '1s', duration: '3m', preAllocatedVUs: 80, maxVUs: 300 },
 stress:   { executor: 'ramping-arrival-rate', startRate: 20, timeUnit: '1s', preAllocatedVUs: 100, maxVUs: 500, stages: [{target:80,duration:'1m'},{target:150,duration:'2m'},{target:200,duration:'1m'}] },
 spike:    { executor: 'ramping-arrival-rate', startRate: 20, timeUnit: '1s', preAllocatedVUs: 100, maxVUs: 500, stages: [{target:20,duration:'30s'},{target:150,duration:'1s'},{target:150,duration:'59s'},{target:20,duration:'1s'},{target:20,duration:'2m'}] },
 soak:     { executor: 'constant-arrival-rate', rate: 20, timeUnit: '1s', duration: __ENV.SOAK_DURATION || '15m', preAllocatedVUs: 30, maxVUs: 100 },
 outage:   { executor: 'constant-arrival-rate', rate: 20, timeUnit: '1s', duration: '2m', preAllocatedVUs: 30, maxVUs: 150 }
};
if (!profiles[scenario]) throw new Error(`Unknown scenario: ${scenario}`);
const thresholds = { dropped_iterations: ['count==0'] };
if (scenario === 'outage') {
 thresholds.business_success = ['rate==0'];
 thresholds.controlled_response = ['rate==1'];
 thresholds['http_req_duration{kind:order}'] = ['p(95)<1000'];
} else if (scenario !== 'stress' && scenario !== 'spike') {
 thresholds.business_success = ['rate>=0.99'];
 thresholds['http_req_duration{kind:order}'] = ['p(95)<500', 'p(99)<1000'];
 thresholds['http_req_failed{kind:order}'] = ['rate<0.01'];
}
export const options = {
 scenarios: { selected: profiles[scenario] },
 setupTimeout: '90s',
 summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
 thresholds
};
export function setup() {
 for (let attempt = 0; attempt < 60; attempt++) {
   const r = http.get(`${base}/actuator/health/readiness`, {timeout:'1s', tags:{kind:'health'}});
   if (r.status === 200) {
     console.log('Orders is ready. Business SLI counts CONFIRMED / sent orders, not all checks.');
     if (scenario === 'outage') console.log('Outage gate assumes a 100% unavailable/slow provider; no successful orders expected.');
     if (scenario === 'stress' || scenario === 'spike') console.log('Exploratory profile: exit 0 does not certify SLO/recovery. Inspect time-series and recovery phase.');
     return;
   }
   sleep(1);
 }
 fail('Orders is not ready. Start both services; check docker compose logs or IDE logs and BASE_URL.');
}
export default function () {
 const id = `${__VU}-${__ITER}-${Date.now()}`;
 const res = http.post(`${base}/api/orders`, JSON.stringify({orderId:id, amount:10.25}), {
   headers:{'Content-Type':'application/json'}, timeout:'3s', tags:{kind:'order'}
 });
 let body = {}; try { body = res.json(); } catch (_) {}
 const confirmed = res.status === 200 && body.status === 'CONFIRMED';
 const controlled = (res.status === 200 || res.status === 503) && ['REJECTED','UNAVAILABLE','PENDING'].includes(body.status);
 businessSuccess.add(confirmed);
 controlledResponse.add(controlled);
 validContract.add(confirmed || controlled);
 check(res, {'known business response': () => confirmed || controlled});
}
