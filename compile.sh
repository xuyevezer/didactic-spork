#!/bin/bash
# Usage: ./compile.sh
cd BankingServer
javac -d bin -sourcepath src/ -cp lib/*:. src/ServerMain.java
cd ..
cd BankingClient
javac -d bin -sourcepath src/:../BankingServer/src/ -cp lib/*:. src/ClientMain.java
cd ..