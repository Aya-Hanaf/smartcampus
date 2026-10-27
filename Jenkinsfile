pipeline {
    agent any

    tools {
        maven 'Maven 3.9'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Deploy') {
            steps {
                sh '''
                    pkill -f smartcampus-0.0.1-SNAPSHOT.jar || true
                    sleep 2
                    JENKINS_NODE_COOKIE=dontKillMe BUILD_ID=dontKillMe \
                      nohup java -jar "$WORKSPACE/target/smartcampus-0.0.1-SNAPSHOT.jar" \
                      > /var/lib/jenkins/smartcampus.log 2>&1 &
                    for i in $(seq 1 30); do
                      if curl -s http://localhost:8090/api/health | grep -q UP; then
                        echo "SmartCampus est UP sur le port 8090"; exit 0
                      fi
                      sleep 2
                    done
                    echo "ECHEC"; tail -50 /var/lib/jenkins/smartcampus.log; exit 1
                '''
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
    }
}
