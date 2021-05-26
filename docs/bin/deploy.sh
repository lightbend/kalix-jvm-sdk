#!/usr/bin/env bash
#
# Deploy doc sources to another branch.
#
# Note: this script assumes CircleCI is pushing with a deploy key.
#
# Usage:
#   deploy.sh <option>... <dir>...
#
# Options:
#   -m | --module <module>      Name of this antora module (required)
#   -u | --upstream <upstream>  Github upstream as <owner>/<repo> (required)
#   -b | --branch <branch>      Git branch to publish sources (required)

# deploy author

readonly deploy_name="Akka Serverless Bot"
readonly deploy_email="bot@akkaserverless.com"

# locations

readonly docs_dir=$(pwd)
readonly deploy_dir="$docs_dir/.deploy"
readonly base_path=$(git rev-parse --show-prefix 2> /dev/null || echo "")

# echo logs

function red {
  echo -en "\033[0;31m$@\033[0m"
}

function green {
  echo -en "\033[0;32m$@\033[0m"
}

function yellow {
  echo -en "\033[0;33m$@\033[0m"
}

function blue {
  echo -en "\033[0;34m$@\033[0m"
}

function info {
  echo "$@"
}

function error {
  echo $(red "error:" "$@") 1>&2
}

function fail {
  error "$@"
  exit 1
}

# deploy functions

function __deploy_repo_url {
  local repo="$1"
  echo "git@github.com:${repo}.git"
}

function __deploy_repo_dir {
  local repo="$1"
  echo "$deploy_dir/repo/$repo"
}

function __deploy_checkout {
  local repo="$1"
  local branch="$2"
  local repo_url="https://github.com/${repo}.git"
  local dir=$(__deploy_repo_dir "$repo")
  mkdir -p $(dirname "$dir")
  if [ -d "$dir" ]; then
    info "Updating local repository in $dir ..."
    if git -C "$dir" remote | grep upstream > /dev/null ; then
      git -C "$dir" remote set-url upstream $repo_url
    else
      git -C "$dir" remote add upstream $repo_url
    fi
    git -C "$dir" fetch upstream
  else
    info "Cloning $repo repository ..."
    git clone --origin upstream $repo_url "$dir"
  fi
  local local_branch_exists=$(git -C "$dir" rev-parse --verify $branch &> /dev/null; echo $?)
  local remote_branch_exists=$(git -C "$dir" ls-remote --exit-code --heads $repo_url $branch &> /dev/null; echo $?)
  if [ $local_branch_exists -ne 0 ] && [ $remote_branch_exists -ne 0 ] ; then
    info "Creating new $branch branch ..."
    git -C "$dir" checkout --orphan "$branch"
    git -C "$dir" reset --hard
    git -C "$dir" config user.name "$deploy_name"
    git -C "$dir" config user.email "$deploy_email"
    git -C "$dir" commit --allow-empty -m "Create $branch branch"
  else
    info "Checking out $branch branch ..."
    if [ $local_branch_exists -ne 0 ] ; then
      git -C "$dir" checkout -f -t upstream/$branch
    else
      git -C "$dir" checkout -f $branch
      [ $remote_branch_exists -eq 0 ] && git -C "$dir" reset --hard upstream/$branch
    fi
  fi
}

function __deploy_push {
  local repo="$1"
  local branch="$2"
  local message="$3"
  local dir=$(__deploy_repo_dir "$repo")
  local url=$(__deploy_repo_url "$repo")
  git -C "$dir" add --all
  if ! $(git -C "$dir" diff --exit-code --quiet HEAD); then
    git -C "$dir" config user.name "$deploy_name"
    git -C "$dir" config user.email "$deploy_email"
    git -C "$dir" commit -m "$message"
    git -C "$dir" push --quiet "$url" $branch
    git -C "$dir" show --stat-count=10 HEAD
    info $(green "Pushed changes for $repo $branch")
  else
    info $(yellow "No changes to push for $repo $branch")
  fi
}

function __deploy {
  local module
  local upstream
  local branch
  local -a dirs
  while [[ $# -gt 0 ]] ; do
    case "$1" in
      --module | -m ) module="$2" ; shift 2 ;;
      --upstream | -u ) upstream="$2" ; shift 2 ;;
      --branch | -b ) branch="$2" ; shift 2 ;;
      * ) dirs+=("$1") ; shift ;;
    esac
  done

  [ -z "$module" ] && fail "missing required argument: module"
  [ -z "$upstream" ] && fail "missing required argument: upstream"
  [ -z "$branch" ] && fail "missing required argument: branch"
  [ ${#dirs[@]} -eq 0 ] && fail "missing required argument: dirs"

  local commit=$(git log -1 '--format=format:%H')

  info $(blue "Deploying $module documentation to $upstream $branch")

  __deploy_checkout "$upstream" "$branch"
  local upstream_repo_dir=$(__deploy_repo_dir "$upstream")

  info "Syncing docs to $upstream_repo_dir ..."
  local sources_dir="$deploy_dir/sources/$upstream"
  rm -rf "$sources_dir"
  for dir in "${dirs[@]}" ; do
    mkdir -p "$sources_dir/${base_path}${dir}"
    rsync -a "$dir/" "$sources_dir/${base_path}${dir}/"
  done
  rsync -av --delete --exclude='.git/' "$sources_dir/" "$upstream_repo_dir/"

  __deploy_push "$upstream" "$branch" "Update docs @ $commit"
}

# deploy doc sources to another branch

__deploy "$@"
