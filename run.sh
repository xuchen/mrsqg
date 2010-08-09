#! /bin/sh

export CLASSPATH=conf:lib/xerces.jar:lib/log4j.jar:lib/opennlp-tools.jar:lib/lingpipe.jar:lib/snowball.jar:lib/maxent.jar:lib/stanford-ner.jar:lib/trove.jar:lib/jwnl.jar:lib/commons-logging.jar:MrsQG.jar:$CLASSPATH

java  -Xms512m -Xmx1500m  com.googlecode.mrsqg.MrsQG
