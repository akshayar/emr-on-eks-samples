VIRTUAL_CLUSTER_ID=$1
if [ -z "${VIRTUAL_CLUSTER_ID}" ]
then
  echo "No cluster id passed, use as ./cancel-job-by-id <custer id> <jobid>"
fi
JOB_ID=$2

echo "Viewing ${JOB_ID}"
aws emr-containers describe-job-run --id ${JOB_ID} --virtual-cluster-id ${VIRTUAL_CLUSTER_ID}
