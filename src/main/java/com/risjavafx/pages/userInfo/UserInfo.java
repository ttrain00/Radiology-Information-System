package com.risjavafx.pages.userInfo;

import com.risjavafx.Driver;
import com.risjavafx.UserStates;
import com.risjavafx.components.TitleBar;
import com.risjavafx.components.NavigationBar;
import com.risjavafx.pages.Pages;
import com.risjavafx.popups.models.Notification;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class UserInfo implements Initializable {

    public BorderPane mainContainer;
    public HBox titleBar;
    public HBox topContent;
    public Button confirmButton;
    public TextField idTextField;
    public TextField usernameTextField;
    public TextField fullNameTextField;
    public TextField emailAdrTextField;
    public TextField passwordTextField;
    public Label welcomeLabel;

    // Load NavigationBar component into home-page.fxml
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Pages.setPage(Pages.USERINFO);
        TitleBar.createTitleBar(mainContainer, titleBar);
        NavigationBar.createNavBar(topContent);

        confirmButton.setStyle("-fx-font-size: 24px");
        createTooltip(idTextField);
        setElements();
    }

    private void setElements() {
        try {
            Driver driver = new Driver();
            PreparedStatement preparedStatement;
            final String sql = """
                    SELECT user_id, email, full_name, username, password
                    FROM users
                    WHERE user_id = ?;
                    """;
            preparedStatement = driver.connection.prepareStatement(sql);
            preparedStatement.setInt(1, UserStates.getUserId());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                welcomeLabel.setText("Welcome " + resultSet.getString("full_name"));
                idTextField.setPromptText(String.valueOf(resultSet.getInt("user_id")));
                usernameTextField.setPromptText(resultSet.getString("username"));
                fullNameTextField.setPromptText(resultSet.getString("full_name"));
                emailAdrTextField.setPromptText(resultSet.getString("email"));
                passwordTextField.setPromptText(resultSet.getString("password"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTooltip(Control element) {
        Tooltip tooltip = new Tooltip("This cannot be changed");
        tooltip.setShowDelay(new Duration(0));
        element.setTooltip(tooltip);
    }

    // Confirm button onClick
    public void confirmChanges() {
        try {
            Driver driver = new Driver();
            PreparedStatement preparedStatement;
            final String sql = """
                    UPDATE users
                    SET username = ?, password = ?, full_name = ?, email = ?
                    WHERE user_id = ?;
                    """;
            preparedStatement = driver.connection.prepareStatement(sql);
            preparedStatement.setString(1, getText(usernameTextField));
            preparedStatement.setString(2, getText(passwordTextField));
            preparedStatement.setString(3, getText(fullNameTextField));
            preparedStatement.setString(4, getText(emailAdrTextField));
            preparedStatement.setInt(5, UserStates.getUserId());
            preparedStatement.execute();

            resetTextFields();
            welcomeLabel.setText("Welcome " + getText(fullNameTextField));

            Notification.createNotification("Changes Complete", "You successfully changed your information");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getText(TextField textField) {
        return textField.getText().isBlank() ? textField.getPromptText() : textField.getText();
    }

    private void resetTextFields() {
        usernameTextField.setPromptText(getText(usernameTextField));
        passwordTextField.setPromptText(getText(passwordTextField));
        emailAdrTextField.setPromptText(getText(emailAdrTextField));
        fullNameTextField.setPromptText(getText(fullNameTextField));

        usernameTextField.clear();
        passwordTextField.clear();
        emailAdrTextField.clear();
        fullNameTextField.clear();
    }
}
