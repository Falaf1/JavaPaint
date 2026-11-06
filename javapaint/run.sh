#!/bin/bash
mkdir -p bin
javac -encoding UTF-8 -d bin src/*.java && java -cp bin App
