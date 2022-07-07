while true
do
        if [ `curl -sL -w %{http_code} "$1" -o /dev/null` != 000 ]; then
                echo "URL does exist: $1"
                break
        else
                echo "URL does not exist: $1"
        fi
        sleep 3
done