This project packages up the apps into different forms of packaging for easier consumption:

* [Management](management):
    * [Logging](logging) provides consolidated logging and visualisation of log statements and events across your environment
    * [Metrics](metrics) provides consolidated historical metric collection and visualisation across your environment
* [Continuous Delivery](cdelivery) 
    * [CD Core](cdelivery-core) using [Gogs](http://gogs.io/), [Jenkins](https://jenkins-ci.org/), [Nexus](http://www.sonatype.org/nexus/) and [SonarQube](http://www.sonarqube.org/)
    * [Chat](http://fabric8.io/guide/chat.html) provides a [hubot](https://hubot.github.com/) integration with the CD infrastructure
* [Distro](distro) is a tarball of all of the main packages along with the microservices which make them up