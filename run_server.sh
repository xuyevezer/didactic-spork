#!/bin/bash
# Usage:
#     To generate a new database: ./run_server.sh DATABASENAME.json generate
#     To load an existing database: ./run_server.sh DATABASENAME.json
cd BankingServer/bin/
java -cp ../lib/*:. ServerMain ${1} ${2:- }
cd ../../
