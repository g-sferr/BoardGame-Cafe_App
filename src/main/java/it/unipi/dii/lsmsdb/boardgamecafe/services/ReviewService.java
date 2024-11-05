package it.unipi.dii.lsmsdb.boardgamecafe.services;

import it.unipi.dii.lsmsdb.boardgamecafe.BoardgamecafeApplication; //To Retrieve model bean instance

import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.mongo.BoardgameModelMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.mongo.GenericUserModelMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.mongo.ReviewModelMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.mongo.UserModelMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.repository.mongodbms.BoardgameDBMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.repository.mongodbms.ReviewDBMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.repository.mongodbms.UserDBMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.utils.Symbols;

import org.neo4j.cypherdsl.core.Use;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class ReviewService {

    @Autowired
    ReviewDBMongo reviewMongoOp;
    @Autowired
    BoardgameDBMongo boardgameMongoOp;
    @Autowired
    UserDBMongo userMongoOp;

    private final static Logger logger = LoggerFactory.getLogger(ReviewService.class);


    @Transactional
    public boolean insertReview(ReviewModelMongo review,
                                BoardgameModelMongo boardgame,
                                UserModelMongo user) {
        try {
            String usernameCreatorReview = user.getUsername();
            String boardgameToBeReviewdName = boardgame.getBoardgameName();

            if (!reviewMongoOp.addReview(review)) {
                throw new RuntimeException("\nError in adding the review to the collection of Reviews.");
            }

            //Recupero la review appena aggiunta alla collection Reviews considerando
            // il gioco a cui fa riferimento l'utente che l'ha creata
            Optional<ReviewModelMongo> reviewFromMongo = reviewMongoOp.
                                       findByUsernameAndBoardgameName(
                                               usernameCreatorReview,
                                               boardgameToBeReviewdName);

            if(reviewFromMongo.isPresent()){

                ReviewModelMongo newReview = reviewFromMongo.get();

                if (!addReviewToUser(user, newReview)) {
                    throw new RuntimeException("\nError in adding the review to the collection of Users. " +
                            "Rollabck performed for review in Reviews Collection into MongoDB");
                }
                if (!addReviewToBoardgame(boardgame, newReview)) {
                    throw new RuntimeException("\nError in adding the review to the collection of Boardgames. " +
                            "Rollabck performed for review in Reviews Collection into MongoDB");
                }
                System.out.println("\nNew Review ID: " + newReview.getId());
                System.out.println("\nReview inserted into MongoDB dbms for Users, Reviews and Boardgames collections.");

            } else {
                throw new RuntimeException("\nReview created by " + usernameCreatorReview +" " +
                        "for te Boardgame -> " + boardgameToBeReviewdName + " not found!");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean addReviewToUser(UserModelMongo user, ReviewModelMongo review) {

        //checkLastReviewUser(user, false);
        user.addReview(review); //local object

        if (!userMongoOp.addReviewInUserArray(user, review)) {
            logger.error("Error in adding the review to the collection of users");
            if (!reviewMongoOp.deleteReview(review)) {
                logger.error("Error in deleting the review from the collection of reviews");
            }
            return false;
        }
        return true;
    }

    public boolean addReviewToBoardgame(BoardgameModelMongo boardgame, ReviewModelMongo review) {

        // checkLastReviewBoardgame(boardgame, false);
        boardgame.addReview(review);

        if (!boardgameMongoOp.addReviewInBoardgameArray(boardgame, review))  {
            logger.error("Error in adding the review to the collection of boardgames");
            if (!reviewMongoOp.deleteReview(review)) {
                logger.error("Error in deleting the review from the collection of reviews");
            }
            return false;
        }
        return true;
    }

    @Transactional
    // Eliminare la review dalla collection REVIEWS -> USERS -> BOARDGAMES
    public boolean deleteReview(ReviewModelMongo selectedReview, UserModelMongo loggedUser) //BoardgameModelMongo boardgame,
    {
        try
        {
            // Solamente l'utente che ha scritto la review può cancellarla. Da grafica dovrebbe già essere verificato,
            // ma un controllo in più non fa male.
            if (!selectedReview.getUsername().equals(loggedUser.getUsername()))
                throw new RuntimeException("deleteReview(): You don't have the permission to delete this review");

            String reviewId = selectedReview.getId();
            String boardgameName = selectedReview.getBoardgameName();

            if (boardgameName.isEmpty())
                throw new RuntimeException("This review is not refered to a boardgame!");

            if (!deleteReviewInBoardgame(selectedReview))
                throw new RuntimeException("deleteReviewInBoardgame(): -> deleteReviewInBoardgame failed");

            if (!deleteReviewInUser(loggedUser, selectedReview)) {
                throw new RuntimeException("deleteReviewInUser(): -> deleteReviewInBoardgame failed");
                //logger.error("Error in deleting the review from the collection of users");
            }
            // cancellare la reviews nelle review dello user in locale

            if (!reviewMongoOp.deleteReview(selectedReview)) {
                throw new RuntimeException("deleteReviewById(): -> deleteReviewInBoardgame failed");
            }

        }
        catch (RuntimeException e)
        {
            System.out.println("DeleteReview Exception: " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean deleteReviewInBoardgame(ReviewModelMongo selectedReview) // Cancella sia da mongo che in locale
    {
        Optional<BoardgameModelMongo> boardgameResult =
                boardgameMongoOp.findBoardgameByName(selectedReview.getBoardgameName());

        if (boardgameResult.isEmpty())
            throw new RuntimeException("deleteReviewInBoardgame Exception: The referred boardgame not exists in DB. Name: " + selectedReview.getBoardgameName());

        BoardgameModelMongo referredBoardgame = boardgameResult.get();
        if (boardgameMongoOp.deleteReviewInBoardgameReviewsById(boardgameResult.get().getBoardgameName(), selectedReview.getId())) // Cancella da Mongo
            if(referredBoardgame.deleteReview(selectedReview.getId())) // Cancella da locale
                return true;
            else
            {
                System.out.println("deleteReviewInBoardgame Exception: The reviews was on Mongo but not in local");
                return false;
            }
        else
            throw new RuntimeException("The referred boardgame ON MONGO doesn't have this review -> ******\n" + selectedReview + "\n******\n");
    }

    private boolean deleteReviewInUser(UserModelMongo user, ReviewModelMongo selectedReview) // Cancella sia da Mongo che in locale
    {
        if (userMongoOp.deleteReviewInUserReviewsById(user.getId(), selectedReview.getId()))
        {
            if (user.deleteReview(selectedReview.getId()))
                return true;
            else
            {
                System.out.println("deleteReviewInUser Exception: Review |" + selectedReview.getId() + "| present in Mongo but non in local");
                return false;
            }
        }
        throw new RuntimeException("deleteReviewInUser Exception: Review |" + selectedReview.getId() + "| not present in Mongo");


//        if (user.deleteReview(selectedReview))
//            return userMongoOp.updateUser(user.getId(), user, "user");
//        return false;
//        if (user == null)
//        {
//            String username = selectedReview.getUsername();
//            if (username.equals("Deleted User")) {
//                return true;
//            }
//
//            Optional<GenericUserModelMongo> userResult =
//                    userMongoOp.findByUsername(username);
//
//            if (userResult.isEmpty()) {
//                return false;
//            }
//
//            UserModelMongo newUser = (UserModelMongo) userResult.get();
//            return deleteUserReview(newUser, reviewId);
//
//        } else
//            return deleteUserReview(user, reviewId);
    }

    @Transactional
    public boolean updateReview(ReviewModelMongo selectedReview) {
        try {
            if (!reviewMongoOp.updateReview(selectedReview.getId(), selectedReview)) {
                throw new RuntimeException("\nError in updating the review in the collection of reviews");
            }
            if (!updateReviewInUser(selectedReview)) {
                throw new RuntimeException("\nError in updating the review in the collection of users");
            }
            if (!updateReviewInBoardgame(selectedReview)) {
                throw new RuntimeException("\nError in updating the review in the collection of Board Games");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean updateReviewInBoardgame(ReviewModelMongo selectedReview) {

        Optional<BoardgameModelMongo> boardgameResult =
                boardgameMongoOp.findBoardgameByName(selectedReview.getBoardgameName());

        if (boardgameResult.isPresent()) {
            BoardgameModelMongo boardgame = boardgameResult.get();

            //Local object state management
            if (boardgame.deleteReview(selectedReview.getId())) {
                boardgame.addReview(selectedReview);
            }

            return boardgameMongoOp.updateBoardgameMongo(boardgame.getId(), boardgame);

            //******************************************************************************************
            // Valutare l'uso di questa variante, potrebbe non essere necessario fare delete/add nell'array
            // dell'oggetto locale dato che si andrebbe ad agire direttamente su MongoDB

//            //delete implementata da fra - pullare
//            if (!boardgameMongoOp.deleteReviewInBoardgameArray(boardgame, selectedReview)) {
//
//                return boardgameMongoOp.addReviewInBoardgameArray(boardgame, selectedReview);
//            }
            //******************************************************************************************
        }
        System.out.println("\nError: There is not present a bordgame with the name: " +
                              boardgameResult.get().getBoardgameName());
        return false;
    }

    public boolean updateReviewInUser(ReviewModelMongo selectedReview) {
        Optional<GenericUserModelMongo> userResult =
                userMongoOp.findByUsername(selectedReview.getUsername());

        if (userResult.isPresent()) {
            GenericUserModelMongo genericUser = userResult.get();
            String _classValue = genericUser.get_class();

            if (!_classValue.equals("user")) {
                logger.error("Error: selected user is not a common user");
                return false;
            }
            UserModelMongo user = (UserModelMongo) genericUser;

            //Local object state management
            if (user.deleteReview(selectedReview.getId())) {
                user.addReview(selectedReview);
            }
            return userMongoOp.updateUser(user.getId(), user, "user");

            //******************************************************************************************
            // Valutare l'uso di questa variante, potrebbe non essere necessario fare delete/add nell'array
            // dell'oggetto locale dato che si andrebbe ad agire direttamente su MongoDB

//            //delete implementata da fra - pullare
//            if (!userMongoOp.deleteReviewInUserArray(user, selectedReview)) {
//
//                return userMongoOp.addReviewInUserArray(user, selectedReview);
//            }
            //******************************************************************************************
        }
        System.out.println("\nError: There is not present a User with the username: " +
                              userResult.get().getUsername());
        return false;
    }


    // BEGIN - ************************* To_Check *************************

    // Valutare l'uso di questi 2 metodi: utili per la gestione automatica della numerosità delle
    // reviews all'interno dell'array di user e boardgame - potrebbe tornare utile anche per la grafica
    private void checkLastReviewBoardgame(BoardgameModelMongo boardgame, boolean isDelete) {

        int numReviews = boardgame.getReviews().size();
        if (numReviews == 50) {
            if (isDelete) {
                List<ReviewModelMongo> oldReviews = reviewMongoOp.
                        findOldReviews(boardgame.getBoardgameName(), true);
                boardgame.getReviews().add(numReviews, oldReviews.get(0));
            } else {
                boardgame.getReviews().remove(numReviews - 1);
            }
        }
    }

    private void checkLastReviewUser(UserModelMongo newUser, boolean isDelete) {

        int numReviews = newUser.getReviews().size();
        if (numReviews == 50) {
            if (isDelete) {
                List<ReviewModelMongo> oldReviews =
                        reviewMongoOp.findOldReviews(newUser.getUsername(), false);

                newUser.getReviews().add(numReviews, oldReviews.get(0));
            } else {
                newUser.getReviews().remove(numReviews - 1);
            }
        }
    }
    // END - ************************* To_Check *************************



//    //TO_CHECK -> GUI
//    public ReviewModelMongo getSelectedReview(int counterPages, int tableIndex,
//                                              UserModelMongo user, BoardgameModelMongo boardagme,
//                                              List<ReviewModelMongo> reviews) {
//        boolean isEmbedded = true;
//        int index;
//
//        ReviewModelMongo review;
//        if (counterPages > 4) {
//            index = tableIndex + (counterPages * 10 - 50);
//            review = reviews.get(index);
//            isEmbedded = false;
//        } else {
//            index = tableIndex + (counterPages * 10);
//            if (boardagme == null) {
//                review = user.getReviews().get(index);
//            } else {
//                review = boardagme.getReviews().get(index);
//            }
//        }
//        if (review == null) {
//            logger.error("Review null, not found ");
//            return null;
//        }
//        BoardgamecafeApplication.getInstance().getModelBean().putBean(Symbols.IS_EMBEDDED, isEmbedded);
//        return review;
//    }

    private String getReviewId(ReviewModelMongo review) {

        Optional<ReviewModelMongo> reviewResult =
                reviewMongoOp.findByUsernameAndBoardgameName( review.getUsername(),
                        review.getBoardgameName());
        if (reviewResult.isPresent()) {
            return reviewResult.get().getId();
        } else {
            logger.error("Review not found");
        }
        return "";
    }
}
