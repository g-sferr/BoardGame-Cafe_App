package it.unipi.dii.lsmsdb.boardgamecafe.repository.mongodbms;

import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.CommentModel;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.mongo.PostModelMongo;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostDBMongoTest {
    @Autowired
    PostDBMongo postDBMongo;

    static PostModelMongo post1;
    static PostModelMongo post2;
    static PostModelMongo post3;
    static CommentModel comment1;
    static final String testBoardgameName1 = "TestBoardgame1";
    static final String testBoardgameName2 = "TestBoardgame2";
    static final String testUsername = "testUsername";

    @BeforeAll
    public static void setup() {
        post1 = new PostModelMongo("gino", "TestPost1", "TextTestPost1", testBoardgameName1, new Date());
        post2 = new PostModelMongo("gino", "TestPost2", "TextTestPost2", testBoardgameName2, new Date());
        post3 = new PostModelMongo(testUsername, "TestPost3", "TextTestPost3", testBoardgameName2, new Date());
    }

    @AfterAll
    public static void clean() {}

    @Test @Order(1)
    public void GIVEN_new_posts_WHEN_adding_posts_to_database_THEN_posts_are_saved() {
        post1 = postDBMongo.addPost(post1);
        post2 = postDBMongo.addPost(post2);
        post3 = postDBMongo.addPost(post3);

        assertInstanceOf(PostModelMongo.class, post1);
        assertInstanceOf(PostModelMongo.class, post2);
        assertInstanceOf(PostModelMongo.class, post3);
    }

    // [18:05] - [18:00] - [17:00] - [16:00]
    /*@Test @Order(5)
    void testDataUpdate()
    {
        List<PostModelMongo> allPost = postDBMongo.findRecentPosts(14005, 0);
        int countUpdatedPost = 0;
        for (PostModelMongo post : allPost)
        {
            if (post.getComments() != null && (post.getComments().size() > 0))
            {
                post.getComments().sort((c1, c2) -> c2.getTimestamp().compareTo(c1.getTimestamp())); // Ordinamento decrescente per timestamp
                boolean shouldReturnTrue = postDBMongo.updatePost(post.getId(), post);
                assertTrue(shouldReturnTrue);
            }
            if (countUpdatedPost % 100 == 0)
                System.out.println("countUpdatedPost -> " + countUpdatedPost);
            countUpdatedPost++;
        }
        System.out.println("Updated");
    }
    */

    @Test @Order(10)
    public void GIVEN_existing_post_WHEN_updating_post_text_THEN_text_is_updated() {
        String updatedText = "Updated test";
        post1.setText(updatedText);
        postDBMongo.updatePost(post1.getId(), post1);

        PostModelMongo updatedPost = postDBMongo.findById(post1.getId()).get();
        assertEquals(updatedText, updatedPost.getText());
    }

    @Test @Order(20)
    public void GIVEN_existing_post_WHEN_incrementing_like_count_THEN_like_count_increases() {
        postDBMongo.updateLikeCount(post1.getId(), true);

        PostModelMongo updatedPost = postDBMongo.findById(post1.getId()).get();
        assertEquals(1, updatedPost.getLikeCount());
    }

    @Test @Order(30)
    public void GIVEN_posts_with_specific_tag_WHEN_deleting_posts_by_tag_THEN_posts_are_removed() {
        postDBMongo.deleteByTag(testBoardgameName2);

        var deletedPost = postDBMongo.findById(post2.getId());
        assertTrue(deletedPost.isEmpty());
    }

    @Test @Order(40)
    public void GIVEN_posts_by_specific_user_WHEN_deleting_posts_by_username_THEN_posts_are_removed() {
        postDBMongo.deleteByUsername(testUsername);

        var deletedPost = postDBMongo.findById(post3.getId());
        assertTrue(deletedPost.isEmpty());
    }

    @Test @Order(50)
    public void GIVEN_post_with_comments_WHEN_adding_comment_to_post_THEN_comment_is_added() {
        String testCommentID = "000000000000000000000000";
        comment1 = new CommentModel(testCommentID, post1.getId(), testUsername, "test comment text", new Date());
        postDBMongo.addCommentInPostArray(post1, comment1);

        PostModelMongo updatedPost = postDBMongo.findById(post1.getId()).get();
        assertEquals(testCommentID, updatedPost.getComments().get(0).getId());
    }

    @Test @Order(60)
    public void GIVEN_existing_comment_WHEN_updating_comment_text_THEN_text_is_updated() {
        String updatedCommentText = "updatedCommentText";
        comment1.setText(updatedCommentText);

        postDBMongo.updatePostComment(post1, comment1);
        CommentModel updatedComment = postDBMongo.findById(post1.getId()).get().getComments().get(0);
        assertEquals(updatedCommentText, updatedComment.getText());
    }

    @Test @Order(70)
    public void GIVEN_post_with_comments_WHEN_deleting_comment_from_post_THEN_comment_is_removed() {
        postDBMongo.deleteCommentFromArrayInPost(post1, comment1);

        PostModelMongo updatedPost = postDBMongo.findById(post1.getId()).get();
        assertEquals(0, updatedPost.getComments().size());
    }

    @Test @Order(200)
    public void GIVEN_existing_post_WHEN_deleting_post_THEN_post_is_removed() {
        assertTrue(postDBMongo.deletePost(post1));
    }
}
