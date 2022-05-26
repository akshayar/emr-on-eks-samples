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
export KAFKA_BOOTSTRAP=ip-192-168-33-209.ap-south-1.compute.internal:9092
export KAFKA_SCHEMA_REGISTRY=http://ip-10-192-11-254.ap-south-1.compute.internal:8081
export KAFKA_TOPIC=data-kafka-json
export HUDI_TABLE_NAME=eks_fargate_hudi_kafka
export HUDI_TARGET_DB_NAME=demohudi
export LOG_GROUP_NAME=/emr-on-eks/eksworkshop-eksctl
export ACCOUNT_ID=`aws sts get-caller-identity --output text --query Account`
export FARGATE_RUN=Y ##N otherwise
```
2. Checkout project and copy properties files to S3. 
```shell
git clone -b EMR_ON_EKS https://github.com/akshayar/apache-hudi-samples.git
cd apache-hudi-samples/spark-streaming-kafka
KAFKA_SERVER=`echo $KAFKA_BOOTSTRAP | cut -f 1 -d ":"`
KAFKA_PORT=`echo $KAFKA_BOOTSTRAP | cut -f 2 -d ":"`
envsubst < hudi-delta-streamer/hudi-deltastreamer-dms.properties > hudi-delta-streamer/hudi-deltastreamer-dms.properties.out
aws s3 cp hudi-delta-streamer/hudi-deltastreamer-dms.properties.out  s3://${S3_BUCKET_FOR_JAR}/hudideltastreamer/hudi-deltastreamer-dms.properties

envsubst <  hudi-delta-streamer/hudi-deltastreamer-schema-file-json.properties > hudi-delta-streamer/hudi-deltastreamer-schema-file-json.properties.out 
aws s3 cp hudi-delta-streamer/hudi-deltastreamer-schema-file-json.properties.out s3://${S3_BUCKET_FOR_JAR}/hudideltastreamer/hudi-deltastreamer-schema-file-json.properties

envsubst <  hudi-delta-streamer/hudi-deltastreamer-schema-registry-avro.properties > hudi-delta-streamer/hudi-deltastreamer-schema-registry-avro.properties.out
aws s3 cp hudi-delta-streamer/hudi-deltastreamer-schema-registry-avro.properties.out  s3://${S3_BUCKET_FOR_JAR}/hudideltastreamer/hudi-deltastreamer-schema-registry-avro.properties

aws s3 cp hudi-delta-streamer/TradeData.avsc s3://${S3_BUCKET_FOR_JAR}/hudideltastreamer/TradeData.avsc

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

export JARS="https://repo1.maven.org/maven2/org/apache/hudi/hudi-spark3-bundle_2.12/0.10.0/hudi-spark3-bundle_2.12-0.10.0.jar,\
https://repo1.maven.org/maven2/org/apache/hudi/hudi-utilities-bundle_2.12/0.10.0/hudi-utilities-bundle_2.12-0.10.0.jar,\
https://repo1.maven.org/maven2/org/apache/spark/spark-sql-kafka-0-10_2.12/3.1.1/spark-sql-kafka-0-10_2.12-3.1.1.jar,\
https://repo1.maven.org/maven2/org/apache/spark/spark-streaming-kafka-0-10-assembly_2.12/3.1.1/spark-streaming-kafka-0-10-assembly_2.12-3.1.1.jar,\
https://repo1.maven.org/maven2/org/apache/commons/commons-pool2/2.6.2/commons-pool2-2.6.2.jar,\
https://repo1.maven.org/maven2/org/apache/httpcomponents/httpclient/4.5.9/httpclient-4.5.9.jar,\
https://repo1.maven.org/maven2/org/apache/calcite/calcite-core/1.29.0/calcite-core-1.29.0.jar,\
https://repo1.maven.org/maven2/org/apache/thrift/libfb303/0.9.3/libfb303-0.9.3.jar,\
https://repo1.maven.org/maven2/org/apache/spark/spark-avro_2.12/3.1.1/spark-avro_2.12-3.1.1.jar"

  

