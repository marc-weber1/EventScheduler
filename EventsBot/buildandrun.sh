#!/bin/sh
rm -r bin/*
javac -cp src:lib/json-simple-1.1.1.jar -d bin src/Master.java
cd bin
jar cfm ../EventBot.jar ../Manifest.txt *
cd ..
java -jar EventBot.jar [BOT API KEY] [DATABASE NAME] [DATABASE PASSWORD]
