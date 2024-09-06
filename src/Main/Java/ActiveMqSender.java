
package Main.Java;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import java.io.IOException;
import java.io.OutputStream;
import org.bson.Document;
import java.util.List;
import java.util.ArrayList;

public class ActiveMqSender implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if ("POST".equals(exchange.getRequestMethod())) {
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            JSONObject jsonRequest = new JSONObject(requestBody);
            String queueName = jsonRequest.optString("queueName");
            String action = jsonRequest.optString("action");

            if (queueName == null || action == null) {
                String response = "Missing queueName or action parameters";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String activeMQ_URL = System.getenv("ACTIVEMQ_QUEUE_URL");

            if (activeMQ_URL == null) {
                String response = "ACTIVEMQ_QUEUE_URL environment variable not set";
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String response = processQueue(activeMQ_URL, queueName, action);
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } else {
            String response = "Only POST method is supported";
            exchange.sendResponseHeaders(405, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private String processQueue(String activemqURL, String queueName, String action) {
        //String brokerUrl = "tcp://192.168.192.1:61616";
        String brokerUrl = "tcp://activemq.activemq-test.svc.cluster.local:61616";

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;
        MessageProducer producer = null;
        String result;

        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue queue = session.createQueue(queueName);

            if ("PURGE".equalsIgnoreCase(action)) {
                            
                consumer = session.createConsumer(queue);
                Message message;
                List<String> messages = new ArrayList<>();
                                                
                while ((message = consumer.receive(1000)) != null) {
                                                
                String messageContent = ((TextMessage) message).getText();
                messages.add(messageContent);
                                                
                }
                if(!messages.isEmpty()){
                long timestamp = System.currentTimeMillis();
                String formattedDateTime = TimeUtils.formatCurrentTime(timestamp);
                            
                Document doc = new Document("queuename", queueName)
                                    .append("message", messages)
                                    .append("timestamp_Dequeue", formattedDateTime);
                                    DatabaseManager.getCollection().insertOne(doc);
                }
                result = "Queue " + queueName + " purged successfully.";

            } else if ("SEND".equalsIgnoreCase(action)) {
                producer = session.createProducer(queue);
                TextMessage message = session.createTextMessage("This is a third test message ");
                producer.send(message);
                result = "Message sent to queue " + queueName + ".";
            } else {
                result = "Unsupported action: " + action;
            }
        } catch (JMSException e) {
            e.printStackTrace();
            result = "Error occurred: " + e.getMessage();
        } finally {
            try {
                if (consumer != null) consumer.close();
                if (producer != null) producer.close();
                if (session != null) session.close();
                if (connection != null) connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
