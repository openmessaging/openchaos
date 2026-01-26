# Based on the deprecated `https://github.com/tutumcloud/tutum-debian`
FROM debian:sid

# Set noninteractive to avoid tz/locale prompts
ENV DEBIAN_FRONTEND=noninteractive

# Install packages
RUN apt-get update && \
    apt-get -y install \
        dos2unix \
        openssh-server \
        openjdk-8-jdk \
        pwgen \
        locales \
        && \
        mkdir -p /var/run/sshd && \
        sed -i "s/UsePrivilegeSeparation.*/UsePrivilegeSeparation no/g" /etc/ssh/sshd_config && \
        sed -i "s/PermitRootLogin without-password/PermitRootLogin yes/g" /etc/ssh/sshd_config && \
        # Configure UTF-8 locale
        sed -i 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
        locale-gen en_US.UTF-8 && \
        update-locale LANG=en_US.UTF-8

# Make sure locale is exported in all shells
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

ENV AUTHORIZED_KEYS **None**

ADD run.sh /run.sh
RUN dos2unix /run.sh \
    && chmod +x /*.sh

RUN apt-get update
RUN apt install -y apt-transport-https
RUN apt install -y software-properties-common
RUN apt-get update && apt-get install -y maven

RUN apt-get install -y \
        sudo net-tools wget \
        curl vim man faketime unzip less \
        iptables iputils-ping logrotate
    # apt-get remove -y --purge --auto-remove systemd

EXPOSE 22
CMD ["/run.sh"]