JOB_RUN_ID=`aws emr-containers start-job-run \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--name hudi-${HUDI_TABLE_NAME} \
--execution-role-arn ${EMR_EKS_EXECUTION_ARN} \
--release-label emr-6.5.0-latest \
--job-driver '{
    "sparkSubmitJobDriver": {
        "entryPoint": "https://repo1.maven.org/maven2/org/apache/hudi/hudi-utilities-bundle_2.12/0.10.0/hudi-utilities-bundle_2.12-0.10.0.jar",
        "entryPointArguments": ["--checkpoint","s3://'${S3_BUCKET_FOR_DATA}'/demo/hudi-delta-streamer/kafka-stream-data-checkpoint/'${HUDI_TABLE_NAME}'",
                                "--continuous","--enable-hive-sync",
                                "--schemaprovider-class","org.apache.hudi.utilities.schema.FilebasedSchemaProvider",
                                "--source-class","org.apache.hudi.utilities.sources.JsonKafkaSource",
                                "--spark-master","yarn",
                                "--table-type","COPY_ON_WRITE",
                                "--target-base-path","s3://'${S3_BUCKET_FOR_DATA}'/demo/hudi-delta-streamer/'${HUDI_TABLE_NAME}'",
                                "--target-table","'${HUDI_TABLE_NAME}'",
                                "--op","UPSERT",
                                "--source-ordering-field","tradeId",
                                "--props","s3://'${S3_BUCKET_FOR_DATA}'/hudideltastreamer/hudi-deltastreamer-schema-file-json.properties"],
        "sparkSubmitParameters": "--class org.apache.hudi.utilities.deltastreamer.HoodieDeltaStreamer --jars '${JARS}'"
        }
    }' \
--configuration-overrides '{
    "applicationConfiguration": [
      {
        "classification": "spark-defaults", 
        "properties": {
          '${FARGATE_LABEL}'
          "spark.driver.memory":"2G",
          "spark.dynamicAllocation.maxExecutors":"4",
          "spark.serializer":"org.apache.spark.serializer.KryoSerializer",
          "spark.sql.hive.convertMetastoreParquet":"false",
          "spark.kubernetes.file.upload.path":"s3://'${S3_BUCKET_FOR_JAR}'/emr/eks",
          "spark.hadoop.hive.metastore.client.factory.class" : "com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory",
          "spark.sql.extensions":"org.apache.spark.sql.hudi.HoodieSparkSessionExtension"
         }
      }
    ], 
    "monitoringConfiguration": {
      "cloudWatchMonitoringConfiguration": {
        "logGroupName": "'${LOG_GROUP_NAME}'", 
        "logStreamNamePrefix": "'${HUDI_TABLE_NAME}'"
      }, 
      "s3MonitoringConfiguration": {
        "logUri": "'s3://"${S3_BUCKET_FOR_JAR}"'/hudi/logs/"
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
aws s3 ls --recursive s3://${S3_BUCKET_FOR_JAR}/hudi/logs/${VIRTUAL_CLUSTER_ID}/jobs/${JOB_RUN_ID}
```
## View Data Saved in Hudi Table
```shell
aws s3 ls --recursive s3://${S3_BUCKET_FOR_DATA}/demo/hudi-delta-streamer/${HUDI_TABLE_NAME}

```
## Query Athena
```shell
QUERY_EXECUTION_ID=`aws athena start-query-execution --query-string "SELECT * FROM ${HUDI_TABLE_NAME} limit 10" \
--query-execution-context Database=${HUDI_TARGET_DB_NAME},Catalog=AwsDataCatalog \
--region ap-south-1  --result-configuration OutputLocation=s3://${S3_BUCKET_FOR_DATA}/athena/  --query QueryExecutionId --output text`

echo Submitted $QUERY_EXECUTION_ID
aws athena get-query-execution --query-execution-id ${QUERY_EXECUTION_ID} --output text
aws athena get-query-results --query-execution-id ${QUERY_EXECUTION_ID} --output text
```