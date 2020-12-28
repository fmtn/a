# A

<img src="https://raw.githubusercontent.com/fmtn/a/master/logo/Logo.png" width="250">

A is a JMS testing/admin utility specialized for ActiveMQ.

Used to send, browse and put messages on queues.
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://github.com/fmtn/a/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/fmtn/a)

```bash
usage: java -jar a-<version>-with-dependencies.jar [-A] [-a] [-B
       <property=value>] [-b <arg>] [-C <arg>] [-c <arg>] [-E <arg>] [-e
       <arg>] [-F <arg>] [-f <arg>] [-g] [-H <property=value>] [-I
       <property=value>] [-i <arg>] [-J <arg>] [-j] [-L <property=value>]
       [-l] [-M <arg>] [-n] [-O] [-o <arg>] [-P <arg>] [-p <arg>] [-R
       <arg>] [-r <arg>] [-S <arg>] [-s <arg>] [-T] [-t <arg>] [-U <arg>]
       [-v] [-w <arg>] [-X <arg>] [-x <arg>] [-y <arg>]
 -A,--amqp                     Set protocol to AMQP. Defaults to OpenWire
 -a,--artemis-core             Set protocol to ActiveMQ Artemis Core.
                               Defaults to OpenWire
 -B <property=value>           use value for given Boolean property. Can
                               be used several times.
 -b,--broker <arg>             URL to broker. defaults to:
                               tcp://localhost:61616
 -C,--copy-queue <arg>         Copy all messages from this to target.
                               Limited by maxBrowsePageSize in broker
                               settings (default 400).
 -c,--count <arg>              A number of messages to browse,get,move or
                               put (put will put the same message <count>
                               times). 0 means all messages.
 -E,--correlation-id <arg>     Set CorrelationID
 -e,--encoding <arg>           Encoding of input file data. Default UTF-8
 -F,--jndi-cf-name <arg>       Specify JNDI name for ConnectionFactory.
                               Defaults to connectionFactory. Use with -J
 -f,--find <arg>               Search for messages in queue with this
                               value in payload. Use with browse.
 -g,--get                      Get a message from destination (which delete 
                                    messages on server queue)
 -H <property=value>           use value for given String property. Can be
                               used several times.
 -I <property=value>           use value for given Integer property. Can
                               be used several times.
 -i,--priority <arg>           sets JMSPriority
 -J,--jndi <arg>               Connect via JNDI. Overrides -b and -A
                               options. Specify context file on classpath
 -j,--jms-headers              Print JMS headers
 -L <property=value>           use value for given Long property. Can be
                               used several times.
 -l,--list-queues              List queues and topics on broker (OpenWire
                               only)
 -M,--move-queue <arg>         Move all messages from this to target
 -n,--non-persistent           Set message to non persistent.
 -O,--openwire                 Set protocol to OpenWire. This is default
                               protocol
 -o,--output <arg>             file to write payload to. If multiple
                               messages, a -1.<ext> will be added to the
                               file. BytesMessage will be written as-is,
                               TextMessage will be written in UTF-8
 -P,--pass <arg>               Password to connect to broker
 -p,--put <arg>                Put a message. Specify data. if starts with
                               @, a file is assumed and loaded
 -R,--read-folder <arg>        Read files in folder and put to queue. Sent
                               files are deleted! Specify path and a
                               filename. Wildcards are supported '*' and
                               '?'. If no path is given, current directory
                               is assumed.
 -r,--reply-to <arg>           Set reply to destination, i.e. queue:reply
 -S,--transform-script <arg>   JavaScript code (or @path/to/file.js). Used
                               to transform messages with the dump
                               options. Access message in JavaScript by
                               msg.JMSType = 'foobar';
 -s,--selector <arg>           Browse or get with selector
 -T,--no-transaction-support   Set to disable transactions if not
                               supported by platform. I.e. Azure Service
                               Bus. When set to false, the Move option is
                               NOT atomic.
 -t,--type <arg>               Message type to put, [bytes, text, map] -
                               defaults to text
 -U,--user <arg>               Username to connect to broker
 -v,--version                  Show version of A
 -W,--batch-file <arg>         Line separated batch file. Used with -p to
                                produce one message per line in file. Used
                                together with Script where each batch line
                                can be accessed with variable 'entry'
 -w,--wait <arg>               Time to wait on get operation. Default 50.
                               0 equals infinity
 -X,--restore-dump <arg>       Restore a dump of messages in a
                               file,created with -x. Can be used with
                               transformation option.
 -x,--write-dump <arg>         Write a dump of messages to a file. Will
                               preserve metadata and type. Can  be used
                               with transformation option. Warning! Will consume queue!
 -y,--jms-type <arg>           Sets JMSType header
 -z,--ttl <arg>                sets JMSExpiry
```

