echo "Querying Running Clusters "
aws emr-containers list-virtual-clusters \
--query 'virtualClusters[?state==`RUNNING`]'