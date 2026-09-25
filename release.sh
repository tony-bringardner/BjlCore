#!/usr/bin/env bash
#
# release.sh - publish the version currently in pom.xml to GitHub Packages
#              and record the release in git.
#
# Usage:  ./release.sh            (asks for confirmation)
#         ./release.sh -y         (no confirmation prompt)
#         ./release.sh -n         (dry run: show what would be done)
#
# Steps:
#   1. Read the version from pom.xml
#   2. Check: git repo, no unfinished merge, GITHUB_TOKEN set, tag not already used
#   3. mvn clean deploy
#   4. git commit (only if there are uncommitted changes), git tag v<version>,
#      git push the current branch and the tag
#
# Set the new version in pom.xml BEFORE running this script
# (edit <version>, or: mvn versions:set -DnewVersion=0.1.2 -DgenerateBackupPoms=false).
#
set -euo pipefail

REMOTE="origin"
DRY_RUN=false
ASSUME_YES=false

usage() { sed -n '3,20p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

while getopts "ynh" opt; do
	case "$opt" in
		y) ASSUME_YES=true ;;
		n) DRY_RUN=true ;;
		h) usage 0 ;;
		*) usage 1 ;;
	esac
done

die()  { echo "ERROR: $*" >&2; exit 1; }
info() { echo "==> $*"; }
run()  {
	if $DRY_RUN; then
		echo "    [dry run] $*"
	else
		"$@"
	fi
}

# Always run from the directory that holds this script (the project root)
cd "$(dirname "$0")"

command -v mvn >/dev/null 2>&1 || die "mvn is not on the PATH (see ~/.zshrc / Homebrew setup)."
command -v git >/dev/null 2>&1 || die "git is not on the PATH."
[[ -f pom.xml ]] || die "pom.xml not found in $(pwd)"
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "$(pwd) is not a git repository."

# ---------------------------------------------------------------------------
# 1. Version from pom.xml
#    Maven itself is the most reliable parser (handles properties, parents, etc.).
#    If the help plugin can't run, fall back to the first <version> after </parent>
#    or after <artifactId> of this project.
# ---------------------------------------------------------------------------
VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version 2>/dev/null || true)"
if [[ -z "$VERSION" || "$VERSION" == *"ERROR"* || "$VERSION" == *" "* ]]; then
	VERSION="$(sed -e '/<parent>/,/<\/parent>/d' pom.xml \
		| sed -n 's:.*<version>[[:space:]]*\([^<[:space:]]*\)[[:space:]]*</version>.*:\1:p' \
		| head -1)"
fi
[[ -n "$VERSION" ]] || die "Could not read the project version from pom.xml"

ARTIFACT="$(sed -e '/<parent>/,/<\/parent>/d' pom.xml \
	| sed -n 's:.*<artifactId>[[:space:]]*\([^<[:space:]]*\)[[:space:]]*</artifactId>.*:\1:p' \
	| head -1)"
TAG="v${VERSION}"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

# ---------------------------------------------------------------------------
# 2. Checks
# ---------------------------------------------------------------------------
[[ "$BRANCH" != "HEAD" ]] || die "Not on a branch (detached HEAD)."
[[ ! -f "$(git rev-parse --git-dir)/MERGE_HEAD" ]] || die "A merge is in progress. Finish it first."

if [[ -z "${GITHUB_TOKEN:-}" ]]; then
	die "GITHUB_TOKEN is not set (needed by ~/.m2/settings.xml to deploy to GitHub Packages)."
fi

SNAPSHOT=false
[[ "$VERSION" == *-SNAPSHOT ]] && SNAPSHOT=true

if ! $SNAPSHOT; then
	if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
		die "Tag $TAG already exists. Change the version in pom.xml first (GitHub rejects re-publishing a version)."
	fi
	if git ls-remote --exit-code --tags "$REMOTE" "refs/tags/$TAG" >/dev/null 2>&1; then
		die "Tag $TAG already exists on $REMOTE. Change the version in pom.xml first."
	fi
fi

CHANGES="$(git status --porcelain --untracked-files=no)"

echo
echo "  Artifact : us.bringardner:${ARTIFACT:-?}:${VERSION}"
echo "  Branch   : $BRANCH  ->  $REMOTE"
if $SNAPSHOT; then
	echo "  Tag      : (none - SNAPSHOT versions are not tagged)"
else
	echo "  Tag      : $TAG"
fi
if [[ -n "$CHANGES" ]]; then
	echo "  Commit   : these uncommitted changes will be committed as \"Release $VERSION\":"
	echo "$CHANGES" | sed 's/^/             /'
else
	echo "  Commit   : nothing to commit (working tree clean)"
fi
UNTRACKED="$(git ls-files --others --exclude-standard)"
if [[ -n "$UNTRACKED" ]]; then
	echo "  Note     : untracked files are NOT included:"
	echo "$UNTRACKED" | head -10 | sed 's/^/             /'
fi
echo

if ! $ASSUME_YES && ! $DRY_RUN; then
	read -r -p "Publish $VERSION? [y/N] " answer
	[[ "$answer" =~ ^[Yy]$ ]] || { echo "Cancelled."; exit 1; }
fi

# ---------------------------------------------------------------------------
# 3. Build, test and deploy (stops here if anything fails)
# ---------------------------------------------------------------------------
info "mvn clean deploy"
run mvn clean deploy

# ---------------------------------------------------------------------------
# 4. Git: commit, tag, push
# ---------------------------------------------------------------------------
if [[ -n "$CHANGES" ]]; then
	info "git commit"
	run git commit -a -m "Release $VERSION"
fi

if ! $SNAPSHOT; then
	info "git tag $TAG"
	run git tag -a "$TAG" -m "Release $VERSION"
fi

info "git push $REMOTE $BRANCH"
run git push "$REMOTE" "$BRANCH"

if ! $SNAPSHOT; then
	info "git push $REMOTE $TAG"
	run git push "$REMOTE" "$TAG"
fi

echo
if $DRY_RUN; then
	info "Dry run complete. Nothing was deployed or pushed."
else
	info "Released ${ARTIFACT:-project} $VERSION"
fi
