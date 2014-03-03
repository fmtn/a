a
=

A is an ActiveMQ testing/admin utility

Used to send, browse and put messages on queues.

```
Usage information.

usage: java -jar a.jar [-b <arg>] [-c <arg>] [-e <arg>] [-g] [-H
       <property=value>] [-j] [-n] [-o <arg>] [-p <arg>] [-r <arg>] [-t
       <arg>]
 -b,--broker <arg>     URL to broker. defaults to: tcp://localhost:61616
 -c,--count <arg>      A number of messages to browse,get or put (put will
                       put the same message <count> times). 0 means all
                       messages.
 -e,--encoding <arg>   Encoding of input file data. Default UTF-8
 -g,--get              Get a message from destination
 -H <property=value>   use value for given property. Can be used several
                       times.
 -j,--jms-headers      Print JMS headers
 -n,--non-persistent   Set message to non persistent.
 -o,--output <arg>     file to write payload to. If multiple messages, a
                       -1.<ext> will be added to the file. BytesMessage
                       will be written as-is, TextMessage will be written
                       in UTF-8
 -p,--put <arg>        Put a message. Specify data. if starts with @, a
                       file is assumed and loaded
 -r,--reply-to <arg>   Set reply to destination, i.e. queue:reply
 -t,--type <arg>       Message type to put, [bytes, text] - defaults to
                       text
```

Example1. Put message with payload "foobar" to queue q on local broker:
    
    $a -p "foobar" q

Example2. Put message with payload of file foo.bar to queue q on local broker, also set a property
    
    $a -p "@foo.bar" -Hfoo=bar q

Example3. Browse five messages from queue q.
 
    $a -c 5 q

Example4. Put 100 messages to queue q (for load test etc)

    $a -p "foobar" -c 100 q

Example5. Get message from queue and show JMS headers
    
    $a -g -j q

Example6. Put file foo.bar as a byte message on queue q
    
    $a -p "@foo.bar" -t bytes q

Example7. Put file foo.bar as text message on queue q, with encoding EBCDIC CP037 (any charset known on server/JVM should work)
    
    $a -p "@foo.bar" -e CP037 q

#Build

    $mvn install

#Make the jar runnable from shell as in examples:
1. copy the jar target/a-VERSION-with-dependencies.jar to someplace. i.e. ~/bin/
2. create a file called "a" on your path (~/bin/a or what have you)
```  
#!/bin/sh
java -jar ~/bin/a-1.0.0-SNAPSHOT-jar-with-dependencies.jar "$@"
```
3. chmod +x a
4. Run a from any place.
