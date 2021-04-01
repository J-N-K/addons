pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        sh 'ls -la /jenkins/.m2'
        sh 'mvn -B clean install'
      }
    }

  }
}