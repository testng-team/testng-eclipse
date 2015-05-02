TestNG Eclipse Plugin
====

# Build

Now, building testng eclipse plugin update site can be as simple as the single command below (provided that you have maven installed already):

```
cd testng-eclipse
mvn -e clean package
```

the update site package will be generated at `<testng-eclipse>/updatesite/target/site.zip`

# Release

1. build the update site as described previously
2. (optional) git tag
3. update the version numbers in pom.xml, feature, plugin, etc., for next development phase, for example

    ```
    cd testng-eclipse

    # saying current version number is 6.8.6
    # to set next development version number to 6.8.7
    mvn -e -Dtycho.mode=maven org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=6.8.7-SNAPSHOT
    ```

4. commit the changes
