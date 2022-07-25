# Overview
# Architectural Components
## DMS , MySQL and Kinesis
## Spark Streaming and Apache Hudi
# Logical Architecture
# Deployment Architecture
# Build , Deployment and Run
## Deploy Infrastructure
### Deploy Database, DMS and Kinesis
1. Create a VPC , public and private subnets. 
2. Create a SSH key in the target region and download pem file.    
3. Create a Cloud9 instance in the public subnet of the VPC. Upload SSH keys to Cloud9 and change permission to 400.
4. Create an IAM role for EC2 and add arn:aws:iam::aws:policy/AmazonEC2FullAccess, arn:aws:iam::aws:policy/SecretsManagerReadWrite , arn:aws:iam::aws:policy/IAMFullAccess and arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore roles. Assign the role to EC2 instance of Cloud9. Go to Settings -> AWS Settings and disable "AWS managed temporary credentials". Validate the role by executing following commands -
    ```
    
    aws sts get-caller-identity
    
    ```
 5. Install Java 8 and Maven 3.1+.
```
sudo yum -y update
sudo yum -y install java-1.8.0-openjdk-devel
sudo update-alternatives --config java
sudo update-alternatives --config javac
```
```
sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
sudo yum install -y apache-maven
``` 
6. Create a bucket for cloudformation templates. 
   ```
   export BUCKET_NAME=<>
   aws s3 mb s3://${BUCKET_NAME}
   ```
7. Clone GitHub repository and copy cloudformation templats to S3.
```shell
git clone https://github.com/akshayar/emr-on-eks-samples.git
cd  emr-on-eks-samples 
export SOURCE_CODE_ROOT=`pwd`
aws s3 cp  ${SOURCE_CODE_ROOT}/mysql-cdc-hudi/cfn-templates s3://${BUCKET_NAME}/cft/emr-on-eks/dms/  --recursive --include "*.yaml"
```
8. Find VPC ID, subnet ids where the database , DMS replication instance and EC2 will be created. You can use following command to find the VPC id and subnet id Cloud9. 
```shell
INSTANCE_ID=`wget -q -O - http://169.254.169.254/latest/meta-data/instance-id`
echo ${INSTANCE_ID}
VPC_ID=`aws ec2 describe-instances   --instance-ids ${INSTANCE_ID} --query Reservations[0].Instances[0].VpcId --output text`
aws ec2 describe-subnets --filters "Name=vpc-id,Values=${VPC_ID}" --query Subnets[].[VpcId,AvailabilityZone,CidrBlock,SubnetId] --output text 
```   
8. Deploy to create database, dms and Kinesis
```
aws cloudformation deploy --template-file ${SOURCE_CODE_ROOT}/mysql-cdc-hudi/mysql-cdc.yaml --stack-name dms3 \
--parameter-overrides DBUsername=root DBPassword=Admin123 MySQlVPC=vpc-d002cabb MySQlSubnetA=subnet-7d90f906 \
MySQlSubnetB=subnet-e07553ac MyStreamName=dms S3BucketName=aksh-code-binaries S3KeyPrefix=cft/emr-on-eks/dms \
KinesisVPCEndpointSG=sg-28d5f054 \
--capabilities CAPABILITY_NAMED_IAM

```
### Deploy EMR on EKS Infrastructure
## Start DMS Migration and CDC
## Validate DMS Migration
### Insert Fake Data
### Consume Kinesis Data Stream to validate
## Build and Run Hudi Spark Streaming Job
### Validate Hudi raw data lake by querying from Athena
## Build and Schedule Hudi Spark batch job
### Validate Hudi silver data lake by querying from Athena
# Best Practices
# Conclusion
```
SOURCE_CODE_ROOT=/Users/rawaaksh/code/public-code/emr-on-eks/emr-on-eks-samples

aws cloudformation deploy --template-file ${SOURCE_CODE_ROOT}/mysql-cdc-hudi/mysql-cdc-rds-mysql.yaml --stack-name rds \
--parameter-overrides DBUsername=root DBPassword=Admin123 MySQlVPC=vpc-d002cabb MySQlSubnetA=subnet-7d90f906 MySQlSubnetB=subnet-e07553ac 

aws cloudformation deploy --template-file ${SOURCE_CODE_ROOT}/mysql-cdc-hudi/mysql-cdc-kinesis.yaml --stack-name kinesis \
--parameter-overrides MyStreamName=dms  --capabilities CAPABILITY_NAMED_IAM

aws cloudformation deploy --template-file ${SOURCE_CODE_ROOT}/mysql-cdc-hudi/mysql-cdc-dms.yaml --stack-name dms \
--parameter-overrides DBUsername=root DBPassword=Admin123 MySQlVPC=vpc-d002cabb MySQlSubnetA=subnet-7d90f906 \
MySQlSubnetB=subnet-e07553ac MySQLServer=mydbinstance.cntgfu50evyo.ap-south-1.rds.amazonaws.com MySQLPort=3306 \
DMSSecurityGroup=sg-023a10599bcf1d082 KinesisArn=arn:aws:kinesis:ap-south-1:229369268201:stream/MyStreamName \
IAMRoleArn=arn:aws:iam::229369268201:role/kinesis-IAMRole-DLR2UDGVT427

aws cloudformation deploy --template-file ${SOURCE_CODE_ROOT}/mysql-cdc-hudi/mysql-cdc.yaml --stack-name dms3 \
--parameter-overrides DBUsername=root DBPassword=Admin123 MySQlVPC=vpc-d002cabb MySQlSubnetA=subnet-7d90f906 \
MySQlSubnetB=subnet-e07553ac MyStreamName=dms S3BucketName=aksh-code-binaries S3KeyPrefix=cft/emr-on-eks/dms \
KinesisVPCEndpointSG=sg-28d5f054 \
--capabilities CAPABILITY_NAMED_IAM


aws s3 cp --recursive  ${SOURCE_CODE_ROOT}/mysql-cdc-hudi/ s3://aksh-code-binaries/cft/emr-on-eks/dms/ 


```