## A quick note about ActiveMQ maxBrowsePageSize limit

ActiveMQ 5 is limited how many messages can be browsed/read from a queue without consuming them.
This is limited by the setting - maxBrowsePageSize in broker, default is 400. This is a server side setting! This makes it impossible to use browse and copy commands for more than 400 or whatever value is configured at a time. Increasing this value may affect broker memory consumption. For other JMS compliant brokers, this limit may not exists or other limits may apply instead.

## Exampels

Example 1. Put message with payload "foobar" to queue q on local broker:

`$a -p "foobar" q`

Example 2. Put message with payload of file foo.bar to queue q on local broker, also set a property

`$a -p "@foo.bar" -Hfoo=bar q`

Example 3. Browse five messages from queue q.

`$a -c 5 q`

Example 4. Put 100 messages to queue q (for load test etc)

`$a -p "foobar" -c 100 q`

Example 5. Get message from queue and show JMS headers

`$a -g -j q`

Example 6. Put file foo.bar as a byte message on queue q

`$a -p "@foo.bar" -t bytes q`

Example 7. Put file foo.bar as text message on queue q, with encoding EBCDIC CP037 (any charset known on server/JVM should work)

`$a -p "@foo.bar" -e CP037 q`

Example 8. Read all XML files in a folder input an put them on queue q. Files are deleted afterwards.

`$a -R "input/*.xml" q`

Example 9. Put file foo.json as map message on queue q

`$a -p "@foo.json" -t map q`

Example 10. Put a map message on a queue using json format.

`$a -p "{\"a\":\"a message tool\"}" -t map q`

Example 11. Backup/dump messages on a queue with metadata

`$a -x dump.json q`

Example 12. Restore dump of messages with metadata to a queue

`$a -X dump.json q2`

Example 12. Restore and transform messagse

`$a -X dump.json -S @transform.js q2`

## Use AMQP 1.0

A defaults to ActiveMQ default protocol, OpenWire. You can also use AMQP 1.0.
In theory, it should work with all AMQP 1.0 compliant brokers. It does not work with older versions of AMQP.

`$a -A -b "amqp://guest:guest@localhost:5672" -p "foobar" q`

## Azure Service Bus

Service Bus supports AMQP 1.0 so it's possible to use A to connect.
However, it does not support transactions, so the -T option has to be set to deal with that.

To connect, you will need a "username" and "password". The username will be the "shared access policy name".
The password is the URL-encoded key for that policy. These are found in the Azure portal.

Example command to send a message to Azure Service Bus:

`$a -A -T -b "amqps://mypolicyname:iAkywS...@mynamespace.servicebus.windows.net" -p "Test msg" q`

A word of warning! There are some features not working with AMQP 1.0 in Service Bus. Some of which are mandatory to support the JMS API fully.
This means some of the features of A will not work - or behave strangely.

## Use Artemis Core

Use Artemis core protocol (HornetQ) with the -a option.

`$a -a -b "tcp://localhost:61616" -p "foobar" q`

Please note that this won't auto deploy the queue in current versions of Artemis. Using OpenWire will autodeploy the queue.

## Use JNDI to connect

To connect in a protocol agnostic way, you can specify a JNDI file that points out the JMS provider and settings.

Simply create a jndi.properties file "at classpath". Then link to it jusing the -J (--jndi) option. Please name your
ConnectionFactory "connectionFactory". Otherwise, the name has to be supplied using the -F (--jndi-cf-name) option.

`$a -J jndi.properties -p "foobar" q`

This way, you can even connect to non ActiveMQ/AMQP brokers. You simply need to provide a JNDI config and the client at classpath.

