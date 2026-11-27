#!/bin/bash
# Updates the project to the latest release of the asciidoctor-pdf gem.
#
# Usage: upgrade-asciidoctor-pdf.sh PR_BODY_FILE
#
# If a newer release exists, the script changes the version files and writes
# the pull request description to PR_BODY_FILE.
# In GitHub Actions, it also sets the 'version' step output.
#
# Requires curl and jq.
set -euo pipefail
# Without this, 'set -e' does not apply in $(...)
shopt -s inherit_errexit

GEM_NAME=asciidoctor-pdf
ROOT_DIR=$(cd "$(dirname "$0")/../.." && pwd)
RUBYGEMS_URL=https://rubygems.org/api

GEM_VERSION_FILE=build.gradle
# The first group of each pattern is the text before the version
GEM_VERSION_PATTERN="^([[:space:]]*asciidoctorPdfGemVersion[[:space:]]*=.*:[[:space:]]*')[^'[:space:]]+"
PROJECT_VERSION_PATTERN='^(version=)[^[:space:]]+'

VERSION_FILES=("$GEM_VERSION_FILE" gradle.properties asciidoctorj-pdf/gradle.properties)

fail() {
  echo "$1" >&2
  exit 1
}

pattern_for() {
  if [ "$1" = "$GEM_VERSION_FILE" ]; then
    echo "$GEM_VERSION_PATTERN"
  else
    echo "$PROJECT_VERSION_PATTERN"
  fi
}

fetch() {
  curl --fail --silent --show-error --location --retry 3 "$RUBYGEMS_URL$1"
}

current_version() {
  local version
  version=$(grep -oE "$GEM_VERSION_PATTERN" "$ROOT_DIR/$GEM_VERSION_FILE" | sed "s/.*'//" || true)
  [ -n "$version" ] || fail "Cannot find the gem version in $GEM_VERSION_FILE"
  echo "$version"
}

latest_version() {
  local json version
  json=$(fetch "/v1/versions/$GEM_NAME/latest.json")
  version=$(jq -er '.version' <<<"$json")
  # Only numeric versions are valid. The value goes into files and shell commands.
  [[ $version =~ ^[0-9]+(\.[0-9]+)+$ ]] || fail "Unexpected version from RubyGems: $version"
  echo "$version"
}

# Succeeds if the first version is newer than the second one
is_newer() {
  [ "$1" != "$2" ] && [ "$(printf '%s\n' "$1" "$2" | sort -V | tail -n 1)" = "$1" ]
}

# Prints a JSON object with the name and requirements of each runtime dependency
runtime_dependencies() {
  local json
  json=$(fetch "/v2/rubygems/$GEM_NAME/versions/$1.json")
  jq -ce '.dependencies.runtime | map({(.name): .requirements}) | add // {}' <<<"$json"
}

dependency_changes() {
  jq -rn --argjson old "$1" --argjson new "$2" '
    ($old + $new | keys[]) as $name
    | $old[$name] as $before
    | $new[$name] as $after
    | select($before != $after)
    | if $before == null then "* Added `\($name)` (`\($after)`)"
      elif $after == null then "* Removed `\($name)` (`\($before)`)"
      else "* Changed `\($name)` from `\($before)` to `\($after)`"
      end'
}

# Changes all the files, or none of them if one file does not have the expected content
update_files() {
  local version=$1 file count
  for file in "${VERSION_FILES[@]}"; do
    count=$(grep -cE "$(pattern_for "$file")" "$ROOT_DIR/$file" || true)
    [ "$count" = 1 ] || fail "Expected 1 version in $file, found $count"
  done
  for file in "${VERSION_FILES[@]}"; do
    sed -i -E "s/$(pattern_for "$file")/\1$version/" "$ROOT_DIR/$file"
  done
}

pull_request_body() {
  local current=$1 latest=$2 old_dependencies new_dependencies changes
  old_dependencies=$(runtime_dependencies "$current")
  new_dependencies=$(runtime_dependencies "$latest")
  changes=$(dependency_changes "$old_dependencies" "$new_dependencies")

  echo "Upgrade Asciidoctor PDF from $current to $latest."
  echo
  echo "Release: https://rubygems.org/gems/$GEM_NAME/versions/$latest"
  echo "Changes: https://github.com/asciidoctor/$GEM_NAME/compare/v$current...v$latest"
  echo
  echo '## Runtime dependencies'
  echo
  if [ -z "$changes" ]; then
    echo 'The runtime dependencies of the gem did not change.'
  else
    echo '**WARNING:** The runtime dependencies of the gem changed.'
    echo 'Before you merge, examine the gem versions in `build.gradle` and the exclusions in `asciidoctorj-pdf/build.gradle`.'
    echo
    echo "$changes"
  fi
}

set_step_output() {
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    echo "$1=$2" >>"$GITHUB_OUTPUT"
  fi
}

upgrade() {
  local current=$1 latest=$2 body_file=$3 body
  # Get all remote data before the files change
  body=$(pull_request_body "$current" "$latest")
  update_files "$latest"
  echo "$body" >"$body_file"
}

main() {
  local body_file=${1:?Usage: $0 PR_BODY_FILE}
  local current latest
  current=$(current_version)
  latest=$(latest_version)

  if ! is_newer "$latest" "$current"; then
    echo 'Already up to date'
    return
  fi

  upgrade "$current" "$latest" "$body_file"
  set_step_output version "$latest"
  echo "Updated to $latest"
}

main "$@"
