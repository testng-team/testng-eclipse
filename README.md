TestNG for Eclipse Plugin
====

[![Build Status](http://img.shields.io/travis/cbeust/testng-eclipse.svg)](https://travis-ci.org/cbeust/testng-eclipse)
[![Join the chat at https://gitter.im/cbeust/testng-eclipse](https://badges.gitter.im/cbeust/testng-eclipse.svg)](https://gitter.im/cbeust/testng-eclipse?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Installation

### Install Release

<a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=1549" class="drag" title="Drag to your running Eclipse workspace."><img class="img-responsive" src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png" alt="Drag to your running Eclipse workspace." /></a>

see more at http://testng.org/doc/download.html

### Install Snapshot

The update site to install snapshot versions of the TestNG Eclipse plug-in is:

`http://beust.com/eclipse-beta`

Use it if you want to experiment with the new features or verify the bug fixes, and please [report back if you encounter any issues](https://github.com/cbeust/testng-eclipse/issues).

To install it:
* Click "Help -> Install New Software..." on top level menu
* Paste the url http://beust.com/eclipse-beta to `Work with: ` text field and press enter.
* Select the plugins
* Click "Next" button and accept the license to complete the installation.
* Restart Eclipse

If you want to install previous version of beta, you can pick up one from [here](http://dl.bintray.com/testng-team/testng-eclipse/updatesites/).

Enjoy.

### Update sites

Plugin Version            | Online Update-Site | Zipped Update-Site
------------------------- | ------------------ | ---------------------
:star2: LATEST RELEASE    | `http://beust.com/eclipse` | [here](http://dl.bintray.com/testng-team/testng-eclipse-release/zipped/)
BETA | `http://beust.com/eclipse-beta` | [here](https://dl.bintray.com/testng-team/testng-eclipse/zipped/)
History:||
6.11.0.201703011520       | `https://dl.bintray.com/testng-team/testng-eclipse-release/6.11.0/` | [download](https://dl.bintray.com/testng-team/testng-eclipse-release/zipped/6.11.0.201703011520/site_assembly.zip)
6.10.0.201612030230       | `https://dl.bintray.com/testng-team/testng-eclipse-release/6.10.0/` | [download](https://dl.bintray.com/testng-team/testng-eclipse-release/zipped/6.10.0.201612030230/site_assembly.zip)
6.9.13.201609291640       | `https://dl.bintray.com/testng-team/testng-eclipse-release/6.9.13/` | [download](https://dl.bintray.com/testng-team/testng-eclipse-release/zipped/6.9.13.201609291640/site_assembly.zip)
6.9.12.201607091356       | `https://dl.bintray.com/testng-team/testng-eclipse-release/6.9.12/` | [download](https://dl.bintray.com/testng-team/testng-eclipse-release/zipped/6.9.12.201607091356/site_assembly.zip)
6.9.11.201604020423       | `https://dl.bintray.com/testng-team/testng-eclipse-release/6.9.11/` | [download](https://dl.bintray.com/testng-team/testng-eclipse-release/zipped/6.9.11.201604020423/site_assembly.zip)
6.9.10.201512240000       | `https://dl.bintray.com/testng-team/testng-eclipse-release/6.9.10/` | [download](https://dl.bintray.com/testng-team/testng-eclipse-release/zipped/6.9.10.201512240000/site_assembly.zip)
6.9.5.201505251947        | `https://dl.bintray.com/testng-team/testng-eclipse-release/6.9.5/` | [download](https://dl.bintray.com/testng-team/testng-eclipse-release/zipped/6.9.5.201505251947/site_assembly.zip)

(**NOTE**: it's always recommended to install from the ***LATEST RELEASE*** updatesite. the version specific updatesites are for cases that you want to stay on old version.)

## Change Logs

The full changelog is [here](CHANGES.md)

## User documentation

The documentation for this plug-in can be found at http://testng.org/doc/eclipse.html

## Build

### Version number

Set the version number with `scripts/set-version`, e.g.:

```bash
scripts/set-version 6.8.22-SNAPSHOT
```

Using `SNAPSHOT` version numbers will generate UTC timestamped plugin-numbers, e.g. 6.8.22.201505030200.

### Building

Once the version is correct, build the Eclipse plug-in as follows:

```bash
mvn -e -U -Dci clean install
```

The update site package will be generated at `testng-eclipse-update-site/target/org.testng.eclipse.updatesite.zip`

## Troubleshooting

See the troubleshooting doc [here](Troubleshooting.md)

## For Plugin Developer

### Setup Dev Env

* In Eclipse, select _Import / Existing Project_ and point the dialog to
this directory. 
* Go to Eclipse Preference page, navigate to _Plug-in Development / Target Platform_, select 'TestNG Eclipse Luna Target Platform' as the active target platform.
* Then you can just create a new Eclipse application launch to run the plug-in.


### Tech Details

The runner view is called TestRunnerViewPart and it receives the test
results from the remote TestNG process. Every new result is passed to
```postTestResult()``` which in turn, passes this result to each tab by
calling their ```updateTestResult()``` method.

The tab's logic is in ```AbstractTab```, which calculates a unique id for
each test result and then either creates or updates the corresponding
node in the tree. Each node is associated with an instance of an
```ITreeItem``` (store in its data map) which contains all the necessary
information to display the label, its image, etc...

The tests are run by a subclass of ```TestNG``` called ```RemoteTestNG```. The
Eclipse client forks the ```RemoteTestNG``` process and adds itself as a
listener. The difference is that this listener will pass the test
results over the network using a serialization based protocol that can
be found in the ```strprotocol``` package.

The plug-in understands two system properties, which you can define as
VM arguments in the launch dialog:

   `-Dtestng.eclipse.verbose`

This will cause both the Eclipse client and RemoteTestNG to issue a
more verbose output.

   `-Dtestng.eclipse.debug`

Use this flag if you need to debug and break into RemoteTestNG. In
this case, you need to start the RemoteTestNG process youself as a
regular Java application and with the "-debug" flag. Then start the
Eclipse client with this system property, and then the two processes
will communicate on a hardcoded port, 12345 (as opposed to the random
port which they usually use) and through a hardcoded XML file
(`"${java.io.tmpdir}/testng-customsuite.xml"`).

Now that you launched both processes yourself, you can set up break
point and inspect variables on either.


### Protocol

When a new run is launched, TestNGLaunchConfigurationDelegate creates
a VMRunnerConfigurationClient that launches RemoteTestNG with a host,
a port and an XML file. Then Eclipse listens on this host and port.

The base class that provides the basic listening functions is
AbstractRemoteTestRunnerClient, which is defined in TestNG. The
Eclipse plug-in subclasses this class with an
EclipseTestRunnerClient. TestRunnerViewPart creates an instance of
this class and then calls startListening() on it.

Whenever a new message is received, AbstractRemoteTestRunnerClient
looks up the type of the message and then calls the subclass's
corresponding method:

```
SUITE -> notifyStart(GenericMessage)
TEST -> notifySuiteEvents(SuiteMessage)
TEST_RESULT -> notifyTestEvents(TestMessage)
other -> notifyResultEvents(TestResultMessage)
```

RemoteTestNG starts by opening a connection to the port passed on the
command line and when it succeeds, runs the suites and uses listeners
to send messages to the Eclipse client.

All these messages implement IStringMessage and they are of several kinds:

GenericMessage: general information message (such as an initial notification of the number of suites/tests)

- TestMessage
- SuiteMessage
- TestResultMessage
