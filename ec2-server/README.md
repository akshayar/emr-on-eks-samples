## EMR on EKS Samples

## EKS and Virtual Cluster Creation
### Create EKS Cluster and bucket
1. Create an Amazon S3 bucket or set bucket name.

```shell
export S3_BUCKET=<>
aws s3 mb s3://${S3_BUCKET}
export S3_BUCKET_FOR_JAR=${S3_BUCKET}
export S3_BUCKET_FOR_DATA=${S3_BUCKET}
```
```shell
export ROLE_NAME=emr-on-eks-job-role
export CLUSTER_NAME=emr-on-eks
export REGION=ap-south-1
export VIRTUAL_CLUSTER_NAME=emr-ec2
```
2. Create cluster without nodegroup
```shell
envsubst < ./eks.yaml > ./eks_out.yaml
cat ./eks_out.yaml

eksctl create cluster -f ./eks_out.yaml

```
3. Set source root.
```shell
export SOURCE_ROOT=<path-to-root-of source ~/environment/emr-on-eks>

```
### Setup Execution Role
1. Create an execution role for EMR on EKS. 

This role provides both S3 access for specific buckets as well as full read and write access to the Glue Data Catalog.

```shell
aws iam create-role --role-name ${ROLE_NAME} --assume-role-policy-document '{
      "Version": "2012-10-17",
      "Statement": [
        {
          "Effect": "Allow",
          "Principal": {
            "Service": "elasticmapreduce.amazonaws.com"
          },
          "Action": "sts:AssumeRole"
        }
      ]
    }'
```
```shell
aws iam put-role-policy --role-name ${ROLE_NAME} --policy-name S3Access --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "ReadFromOutputAndInputBuckets",
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::*.elasticmapreduce",
                "arn:aws:s3:::*.elasticmapreduce/*",
                "arn:aws:s3:::aws-data-analytics-workshops",
                "arn:aws:s3:::aws-data-analytics-workshops/*",
                "arn:aws:s3:::'${S3_BUCKET}'",
                "arn:aws:s3:::'${S3_BUCKET}'/*"
            ]
        },
        {
            "Sid": "WriteToOutputDataBucket",
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:DeleteObject"
            ],
            "Resource": [
                "arn:aws:s3:::'${S3_BUCKET}'/*"
            ]
        }
    ]
}'
```
```shell
aws iam put-role-policy --role-name ${ROLE_NAME} --policy-name GlueAccess --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "GlueCreateAndReadDataCatalog",
        "Effect": "Allow",
        "Action": [
            "glue:CreateDatabase",
            "glue:GetDatabase",
            "glue:GetDataBases",
            "glue:CreateTable",
            "glue:GetTable",
            "glue:GetTables",
            "glue:DeleteTable",
            "glue:UpdateTable",
            "glue:GetPartition",
            "glue:GetPartitions",
            "glue:CreatePartition",
            "glue:DeletePartition",
            "glue:BatchCreatePartition",
            "glue:GetUserDefinedFunctions",
            "glue:BatchDeletePartition"
        ],
        "Resource": ["*"]
      }
    ]
  }'
```
```shell
aws iam attach-role-policy --role-name ${ROLE_NAME} --policy-arn arn:aws:iam::aws:policy/CloudWatchLogsFullAccess
```
### Create a namespace on EKS for EMR cluster
```shell
#Create a namespace on EKS for EMR cluster
kubectl create namespace emr-eks-workshop-namespace

# Create Amazon EMR Cluster in EKS eks-fargate namespace
eksctl create iamidentitymapping \
--cluster ${CLUSTER_NAME} \
--namespace emr-eks-workshop-namespace \
--service-name "emr-containers"

# Create Amazon EMR Cluster in EKS emr-eks-workshop-namespace namespace
export VIRTUAL_CLUSTER_EC2=`aws emr-containers create-virtual-cluster \
--name ${VIRTUAL_CLUSTER_NAME} \
--container-provider '{
    "id":   "'"${CLUSTER_NAME}"'",
    "type": "EKS",
    "info": {
        "eksInfo": {
            "namespace": "emr-eks-workshop-namespace"
        }
    }
}'    --query id --output text`

export VIRTUAL_CLUSTER_ID=${VIRTUAL_CLUSTER_EC2}
echo "ID of EC2 Virtual EMR cluster is ${VIRTUAL_CLUSTER_ID}"
# Setup the Trust Policy for the IAM Job Execution Role

aws emr-containers update-role-trust-policy \
--cluster-name ${CLUSTER_NAME} \
--namespace emr-eks-workshop-namespace \
--role-name ${ROLE_NAME}

eksctl utils associate-iam-oidc-provider --cluster ${CLUSTER_NAME} --approve

export EMR_EKS_EXECUTION_ARN=$(aws iam get-role --role-name ${ROLE_NAME} --query Role.Arn --output text)   
```

```shell
aws emr-containers list-virtual-clusters --query "virtualClusters[?id=='$VIRTUAL_CLUSTER_ID']"
```

### Add additional nodegroup if required
```shell
cat << EOF > addnodegroup.yaml
---
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: emr-on-eks-fargate
  region: ap-south-1

managedNodeGroups:
- name: emrnodegroup
  desiredCapacity: 3
  instanceType: m5.xlarge
  ssh:
    enableSsm: true

EOF