## Build

If you want to build the project.

`$mvn clean install`

However, it is probably easiest to simply build a Docker container.

## Download

Download the distribution from the latest release.
<https://github.com/fmtn/a/releases/latest>

## Install in Unix environment

1. Unzip distribution somewhere
2. Make sure the extracted folder is on path.
3. chmod +x a
4. Run a from any place.

## Install in Windows environment

1. Unzip distribution somewhere
2. Make sure the extracted folder is on path.
3. Run a.bat from any place.

## Use with docker

There is a Docker file with the project. You can build a Docker image and use A from Docker.

```bash
    docker build -t a:latest .
    docker run --rm a:latest a -p "foobar" q
```

You can also use prebuilt docker images.

```bash
    docker run --rm fmtn/a-util a -p "foobar" q 
```

Please note that you need to pass the entire command to the docker run

The default hostname has been replaced with `host.docker.internal` as the original hostname `localhost` points to a location within the docker container. If the broker is not on the docker host, the actual broker hostname still needs to be specified as usual. The hostname of the broker may vary depending on the container environment, Kubernetes, Docker Compose, plain vanilla Docker or what have you.

## Use SSL

Given you have a truststore and a keystore in JKS format, you can edit your a start script, or run it manually like this.
Note that the -Djavax parameters has to come before -jar.

```bash
java -Djavax.net.ssl.keyStore=/Users/petter/client.jks -Djavax.net.ssl.keyStorePassword=password -Djavax.net.ssl.trustStore=/Users/petter/truststore.jks -Djavax.net.ssl.trustStorePassword=password -jar a-1.5.0-jar-with-dependencies.jar -b ssl://example.org:61618 MY.QUEUE 
```

## Listing queues

Listing queues only works for ActiveMQ 5 brokers with Advisory messages not deactivated. Since the ActiveMQ client get a list of queues async, the functionallity to list queues may not work very well on some systems. Try these things if you think you got bad queue lists for your broker.

1. Add more wait time. Defaults to 50ms but you may need much more. Try add `-w 10000` and check your results.
2. If you still have problems, try to subscribe to the Advisory manually to check that it works. Something like this: `a -g topic://ActiveMQ.Advisory.Queue -c 0 -w 0`

## Apply transformations

Using the -S command, a JavaScript transformation can be supplied that will run on each message. The purpose of this feature is to deal with poison-messages that has to be fixed "on-the-fly", removing sensitive data from messages before exporting them from production to a development environment, or to generally help during migrations.

The script is used to modifiy the `msg` variable that will be written or restored.

Example: `msg.JMSPriority = 2;` to change JMS priority of each message.

The `msg.body` parameter depends on `msg.type`. If type is TextMessage, then msg.body is a simple String that can altered in any way. However, if type is BytesMessage, `msg.body` will be a Base64 encoded byte-array which is not convenient in JavaScript. ObjectMessage bodies are also Base64 encoded, but can't be decoded/encoded. Other message types are not yet implmenteted for dump/restore and transformations.

To deal with a BytesMessage use

`msg.encode('Some string', 'UTF-8');`

and

`var contentAsString = msg.decode('UTF-8');`

This can be powerful, for instance, convert TextMessages to BytesMessages:

```java
    // TextMessage to BytesMessage encoded as UTF-8
    msg.type = 'BytesMessage';
    msg.encode(msg.body, 'UTF-8');
```

or set some message property that is missing

```java
    msg.stringProperties.put('foo', 'bar');
```

## Batch files

If you want to send a large amount of similar messages, where only a small value is alterd. You can use the batch command -W

So, create a file where all those different values are, like id:s, names or whatnot. One entry per line.
batch.txt:

```text
    id1
    id2
    id3
```

Then use a script together with put, like this:

`a -p "<xml>PLACEHOLDER</xml>" -S "msg.body=msg.body.replace('PLACEHOLDER',entry);" -W /path/to/batch.txt SOME.QUEUE`

will produce three messages on SOME.QUEUE.

```xml
    <xml>id1</xml>
    <xml>id2</xml>
    <xml>id3</xml>
```

Using -W is much faster than invoking A for each message, since it does not require a reconnection per message.
