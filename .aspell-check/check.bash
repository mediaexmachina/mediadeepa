#!/bin/bash

set -eu
SCRIPT_DIR="$(dirname "$(realpath "$0")")";
cd "$SCRIPT_DIR/..";

CORRECT_MODE="0";
if [[ $# -gt 0 ]]; then
    if [[ "$1" == "correct" ]]; then
        CORRECT_MODE=1;
    fi
fi

list_aspell () {
    aspell list --lang=en_US --mode=markdown --home-dir="$SCRIPT_DIR" < "$1";
}

HAS_ERRORS=0;
for f in src/main/resources/doc/en/*.md; do
    ERROR_COUNT=$(list_aspell "$f" | wc -l);
    if [[ $ERROR_COUNT -gt 0 ]]; then
        if [[ $CORRECT_MODE -eq 0 ]]; then
            echo "$ERROR_COUNT spell error(s) found on $f:";
            list_aspell "$f";
            echo "Run $0 correct";
            HAS_ERRORS=1;
        else
            aspell check --dont-backup --lang=en_US --mode=markdown --home-dir="$SCRIPT_DIR" "$f";
        fi
    fi
done

if [[ $HAS_ERRORS -eq 0 ]]; then
    echo "No error found.";
fi

exit $HAS_ERRORS;
