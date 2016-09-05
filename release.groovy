#!/usr/bin/groovy
def imagesBuiltByPipline() {
  return ['git-collector','chaos-monkey','elasticsearch-logstash-template','hubot-notifier','image-linker','kibana-config']
}

def externalImages(){
  return ['exposecontroller','nginx-controller','alpine-caddy','hubot-irc','eclipse-orion','nexus','gerrit','fabric8-kiwiirc','brackets','jenkins-jnlp-client','taiga-front','taiga-back','hubot-slack','lets-chat','jenkins-docker','maven-builder','gogs','grafana']
}

def repo(){
 return 'fabric8io/fabric8-devops'
}

def updateDependencies(source){

  def properties = []
  properties << ['<fabric8.version>','io/fabric8/kubernetes-api']
  //properties << ['<docker.maven.plugin.version>','io/fabric8/docker-maven-plugin']
  //properties << ['<fabric8.maven.plugin.version>','io/fabric8/fabric8-maven-plugin']

  updatePropertyVersion{
    updates = properties
    repository = source
    project = repo()
  }
}

def stage(){
  return stageProject{
    project = repo()
    useGitTagForNextVersion = true
    extraImagesToStage = externalImages()
  }
}

def approveRelease(project){
  def releaseVersion = project[1]
  approve{
    room = null
    version = releaseVersion
    console = null
    environment = 'fabric8'
  }
}

def updateDownstreamDependencies(stagedProject) {
  pushPomPropertyChangePR {
    propertyName = 'fabric8-devops.version'
    projects = [
            'fabric8io/fabric8-maven-dependencies'
    ]
    version = stagedProject[1]
  }
  pushPomPropertyChangePR {
    propertyName = 'fabric8.devops.version'
    projects = [
            'fabric8io/fabric8-forge'
    ]
    version = stagedProject[1]
  }
}

def release(project){
  releaseProject{
    stagedProject = project
    useGitTagForNextVersion = true
    helmPush = false
    groupId = 'io.fabric8.devops.apps'
    githubOrganisation = 'fabric8io'
    artifactIdToWatchInCentral = 'jenkins'
    artifactExtensionToWatchInCentral = 'jar'
    promoteToDockerRegistry = 'docker.io'
    dockerOrganisation = 'fabric8'
    imagesToPromoteToDockerHub = imagesBuiltByPipline()
    extraImagesToTag = externalImages()
  }
}

def mergePullRequest(prId){
  mergeAndWaitForPullRequest{
    project = repo()
    pullRequestId = prId
  }

}
return this;
