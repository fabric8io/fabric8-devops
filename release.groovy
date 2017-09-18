#!/usr/bin/groovy
def imagesBuiltByPipeline() {
  return ['git-collector','chaos-monkey','elasticsearch-logstash-template','grafana','hubot-notifier','image-linker','kibana-config']
}

def externalImages(){
  return ['nginx-controller','alpine-caddy','hubot-irc','eclipse-orion','nexus','gerrit','fabric8-kiwiirc','brackets','jenkins-jnlp-client','taiga-front','taiga-back','hubot-slack','lets-chat','gogs','grafana']
}

def repo(){
 return 'fabric8io/fabric8-devops'
}

def updateDependencies(source){

  def properties = []
  //properties << ['<fabric8.version>','io/fabric8/kubernetes-api']
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
            'fabric8io/fabric8-platform',
            'fabric8io/fabric8-online',
            'fabric8io/fabric8-pipeline-library',
            'fabric8io/ipaas-platform',
            'fabric8io/fabric8-forge',
            'fabric8io/fabric8-ipaas',
            'fabric8io/django'
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
    imagesToPromoteToDockerHub = imagesBuiltByPipeline()
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
