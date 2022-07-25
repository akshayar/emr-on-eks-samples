## Submit Spark Streaming with Hudi
### Prerequisite
1. Follow prerequisite mentioned at [README.md](fargate-serverless/README.md)
### Build the package of job
1. Setup the bucket , role , virtual server id and other parameters. Ensure that the execution role has permission on S3 bucket and log group. 
```shell
export S3_BUCKET_FOR_JAR=${S3_BUCKET}
export S3_BUCKET_FOR_DATA=${S3_BUCKET}
export VIRTUAL_CLUSTER_ID=<>
export EMR_EKS_EXECUTION_ARN=<>
export KAFKA_BOOTSTRAP=ip-192-168-20-233.ap-south-1.compute.internal:9092
export KAFKA_TOPIC=data-kafka-json
export ICEBERG_TABLE_NAME=eks_ec2_iceberg_kafka
export ICEBERG_TARGET_DB_NAME=demoiceberg
export LOG_GROUP_NAME=/emr-on-eks/demoiceberg
export ACCOUNT_ID=`aws sts get-caller-identity --output text --query Account`
export FARGATE_RUN=Y ##N otherwise
```
2. Checkout and build sample project. 
```shell
git clone -b EMR_ON_EKS https://github.com/akshayar/aws-iceberg.git
cd aws-iceberg/spark-streaming-kafka
./build.sh ${S3_BUCKET_FOR_JAR}
aws s3 ls s3://${S3_BUCKET_FOR_JAR}/spark-structured-streaming-kafka-iceberg_2.12-1.0.jar
```
3. Ensure connectivity with Kafka. 

## Submit Job and Monitor Job
### Submit Job
```shell
if  [[ "${FARGATE_RUN}" = "Y" ]] || [[ "${FARGATE_RUN}" = "y" ]] 
then
  FARGATE_LABEL='"spark.kubernetes.driver.label.type":"etl","spark.kubernetes.executor.label.type":"etl",'
  echo "Fargate Run, Labe config  ${FARGATE_LABEL}"
else
  FARGATE_LABEL=''
  echo "EC2 Run , Labe config  ${FARGATE_LABEL}"
fi
```

### Submit Iceberg Streaming Job without providing accessKey and secret access key
```

export JARS="https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark3-runtime/0.13.1/iceberg-spark3-runtime-0.13.1.jar,\
https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark3-extensions/0.13.1/iceberg-spark3-extensions-0.13.1.jar,\
https://repo1.maven.org/maven2/org/apache/spark/spark-sql-kafka-0-10_2.12/3.1.1/spark-sql-kafka-0-10_2.12-3.1.1.jar,\
https://repo1.maven.org/maven2/org/apache/spark/spark-streaming-kafka-0-10-assembly_2.12/3.1.1/spark-streaming-kafka-0-10-assembly_2.12-3.1.1.jar,\
https://repo1.maven.org/maven2/software/amazon/awssdk/bundle/2.15.40/bundle-2.15.40.jar,\
https://repo1.maven.org/maven2/org/apache/commons/commons-pool2/2.6.2/commons-pool2-2.6.2.jar,\
https://repo1.maven.org/maven2/software/amazon/awssdk/url-connection-client/2.15.40/url-connection-client-2.15.40.jar"



JOB_RUN_ID=`aws emr-containers start-job-run \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--name iceberg-${ICEBERG_TABLE_NAME} \
--execution-role-arn ${EMR_EKS_EXECUTION_ARN} \
--release-label emr-6.5.0-latest \
--job-driver '{
    "sparkSubmitJobDriver": {
        "entryPoint": "s3://'"${S3_BUCKET_FOR_JAR}"'/spark-structured-streaming-kafka-iceberg_2.12-1.0.jar",
        "entryPointArguments": ["'${S3_BUCKET_FOR_DATA}'", "'${KAFKA_BOOTSTRAP}'","'${KAFKA_TOPIC}'", "my_catalog.'${ICEBERG_TARGET_DB_NAME}'.'${ICEBERG_TABLE_NAME}'", "LATEST"],
        "sparkSubmitParameters": "--conf spark.sql.catalog.my_catalog.client.factory=com.aksh.iceberg.ClientForFargateRun --class kafka.iceberg.latefile.SparkKakfaConsumerIcebergProcessor --jars '${JARS}'"
        }
    }' \
--configuration-overrides '{
    "applicationConfiguration": [
      {
        "classification": "spark-defaults", 
        "properties": {
          '${FARGATE_LABEL}'
          "spark.sql.extensions":"org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions",    \
          "spark.sql.catalog.my_catalog":"org.apache.iceberg.spark.SparkCatalog" ,   \
          "spark.sql.catalog.my_catalog.warehouse":"s3://'${S3_BUCKET_FOR_DATA}'/iceberg" ,\
          "spark.sql.catalog.my_catalog.catalog-impl":"org.apache.iceberg.aws.glue.GlueCatalog" ,\
          "spark.sql.catalog.my_catalog.io-impl":"org.apache.iceberg.aws.s3.S3FileIO",\
          "spark.sql.catalog.my_catalog.client.assume-role.arn":"'${EMR_EKS_EXECUTION_ARN}'", \
          "spark.sql.catalog.my_catalog.client.assume-role.region":"ap-south-1"
         }
      }
    ], 
    "monitoringConfiguration": {
      "cloudWatchMonitoringConfiguration": {
        "logGroupName": "'${LOG_GROUP_NAME}'", 
        "logStreamNamePrefix": "'${ICEBERG_TABLE_NAME}'"
      }, 
      "s3MonitoringConfiguration": {
        "logUri": "'s3://"${S3_BUCKET_FOR_JAR}"'/iceberg/logs/"
      }
    }
}' --query id --output text`

