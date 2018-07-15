This project implements a gis plugin to add the support of geospatial services to Taverna 2.5

## Install

```
git clone -b taverna2_dev3 https://github.com/skoulouzis/incubator-taverna-plugin-gis.git
```
```
cd incubator-taverna-plugin-gis 
```
```
mvn install
```

In the ~/.taverna-core-2.5.0/plugins/plugins.xml file above the </plugins:plugins> line add :

```XML
 <plugin>
        <provider>org.apache.taverna.gis</provider>
        <identifier>org.apache.taverna2.gis.apache-taverna2-plugin-gis-plugin</identifier>
        <version>0.0.1-incubating-SNAPSHOT</version>
        <name>Gis Taverna plugin</name>
        <description/>
        <enabled>true</enabled>
        <repositories>
            <repository>http://www.mygrid.org.uk/maven/repository/</repository>
            <repository>http://52north.org/maven/repo/releases/</repository>
            <repository>http://repo.maven.apache.org/maven2/</repository>
            <repository>http://www.mygrid.org.uk/maven/snapshot-repository/</repository>
            <repository>http://uk.maven.org/maven2/</repository>
            <repository>http://download.java.net/maven/2/</repository>
            <repository>http://www.mygrid.org.uk/maven/biomoby/moby-dev.inab.org/m2/</repository>
            <repository>http://openprovenance.org/java/maven-releases/</repository>
            <repository>http://build.mygrid.org.uk/maven/404/</repository>
            <repository>http://build.mygrid.org.uk/maven/aduna/</repository>
            <repository>http://repo1.maven.org/maven2/</repository>
            <repository>http://download.osgeo.org/webdav/geotools/</repository>
            <repository>http://repo.opengeo.org/</repository>
            <repository>http://52north.org/maven/repo/snapshots/</repository>
            <repository>http://people.apache.org/repo/m2-snapshot-repository/</repository>
            <repository>http://nexus.codehaus.org/snapshots/</repository>
            <repository>file:///home/{$user}/.m2/repository/</repository>
        </repositories>
        <profile>
            <dependency>
                <groupId>org.apache.taverna2.gis</groupId>
                <artifactId>apache-taverna2-plugin-gis-activity-ui</artifactId>
                <version>0.0.1-incubating-SNAPSHOT</version>
            </dependency>
            <dependency>
                <groupId>junit</groupId>
                <artifactId>junit</artifactId>
                <version>4.11</version>
            </dependency>
            <dependency>
                <groupId>org.apache.taverna2.gis</groupId>
                <artifactId>apache-taverna2-plugin-gis-activity</artifactId>
                <version>0.0.1-incubating-SNAPSHOT</version>
            </dependency>
        </profile>
        <compatibility>
            <application>
                <version>2.5.0</version>
            </application>
        </compatibility>
    </plugin>
```

