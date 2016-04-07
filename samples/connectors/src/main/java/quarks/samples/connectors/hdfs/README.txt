Sample HDFS(Hadoop File System) connector topology applications.

The HDFS writer application writes a stream's tuples to HDFS.

The HDFS reader application watches a HDFS directory for files and reads their
contents into a stream of tuples.

see scripts/connectors/hdfs/README to run them

HdfsWriterApp.java - the HDFS writer application topology
HdfsReaderApp.java - the HDFS reader application topology