#!/usr/bin/env bash
set -e

CURRENT_DIR=$( pwd )
cd "$( dirname "${BASH_SOURCE[0]}" )"

VERSION=$(grep -oP "<version>\K(.*?)(?=(-SNAPSHOT)?</version>)" pom.xml| head -n 1)

read -p "Enter release version (${VERSION}):" TMP
if [ ! -z "${TMP}" ]; then
  VERSION="${TMP}"
fi

TAG="${VERSION}"

TMP=$(($(echo ${VERSION} | rev | cut -d'.' -f 1 | rev) + 1))
NEW_DEV_VERSION=$(echo "$(echo ${VERSION} | rev | cut -d'.' -f 2- | rev)".${TMP}-SNAPSHOT)

read -p "Enter new development version (${NEW_DEV_VERSION}):" TMP
if [ ! -z "${TMP}" ]; then
  NEW_DEV_VERSION="${TMP}"
fi

echo "Version: ${VERSION}"
echo "Tag: ${TAG}"
echo "New development version: ${NEW_DEV_VERSION}"

remotes=( $(git remote) )
if [[ ${#remotes[@]} != 1 ]]; then
    echo "Which remote you want to use?"
    select remote_name in ${remotes[@]}; do
        if [[ ! -z ${remote_name} ]]; then
            break
        fi
    done
fi

mvn -B versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false
current_hash="$( git rev-parse HEAD )"
git commit -am "Version ${VERSION}"
git tag -a ${TAG} -m "Version ${VERSION}"
git push ${remote_name} ${TAG}
git reset --hard "${current_hash}"

mvn -B versions:set -DnewVersion="${NEW_DEV_VERSION}" -DgenerateBackupPoms=false
git commit -am "New development version: ${NEW_DEV_VERSION}"

push_changes() {
    branch_name=$( git branch | grep '*' | cut -d' ' -f 2 )
    read -p "Which branch to use (default: ${branch_name}): " TMP
    if [ ! -z "${TMP}" ]; then
        branch_name="${TMP}"
    fi
    while true; do
        git push ${remote_name} ${branch_name}
        if [[ $? == 0 ]]; then
            break
        fi
    done
}

echo "Do you want to push development version change to remote repo?"
select TMP in "Yes" "No"; do
    case "${TMP}" in
        "Yes") push_changes; break;;
        "No") break;;
    esac
done

cd "${CURRENT_DIR}"
