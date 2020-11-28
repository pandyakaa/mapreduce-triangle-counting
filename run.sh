rm -rf temp
/usr/local/hadoop/bin/hadoop com.sun.tools.javac.Main TriangleCount.java
jar cf TriangleCount.jar TriangleCount*.class

/usr/local/hadoop/bin/hadoop jar TriangleCount.jar TriangleCount /home/aptanagi/Documents/college/PAT/mapreduce-triangle-counting/example.net /home/aptanagi/Documents/college/PAT/mapreduce-triangle-counting/output

cat output/*
