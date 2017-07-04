/*
 * Copyright (c) 2017 Hendrik Depauw
 * Copyright (c) 2017 Lorenz van Herwaarden
 * Copyright (c) 2017 Nick Aelterman
 * Copyright (c) 2017 Olivier Cammaert
 * Copyright (c) 2017 Maxim Deweirdt
 * Copyright (c) 2017 Gerwin Dox
 * Copyright (c) 2017 Simon Neuville
 * Copyright (c) 2017 Stiaan Uyttersprot
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package com.dp16.runamicghent.Persistence;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.util.JSON;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage component that takes care of the server side storage on the MongoDB database.
 * Before calling any of the methods that interact with the database, {@link #connect()} should be called.
 * Otherwise the methods will return failures.
 * Likewise, {@link #disconnect()} should be called to clean up resources.
 * Created by Nick on 9-4-2017.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class ServerStorage implements StorageComponent {
    private boolean connected;
    private MongoClient mongoClient;
    private MongoCollection<Document> runningStatisticsCollection;
    private MongoCollection<Document> runningStatisticsGhostCollection;
    private MongoCollection<Document> aggregateRunningStatisticsCollection;
    private String databaseToUse;
    private String userId;
    private static final String FILENAME_KEY = "filename";
    private static final String EDIT_TIME_KEY = "edittime";
    private static final String USER_KEY = "user";
    private static final String TAG = "ServerStorage";

    ServerStorage(String clientToken) {
        connected = false;
        databaseToUse = Constants.Storage.MONGODBNAME;
        userId = clientToken;
    }

    void setUserId(String clientToken) {
        userId = clientToken;
    }

    /**
     * For testing purposes only. Overrides the database in Constants.Storage.MONGODBNAME.
     * <p>
     * Must be called <i>before</i> {@link #connect()} to have any effect.
     *
     * @param database New database to use.
     */
    @VisibleForTesting
    public void setDatabaseToUse(String database) {
        databaseToUse = database;
    }

    /**
     * Sets up the database connection.<br>
     * <b>Must</b> be called before any method that accesses the database. Otherwise the other methods will report failure.
     * <p>
     * Does nothing if already connected.
     */
    void connect() {
        if (!connected) {
            String databaseURI = "mongodb://" + Constants.Storage.MONGOUSER + ":" + Constants.Storage.MONGOPASS + "@" + Constants.Server.ADDRESS + ":" + Constants.Server.MONGOPORT + "/" + databaseToUse;
            mongoClient = new MongoClient(new MongoClientURI(databaseURI));
            MongoDatabase mongoDatabase = mongoClient.getDatabase(databaseToUse);
            runningStatisticsCollection = mongoDatabase.getCollection(Constants.Storage.RUNNINGSTATISTICSCOLLECTION);
            runningStatisticsGhostCollection = mongoDatabase.getCollection(Constants.Storage.RUNNINGSTATISTICSGHOSTCOLLECTION);
            aggregateRunningStatisticsCollection = mongoDatabase.getCollection(Constants.Storage.AGGREGATERUNNINGSTATISTICSCOLLECTION);
            connected = true;
        }
    }


    /**
     * Cleans up the database connection. Should be called when done with the database.
     * <p>
     * Does nothing if not connected.
     */
    void disconnect() {
        if (connected) {
            mongoClient.close();
            connected = false;
        }
    }

    @Nullable
    @Override
    public List<String> getFilenamesRunningStatistics() {
        if (!connected) {
            return null;
        }
        return getFilenamesFromCollection(runningStatisticsCollection);
    }

    @Override
    public List<String> getFilenamesRunningStatisticsGhosts() {
        if (!connected) {
            return null;
        }
        return getFilenamesFromCollection(runningStatisticsGhostCollection);
    }

    @Nullable
    @Override
    public RunningStatistics getRunningStatisticsFromFilename(String filename) {
        if (!connected) {
            return null;
        }
        // make query
        Document query = new Document(USER_KEY, userId);
        query.put(FILENAME_KEY, filename);

        return (RunningStatistics) getObjectFromQuery(runningStatisticsCollection, query, RunningStatistics.class);
    }


    @Override
    public boolean saveRunningStatistics(RunningStatistics runningStatistics, long editTime) {
        if (!connected) {
            return false;
        }
        Document wrapper = new Document(USER_KEY, userId);
        wrapper.put(FILENAME_KEY, String.valueOf(runningStatistics.getStartTimeMillis()));
        wrapper.put(EDIT_TIME_KEY, editTime);

        return saveObjectInWrapper(runningStatisticsCollection, wrapper, runningStatistics);
    }

    @Override
    public boolean saveRunningStatisticsGhost(String filename) {
        if (!connected) {
            return false;
        }
        // make ghost document
        Document document = new Document(USER_KEY, userId);
        document.put(FILENAME_KEY, filename);

        // put in database
        try {
            runningStatisticsGhostCollection.insertOne(document);
            return true;
        } catch (MongoException e) {
            Log.d(TAG, "Unable to save runningStatisticsGhost on server.", e);
            return false;
        }
    }

    @Override
    public long getRunningStatisticsEditTime(String filename) {
        if (!connected) {
            return 0;
        }
        return getEditTimeFromFilename(runningStatisticsCollection, filename);
    }

    @Override
    public boolean deleteRunningStatistics(String filename) {
        if (!connected) {
            return false;
        }
        return deleteDocumentByFilename(filename, runningStatisticsCollection);
    }

    @Override
    public boolean deleteRunningStatisticsGhost(String filename) {
        if (!connected) {
            return false;
        }
        return deleteDocumentByFilename(filename, runningStatisticsGhostCollection);
    }

    @Nullable
    @Override
    public AggregateRunningStatistics getAggregateRunningStatistics() {
        if (!connected) {
            return null;
        }
        // make query
        Document query = new Document(USER_KEY, userId);

        return (AggregateRunningStatistics) getObjectFromQuery(aggregateRunningStatisticsCollection, query, AggregateRunningStatistics.class);
    }


    @Override
    public boolean saveAggregateRunningStatistics(AggregateRunningStatistics aggregateRunningStatistics, long editTime) {
        if (!connected) {
            return false;
        }
        Document wrapper = new Document(USER_KEY, userId);
        wrapper.put(EDIT_TIME_KEY, editTime);

        return saveObjectInWrapper(aggregateRunningStatisticsCollection, wrapper, aggregateRunningStatistics);
    }

    /**
     * @return time (in ms since EPOCH) the AggregateRunningStatistics file was last edited. 0 if file does not exist.
     */
    long getAggregateRunningStatisticsEditTime() {
        if (!connected) {
            return 0;
        }

        // make query
        Document query = new Document(USER_KEY, userId);

        // retrieve data from database, only fetching the 'edittime' field
        FindIterable<Document> returnedDocuments = aggregateRunningStatisticsCollection.find(query).projection(Projections.include(EDIT_TIME_KEY));

        // process response
        Document retrievedDocument = returnedDocuments.first();
        if (retrievedDocument == null) {
            // database had nothing for us, so we return 0
            return 0;
        }
        return retrievedDocument.getLong(EDIT_TIME_KEY);

    }

    /**
     * Warning: really deleting the AggregateRunningStatistics will cause problems with server synchronization.
     * It is recommended to just do {@link #saveAggregateRunningStatistics(AggregateRunningStatistics, long)} with an empty object.
     */
    @Override
    public boolean deleteAggregateRunningStatistics() {
        if (!connected) {
            return false;
        }
        // make query
        Document query = new Document(USER_KEY, userId);

        // remove from database
        try {
            aggregateRunningStatisticsCollection.deleteOne(query);
            return true;
        } catch (MongoException e) {
            Log.d(TAG, "Unable to delete aggr. stats on server", e);
            return false;
        }
    }

    /**
     * Retrieves all filenames related to the current user from a collection.
     *
     * @param collection Collection to retrieve from
     * @return List of all filenames
     */
    private List<String> getFilenamesFromCollection(MongoCollection<Document> collection) {
        List<String> result = new ArrayList<>();

        // make a query
        Document query = new Document(USER_KEY, userId);

        // execute the query, only returning 'filename'
        FindIterable<Document> returnedDocuments = collection.find(query).projection(Projections.include(FILENAME_KEY));

        // process the result
        for (Document i : returnedDocuments) {
            result.add(i.getString(FILENAME_KEY));
        }

        return result;
    }

    /**
     * Deletes a document identified by a filename and userId (implicit) from the given collection.
     *
     * @param filename   Name of document to delete.
     * @param collection Collection to delete from.
     * @return true on success, false on failure
     */
    private boolean deleteDocumentByFilename(String filename, MongoCollection<Document> collection) {
        // make query
        Document query = new Document(USER_KEY, userId);
        query.put(FILENAME_KEY, filename);

        // remove from database
        try {
            DeleteResult deleteResult = collection.deleteOne(query);
            if (deleteResult.getDeletedCount() == 0) {
                // if no file was deleted: we report a failure
                return false;
            }
            return true;
        } catch (MongoException e) {
            Log.d(TAG, "Unable to delete file on server: " + filename, e);
            return false;
        }
    }

    /**
     * Retrieves an object identified by the given query from the database and parses it to an object of Class clazz.
     *
     * @param collection Collection to retrieve from.
     * @param query      Query that <i>uniquely</i> identifies the document.
     * @param clazz      Class to parse the document to.
     * @return Object of Class clazz. Null if nothing is found.
     * @throws ClassCastException if contents of the document could not be parsed to an object of clazz.
     */
    private Object getObjectFromQuery(MongoCollection<Document> collection, Document query, Class clazz) throws ClassCastException {
        // retrieve data from database
        FindIterable<Document> returnedDocuments = collection.find(query);

        // process response
        Document retrievedDocument = returnedDocuments.first();
        if (retrievedDocument == null) {
            // database had nothing for us
            return null;
        }
        Document unpackedDocument = (Document) retrievedDocument.get("content");

        // json from document
        // Note: I'm using a 2.x mongo driver feature that is near Deprecation.
        // The reason: the new 3.x Document.toJson() does not produce valid JSON, even with the STRICT flag.
        String contentAsJSon = JSON.serialize(unpackedDocument);

        // object from JSON
        try {
            return new Gson().fromJson(contentAsJSon, clazz);
        } catch (JsonSyntaxException e) {
            Log.d(TAG, e.getMessage(), e);
            throw new ClassCastException("The document contents could not be cast to a " + clazz.getName() + " object");
        }
    }

    /**
     * Puts an object in a wrapper document and saves it to the collection.
     *
     * @param collection Collection to save in.
     * @param wrapper    Top level document to wrap the object in.
     * @param object     Object to save
     * @return true on success, false on failure.
     */
    private boolean saveObjectInWrapper(MongoCollection<Document> collection, Document wrapper, Object object) {
        // JSON from object
        String objectAsJson = new Gson().toJson(object);

        // document from JSON
        Document document = Document.parse(objectAsJson);
        document.remove("_id");

        // put document in wrapper
        wrapper.put("content", document);

        // put in database
        try {
            collection.insertOne(wrapper);
            return true;
        } catch (MongoException e) {
            Log.d(TAG, "Unable to save object on server: " + object.getClass(), e);
            return false;
        }
    }

    /**
     * @param collection Collection the file lives in
     * @param filename   File for which to fetch the filename
     * @return time (in ms since EPOCH) the file was last edited. 0 if file does not exist.
     */
    private long getEditTimeFromFilename(MongoCollection<Document> collection, String filename) {
        // make query
        Document query = new Document(USER_KEY, userId);
        query.put(FILENAME_KEY, filename);

        // retrieve data, only fetching the 'edittime' field
        FindIterable<Document> returnedDocuments = collection.find(query).projection(Projections.include(EDIT_TIME_KEY));

        // process response
        Document retrievedDocument = returnedDocuments.first();
        if (retrievedDocument == null) {
            // database had nothing for us, so we return 0
            return 0;
        }
        return retrievedDocument.getLong(EDIT_TIME_KEY);
    }
}
