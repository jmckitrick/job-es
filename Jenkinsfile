library identifier: 'jenkins-global-lib'

try {
    kube().
    pod("kubernetes", "job-elastic-search").
    withServiceAccount("jenkins").
    withGitCrypt().
    withContainer(kube.steps.containerTemplate(
        name: 'schema',
        image: "docker.infra.tstllc.net/job/job-es:latest",
        command: 'cat',
        ttyEnabled: true,
        alwaysPullImage: true,
        resourceRequestCpu: '100m',
        resourceLimitCpu: '2000m',
        resourceRequestMemory: '512Mi',
        resourceLimitMemory: '1536Mi')).
    node {

        stage('checkout') {
            dir( 'kube-deploy' ) {
                gitty().checkout('kube-deploy', 'master')
            }
        }

        crypt(pods: true, dir: "${WORKSPACE}/kube-deploy" ).unlocked {
            stage('run') {

                container('schema') {
                    sh """
                       set +x
                       export DB_USER=\$(grep "^DB_USER" ${WORKSPACE}/kube-deploy/.secrets/${SECRETS}.env | cut -f2 -d=)
                       export DB_PASS=\$(grep "^DB_PASSWORD" ${WORKSPACE}/kube-deploy/.secrets/${SECRETS}.env | cut -f2 -d=)
                       echo "Using $DB_USER"
                       echo "Running elastic search job"
                       cd /app
                       sh import-es cdev
                      """

                    notifySuccessful()
                }

            }

        }

    }
} catch (e) {
    notifyFailed()
    throw e
}

def notifySuccessful() {
    slackSend channel: "#agent", color: "good", message: "Job: 'job-elastic-search' SUCCESSFUL"
}

def notifyFailed() {
    slackSend channel: "#agent", color: "#FF0000", message: "Job: 'job-elastic-search' FAILED"
}
