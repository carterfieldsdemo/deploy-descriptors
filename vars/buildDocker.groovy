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
    DOCKER_TAG  = "${(env.BRANCH_NAME == 'develop' ? 'dev-' : (env.BRANCH_NAME == 'hotfix' ? 'hotfix-' : '' )) + env.BUILD_NUMBER}"
#    NEXUS=credentials('jenkins-nexus')
#    NPMRC="""@vylla:registry=https://nexus.vylla.com/repository/vylla-npm///nexus.vylla.com/repository/vylla-npm/:_auth=${(NEXUS_USR + ":" + NEXUS_PSW).bytes.encodeBase64().toString()}
"""
  }
  stages {
    stage('Build') {
      steps {
        container('docker') {
          sh "#!/bin/sh -e\ndocker build --build-arg NPMRC=\"${NPMRC}\" -t 895239460497.dkr.ecr.us-east-1.amazonaws.com/${serviceName}:${DOCKER_TAG} ."
        }
      }
    }
    stage('Push') {
      when {
        anyOf {
          branch 'master'
          branch 'develop'
          branch 'hotfix'
        }
      }
      steps {
        container('docker') {
	  script {
		docker.withRegistry('https://895239460497.dkr.ecr.us-east-1.amazonaws.com', 'ecr:us-east-1:ecr-vylla') {
    		docker.image("895239460497.dkr.ecr.us-east-1.amazonaws.com/${serviceName}:${env.DOCKER_TAG}").push()
  		}
	  }
        }
      }
    }
    stage('Trigger deploy to dev') {
      when {
        branch 'develop'
      }
      steps {
        build(job: '/vylla/deploy-descriptors-2/master', wait: false, parameters: [
          [$class: 'StringParameterValue', name: 'TAG', value: DOCKER_TAG],
          [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: 'DEV'],
          [$class: 'StringParameterValue', name: 'SERVICE', value: serviceName]
        ])
      }
    }
   }          
  }      
}
