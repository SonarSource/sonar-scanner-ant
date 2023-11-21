#------------------------------------------------------------------------------
# Installs ANT, which is needed to run the ITs during the QA task.
#
# Build from the basedir:
#   docker build -f its/docker/Dockerfile -t sonar-scanner-ant-qa its/docker
#
# Verify the content of the image by running a shell session in it:
#   docker run -it sonar-scanner-ant-qa bash
#
# CirrusCI builds the image when needed. No need to manually upload it to
# AWS ECR. See section "eks_container" of .cirrus.yml
#------------------------------------------------------------------------------

ARG CIRRUS_AWS_ACCOUNT=275878209202
FROM ${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j17-m3-latest

ARG ANT_VERSION=1.10.13

USER root

RUN cd /opt/ && \
    curl -O -L https://downloads.apache.org/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -zxvf apache-ant-${ANT_VERSION}-bin.tar.gz && \
    mv apache-ant-${ANT_VERSION} ant && \
    ANT_HOME=/opt/ant && \
    PATH=$PATH:${ANT_HOME}/bin

USER sonarsource

ENV ANT_HOME="/opt/ant"
ENV PATH="${PATH}:${ANT_HOME}/bin"
