package it.unipi.dii.lsmsdb.boardgamecafe.repository.mongodbms;

import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.mongo.BoardgameModelMongo;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardgameRepoMongo extends MongoRepository<BoardgameModelMongo, String> {
    @Query("{'boardgameName': ?0}")
    Optional<BoardgameModelMongo> findByBoardgameName(String boardgameName);

    Optional<BoardgameModelMongo> findById(String id);

    @Aggregation(pipeline = { "{ $project: { boardgameName: 1, _id: 0 } }" })
    List<String> findAllBoardgameNames();
}
