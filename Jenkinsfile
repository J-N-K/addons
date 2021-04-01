pipeline {
  # environment {
  #     JAVA_TOOL_OPTIONS = '-Duser.home=$HOME'
  # }
  agent {
    docker {
      image 'maven:3-alpine'
      args '-v /var/jenkins_home/.m2:$HOME/.m2'
    }

  }
  stages {
    stage('Build') {
      steps {
        sh 'pwd'
        sh 'mvn -B clean install'
      }
    }

  }
}