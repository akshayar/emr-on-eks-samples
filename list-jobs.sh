VIRTUAL_CLUSTER_ID=$1
if [ -z "${VIRTUAL_CLUSTER_ID}" ]
then
  echo "No clustet id passed, use as ./list-jobs <custer id> <RUNNING/FAILED/COMPLETE RUNNING default>"
  exit 1
fi
TARGET_STAT=$2
if [ -z "${TARGET_STAT}" ]
then
  TARGET_STAT=RUNNING
fi

echo "Querying ${VIRTUAL_CLUSTER_ID} for ${TARGET_STAT}"
aws emr-containers list-job-runs \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--query 'jobRuns[?state==`'${TARGET_STAT}'`]'