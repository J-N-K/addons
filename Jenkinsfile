pipeline {
  environment {
       JAVA_TOOL_OPTIONS = '-Duser.home=/jenkins'
  }
  agent {
    docker {
      image 'maven:3-alpine'
      args '-v /var/jenkins_home/.m2:/jenkins/.m2'
    }

  }
  stages {
    stage('Build') {
      steps {
        sh 'ls -la /jenkins/.m2'
        sh 'mvn -B clean install'
      }
    }

  }
}