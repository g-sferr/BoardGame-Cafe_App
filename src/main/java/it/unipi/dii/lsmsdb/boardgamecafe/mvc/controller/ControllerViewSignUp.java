package it.unipi.dii.lsmsdb.boardgamecafe.mvc.controller;

import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.mongo.GenericUserModelMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.model.mongo.UserModelMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.mvc.view.StageManager;
import it.unipi.dii.lsmsdb.boardgamecafe.repository.mongodbms.UserDBMongo;
import it.unipi.dii.lsmsdb.boardgamecafe.services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ControllerViewSignUp implements Initializable {

    @FXML
    private TextField textFieldFirstName;
    @FXML
    private Label labelFirstName;
    @FXML
    private TextField textFieldLastName;
    @FXML
    private Label labelLastName;
    @FXML
    private ComboBox<String> comboBoxNationality;
    @FXML
    private Label labelNationality;
    @FXML
    private ComboBox<String> comboBoxGender;
    @FXML
    private Label labelGender;
    @FXML
    private DatePicker datePickerDate;
    @FXML
    private Label labelDate;
    @FXML
    private TextField textFieldEmail;
    @FXML
    private Label labelEmail;
    @FXML
    private TextField textFieldUsername;
    @FXML
    private Label labelUsername;
    @FXML
    private TextField textFieldPassword;
    @FXML
    private Label labelPassword;
    @FXML
    private TextField textFieldRepeatPassword;
    @FXML
    private Label labelRepeatedPassword;
    @FXML
    private Button buttonCancel;
    @FXML
    private Button buttonFinish;

    @Autowired
    private UserDBMongo userDBMongo;
    @Autowired
    private UserService serviceUser;

    private static final String EMAIL_REGEX =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern pattern = Pattern.compile(EMAIL_REGEX);

    private final StageManager stageManager;

    @Autowired
    @Lazy
    public ControllerViewSignUp(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.initComboBox();
    }

    public void onClickFinish() {
        String firstName = this.textFieldFirstName.getText();
        String lastName = this.textFieldLastName.getText();
        String country = this.comboBoxNationality.getValue();
        String gender = this.comboBoxGender.getValue();
        LocalDateTime dateOfBirth = selectDate();
        String email = selectEmail();
        String username = selectUsername();
        String password = this.textFieldPassword.getText();
        String repeatedPassword = this.textFieldRepeatPassword.getText();
        int year=0; int month=0 ; int day=0;

        boolean isValid = true;

        if (firstName.isEmpty()) {
            labelFirstName.setText("Your first name is missing.");
            isValid = false;
        } else {
            labelFirstName.setText("");
        }
        if (lastName.isEmpty()) {
            labelLastName.setText("Your last name is missing.");
            isValid = false;
        } else {
            labelLastName.setText("");
        }
        if (country == null) {
            labelNationality.setText("Your country is missing.");
            isValid = false;
        } else {
            labelNationality.setText("");
        }
        if (gender == null) {
            labelGender.setText("Your gender is missing.");
            isValid = false;
        } else {
            labelGender.setText("");
        }
        if (dateOfBirth == null) {
            labelDate.setText("Your date of birth is missing.");
            isValid = false;
        } else {
            labelDate.setText("");
            year = dateOfBirth.getYear();
            month = dateOfBirth.getMonthValue();
            day = dateOfBirth.getDayOfMonth();
        }
        if (email.isEmpty()) {
            labelEmail.setText("Your e-mail is missing.");
            isValid = false;
        } else if (email.equals("user_banned")) {
            labelEmail.setText("A user is already associated to this e-mail address.");
            isValid = false;
        } else if (email.equals("already_used")) {
            labelEmail.setText("A user is already associated to this e-mail address");
            isValid = false;
        } else if (!validateEmail(email)) {
            labelEmail.setText("The used e-mail is not valid.");
            isValid = false;
        } else {
            labelEmail.setText("");
        }
        if (username.isEmpty()) {
            labelUsername.setText("Please choose a username is missing.");
            isValid = false;
        } else if (username.equals("already_used")) {
            labelUsername.setText("This username is already taken.");
            isValid = false;
        } else {
            labelUsername.setText("");
        }
        if (password.isEmpty()) {
            labelPassword.setText("Please choose a password.");
            labelRepeatedPassword.setText("");
            isValid = false;
        } else if (repeatedPassword.isEmpty()) {
            labelPassword.setText("");
            labelRepeatedPassword.setText("Please repeat the password.");
            isValid = false;
        } else if (!password.equals(repeatedPassword)) {
            labelPassword.setText("");
            labelRepeatedPassword.setText("The two passwords do not match.");
            isValid = false;
        } else {
            labelPassword.setText("");
            labelRepeatedPassword.setText("");
        }

        if (isValid) {
            UserModelMongo newUser = serviceUser.createUser(
                                    username, email, password,
                                    firstName, lastName, gender,
                                    country, year, month, day);
            if(serviceUser.insertUser(newUser)){
                stageManager.showInfoMessage("INFO", "You successfully registered to Boardgame-Cafè!");
                stageManager.closeStageButton(this.buttonFinish);
            } else {
                stageManager.showInfoMessage("INFO", "Something went wrong. Please try again in a while.");
            }
        }
    }

    public static boolean validateEmail(String email) {
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public LocalDateTime selectDate() {
        LocalDate selectedDate = this.datePickerDate.getValue();
        LocalDate currentDate = LocalDate.now();

        if (selectedDate == null) {
            labelDate.setText("Your date of birth is missing.");
        } else if (selectedDate.isAfter(currentDate)) {
            labelDate.setText("You can't choose a future date as your date of birth.");
        } else {
            LocalDateTime dateTime = selectedDate.atStartOfDay();
            Period age = Period.between(selectedDate, currentDate);

            if (age.getYears() < 13 ) {
                labelDate.setText("You must be at least 13 years old to register to this application.");
            } else {
                labelDate.setText("");
                return dateTime;
            }
        }
        return null;
    }

    public String selectEmail() {
        Optional<GenericUserModelMongo> user = userDBMongo.findByEmail(this.textFieldEmail.getText());
        if (user.isPresent()) {
            UserModelMongo userFromMongo = (UserModelMongo) user.get();
            if (userFromMongo.isBanned())
                return "user_banned";
            return "already_used";
        }
        return this.textFieldEmail.getText();
    }


    public String  selectUsername(){
        Optional<GenericUserModelMongo> user = userDBMongo.findByUsername(this.textFieldUsername.getText(), false);
        if (user.isPresent()) {
            return "already_used";
        }
        return this.textFieldUsername.getText();
    }

    public void onClickCancel() {
        stageManager.closeStageButton(this.buttonCancel);
    }

    private void initComboBox() {
        this.comboBoxGender.getItems().addAll("Male", "Female", "Prefer not to say");
        this.comboBoxNationality.getItems().addAll(
                "Afghanistan", "Albania", "Algeria", "American Samoa", "Andorra", "Angola", "Anguilla", "Antarctica",
                "Antigua and Barbuda", "Argentina", "Armenia", "Aruba", "Australia", "Austria", "Azerbaijan", "Bahamas",
                "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bermuda", "Bhutan",
                "Bolivia", "Bonaire Sint Eustatius and Saba", "Bosnia and Herzegovina", "Botswana", "Bouvet Island",
                "Brazil", "British Indian Ocean Territory", "Brunei Darussalam", "Bulgaria", "Burkina Faso", "Burundi",
                "Cabo Verde", "Cambodia", "Cameroon", "Canada", "Cayman Islands", "Central African Republic", "Chad",
                "Chile", "China", "Christmas Island", "Cocos Islands", "Colombia", "Comoros", "Congo", "Congo",
                "Cook Islands", "Costa Rica", "Croatia", "Cuba", "Curaçao", "Cyprus", "Czechia", "Côte d'Ivoire",
                "Denmark", "Djibouti", "Dominica", "Dominican Republic", "Ecuador", "Egypt", "El Salvador",
                "Equatorial Guinea", "Eritrea", "Estonia", "Eswatini", "Ethiopia", "Falkland Islands", "Faroe Islands",
                "Fiji", "Finland", "France", "French Guiana", "French Polynesia", "French Southern Territories",
                "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Gibraltar", "Greece", "Greenland", "Grenada",
                "Guadeloupe", "Guam", "Guatemala", "Guernsey", "Guinea", "Guinea-Bissau", "Guyana", "Haiti",
                "Heard Island and McDonald Islands", "Holy See", "Honduras", "Hong Kong", "Hungary", "Iceland",
                "India", "Indonesia", "Iran", "Iraq", "Ireland", "Isle of Man", "Israel", "Italy", "Jamaica",
                "Japan", "Jersey", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "North Korea", "South Korea",
                "Kuwait", "Kyrgyzstan", "Lao", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein",
                "Lithuania", "Luxembourg", "Macao", "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta",
                "Marshall Islands", "Martinique", "Mauritania", "Mauritius", "Mayotte", "Mexico", "Micronesia",
                "Moldova", "Monaco", "Mongolia", "Montenegro", "Montserrat", "Morocco", "Mozambique", "Myanmar",
                "Namibia", "Nauru", "Nepal", "Netherlands", "New Caledonia", "New Zealand", "Nicaragua", "Niger",
                "Nigeria", "Niue", "Norfolk Island", "Northern Mariana Islands", "Norway", "Oman", "Pakistan", "Palau",
                "Palestine State of", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Pitcairn",
                "Poland", "Portugal", "Puerto Rico", "Qatar", "Republic of North Macedonia", "Romania",
                "Russian Federation", "Rwanda", "Réunion", "Saint Barthélemy", "Saint Helena Ascension and Tristan da Cunha",
                "Saint Kitts and Nevis", "Saint Lucia", "Saint Martin", "Saint Pierre and Miquelon",
                "Saint Vincent and the Grenadines", "Samoa", "San Marino", "Sao Tome and Principe", "Saudi Arabia",
                "Senegal", "Serbia", "Seychelles", "Sierra Leone", "Singapore", "Sint Maarten", "Slovakia", "Slovenia",
                "Solomon Islands", "Somalia", "South Africa", "South Georgia and the South Sandwich Islands", "South Sudan",
                "Spain", "Sri Lanka", "Sudan", "Suriname", "Svalbard and Jan Mayen", "Sweden", "Switzerland",
                "Syrian Arab Republic", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste", "Togo", "Tokelau",
                "Tonga", "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan", "Turks and Caicos Islands",
                "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States Minor Outlying Islands",
                "United States of America", "Uruguay", "Uzbekistan", "Vanuatu", "Venezuela", "Viet Nam", "Virgin Islands",
                "Virgin Islands", "Wallis and Futuna", "Western Sahara", "Yemen", "Zambia", "Zimbabwe", "Åland Islands"
        );
    }
}
