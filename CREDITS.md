# Credits:
* Paschalis Mpeis [paschalis.mp](http://paschalis.mp/) 
      - Implemented TVM Server and Client.
* Giannis Evagorou [doc.ic.ac.uk/~ge14](https://www.doc.ic.ac.uk/~ge14/)
      - Contributed in experimental evaluation.
* Andreas Konstantinidis [cs.ucy.ac.cy/~akonstan](http://www.cs.ucy.ac.cy/~akonstan/)
      - Project supervision.
* Demetris Zeinalipour [cs.ucy.ac.cy/~dzeina](https://www.cs.ucy.ac.cy/~dzeina/)
      - Project supervision and co-ordination.

# Source code contributions:
* [Julia Metochi](http://www.cs.ucy.ac.cy/~jmetoc01/)
   - Authored [Client: Heading.java](./Client/app/src/main/java/cy/ac/ucy/cs/tvm/tvm/Heading.java)

## Open Source libraries:

### Murmur2 Java implementation:
* Originally ported to Java by Andrzej Bialecki (ab at getopt org).
* License: Apache License, Version 2.0
* Files:
      - [Client: Murmur2.java](./Client/app/src/main/java/cy/ac/ucy/cs/tvm/Bloomfilter/Murmur2.java)
      - [Server: Murmur2.java](./Server/src/java/cy/ac/ucy/dmsl/vectormap/paschalis/bloom/Murmur2.java)
* URL: [Apache Mahoot](https://github.com/apache/mahout)

### JenkinsHash
* Written by Bob Jenkins, 1996
* Ported to Java by [Gray Watson](http://256.com/gray/)
* Files:
      - [Client: JenkinsHash.java](./Client/app/src/main/java/cy/ac/ucy/cs/tvm/Bloomfilter/JenkinsHash.java)
      - [Server: JenkinsHash.java](./Server/src/java/cy/ac/ucy/dmsl/vectormap/paschalis/bloom/JenkinsHash.java)
