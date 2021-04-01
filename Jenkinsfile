pipeline {
  /*
  environment {
       JAVA_TOOL_OPTIONS = '-Duser.home=$HOME'
  }
  */
  agent {
    docker {
      image 'maven:3-alpine'
      args '-v /var/jenkins_home/.m2:/jenkins/.m2'
    }

  }
  stages {
    stage('Build') {
      steps {
        sh 'pwd'
        sh 'ls -la /jenkins'
        sh 'ls -la /*'
        //sh 'mvn -B clean install'
      }
    }

  }
}