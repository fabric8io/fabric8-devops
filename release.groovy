#!/usr/bin/groovy
def updateDependencies(source){

  def properties = []
  properties << ['<fabric8.version>','io/fabric8/kubernetes-api']
  properties << ['<docker.maven.plugin.version>','io/fabric8/docker-maven-plugin']

  updatePropertyVersion{
    updates = properties
    repository = source
    project = 'fabric8io/fabric8-devops'
  }
}

def stage(){
  return stageProject{
    project = 'fabric8io/fabric8-devops'
    useGitTagForNextVersion = true
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
    imagesToPromoteToDockerHub = ['git-collector','chaos-monkey','elasticsearch-logstash-template','hubot-notifier','image-linker','kibana-config','prometheus-kubernetes']
    extraImagesToTag = ['hubot-irc','eclipse-orion','nexus','gerrit','fabric8-kiwiirc','brackets','jenkins-jnlp-client','taiga-front','taiga-back','hubot-slack','lets-chat','jenkins-docker','maven-builder']
  }
}

def mergePullRequest(prId){
  mergeAndWaitForPullRequest{
    project = 'fabric8io/fabric8-devops'
    pullRequestId = prId
  }

}
return this;
