def getDockerCompatibleTag(tag){
	def fixedTag = tag.replaceAll('\\+', '-')
	return fixedTag
}

def getArtifactoryUrl() {
	echo "Choosing an Artifactory port based off of branch name: $GIT_BRANCH"

    if (GIT_BRANCH ==~ /release/){
		echo "Publishing to 16002-STAGE-LOCAL"
        return "cae-artifactory.jpl.nasa.gov:16002"
    }
    else {
		echo "Publishing to 16001-DEVELOP-LOCAL"
        return "cae-artifactory.jpl.nasa.gov:16001"
    }
}

pipeline {

	agent {
		// NOTE: DEPLOY WILL ONLY WORK WITH Coronado SERVER SINCE AWS CLI VERSION 2 IS ONLY INSTALLED ON THAT SERVER.
		// label 'coronado || Pismo || San-clemente || Sugarloaf'
		label 'coronado'
	}

	environment {
		ARTIFACT_TAG = "${GIT_BRANCH}"
		ARTIFACTORY_URL = "${getArtifactoryUrl()}"
		AWS_ACCESS_KEY_ID = credentials('aerie-aws-access-key')
    	AWS_DEFAULT_REGION = 'us-gov-west-1'
		AWS_ECR = "448117317272.dkr.ecr.us-gov-west-1.amazonaws.com"
    	AWS_SECRET_ACCESS_KEY = credentials('aerie-aws-secret-access-key')
		BUCK_HOME = "/usr/local/bin"
		BUCK_OUT = "${env.WORKSPACE}/buck-out"
		DOCKER_TAG = "${getDockerCompatibleTag(ARTIFACT_TAG)}"
		DOCKERFILE_DIR = "${env.WORKSPACE}/scripts/dockerfiles"
		JDK11_HOME = "/usr/lib/jvm/java-11-openjdk"
		LD_LIBRARY_PATH = "/usr/local/lib64:/usr/local/lib:/usr/lib64:/usr/lib"
		WATCHMAN_HOME = "/opt/watchman"
	}

	stages {
		stage('Setup'){
			steps {
				echo "Printing environment variables..."
				sh "env | sort"
			}
		}

		stage ('Build') {
			steps {
				echo "Building $ARTIFACT_TAG..."
				script {
					def statusCode = sh returnStatus: true, script:
					"""
					echo "Building all build targets"
					buck build //...
					"""
					if (statusCode > 0) {
						error "Failure in Build stage."
					}
				}
			}
		}

		stage ('Test') {
			steps {
				echo "TODO: Run tests via BUCK"
				// sh "buck test //..."
			}
		}

		stage ('Docker') {
			when {
				expression { GIT_BRANCH ==~ /(develop|staging|release)/ }
			}
			steps {
				withCredentials([usernamePassword(credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86', passwordVariable: 'DOCKER_LOGIN_PASSWORD', usernameVariable: 'DOCKER_LOGIN_USERNAME')]) {
					script {
						def statusCode = sh returnStatus: true, script:
						"""
							docker logout
							echo "${DOCKER_LOGIN_PASSWORD}" | docker login -u "${DOCKER_LOGIN_USERNAME}" $ARTIFACTORY_URL --password-stdin

							docker_dir=$DOCKERFILE_DIR
							for filename in \$docker_dir/*.Dockerfile; do
								[ -e "\$filename" ] || continue

								f=\${filename:\${#docker_dir}+1}
								d=\${f::(-11)}
								tag_name="$ARTIFACTORY_URL/gov/nasa/jpl/ammos/mpsa/aerie/\$d:$DOCKER_TAG"

								printf "Building \$d Docker container"
								docker build --progress plain -f \$filename -t "\$tag_name" --rm .
								docker push \$tag_name
							done
						"""
						if (statusCode > 0) {
							error "Failure in Docker stage."
						}
					}
				}
			}
		}

		stage ('Archive') {
			when {
				expression { GIT_BRANCH ==~ /(develop|staging|release)/ }
			}
			steps {
				// TODO: Publish Merlin-SDK.jar to Maven/Artifactory

				echo 'Publishing JARs to Artifactory...'
				script {
					def statusCode = sh returnStatus: true, script:
					"""
					# Tar up all build artifacts.
					find . -name "*.jar" ! -path "**/third-party/*" ! -path "**/.buckd/*"  ! -path "**/*test*/*" ! -name "*abi.jar" | tar -czf aerie-${ARTIFACT_TAG}.tar.gz -T -
					"""

					if (statusCode > 0) {
						error "Failure in Archive stage."
					}
				}

				script {
					try {
						def server = Artifactory.newServer url: 'https://cae-artifactory.jpl.nasa.gov/artifactory', credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86'
						def uploadSpec =
						'''
						{
							"files": [
								{
									"pattern": "aerie-${ARTIFACT_TAG}.tar.gz",
									"target": "general-develop/gov/nasa/jpl/ammos/mpsa/aerie/",
									"recursive":false
								}
							]
						}
						'''
						def buildInfo = server.upload spec: uploadSpec
						server.publishBuildInfo buildInfo
					} catch (Exception e) {
						println("Publishing to Artifactory failed with exception: ${e.message}")
						currentBuild.result = 'UNSTABLE'
					}
				}
			}
		}

		stage('Deploy') {
			when {
				expression { GIT_BRANCH ==~ /(develop|staging|release)/ }
			}
			steps {
				echo 'Deployment stage started...'
				withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'mpsa-aws-test-account']]) {
					script{
						echo 'Logging out docker'
						sh 'docker logout || true'

						echo 'Logging into ECR'
						sh ('aws ecr get-login-password | docker login --username AWS --password-stdin https://$AWS_ECR')

						docker.withRegistry(AWS_ECR){
							echo "Tagging docker images to point to AWS ECR"
							sh '''
							docker tag $(docker images | awk '\$1 ~ /plan/ { print \$3; exit }') ${AWS_ECR}/aerie/plan:${GIT_BRANCH}
							'''
							sh '''
							docker tag $(docker images | awk '\$1 ~ /adaptation/ { print \$3; exit }') ${AWS_ECR}/aerie/adaptation:${GIT_BRANCH}
							'''

							echo 'Pushing images to ECR'
							sh "docker push ${AWS_ECR}/aerie/plan:${GIT_BRANCH}"
							sh "docker push ${AWS_ECR}/aerie/adaptation:${GIT_BRANCH}"

							sleep 5
							try {
								sh '''
								aws ecs stop-task --cluster "aerie-${GIT_BRANCH}-cluster" --task $(aws ecs list-tasks --cluster "aerie-${GIT_BRANCH}-cluster" --output text --query taskArns[0])
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
			echo 'Cleaning up images'
			sh "docker image prune -f"

			echo 'Logging out docker'
			sh 'docker logout || true'
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
