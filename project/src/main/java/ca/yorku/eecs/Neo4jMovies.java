package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;

import java.io.IOException;
import java.util.List;

public class Neo4jMovies {

    private Driver driver;
    private String uriDb;

    public Neo4jMovies() {
        uriDb = "bolt://localhost:7687"; // may need to change if you used a different port for your DBMS
        Config config = Config.builder().withoutEncryption().build();
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","12345678"), config);
    }

    public void addActor(HttpExchange request) throws IOException, JSONException {

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
            try (Session session = driver.session()) {
                //Check if the actorId already exists
                try (Transaction tx = session.beginTransaction()) {
                    StatementResult result = tx.run("MATCH (a:Actor {actorId: $actorId}) RETURN a",
                            parameters("actorId", actorId));
                    if (result.hasNext()) {
                        String response = "The actor already exists!";
                        Utils.sendString(request, response, 400);
                        tx.failure(); // Rollback the transaction since the actor already exists
                        return;
                    }
                }

                //Actor with the given actorId doesn't exist
                try (Transaction tx = session.beginTransaction()) {
                    tx.run("CREATE (a:Actor {actorId: $actorId, name: $name, hasOscar: $hasOscar})",
                            parameters("actorId", actorId, "name", name, "hasOscar", hasOscar));
                    tx.success(); // Commit the transaction
                }

                String response = "Successfully added actor with id: " + actorId + " and name: " + name;
                Utils.sendString(request, response, 200);
            }
        }

    }

    public void addMovie(HttpExchange request) throws IOException, JSONException {
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
            try (Session session = driver.session()) {
                //Check if the movieId already exists
                try (Transaction tx = session.beginTransaction()) {
                    StatementResult result = tx.run("MATCH (m:Movie {movieId: $movieId}) RETURN m",
                            parameters("movieId", movieId));
                    if (result.hasNext()) {
                        String response = "The movie already exists!";
                        Utils.sendString(request, response, 400);
                        tx.failure(); // Rollback the transaction since the movie already exists
                        return;
                    }
                }

                //Movie with the given movieId doesn't exist
                try (Transaction tx = session.beginTransaction()) {
                    tx.run("CREATE (m:Movie {movieId: $movieId, name: $name })",
                            parameters("movieId", movieId, "name", name));
                    tx.success(); // Commit the transaction
                }

                String response = "Successfully added movie with id: " + movieId + " and name: " + name;
                Utils.sendString(request, response, 200);
            }
        }
    }


    public void addRelationship(HttpExchange request) {

    }

    public void getActor(HttpExchange request){

    }

    public void getMovie(HttpExchange request){

    }

    public void hasRelationship(HttpExchange request){

    }

    //We can compute the path in this helper and then use the list to determine
    //the bacon number as well
    public List<String> computeBaconHelper(HttpExchange request){
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

