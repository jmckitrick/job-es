# IMPORT BOOKINGS TO ELASTIC SEARCH

Import selected fields from bookings so Elastic Search has them for the Quicksearch bar.
Existing data for specified environment is deleted from Elastic Search.


## PRODUCTION

- run in a Jenkins job


## REQUIREMENTS

- Permissions for kubernetes `lower` and `kubectl` to access it.
- VPN connection and permissions for database access.


## INSTALLING

Install [leiningen][1] per the instructions (4 steps).


## RUNNING

1. Use kubectl to find the elastic search service:
   `kc get po -n search`
1. Copy the name of one of the pods starting with `elasticsearch-client`.
1. In a separate terminal, create a tunnel from local port 8000 to the elastic search pod:
   `while true; do kc port-forward elasticsearch-client-XXXXXXXXX-ZZZZZ 8000:9200 --namespace=search; done`
1. Open `import-es` in this project.
1. Navigate to the connection specs around line 13.
1. Set connection specs for your target database.
1. Run `./import-es $ENV` where ENV is one of: [dev, staging, cdev, cstaging, client]
1. Hit ctrl-C twice (quickly) in the terminal with the tunnel to ES when you are done.


[1]: http://leiningen.org/
