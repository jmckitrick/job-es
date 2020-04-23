# IMPORT BOOKINGS TO ELASTIC SEARCH

Import selected fields from bookings so ElasticSearch has them for the quicksearch bar.
Existing data for specified environment can be deleted from ElasticSearch.


## DESIGN

- Call `import-es` which is a wrapper script to manage the import.
  - Check for all required settings.
  - Environment variables:
    - `DB_HOST`
    - `DB_PORT`
    - `DB_USER`
    - `DB_PASS`
    - `ES_HOST`
  - Use settings to configure the behavior.
  - Environment variables:
    - `ES_PORT` - defaults to 3220
    - `START_YEAR` - Start year for import
    - `END_YEAR` - End year for import
  - Command line arguments:
    - `NAMESPACE` - argument 1
      - Example values: `cdev` `cstaging` `prod`
    - `DELETE_DATA` - argument 2
      - If non-null, will delete all data before importing.
  - Iterate the months in the provided year range.
  - For each month, call the app in the jarfile.

- Jarfile contains main import logic.
  - Query `TRAVEL_BOOKING` in month-sized chunks.
  - Add/modify any of the booking data as needed for quicksearch.
  - Use the bulk insert API of ElasticSearch.
  - Check the results and log errors.


## PRODUCTION

- run in a Jenkins job


## REQUIREMENTS

- Permissions for kubernetes `lower` and `kubectl` to access it,
if using port forwarding.
- VPN connection and permissions for database access.


## INSTALLATION

Install [leiningen][1] per the instructions (4 steps).


## RUNNING THE IMPORT

1. Use kubectl to find the elastic search service:
   `kc get po -n search`
1. Copy the name of one of the pods starting with `elasticsearch-client`.
1. In a separate terminal, create a tunnel from local port 8000
to the elastic search pod:
   `while true; do kc port-forward elasticsearch-client-XXXXXXXXX-ZZZZZ 8000:9200 --namespace=search; done`
1. Open `import-es` in this project.
1. Navigate to the connection specs around line 13.
1. Set connection specs for your target database.
1. Run `./import-es $ENV` where ENV is one of: [dev, staging, cdev, cstaging, client]
1. Hit ctrl-C twice (quickly) in the terminal with the tunnel to ES when you are done.


[1]: http://leiningen.org/
