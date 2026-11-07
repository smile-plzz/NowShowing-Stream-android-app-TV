import re
import unittest
from pathlib import Path
from package_release import read_version


class VersionTruthTest(unittest.TestCase):
    def setUp(self):
        self.repo_root = Path(__file__).resolve().parent.parent
        self.build_gradle = self.repo_root / "app" / "build.gradle.kts"
        self.readme = self.repo_root / "README.md"
        self.docs_dir = self.repo_root / "docs"

    def test_gradle_version_extracted(self):
        text = self.build_gradle.read_text(encoding="utf-8")
        version_name, version_code = read_version(text)
        self.assertTrue(version_name.startswith("1.6.0"))
        self.assertGreaterEqual(version_code, 16004)

    def test_readme_matches_gradle_version(self):
        gradle_text = self.build_gradle.read_text(encoding="utf-8")
        version_name, version_code = read_version(gradle_text)

        readme_text = self.readme.read_text(encoding="utf-8")
        self.assertIn(version_name, readme_text, f"README.md does not contain versionName {version_name}")
        self.assertIn(str(version_code), readme_text, f"README.md does not contain versionCode {version_code}")

    def test_release_notes_exist_for_version(self):
        gradle_text = self.build_gradle.read_text(encoding="utf-8")
        version_name, _ = read_version(gradle_text)

        # Check for release notes matching version (e.g., docs/releases/v1.6.0-rc.5.md)
        release_notes = self.docs_dir / "releases" / f"v{version_name}.md"
        self.assertTrue(release_notes.exists(), f"Release notes file missing: {release_notes}")

    def test_canonical_repo_url(self):
        # Verify canonical URL smile-plzz/NowShowing-Stream-Android-TV
        readme_text = self.readme.read_text(encoding="utf-8")
        self.assertNotIn("NowShowing-Stream-android-app-TV", readme_text)
        self.assertIn("https://github.com/smile-plzz/NowShowing-Stream-Android-TV", readme_text)


if __name__ == "__main__":
    unittest.main()

