export JOB_RUN_ID=`aws emr-containers start-job-run \
  --virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
  --name word_count \
  --execution-role-arn ${JOB_ROLE_ARN} \
  --release-label emr-6.5.0-latest \
  --job-driver '{
    "sparkSubmitJobDriver": {
      "entryPoint": "s3://'${S3_BUCKET}'/code/pyspark/copy-data.py",
      "entryPointArguments": ["s3://noaa-gsod-pds/2021/","s3://'${S3_BUCKET}'/output2/noaa_gsod_pds","emrserverless.noaa_gsod_pds"],
      "sparkSubmitParameters": " --conf spark.kubernetes.driver.label.type=etl --conf spark.kubernetes.executor.label.type=etl   --conf spark.driver.memory=4g --conf spark.driver.cores=2   --conf spark.executor.memory=4g --conf spark.executor.cores=2"
      }
    }' \
  --configuration-overrides '{
    "applicationConfiguration": [
      {
        "classification": "spark-defaults",
        "properties": {
          "spark.dynamicAllocation.enabled":"true",
          "spark.dynamicAllocation.shuffleTracking.enabled":"true",
          "spark.dynamicAllocation.minExecutors":"1",
          "spark.dynamicAllocation.maxExecutors":"100",
          "spark.dynamicAllocation.initialExecutors":"1"
         }
      }
    ],
    "monitoringConfiguration": {
      "s3MonitoringConfiguration": {
         "logUri": "s3://'${S3_BUCKET}'/fargate-logs/"
         }
    }
  }' --query id --output text`

aws emr-containers describe-job-run --virtual-cluster-id ${VIRTUAL_CLUSTER_ID} --id ${JOB_RUN_ID}
START_TIME=`date "+TIME: %H:%M:%S"`
JOB_STATE=`aws emr-containers describe-job-run --virtual-cluster-id ${VIRTUAL_CLUSTER_ID} --id ${JOB_RUN_ID} --query jobRun.state --output text`
while [ "$JOB_STATE" != "COMPLETED" ] && [ "$JOB_STATE" != "FAILED" ]; do
    sleep 30
    echo "Job $JOB_RUN_ID State is $JOB_STATE"
    kubectl get pod -n spark
    JOB_STATE=`aws emr-containers describe-job-run --virtual-cluster-id ${VIRTUAL_CLUSTER_ID} --id ${JOB_RUN_ID} --query jobRun.state --output text`
done
echo "Job $JOB_RUN_ID State is $JOB_STATE"
END_TIME=`date "+TIME: %H:%M:%S"`
echo "Job $JOB_RUN_ID Started $START_TIME , ended $END_TIME"