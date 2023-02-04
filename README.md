# amqp-receiver

`amqp-receiver` can be used to test an AMQP connection and will print all the properties and the body. It is possible to
configure the response using environment variables.

`amqp-receiver` is available as a Docker image: `ninckblokje/amqp-receiver`

## Configuration

It is possible to configure `amqp-receiver` using environment variables specified in the table below.

| Key                        | Default value | Description                                                |
|----------------------------|---------------|------------------------------------------------------------|
| AMQP_RECEIVER_HOST         | localhost     | AMQP server hostname                                       |
| AMQP_RECEIVER_PORT         | 5672          | AMQP server port                                           |
| AMQP_RECEIVER_USERNAME     |               | AMQP server username                                       |
| AMQP_RECEIVER_PASSWORD     |               | AMQP server password                                       |
| AMQP_RECEIVER_ADDRESS      |               | AMQP address                                               |
| AMQP_RECEIVER_ADDRESS_TYPE |               | AMQP address type (`queue` or `topic`), useful for Artemis |
