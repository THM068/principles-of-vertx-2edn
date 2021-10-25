package net.yurimednikov.vertxbook.cashx.repositories;

import java.util.Optional;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import net.yurimednikov.vertxbook.cashx.models.Operation;
import net.yurimednikov.vertxbook.cashx.models.OperationList;

public record OperationMongoRepositoryImpl(MongoClient client) implements OperationRepository {

    private final static OperationDocumentMapper mapper = new OperationDocumentMapper();

    @Override
    public Future<Operation> saveOperation(Operation operation) {
        JsonObject document = new JsonObject();
        document.put("userId", operation.userId());
        
        JsonObject amount = new JsonObject();
        amount.put("value", operation.amount().value().doubleValue());
        amount.put("currency", operation.amount().currency());

        document.put("amount", amount);
        document.put("name", operation.name());
        document.put("categoryId", operation.category().id());
        document.put("dateTime", operation.dateTime().toString());
        document.put("accountId", operation.accountId());
        return client.insert("operations", document)
            .flatMap(id -> Future.succeededFuture(Operation.withId(id, operation)));
    }

    @Override
    public Future<Optional<Operation>> findOperationById(String id) {

        JsonArray aggregatePipeline = new JsonArray();
        aggregatePipeline.add(new JsonObject().put("$match", new JsonObject().put("_id", id)));
        aggregatePipeline.add(
            new JsonObject().put("$lookup", 
                new JsonObject().put("from", "categories")
                .put("localField", "categoryId")
                .put("foreignField", "_id")
                .put("as", "category"))
        );
        // aggregatePipeline.add(new JsonObject().put("cursor", new JsonObject().put("batchSize", 0)));

        JsonObject aggregateCommand = new JsonObject();
        aggregateCommand.put("aggregate", "operations");
        aggregateCommand.put("pipeline", aggregatePipeline);
        aggregateCommand.put("cursor", new JsonObject());
        // JsonObject query = new JsonObject();
        // query.put("_id", id);
        // return client.findOne("operations", query, null)
        //     .flatMap(document -> Future.succeededFuture(Optional.ofNullable(document)))
        //     .flatMap(result -> Future.succeededFuture(result.map(mapper::apply)));
        return client.runCommand("aggregate", aggregateCommand)
            .flatMap(result -> {
                System.out.println(result.getJsonObject("cursor").getJsonArray("firstBatch"));
                JsonArray results = result.getJsonObject("cursor").getJsonArray("firstBatch");
                System.out.println(results);
                JsonObject data = results.getJsonObject(0);
                System.out.println(data);
                return Future.succeededFuture(data);
            })
            .flatMap(document -> Future.succeededFuture(Optional.ofNullable(document)))
            .flatMap(result -> Future.succeededFuture(result.map(mapper::apply)));
        // return null;
    }

    @Override
    public Future<Boolean> removeOperation(String id) {
        JsonObject query = new JsonObject();
        query.put("_id", id);  
        Future<Boolean> future = client.removeDocument("operations", query)
            .flatMap(result -> Future.succeededFuture(result.getRemovedCount() > 0));
        return future;
    }

    @Override
    public Future<OperationList> findOperations(String userId) {
        // todo with aggregation
        return null;
    }
    
}
