FROM golang:1.5

RUN mkdir -p /go/src/renderer
WORKDIR /go/src/renderer

COPY . /go/src/renderer
RUN go get .
RUN go build .
ENTRYPOINT ["renderer"]
EXPOSE 50052
