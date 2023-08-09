package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Node;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Neo4jMovies {

    private Driver driver;
    private String uriDb;

    public Neo4jMovies() {
        uriDb = "bolt://localhost:7687"; // may need to change if you used a different port for your DBMS
        Config config = Config.builder().withoutEncryption().build();
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","12345678"), config);
    }

    //Check if the actor already exists or not and return a boolean
    public boolean actorExists (String actorId){
        try (Session session = driver.session()) {
            //Check if the actorId already exists
            try (Transaction tx = session.beginTransaction()) {
                StatementResult result = tx.run("MATCH (a:Actor {actorId: $actorId}) RETURN a",
                        parameters("actorId", actorId));
                if (result.hasNext()) {
                    return true;
                }
                else{
                    return false;
                }
            }
        }
    }

    public void addActor(HttpExchange request) throws IOException, JSONException {
        try{
            // Extract and parse JSON data from request body
            String requestBody = Utils.getBody(request);
            JSONObject jsonObject = new JSONObject(requestBody);

            String actorId = jsonObject.optString("actorId", null);
            String name = jsonObject.optString("name", null);
            boolean hasOscar = jsonObject.optBoolean("hasOscar");

            //if improper formatting
            if (actorId == null || name == null){
                String response = "Improper formatting. Actor was not added.";
                Utils.sendString(request, response, 400);
            }

            else {
                if (actorExists(actorId)){
                    String response = "The actor already exists!";
                    Utils.sendString(request, response, 400);
                    return;
                }

                try (Session session = driver.session()) {
                    //Actor with the given actorId doesn't exist
                    try (Transaction tx = session.beginTransaction()) {
                        tx.run("CREATE (a:Actor {actorId: $actorId, name: $name, hasOscar: $hasOscar, movies: []})",
                                parameters("actorId", actorId, "name", name, "hasOscar", hasOscar));
                        tx.success(); // Commit the transaction
                    }

                    String response = "Successfully added actor with id: " + actorId + " and name: " + name;
                    Utils.sendString(request, response, 200);
                }
            }
        }

        //If there is some exception thrown, send 500 response
        catch(Exception e){
            String response = "Internal Server Error: " + e.getMessage();
            Utils.sendString(request, response, 500);
        }

    }

    //Check if the movie already exists or not and return a boolean
    public boolean movieExists (String movieId){
        try (Session session = driver.session()) {
            //Check if the movieId already exists
            try (Transaction tx = session.beginTransaction()) {
                StatementResult result = tx.run("MATCH (m:Movie {movieId: $movieId}) RETURN m",
                        parameters("movieId", movieId));
                if (result.hasNext()) {
                    return true;
                }
                else{
                    return false;
                }
            }
        }
    }

    public void addMovie(HttpExchange request) throws IOException, JSONException {
        try{
            // Extract and parse JSON data from request body
            String requestBody = Utils.getBody(request);
            JSONObject jsonObject = new JSONObject(requestBody);

            String movieId = jsonObject.optString("movieId", null);
            String name = jsonObject.optString("name", null);

            //if improper formatting
            if (movieId == null || name == null){
                String response = "Improper formatting. Movie was not added.";
                Utils.sendString(request, response, 400);
            }

            else {
                if (movieExists(movieId)){
                    String response = "The movie already exists!";
                    Utils.sendString(request, response, 400);
                    return;
                }

                try (Session session = driver.session()) {
                    //Movie with the given movieId doesn't exist
                    try (Transaction tx = session.beginTransaction()) {
                        tx.run("CREATE (m:Movie {movieId: $movieId, name: $name, actors: [] })",
                                parameters("movieId", movieId, "name", name));
                        tx.success(); // Commit the transaction
                    }

                    String response = "Successfully added movie with id: " + movieId + " and name: " + name;
                    Utils.sendString(request, response, 200);
                }
            }
        }

        //If there is some exception thrown, send 500 response
        catch(Exception e){
            String response = "Internal Server Error: " + e.getMessage();
            Utils.sendString(request, response, 500);
        }
    }

    public void addRelationship(HttpExchange request) throws IOException, JSONException {
        try{
            // Extract and parse JSON data from request body
            String requestBody = Utils.getBody(request);
            JSONObject jsonObject = new JSONObject(requestBody);

            String actorId = jsonObject.optString("actorId", null);
            String movieId = jsonObject.optString("movieId", null);

            //if improper formatting
            if (actorId == null || movieId == null){
                String response = "Improper formatting. Relationship was not added.";
                Utils.sendString(request, response, 400);
            }

            else {
                // Check if both the actor and movie exist
                if (!actorExists(actorId)){
                    String response = "The actor with ID " + actorId + " does not exist!";
                    Utils.sendString(request, response, 404);
                    return;
                }

                if (!movieExists(movieId)){
                    String response = "The movie with ID " + movieId + " does not exist!";
                    Utils.sendString(request, response, 404);
                    return;
                }

                try (Session session = driver.session()) {
                    try (Transaction tx = session.beginTransaction()) {
                        //check if relationship exists
                        StatementResult result = tx.run(
                                "MATCH (a:Actor {actorId: $actorId})-[r:ACTED_IN]->(m:Movie {movieId: $movieId}) RETURN r",
                                parameters("actorId", actorId, "movieId", movieId)
                        );

                        if (result.hasNext()) {
                            String response = "The ACTED_IN relationship already exists between actor with ID " + actorId +
                                    " and movie with ID " + movieId;
                            Utils.sendString(request, response, 400);
                            return;
                        }
                    }

                    //when both actor and movie ids exist, do this
                    try (Transaction tx = session.beginTransaction()) {
                        //Create relationship
                        tx.run("MATCH (a:Actor {actorId: $actorId}), (m:Movie {movieId: $movieId})\n" +
                                        "CREATE (a)-[:ACTED_IN]->(m)",
                                parameters("actorId", actorId, "movieId", movieId));

                        // Update the actors and movies list properties
                        tx.run("MATCH (m:Movie {movieId: $movieId})\n" +
                                        "SET m.actors = m.actors + $actorId",
                                parameters("actorId", actorId, "movieId", movieId));

                        tx.run("MATCH (a:Actor {actorId: $actorId}) " +
                                        "SET a.movies = a.movies + $movieId",
                                parameters("actorId", actorId, "movieId", movieId));

                        tx.success();
                    }

                    String response = "Successfully created ACTED_IN relationship between actor with ID " + actorId
                            + " and movie with ID " + movieId;
                    Utils.sendString(request, response, 200);
                }
            }
        }

        //If there is some exception thrown, send 500 response
        catch(Exception e){
            String response = "Internal Server Error: " + e.getMessage();
            Utils.sendString(request, response, 500);
        }
    }

    public void getActor(HttpExchange request){

    }

    public void getMovie(HttpExchange request){

    }

    public void hasRelationship(HttpExchange request){

    }

    public void getOscarActor(HttpExchange request) throws IOException, JSONException {

        try (Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                StatementResult result = tx.run("MATCH (a:Actor {hasOscar: true}) RETURN a");
                JSONArray jsonArray = new JSONArray(); // Array to hold actor objects

                while (result.hasNext()) {
                    Record record = result.next();
                    Node actorNode = record.get("a").asNode();
                    JSONObject actorJson = new JSONObject();

                    // Extract attributes from the actor node and add them to the JSON object
                    actorJson.put("name", actorNode.get("name").asString());
                    actorJson.put("hasOscar", actorNode.get("hasOscar").asBoolean());
                    // Add more attributes as needed

                    jsonArray.put(actorJson); // Add the JSON object to the array
                }

                JSONObject responseJson = new JSONObject();
                responseJson.put("actors", jsonArray); // Add the array to the response JSON

                String response = responseJson.toString(4);
                System.out.println(response);
                Utils.sendString(request, response, 200);
            }
        }
    }

    //We can compute the path in this helper and then use the list to determine
    //the bacon number as well
    public List<String> computeBaconHelper(HttpExchange request) throws IOException, JSONException {

        // Extract and parse JSON data from request body
        String requestBody = Utils.getBody(request);
        JSONObject jsonObject = new JSONObject(requestBody);

        String actorId = jsonObject.optString("actorId", null);
        String movieId = jsonObject.optString("movieId", null);

        // Check if the actorId exists
        if (!actorExists(actorId)) {
            String response = "The actor with ID " + actorId + " does not exist!";
            Utils.sendString(request, response, 404);
            return null;
        }
        return null;
    }

    public void computeBaconNumber(HttpExchange request){

    }

    public void computeBaconPath(HttpExchange request){
        // comment test
    }

    public void close() {
        driver.close();
    }
}

