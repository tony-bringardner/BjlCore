#!/usr/bin/env bash
#
# release.sh - publish the version currently in pom.xml to GitHub Packages
#              and record the release in git.
#
# Works on the project you run it from, so one copy (e.g. in ~/bin) serves every project.
#
# Usage:  release.sh [-y] [-n] [project-dir]
#         release.sh              release the project in the current directory (asks first)
#         release.sh -y           no confirmation prompt
#         release.sh -n           dry run: show what would be done
#         release.sh ~/git/Proj   release the project in that directory
#
# Steps:
#   1. Read the version from pom.xml
#   2. Check: git repo, no unfinished merge, GitHub CLI logged in, GitHub credentials
#      in ~/.m2/settings.xml, tag not already used, git can push to the remote
#   3. mvn clean deploy
#   4. git commit (only if there are uncommitted changes), git tag v<version>,
#      git push the current branch and the tag
#
# Set the new version in pom.xml BEFORE running this script
# (edit <version>, or: mvn versions:set -DnewVersion=0.1.2 -DgenerateBackupPoms=false).
#
# GitHub sign in uses the GitHub CLI (brew install gh). One time setup:
#   gh auth login -s write:packages
# The script then pushes with gh's credentials (no password prompt), and when
# ~/.m2/settings.xml uses ${env.GITHUB_TOKEN} and it is not set, uses gh's token for Maven.
#
set -euo pipefail

REMOTE="origin"
DRY_RUN=false
ASSUME_YES=false

usage() { sed -n '3,28p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

while getopts "ynh" opt; do
	case "$opt" in
		y) ASSUME_YES=true ;;
		n) DRY_RUN=true ;;
		h) usage 0 ;;
		*) usage 1 ;;
	esac
done
shift $((OPTIND - 1))
PROJECT_DIR="${1:-.}"

die()  { echo "ERROR: $*" >&2; exit 1; }
info() { echo "==> $*"; }
run()  {
	if $DRY_RUN; then
		echo "    [dry run] $*"
	else
		"$@"
	fi
}

command -v mvn >/dev/null 2>&1 || die "mvn is not on the PATH (see ~/.zshrc / Homebrew setup)."
command -v git >/dev/null 2>&1 || die "git is not on the PATH."
command -v gh  >/dev/null 2>&1 || die "The GitHub CLI (gh) is not on the PATH. Install it: brew install gh"
gh auth status --hostname github.com >/dev/null 2>&1 \
	|| die "The GitHub CLI is not logged in. Run: gh auth login -s write:packages"

