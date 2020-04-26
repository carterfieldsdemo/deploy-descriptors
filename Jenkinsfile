pipeline {
  agent {
    label 'kompose'
  }
  options {
    timeout(time: 10, unit: 'MINUTES')
  }
  environment {
    KUBECONFIG='kubeconfig'
  }
  parameters {
    choice(name: 'ENVIRONMENT', choices: ['DEV', 'QA', 'STG', 'PROD'], description: 'Environment to deploy in')
    string(name: 'SERVICE', description: 'Service name (same as repository name)')
    string(name: 'TAG', defaultValue: 'latest', description: 'Version to Deploy')
  }
  stages {
    stage('Deploy') {
      steps {
        container('kompose') {
          withAWS(credentials: "ecr-vylla"){
            sh "kubectl config use-context ${params.ENVIRONMENT}"
            sh "rm -f .deploy.yml"

            //If a full kubernetes descriptor is available, use that one instead (after replacing the tag.)
            sh """
            TAG=${params.TAG} 
            if [ -e "${params.ENVIRONMENT}/${params.SERVICE}-k8s.yml" ]; then
              sed -e "s/\\\${TAG}/\$TAG/" "${params.ENVIRONMENT}/${params.SERVICE}-k8s.yml" > .deploy.yml
            else
              kompose convert -o .deploy.yml -f ${params.ENVIRONMENT}/${params.SERVICE}.yml
            fi
            """

            sh "kubectl apply -f .deploy.yml"     
          }
        }
      }
    }
  }
}
