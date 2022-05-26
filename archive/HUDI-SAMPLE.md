## Submit Spark Streaming with Hudi
### Prerequisite
1. Follow prerequisite mentioned at [README.md](../ec2-server/README.md)
### Build the package of job
1. Setup the bucket , role , virtual server id and other parameters. Ensure that the execution role has permission on S3 bucket and log group. 
```shell
export S3_BUCKET_FOR_JAR=${S3_BUCKET}
export S3_BUCKET_FOR_DATA=${S3_BUCKET}
export VIRTUAL_CLUSTER_ID=<>
export EMR_EKS_EXECUTION_ARN=<>
export KINESIS_STREAM_NAME=data-stream-ingest
export HUDI_TABLE_NAME=eks_hudi_table
export HUDI_TARGET_DB_NAME=demohudi
export LOG_GROUP_NAME=/emr-on-eks/eksworkshop-eksctl
export ACCOUNT_ID=`aws sts get-caller-identity --output text --query Account`

```
2. Checkout and build kinesis-sql library required for the job. 
```shell
git clone https://github.com/akshayar/kinesis-sql.git
cd kinesis-sql
mvn install -DskipTests
aws s3 cp target/spark-sql-kinesis_2.12-1.2.1_spark-3.0-SNAPSHOT.jar s3://${S3_BUCKET_FOR_JAR}/
```
3. Checkout and build sample project. 
```shell
git clone -b EMR_ON_EKS https://github.com/akshayar/apache-hudi-samples.git
cd apache-hudi-samples/spark-streaming-kinesis
./build.sh ${S3_BUCKET_FOR_JAR}
aws s3 ls s3://${S3_BUCKET_FOR_JAR}/spark-structured-streaming-kinesis-hudi_2.12-1.0.jar
```
3.  Give Permission on Kinesis
```shell
aws iam put-role-policy --role-name ${ROLE_NAME} --policy-name KinesisAccess --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "kinesis:Get*",
                "kinesis:DescribeStreamSummary",
                "kinesis:ListShards"
            ],
            "Resource": [
                "arn:aws:kinesis:'${REGION}':'${ACCOUNT_ID}':stream/'${KINESIS_STREAM_NAME}'"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "kinesis:ListStreams"
            ],
            "Resource": [
                "*"
            ]
        }
    ]
}'
```
## Submit Job and Monitor Job
### Submit Job
```shell
JOB_RUN_ID=`aws emr-containers start-job-run \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--name hudi-${HUDI_TABLE_NAME} \
--execution-role-arn ${EMR_EKS_EXECUTION_ARN} \
--release-label emr-6.5.0-latest \
--job-driver '{
    "sparkSubmitJobDriver": {
        "entryPoint": "s3://'"${S3_BUCKET_FOR_JAR}"'/spark-structured-streaming-kinesis-hudi_2.12-1.0.jar",
        "entryPointArguments": ["'${S3_BUCKET_FOR_DATA}'", "'${KINESIS_STREAM_NAME}'", "ap-south-1", "COW", "'${HUDI_TABLE_NAME}'", "'${HUDI_TARGET_DB_NAME}'", "LATEST"],
        "sparkSubmitParameters": "--class kinesis.hudi.latefile.SparkKinesisConsumerHudiProcessor --jars https://repo1.maven.org/maven2/org/apache/hudi/hudi-spark3-bundle_2.12/0.9.0/hudi-spark3-bundle_2.12-0.9.0.jar,https://repo1.maven.org/maven2/org/apache/spark/spark-streaming-kinesis-asl_2.12/3.1.1/spark-streaming-kinesis-asl_2.12-3.1.1.jar,s3://'"${S3_BUCKET_FOR_JAR}"'/spark-sql-kinesis_2.12-1.2.1_spark-3.0-SNAPSHOT.jar"
        }
    }' \
--configuration-overrides '{
    "applicationConfiguration": [
      {
        "classification": "spark-defaults", 
        "properties": {
          "spark.driver.memory":"2G",
          "spark.dynamicAllocation.maxExecutors":"4",
          "spark.serializer":"org.apache.spark.serializer.KryoSerializer",
          "spark.sql.hive.convertMetastoreParquet":"false",
          "spark.kubernetes.file.upload.path":"s3://'${S3_BUCKET_FOR_JAR}'/emr/eks",
          "spark.hadoop.hive.metastore.client.factory.class":"com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory"
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
aws s3 ls --recursive s3://${S3_BUCKET_FOR_DATA}/demo/hudi/${HUDI_TABLE_NAME}

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