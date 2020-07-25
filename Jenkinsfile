def getDockerCompatibleTag(tag) {
    return tag.replaceAll('\\+', '-')
}

def getDockerImageName(folder) {
    def list = []

    for (def file : findFiles(glob: "${DOCKERFILE_PATH}/*.*")) {
        def token = file.getName().tokenize('.')
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
    } else if (tag ==~ /develop/) {
        return "develop"
    } else if (tag ==~ /staging/) {
        return "staging"
    } else {
        return "unknown"
    }
}

def getArtifactoryUrl() {
    if (GIT_BRANCH ==~ /release-.*/){
        return "cae-artifactory.jpl.nasa.gov:16003"
    } else if (GIT_BRANCH ==~ /staging/) {
        return "cae-artifactory.jpl.nasa.gov:16002"
    } else {
        return "cae-artifactory.jpl.nasa.gov:16001"
    }
}

def getPublishPath() {
    if (GIT_BRANCH ==~ /release-.*/) {
        return "general/gov/nasa/jpl/aerie/"
    } else if (GIT_BRANCH ==~ /staging/) {
        return "general-stage/gov/nasa/jpl/aerie/"
    } else {
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

// Save built image name:tag
def buildImages = []
// Save dockerfile name inside script/Dockerfiles
def imageNames = []

pipeline {
    agent {
        label 'coronado || Pismo || San-clemente || Sugarloaf'
    }

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-11-openjdk'
        ARTIFACT_TAG = getArtifactTag()

        DOCKERFILE_PATH = "scripts/dockerfiles"
        DOCKER_TAG = "${getDockerCompatibleTag(ARTIFACT_TAG)}"
        DOCKERFILE_DIR = "${env.WORKSPACE}/scripts/dockerfiles"

        ARTIFACTORY_URL = getArtifactoryUrl()
        ARTIFACT_PATH = "${ARTIFACTORY_URL}/gov/nasa/jpl/aerie"

        AWS_ACCESS_KEY_ID = credentials('aerie-aws-access-key')
        AWS_SECRET_ACCESS_KEY = credentials('aerie-aws-secret-access-key')
        AWS_DEFAULT_REGION = 'us-gov-west-1'
        AWS_ECR = "448117317272.dkr.ecr.us-gov-west-1.amazonaws.com"
        AWS_ECR_PATH = "${AWS_ECR}/aerie"
        AWS_TAG = getAWSTag(DOCKER_TAG)
    }

    stages {
        stage('Setup') {
            steps {
                echo "Printing environment variables..."
                sh "env | sort"
            }
        }

        stage('Build') {
            steps {
                sh './gradlew build'

                // Jenkins will complain about "old" test results if Gradle didn't need to re-run them.
                // Bump their last modified time to trick Jenkins.
                sh 'find . -name "TEST-*.xml" -exec touch {} \\;'

                junit testResults: '*/build/test-results/test/*.xml'
            }
        }

        stage('Assemble') {
            steps {
                // TODO: Publish Merlin-SDK.jar to Maven/Artifactory

                sh """
                    echo ${BUILD_NUMBER}

                    # For adaptations
                    mkdir -p /tmp/aerie-jenkins/${BUILD_NUMBER}/adaptations
                    cp sample-adaptation/build/libs/*.jar \
                       banananation/build/libs/*.jar \
                       /tmp/aerie-jenkins/${BUILD_NUMBER}/adaptations/

                    # For services
                    mkdir -p /tmp/aerie-jenkins/${BUILD_NUMBER}/services
                    cp plan-service/build/distributions/*.tar \
                       adaptation-service/build/distributions/*.tar \
                       /tmp/aerie-jenkins/${BUILD_NUMBER}/services/

                    # For merlin-sdk
                    mkdir -p /tmp/aerie-jenkins/${BUILD_NUMBER}/merlin-sdk
                    cp merlin-sdk/build/libs/*.jar \
                       /tmp/aerie-jenkins/${BUILD_NUMBER}/merlin-sdk/

                    # For merlin-cli
                    mkdir -p /tmp/aerie-jenkins/${BUILD_NUMBER}/merlin-cli
                    cp merlin-cli/build/distributions/*.tar \
                       /tmp/aerie-jenkins/${BUILD_NUMBER}/merlin-cli/

                    tar -czf aerie-${ARTIFACT_TAG}.tar.gz -C /tmp/aerie-jenkins/${BUILD_NUMBER} .
                    tar -czf aerie-docker-compose.tar.gz -C ./scripts/docker-compose-aerie .
                """.stripIndent()
            }
        }

        stage('Release') {
            when {
                expression { GIT_BRANCH ==~ /(develop|staging|release-.*)/ }
            }

            steps {
                echo "Publishing to $ARTIFACTORY_URL"

                script {
                    def uploadSpec = """
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

                    try {
                        def server = Artifactory.newServer(
                            url: 'https://cae-artifactory.jpl.nasa.gov/artifactory',
                            credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86')

                        def buildInfo = server.upload spec: uploadSpec
                        server.publishBuildInfo buildInfo
                    } catch (Exception e) {
                        println("Publishing to Artifactory failed with exception: ${e.message}")
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        stage('Docker') {
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

        stage('Deploy') {
            when {
                expression { GIT_BRANCH ==~ /(develop|staging|release-.*)/ }
            }
            steps {
                withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding',
                    credentialsId: 'mpsa-aws-test-account']]
                ) {
                    script {
                        echo 'Logging out docker'
                        sh 'docker logout || true'
                        echo 'Logging into ECR'
                        sh 'aws ecr get-login-password | docker login --username AWS --password-stdin https://$AWS_ECR'

                        def filenames = getDockerImageName(env.DOCKERFILE_DIR)
                        docker.withRegistry(AWS_ECR) {
                            for (def name: imageNames) {
                                def old_tag_name = "$ARTIFACT_PATH/$name:$DOCKER_TAG"
                                def new_tag_name = "$AWS_ECR_PATH/$name:$AWS_TAG"

                                // retag the image and push to aws
                                sh "docker tag $old_tag_name $new_tag_name"
                                sh "docker push $new_tag_name"

                                buildImages.push(new_tag_name)
                            }

                            sleep 5
                            try {
                                sh '''
                                    aws ecs stop-task \
                                        --cluster "aerie-${AWS_TAG}-cluster" \
                                        --task $(aws ecs list-tasks \
                                                     --cluster "aerie-${AWS_TAG}-cluster" \
                                                     --output text \
                                                     --query taskArns[0])
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
    }

    post {
        always {
            script {
                println(buildImages)
                for (def image: buildImages) {
                    sh "docker rmi $image"
                }
            }
            echo 'Cleaning up images'
            sh "docker image prune -f"

            echo 'Logging out docker'
            sh 'docker logout || true'

            echo 'Remove temp folder'
            sh 'rm -rf /tmp/aerie-jenkins'
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
    }
}
