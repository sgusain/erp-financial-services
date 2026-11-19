# 14. Pull request review agent in CI (Course 2 sections 11, 12, 13)

## Goal
An AI reviewer that comments on every pull request, running in GitHub Actions next to the existing `ci.yml`.

## Steps
1. New workflow `.github/workflows/ai-pr-review.yml` on `pull_request` (opened, synchronize). Do not change `ci.yml`.
2. Only run for PRs from branches in this repo (not forks), because forks cannot read secrets. Skip when the PR has the label `no-ai-review`.
3. Get the diff with `git diff origin/main...HEAD`. Limit to changed `*.java`, `*.yml`, `*.sql` files. Cap the size (for example 60 KB); if larger, review only the first files and say so in the comment.
4. Call Claude with a review prompt kept in `.github/ai-review-prompt.md`: look for bugs, security problems (auth, SQL, secrets, logging of personal data), missing validation and missing tests; ignore style; max 10 findings; each finding has file, line, severity and a short fix; if nothing is wrong say so. Use either the official Claude Code GitHub Action or a small script calling the API. Verify the current action name and a pinned version (use a commit SHA) before using it. Do not guess.
5. Post one summary comment on the PR (update the same comment on new pushes, do not spam).
6. Secrets: `ANTHROPIC_API_KEY` in GitHub repository secrets only. Set minimal `permissions` (`contents: read`, `pull-requests: write`). Set `timeout-minutes` and a max token limit.
7. The AI review never blocks merge. It is advice only.

## Done when
- A test PR with a deliberate bug (for example SQL string concatenation) gets a useful comment.
- A second push updates the same comment.
- A fork PR or a labeled PR skips cleanly.
- Workflow has no hardcoded secrets and minimal permissions.

## Interview notes
- "I would run an AI reviewer on every pull request through GitHub Actions. It catches simple bugs and security issues early, but a human still approves the merge. I keep the key in secrets, limit the diff size and cap the cost."
