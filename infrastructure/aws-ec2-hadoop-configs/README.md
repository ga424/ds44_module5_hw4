# AWS EC2 Hadoop Cluster Configuration

This folder captures the working configuration for my four-node Hadoop cluster deployed on AWS EC2.

## Cluster Layout

- master: NameNode, SecondaryNameNode, ResourceManager
- slave1: DataNode, NodeManager
- slave2: DataNode, NodeManager
- slave3: DataNode, NodeManager

## Key Hadoop Configuration Files

Each node folder includes:

- core-site.xml
- hdfs-site.xml
- mapred-site.xml
- yarn-site.xml
- workers
- hadoop-env.sh
- hosts

## Validation Files

Each node folder also includes validation outputs such as:

- hostname.txt
- java-version.txt
- hadoop-version.txt
- jps.txt
- disk-usage.txt
- block-devices.txt

The master folder also includes:

- hdfs-report.txt
- yarn-nodes.txt

## Security Note

Private SSH keys, AWS key pairs, `.pem` files, and other credentials should not be committed to this repository.
