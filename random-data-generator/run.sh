#!/bin/bash
if [ -z "${DB_HOST_NAME}" ] || [ -z "${DB_PORT}" || [ -z "${DB_NAME}" ] || [ -z "${DB_NAME}" ] || [ -z "${SECRET_REGION}" ]
then
 echo "Environment varriables not set. Set DB_HOST_NAME , DB_PORT, DB_NAME , SECRET_NAME ,  SECRET_REGION"
 echo "export DB_HOST_NAME=<DB_HOST_NAME>;"
 echo "export DB_PORT=<port>; "
 echo "export DB_NAME=<db name> ;"
 echo "export SECRET_NAME=<secret-name>; "
 echo "export SECRET_REGION=<region of secret>;"
 echo "./run.sh pgsql/mysql"
 exit 1
fi

if [ -z "${1}" ]
then
  echo "./run.sh pgsql/mysql"
  exit 1
fi

JAVA_OPTIONS="-Ddb.server=${DB_SERVER} -Ddb.port=${DB_PORT} -Ddb.dbName=${DB_NAME} -DsecretName=${SECRET_NAME} -DsecretRegion=${SECRET_REGION}"
echo ${JAVA_OPTIONS}
if [ "${1}" = "mysql" ]
then
 java -jar mysql/target/mysql-random-data-generator.jar ${JAVA_OPTIONS}
else
 java -jar pgsql/target/pgsql-random-data-generator.jar ${JAVA_OPTIONS}
fi
