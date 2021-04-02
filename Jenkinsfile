pipeline {
  agent {
    docker {
      image 'maven:3.6.3-jdk-11'
    }

  }
  stages {
    stage('Build') {
      steps {
        sh 'mvn -Dmaven.repo.local=/var/jenkins_home/.m2/repository -B clean install'
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'target/summary_report.html', fingerprint: true
      junit 'build/reports/**/*.xml'
    }
  }
}