# git push through gh's credentials (for HTTPS remotes), whatever git's own credential
# helper is. The empty helper clears any other helpers (e.g. osxkeychain) for this command.
git_push() {
	if [[ "$(git remote get-url "$REMOTE" 2>/dev/null)" == https://github.com/* ]]; then
		git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push "$@"
	else
		git push "$@"
	fi
}

# Work on the project in PROJECT_DIR (default: the current directory), starting from the
# top of its git repository, so this works from any subfolder of the project.
[[ -d "$PROJECT_DIR" ]] || die "$PROJECT_DIR is not a directory."
cd "$PROJECT_DIR"
ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" \
	|| die "$(pwd) is not in a git repository. Run release.sh from inside a project."
cd "$ROOT"
[[ -f pom.xml ]] || die "pom.xml not found in $ROOT"

# Read a top-level value (groupId, artifactId, version) of THIS project from pom.xml,
# ignoring the same tags inside <parent>, <dependencies>, <build>, etc.
pom_value() {
	local t args=(-e 's/<!--.*-->//g' -e '/<!--/,/-->/d')   # comments (one-line, then multi-line)
	for t in parent dependencies dependencyManagement build profiles reporting \
	         distributionManagement repositories pluginRepositories; do
		# remove the section when it is on one line, otherwise from its start line to its end line
		args+=(-e "s:<$t>.*</$t>::g" -e "/<$t>/,/<\\/$t>/d")
	done
	sed "${args[@]}" pom.xml \
	| sed -n "s:.*<$1>[[:space:]]*\\([^<[:space:]]*\\)[[:space:]]*</$1>.*:\\1:p" \
	| head -1
}

# The same value from the <parent> section (used when the project inherits it).
parent_value() {
	sed -n '/<parent>/,/<\/parent>/p' pom.xml \
	| sed -n "s:.*<$1>[[:space:]]*\([^<[:space:]]*\)[[:space:]]*</$1>.*:\1:p" \
	| head -1
}

# ---------------------------------------------------------------------------
# 1. Version from pom.xml
#    Maven itself is the most reliable parser (handles properties, parents, etc.).
#    If the help plugin can't run, fall back to the first <version> after </parent>
#    or after <artifactId> of this project.
# ---------------------------------------------------------------------------
VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version 2>/dev/null || true)"
if [[ -z "$VERSION" || "$VERSION" == *"ERROR"* || "$VERSION" == *" "* ]]; then
	VERSION="$(pom_value version)"
	[[ -n "$VERSION" ]] || VERSION="$(parent_value version)"
fi
[[ -n "$VERSION" ]] || die "Could not read the project version from pom.xml"

ARTIFACT="$(pom_value artifactId)"
GROUP="$(pom_value groupId)"
[[ -n "$GROUP" ]] || GROUP="$(parent_value groupId)"
TAG="v${VERSION}"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

# ---------------------------------------------------------------------------
# 2. Checks
# ---------------------------------------------------------------------------
[[ "$BRANCH" != "HEAD" ]] || die "Not on a branch (detached HEAD)."
[[ ! -f "$(git rev-parse --git-dir)/MERGE_HEAD" ]] || die "A merge is in progress. Finish it first."

# Deploying needs GitHub credentials for the "github" server id in ~/.m2/settings.xml.
# The token may be written in settings.xml directly or read from an environment
# variable such as ${env.GITHUB_TOKEN}; both are fine.
SETTINGS="${HOME}/.m2/settings.xml"
if [[ ! -f "$SETTINGS" ]] || ! grep -q "<id>[[:space:]]*github[[:space:]]*</id>" "$SETTINGS"; then
	die "No <server> with <id>github</id> in $SETTINGS (Maven needs it to deploy to GitHub Packages)."
fi
if grep -q '\${env\.GITHUB_TOKEN}' "$SETTINGS" && [[ -z "${GITHUB_TOKEN:-}" ]]; then
	# Use the GitHub CLI's token. Publishing packages needs the write:packages scope.
	GITHUB_TOKEN="$(gh auth token --hostname github.com 2>/dev/null)" \
		|| die "Could not get a token from the GitHub CLI. Run: gh auth login -s write:packages"
	export GITHUB_TOKEN
	SCOPES="$(gh api -i user 2>/dev/null | tr -d '\r' | sed -n 's/^[Xx]-[Oo][Aa]uth-[Ss]copes: *//p')"
	if [[ -n "$SCOPES" && "$SCOPES" != *write:packages* ]]; then
		die "The GitHub CLI token can't publish packages (scopes: $SCOPES). Run: gh auth refresh -s write:packages"
	fi
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

# Make sure the push at the end will work BEFORE anything is deployed
# (sign in, access to the repository, and the branch is not behind the remote).
info "Checking that git can push to $REMOTE"
git_push --dry-run --quiet "$REMOTE" "$BRANCH" \
	|| die "git can't push $BRANCH to $REMOTE (see above). Nothing was deployed."

CHANGES="$(git status --porcelain --untracked-files=no)"

echo
echo "  Project  : $ROOT"
echo "  Artifact : ${GROUP:-?}:${ARTIFACT:-?}:${VERSION}"
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
run git_push "$REMOTE" "$BRANCH"

if ! $SNAPSHOT; then
	info "git push $REMOTE $TAG"
	run git_push "$REMOTE" "$TAG"
fi

echo
if $DRY_RUN; then
	info "Dry run complete. Nothing was deployed or pushed."
else
	info "Released ${ARTIFACT:-project} $VERSION"
fi
