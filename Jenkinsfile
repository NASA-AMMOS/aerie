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
		label 'coronado || Pismo || San-clemente || Sugarloaf'
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
				script {
					def statusCode = sh returnStatus: true, script:
					"""
					# setup nvm/node
					export NVM_DIR="\$HOME/.nvm"
					if [ ! -d \$NVM_DIR ]; then
						curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.33.8/install.sh | bash
					fi
					[ -s "\$NVM_DIR/nvm.sh" ] && . "\$NVM_DIR/nvm.sh"
					nvm install v10.13.0

					./scripts/build.sh --commit ${env.GIT_COMMIT} --tag ${getTag()} ${remoteBranch}
					"""
					if (statusCode > 0) {
						error "Failure setting up node"
					}
				}
				// TODO: Use this instead of the above script once node is installed on the server
				// sh "./scripts/build.sh --commit ${env.GIT_COMMIT} --tag ${getTag()} ${remoteBranch}"

				junit allowEmptyResults: true, healthScaleFactor: 10.0, keepLongStdio: true, testResults: '**/karma-test-results.xml'
			}
		}

		stage ('analyze') {
			when {
				expression { BRANCH_NAME ==~ /(develop|^release.*)/ }
			}
			steps {
				echo 'analyzing...'
				script {
					def statusCode = sh returnStatus: true, script:
					"""
					# setup nvm/node
					export NVM_DIR="\$HOME/.nvm"
					if [ ! -d \$NVM_DIR ]; then
						curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.33.8/install.sh | bash
					fi
					[ -s "\$NVM_DIR/nvm.sh" ] && . "\$NVM_DIR/nvm.sh"
					nvm install v10.13.0
					# install cloc for lines-of-code analysis
					npm install -g cloc

					./scripts/analyze.sh --commit ${env.GIT_COMMIT} --tag ${getTag()} ${remoteBranch}
					"""
					if (statusCode > 0) {
						error "Failure setting up node"
					}
				}
				// TODO: Use this instead of the above script once node is installed on the server
				// sh "./scripts/analyze.sh --commit ${env.GIT_COMMIT} --tag ${getTag()} ${remoteBranch}"
			}
		}

		stage ('build archive') {
			steps {
				echo 'archiving build files...'
				script {
					def statusCode = sh returnStatus: true, script:
					"""
					if [ -d nest/dist-mpsserver ]; then
						cd nest/dist-mpsserver
						tar -czf nest-${getTag()}.tar.gz `ls -A`
						cd ../../
					fi
					"""
					if (statusCode > 0) {
						error "Failure compressing mpsserver-dist"
					}
				}
				archiveArtifacts 'README.md,*-src-*.tar.gz,*-cloc-*.txt,**/target/*.jar,nest/dist-mpsserver/*.tar.gz'
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
								"pattern": "aerie-src-*.tar.gz",
								"target": "general-develop/gov/nasa/jpl/ammos/mpsa/aerie/",
								"recursive":false
							},
							{
								"pattern": "nest/dist-mpsserver/*.tar.gz",
								"target": "general-develop/gov/nasa/jpl/ammos/mpsa/nest/",
								"recursive":false
							}
						]
					}'''
					def buildInfo = server.upload spec: uploadSpec
					server.publishBuildInfo buildInfo
				}

				withCredentials([usernamePassword(credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86', passwordVariable: 'DOCKER_LOGIN_PASSWORD', usernameVariable: 'DOCKER_LOGIN_USERNAME')]) {
					sh "./scripts/publish.sh --commit ${env.GIT_COMMIT} --tag ${getTag()} ${remoteBranch}"
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
