#!/bin/bash

mvn clean package

java -jar server-io/target/server-io.jar -s 5001 -c 5002 -n "Alice" &
PID_1=$!
java -jar server-io/target/server-io.jar -s 5002 -c 5003 -n "Bob" &
PID_2=$!
java -jar server-io/target/server-io.jar -s 5003 -c 5001 -n "Carol" &
PID_3=$!

echo "Started process: $PID_1 $PID_2 $PID_3"

# trap ctrl-c and call ctrl_c()
trap ctrl_c INT

function ctrl_c() {
   echo "Killing process: $PID_1 $PID_2 $PID_3"
   kill -9 $PID_1 $PID_2 $PID_3
   wait $PID_1 $PID_2 $PID_3
}

wait $PID_1 $PID_2 $PID_3

echo "Done!"