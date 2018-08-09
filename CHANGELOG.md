# Changelog

## [unreleased]
### Fixed
- #30 Default count 0 fixed

## [1.4.7] - 2018-06-23
## Added
- Initial load features. Updated with new Rhino JS engine.

## [1.4.5] - 2018-06-01

### Added
- #26 Logo (by @nunojesus)
- #27 Support for boolean propereties

### Fixed
- #24 removed annoying printout during message dumping 

## [1.4.4]

### Added
- #23 Support for legacy ActiveMQ versions

### Changed
- #22 Script transformation of messages during copy and move

## [1.4.3] - 2018-03-27

### Fixed
- #21 Object properties that cannot be deserialized are now siently ignored. This gives better support for Azure Service Bus.

## [1.4.2] - 2017-12-02

### Changed
- #18 The start script in distribution does no longer need to be edited, just put on path in the same folder as the jar-filer. Suggestion from @kutzi

### Fixed
- #17 Queues with slashes are now supported
- #19 Argument -D renamed to -E to avoid confusion with Java params. 
- #20 Incorrect start script fixed

## [1.4.1] - 2017-09-05

### Added
- New command options: version and jms-type
- Automatic build of dist

### Changed
- Script transformation of messages during read-folder and put.

## [1.4.0] - 2017-05-30

### Added
- Support for Dump and Restore of queue content for data migrations between brokers and JMS providers.
- Support for JavaScript transformations during Dump and Restore.

### Removed
- Removed support for Java 1.5 and 1.6.

### Fixed
- Fix of bug that prevented putting BytesMessages

## [1.3.2] - 2017-03-11

### Added
- Litmited support for Azure Service Bus
- Support to send MapMessage

### Changed
- New client versions of ActiveMQ (5.14.3) and Artemis (1.4.0)

## [1.3.1] - 2016-09-19

### Added
- CorrelationID can be set using -D
- MapMessage can be displaed
- Debian package assembler included in build process

### Changed
- ActiveMQ client version updated to 5.14.0

## [1.3.0] - 2016-04-12

### Added
- Support to set Int and Long properties when sending messages
- Advisiory messages are handled and will be printed if listening to topic ActiveMQ.Advisory.>
- ActiveMQ Artemis protocol support
- List queues on the broker (OpenWire, experimental)

### Fixed
- Number of messages in move command

## [1.2.0] - 2015-10-24

### Changed
- Change of group-id/package of artifact.
- Update AMQP client lib to QPid proton 0.32
- Update of ActiveMQ lib to 5.12.1

## [1.1.0] - 2015-01-29

### Added 
- AMQP 1.0 support
- 
### Changes
- Binary size reduced to half
- Logback logging instead of log4j

### Fixed
- Corrected output after copy/move

## [1.0.3] - 2014-10-04

### Added
- JMSPriority can be set

### Changed
- Enhanced documentation on copy and TLS/SSL

## [1.0.2] - 2014-08-13

### Added
- Support for authentication via username and password
- Support for subscribing to topics

## [1.0.1] - 2014-03-12

### Changed
- wait on get operation can be specified.

### Fixed
- Bug fix that allows connection to remote broker

## [1.0.0] - 2014-03-04

### Added
- A



