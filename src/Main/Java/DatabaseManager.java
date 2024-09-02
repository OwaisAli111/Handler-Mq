package Main.Java;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class DatabaseManager {
    private static MongoCollection<org.bson.Document> collection;

    public static void initialize() {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("database_name");
        collection = database.getCollection("your_collection_name");
    }

    public static MongoCollection<org.bson.Document> getCollection() {
        return collection;
    }
}
