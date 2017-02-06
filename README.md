# TVM - Temporal Vector Map
TVM is an open source, GPL licensed, framework that allows a user to accurately localize 
indoors using Wi-Fi Received Signal Strength (RSS) Fingerprints. The framework effectively
obfuscates intelligently its localization requests using the notion of k-anonymity.

## Motivation
Indoor Positioning Systems (IPS) have recently received considerable attention (e.g., see https://anyplace.cs.ucy.ac.cy), mainly because GPS is unavailable in
indoor spaces and consumes considerable energy. On the other hand, predominant Smartphone OS localization subsystems currently
rely on server-side localization processes, allowing the service provider to know the location of a user at all times. In this paper, we
propose an innovative algorithm for protecting users from location tracking by the localization service, without hindering the provisioning
of fine-grained location updates on a continuous basis.

## Overview
Our proposed Temporal Vector Map (TVM) algorithm, allows a user to accurately
localize by exploiting a k-Anonymity Bloom (kAB) filter and a bestNeighbors generator of camouflaged localization requests, both of
which are shown to be resilient to a variety of privacy attacks. 

Spitfire is based on a IEEE TKDE'15 journal paper and a respective IEEE ICDE'16 poster paper. 
Its source code can be downloaded at https://github.com/dmsl/tvm/

* "Privacy-Preserving Indoor Localization on Smartphones", Andreas Konstantinidis, Georgios Chatzimilioudis, Demetrios Zeinalipour-Yazti, Paschalis Mpeis, Nikos Pelekis and Yannis Theodoridis IEEE Transactions on Knowledge and Data Engineering (TKDE '15), Volume 27, Pages: 3042-3055, 2015.
http://www.cs.ucy.ac.cy/~dzeina/papers/tkde15-tvm.pdf

* "Privacy-Preserving Indoor Localization on Smartphones (Extended Abstract)", Andreas Konstantinidis, Georgios Chatzimilioudis, Demetrios Zeinalipour-Yazti, Paschalis Mpeis, Nikos Pelekis and Yannis Theodoridis "Proceedings of the 2016 IEEE 32nd International Conference on Data Engineering" (ICDE '16), IEEE Computer Society, Pages: 1470--1471, Helsinki, Finland, ISBN: 978-1-5090-2020-1, 2016.
http://www.cs.ucy.ac.cy/~dzeina/papers/icde16-tvm-poster.pdf

## Full Publications: 
http://tvm.cs.ucy.ac.cy/publications.php

Enjoy TVM!

The TVM Team
	
## Components 

### Source code
Sources comply with Google's Java style for AOSP:

* `google-java-format --aosp file(s)`

#### Server
The HBase Source code for the TVM Server. 

Server accepts localization requests encoded as bloom filters by the clients.
After querying the database the server builds partial radiomaps for the actual
area of the user, as well for the false-positives matching with the bloom filter.
As a result, the server cannot know in which area of all the matches the user
actually is.

#### Client
The Source code for a demo TVM Android Client.

The client sends localization requests after encoding them into bloom filters to obfuscate its location from the server. By controlling the size of the bloom filter, the client can control the level of its privacy.

NOTE: The Server machine used during our experimental evaluation was accessed within UCY's virtual private network, and currently it is offline. It is the default machine that the Client sources will try to communicate to.

### Datasets
The Datasets used for the experimental evaluation are available through the following URLs:
* CRAWDAD: http://tvm.cs.ucy.ac.cy/tvm/datasets/crawdad.rar
* KIOS: http://tvm.cs.ucy.ac.cy/tvm/datasets/kios.txt
* UCY: http://tvm.cs.ucy.ac.cy/tvm/datasets/ucy.txt

## Anyplace
Anyplace is an MIT open source indoor information service offering GPS-less localization, navigation and search inside buildings using ordinary smartphones. Anyplace offers:: IMU/IP/WiFI localization, crowdsourcing, web modeling through Architect, JSON API, multi-OS smartphone support. It can be downloaded here: 
http://anyplace.cs.ucy.ac.cy/

# [Credits](./CREDITS.md)
* Paschalis Mpeis [paschalis.mp](http://paschalis.mp/) 
      - Implemented TVM Server and Client.
* Giannis Evagorou [doc.ic.ac.uk/~ge14](https://www.doc.ic.ac.uk/~ge14/)
      - Contributed in experimental evaluation.
* Andreas Konstantinidis [cs.ucy.ac.cy/~akonstan](http://www.cs.ucy.ac.cy/~akonstan/)
      - Project supervision.
* Demetris Zeinalipour [cs.ucy.ac.cy/~dzeina](https://www.cs.ucy.ac.cy/~dzeina/)
      - Project supervision and co-ordination.

Licence
=======

TVM: A privacy-preserving indoor localization algorithm.

Copyright (c) 2016, Data Management Systems Lab (DMSL), University of Cyprus. All rights reserved.

TVM is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
