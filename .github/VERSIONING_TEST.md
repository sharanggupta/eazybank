# Versioning System Test File

This file is used exclusively for testing the semantic versioning system.

## Purpose
- Test version calculation logic in different scenarios
- Validate conventional commit message parsing
- Verify MAJOR/MINOR/PATCH/NONE bump detection
- Ensure git tags are created correctly

Do not commit to production - delete after testing.

---

## Scenario 1: Feature Commit (MINOR Bump)
- File edited: account/.versiontest.txt (triggers deploy-account.yml)
- Commit message: feat: test MINOR version bump
- Expected: Version bumps MINOR, builds, deploys to staging
- Status: About to commit (workflow files were just fixed)
