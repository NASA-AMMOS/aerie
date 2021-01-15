def getDockerCompatibleTag(tag) {
  def fixedTag = tag.replaceAll('\\+', '-')
  return fixedTag
}
def getDockerImageName(folder) {
  files = findFiles(glob: "${DOCKERFILE_PATH}/*.*")
  def list = []

  for (def file : files) {
    def token = (file.getName()).tokenize('.')
    if (token.size() > 1 && token[1] == "Dockerfile") {
      list.push(token[0])
    } else {
      println("File not added " + token[0])
    }
  }
  return list
}

def getAWSTag(tag) {
  if (tag ==~ /release-.*/) {
    return "release"
  }
  if (tag ==~ /develop/) {
    return "develop"
  }
  if (tag ==~ /staging/) {
    return "staging"
  }
  return "unknown"
}

def getArtifactoryUrl() {
  echo "Choosing an Artifactory port based off of branch name: $GIT_BRANCH"

  if (GIT_BRANCH ==~ /release-.*/){
    echo "Publishing to 16003-RELEASE-LOCAL"
    return "cae-artifactory.jpl.nasa.gov:16003"
  }
  else if (GIT_BRANCH ==~ /staging/) {
    echo "Publishing to 16002-STAGE-LOCAL"
    return "cae-artifactory.jpl.nasa.gov:16002"
  }
  else {
    echo "Publishing to 16001-DEVELOP-LOCAL"
    return "cae-artifactory.jpl.nasa.gov:16001"
  }
}

def getPublishPath() {
  if (GIT_BRANCH ==~ /release-.*/) {
    return "general/gov/nasa/jpl/aerie/"
  }
  else if (GIT_BRANCH ==~ /staging/) {
    return "general-stage/gov/nasa/jpl/aerie/"
  }
  else {
    return "general-develop/gov/nasa/jpl/aerie/"
  }
}

def getArtifactTag() {
  if (GIT_BRANCH ==~ /(develop|staging|release-.*)/) {
    return GIT_BRANCH
  } else {
    return GIT_COMMIT
  }
}

void setBuildStatus(String message, String state, String context) {
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: env.GIT_URL],
      commitShaSource: [$class: "ManuallyEnteredShaSource", sha: env.GIT_COMMIT],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: context],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [
        [$class: "AnyBuildResult", message: message, state: state.toUpperCase()] ] ]
  ]);
}

// Save built image name:tag
def buildImages = []
// Save dockerfile name inside scipt/Dockerfiles
def imageNames = []

