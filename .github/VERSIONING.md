# Service Versioning Strategy

Clear, predictable semantic versioning for EazyBank microservices.

---

## Overview

Each microservice maintains **independent semantic versions** tracked via **git tags**.

### Version Format

```
{service}-v{major}.{minor}.{patch}

Examples:
account-v0.1.2
card-v1.0.0
gateway-v0.2.3
loan-v0.0.5
```

### How It Works

```
1. Developer commits code with conventional commit message
                    ↓
2. Push to main branch
                    ↓
3. GitHub Actions detects service changed
                    ↓
4. Workflow calculates new version based on commit message
   - "feat:" → MINOR bump (0.1.0)
   - "fix:" → PATCH bump (0.0.1)
   - "BREAKING CHANGE:" → MAJOR bump (1.0.0)
   - No prefix → PATCH bump (default)
                    ↓
5. Builds Docker image with new version tag
                    ↓
6. Deploys to staging, then production (with approval)
                    ↓
7. Creates git tag: {service}-v{version}
   (Used as source of truth for future builds)
```

---

## Conventional Commits (How to Write Commit Messages)

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

| Type | Version Impact | Use When |
|------|----------------|----------|
| `feat` | MINOR (0.1.0) | Adding new feature/endpoint |
| `fix` | PATCH (0.0.1) | Fixing a bug |
| `docs` | NONE | Updating documentation |
| `test` | NONE | Adding/modifying tests |
| `chore` | NONE | Dependency updates, build config |
| `refactor` | PATCH (0.0.1) | Code restructuring (no new features) |
| `perf` | PATCH (0.0.1) | Performance improvements |
| `ci` | NONE | CI/CD changes |
| `style` | NONE | Code style (formatting, linting) |

### BREAKING CHANGE

For major version bumps, add anywhere in commit message or footer:

```
BREAKING CHANGE: description of what broke
```

or in footer:

```
BREAKING CHANGE: API endpoint /account/fetch changed to /account/api/fetch
```

---

## Examples

### Example 1: Simple Bug Fix

```bash
git commit -m "fix: resolve null pointer in card validation"
```

**Result**:
```
Previous version: 0.1.2
New version: 0.1.3 (PATCH bump)
Git tag: card-v0.1.3
Docker image: ghcr.io/.../card:card-v0.1.3
```

### Example 2: New Feature

```bash
git commit -m "feat: add card limit increase endpoint"
```

**Result**:
```
Previous version: 0.1.2
New version: 0.2.0 (MINOR bump)
Git tag: card-v0.2.0
Docker image: ghcr.io/.../card:card-v0.2.0
```

### Example 3: Breaking Change

```bash
git commit -m "refactor: change account API response format

BREAKING CHANGE: API now returns ISO 8601 timestamps instead of Unix epoch"
```

**Result**:
```
Previous version: 0.2.1
New version: 1.0.0 (MAJOR bump)
Git tag: account-v1.0.0
Docker image: ghcr.io/.../account:account-v1.0.0
```

### Example 4: Documentation Only

```bash
git commit -m "docs: add API usage examples to README"
```

**Result**:
```
NO VERSION BUMP
NO NEW IMAGE BUILT
Workflow skips build/deploy
```

### Example 5: Multiple Services Changed

```bash
# Changed files: customergateway/src/..., account/src/..., docs/...

git add customergateway/src account/src docs/
git commit -m "feat: add authentication middleware to gateway

- Implement JWT validation in WriteGateInterceptor
- Update account service API for auth token generation
- Add security documentation"
```

**Result**:
```
gateway:  0.1.0 → 0.2.0 (feature → MINOR bump)
account:  0.0.5 → 0.0.6 (implicit in message → PATCH bump)
docs:     no bump
```

Each service builds independently, each version bumps appropriately.

---

## Workflow Decision Tree

```
┌─ Commit pushed to main
│
├─ Is it a documentation-only change (docs/**, README, etc)?
│  ├─ YES → Skip build/deploy
│  └─ NO → Continue
│
├─ Did {service}/** change?
│  ├─ NO → Don't rebuild this service
│  └─ YES → Continue
│
├─ Parse commit message
│  ├─ Contains "BREAKING CHANGE:" → MAJOR bump (x.0.0)
│  ├─ Starts with "feat:" → MINOR bump (0.x.0)
│  ├─ Starts with "fix:" or "refactor:" or "perf:" → PATCH bump (0.0.x)
│  └─ Default (no recognized prefix) → PATCH bump (0.0.x)
│
├─ Calculate new version
│  └─ Get latest git tag for service
│     └─ Apply bump logic
│
├─ Build Docker image
│  └─ Tag: ghcr.io/.../service:service-v{version}
│
├─ Deploy to staging
│  └─ Auto-deploy with new image
│
├─ Wait for approval
│  └─ Manual approval required
│
├─ Deploy to production
│  └─ Deploy with approved image
│
└─ Create git tag
   └─ service-v{version}
      (Pushed to repo for future reference)
```

---

## Checking Versions

### View Latest Version for a Service

```bash
# Get latest git tag for account service
git describe --tags --match "account-v*" --abbrev=0

# Output: account-v0.1.2
```

### View All Versions for a Service

```bash
# List all tags for account service
git tag -l "account-v*"

# Output:
# account-v0.0.1
# account-v0.0.2
# account-v0.1.0
# account-v0.1.1
# account-v0.1.2
```

