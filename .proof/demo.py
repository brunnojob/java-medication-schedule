import json
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

def run(argv, expected=0):
    result = subprocess.run(argv, cwd=ROOT, capture_output=True, text=True, timeout=30)
    if result.returncode != expected:
        raise RuntimeError(result.stderr + result.stdout)
    return result.stdout

with tempfile.TemporaryDirectory() as temp:
    path=Path(temp)/'schedule.log'
    argv=['java','MedicationSchedule',str(path)]
    run(argv+['init','America/New_York','02:30'])
    skipped=json.loads(run(argv+['report','2026-03-08']))
    regular=json.loads(run(argv+['report','2026-03-09']))
    assert skipped['doses']==[] and len(regular['doses'])==1
    print(json.dumps({'nonexistent_dst_time':skipped,'regular_day':regular},sort_keys=True))
