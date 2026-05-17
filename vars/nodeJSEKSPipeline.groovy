def call(configMap) {
    pipeline {
      agent {
        label 'AGENT-1'
      }
      options {
          timeout(time: 30, unit: 'MiNUTES')
          disableConcurrentBuilds()
      }
      parameters{
          booleanParam(name: 'deploy', defaultValue: false, description: 'Select to deploy or not')
      }

      environment {
          DEBUG = 'true'
          appVersion = ''
          account_id = pipelineGlobals.getAccountID('development')
          region = 'us-east-1'
          project = configMap.get('project')
          environment = 'development'
          component = configMap.get('component')
      }
      stages {
          stage('Read Version') {
              steps {
                  script {
                      def packageJson = readJSON file: 'package.json'
                      appVersion = packageJson.version
                      echo "App Version: ${appVersion}"
                  }
              }
          }
          stage('Install Dependencies') {
            steps {
                sh "npm install"
            }
          }
          /*stage('SonarQube analysis') {
              environment {
                  SCANNER_HOME = tool 'sonar-8.0' // this is the name of the sonar-scanner installation in jenkins global tools configuration
              }
              steps {
                  // this step will run sonar-scanner with the environment variable SCANNER_HOME, which points to the installation directory of sonar-scanner in jenkins
                  withSonarQubeEnv('sonar-8.0') {
                      sh '$SCANNER_HOME/bin/sonar-scanner' // execute the sonar-scanner command, which will automatically pick up the configuration from the sonar-project.properties file in the root of the project
                  }
              }
          }

          stage('SQuality Gate') {
              steps {
                  timeout(time: 5, unit: 'MINUTES') {
                      waitForQualityGate abortPipeline: true
                  }
              }
          }*/
          stage('Building image') {
              steps {
                  withAWS(region: 'us-east-1', credentials: 'aws-creds') {
                      sh """
                          aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.us-east-1.amazonaws.com

                          docker build -t ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appVersion} .

                          docker images

                          docker push ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appVersion}
                      """
                  }
              }
          }
          stage('Deploy'){
              when {
                  expression { params.deploy }
              }
              steps{
                  build job: '../backend/backend-cd', parameters: [
                      string(name: 'version', value: "$appVersion"),
                      string(name: 'environment', value: "development"),
                  ], wait: true
              }
          }
      }

      post {
          always{
              echo "This sections runs always"
              deleteDir()
          }
          success {
              echo "Build Successful"
          }
          failure {
              echo "Build Failed"
          }
          unstable {
              echo "Build Unstable"
          }
          changed {
              echo "Status changed from previous build"
          }
      }
  }
}
