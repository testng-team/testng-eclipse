Troubleshooting
====

## Can't install TestNG Eclipse Plugin

**Note that, https://testng.org/testng-eclipse-update-site is the official update site for TestNG Eclipse Plugin. Do not install from https://testng.org/testng-p2-update-site, it only contains the dependency bundles (`org.testng`, `jcommander`, `snakeyaml`, `jquery`), not the plugin, and the install fails with `No repository found containing: osgi.bundle,org.testng.eclipse,...`**

If you failed to install TestNG Eclipse Plugin with following error:
* Unable to read repository at https://beust.com/eclipse/content.xml.
* HTTP Service 'Service Unavailable': https://beust.com/eclipse-beta/content.xml
* An error occurred while collecting items to be installed session context was:(profile=epp.package.committers, phase=org.eclipse.equinox.internal.p2.engine.phases.Collect, operand=, action=). Unable to read repository at https://beust.com/eclipse/updatesites/6.11.0.201703011520/plugins/org.testng.eclipse_6.11.0.201703011520.jar. Read timed out

You can follow thses steps try to fix/bypass the problem:
* Check if you have any http proxy setting, or behind the company firewall, make sure you can access the update site: `https://testng.org/testng-eclipse-update-site`. If the error is `PKIX path building failed`, see [PKIX path building failed](#pkix-path-building-failed) below.
* Add vmargs '-Djava.net.preferIPv4Stack=true' to Eclipse config.ini, read the wiki [[1]](https://wiki.eclipse.org/Eclipse.ini) for the detail.
* Or, for whatever reason, you still can't access https://testng.org/testng-eclipse-update-site in the Eclipse IDE, you can download the [offline updatesite](https://github.com/cbeust/testng-eclipse#update-sites), then copy to your workstation.


## PKIX path building failed

```
Unable to read repository at https://testng.org/testng-eclipse-update-site/content.xml.
(certificate_unknown) PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

This is not a TestNG or update-site problem. The JRE Eclipse runs on doesn't trust the certificate it receives. It almost always means a corporate proxy or firewall (Zscaler, Netskope, Fortinet, etc.) is re-signing HTTPS traffic with its own root CA. Your OS trusts that CA, but Eclipse's bundled JRE uses its own `cacerts` and doesn't. That is why the Marketplace, other update sites and Maven Central fail too, not just testng.org.

**1. Confirm it's an intercepting proxy**

```
curl -v https://testng.org 2>&1 | grep -E "issuer|subject"
```

If the issuer is your company or a security vendor instead of a public CA (Let's Encrypt, DigiCert, GitHub...), that's the cause.

**2. Import the proxy root CA into the JRE Eclipse actually runs on**

Get the root CA as a `.pem`/`.cer` file: export it from Keychain Access (macOS) or `certmgr.msc` (Windows), or ask your IT team. Then import it into the bundled JRE (`<eclipse>/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.<version>/jre`). On macOS, using Zscaler as an example:

```bash
security find-certificate -c "Zscaler Root CA" -p /Library/Keychains/System.keychain > /tmp/root.pem
JRE=<eclipse install>/Contents/Eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.<version>/jre
$JRE/bin/keytool -importcert -noprompt -trustcacerts -alias corp-root \
  -file /tmp/root.pem -keystore $JRE/lib/security/cacerts -storepass changeit
```

`changeit` is the default `cacerts` password. On Windows, run `%JRE%\bin\keytool.exe` from an elevated prompt. Then fully quit Eclipse and restart it, `cacerts` is only read at startup.

Pitfalls:
* Import into the JRE Eclipse really starts with: check `-vm` in `eclipse.ini`, or `java.home` in Help -> About Eclipse -> Installation Details -> Configuration. It is usually not your system JDK.
* An Eclipse update can ship a new `jre.full.<version>` folder with a fresh `cacerts`. Repeat the import after updating.
* Don't set `-Djavax.net.ssl.trustStore=NONE -Djavax.net.ssl.trustStoreType=KeychainStore` in `eclipse.ini` as a shortcut on macOS. It replaces the default trust store and can still fail with PKIX. Importing into `cacerts` works.
* If Maven (m2e, e.g. the TestNG M2E integration) fails with `Could not transfer artifact ... from/to central ... PKIX`, the same fix applies. Then clear the cached failure, m2e won't retry until you do: delete the `*.lastUpdated` files under `~/.m2/repository`, or use Maven -> Update Project with "Force Update of Snapshots/Releases".
* If the proxy also blocks `testng.org`, downloads can fail with 403 responses containing a block page instead of a jar. Ask IT to allow `testng.org` and `github.io`, or use the offline update site.

**3. If you can't change the JRE trust store**

Download the zipped update site from [testng-eclipse-update-site/zipped](https://github.com/testng-team/testng-eclipse-update-site/tree/main/zipped), then use Help -> Install New Software -> Add -> Archive. This avoids PKIX for the plugin itself, but a proxy that re-signs traffic can still break Maven dependency resolution later.


## TestNG Eclipse Plugin not showing up

On Windows platform, if you installed plugin succeed, but it's [not showing up](https://github.com/cbeust/testng-eclipse/issues/378#issuecomment-359957096), please make sure install Eclipse to a folder where your user has full rights (Ex., user home dir).
see relate StackOverflow answer: https://stackoverflow.com/a/3448786/4867232
