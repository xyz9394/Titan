package db.mongodb;

//This line needs manual import.
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.and;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.ExternalAPI;
import external.ExternalAPIFactory;

public class MongoDBConnection implements DBConnection {
	private MongoClient mongoClient;
	private MongoDatabase db;

	public MongoDBConnection() {
		// Connects to local mongodb server.
		mongoClient = new MongoClient();
		db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
	}

	@Override
	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$push", new Document("favorite", 
						new Document("$each", itemIds))));
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$pullAll", new Document("favorite", itemIds)));
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		Set<String> favoriteItems = new HashSet<String>();
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		if (iterable.first().containsKey("favorite")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) iterable.first().get("favorite");
			favoriteItems.addAll(list);
		}
		return favoriteItems;

	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		Set<String> itemIds = getFavoriteItemIds(userId);
		Set<Item> favoriteItems = new HashSet<>();
		for (String itemId : itemIds) {
			FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
			Document doc = iterable.first();
			ItemBuilder builder = new ItemBuilder();

			// Because itemId is unique and given one item id there should
			// have
			// only one result returned.
			builder.setItemId(doc.getString("item_id"));
			builder.setName(doc.getString("name"));
			builder.setCity(doc.getString("city"));
			builder.setState(doc.getString("state"));
			builder.setCountry(doc.getString("country"));
			builder.setZipcode(doc.getString("zipcode"));
			builder.setRating(doc.getDouble("rating"));
			builder.setAddress(doc.getString("address"));
			builder.setLatitude(doc.getDouble("latitude"));
			builder.setLongitude(doc.getDouble("longitude"));
			builder.setDescription(doc.getString("description"));
			builder.setSnippet(doc.getString("snippet"));
			builder.setSnippetUrl(doc.getString("snippet_url"));
			builder.setImageUrl(doc.getString("image_url"));
			builder.setUrl(doc.getString("url"));
			builder.setCategories(getCategories(itemId));
			favoriteItems.add(builder.build());
		}
		return favoriteItems;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		Set<String> categories = new HashSet<String>();
		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", itemId));
		if (iterable.first().containsKey("categories")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) iterable.first().get("categories");
			categories.addAll(list);
		}
		return categories;
	}

	@Override
	public List<Item> searchItems(String userId, double lat, double lon, String term) {
		// Connect to external API
		ExternalAPI api = ExternalAPIFactory.getExternalAPI(); // moved here
		List<Item> items = api.search(lat, lon, term);
		for (Item item : items) {
			// Save the item into our own db.
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		// You can construct the query like
		// db.getCollection("items").find(new Document().append("item_id", item.getItemId()))
		// But the java drive provides you a clearer way to do this.

		FindIterable<Document> iterable = db.getCollection("items").find(eq("item_id", item.getItemId()));
		if (iterable.first() == null) {
			db.getCollection("items")
					.insertOne(new Document().append("item_id", item.getItemId()).append("name", item.getName())
							.append("city", item.getCity()).append("state", item.getState())
							.append("country", item.getCountry()).append("zip_code", item.getZipcode())
							.append("rating", item.getRating()).append("address", item.getAddress())
							.append("latitude", item.getLatitude()).append("longitude", item.getLongitude())
							.append("description", item.getDescription()).append("snippet", item.getSnippet())
							.append("snippet_url", item.getSnippetUrl()).append("image_url", item.getImageUrl())
							.append("url", item.getUrl()).append("categories", item.getCategories()));
		}
	}

	@Override
	public String getFullname(String userId) {
		String name = new String();
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		Document user = iterable.first();
		if (user.containsKey("first_name") && user.containsKey("last_name")) {
			name = user.getString("first_name") + " " + user.getString("last_name");
		}
		return name;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		FindIterable<Document> iterable = db.getCollection("users").find(and(eq("user_id", userId), eq("password", password)));
		if (iterable.first() != null) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean register(String userId, String pwd, String first, String last) {
		if (userId == null || userId.length() == 0) {
			return false;
		}
		FindIterable<Document> iterable = db.getCollection("users").find(eq("user_id", userId));
		if(iterable.first() != null) {
			return false;
		} 
		db.getCollection("users")
	        .insertOne(new Document().append("first_name", first).append("last_name", last)
	            .append("password", pwd).append("user_id", userId));
		return true;
	}
}

