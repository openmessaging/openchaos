# OpenMessaging-Chaos

The OpenMessaging Chaos Framework is used to check the high availability and consistency of messaging platforms with fault injection. Currently supported message platforms：

- [Apache RocketMQ](https://rocketmq.apache.org/)

## Usage

Take rocketmq for example:

1. Prepare one control node and  some cluster nodes and ensure that the control node can use SSH to log into a bunch of db nodes
2. Edit driver-rocketmq/rocketmq.yaml to set the host name of cluster nodes, client config, broker config.
3. Intstall openmessaging-chaos in control node:  `mvn clean install`
4. Run the test in the control node: `bin/chaos.sh --drivers driver-rocketmq/rocketmq.yaml --install` 

## Quick Start（Docker）

In one shell, we start the five nodes and the controller using docker compose.

```shell
cd docker
./up.sh --dev
```
In another shell, use `docker exec -it chaos-control bash` to enter the controller, then

```shell
mvn clean install
bin/chaos.sh --drivers driver-rocketmq/rocketmq.yaml --install
```

## Option

```
  Options:
    -c, --concurrency
       The number of clients. eg: 5
       Default: 4
  * -d, --drivers
       Drivers list. eg.: driver-rocketmq/rocketmq.yaml
    -h, --help
       Help message
       Default: false
    --install
       Whether to install program. It will download the installation package on
       each cluster node. When you first use openmessaging-chaos to test a
       distributed system, it should be true.
       Default: false
    -t, --limit-time
       Chaos execution time in seconds (excluding check time and recovery time).
       eg: 60
       Default: 60
    -f, --fault
       Fault type to be injected. eg: noop, minor-kill, major-kill, random-kill,
       random-partition, random-delay, random-loss
       Default: noop
    -i, --fault-interval
       Fault execution interval. eg: 30
       Default: 30
    -r, --rate
       Approximate number of requests per second. eg: 20
       Default: 20
    -u, --username
       User name for ssh remote login. eg: admin
       Default: root
```
## Scaffold

It is easy to test your own messaging platform, just implement the interface in driver-api.

