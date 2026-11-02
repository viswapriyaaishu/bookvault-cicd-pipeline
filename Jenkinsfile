pipeline {
    agent any

    environment {
        IMAGE_NAME = 'viswapriya/bookvault'
        KUBECONFIG = '/var/jenkins_home/.kube/config'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Gitleaks Secret Scan') {
            steps {
                sh '''
                    docker run --rm \
                      -v "$WORKSPACE:/repo" \
                      ghcr.io/gitleaks/gitleaks:latest \
                      dir --redact /repo
                '''
            }
        }

        stage('Build and Test') {
            steps {
                sh '''
                    chmod +x mvnw
                    ./mvnw -B clean package
                '''
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

                    kubectl port-forward svc/bookvault 18080:8080 \
                      > /tmp/bookvault-port-forward.log 2>&1 &

                    PORT_FORWARD_PID=$!

                    trap 'kill $PORT_FORWARD_PID 2>/dev/null || true' EXIT

                    for i in $(seq 1 12); do
                      if curl -fsS \
                        http://127.0.0.1:18080/actuator/health; then
                        echo "BookVault health check passed"
                        exit 0
                      fi

                      sleep 5
                    done

                    echo "BookVault health check failed"
                    cat /tmp/bookvault-port-forward.log
                    exit 1
                '''
            }
        }

        stage('Ansible Configuration') {
            steps {
                sh '''
                    echo "Checking Ansible installation..."

                    ansible --version

                    echo "Running Ansible playbook..."

                    ansible-playbook \
                      -i inventory.ini \
                      setup-bookvault.yml
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
