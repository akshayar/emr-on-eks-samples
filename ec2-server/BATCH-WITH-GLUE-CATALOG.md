## Example of job with Glue Catalog
```shell
aws emr-containers start-job-run \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--name spark-etl-s3-awsglue-integration \
--execution-role-arn ${EMR_EKS_EXECUTION_ARN} \
--release-label emr-5.33.0-latest \
--job-driver '{
    "sparkSubmitJobDriver": {
        "entryPoint": "s3://aws-data-analytics-workshops/emr-eks-workshop/scripts/spark-etl-glue.py",
        "entryPointArguments": [
          "s3://aws-data-analytics-workshops/shared_datasets/tripdata/","'s3://"$S3_BUCKET"'/taxi-data-glue4/","tripdata"
        ],
        "sparkSubmitParameters": "--conf spark.executor.instances=2 --conf spark.executor.memory=2G --conf spark.executor.cores=1 --conf spark.driver.cores=1"
        }
    }' \
--configuration-overrides '{
    "applicationConfiguration": [
      {
        "classification": "spark-defaults", 
        "properties": {
          "spark.hadoop.hive.metastore.client.factory.class":"com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory",
          "spark.sql.catalogImplementation": "hive"
         }
      }
    ], 
    "monitoringConfiguration": {
      "cloudWatchMonitoringConfiguration": {
        "logGroupName": "/emr-containers/jobs", 
        "logStreamNamePrefix": "emr-eks-workshop"
      }, 
      "s3MonitoringConfiguration": {
        "logUri": "'s3://"$S3_BUCKET"'/logs/"
      }
    }
}'

### Submiting the job on EMR Master Node
spark-submit --conf spark.executor.instances=2 --conf spark.executor.memory=2G \
--conf spark.executor.cores=1 --conf spark.driver.cores=1 \
--conf spark.hadoop.hive.metastore.client.factory.class=com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory \
s3://aws-data-analytics-workshops/emr-eks-workshop/scripts/spark-etl-glue.py \
s3://aws-data-analytics-workshops/shared_datasets/tripdata/ s3://${$S3_BUCKET}/taxi-data-glue5/tripdata

aws emr-containers start-job-run \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--name spark-etl-s3-awsglue-integration \
--execution-role-arn ${EMR_EKS_EXECUTION_ARN} \
--release-label emr-5.33.0-latest \
--job-driver '{
    "sparkSubmitJobDriver": {
        "entryPoint": "s3://aws-data-analytics-workshops/emr-eks-workshop/scripts/spark-etl-glue.py",
        "entryPointArguments": [
          "s3://aws-data-analytics-workshops/shared_datasets/tripdata/","'s3://"$S3_BUCKET"'/taxi-data-glue4/","tripdata"
        ],
        "sparkSubmitParameters": ""
        }
    }' \
--configuration-overrides '{
    "applicationConfiguration": [
      {
        "classification": "spark-defaults", 
        "properties": {
          "spark.hadoop.hive.metastore.client.factory.class":"com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory",
          "spark.sql.catalogImplementation": "hive"
         }
      }
    ], 
    "monitoringConfiguration": {
      "cloudWatchMonitoringConfiguration": {
        "logGroupName": "/emr-containers/jobs", 
        "logStreamNamePrefix": "emr-eks-workshop"
      }, 
      "s3MonitoringConfiguration": {
        "logUri": "'s3://"$S3_BUCKET"'/logs/"
      }
    }
}'
```