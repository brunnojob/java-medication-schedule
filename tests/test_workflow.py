import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


class WorkflowTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        home = os.environ.get("JAVA_HOME")
        self.java = str(Path(home) / "bin/java") if home else "java"
        compiler = str(Path(home) / "bin/javac") if home else "javac"
        subprocess.run(
            [
                compiler,
                "-Xlint:all",
                "-Werror",
                "-d",
                str(self.directory),
                str(Path(__file__).resolve().parents[1] / "MedicationSchedule.java"),
            ],
            check=True,
        )

    def raw(self, *arguments):
        return subprocess.run(
            [
                self.java,
                "-cp",
                str(self.directory),
                "MedicationSchedule",
                *map(str, arguments),
            ],
            capture_output=True,
            text=True,
        )

    def command(self, *arguments):
        result = self.raw(*arguments)
        self.assertEqual(result.returncode, 0, result.stderr)
        return json.loads(result.stdout) if result.stdout.strip() else None

    def test_persistent_workflow(self):
        file = self.directory / "schedule.log"
        self.command(file, "init", "America/New_York", "02:30")
        self.assertEqual(self.command(file, "report", "2026-03-08")["doses"], [])
        self.assertEqual(len(self.command(file, "report", "2026-03-09")["doses"]), 1)