pipeline {

  agent {
    label 'coronado || Pismo || San-clemente || Sugarloaf'
  }

  environment {
    ARTIFACT_TAG = "${getArtifactTag()}"
    ARTIFACTORY_URL = "${getArtifactoryUrl()}"
    AWS_DEFAULT_REGION = 'us-gov-west-1'
    AWS_ACCESS_KEY_ID = credentials('aerie-aws-access-key')
    AWS_ECR = "448117317272.dkr.ecr.us-gov-west-1.amazonaws.com"
    AWS_SECRET_ACCESS_KEY = credentials('aerie-aws-secret-access-key')
    AERIE_SECRET_ACCESS_KEY = credentials('Aerie-Access-Token')
    DOCKER_TAG = "${getDockerCompatibleTag(ARTIFACT_TAG)}"
    AWS_TAG = "${getAWSTag(DOCKER_TAG)}"
    DOCKERFILE_DIR = "${env.WORKSPACE}/scripts/dockerfiles"
    LD_LIBRARY_PATH = "/usr/local/lib64:/usr/local/lib:/usr/lib64:/usr/lib"
    ARTIFACT_PATH = "${ARTIFACTORY_URL}/gov/nasa/jpl/aerie"
    AWS_ECR_PATH = "${AWS_ECR}/aerie"
    DOCKERFILE_PATH = "scripts/dockerfiles"
    JAVA_HOME = "/usr/lib/jvm/java-11-openjdk"
  }

  stages {
    stage ('Setup') {
      steps {
        echo "Printing environment variables..."
        sh "env | sort"
      }
    }
    stage ('Build') {
      steps {
        script { setBuildStatus("Building", "pending", "jenkins/branch-check"); }
        echo "Building $ARTIFACT_TAG..."
        sh './gradlew classes'
      }
    }
    stage ('Test') {
      steps {
        script { setBuildStatus("Testing", "pending", "jenkins/branch-check"); }
        sh "./gradlew test"

          // Jenkins will complain about "old" test results if Gradle didn't need to re-run them.
          // Bump their last modified time to trick Jenkins.
          sh 'find . -name "TEST-*.xml" -exec touch {} \\;'

          junit testResults: '*/build/test-results/test/*.xml'
      }
    }
    stage ('Assemble') {
      steps {
        echo 'Publishing JARs and Aerie Docker Compose to Artifactory...'
        sh '''
        ASSEMBLE_PREP_DIR=$(mktemp -d)
        STAGING_DIR=$(mktemp -d)

        ./gradlew assemble

        # For adaptations
        mkdir -p ${ASSEMBLE_PREP_DIR}/adaptations
        cp banananation/build/libs/*.jar \
           ${ASSEMBLE_PREP_DIR}/adaptations/

        # For services
        mkdir -p ${ASSEMBLE_PREP_DIR}/services
        cp plan-service/build/distributions/*.tar \
           adaptation-service/build/distributions/*.tar \
           ${ASSEMBLE_PREP_DIR}/services/

        # For merlin-cli
        mkdir -p ${ASSEMBLE_PREP_DIR}/merlin-cli
        cp merlin-cli/build/distributions/*.tar \
           ${ASSEMBLE_PREP_DIR}/merlin-cli/

        # For docker-compose
        cp -r ./scripts/docker-compose-aerie ${STAGING_DIR}

        if [[ $GIT_BRANCH =~ staging ]] || [[ $GIT_BRANCH =~ release-.* ]]; then
            cd ${STAGING_DIR}/docker-compose-aerie
            echo "# This file contains environment variables used in docker-compose files." > .env
            echo "AERIE_DOCKER_URL=$ARTIFACT_PATH" >> .env
            echo "DOCKER_TAG=$DOCKER_TAG" >> .env
            cd -
        fi

        tar -czf aerie-${ARTIFACT_TAG}.tar.gz -C ${ASSEMBLE_PREP_DIR}/ .
        tar -czf aerie-docker-compose.tar.gz -C ${STAGING_DIR}/ .
        rm -rfv ${ASSEMBLE_PREP_DIR}
        rm -rfv ${STAGING_DIR}
        '''
      }

    }
    stage ('Release') {
      when {
        expression { GIT_BRANCH ==~ /(develop|staging|release-.*)/ }
      }
      steps {
        script {
          try {
            def server = Artifactory.newServer url: 'https://cae-artifactory.jpl.nasa.gov/artifactory', credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86'
            def uploadSpec =
            """
            {
              "files": [
                {
                  "pattern": "aerie-${ARTIFACT_TAG}.tar.gz",
                  "target": "${getPublishPath()}",
                  "recursive":false
                },
                {
                  "pattern": "aerie-docker-compose.tar.gz",
                  "target": "${getPublishPath()}",
                  "recursive":false
                }
              ]
            }
            """
            def buildInfo = server.upload spec: uploadSpec
            server.publishBuildInfo buildInfo
          } catch (Exception e) {
              println("Publishing to Artifactory failed with exception: ${e.message}")
              currentBuild.result = 'UNSTABLE'
          }
        }
      }
    }
    stage ('Docker') {
      when {
        expression { GIT_BRANCH ==~ /(develop|staging|release-.*)/ }
      }
      steps {
        script {
          imageNames = getDockerImageName(env.DOCKERFILE_DIR)
          docker.withRegistry("https://$ARTIFACTORY_URL", '9db65bd3-f8f0-4de0-b344-449ae2782b86') {
            for (def name: imageNames) {
              def tag_name="$ARTIFACT_PATH/$name:$DOCKER_TAG"
              def image = docker.build("${tag_name}", "--progress plain -f ${DOCKERFILE_PATH}/${name}.Dockerfile --rm ." )
              image.push()
              buildImages.push(tag_name)
            }
          }
        }
      }
    }
    stage ('Deploy') {
      when {
        expression { GIT_BRANCH ==~ /(develop|staging|release-.*)/ }
      }
      steps {
        echo 'Deployment stage started...'
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'mpsa-aws-test-account']]) {
          script {
            echo 'Logging out docker'
            sh 'docker logout || true'
            echo 'Logging into ECR'
            sh('aws ecr get-login-password | docker login --username AWS --password-stdin https://$AWS_ECR')

            def filenames = getDockerImageName(env.DOCKERFILE_DIR)
            docker.withRegistry(AWS_ECR) {
              for (def name: imageNames) {
                def old_tag_name="$ARTIFACT_PATH/$name:$DOCKER_TAG"
                def new_tag_name="$AWS_ECR_PATH/$name:$AWS_TAG"
                def changeTagCmd = "docker tag $old_tag_name $new_tag_name"
                def pushCmd = "docker push $new_tag_name"

                // retag the image and push to aws
                sh changeTagCmd
                sh pushCmd
                buildImages.push(new_tag_name)
              }
              sleep 5
              try {
                sh '''
                 aws ecs stop-task --cluster "aerie-${AWS_TAG}-cluster" --task $(aws ecs list-tasks --cluster "aerie-${AWS_TAG}-cluster" --output text --query taskArns[0])
                '''
              } catch (Exception e) {
                echo "Restarting failed since the task does not exist."
                echo e.getMessage()
              }
            }
          }
        }
      }
    }
    stage ('Generate Javadoc for Merlin-SDK') {
      when {
        expression { GIT_BRANCH ==~ /(develop|staging|release-.*)/ }
      }
      steps {
        sh """
        JAVADOC_PREP_DIR=\$(mktemp -d)

        ./gradlew merlin-sdk:javadoc
        cp -r merlin-sdk/build/docs/javadoc/ \${JAVADOC_PREP_DIR}/.

        git checkout gh-pages
        rsync -av --delete \${JAVADOC_PREP_DIR}/javadoc/ javadoc
        rm -rf \${JAVADOC_PREP_DIR}

        git config user.email "achong@jpl.nasa.gov"
        git config user.name "Jenkins gh-pages sync"
        git add javadoc/
        git diff --quiet HEAD || git commit -m "Publish Javadocs for commit ${GIT_COMMIT}"
        git push https://${AERIE_SECRET_ACCESS_KEY}@github.jpl.nasa.gov/Aerie/aerie.git gh-pages
        git checkout ${GIT_BRANCH}
        """
      }
    }
  }
  post {
    always {
      script {
        println(buildImages)
        for(def image: buildImages) {
          def removeCmd = "docker rmi $image"
          sh removeCmd
        }
      }
      echo 'Cleaning up images'
      sh "docker image prune -f"

      echo 'Logging out docker'
      sh 'docker logout || true'

      setBuildStatus("Build ${currentBuild.currentResult}", "${currentBuild.currentResult}", "jenkins/branch-check")
    }
    unstable {
      emailext subject: "Jenkins UNSTABLE: ${env.JOB_BASE_NAME} #${env.BUILD_NUMBER}",
      body: """
          <p>Jenkins job unstable (failed tests): <br> <a href=\"${env.BUILD_URL}\">${env.JOB_NAME} #${env.BUILD_NUMBER}</a></p>
      """,
      mimeType: 'text/html',
      recipientProviders: [[$class: 'FailingTestSuspectsRecipientProvider']]
    }
    failure {
      emailext subject: "Jenkins FAILURE: ${env.JOB_BASE_NAME} #${env.BUILD_NUMBER}",
      body: """
          <p>Jenkins job failure: <br> <a href=\"${env.BUILD_URL}\">${env.JOB_NAME} #${env.BUILD_NUMBER}</a></p>
      """,
      mimeType: 'text/html',
      recipientProviders: [[$class: 'CulpritsRecipientProvider']]
    }
    cleanup {
      cleanWs()
      deleteDir()
    }
  }
}
