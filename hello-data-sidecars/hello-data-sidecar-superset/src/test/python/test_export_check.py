import os
import subprocess
import sys
import tempfile
import unittest
import zipfile

SCRIPT = os.path.join(os.path.dirname(__file__), '..', '..', 'main', 'python', 'export_check.py')
ROOT = 'dashboard_export_20261124T092642/'

METADATA = 'version: 1.0.0\ntype: Dashboard\ntimestamp: 2026-11-24T09:26:42\n'
DASHBOARD = 'dashboard_title: Nothing\nuuid: 11111111-1111-1111-1111-111111111111\n'
CHART = 'slice_name: c\nuuid: 22222222-2222-2222-2222-222222222222\ndataset_uuid: 33333333-3333-3333-3333-333333333333\n'
DATASET = 'table_name: t\nuuid: 33333333-3333-3333-3333-333333333333\n'
DATABASE = 'database_name: db\nuuid: 44444444-4444-4444-4444-444444444444\n'


def run_check(entries, zip_prefix='upload'):
    with tempfile.TemporaryDirectory() as tmp:
        zip_path = os.path.join(tmp, zip_prefix + '.zip')
        with zipfile.ZipFile(zip_path, 'w') as zf:
            for name, content in entries.items():
                zf.writestr(ROOT + name, content)
        return subprocess.run([sys.executable, SCRIPT, '-i', zip_path], capture_output=True, text=True)


class ExportCheckTest(unittest.TestCase):

    def test_empty_dashboard_is_rejected_with_a_clear_message(self):
        result = run_check({'metadata.yaml': METADATA, 'dashboards/Nothing_3.yaml': DASHBOARD})

        self.assertEqual(1, result.returncode)
        self.assertIn('The dashboard contains no charts', result.stdout)
        self.assertNotIn('is missing in export zip file', result.stdout)

    def test_complete_dashboard_export_passes(self):
        result = run_check({'metadata.yaml': METADATA,
                            'dashboards/D_5.yaml': DASHBOARD,
                            'charts/c_1.yaml': CHART,
                            'datasets/db/t.yaml': DATASET,
                            'databases/db.yaml': DATABASE})

        self.assertEqual(0, result.returncode, result.stdout)

    def test_dashboard_with_charts_but_missing_datasets_reports_the_missing_folders(self):
        result = run_check({'metadata.yaml': METADATA,
                            'dashboards/D_5.yaml': DASHBOARD,
                            'charts/c_1.yaml': CHART})

        self.assertEqual(1, result.returncode)
        self.assertIn('[ databases ] is missing in export zip file', result.stdout)
        self.assertIn('[ datasets ] is missing in export zip file', result.stdout)
        self.assertNotIn('The dashboard contains no charts', result.stdout)

    def test_zip_name_containing_charts_does_not_hide_an_empty_dashboard(self):
        result = run_check({'metadata.yaml': METADATA, 'dashboards/Nothing_3.yaml': DASHBOARD}, zip_prefix='charts')

        self.assertEqual(1, result.returncode)
        self.assertIn('The dashboard contains no charts', result.stdout)


if __name__ == '__main__':
    unittest.main()
