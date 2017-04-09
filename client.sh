#!/bin/bash
if [ $1 == '-c' ]	
then
	javac -cp lib/zookeeper-3.4.9.jar:lib/slf4j-api-1.7.25.jar:lib/slf4j-simple-1.7.25.jar -sourcepath src -d bin src/**/**/**/*.java
	echo "Compilation success"
else
	java -cp $PWD/lib/zookeeper-3.4.9.jar:$PWD/lib/slf4j-api-1.7.25.jar:$PWD/lib/slf4j-simple-1.7.25.jar:bin: com.hbase.client.ClientDriver $@
fi

