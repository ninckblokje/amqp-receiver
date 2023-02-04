/*
  Copyright (c) 2023, ninckblokje
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

  * Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package ninckblokje.vertx.amqpreceiver;

import io.vertx.amqp.*;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  private final static String amqpReceiverHost = "AMQP_RECEIVER_HOST";
  private final static String amqpReceiverPort = "AMQP_RECEIVER_PORT";
  private final static String amqpReceiverUsername = "AMQP_RECEIVER_USERNAME";
  private final static String amqpReceiverPassword = "AMQP_RECEIVER_PASSWORD";

  private final static String amqpReceiverAddress = "AMQP_RECEIVER_ADDRESS";
  private final static String amqpReceiverAddressType = "AMQP_RECEIVER_ADDRESS_TYPE";

  public static void main(String[] args) {
    var vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class.getName());
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    var envCfgOptions = new ConfigStoreOptions()
      .setType("env")
      .setConfig(new JsonObject()
        .put("keys", new JsonArray()
          .add(amqpReceiverHost)
          .add(amqpReceiverPort)
          .add(amqpReceiverUsername)
          .add(amqpReceiverPassword)
          .add(amqpReceiverAddress)
          .add(amqpReceiverAddressType)
        ));
    var cfgRetrieverOptions = new ConfigRetrieverOptions()
      .addStore(envCfgOptions);

    var cfgRetriever = ConfigRetriever.create(vertx, cfgRetrieverOptions);
    cfgRetriever.getConfig()
      .onSuccess(entries -> {
        var address = entries.getString(amqpReceiverAddress);
        var addressType = entries.getString(amqpReceiverAddressType);

        var amqpClientOptions = createAmqpClientOptions(entries);
        startAmqpClient(startPromise, address, addressType, amqpClientOptions);
      });
  }

  private AmqpClientOptions createAmqpClientOptions(JsonObject entries) {
    return new AmqpClientOptions()
      .setHost(entries.getString(amqpReceiverHost, "localhost"))
      .setPort(entries.getInteger(amqpReceiverPort, 5672))
      .setUsername(entries.getString(amqpReceiverUsername))
      .setPassword(entries.getString(amqpReceiverPassword));
  }

  private void startAmqpClient(Promise<Void> startPromise, String address, String addressType, AmqpClientOptions amqpClientOptions) {
    var amqpClient = AmqpClient.create(vertx, amqpClientOptions);

    amqpClient.connect(ar -> {
      if (ar.succeeded()) {
        logger.atInfo()
          .setMessage("Created AMQP connection to {}:{} as {}")
          .addArgument(amqpClientOptions.getHost())
          .addArgument(amqpClientOptions.getPort())
          .addArgument(amqpClientOptions.getUsername())
          .log();
        var conn = ar.result();
        createReceiver(startPromise, address, addressType, conn);
      } else {
        startPromise.fail(ar.cause());
      }
    });
  }

  private void createReceiver(Promise<Void> startPromise, String address, String addressType, AmqpConnection conn) {
    var amqpReceiverOptions = new AmqpReceiverOptions();
    if ("queue".equals(addressType) || "topic".equals(addressType)) {
      amqpReceiverOptions.addCapability(addressType);
    }

    conn.createReceiver(address, amqpReceiverOptions, done -> {
      if (done.succeeded()) {
        var receiver = done.result();
        receiver.handler(this::handleMessage);
        logger.atInfo()
          .setMessage("Receiving messages from address {}")
          .addArgument(address)
          .log();
        startPromise.complete();
      } else {
        startPromise.fail(done.cause());
      }
    });
  }

  private void handleMessage(AmqpMessage msg) {
    logger.atInfo()
      .setMessage("Received msg {}")
      .addArgument(msg.id())
      .log();

    if (msg.unwrap().getProperties() != null) {
      logger.atInfo()
        .log("- With properties:");
      logger.atInfo()
        .setMessage("{}")
        .addArgument(msg.unwrap().getProperties())
        .log();
    }

    logger.atInfo()
      .log("- With body:");
    logger.atInfo()
      .setMessage("{}")
      .addArgument(msg.bodyAsString())
      .log();
  }
}
