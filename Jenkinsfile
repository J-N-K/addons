pipeline {
  environment {
       JAVA_TOOL_OPTIONS = '-Duser.home=/home/jenkins'
  }
  agent {
    docker {
      image 'maven:3.6.3-jdk-11'
      args '-v /var/jenkins_home/.m2:/jenkins/.m2'
    }

  }
  stages {
    stage('Build') {
      steps {
        sh 'ls -la /jenkins/.m2'
        sh 'ls -la /var/jenkins_home/.m2'
        sh 'ls ../'
        sh 'mvn -B clean install'
      }
    }

  }
}