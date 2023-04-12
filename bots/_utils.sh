check_url() {
    if [ -z "$PAINTBOTS_URL" ]
    then
        echo "No server URL defined with PAINTBOTS_URL environment variable!"
        exit 1
    fi
}

check_id() {
    if [ -z "$PAINTBOTS_ID" ]
    then
        echo "No bot id defined with PAINTBOTS_ID environment variable!"
        exit 1
    fi
}

post() {
    check_url
    curl -H "Content-Type: application/x-www-form-urlencoded" -d $1 $PAINTBOTS_URL
}

register() {
    check_url
    if [ -z "$1" ]
    then
        echo "Call register with name!"
        exit 1
    fi
    post "register=$1"
}

paint() {
    check_id
    post "id=$PAINTBOTS_ID&paint";
}

move() {
    check_id
    post "id=$PAINTBOTS_ID&move=$1";
}

color() {
    check_id
    post "id=$PAINTBOTS_ID&color=$1";
}

say() {
    MSG="$@"
    check_id
    check_url
    curl -H "Content-Type: application/x-www-form-urlencoded" \
         --data-urlencode "id=$PAINTBOTS_ID" \
         --data-urlencode "msg=$MSG" \
         $PAINTBOTS_URL
}
