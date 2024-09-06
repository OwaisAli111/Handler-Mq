package Main.Java;

import com.mongodb.client.FindIterable;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.json.JSONObject;

import javax.jms.*;

import org.bson.Document;


public class MessageRetrievalHandler implements HttpHandler{
    @Override
    public void handle(HttpExchange exchange) throws IOException{
        if("POST".equals(exchange.getRequestMethod())){
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            JSONObject jsonRequest = new JSONObject(requestBody);
            String queueName = jsonRequest.optString("queuename");
            String timestamp = jsonRequest.optString("timestamp_Dequeue");

            if (queueName == null || timestamp == null) {
                String response = "Missing queueName or timestamp parameters";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String result = restoreMessages(queueName, timestamp);
            exchange.sendResponseHeaders(200, result.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(result.getBytes());
            }
        
    } else {
        String response = "Only POST method is supported";
        exchange.sendResponseHeaders(405, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes());
    }
}
}
    private String restoreMessages(String queueName, String timestamp){
        //String brokerUrl = "tcp://192.168.192.1:61616";
        String brokerUrl = "tcp://activemq.activemq-test.svc.cluster.local:61616";

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null; 
        String result;

        try{
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue queue = session.createQueue(queueName);
            producer = session.createProducer(queue);

            Document filter = new Document("queuename", queueName)
                                .append("timestamp_Dequeue", timestamp);
            FindIterable<Document> documents = DatabaseManager.getCollection().find(filter);

            for (Document doc : documents) {
                Object messagesObj = doc.get("message");
                System.out.println("Document: " + doc.toJson()); // Print entire document
                if (messagesObj instanceof List<?>) {
                    List<?> messagesList = (List<?>) messagesObj;
                    for (Object messageObj : messagesList) {
                        if (messageObj instanceof String) {
                            String messageContent = (String) messageObj;
                            TextMessage message = session.createTextMessage(messageContent);
                            producer.send(message);
                        } else {
                            result = "Error: Message content is not a String.";
                            return result;
                        }
                    }
                } else {
                    // Handle the case where the 'message' field is not a List
                    result = "Error: Messages are not in a List format.";
                    return result;
                }
            }
    
            result = "Messages restore to queue" + queueName + ".";
        }
        catch (JMSException e) {
            e.printStackTrace();
            result = "Error occurred: " + e.getMessage();
        } finally {
            try {
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
