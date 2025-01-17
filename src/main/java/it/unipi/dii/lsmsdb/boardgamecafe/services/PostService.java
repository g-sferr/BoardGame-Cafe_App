package it.unipi.dii.lsmsdb.boardgamecafe.services;

import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.CommentModel;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.mongo.PostModelMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.neo4j.BoardgameModelNeo4j;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.neo4j.PostModelNeo4j;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.neo4j.UserModelNeo4j;
//import it.unipi.dii.lsmsdb.boardgamecafe.repository.mongodbms.CommentDBMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.repository.mongodbms.PostDBMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.repository.neo4jdbms.BoardgameDBNeo4j;
import it.unipi.dii.lsmsdb.boardgamecafe.repository.neo4jdbms.PostDBNeo4j;
import it.unipi.dii.lsmsdb.boardgamecafe.repository.neo4jdbms.UserDBNeo4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PostService {
    @Autowired
    PostDBMongo postDBMongo;
    @Autowired
    PostDBNeo4j postDBNeo4j;
    @Autowired
    UserDBNeo4j userDBNeo4j;
    @Autowired
    BoardgameDBNeo4j boardgameDBNeo4j;
//    @Autowired
//    CommentDBMongo commentDBMongo;

    private final static Logger logger = LoggerFactory.getLogger(PostService.class);

    @Transactional
    public PostModelMongo insertPost(PostModelMongo postModelMongo)
    {
        try
        {
            String usernameAuthorPost = postModelMongo.getUsername();
            Optional<UserModelNeo4j> authorPostOptional = userDBNeo4j.findByUsername(usernameAuthorPost);
            if (authorPostOptional.isEmpty()) // Check if the user is OK
                throw new RuntimeException("Post not added. No account match was found.");

            PostModelMongo insertedPost = postDBMongo.addPost(postModelMongo);
            if (insertedPost == null)
                throw new RuntimeException("Unable to add post to MongoDB collection.");

            PostModelNeo4j postModelNeo4j = new PostModelNeo4j(insertedPost.getId()); // Creation of post node in neo
            UserModelNeo4j authorPost = authorPostOptional.get();
            if (!postModelMongo.getTag().isEmpty()) // if the game is referred to a boardGame, then it's necessary the creation of the "REFERRED TO" relationship
            {
                Optional<BoardgameModelNeo4j> referredBoardgameOptional = boardgameDBNeo4j.findByBoardgameName(insertedPost.getTag());
                referredBoardgameOptional.ifPresent(referredBoardgames -> postModelNeo4j.setTaggedGame(referredBoardgames));
            }
            if (!postDBNeo4j.addPost(postModelNeo4j)) // The REFERES TO relationship is already added (if exists)
            {
                deletePost(insertedPost);
                throw new RuntimeException("Unable to add post to Neo4j.");
            }
            if (!addPostToUser(postModelNeo4j, authorPost))
            {
                deletePost(insertedPost);
                throw new RuntimeException("Unable to create relationships.");
            }
            return insertedPost;
        }
        catch (Exception ex) {
            System.err.println("[ERROR] insertPost@PostService.java raised an exception: " + ex.getMessage());
            return null;
        }
    }

    private boolean addPostToUser(PostModelNeo4j postModelNeo4j, UserModelNeo4j userModelNeo4j)
            // This methos is private beacuse is used only after the insetion-Post
    {
        try {
            userModelNeo4j.addWrittenPost(postModelNeo4j);
            if (!userDBNeo4j.updateUser(userModelNeo4j.getId(), userModelNeo4j)) {
                logger.error("Error in connecting post to user in Neo4j");
                return false;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Transactional
    public boolean deletePost(PostModelMongo postModelMongo)
    {
        try
        {
            // delete all comments
//            if (!commentDBMongo.deleteByPost(postModelMongo.getId())) {
//                throw new RuntimeException("Error in deleting comments of post in MongoDB");
//            }
//            if (!commentDBNeo4j.deleteByPost(postModelMongo.getId())) {
//                throw new RuntimeException("Error in deleting comments of post in Neo4j");
//            }
            // delete post
            if (!postDBNeo4j.deletePost(postModelMongo.getId())) {
                throw new RuntimeException("Error in deleting post in Neo4j");
            }
            if (!postDBMongo.deletePost(postModelMongo)) {
                throw new RuntimeException("Error in deleting post in MongoDB");
            }
        }
        catch (Exception ex) {
            System.out.println("Exception deletePost(): " + ex.getMessage());
            return false;
        }
        return true;
    }

    /*
        Da eliminare? Questa funzione ritorna il post, avendo solamente il timestamp ed username dell'autore.
        Può essere davvero utile? Sembra di no, in quanto dovrei avere la lista in locale dei post che ho letto da
        mongo, e quindi per ognuno di essi dovrei già avere l'id memorizzato.
        Comunque, per il momento la lascio. -> 29/10/2024
    public String getPostId(PostModelMongo postModelMongo)
    {

        Optional<PostModelMongo> postResult =
                postDBMongo.findByUsernameAndTimestamp(postModelMongo.getUsername(), postModelMongo.getTimestamp());
        if (postResult.isPresent()) {
            return postResult.get().getId();
        }
        else {
            logger.error("Post not found");
        }
        return "";
    }
    */
    public void likeOrDislikePost(String username, String postId)
    {
        try {
            if (postDBNeo4j.hasUserLikedPost(username, postId))
            {
                // System.out.println("PostService: Il post ha il Like. Rimuovo...");
                if (postDBMongo.updateLikeCount(postId, false))
                    postDBNeo4j.removeLikePost(username, postId);
                else
                    throw new RuntimeException("Problem with Mongo removing like count");
            }
            else
            {
                // System.out.println("PostService: Il post non ha il Like. Aggiungo...");
                if (postDBMongo.updateLikeCount(postId, true))
                    postDBNeo4j.addLikePost(username, postId, true);
                else
                    throw new RuntimeException("Problem with Mongo adding like count");
            }
        } catch (Exception ex) {
            // Log dell'eccezione
            System.out.println("Error liking or disliking post for user " + username + " on post " + postId + ": " + ex.getMessage());
        }
    }

    public boolean hasLikedPost(String username, String postId) {
        try {
            return postDBNeo4j.hasUserLikedPost(username, postId);
        } catch (Exception ex) {
            // Log dell'eccezione
            logger.error("Error checking like status for user " + username + " on post " + postId + ": " + ex.getMessage());
            return false; // Restituisce false in caso di errore
        }
    }
//    fra: NON USATA. Da eliminare? -> 29/12/2024
//    public Optional<PostModelMongo> showMostLikedPost() {
//        try {
//            // Ottieni il post con il maggior numero di like da Neo4j tramite la relativa relazione LIKES
//            PostModelNeo4j mostLikedPost = postDBNeo4j.findPostWithMostLikes();
//
//            // Se il post con più like esiste, recupera da MongoDB lo stesso post utilizzando il suo ID
//            if (mostLikedPost != null) {
//                // Usa il metodo findById per ottenere il post dal database Mongo
//                return postDBMongo.findById(mostLikedPost.getId());
//            } else {
//                return Optional.empty(); // Restituisce un Optional vuoto se non ci sono post (meglio del null in questo caso)
//            }
//        } catch (Exception ex) {
//            // Log dell'eccezione
//            logger.error("Error fetching most liked post: " + ex.getMessage());
//            return Optional.empty(); // Restituisce un Optional vuoto in caso di errore
//        }
//    }

    public List<PostModelMongo> suggestPostLikedByFollowedUsers(String currentUser, int limitResults, int skipCounter) {
        // skipCounter needed for incremental post displaying
        List<PostModelNeo4j> postsLikedByFollowedUsers = postDBNeo4j.
                getPostsLikedByFollowedUsers(currentUser, limitResults, skipCounter);
        List<PostModelMongo> suggestedPostsMongo = new ArrayList<>();

        for (PostModelNeo4j postsLikedId : postsLikedByFollowedUsers)
        {
            Optional<PostModelMongo> postMongo = postDBMongo.findById(postsLikedId.getId());
            // (Lambda fun) If the suggested Post is found, then it's added to the suggestedMongoUsers list
            postMongo.ifPresent(suggestedPostsMongo::add);
        }

        return suggestedPostsMongo;
    }

    public List<PostModelMongo> findPostsByFollowedUsers(String currentUser, int limitResults, int skipCounter) {
        // skipCounter needed for incremental post displaying
        List<PostModelNeo4j> postsByFollowedUsers = postDBNeo4j.
                getPostsByFollowedUsers(currentUser, limitResults, skipCounter);
        List<PostModelMongo> retrievedPostsMongo = new ArrayList<>();

        for (PostModelNeo4j postByFollowedUser : postsByFollowedUsers)
        {
            Optional<PostModelMongo> postMongo = postDBMongo.findById(postByFollowedUser.getId());
            // (Lambda fun) If the suggested Post is found, then it's added to the suggestedMongoUsers list
            postMongo.ifPresent(retrievedPostsMongo::add);
        }

        return retrievedPostsMongo;
    }

    public List<PostModelMongo> findPostsByTag(String tag, int limitResults, int skipCounter) {
        // skipCounter needed for incremental post displaying
        List<PostModelMongo> postsReferringToTag = postDBMongo.findByTag(tag, limitResults, skipCounter);
        List<PostModelMongo> retrievedPostsMongo = new ArrayList<>();
        for (PostModelMongo postReferringToTag : postsReferringToTag) {
            Optional<PostModelMongo> postMongo = postDBMongo.findById(postReferringToTag.getId());
            postMongo.ifPresent(retrievedPostsMongo::add);
        }

        return retrievedPostsMongo;
    }

    @Transactional
    public boolean insertComment(CommentModel comment, PostModelMongo post, UserModelNeo4j user) {
        try {
            //CommentModelMongo insertedCommentResult = commentMongo.addComment(comment);         // Inserting the comment in MongoDB
            //if (insertedCommentResult == null) {
            //    throw new RuntimeException("Error while inserting the new MongoDB comment.");
            //}
//            String newCommentId = comment.getId();
//
//            if (!commentDBNeo4j.addComment(new CommentModelNeo4j(newCommentId))) {       // Inserting the comment in Neo4J
//                //commentMongo.deleteComment(insertedCommentResult);
//                throw new RuntimeException("Error while inserting the new Neo4J comment (MongoDB comment has been removed).");
//            }
//
//            if (!addCommentRelationshipToNeo4jUser(new CommentModelNeo4j(newCommentId), user)) {       // Creating the needed relationships in Neo4J
//                deleteComment(comment, post);
//                throw new RuntimeException("Error while creating relationships in Neo4J related to a new comment insertion.");
//            }

            if (!addCommentToMongoPost(comment, post)) {        // Adding the comment to the post's comment list
                deleteComment(comment, post);
                throw new RuntimeException("Error while creating relationships in Neo4J related to a new comment insertion.");
            }
        } catch (RuntimeException e) {
            System.err.println("[ERROR] insertComment@CommentService.java raised an exception: " + e.getMessage());
            return false;
        }

        return true;
    }

    @Transactional
    public boolean deleteComment(CommentModel comment, PostModelMongo post) {
        try
        {
            // delete all comments
            if (!postDBMongo.deleteCommentFromArrayInPost(post, comment)) {
                throw new RuntimeException("Error in deleting comments from array post in MongoDB");
            }
//            if (!commentDBNeo4j.deleteAndDetachComment(comment.getId())) {
//                throw new RuntimeException("Error in deleting comments of post in Neo4j");
//            }
//            if (!commentMongo.deleteComment(comment)) {
//                throw new RuntimeException("Error in deleting comment from Comment Collection MongoDB");
//            }
        }
        catch (Exception ex) {
            System.out.println("Exception deletePost(): " + ex.getMessage());
            return false;
        }
        return true;
    }
//
//    private boolean addCommentRelationshipToNeo4jUser(CommentModelNeo4j comment, UserModelNeo4j user) {
//        try {
//            user.addWrittenComment(comment);
//            if(!userDBNeo4j.addUser(user)) {
//                return false;
//            }
//        } catch (Exception e) {
//            System.out.println("[ERROR] addCommentToUser()@PostService.java generated an exception: " + e.getMessage());
//            return false;
//        }
//        return true;
//    }

    private boolean addCommentToMongoPost(CommentModel comment, PostModelMongo post) {

        post.addComment(comment);           // Adding the comment to the local MongoDB post object

        if (!postDBMongo.addCommentInPostArray(post, comment)) {             // Updating the actual document in MongoDB
            return false;               // Aborting whole operation, this will make insertComment() fail and rollback
        }
        return true;
    }
}