Troubleshooting
====

## Can't install TestNG Eclipse Plugin

**Note that, https://dl.bintray.com/testng-team/testng-eclipse/ is the official update site for TestNG Eclipse Plugin**

If you failed to install TestNG Eclipse Plugin with following error:
* Unable to read repository at https://beust.com/eclipse/content.xml.
* HTTP Service 'Service Unavailable': https://beust.com/eclipse-beta/content.xml
* An error occurred while collecting items to be installed session context was:(profile=epp.package.committers, phase=org.eclipse.equinox.internal.p2.engine.phases.Collect, operand=, action=). Unable to read repository at https://beust.com/eclipse/updatesites/6.11.0.201703011520/plugins/org.testng.eclipse_6.11.0.201703011520.jar. Read timed out

You can follow thses steps try to fix/bypass the problem:
* Check if you have any http proxy setting, or behind the company firewall, make you can access the update site: `https://beust.com/eclipse`
* Make sure your network can reach `https://dl.bintray.com/testng-team/testng-eclipse-release/` (the main entrypoint https://beust.com/eclipse redirects to it)
* Add vmargs '-Djava.net.preferIPv4Stack=true' to Eclipse config.ini, read the wiki [[1]](https://wiki.eclipse.org/Eclipse.ini) for the detail.
* Retry two or more times, if still can't install, it could be the site https://beust.com temporarily unavailable, please install from the updatesite: `http://dl.bintray.com/testng-team/testng-eclipse-release/`
* Or, for whatever reason, you can't access either `https://beust.com/eclipse` or `http://dl.bintray.com/testng-team/testng-eclipse-release/` directly, you can download the [offline updatesite](https://github.com/cbeust/testng-eclipse#update-sites), then copy to your workstation.


## TestNG Eclipse Plugin not showing up

On Windows platform, if you installed plugin succeed, but it's [not showing up](https://github.com/cbeust/testng-eclipse/issues/378#issuecomment-359957096), please make sure install Eclipse to a folder where your user has full rights (Ex., user home dir).
see relate StackOverflow answer: https://stackoverflow.com/a/3448786/4867232
