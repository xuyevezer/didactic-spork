#!/bin/bash
# Usage: ./run_client.sh IP PORT
# Example: ./run_client.sh 192.168.0.101 12301
cd BankingClient/bin/
java -cp ../lib/*:. ClientMain ${1} ${2}
cd ../../
