#!/bin/sh
TAXONOMY_SCRIPT="../scripts/PBDBbuild_Jan_2013.xml"
java -Xmx2500M -cp .:../jars/bbop.jar:../jars/jena-2.6.3.jar:../jars/junit.jar:../jars/log4j-1.2.15.jar:../jars/obo.jar:../jars/owlapi-bin.jar:../jars/mysql-connector-java-5.1.21.bin.jar:../jars/VTOTool.jar org.nescent.VTO.VTOTool $TAXONOMY_SCRIPT
