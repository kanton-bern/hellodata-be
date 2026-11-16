#!/usr/bin/env bash
# Changes the gh-pages branch, which GitHub Pages serves as https://kanton-bern.github.io/hellodata-be/.
# Two workflows write to it: deploy_docs.yml replaces the docs and keeps e2e/, e2e.yml changes only e2e/.
#
# .github/scripts/update-gh-pages.sh "<commit message>" <command> [args...]
#
# The command runs in a checkout of gh-pages (its working directory), so it gets absolute paths as arguments. The result
# is pushed as a single commit without parent, as mkdocs gh-deploy did before: the branch keeps no history, so old
# reports with their traces and videos do not pile up in every clone of the repository. When the other workflow pushed
# in the meantime, the lease fails and the command runs again on top of the newer state - so it must be repeatable.
set -euo pipefail

message=$1
shift
branch=gh-pages
export GIT_AUTHOR_NAME=${GIT_AUTHOR_NAME:-github-actions[bot]}
export GIT_AUTHOR_EMAIL=${GIT_AUTHOR_EMAIL:-41898282+github-actions[bot]@users.noreply.github.com}
export GIT_COMMITTER_NAME=$GIT_AUTHOR_NAME GIT_COMMITTER_EMAIL=$GIT_AUTHOR_EMAIL
dir=$(mktemp -d)

for attempt in 1 2 3 4 5; do
  git fetch --quiet --depth 1 origin "$branch"
  lease=$(git rev-parse FETCH_HEAD)
  git worktree add --quiet --detach --force "$dir" "$lease"
  (cd "$dir" && "$@")
  git -C "$dir" add --all
  commit=$(git -C "$dir" commit-tree "$(git -C "$dir" write-tree)" -m "$message")
  if git push --quiet --force-with-lease="$branch:$lease" origin "$commit:refs/heads/$branch"; then
    git worktree remove --force "$dir"
    echo "pushed $branch $commit"
    exit 0
  fi
  echo "push of $branch rejected (attempt $attempt), running again on top of the newer state"
  git worktree remove --force "$dir"
done
exit 1
