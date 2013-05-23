#!/bin/sh
TAXONOMY_SCRIPT="../scripts/VTO_v2.xml"
#TAXONOMY_SCRIPT="../scripts/justNCBI.xml"
java -Xmx8G -cp .:../jars/bbop.jar:../jars/jena-2.6.3.jar:../jars/junit.jar:../jars/log4j-1.2.15.jar:../jars/obo.jar:../jars/owlapi-bin.jar:../jars/mysql-connector-java-5.1.21.bin.jar:../jars/VTOTool.jar org.nescent.VTO.VTOTool $TAXONOMY_SCRIPT
