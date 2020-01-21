def getTag() {
	def branchName = env.BRANCH_NAME.replaceAll('/', '_').replace('release_', '')
	def shortDate = new Date().format('yyyyMMdd')
	def shortCommit = env.GIT_COMMIT.take(7)
	return "${branchName}+b${BUILD_NUMBER}.r${shortCommit}.${shortDate}"
}

def remoteBranch = ''
if (env.CHANGE_TARGET) {
	remoteBranch = "--branch origin/${env.CHANGE_TARGET}"
}

pipeline {

	options {
		disableConcurrentBuilds()
	}

	agent {
		label 'Pismo || San-clemente || Sugarloaf'
	}

	stages {

		stage ('src archive') {
			when {
				expression { BRANCH_NAME ==~ /^release.*/ }
			}
			steps {
				echo 'archiving source code...'
				sh "tar -czf aerie-src-${getTag()}.tar.gz --exclude='.git' `ls -A`"
			}
		}

		stage ('build') {
			steps {
				echo "building ${getTag()}..."
				withCredentials([usernamePassword(credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86', passwordVariable: 'DOCKER_LOGIN_PASSWORD', usernameVariable: 'DOCKER_LOGIN_USERNAME')]) {
				script {
					def statusCode = sh returnStatus: true, script:
					"""
					# setup env
					export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
					export MAVEN_HOME=/usr/local/share/maven
					export PATH=\$JAVA_HOME/bin:\$MAVEN_HOME/bin:/usr/local/bin:/usr/bin
					export LD_LIBRARY_PATH=/usr/local/lib64:/usr/local/lib:/usr/lib64:/usr/lib

					echo -e "\ncurrent environment variables:\n"
					env | sort

					./scripts/build.sh --commit ${env.GIT_COMMIT} --tag ${getTag()} ${remoteBranch}
					"""
					if (statusCode > 0) {
						error "Failure build"
					}
				}
				}

				junit allowEmptyResults: true, healthScaleFactor: 10.0, keepLongStdio: true, testResults: '**/karma-test-results.xml'
			}
		}

		stage ('build archive') {
			steps {
				echo 'archiving build files...'
				script {
					def statusCode = sh returnStatus: true, script:
					"""
					tar -czf aerie-${getTag()}.tar.gz --exclude='.git' --exclude='aerie-src-*.tar.gz'  `ls -A`
					"""
					if (statusCode > 0) {
						error "Failure build archive"
					}
				}
			}
		}

		stage ('publish') {
			when {
				expression { BRANCH_NAME ==~ /(develop|release.*|PR-.*)/ }
			}
			steps {
				echo 'publishing...'
				script {
					try {
						def server = Artifactory.newServer url: 'https://cae-artifactory.jpl.nasa.gov/artifactory', credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86'
						def uploadSpec =
						'''{
							"files": [
								{
									"pattern": "aerie-*.tar.gz",
									"target": "general-develop/gov/nasa/jpl/ammos/mpsa/aerie/",
									"recursive":false
								}
							]
						}'''
						def buildInfo = server.upload spec: uploadSpec
						server.publishBuildInfo buildInfo
					} catch (Exception e) {
						println("Publishing to Artifactory failed with exception: ${e.message}")
						currentBuild.result = 'UNSTABLE'
					}
				}

				withCredentials([usernamePassword(credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86', passwordVariable: 'DOCKER_LOGIN_PASSWORD', usernameVariable: 'DOCKER_LOGIN_USERNAME')]) {
					script {
						def statusCode = sh returnStatus: true, script:
						"""
						export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
						export MAVEN_HOME=/usr/local/share/maven
						export PATH=\$JAVA_HOME/bin:\$MAVEN_HOME/bin:/usr/local/bin:/usr/bin

						./scripts/publish.sh --commit ${env.GIT_COMMIT} --tag ${getTag()} ${remoteBranch}
						"""
						if (statusCode > 0) {
							error "Failure publish"
						}
					}
				}

			}
		}
	}

	post {
		always {
			echo "cleaning up..."
			sh "./scripts/cleanup.sh --commit ${env.GIT_COMMIT} --tag ${getTag()} ${remoteBranch}"
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
