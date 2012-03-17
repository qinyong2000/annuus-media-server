== Annuus Media Server(AMS)
AMS is an Open Source Flash Media Server written in Java.
it now supports the following features: 
 1.Rtmp Protcol 
 2.Http Protcol 
 3.AMF0/AMF3 
 4.Streaming Audio/Video (FLV, MP4(H264/AAC), F4V) 
 5.Recording Live Streams (FLV) 
 6.Live Stream Publishing(VP6, H264)
 7.Live Stream Replication(TCP unicast and UDP multicast) 

== Setting
The name of configure file is server.conf. 
1. worker setting
server.dispatchers=4       ;number of worker thread to read packet data from client
server.workers=16          ;number of worker thread to handle http or rtmp request
server.command.port=55555  ;command port for server control


2. http setting
http.host=0.0.0.0   ;IP address of http server
http.port=8080      ;listen port of http server
http.root=www       ;root path of html file 

3. rtmp setting
rtmp.host=0.0.0.0   ;IP address of rtmp server
rtmp.port=1935      ;listen port of rtmp server
rtmp.root=video     ;root path of video file

4. replication setting
replication.unicast.host=0.0.0.0        ;IP address of TCP replication
replication.unicast.port=1936           ;listen port of TCP replication
replication.unicast.slaves=192.168.3.3,192.168.3.100    ;slave server IP address, spread by comma
replication.multicast.host=0.0.0.0      ;IP address of multicast replication
replication.multicast.port=5000         ;listen port of multicast replication
replication.multicast.group=239.0.0.0   ;group IP address of multicast

== Run Server
./run.sh
or
run.bat

== Release
version 0.0.1
 the first release.
version 0.1.0
 fix some AMF3 bugs.
 rewrite ByteBufferFactory class with slab memory management mechanism.
version 0.1.1
 fix a buffer allocation bug.
version 0.2.0
 add H264/AAC streaming.
 fix some bugs.
 refactoring.
  
== Author
 qinyong2000@gmail.com
 