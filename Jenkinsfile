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

        stage('run') {

            container('schema') {
                sh """
                   echo "Running elastic search job"
                   cd /app
                   sh import-es cdev
                  """
                notifySuccessful()
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
