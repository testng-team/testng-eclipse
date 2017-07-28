Change Log
====

## 6.12

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Juno (4.2) or above |
| TestNG M2E Integration (Optional) | M2E 1.5 or above |

* PR #315: Debundle Guava from the plugin. (@mbooth101)
* PR #319: Update build machinery to latest version of tycho 1.0.0 (@mbooth101)
* Feature #287: Provide testng in the update site (aka, de-bundle TestNG jar from the plugin). (@mschreiber & Nick Tan)
* Fixed #336: if m2e integration is installed, reportNG listener with spaces in title breaks testNG. (@borisivan & Nick Tan)
* Fixed #344: The dependonMethod is not work when i dependon a different class method，this issue seems have been fixed in 6.9.9，but now i still have this problem
* Fixed #104: testng-eclipse-plugin run configurations not check dependencies
* Fixed #346: the first launch on test class is a little bit slow

## 6.11

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Juno (4.2) or above |
| TestNG M2E Integration (Optional) | M2E 1.5 or above |

* fixed #301: UI typo: Serilization -> Serialization
* feature #300: testng eclipse plugin results view should allow the user to copy the test data that is displayed for test iterations
* fixed #279: java.lang.SecurityException: class "org.osgi.framework.BundleException"'s signer information does not match signer information of other classes in the same package
* feature #102: Template XML file should should offer workspace-related location
* feature #152: add more templates for quickly adding new method for 'setup' and 'teardown'
* feature #154: Add Eclipse Favorites - for static import of 'org.testng.Assert.*'

## 6.10

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Juno (4.2) or above |
| TestNG M2E Integration (Optional) | M2E 1.5 or above |

* fixed #284: Reference to undefined variable env.DOMAIN_PATH when launch the test.
* fixed https://github.com/cbeust/testng/issues/1209: revert PR #252 for issue #251; to support `<attachClasses>true</attachClasses>`, please install '[Eclipse IDE for Java EE Developers](http://www.eclipse.org/downloads/packages/)' package, or the [m2e-wtp](https://www.eclipse.org/m2e-wtp/) plugin to be more specific. 
* fixed #294: Unable to launch TestNG Test - ClassCastException: org.eclipse.jdt.core.dom.MarkerAnnotation cannot be cast to org.eclipse.jdt.core.dom.SingleMemberAnnotation
* fixed #298: IConfigurationListener was not loaded when running Test in Eclipse Plugin

## 6.9.13

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Juno (4.2) or above |
| TestNG M2E Integration (Optional) | M2E 1.5 or above |

* #268: Add a rerun test key command/shortcut
* fixed issue #129: Results View not synchronizing properly
* https://github.com/testng-team/testng-remote/issues/33: add support for TestNG versions in [6.0, 6.5.1)
* fixed https://github.com/cbeust/testng/issues/455: Optional parameter is not initialized properly
* fixed #273: Not able to create a test with the default package
* https://github.com/testng-team/testng-remote/issues/36: Support DEV-SNAPSHOT version
* #284: for m2e integration, enable parsing systemProperties and environmentVariables by default. and fix a potential NPE if 'environmentVariables' not present in pom.xml
* fixed #288: Missing failed assert details in the console. Now set the default log level to 2, which means reverted change for #253

## 6.9.12

Supported Metrics:

| Plugin | Dependency |
| ------------- | ------------- |
| TestNG for Eclipse | Eclipse Juno (4.2) or above |
| TestNG M2E Integration (Optional) | M2E 1.5 or above |

* enhancement #241: give more space to the testng result tree
* issue #238: fixed the toolbar position on Eclipse Neon
* fixed issue #157: NPE in TestRunnerViewPart
* fixed #251: Classes from WAR project exported with `<attachClasses>true</attachClasses>` are not visible
* fixed #248: run test failed with snapshot version of testng, with error of `java.lang.AbstractMethodError`
* #253: default log level to 0 for nothing

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

**IMPORTANT:** TestNG versions below 6.5.1 are not supported since this version.

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
