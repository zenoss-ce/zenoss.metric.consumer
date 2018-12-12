#!/usr/bin/env groovy

node {

  stage('Checkout') {
    checkout scm
  }

  docker.image('zenoss/build-tools:0.0.10').inside() { 

    stage('Build') {
      withMaven(mavenSettingsConfig: 'bintray') {
        sh '''
          touch .checkedenv .checkedtools .checkedtools_version_brand
          export PATH=$MVN_CMD_DIR:$PATH 
          make build
        '''
      }
    }

    stage('Publish to maven repository') {
      withMaven(mavenSettingsConfig: 'bintray') {
        sh '''
          export PATH=$MVN_CMD_DIR:$PATH 
          mvn -B deploy
        '''
      }
    }
  }

  stage('Publish app') {
    def remote = [:]
    withFolderProperties {
      withCredentials( [sshUserPrivateKey(credentialsId: 'PUBLISH_SSH_KEY', keyFileVariable: 'identity', passphraseVariable: '', usernameVariable: 'userName')] ) {
        remote.name = env.PUBLISH_SSH_HOST
        remote.host = env.PUBLISH_SSH_HOST
        remote.user = userName
        remote.identityFile = identity
        remote.allowAnyHosts = true

        def tar_ver = sh( returnStdout: true, script: "awk -F'(>|<)' 'NR==8{print \$3}' pom.xml" ).trim()
        sshPut remote: remote, from: 'metric-consumer-app/target/metric-consumer-app-' + tar_ver + '-zapp.tar.gz', into: env.PUBLISH_SSH_DIR
      }
    }
  }
}
