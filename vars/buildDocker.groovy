def call(String serviceName) {
pipeline {
  agent {
    label 'docker-client'
  }
  // triggers { pollSCM('*/10 * * * *') }
  options {
    disableConcurrentBuilds()
    timeout(time: 30, unit: 'MINUTES')
  }
  environment {
    DOCKER_TAG  = "${(env.BRANCH_NAME == 'master' ? 'dev-' : (env.BRANCH_NAME == 'hotfix' ? 'hotfix-' : '' )) + env.BUILD_NUMBER}"
  }
  stages {
    stage('Build') {
      steps {
        container('docker') {
          sh "#!/bin/sh -e\ndocker build -t 778557655318.dkr.ecr.us-west-1.amazonaws.com/${serviceName}:${DOCKER_TAG} ."
        }
      }
    }
    stage('Push') {
      when {
        anyOf {
          branch 'master'
        }
      }
      steps {
        container('docker') {
	  script {
		docker.withRegistry('https://778557655318.dkr.ecr.us-west-1.amazonaws.com', 'ecr:us-west-1:carter-ecr') {
    		docker.image("778557655318.dkr.ecr.us-west-1.amazonaws.com/${serviceName}:${env.DOCKER_TAG}").push()
  		}
	  }
        }
      }
    }
    stage('Trigger deploy to dev') {
      when {
        branch 'master'
      }
      steps {
        build(job: '/vylla/deploy-descriptors-2/master', wait: false, parameters: [
          [$class: 'StringParameterValue', name: 'TAG', value: DOCKER_TAG],
          [$class: 'StringParameterValue', name: 'SERVICE', value: serviceName]
        ])
      }
    }
   }          
  }      
}
