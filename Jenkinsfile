library identifier: 'jenkins-global-lib'

try {
    kube().
    pod("kubernetes", "job-report-test-bookings").
    withServiceAccount("jenkins").
    withContainer(kube.steps.containerTemplate(
        name: 'schema',
        image: "docker.infra.tstllc.net/job/job-report-test-bookings:latest",
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
                   echo "Running test bookings report job"
                   cd /app
                   java -jar app-standalone.jar
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
    slackSend channel: "#agent", color: "good", message: "Job: 'job-report-test-bookings' SUCCESSFUL"
}

def notifyFailed() {
    slackSend channel: "#agent", color: "#FF0000", message: "Job: 'job-report-test-bookings' FAILED"
}
