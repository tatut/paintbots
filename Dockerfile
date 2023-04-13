FROM clojure:tools-deps
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY deps.edn /usr/src/app/
COPY src /usr/src/app/src
COPY resources /usr/src/app/resources
COPY config.edn /usr/src/app
RUN clojure -X:deps prep
CMD ["clojure", "-m", "paintbots.main"]
