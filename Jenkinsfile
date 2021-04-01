pipeline {
  /*
  environment {
       JAVA_TOOL_OPTIONS = '-Duser.home=$HOME'
  }
  */
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
        sh 'echo $HOME'
        sh 'echo $JENKINS_HOME'
        sh 'ls -la .m2'
        //sh 'mvn -B clean install'
      }
    }

  }
}