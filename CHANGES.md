CHANGELOG
====

## 6.9.12

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Juno (4.2) or above |
| TestNG M2E Integration (Optional) | M2E 1.5 or above |

* issue #238: fixed the toolbar position on Eclipse Neon

## 6.9.11

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Juno (4.2) or above |
| TestNG M2E Integration (Optional) | M2E 1.5 or above |
*NOTE: Technically, 'TestNG for Eclipse' still work on Eclipse Helio and above, but since this version, we're dropping support for Eclipse 3.x.*

* Fix issue #243: Eclipse: Can't recognize runtime TestNG version
* Feature #224: remove option "Use Project TestNG jar", to always use Project TestNG
* PR #218: use the new [testng-remote](https://github.com/testng-team/testng-remote)
  The new testng-remote supports json protocol to be communicating between variant version of testng.
  This should fix issue #91 thoroughly.
* Issue #98: Rerun test method (declared in superclass) should use same class as before. (@aledsage)
* Fix issue 70: isTest looks at superclass methods. (@aledsage)
* Feature #232: show the git revision info the plugin being build against, for future diagnose purpose

## 6.9.10

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Helio (3.6) or above |
| TestNG M2E Integration (Optional) | M2E 1.5 or above |

* Feature #188: Add M2E integrtion support
* Fixed issue #211 jvm args are appended two times
* Fixed issue #206: lauching test is failing with non maven projects
* Fixed issue #41: improve the speed when browse Tests with JDT Search Engine
* testng #912: update testng-remote jar with serialVersionUUID explicitly set
* [travis] Preserve Maven/Tycho cache between builds
* Fixed issue #198: can't find phantomjs on Windows
* issue #42: option to disable/enable show the TestNG part name as the test suite name

## 6.9.9

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Helio (3.6) or above |

* feature #168: running TestNG with the 'argLine' of maven-surefire-plugin from pom.xml by default
* Fixed issue #60: Stop-button is not working
* Fixed issue #32 "dependsOnMethods" not resolved when using fully qualified method name
* Fixed issue #19: TestRunner not remembering view orientation

## 6.9.8

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Helio (3.6) or above |

* issue #91: SocketException: Software caused connection abort
* issue #42: Save space in TestNG panel
* PR #174: fixed testng issue #820 "After getting update tesng (6.9) in my eclipse IDE, Not able to run testng.xml"
* issue #167: Distinguish test class instances when using Factory

## 6.9.5

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Helio (3.6) or above |

* Issue/155: Timeout while trying to contact RemoteTestNG
* PR #128: fix bug: eclipse throw MalformedByteSequenceException when excute method. (@liefdiy)



## 5.9.0 and OLDER

TESTNG-239  	 Copy Exception Message
    Added MessageCopyAction, refactoring TraceCopyAction so both are minor variations on AbstractTraceAction, 
    so that a right click on the failure trace offers the option of copying only the trace.
    revision 93.

Rev 95: "method launcher name when created from outline view is no longer null"

Rev 96 "rename variable to keep class conventions and reduce confusion between it 
and a local method name, thus removing the method launcher's inclination 
to lose its method name"  src/main/org/testng/eclipse/ui/util/ConfigurationHelper.java

Rev 97 "Better detection of end of exception message" FailureTrace.java

Rev.98
Refactored TestHierarchyTab and FailureTab so that they use nearly the same code,
putting in their own labels, except that for the FailureTab the test items are 
not added to the displayed tree until they are known to fail.
Fixes TESTNG-245     
  Unify 'All Tests' and 'Failed Tests' with the only difference being filtering for failures.
  
Rev.101
  Fixes TESTNG-246  eclipse plugin needs automated build
  
Rev.104
  Dan Fabulich's patches for testng184 and testng233  
