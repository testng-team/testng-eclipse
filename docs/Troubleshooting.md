Troubleshooting
====

## Can't install TestNG Eclipse Plugin

If you failed to install TestNG Eclipse Plugin with following error:
* Unable to read repository at http://beust.com/eclipse/content.xml.
* HTTP Service 'Service Unavailable': http://beust.com/eclipse-beta/content.xml
* An error occurred while collecting items to be installed session context was:(profile=epp.package.committers, phase=org.eclipse.equinox.internal.p2.engine.phases.Collect, operand=, action=). Unable to read repository at http://beust.com/eclipse/updatesites/6.11.0.201703011520/plugins/org.testng.eclipse_6.11.0.201703011520.jar. Read timed out

You can follow thses steps try to fix/bypass the problem:
* Check if you have any http proxy setting, or behind the company firewall, make you can access the update site: `http://beust.com/eclipse`
* Make sure your network can reach `http://dl.bintray.com/testng-team/testng-eclipse-release/` (the main entrypoint http://beust.com/eclipse redirects to it)
* Add vmargs '-Djava.net.preferIPv4Stack=true' to Eclipse config.ini, read the wiki [[1]](https://wiki.eclipse.org/Eclipse.ini) for the detail.
* Retry two or more times, if still can't install, it could be the site http://beust.com temporarily unavailable, please install from the updatesite: `http://dl.bintray.com/testng-team/testng-eclipse-release/`
* Or, for whatever reason, you can't access either `http://beust.com/eclipse` or `http://dl.bintray.com/testng-team/testng-eclipse-release/` directly, you can download the [offline updatesite](https://github.com/cbeust/testng-eclipse#update-sites), then copy to your workstation.
