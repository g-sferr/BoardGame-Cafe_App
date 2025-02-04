package it.unipi.dii.lsmsdb.boardgamecafe.mvc.controller;

import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.ModelBean;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.CommentModel;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.mongo.PostModelMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.view.StageManager;
import it.unipi.dii.lsmsdb.boardgamecafe.repository.mongodbms.PostDBMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.utils.Constants;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.util.ResourceBundle;

@Component
public class ControllerViewEditCommentPage implements Initializable {

    @FXML
    public Button cancelButton;
    @FXML
    public Button submitButton;
    @FXML
    public TextField bodyTextLabel;

    @Autowired
    private ModelBean modelBean;
    @Autowired
    private PostDBMongo postDBMongo;
    @Autowired
    @Lazy
    private StageManager stageManager;

    private static CommentModel selectedComment;
    private static PostModelMongo postReferredByComment;

    public ControllerViewEditCommentPage() {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        selectedComment = (CommentModel) modelBean.getBean(Constants.SELECTED_COMMENT);
        postReferredByComment = (PostModelMongo) modelBean.getBean(Constants.SELECTED_POST);
        this.bodyTextLabel.setText(selectedComment.getText());
    }

    public void onClickCancelButton() {
        String updatedBody = this.bodyTextLabel.getText();

        if (updatedBody.equals(selectedComment.getText())) {        // No changes were made
            stageManager.closeStage();
            return;
        }

        boolean userChoice = stageManager.showUpdatePostInfoMessage();      // Ask confirmation, as changes were made
        if (userChoice) {
            stageManager.closeStage();
        }
    }

    public void onClickSubmitButton() {
        String updatedBody = this.bodyTextLabel.getText();

        if (updatedBody.equals(selectedComment.getText())) {      // Nothing was updated, ok to close and no further action
            stageManager.closeStage();
            return;
        }

        // Changes were made, actually update comment
        try {
            // Update mongoDB comment in comment collection
            CommentModel updatedComment = selectedComment;
            updatedComment.setText(updatedBody);
            postDBMongo.updatePostComment(postReferredByComment, updatedComment);

            // Setting updated in model bean to retrieve them in post details page for UI update
            modelBean.putBean(Constants.UPDATED_COMMENT, updatedComment);

            System.out.println("[INFO] Successfully updated a comment.");
            stageManager.closeStage();
        } catch (Exception ex) {
            stageManager.showInfoMessage("INFO", "Something went wrong. Please try again  in a while.");
            System.err.println("[ERROR] onClickSubmitButton()@ControllerViewEditPostPage.java raised an exception: " + ex.getMessage());
        }
    }
}
