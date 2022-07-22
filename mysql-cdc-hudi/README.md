```
SOURCE_CODE_ROOT=/Users/rawaaksh/code/public-code/emr-on-eks/emr-on-eks-samples

aws cloudformation deploy --template-file ${SOURCE_CODE_ROOT}/mysql-cdc-hudi/mysql-cdc-rds-mysql.yaml --stack-name rds \
--parameter-overrides DBUsername=root DBPassword=Admin123 MySQlVPC=vpc-d002cabb MySQlSubnetA=subnet-7d90f906 MySQlSubnetB=subnet-e07553ac 
```