eksctl create nodegroup --config-file=addnodegroup.yaml
```
## Submit a Batch job to test
### Job without logging
1. Submit Job
```shell

JOB_RUN_ID=`aws emr-containers start-job-run \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--name spark-pi \
--execution-role-arn ${EMR_EKS_EXECUTION_ARN} \
--release-label emr-6.2.0-latest \
--job-driver '{
    "sparkSubmitJobDriver": {
        "entryPoint": "s3://aws-data-analytics-workshops/emr-eks-workshop/scripts/pi.py",
        "sparkSubmitParameters": "  --conf spark.executor.instances=2 --conf spark.executor.memory=2G --conf spark.executor.cores=1 --conf spark.driver.cores=1"
        }
    }' --query id --output text`
echo ${JOB_RUN_ID}
```
2. View Job Details
```shell
../view-job-by-id.sh ${VIRTUAL_CLUSTER_ID} ${JOB_RUN_ID}

## Get Pods
kubectl get pods --namespace=emr-eks-workshop-namespace
```
### Job with monitoring and logging
1. Submit Job
```shell
JOB_RUN_ID=`aws emr-containers start-job-run \
--virtual-cluster-id ${VIRTUAL_CLUSTER_ID} \
--name spark-pi-logging \
--execution-role-arn ${EMR_EKS_EXECUTION_ARN} \
--release-label emr-6.2.0-latest \
--job-driver '{
    "sparkSubmitJobDriver": {
        "entryPoint": "s3://aws-data-analytics-workshops/emr-eks-workshop/scripts/pi.py",
        "sparkSubmitParameters": " --conf spark.executor.instances=2 --conf spark.executor.memory=2G --conf spark.executor.cores=1 --conf spark.driver.cores=1"
        }
    }' \
--configuration-overrides '{
    "applicationConfiguration": [
      {
        "classification": "spark-defaults", 
        "properties": {
          "spark.driver.memory":"2G"
         }
      }
    ], 
    "monitoringConfiguration": {
      "cloudWatchMonitoringConfiguration": {
        "logGroupName": "/emr-on-eks/eksworkshop-eksctl", 
        "logStreamNamePrefix": "emr-eks-ec2"
      }, 
      "s3MonitoringConfiguration": {
        "logUri": "'s3://"$S3_BUCKET"'/logs/"
      }
    }
}' --query id --output text`

echo Submitted ${JOB_RUN_ID}
```
2. View Job Details
```shell
../view-job-by-id.sh ${VIRTUAL_CLUSTER_ID} ${JOB_RUN_ID}

## Get Pods
kubectl get pods --namespace=emr-eks-workshop-namespace
```
3. View Job logs
```shell
aws s3 ls --recursive s3://${S3_BUCKET_FOR_JAR}/logs/${VIRTUAL_CLUSTER_ID}/jobs/${JOB_RUN_ID}
```
## Submit Streaming Job with Hudi that consumes Kinesis
Refer [Hudi Sample Streaming Job](../HUDI-SAMPLE-KINESIS.md)

## Submit Streaming Job with Hudi that consumes Kafka
Refer [Hudi Sample Streaming Job](../HUDI-SAMPLE-KAFKA.md)

## Submit Batch Job with Glue Catalog
Refer [Batch Job with Glue Catalog](./BATCH-WITH-GLUE-CATALOG.md)

## List /Cancel Job Run
### List Job Runs for a cluster
```shell
./list-jobs.sh <cluster-id/> <RUNNING/FAILED/COMPLETED RUNING is default>
#For Running jobs
./list-jobs.sh ${VIRTUAL_CLUSTER_ID}
#For Failed jobs
./list-jobs.sh ${VIRTUAL_CLUSTER_ID} FAILED
```
### Cancel all running jobs on a cluster
```shell
./cancel-running-jobs.sh <cluster-id>
#
./cancel-running-jobs.sh ${VIRTUAL_CLUSTER_ID}

```
### Get details of a job on the cluster
```shell
./view-job-by-id.sh <cluster-id> <jobid>
#
./view-running-jobs.sh ${VIRTUAL_CLUSTER_ID} <jobid>
```

### Cancel A job on the cluster
```shell
./cancel-job-by-id.sh <cluster-id> <jobid>
#
./cancel-job-by-id.sh ${VIRTUAL_CLUSTER_ID} <jobid>
```
## Cleanup
```shell
../cancel-running-jobs.sh ${VIRTUAL_CLUSTER_ID}
aws emr-containers delete-virtual-cluster --id ${VIRTUAL_CLUSTER_ID}
eksctl get nodegroup --cluster emr-on-eks
eksctl delete nodegroup --config-file ./eks.yaml --approve
eksctl delete nodegroup emrnodegroup --cluster emr-on-eks
eksctl delete cluster -f ./eks.yaml

aws iam detach-role-policy --role-name ${ROLE_NAME} --policy-arn arn:aws:iam::aws:policy/CloudWatchLogsFullAccess
aws iam detach-role-policy --role-name ${ROLE_NAME} --policy-arn arn:aws:iam::aws:policy/AmazonKinesisFullAccess
aws iam detach-role-policy --role-name ${ROLE_NAME} --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess

aws iam delete-role-policy --role-name ${ROLE_NAME} --policy-name GlueAccess
aws iam delete-role-policy --role-name ${ROLE_NAME} --policy-name S3Access

aws iam delete-role --role-name ${ROLE_NAME}
```


