FROM ubuntu:18.04

RUN apt-get -y -q update && \
        apt-get install -qqy \
        openjdk-8-jdk \
        maven \
        git \
        gnuplot \
        wget \
        less vim # not required by chaos itself, just for ease of use

# You need to locate openchaos in this directory (up.sh does that automatically)
ADD chaos openchaos

ADD ./bashrc /root/.bashrc
ADD ./init.sh /init.sh
RUN chmod +x /init.sh

CMD /init.sh
