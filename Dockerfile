FROM clojure:alpine

WORKDIR /app
ADD . /app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar
