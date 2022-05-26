VIRTUAL_CLUSTER_ID=$1
if [ -z "${VIRTUAL_CLUSTER_ID}" ]
then
  echo "No clustet id passed, use as ./cancel-running-jobs <custer id>"
fi
TARGET_STAT=RUNNING

echo "Querying ${VIRTUAL_CLUSTER_ID} for ${TARGET_STAT}"
RUNNING_JOBS=`aws emr-containers list-job-runs --virtual-cluster-id ${VIRTUAL_CLUSTER_ID} --query 'jobRuns[?state==\`'${TARGET_STAT}'\`].[id]' --output text`
echo $RUNNING_JOBS

for jobid in $RUNNING_JOBS
do
echo "Cancelling $jobid"
aws emr-containers cancel-job-run --id $jobid --virtual-cluster-id ${VIRTUAL_CLUSTER_ID}
done