### View All Versions (All Services)

```bash
# Sort by version
git tag -l "*-v*" | sort -V

# Output:
# account-v0.0.1
# account-v0.0.2
# account-v0.1.2
# card-v0.0.1
# card-v0.1.0
# customergateway-v0.0.5
# loan-v0.0.1
```

### View Commits Since Last Release

```bash
# For account service
LAST_TAG=$(git describe --tags --match "account-v*" --abbrev=0)
git log ${LAST_TAG}..HEAD --oneline -- account/

# Output:
# a1b2c3d fix: resolve validation bug
# d4e5f6g feat: add new endpoint
```

---

## Common Patterns

### Single Service Change

```bash
# Only modify account service
cd account/src
# ... make changes ...

git add account/
git commit -m "feat: add account status endpoint"
git push origin main

# Result: account version bumps, other services unaffected
```

### Multiple Services, Same Feature

```bash
# Feature touches account and card services
# Account: adds export endpoint
# Card: adds export-compatible format

git add account/ card/
git commit -m "feat: add customer data export

- Account exports account details in standard format
- Card exports card details in standard format
- Both use new ExportDto"

# Result: BOTH account and card get MINOR bump
```

### Shared Dependency Update

```bash
# Update Spring Boot version for all services
cd pom.xml  # Parent pom

git add pom.xml account/ card/ loan/ customergateway/
git commit -m "chore: upgrade Spring Boot to 4.1.0

Updated all services to Spring Boot 4.1.0 for security patches"

# Result: NO version bumps (chore type)
# But services won't rebuild anyway (only chore files touched)
```

---

## Troubleshooting

### Version Didn't Bump

**Problem**: Pushed code but Docker image version stayed same

**Cause**:
1. Commit message doesn't start with `feat:`, `fix:`, `refactor:`, `perf:`
2. Only documentation changed (docs, test, ci, chore)
3. Wrong service directory changed

**Solution**:
1. Check commit message format:
   ```bash
   git log -1 --format=%B

   # Should show:
   # feat: description
   # OR
   # fix: description
   # OR
   # BREAKING CHANGE: in the message
   ```

2. Check if service files actually changed:
   ```bash
   # See what changed in last commit
   git diff HEAD~1 --name-only

   # Should include: account/**, card/**, loan/**, or customergateway/**
   ```

3. If message was wrong, amend:
   ```bash
   git commit --amend -m "feat: correct description of change"
   git push --force-with-lease origin main
   ```

### Multiple Commits Before Merge

If you have multiple commits before deployment:

```bash
git log -5 --oneline account/

# a1b2c3d feat: add endpoint A
# d4e5f6g fix: resolve bug in endpoint A
# g7h8i9j docs: update README
# j0k1l2m fix: resolve bug in B
# n3o4p5q feat: add endpoint B
```

**Version calculation**:
- `feat:` appears twice → MINOR bump
- Workflow uses the **highest bump level** needed
- Result: MINOR bump (not cumulative)

```
Previous: 0.1.0
New: 0.2.0 (MINOR, because at least one "feat:" exists)
```

### Deployment Status

Check GitHub Actions UI to see:
- Was build triggered?
- Which service version?
- Deployed to which environments?

```
https://github.com/sharanggupta/eazybank/actions
→ Click "Deploy Account Service" (or your service)
→ View latest workflow run
```

---

## Best Practices

### 1. Descriptive Commit Messages

```bash
# Good ✓
git commit -m "feat: add JWT authentication to gateway

Implement JWT validation in WriteGateInterceptor to protect backend services.
Tokens issued by account service, verified before routing requests."

# Bad ✗
git commit -m "add auth"
```

### 2. Atomic Commits

Each commit should represent one logical change:

```bash
# Good ✓ - One feature per commit
git commit -m "feat: add card limit increase endpoint"
git commit -m "feat: add card limit decrease endpoint"

# Bad ✗ - Multiple features in one commit
git commit -m "feat: add card endpoints and update database schema"
```

### 3. Use Scope (Optional)

Include what you changed in parentheses:

```bash
git commit -m "fix(card): resolve NPE in limit validation"
git commit -m "feat(account): add export API"
git commit -m "chore(deps): upgrade Spring Boot"
```

Scope doesn't affect versioning, just readability.

### 4. Link to Issues

```bash
git commit -m "fix: resolve null pointer in validation

Fixes #123"
```

### 5. Test Before Committing

```bash
cd account && ./mvnw clean test
# Ensure tests pass, THEN commit
```

---

## Version Release Checklist

Before releasing a major version (1.0.0), ensure:

- [ ] All tests pass locally and in CI
- [ ] Staging deployment successful
- [ ] Manual testing in staging environment
- [ ] API contract changes documented
- [ ] Breaking changes highlighted in commit message
- [ ] Production approval granted
- [ ] Post-deployment monitoring setup

---

## Migration Guide (From Current Setup)

If you're switching from the old auto-semver:

**Before**:
- All services bumped together
- Version based on workspace, not per-service

**After**:
- Each service independent
- Version based on git tags
- Only builds/deploys changed services

**No action needed**:
- Existing Docker images keep their tags
- Start using new format for new commits
- Old `deploy.yml` still works for full-stack deployments

---

## Questions?

Refer to:
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Semantic Versioning](https://semver.org/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

Contact the team or open an issue if versioning doesn't work as expected.
