#!/bin/bash
if [ -z "${KINESIS_STREAM_NAME}" ] || [ -z "${KINESIS_REGION}" ]
then
 echo "Environment varriables not set. Set KINESIS_STREAM_NAME , KINESIS_REGION, "
 echo "export KINESIS_STREAM_NAME=<KINESIS_STREAM_NAME>;"
 echo "export KINESIS_REGION=<KINESIS_REGION>; "
 echo "./run.sh"
 exit 1
fi
JAVA_OPTIONS="-DstreamName=${KINESIS_STREAM_NAME} -Dregion=${KINESIS_REGION}"
echo ${JAVA_OPTIONS}
java -jar target/kinesis-consumer-printer.jar ${JAVA_OPTIONS}