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
    NEXUS=credentials('jenkins-nexus')
    NPMRC="""
@vylla:registry=https://nexus.vylla.com/repository/vylla-npm/
//nexus.vylla.com/repository/vylla-npm/:_auth=${(NEXUS_USR + ":" + NEXUS_PSW).bytes.encodeBase64().toString()}
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
    stage('Trigger loan automation job') {
      when {
        expression { serviceName == 'vylla-fe-loaniq'}
      }
      steps {
        sh "curl -X POST http://devops:11565eae9277e7e316a7e1226285aba54a@10.15.5.13/job/loan-automation/build?token=sMMswwUkvA7BpNhmnxaa9qm1yIB4oshA"
      }
    }
    stage('Trigger my account automation job') {
      when {
        expression { serviceName == 'vylla-fe-user'}
      }
      steps {
        sh "curl -X POST http://devops:11565eae9277e7e316a7e1226285aba54a@10.15.5.13/job/my-account-automation/build?token=sMMswwUkvA7BpNhmnxaa9qm1yIB4oshA"
      }
    }
    stage('Trigger homesearch automation job') {
      when {
        expression { serviceName == 'vylla-fe-homeiq'}
      }
      steps {
        sh "curl -X POST http://devops:11565eae9277e7e316a7e1226285aba54a@10.15.5.13/job/homesearch-automation/build?token=sMMswwUkvA7BpNhmnxaa9qm1yIB4oshA"
      }
    }
    stage('Trigger static automation job') {
      when {
        expression { serviceName == 'vylla-fe-statics'}
      }
      steps {
        sh "curl -X POST http://devops:11565eae9277e7e316a7e1226285aba54a@10.15.5.13/job/static-automation/build?token=sMMswwUkvA7BpNhmnxaa9qm1yIB4oshA"
      }
    }
  }
}
}
