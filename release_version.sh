#!/usr/bin/env bash
set -e

LOGS_FILE="$( mktemp )"
echo "Logs will be written to ${LOGS_FILE}"

json_escape_file () {
    python -c "
import json, sys
with open('${1}', 'r') as f:
    print(json.dumps(f.read()))"
}

create_github_release() {
    TAG="${1}"
    VERSION="${2}"
    if [ -z "${ACCESS_TOKEN}" ]; then
        echo "ACCESS_TOKEN environment variable doesn't provided"
        read -s -p "Please, copy/paste GitHub personal access token (input is hidden)" ACCESS_TOKEN
    fi
    read -p "This repo owner's username: " REPO_OWNER
    read -p "This repo's real name: " REPO_NAME

    echo "Write release description. This description will be displayed on GitHub."
    echo "New nano window will open. Press any key to continue..."
    read -n 1 -s

    DESCRIPTION_FILE="$( mktemp )"
    nano "${DESCRIPTION_FILE}"

    API_JSON=$(printf '{"tag_name": "%s","name": "Version %s","body": %s,"draft": false,"prerelease": false}' \
        "${TAG}" "${VERSION}" "$(json_escape_file ${DESCRIPTION_FILE})")
    rm "${DESCRIPTION_FILE}"
    curl --data "${API_JSON}" \
        "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases?access_token=${ACCESS_TOKEN}" >> "${LOGS_FILE}"
}

push_changes() {
    remote_name="${1}"
    branch_name=$( git branch | grep '*' | cut -d' ' -f 2 )

    git push ${remote_name} ${branch_name} >> "${LOGS_FILE}"
}

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
if [ 1 -gt "${#remotes[@]}" ]; then
    echo "Select remote, that will be used in current session"
    select remote_name in ${remotes[@]}; do
        if [ ! -z "${remote_name}" ]; then
            REMOTE="${remote_name}"
            break
        fi
    done
else
    REMOTE="${remotes[0]}"
fi

mvn -B versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false >> "${LOGS_FILE}"
current_hash="$( git rev-parse HEAD )"
git commit -am "Version ${VERSION}" >> "${LOGS_FILE}"
git tag -a ${TAG} -m "Version ${VERSION}" >> "${LOGS_FILE}"
git push "${REMOTE}" ${TAG} >> "${LOGS_FILE}"
git reset --hard "${current_hash}" >> "${LOGS_FILE}"

echo "We've create the tag for version ${VERSION}. Do you want to create GitHub Release?"
echo "This action requires access token (https://github.com/settings/tokens)."
select TMP in "Yes" "No"; do
    case "${TMP}" in
        "Yes") create_github_release "${TAG}" "${VERSION}"; break;;
        "No") break;;
    esac
done

mvn -B versions:set -DnewVersion="${NEW_DEV_VERSION}" -DgenerateBackupPoms=false >> "${LOGS_FILE}"
git commit -am "New development version: ${NEW_DEV_VERSION}" >> "${LOGS_FILE}"

echo "Do you want to push development version change to remote repo?"
echo "We strongly recommend to preform this step"
select TMP in "Yes" "No"; do
    case "${TMP}" in
        "Yes") push_changes "${REMOTE}"; break;;
        "No") break;;
    esac
done

echo "Remove logs file ${LOGS_FILE}?"
select TMP in "Yes" "No"; do
    case "${TMP}" in
        "Yes") rm "${LOGS_FILE}"; break;;
        "No") break;;
    esac
done

cd "${CURRENT_DIR}"
