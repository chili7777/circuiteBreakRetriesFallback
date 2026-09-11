"""CI/maintainer check. Python is NOT a prerequisite for workshop participants.
Run after mvn clean verify. Uses only Python standard library and the installed JDK.
"""
import json
import pathlib
import subprocess
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
REPORTS = ROOT / 'reports'
REPORTS.mkdir(exist_ok=True)
PROCESSES = []
LOGS = []


def http(url, method='GET', data=None):
    body = json.dumps(data).encode() if data is not None else None
    request = urllib.request.Request(url, data=body, method=method,
                                     headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=5) as response:
        return json.load(response)


def order(i):
    return http('http://localhost:8080/api/orders', 'POST',
                {'orderId': f'ci-{i}', 'amount': 10.25})


def mode(percent, delay=0):
    return http('http://localhost:8081/api/payments/lab/failure-mode', 'PUT',
                {'failurePercent': percent, 'delayMs': delay})


try:
    for name, port in [('payment-service', 8081), ('orders-service', 8080)]:
        log = (REPORTS / f'{name}.log').open('w', encoding='utf-8')
        LOGS.append(log)
        process = subprocess.Popen(['java', '-jar', str(ROOT / name / 'target' / f'{name}-1.0.0.jar')],
                                   cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        PROCESSES.append(process)
        ready = False
        for _ in range(120):
            if process.poll() is not None:
                raise RuntimeError(f'{name} exited with {process.returncode}')
            try:
                if http(f'http://localhost:{port}/actuator/health/readiness')['status'] == 'UP':
                    ready = True
                    break
            except Exception:
                pass
            time.sleep(1)
        if not ready:
            raise RuntimeError(f'{name} did not become ready')
    for i in range(10):
        assert order(i)['status'] == 'CONFIRMED', 'Healthy provider did not authorize'
    subprocess.run(['java', 'tools/LabLoad.java', 'check'], cwd=ROOT, check=True)
    subprocess.run(['java', 'tools/LabLoad.java', 'smoke', 'http://localhost:8080', '5'],
                   cwd=ROOT, check=True, timeout=30)
    mode(100)
    for i in range(20):
        assert order(f'fail-{i}')['status'] == 'REJECTED', 'False authorization during outage'
    subprocess.run(['java', 'tools/LabLoad.java', 'outage', 'http://localhost:8080', '5'],
                   cwd=ROOT, check=True, timeout=30)
    mode(0)
    time.sleep(11)
    for i in range(5):
        assert order(f'recover-{i}')['status'] == 'CONFIRMED', 'No recovery after open window'
    mode(0, 1200)
    assert order('slow')['status'] == 'REJECTED', 'Slow dependency did not trigger protection'
    mode(0)
    print('PASS: startup, health, healthy order, Java smoke, controlled outage, recovery, slow dependency.')
finally:
    for process in PROCESSES:
        process.terminate()
    for process in PROCESSES:
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)
    for log in LOGS:
        log.close()
    for file in REPORTS.glob('*-service.log'):
        print(f'--- {file.name} (tail) ---')
        print(file.read_text(encoding='utf-8', errors='replace')[-6000:])
