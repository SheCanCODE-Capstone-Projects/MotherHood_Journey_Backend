#!/bin/sh

export FILTER_BRANCH_SQUELCH_WARNING=1
git filter-branch --force --env-filter '
CORRECT_NAME="Pichry"
CORRECT_EMAIL="sabatoclesence@gmail.com"
export GIT_COMMITTER_NAME="$CORRECT_NAME"
export GIT_COMMITTER_EMAIL="$CORRECT_EMAIL"
export GIT_AUTHOR_NAME="$CORRECT_NAME"
export GIT_AUTHOR_EMAIL="$CORRECT_EMAIL"
' --msg-filter '
sed "/Co-authored-by: Junie <junie@jetbrains.com>/d"
' --tag-name-filter cat -- --all
