This is a [RESP](https://redis.io/docs/latest/develop/reference/protocol-spec/) compliant redis server only support [RESP3](https://github.com/redis/redis-specifications/blob/master/protocol/RESP3.md).<br>
## Getting Started
```bash
cd /redis-server

./gradlew build

./gradlew run
```

1. Open command line interface
2. execute under command
```bash
nc localhost 8888
```
Then cli can communicate with the redis-server.
## interaction between the client and the server
A client sends the Redis server an **array consisting of only bulk strings**.<br>
A Redis server replies to clients, sending any valid RESP data type as a reply.<br>

So, for example, a typical interaction could be the following.

The client sends the command **SET key value NX**. Then the server replies with simple reply(**OK**) as in the following example (C: is the client, S: the server).

C: *4\r\n<br>
C: $3\r\n<br>
C: SET\r\n<br>
C: $3\r\n<br>
C: key\r\n<br>
C: $5\r\n<br>
C: value\r\n<br>
C: $2\r\n<br>
C: NX

S: :+OK\r\n<br>

As usual, we separate different parts of the protocol with newlines(\r\n) for simplicity, but the actual interaction is the client sending *4$3SET$3key$5value$2NX as a whole.

## Supported Commands
- [PING](https://redis.io/docs/latest/commands/ping/)
- [ECHO](https://redis.io/docs/latest/commands/echo/)
- [GET](https://redis.io/docs/latest/commands/get/)
- [SET](https://redis.io/docs/latest/commands/set/)