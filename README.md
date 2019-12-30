# IMPORT BOOKINGS TO ELASTIC SEARCH JOB

Import selected fields from bookings so Elastic Search has them for the Quicksearch bar.
Existing data for specified environment is deleted from Elastic Search.

## REQUIREMENTS

- Permissions for kubernetes `lower` and `kubectl` to access it.
- VPN connection and permissions for database access.


## RUNNING

1. Go to http://leiningen.org/ and follow the instructions to install Leiningen (4 steps).
2. Use kubectl to find the elastic search service:
   `kc get po -n search`
3. Copy the name of one of the pods starting with `elasticsearch-client`.
4. In a separate terminal, create a tunnel from local port 8000 to the elastic search pod:
   `while true; do kc port-forward elasticsearch-client-XXXXXXXXX-ZZZZZ 8000:9200 --namespace=search; done`
5. Open `import-es` in this project.
6. Navigate to the connection specs around line 13.
7. Set connection specs for your target database.
8. Run `./update-es $ENV` where ENV is one of: [dev, staging, cdev, cstaging, client]
9. Hit ctrl-C twice (quickly) in the terminal with the tunnel to ES when you are done.
