
package com.risjavafx.pages.appointments;

import com.risjavafx.Driver;
import com.risjavafx.Miscellaneous;
import com.risjavafx.components.InfoTable;
import com.risjavafx.components.NavigationBar;
import com.risjavafx.components.TableSearchBar;
import com.risjavafx.components.TitleBar;
import com.risjavafx.pages.*;
import com.risjavafx.pages.LoadingService;
import com.risjavafx.pages.PageManager;
import com.risjavafx.pages.Pages;
import com.risjavafx.pages.TableManager;
import com.risjavafx.pages.admin.AdminEditPopup;
import com.risjavafx.popups.models.PopupConfirmation;
import com.risjavafx.popups.PopupManager;
import com.risjavafx.popups.Popups;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Appointments implements Initializable, Loadable {
    public BorderPane mainContainer;
    public HBox topContent, titleBar, tableSearchBarContainer;
    public StackPane centerContent;
    public SplitPane centerContentContainer;
    public static ObservableList<AppointmentData> observableList = FXCollections.observableArrayList();
    SortedList<AppointmentData> sortedList;
    FilteredList<AppointmentData> filteredList;


    InfoTable<AppointmentData, String> infoTable = new InfoTable<>();
    TableSearchBar tableSearchBar = new TableSearchBar();
    Miscellaneous misc = new Miscellaneous();

    public TableColumn<AppointmentData, String>
            appointmentId = new TableColumn<>("ID"),
            patient = new TableColumn<>("Patient"),
            modality = new TableColumn<>("Modality"),
            price = new TableColumn<>("Price"),
            dateTime = new TableColumn<>("Date and Time"),
            radiologist = new TableColumn<>("Radiologist"),
            technician = new TableColumn<>("Technician"),
            closedFlag = new TableColumn<>("Closed");

    public ArrayList<TableColumn<AppointmentData, String>> tableColumnsList = new ArrayList<>() {{
        add(appointmentId);
        add(patient);
        add(modality);
        add(price);
        add(dateTime);
        add(radiologist);
        add(technician);
        add(closedFlag);
    }};


    // Load NavigationBar component into home-page.fxml
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        observableList.clear();
        Pages.setPage(Pages.APPOINTMENTS);
        TitleBar.createTitleBar(mainContainer, titleBar);
        NavigationBar.createNavBar(topContent);
        createTable();
        tableViewListener();
        manageRowSelection();

        // Overrides caching functionality and loads *TableSearchBar* every time page is opened
        PageManager.getScene().rootProperty().addListener(observable -> {
            if (Pages.getPage() == Pages.APPOINTMENTS) {
                createTableSearchBar();
                refreshTable();
            }
        });
    }

    public void createTableSearchBar() {
        tableSearchBar.createSearchBar(tableSearchBarContainer);
        tableSearchBarAddButtonListener();
        tableSearchBarEditButtonListener();
        setComboBoxItems();
        filterData();

        if (!infoTable.tableView.getSelectionModel().getSelectedItems().isEmpty()) {
            tableSearchBar.toggleButtons(false);
        }

    }

    public void createTable() {
        try {
            setCellFactoryValues();

            infoTable.setColumns(tableColumnsList);
            infoTable.addColumnsToTable();
            infoTable.setCustomColumnWidth(appointmentId, .07);
            infoTable.setCustomColumnWidth(patient, .15);
            infoTable.setCustomColumnWidth(modality, .13);
            infoTable.setCustomColumnWidth(price, .09);
            infoTable.setCustomColumnWidth(dateTime, .15);
            infoTable.setCustomColumnWidth(radiologist, .15);
            infoTable.setCustomColumnWidth(technician, .15);
            infoTable.setCustomColumnWidth(closedFlag, .11);


            centerContentContainer.setMaxWidth(misc.getScreenWidth() * .75);
            centerContentContainer.setMaxHeight(misc.getScreenHeight() * .85);
            centerContent.getChildren().add(infoTable.tableView);

            queryData(getAllDataStringQuery());
            infoTable.tableView.setItems(observableList);
            infoTable.tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            TableManager.setAppointmentsTable(infoTable.tableView);
        } catch (
                Exception exception) {
            exception.printStackTrace();
        }
    }

    private void refreshTable() {
        if (!centerContent.getChildren().contains(TableManager.getAppointmentsTable())) {
            centerContent.getChildren().add(TableManager.getAppointmentsTable());
        }
    }


    public static void queryData(String sql) throws SQLException {
        // ObservableList<AppointmentData> observableList = FXCollections.observableArrayList();
        ResultSet resultSet = Driver.getConnection().createStatement().executeQuery(sql);

        while (resultSet.next()) {
            String name = resultSet.getString("first_name") + " " + resultSet.getString("last_name");
            String checkIn = "";
            if (resultSet.getInt("closed") == 0) {
                checkIn = "In Progress";
            } else if (resultSet.getInt("closed") == 1) {
                checkIn = "Concluded";
            }
            observableList.add(new AppointmentData(
                    resultSet.getInt("appointment_id"),
                    name,
                    resultSet.getString("name"),
                    "$" + resultSet.getString("price"),
                    resultSet.getString("date_time"),
                    resultSet.getString(7),
                    resultSet.getString(8),
                    checkIn
            ));
        }
        //  return observableList;
    }

    public String getAllDataStringQuery() {
        return """
                SELECT appointments.appointment_id,
                       patients.first_name,
                       patients.last_name,
                       modalities.name,
                       modalities.price,
                       DATE_FORMAT(appointments.date_time, '%c/%e/%Y %H:%i') as date_time,
                       u1.full_name,
                       u2.full_name,
                       appointments.closed
                FROM appointments,
                     users as u1,
                     users as u2,
                     patients,
                     modalities
                WHERE u1.user_id = appointments.radiologist
                  AND u2.user_id = appointments.technician
                  AND patients.patient_id = appointments.patient
                  AND modalities.modality_id = appointments.modality
                ORDER BY appointments.appointment_id;
                """;
    }

    public static String getLastRowStringQuery() {
        return """
                SELECT appointments.appointment_id,
                       patients.first_name,
                       patients.last_name,
                       modalities.name,
                       modalities.price,
                       DATE_FORMAT(appointments.date_time, '%c/%e/%Y %H:%i') as date_time,
                       u1.full_name,
                       u2.full_name,
                       appointments.closed
                FROM appointments,
                     users as u1,
                     users as u2,
                     patients,
                     modalities
                WHERE u1.user_id = appointments.radiologist
                  AND u2.user_id = appointments.technician
                  AND patients.patient_id = appointments.patient
                  AND modalities.modality_id = appointments.modality
                ORDER BY appointments.appointment_id
                        DESC
                LIMIT 1;
                """;
    }

    @SuppressWarnings("SqlWithoutWhere")
    public void deleteSelectedItemsQuery(String table) throws SQLException {

        ObservableList<AppointmentData> selectedItems = infoTable.tableView.getSelectionModel().getSelectedItems();
        for (AppointmentData selectedItem : selectedItems) {
            String sql = """
                    DELETE FROM %$
                    WHERE appointment_id = ?
                    """.replace("%$", table);
            PreparedStatement preparedStatement = Driver.getConnection().prepareStatement(sql);
            preparedStatement.setInt(1, selectedItem.getAppointmentId());
            preparedStatement.execute();
        }
    }

    public void changeCheckIn() throws SQLException {

        ObservableList<AppointmentData> selectedItems = infoTable.tableView.getSelectionModel().getSelectedItems();
        for (AppointmentData selectedItem : selectedItems) {
            String sql = """
                    UPDATE appointments
                    SET closed = !closed
                    WHERE appointment_id = ?;
                    """;
            PreparedStatement preparedStatement = Driver.getConnection().prepareStatement(sql);
            preparedStatement.setInt(1, selectedItem.getAppointmentId());
            preparedStatement.execute();

            Loadable loadable = new Appointments();
            String notiHeader = "Submission Complete";
            String notiText = "You have successfully changed your information";
            LoadingService.CustomReload defaultReset = new LoadingService.CustomReload(loadable, notiHeader, notiText);
            defaultReset.start();
        }
    }

    public void loadMethods() {
        PageManager.loadPagesToCache();
        PopupManager.loadPopupsToCache(Popups.values());
    }

    public void setCellFactoryValues() {
        appointmentId.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        patient.setCellValueFactory(new PropertyValueFactory<>("patient"));
        modality.setCellValueFactory(new PropertyValueFactory<>("modality"));
        price.setCellValueFactory(new PropertyValueFactory<>("price"));
        dateTime.setCellValueFactory(new PropertyValueFactory<>("date"));
        radiologist.setCellValueFactory(cellData -> cellData.getValue().radiologist);
        technician.setCellValueFactory(cellData -> cellData.getValue().technician);
        closedFlag.setCellValueFactory(new PropertyValueFactory<>("closedFlag"));
    }

    public void setComboBoxItems() {
        ObservableList<String> oblist = FXCollections.observableArrayList(
                "Patient",
                "Date",
                "Radiologist",
                "Technician"
        );
        tableSearchBar.getComboBox().setItems(oblist);
    }

    public boolean getComboBoxItem(String string) {
        String selectedComboValue = tableSearchBar.getComboBox().getValue();
        return string.equals(selectedComboValue) || "All".equals(selectedComboValue);
    }

    // Listener for Admin TextField and ComboBox
    public void filterData() {
        try {
            filteredList = new FilteredList<>(Appointments.observableList);

            tableSearchBar.getTextField().textProperty().addListener((observable, oldValue, newValue) ->
                    filteredList.setPredicate(appointmentData -> filterDataEvent(newValue, appointmentData)));

            tableSearchBar.getComboBox().valueProperty().addListener((newValue) -> filteredList.setPredicate(appointmentData -> {
                if (newValue != null) {
                    return filterDataEvent(tableSearchBar.getTextField().getText(), appointmentData);
                }
                return false;
            }));

            sortedList = new SortedList<>(filteredList);
            sortedList.comparatorProperty().bind(infoTable.tableView.comparatorProperty());
            infoTable.tableView.setItems(sortedList);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public boolean filterDataEvent(String newValue, AppointmentData appointmentData) {
        if (tableSearchBar.getComboBox().getValue() == null) {
            tableSearchBar.getErrorLabel().setText("Please select a filter");
        } else {
            tableSearchBar.getErrorLabel().setText(null);
        }

        if (newValue.isEmpty() || newValue.isBlank()) {
            return true;
        }

        String searchKeyword = newValue.toLowerCase();

        if (appointmentData.getPatient().toLowerCase().contains(searchKeyword) && getComboBoxItem("Patient")) {
            return true;
        } else if (appointmentData.getDate().toLowerCase().contains(searchKeyword) && getComboBoxItem("Date")) {
            return true;
        } else if (appointmentData.getDate().toLowerCase().contains(searchKeyword) && getComboBoxItem("Technician")) {
            return true;
        } else
            return appointmentData.getRadiologist().toLowerCase().contains(searchKeyword) && getComboBoxItem("Radiologist");
    }


    // Listener for Admin TableView
    public void tableViewListener() {
        infoTable.tableView.getSelectionModel().selectedItemProperty().addListener((observableValue, adminData, t1) -> {
            if (t1 != null) {
                tableSearchBar.toggleButtons(false);
                tableSearchBar.getDeleteButton().setOnAction(actionEvent ->
                        customConfirmationPopup(confirm -> confirmDeletion(), cancel -> PopupManager.removePopup()));
            } else {
                tableSearchBar.toggleButtons(true);
            }
            AppointmentEditPopup.setAppointmentClickedId(infoTable.tableView.getSelectionModel().getSelectedItem().appointmentId.get());
            tableSearchBar.getCheckInButton().setOnAction(actionEvent ->
                    customCheckInConfirmationPopup(confirm -> confirmCheckIn(), cancel -> PopupManager.removePopup()));
            refreshTable();
        });
    }

    // If a selected row is clicked again, it will unselect. TableSearchBar Buttons will also adjust appropriately
    public void manageRowSelection() {
        infoTable.tableView.setRowFactory(tableView -> {
            final TableRow<AppointmentData> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                final int index = row.getIndex();
                if (index >= 0 && index < infoTable.tableView.getItems().size() &&
                    infoTable.tableView.getSelectionModel().isSelected(index)) {
                    infoTable.tableView.getSelectionModel().clearSelection();

                    tableSearchBar.toggleButtons(true);
                    event.consume();
                }
            });
            return row;
        });
    }

    public void customConfirmationPopup(EventHandler<ActionEvent> confirm, EventHandler<ActionEvent> cancel) {
        PopupManager.createPopup(Popups.CONFIRMATION);
        new PopupConfirmation() {{
            setConfirmButtonLabel("Continue");
            setExitButtonLabel("Cancel");
            setHeaderLabel("Warning");
            setContentLabel("This data will be permanently deleted");
            getConfirmationButton().setOnAction(confirm);
            getCancelButton().setOnAction(cancel);
        }};
    }

    public void customCheckInConfirmationPopup(EventHandler<ActionEvent> confirm, EventHandler<ActionEvent> cancel) {
        PopupManager.createPopup(Popups.CONFIRMATION);
        new PopupConfirmation() {{
            setConfirmButtonLabel("Yes");
            setExitButtonLabel("No");
            setHeaderLabel("Notice");
            setContentLabel("This Person is done with their appointment");
            getConfirmationButton().setOnAction(confirm);
            getCancelButton().setOnAction(cancel);
        }};
    }

    private void confirmCheckIn() {
        try {
            changeCheckIn();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        closedFlag.setCellValueFactory(new PropertyValueFactory<>("closedFlag"));
        PopupManager.removePopup();
        refreshTable();

    }

    public void confirmDeletion() {

        observableList.removeAll(infoTable.tableView.getSelectionModel().getSelectedItems());
        PopupManager.removePopup();
    }

    public void tableSearchBarAddButtonListener() {
        tableSearchBar.getAddButton().setOnAction(event -> PopupManager.createPopup(Popups.APPOINTMENT));

    }

    public void tableSearchBarEditButtonListener() {
        tableSearchBar.getEditButton().setOnAction(event -> PopupManager.createPopup(Popups.APPOINTMENT_EDIT));
    }
}