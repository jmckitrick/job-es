#!/bin/sh

ENV=$1
YEARS="2010 2011 2012 2013 2014 2015 2016 2017 2018 2019"

echo "Targeting Elastic Search data for $ENV"

echo "Deleting data...."
curl -i -XDELETE http\://localhost\:8000/booking-$ENV
echo
echo "Done."

DB_HOST=127.0.0.1
DB_USER=root
DB_PASS=""
DB_PORT=3306

for YEAR in $YEARS; do
  echo "Importing data from $YEAR"
  lein run $ENV $YEAR 1 12
done
