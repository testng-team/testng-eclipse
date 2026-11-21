---
name: release-testng-eclipse-update-site
description: Release a new version of the TestNG Eclipse plug-in update site. Builds the p2 repository from the testng-eclipse repo with Tycho, copies it into the testng-eclipse-update-site repo (updatesites/, zipped/, composite XMLs, README), then commits and pushes. Use when asked to release, publish, or cut a new testng-eclipse / TestNG Eclipse plugin version or update site.
---

Two sibling repos are involved. Confirm both paths with the user if they differ:

- `SRC` = `/Users/xuqing.tan/workspace/my/testng-eclipse` (Tycho build; produces the p2 repo)
- `SITE` = `/Users/xuqing.tan/workspace/my/testng-eclipse-update-site` (published via GitHub; branch `main`; served at https://testng.org/testng-eclipse-update-site)

`SRC/new-release` and `SRC/scripts/upload-updatesite` publish to Bintray, which no longer exists. Do NOT use them. Release by copying into `SITE`.

## 0. Gather inputs

- `VERSION`, e.g. `7.12.0`. Ask if not given. Previous release: `git -C SRC describe --tags --abbrev=0`.
- Folder naming: `QUAL` = `<VERSION>.<yyyyMMddHHmm>` in UTC, e.g. `7.11.0.202503091801` (all existing `updatesites/` and `zipped/` folders follow this; the bare `<VERSION>/` folder holds only the composite XMLs). Take the timestamp from the build: `TZ=UTC stat -f %Sm -t %Y%m%d%H%M SRC/testng-eclipse-update-site/target/org.testng.eclipse.updatesite.zip`. Preferably build with `X.Y.Z-SNAPSHOT` so the bundle/feature versions inside the repo carry the same qualifier; building with plain `X.Y.Z` yields jars versioned `X.Y.Z` (7.12.0 was released that way, folder renamed afterwards).
- Working tree of `SITE` must be clean: `git -C SITE status --short`. If not, stop and ask.

## 1. Set the version in SRC

```bash
cd SRC && scripts/set-version <VERSION>
```

This edits `pom.xml`, the feature/plugin `pom.xml`s, `feature.xml` and `MANIFEST.MF` files (uncommitted changes in `SRC`; leave them for the user to commit unless asked).

## 2. Build (JDK 17+)

```bash
cd SRC && ./mvnw -e -Dci clean install
```

Output: `SRC/testng-eclipse-update-site/target/repository/` (p2 repo) and `.../target/org.testng.eclipse.updatesite.zip`.

Known failures:

- `PKIX path building failed` / `unable to find valid certification path`: a TLS-intercepting proxy (Zscaler) is re-signing HTTPS and the JDK `cacerts` lacks its root. Fix without touching the JDK:
  ```bash
  JH=$(java -XshowSettings:properties -version 2>&1 | awk '/java.home/{print $3}')
  mkdir -p ~/.m2/certs && cp "$JH/lib/security/cacerts" ~/.m2/certs/cacerts-zscaler && chmod u+w ~/.m2/certs/cacerts-zscaler
  security find-certificate -a -c "Zscaler" -p /Library/Keychains/System.keychain ~/Library/Keychains/login.keychain-db > /tmp/zs.pem
  keytool -importcert -noprompt -trustcacerts -alias zscaler-root -file /tmp/zs.pem -keystore ~/.m2/certs/cacerts-zscaler -storepass changeit
  export MAVEN_OPTS="-Djavax.net.ssl.trustStore=$HOME/.m2/certs/cacerts-zscaler -Djavax.net.ssl.trustStorePassword=changeit"
  ```
  Skip if `~/.m2/certs/cacerts-zscaler` already exists. `-Djavax.net.ssl.trustStoreType=KeychainStore` does NOT work for Maven's resolver.
- `Could not mirror artifact ... Server returned HTTP code: 403` while assembling the repository: transient and on a different artifact each run (the same URLs return 200 with curl). Re-run the same command; Tycho caches downloaded artifacts, and it passed on the 2nd attempt. Retry up to ~3 times before investigating.

Success = `BUILD SUCCESS` and `target/org.testng.eclipse.updatesite.zip` exists. Check `features/` contains `org.testng.eclipse_<built version>.jar`.

Note: the bundled `org.testng` jar comes from `https://testng.org/testng-p2-update-site/updatesites/...` (see `SRC/testng-eclipse-update-site/category.xml`/pom). If the release should ship a newer TestNG, update that reference before building.

## 3. Copy into SITE

Layout in `SITE`:

| Path | Content |
| --- | --- |
| `updatesites/<QUAL>/` | copy of `target/repository/` |
| `zipped/<QUAL>/org.testng.eclipse.updatesite.zip` | the zip |
| `<VERSION>/compositeArtifacts.xml`, `compositeContent.xml` | composite pointing at `../updatesites/<QUAL>` |
| root `compositeArtifacts.xml`, `compositeContent.xml` | list of `<child location="<VERSION>"/>`, oldest first |
| `README.md` | versioned URL list, newest first |

Folder names under `updatesites/` and `zipped/` MUST be `<VERSION>.<yyyyMMddHHmm>` (UTC). NEVER copy into a bare `<VERSION>` folder there. Derive `QUAL` first, then copy:

```bash
T=SRC/testng-eclipse-update-site/target
QUAL=<VERSION>.$(TZ=UTC stat -f %Sm -t %Y%m%d%H%M $T/org.testng.eclipse.updatesite.zip)
echo "$QUAL"   # must match ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]{12}$
mkdir -p SITE/updatesites/$QUAL SITE/zipped/$QUAL SITE/<VERSION>
cp -R $T/repository/. SITE/updatesites/$QUAL/
cp $T/org.testng.eclipse.updatesite.zip SITE/zipped/$QUAL/
```

Per-version composites: if `SITE/<VERSION>/` already exists (re-release of the same X.Y.Z), only change the `<child location="../updatesites/...">` line in both XMLs. Otherwise copy the previous version's two XMLs and replace the child location with `../updatesites/<QUAL>`.

Root composites and README, only for a NEW `<VERSION>` (skip for a re-release):

- Add `    <child location="<VERSION>"/>` (4-space indent) after the last `<child>` in both root XMLs.
- Add `* https://testng.org/testng-eclipse-update-site/<VERSION>` as the first bullet under "Versioned update site URL:" in `README.md`.

Use the `edit` tool or a script that writes real newlines. macOS `sed -i '' 's/x/&\ny/'` does NOT insert a newline here and joins the lines.

Verify: `ls SITE/updatesites SITE/zipped | grep <VERSION>` shows only `<VERSION>.<12 digits>` names (no bare `<VERSION>`); `git -C SITE diff` shows exactly one added line in each of the 3 edited files; `git -C SITE status --short` shows the new `<VERSION>/`, `updatesites/<QUAL>/`, `zipped/<QUAL>/`.

## 4. Commit and push SITE

Only after the user has OK'd it (push is public):

```bash
cd SITE && git add -A && git commit -m "release <QUAL>" && git push
```

Commit message history: `release 7.11.0.202503091801`, `release 7.12.0.202611210213`. Use `release <QUAL>`.

## 5. Report

State: version and `QUAL`, the commit hash, that the URL `https://testng.org/testng-eclipse-update-site/<VERSION>` may take a few minutes to serve, and whether the `SRC` version-bump changes are still uncommitted (commit them as `release <VERSION>` if the user asks). Optionally offer to tag `SRC` (`git tag -a <VERSION> -m "release <VERSION>"`; the old `new-release` script did this) and to update the update-site table in `SRC/README.md`; do neither without being asked.
