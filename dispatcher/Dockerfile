FROM java:8
COPY build/install/dispatcher /usr/src/dispatcher
WORKDIR /usr/src/dispatcher
ENTRYPOINT ["bin/dispatcher-server", ""]
EXPOSE 50051