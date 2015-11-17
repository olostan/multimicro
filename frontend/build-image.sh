#!/bin/sh
cp ../dispatcher/src/main/proto/dispatcher.proto dispatcher.proto
docker build  -t multimicro-frontend .
