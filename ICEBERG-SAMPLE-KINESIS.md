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
export KINESIS_STREAM_NAME=data-stream-ingest
export ICEBERG_TABLE_NAME=eks_ec2_iceberg_kinesis
export ICEBERG_TARGET_DB_NAME=demohudi
export LOG_GROUP_NAME=/emr-on-eks/eks_ec2_iceberg_kinesis
export ACCOUNT_ID=`aws sts get-caller-identity --output text --query Account`
export FARGATE_RUN=Y ##N otherwise
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
git clone -b EMR_ON_EKS https://github.com/akshayar/aws-iceberg.git
cd aws-iceberg/spark-streaming-kinesis
./build.sh ${S3_BUCKET_FOR_JAR}
aws s3 ls s3://${S3_BUCKET_FOR_JAR}/spark-structured-streaming-kinesis-iceberg_2.12-1.0.jar
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
```shell
export JARS="https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-3.1_2.12/0.13.1/iceberg-spark-runtime-3.1_2.12-0.13.1.jar,\
https://repo1.maven.org/maven2/org/apache/spark/spark-streaming-kinesis-asl_2.12/3.1.1/spark-streaming-kinesis-asl_2.12-3.1.1.jar,\
s3://'"${S3_BUCKET_FOR_JAR}"'/spark-sql-kinesis_2.12-1.2.1_spark-3.0-SNAPSHOT.jar,\
https://repo1.maven.org/maven2/software/amazon/awssdk/bundle/2.15.40/bundle-2.15.40.jar,\
https://repo1.maven.org/maven2/software/amazon/awssdk/url-connection-client/2.15.40/url-connection-client-2.15.40.jar"



JOB_RUN_ID=`aws emr-containers start-job-run \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--name hudi-${ICEBERG_TABLE_NAME} \
--execution-role-arn ${EMR_EKS_EXECUTION_ARN} \
--release-label emr-6.5.0-latest \
--job-driver '{
    "sparkSubmitJobDriver": {
        "entryPoint": "s3://'"${S3_BUCKET_FOR_JAR}"'/spark-structured-streaming-kinesis-iceberg_2.12-1.0.jar",
        "entryPointArguments": ["'${S3_BUCKET_FOR_DATA}'", "'${KINESIS_STREAM_NAME}'","ap-south-1", "my_catalog.'${ICEBERG_TARGET_DB_NAME}'.'${ICEBERG_TABLE_NAME}'", "LATEST"],
        "sparkSubmitParameters": "--class kinesis.iceberg.latefile.SparkKinesisConsumerIcebergProcessor --jars '${JARS}'"
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
        "logUri": "'s3://"${S3_BUCKET_FOR_JAR}"'/hudi/logs/"
      }
    }
}' --query id --output text`

```
### Submit Iceberg Streaming Job while providing accessKey and secret access key
```shell
export JARS="https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-3.1_2.12/0.13.1/iceberg-spark-runtime-3.1_2.12-0.13.1.jar,\
https://repo1.maven.org/maven2/org/apache/spark/spark-streaming-kinesis-asl_2.12/3.1.1/spark-streaming-kinesis-asl_2.12-3.1.1.jar,\
s3://'"${S3_BUCKET_FOR_JAR}"'/spark-sql-kinesis_2.12-1.2.1_spark-3.0-SNAPSHOT.jar,\
https://repo1.maven.org/maven2/software/amazon/awssdk/bundle/2.15.40/bundle-2.15.40.jar,\
https://repo1.maven.org/maven2/software/amazon/awssdk/url-connection-client/2.15.40/url-connection-client-2.15.40.jar"



JOB_RUN_ID=`aws emr-containers start-job-run \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--name hudi-${ICEBERG_TABLE_NAME} \
--execution-role-arn ${EMR_EKS_EXECUTION_ARN} \
--release-label emr-6.5.0-latest \
--job-driver '{
    "sparkSubmitJobDriver": {
        "entryPoint": "s3://'"${S3_BUCKET_FOR_JAR}"'/spark-structured-streaming-kinesis-iceberg_2.12-1.0.jar",
        "entryPointArguments": ["'${S3_BUCKET_FOR_DATA}'", "'${KINESIS_STREAM_NAME}'","ap-south-1", "my_catalog.'${ICEBERG_TARGET_DB_NAME}'.'${ICEBERG_TABLE_NAME}'", "LATEST"],
        "sparkSubmitParameters": "--class kinesis.iceberg.latefile.SparkKinesisConsumerIcebergProcessor --jars '${JARS}'"
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
aws s3 ls --recursive s3://${S3_BUCKET_FOR_DATA}/demo/hudi/${ICEBERG_TABLE_NAME}

```
## Query Athena
```shell
QUERY_EXECUTION_ID=`aws athena start-query-execution --query-string "SELECT * FROM ${ICEBERG_TABLE_NAME}_cow limit 10" \
--query-execution-context Database=${ICEBERG_TARGET_DB_NAME},Catalog=AwsDataCatalog \
--region ap-south-1  --result-configuration OutputLocation=s3://${S3_BUCKET_FOR_DATA}/athena/  --query QueryExecutionId --output text`

echo Submitted $QUERY_EXECUTION_ID
aws athena get-query-execution --query-execution-id ${QUERY_EXECUTION_ID} --output text
aws athena get-query-results --query-execution-id ${QUERY_EXECUTION_ID} --output text
```

"": "",
"": "",
"connectionName": "BigQueryConnector",


Exception in thread "main" software.amazon.awssdk.core.exception.SdkClientException: Unable to load credentials from any of the providers in the chain AwsCredentialsProviderChain(
credentialsProviders=[
SystemPropertyCredentialsProvider(), 
EnvironmentVariableCredentialsProvider(), 
WebIdentityTokenCredentialsProvider(), 
ProfileCredentialsProvider(), 
ContainerCredentialsProvider(), 
InstanceProfileCredentialsProvider()]) : 
[SystemPropertyCredentialsProvider(): Unable to load credentials from system settings. Access key must be specified either via environment variable (AWS_ACCESS_KEY_ID) or system property (aws.accessKeyId)., 
EnvironmentVariableCredentialsProvider(): Unable to load credentials from system settings. Access key must be specified either via environment variable (AWS_ACCESS_KEY_ID) or system property (aws.accessKeyId)., 
WebIdentityTokenCredentialsProvider(): Multiple HTTP implementations were found on the classpath. To avoid non-deterministic loading implementations, please explicitly provide an HTTP client via the client builders, set the software.amazon.awssdk.http.service.impl system property with the FQCN of the HTTP service to use as the default, or remove all but one HTTP implementation from the classpath, 
ProfileCredentialsProvider(): Profile file contained no credentials for profile 'default': ProfileFile(profiles=[]), 
ContainerCredentialsProvider(): Cannot fetch credentials from container - neither AWS_CONTAINER_CREDENTIALS_FULL_URI or AWS_CONTAINER_CREDENTIALS_RELATIVE_URI environment variables are set., 
InstanceProfileCredentialsProvider(): Failed to load credentials from IMDS.]