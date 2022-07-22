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
```