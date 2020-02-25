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
# Google Cloud Container Registry. See section "gke_container" of .cirrus.yml
#------------------------------------------------------------------------------

FROM gcr.io/ci-cd-215716/base:latest

USER root

RUN cd /opt/ && \
    wget https://downloads.apache.org//ant/binaries/apache-ant-1.10.7-bin.tar.gz && \
    tar -zxvf apache-ant-1.10.7-bin.tar.gz && \
    mv apache-ant-1.10.7 ant && \
    ANT_HOME=/opt/ant && \
    PATH=$PATH:${ANT_HOME}/bin && \
    cd ant && \
    ant -f fetch.xml -Ddest=system

USER sonarsource

ENV ANT_HOME="/opt/ant"
ENV PATH="${PATH}:${ANT_HOME}/bin"
