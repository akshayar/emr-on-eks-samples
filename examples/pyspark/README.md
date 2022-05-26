### Sumit WordCount PySpark Job
- Submit Job
```shell
export JOB_RUN_ID=`aws emr-containers start-job-run \
  --virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
  --name word_count \
  --execution-role-arn ${JOB_ROLE_ARN} \
  --release-label emr-6.2.0-latest \
  --job-driver '{
    "sparkSubmitJobDriver": {
      "entryPoint": "s3://us-east-1.elasticmapreduce/emr-containers/samples/wordcount/scripts/wordcount.py",
      "entryPointArguments": ["s3://'${S3_BUCKET}'/emreks/fargate/output"],
      "sparkSubmitParameters": "--conf spark.kubernetes.driver.label.type=etl --conf spark.kubernetes.executor.label.type=etl --conf spark.executor.instances=8 --conf spark.executor.memory=2G --conf spark.driver.cores=1 --conf spark.executor.cores=3"
      }
    }' \
  --configuration-overrides '{
    "applicationConfiguration": [{
        "classification": "spark-defaults", 
        "properties": {"spark.kubernetes.allocation.batch.size": "8"}
    }],
    "monitoringConfiguration": {
      "s3MonitoringConfiguration": {
         "logUri": "s3://'${S3_BUCKET}'/fargate-logs/"
         }
    }
  }' --query id --output text`
  
```
- View Job Status
```shell
aws emr-containers list-job-runs --virtual-cluster-id ${VIRTUAL_CLUSTER_ID}
  
aws emr-containers describe-job-run --virtual-cluster-id ${VIRTUAL_CLUSTER_ID} --id ${JOB_RUN_ID}
  
aws emr-containers describe-job-run --virtual-cluster-id ${VIRTUAL_CLUSTER_ID} --id ${JOB_RUN_ID} --query jobRun.state

```
- View Logs 
```shell
aws s3 ls  --recursive s3://${S3_BUCKET}/fargate-logs/${VIRTUAL_CLUSTER_ID}/jobs/$JOB_RUN_ID/
aws s3 cp  s3://${S3_BUCKET}/fargate-logs/${VIRTUAL_CLUSTER_ID}/jobs/$JOB_RUN_ID/containers/spark-$JOB_RUN_ID/spark-$JOB_RUN_ID-driver/stdout.gz - | gunzip
aws s3 cp  s3://${S3_BUCKET}/fargate-logs/${VIRTUAL_CLUSTER_ID}/jobs/$JOB_RUN_ID/containers/spark-$JOB_RUN_ID/spark-$JOB_RUN_ID-driver/stderr.gz - | gunzip
```
- View Job Output
```shell
aws s3 ls --recursive s3://${S3_BUCKET}/emreks/fargate/output
mkdir -p  temp/output/
aws s3 cp --recursive s3://${S3_BUCKET}/emreks/fargate/output temp/output/

```

## Execute copy-data.py job
- First, make sure the `copy-data.py` script is uploaded to an S3 bucket .
```shell
aws s3 cp $SOURCE_ROOT/examples/pyspark/copy-data.py s3://${S3_BUCKET}/code/pyspark/
```
- Execute job and wait till completion
```shell
cd $SOURCE_ROOT/examples/pyspark
chmod +x *.sh
./run-job-to-completion.sh
```