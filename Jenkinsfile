pipeline {

	options {
		disableConcurrentBuilds()
	}

	agent {
		label 'coronado || Pismo || San-clemente || Sugarloaf'
	}

	stages {

		stage ('build') {
			steps {
				script {
					def statusCode = sh returnStatus: true, script:
					'''
						# don't echo commands by default
						set +x

						# setup env
						export PATH=/usr/local/bin:/usr/bin
						export LD_LIBRARY_PATH=/usr/local/lib64:/usr/local/lib:/usr/lib64:/usr/lib
						export BUILD_DATE=`date +%Y%m%d`
						export REPO_REV_SHORT=`git rev-parse --short HEAD`
						# remove forward slash and release from branch name
						export BRANCH_VERSION=`echo ${BRANCH_NAME} | sed -e 's/\\//_/g' -e 's/^release_//g'`
						export SEQBASETAG="${BRANCH_VERSION}+b${BUILD_NUMBER}.r${REPO_REV_SHORT}.${BUILD_DATE}"

						# setup nvm/node
						export NVM_DIR="$HOME/.nvm"
						# install nvm if necessary
						if [ ! -d $NVM_DIR ]; then
							curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.33.8/install.sh | bash
						fi
						# load nvm shell commands
						[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
						# install/use proper node version
						nvm install v10.13.0

						echo -e "\ncurrent environment variables:\n"
						env | sort

						if [[ $BRANCH_NAME == release* ]]; then
							echo "Release branch detected. Analyzing and archiving source for CM purposes..."
							# tar up source
							tar -czf nest-src-$SEQBASETAG.tar.gz --exclude='.git' `ls -A`
							# record lines of code
							npm install cloc
							./node_modules/cloc/lib/cloc --exclude-dir=bower_components src --report-file=nest-cloc-$SEQBASETAG.txt
						fi

						# build and test
						./build_dist.sh
					'''
					if (statusCode > 0) {
						error "Build failure detected. See log."
					}
				}
				echo "Analyzing JUnit tests..."
				junit healthScaleFactor: 10.0, keepLongStdio: true, testResults: 'karma-test-results.xml'
			}
		}

	stage ('analyze') {
		steps {
			script {
				def statusCode = sh returnStatus: true, script:
				'''
					# don't echo commands by default
					set +x

					if [[ $BRANCH_NAME == 'develop' || $BRANCH_NAME == release* ]]; then
						echo "Anaylsis branch detected. Performing code analysis..."

						# setup nvm/node
						export NVM_DIR="$HOME/.nvm"
						# install nvm if necessary
						if [ ! -d $NVM_DIR ]; then
							curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.33.8/install.sh | bash
						fi
						# load nvm shell commands
						[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
						# install/use proper node version
						nvm install v10.13.0

						# run sonarqube analysis
						npm run sonarqube
					else
						echo "Skipping analysis."
					fi
				'''
				if (statusCode > 0) {
					error "Analysis failure detected. See log."
				}
			}
		}
	}

		stage ('publish') {
			steps {
				echo "Archiving artifacts in Jenkins..."
				archiveArtifacts 'dist/*.tar.gz,*-src-*.tar.gz,*-cloc-*.txt,coverage/**/*,*/karma-test-results.xml'
				script {
					if ( env.BRANCH_NAME.equals('develop') || env.BRANCH_NAME.startsWith('release') || env.BRANCH_NAME.startsWith('PR-') ) {
						echo "Release branch detected. Publishing files to Artifactory..."
						// credentialsId refers to cmci account
						def server = Artifactory.newServer url: 'https://cae-artifactory.jpl.nasa.gov/artifactory', credentialsId: '9db65bd3-f8f0-4de0-b344-449ae2782b86'
						def uploadSpec =
						'''{
							"files": [
								{
									"pattern": "*-src-*.tar.gz",
									"target": "general-develop/gov/nasa/jpl/ammos/mpsa/nest/",
									"recursive":false
								},
								{
									"pattern": "dist/*.tar.gz",
									"target": "general-develop/gov/nasa/jpl/ammos/mpsa/nest/",
									"recursive":false
								}
							]
						}'''
						def buildInfo = server.upload spec: uploadSpec
						server.publishBuildInfo buildInfo
					}
				}
			}
		}

	}

	post {

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
