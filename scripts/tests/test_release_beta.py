from __future__ import annotations

import importlib.util
import pathlib
import unittest


SCRIPT_PATH = pathlib.Path(__file__).resolve().parents[1] / "release_beta.py"
SPEC = importlib.util.spec_from_file_location("release_beta", SCRIPT_PATH)
assert SPEC is not None and SPEC.loader is not None
release_beta = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release_beta)


class EnsureReleaseBranchTest(unittest.TestCase):
    def test_allows_main(self) -> None:
        release_beta.ensure_release_branch("main")

    def test_rejects_non_main(self) -> None:
        with self.assertRaises(SystemExit) as context:
            release_beta.ensure_release_branch("feature/release-test")

        self.assertIn("main branch", str(context.exception))


class EnsureReleaseTagChannelTest(unittest.TestCase):
    def test_allows_main_channel_tag(self) -> None:
        release_beta.ensure_release_tag_channel("v0.1.1-beta.20260502-1-main")

    def test_rejects_tag_without_main_channel_marker(self) -> None:
        with self.assertRaises(SystemExit) as context:
            release_beta.ensure_release_tag_channel("v0.1.1-beta.20260502-1")

        self.assertIn("main channel marker", str(context.exception))


if __name__ == "__main__":
    unittest.main()
