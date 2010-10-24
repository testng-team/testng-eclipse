User
===

The documentation for this plug-in can be found at http://testng.org/doc/eclipse.html


Developer
======

In Eclipse, select "Import / Existing Project" and point the dialog to this directory. Then you can just create a new Eclipse application launch to run the plug-in.

The runner view is called TestRunnerViewPart and it receives the test results from the remote TestNG process. Every new result is passed to postTestResult() which in turn, passes this result to each tab by calling their updateTestResult() method.

The tab's logic is in AbstractTab, which calculates a unique id for each test result and then either creates or updates the corresponding node in the tree. Each node is associated with an instance of an ITreeItem (store in its data map) which contains all the necessary information to display the label, its image, etc...

The tests are run by a subclass of TestNG called RemoteTestNG. The Eclipse client forks the RemoteTestNG process and adds itself as a listener. The difference is that this listener will pass the test results over the network using a string protocol that can be found in the strprotocol package (note: I'm planning to replace this complicated and fragile protocol with simple serialization).