```

### Submit Iceberg Streaming Job with  accessKey and secret access key
```

export JARS="https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark3-runtime/0.13.1/iceberg-spark3-runtime-0.13.1.jar,\
https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark3-extensions/0.13.1/iceberg-spark3-extensions-0.13.1.jar,\
https://repo1.maven.org/maven2/org/apache/spark/spark-sql-kafka-0-10_2.12/3.1.1/spark-sql-kafka-0-10_2.12-3.1.1.jar,\
https://repo1.maven.org/maven2/org/apache/spark/spark-streaming-kafka-0-10-assembly_2.12/3.1.1/spark-streaming-kafka-0-10-assembly_2.12-3.1.1.jar,\
https://repo1.maven.org/maven2/software/amazon/awssdk/bundle/2.15.40/bundle-2.15.40.jar,\
https://repo1.maven.org/maven2/org/apache/commons/commons-pool2/2.6.2/commons-pool2-2.6.2.jar,\
https://repo1.maven.org/maven2/software/amazon/awssdk/url-connection-client/2.15.40/url-connection-client-2.15.40.jar"



JOB_RUN_ID=`aws emr-containers start-job-run \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--name iceberg-${ICEBERG_TABLE_NAME} \
--execution-role-arn ${EMR_EKS_EXECUTION_ARN} \
--release-label emr-6.5.0-latest \
--job-driver '{
    "sparkSubmitJobDriver": {
        "entryPoint": "s3://'"${S3_BUCKET_FOR_JAR}"'/spark-structured-streaming-kafka-iceberg_2.12-1.0.jar",
        "entryPointArguments": ["'${S3_BUCKET_FOR_DATA}'", "'${KAFKA_BOOTSTRAP}'","'${KAFKA_TOPIC}'", "my_catalog.'${ICEBERG_TARGET_DB_NAME}'.'${ICEBERG_TABLE_NAME}'", "LATEST"],
        "sparkSubmitParameters": "--class kafka.iceberg.latefile.SparkKakfaConsumerIcebergProcessor --jars '${JARS}'"
        }
    }' \
--configuration-overrides '{
    "applicationConfiguration": [
      {
        "classification": "spark-defaults", 
        "properties": {
          '${FARGATE_LABEL}'
          "spark.sql.extensions":"org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions",    \
          "spark.sql.catalog.my_catalog":"org.apache.iceberg.spark.SparkCatalog" ,   \
          "spark.sql.catalog.my_catalog.warehouse":"s3://'${S3_BUCKET_FOR_DATA}'/iceberg" ,\
          "spark.sql.catalog.my_catalog.catalog-impl":"org.apache.iceberg.aws.glue.GlueCatalog" ,\
          "spark.sql.catalog.my_catalog.io-impl":"org.apache.iceberg.aws.s3.S3FileIO",\
          "spark.sql.catalog.my_catalog.client.factory":"org.apache.iceberg.aws.AssumeRoleAwsClientFactory", \
          "spark.sql.catalog.my_catalog.client.assume-role.arn":"'${EMR_EKS_EXECUTION_ARN}'", \
          "spark.sql.catalog.my_catalog.client.assume-role.region":"ap-south-1",\
          "spark.driver.extraJavaOptions":"-Daws.accessKeyId=ACCESS_KEY_ID -Daws.secretAccessKey=SECRET_ACCESS_KEY",\
          "spark.executor.extraJavaOptions":"-Daws.accessKeyId=ACCESS_KEY_ID -Daws.secretAccessKey=SECRET_ACCESS_KEY"
         }
      }
    ], 
    "monitoringConfiguration": {
      "cloudWatchMonitoringConfiguration": {
        "logGroupName": "'${LOG_GROUP_NAME}'", 
        "logStreamNamePrefix": "'${ICEBERG_TABLE_NAME}'"
      }, 
      "s3MonitoringConfiguration": {
        "logUri": "'s3://"${S3_BUCKET_FOR_JAR}"'/iceberg/logs/"
      }
    }
}' --query id --output text`

```
### View Job Details
```shell
../view-job-by-id.sh ${VIRTUAL_CLUSTER_ID} ${JOB_RUN_ID}

## Get Pods
kubectl get pods --namespace=emr-eks-workshop-namespace
```
## View Logs
```shell
aws s3 ls --recursive s3://${S3_BUCKET_FOR_JAR}/iceberg/logs/${VIRTUAL_CLUSTER_ID}/jobs/${JOB_RUN_ID}
```
## View Data Saved in Hudi Table
```shell
aws s3 ls --recursive s3://${S3_BUCKET_FOR_DATA}/demo/iceberg/${HUDI_TABLE_NAME}

```
## Query Athena
```shell
QUERY_EXECUTION_ID=`aws athena start-query-execution --query-string "SELECT * FROM ${HUDI_TABLE_NAME}_cow limit 10" \
--query-execution-context Database=${HUDI_TARGET_DB_NAME},Catalog=AwsDataCatalog \
--region ap-south-1  --result-configuration OutputLocation=s3://${S3_BUCKET_FOR_DATA}/athena/  --query QueryExecutionId --output text`

echo Submitted $QUERY_EXECUTION_ID
aws athena get-query-execution --query-execution-id ${QUERY_EXECUTION_ID} --output text
aws athena get-query-results --query-execution-id ${QUERY_EXECUTION_ID} --output text
```