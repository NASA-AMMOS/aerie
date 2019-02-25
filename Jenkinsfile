
def getTag() {
	def branchName = env.BRANCH_NAME.replaceAll('/', '_').replace('release_', '')
	def shortDate = new Date().format('yyyyMMdd') 
	def shortCommit = env.GIT_COMMIT.take(8)
	return "${branchName}_b${BUILD_NUMBER}_r${shortCommit}_${shortDate}"
}

pipeline {

	options {
		disableConcurrentBuilds()
	}

	agent {
		label 'San-clemente'
	}

	stages {
		stage ('build') {
			steps {
				echo "building ${getTag()}..."
				sh "rm -rf aerie-* cloc* /tmp/cloc*"
				sh "chmod +x ./scripts/build.sh && ./scripts/build.sh --commit ${env.GIT_COMMIT} --tag ${getTag()}"
				junit healthScaleFactor: 10.0, keepLongStdio: true, testResults: '**/karma-test-results.xml'
			}
		}

		stage ('analyze') {
			when {
				expression { BRANCH_NAME ==~ /(develop|release.*)/ }
			}
			steps {
				echo 'analyzing...'
				sh "chmod +x ./scripts/analyze.sh && ./scripts/analyze.sh --commit ${env.GIT_COMMIT} --tag ${getTag()}"
			}
		}

		stage ('archive') {
			steps {
				echo 'archiving...'
      			sh "tar -czf aerie-src-${getTag()}.tar.gz --exclude='.git' `ls -A`"
				archiveArtifacts '*-src-*.tar.gz,*-cloc-*.txt,**/coverage/**/*,**/karma-test-results.xml'
			}
		}

		stage ('publish') {
			when {
				expression { BRANCH_NAME ==~ /(develop|release.*|PR-.*)/ }
			}
			steps {
				echo 'publishing...'
				script {
					def server = Artifactory.newServer url: 'https://cae-artifactory.jpl.nasa.gov/artifactory', credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86'
					def uploadSpec =
					'''{
						"files": [
							{
								"pattern": "*.tar.gz",
								"target": "general-develop/gov/nasa/jpl/ammos/mpsa/aerie/",
								"recursive":false
							}
						]
					}'''
					def buildInfo = server.upload spec: uploadSpec
					server.publishBuildInfo buildInfo
				}

				withCredentials([usernamePassword(credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86', passwordVariable: 'DOCKER_LOGIN_PASSWORD', usernameVariable: 'DOCKER_LOGIN_USERNAME')]) {
					sh "chmod +x ./scripts/publish.sh && ./scripts/publish.sh --commit ${env.GIT_COMMIT} --tag ${getTag()}"
				}

			}
		}
	}

	post {
		always {
			echo "cleaning up..."
			sh "chmod +x ./scripts/cleanup.sh && ./scripts/cleanup.sh --commit ${env.GIT_COMMIT} --tag ${getTag()}"
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
