@ECHO OFF
SETLOCAL

set in=%6
set from=%2
set to=%4

cd %in%
FOR /F "tokens=1 delims=" %%A in ('git rev-parse --abbrev-ref HEAD') do SET cb=%%A
echo %cb%
git stash
git fetch
git checkout %to%
git pull
git merge -m "Merge branch %from% into %to%" origin/%from%
git mergetool
git commit --no-edit
git push
git checkout %cb%
git stash pop
