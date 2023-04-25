FROM clojure:temurin-17-tools-deps-focal
RUN apt-get update
RUN apt-get install -y ffmpeg
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY deps.edn /usr/src/app/
COPY src /usr/src/app/src
COPY resources /usr/src/app/resources
COPY config.edn /usr/src/app
RUN clojure -X:deps prep
CMD ["clojure", "-M:run"]
