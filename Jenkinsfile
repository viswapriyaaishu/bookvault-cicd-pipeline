
pipeline {
    agent any

    environment {
        IMAGE_NAME = 'viswapriya/bookvault'
        KUBECONFIG = '/var/lib/jenkins/.kube/config'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw -B clean package'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    docker build \
                      -t $IMAGE_NAME:$BUILD_NUMBER \
                      -t $IMAGE_NAME:latest .
                '''
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKERHUB_USERNAME',
                    passwordVariable: 'DOCKERHUB_TOKEN'
                )]) {
                    sh '''
                        echo "$DOCKERHUB_TOKEN" |
                          docker login -u "$DOCKERHUB_USERNAME" --password-stdin

                        docker push "$IMAGE_NAME:$BUILD_NUMBER"
                        docker push "$IMAGE_NAME:latest"
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    sed "s|IMAGE_TAG|$BUILD_NUMBER|g" \
                      k8s/deployment.yaml |
                      kubectl apply -f -

                    kubectl apply -f k8s/service.yaml

                    kubectl rollout status deployment/bookvault \
                      --timeout=240s
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    kubectl get deployment bookvault
                    kubectl get pods -l app=bookvault
                    kubectl get service bookvault

                    for i in $(seq 1 12); do
                      if curl -fsS \
                        http://127.0.0.1:30081/actuator/health; then
                        echo "BookVault health check passed"
                        exit 0
                      fi
                      sleep 10
                    done

                    echo "BookVault health check failed"
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            echo 'BookVault CI/CD pipeline finished.'
        }
    }
}
