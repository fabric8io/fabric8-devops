## Developer Guide

This kibana image contains a sidecar that initialises on startup a default set of Kibana objects for config, index patterns, searches, visualisations and dashbaords.

Over time we should add more and more default searches, visualisations and dashboards. Here's how to add more kibana objects to the distribution.

* run the Logging application in fabric8 which runs Elasticsearch and Kibana
* use Kibana to add new searches, visualisations or dashboards, saving them as you make changes
* when you are ready to save your changes to the distro type the following command:

```
mvn test-compile exec:java
```

Which assumes your elasticsearch instance is at `http://elasticsearch.vagrant.f8/`. If you are running Elasticsearch at a different location then use:

```
mvn test-compile exec:java -Delasticsearch.url=http://myserver.whatever.it.is/
```

* now any chances made to the various kibana objects are now saved in your `src/main/resources` folders
* review the changes, any new files will need to be added to git
* commit and then next release of fabric8-devops should include your changes!